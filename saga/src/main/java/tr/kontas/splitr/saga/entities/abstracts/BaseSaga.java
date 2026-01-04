package tr.kontas.splitr.saga.entities.abstracts;

import lombok.Getter;
import lombok.Setter;
import tr.kontas.splitr.saga.annotations.Saga;
import tr.kontas.splitr.saga.entities.Event;
import tr.kontas.splitr.saga.entities.EventContext;
import tr.kontas.splitr.saga.entities.State;
import tr.kontas.splitr.saga.exceptions.HandlerNotFoundException;
import tr.kontas.splitr.saga.exceptions.InvalidStateTransitionException;
import tr.kontas.splitr.saga.repositories.SagaRepository;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public abstract class BaseSaga<TState extends BaseSagaState> {
    protected static final String INITIAL_STATE = "Initial";

    @Getter
    protected final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    protected TState state;
    protected Map<String, TState> sagaInstances = new ConcurrentHashMap<>();
    protected Map<Class<?>, Event<?>> events = new ConcurrentHashMap<>();
    protected Map<String, Map<Class<?>, EventActivityBinder<?, TState>>> stateEventHandlers = new ConcurrentHashMap<>();
    protected Set<String> completedSagas = ConcurrentHashMap.newKeySet();
    protected Set<String> cancelledSagas = ConcurrentHashMap.newKeySet();
    protected Map<String, Set<State>> allowedTransitions = new ConcurrentHashMap<>();
    protected boolean strictStateTransitions = false;

    protected final State Completed = new State("Completed");
    protected final State Cancelled = new State("Cancelled");
    protected final State Timeouted = new State("Timeouted");

    private final Saga config;

    protected final SagaRepository<TState> repository;

    protected BaseSaga(SagaRepository<TState> repository) {
        this.repository = repository;
        this.config = validateSagaAnnotation();
    }

    private Saga validateSagaAnnotation() {
        if (!this.getClass().isAnnotationPresent(Saga.class))
            throw new IllegalStateException(String.format("The saga class '%s' must be annotated with @Saga", this.getClass().getSimpleName()));
        return this.getClass().getAnnotation(Saga.class);
    }

    public boolean isCancelled() {
        return state.getCurrentState().equals("Cancelled");
    }

    public boolean isCompleted() {
        return state.getCurrentState().equals("Completed");
    }

    protected abstract TState createNewState();

    // Enable strict state transition checking
    protected void enableStrictTransitions() {
        this.strictStateTransitions = true;
    }

    // Define allowed state transitions
    protected void allowTransition(State from, State to) {
        String fromName = from == null ? INITIAL_STATE : from.getName(); // null kontrolü
        allowedTransitions.computeIfAbsent(fromName, k -> new HashSet<>()).add(to);
    }

    // Event registration
    protected <TEvent> void Event(
            EventProvider<TEvent> eventProvider,
            Consumer<Event<TEvent>> configureCorrelation) {

        Event<TEvent> event = eventProvider.getEvent();
        configureCorrelation.accept(event);
        events.put(event.getEventType(), event);
    }

    // Initially - for initial state
    protected <TEvent> StateEventBinder<TState> Initially(
            EventActivityBinder<TEvent, TState> binder) {
        binder.registerIn(null, stateEventHandlers);
        return new StateEventBinder<>();
    }

    // During - for specific state
    protected <TEvent> StateEventBinder<TState> During(
            State state,
            EventActivityBinder<TEvent, TState> binder) {
        binder.registerIn(state.getName(), stateEventHandlers);
        return new StateEventBinder<>();
    }

    // DuringAny - for any state
    protected <TEvent> StateEventBinder<TState> DuringAny(
            EventActivityBinder<TEvent, TState> binder) {
        binder.registerIn(null, stateEventHandlers);
        for (String stateName : getAllStateNames()) {
            binder.registerIn(stateName, stateEventHandlers);
        }
        return new StateEventBinder<>();
    }

    // When - event matcher
    protected <TEvent> EventActivityBinder<TEvent, TState> When(
            EventProvider<TEvent> eventProvider) {
        Event<TEvent> event = eventProvider.getEvent();
        return new EventActivityBinder<>(event);
    }

    // WhenEnter - state entry (placeholder)
    protected StateEventBinder<TState> WhenEnter(State state, Consumer<TState> action) {
        return new StateEventBinder<>();
    }

    // WhenLeave - state exit (placeholder)
    protected StateEventBinder<TState> WhenLeave(State state, Consumer<TState> action) {
        return new StateEventBinder<>();
    }

    protected void cancelSaga(String correlationId) {
        TState stateInstance = sagaInstances.get(correlationId);
        if (stateInstance != null) {
            stateInstance.setCancelled(true);
            cancelledSagas.add(correlationId);
//            sagaInstances.remove(correlationId);
        }
    }

    protected Set<String> getAllStateNames() {
        Set<String> stateNames = new HashSet<>();
        for (var field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(State.class)) {
                try {
                    field.setAccessible(true);
                    State state = (State) field.get(this);
                    if (state != null) {
                        stateNames.add(state.getName());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return stateNames;
    }

    // Check if saga is cancelled
    public boolean isCancelled(String correlationId) {
        return cancelledSagas.contains(correlationId);
    }

    // Check if saga is completed
    public boolean isCompleted(String correlationId) {
        return completedSagas.contains(correlationId);
    }

    // Get saga state
    public TState getSagaState(String correlationId) {
        return repository.findById(correlationId).orElse(null);
    }

    // Get all active sagas
    public List<TState> getAllActiveSagas() {
        return repository.findAll().stream()
                .filter(s -> !s.isCompleted() && !s.isCancelled())
                .toList();
    }

    public void consume(Object message) {
        int maxRetries = config.retryAttempts();
        int attempt = 0;
        Random random = new Random();
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                processMessage(message);
                return;
            } catch (Exception e) {
                lastException = e;

                if (isRetryable(e) && (attempt + 1) < maxRetries) {
                    attempt++;

                    long backoff = (long) Math.min(Math.pow(1.2, attempt) * 10, 500);
                    int jitter = config.retryWithJitter() ? random.nextInt(200) : 0;
                    long sleepTime = backoff + jitter;

                    System.out.println("[RETRY] Attempt " + attempt + " | Sleeping " + sleepTime + "ms | Cause: " + e.getMessage());

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    // Retry edilebilir bir hata değilse VEYA deneme hakkı bittse fırlat
                    throw wrapAndThrow(lastException);
                }
            }
        }

        // Eğer while bir şekilde doğal yollarla biterse (normalde yukarıdaki throw çalışır)
        if (lastException != null) {
            throw wrapAndThrow(lastException);
        }
    }

    private RuntimeException wrapAndThrow(Exception e) {
        if (e instanceof RuntimeException) return (RuntimeException) e;
        return new RuntimeException(e);
    }

    private boolean isRetryable(Exception e) {
        return e instanceof org.springframework.dao.OptimisticLockingFailureException ||
                e instanceof HandlerNotFoundException;
    }

    // Consume event with exception handling
    public void processMessage(Object message) {
        @SuppressWarnings("unchecked")
        Event<Object> event = (Event<Object>) events.get(message.getClass());
        if (event == null) return;

        String correlationId = event.getCorrelationFunction().apply(message);

        Optional<TState> existingState = repository.findById(correlationId);

        TState stateInstance;
        if (existingState.isPresent()) {
            stateInstance = existingState.get();
        } else {
            Map<Class<?>, EventActivityBinder<?, TState>> initialHandlers = stateEventHandlers.get(INITIAL_STATE);
            if (initialHandlers != null && initialHandlers.containsKey(message.getClass())) {
                stateInstance = createNewState();
                stateInstance.setId(correlationId);
                stateInstance.setCorrelationId(correlationId);
                stateInstance.setCurrentState(INITIAL_STATE);
            } else {
                throw new HandlerNotFoundException(message.getClass().getSimpleName(), "Saga not found yet");
            }
        }

        if (stateInstance.isCompleted() || stateInstance.isCancelled()) return;

        String currentStateName = stateInstance.getCurrentState();
        Map<Class<?>, EventActivityBinder<?, TState>> handlers = stateEventHandlers.get(currentStateName);

        if (handlers == null || !handlers.containsKey(message.getClass())) {
            if (currentStateName.equals(INITIAL_STATE)) return;
            throw new HandlerNotFoundException(message.getClass().getSimpleName(), currentStateName);
        }

        @SuppressWarnings("unchecked")
        EventActivityBinder<Object, TState> handler = (EventActivityBinder<Object, TState>) handlers.get(message.getClass());

        EventContext<Object> context = new EventContext<>(message);
        context.setInstance(stateInstance);
        context.setSaga(this);

        // 3. ADIM: Handler'ı çalıştır
        handler.execute(context, stateInstance, this, correlationId);

        // 4. ADIM: Kaydet ve Hemen Commit Et
        repository.saveAndFlush(stateInstance);
    }

    @FunctionalInterface
    public interface EventProvider<TEvent> {
        Event<TEvent> getEvent();
    }

    // State Event Binder
    public static class StateEventBinder<TState extends BaseSagaState> {
    }

    // Event Activity Binder
    public static class EventActivityBinder<TEvent, TState extends BaseSagaState> {
        private final Event<TEvent> event;
        private final List<BiConsumer<EventContext<TEvent>, TState>> handlers = new ArrayList<>();
        private final List<AsyncHandler<TEvent, TState>> asyncHandlers = new ArrayList<>();
        private final List<ConditionalActivity<TEvent, TState>> conditionalActivities = new ArrayList<>();
        private State transitionToState;
        private boolean shouldFinalize = false;
        private boolean shouldCancel = false;
        private BiPredicate<EventContext<TEvent>, TState> filterPredicate;
        private BiConsumer<EventContext<TEvent>, Exception> exceptionHandler;

        public EventActivityBinder(Event<TEvent> event) {
            this.event = event;
        }

        // Filter - conditional execution
        public EventActivityBinder<TEvent, TState> Filter(
                BiPredicate<EventContext<TEvent>, TState> predicate) {
            this.filterPredicate = predicate;
            return this;
        }

        // Then - sync handler
        public EventActivityBinder<TEvent, TState> Then(
                BiConsumer<EventContext<TEvent>, TState> handler) {
            this.handlers.add(handler);
            return this;
        }

        // ThenAsync - async handler
        public EventActivityBinder<TEvent, TState> ThenAsync(
                BiFunction<EventContext<TEvent>, TState, CompletableFuture<Void>> asyncHandler) {
            this.asyncHandlers.add(new AsyncHandler<>(asyncHandler));
            return this;
        }

        // Catch - exception handler
        public EventActivityBinder<TEvent, TState> Catch(
                BiConsumer<EventContext<TEvent>, Exception> handler) {
            this.exceptionHandler = handler;
            return this;
        }

        // IfElse - conditional branching
        public EventActivityBinder<TEvent, TState> IfElse(
                BiPredicate<EventContext<TEvent>, TState> condition,
                Consumer<EventActivityBinder<TEvent, TState>> thenActivity,
                Consumer<EventActivityBinder<TEvent, TState>> elseActivity) {

            ConditionalActivity<TEvent, TState> conditional = new ConditionalActivity<>(condition);

            if (thenActivity != null) {
                EventActivityBinder<TEvent, TState> thenBinder = new EventActivityBinder<>(event);
                thenActivity.accept(thenBinder);
                conditional.setThenBinder(thenBinder);
            }

            if (elseActivity != null) {
                EventActivityBinder<TEvent, TState> elseBinder = new EventActivityBinder<>(event);
                elseActivity.accept(elseBinder);
                conditional.setElseBinder(elseBinder);
            }

            conditionalActivities.add(conditional);
            return this;
        }

        // If - conditional with only then branch
        public EventActivityBinder<TEvent, TState> If(
                BiPredicate<EventContext<TEvent>, TState> condition,
                Consumer<EventActivityBinder<TEvent, TState>> thenActivity) {
            return IfElse(condition, thenActivity, null);
        }

        // Publish - publish a message
        public EventActivityBinder<TEvent, TState> Publish(
                BiFunction<EventContext<TEvent>, TState, Object> messageFactory) {
            return ThenAsync((context, state) -> {
                Object message = messageFactory.apply(context, state);
                return context.publish(message);
            });
        }

        // Schedule - schedule a message
        public EventActivityBinder<TEvent, TState> Schedule(
                BiFunction<EventContext<TEvent>, TState, Object> messageFactory,
                long delayMillis) {
            return Then((context, state) -> {
                Object timeoutEvent = messageFactory.apply(context, state);

                context.getSaga().getScheduler().schedule(() -> context.getSaga().consume(timeoutEvent), delayMillis, TimeUnit.MILLISECONDS);

                System.out.println("Scheduled: " + timeoutEvent.getClass().getSimpleName());
            });
        }

        // TransitionTo - state transition
        public EventActivityBinder<TEvent, TState> TransitionTo(State state) {
            this.transitionToState = state;
            return this;
        }

        // Finalize - complete saga
        public EventActivityBinder<TEvent, TState> Finalize() {
            this.shouldFinalize = true;
            return this;
        }

        // Cancel - cancel saga
        public EventActivityBinder<TEvent, TState> Cancel() {
            this.shouldCancel = true;
            return this;
        }

        @SuppressWarnings("unchecked")
        public void execute(EventContext<?> context, TState stateInstance,
                            BaseSaga<TState> saga, String correlationId) {

            EventContext<TEvent> typedContext = (EventContext<TEvent>) context;

            try {
                // Apply filter if exists
                if (filterPredicate != null && !filterPredicate.test(typedContext, stateInstance)) {
                    return;
                }

                // Execute sync handlers
                for (BiConsumer<EventContext<TEvent>, TState> handler : handlers) {
                    handler.accept(typedContext, stateInstance);
                }

                // Execute async handlers
                for (AsyncHandler<TEvent, TState> asyncHandler : asyncHandlers) {
                    asyncHandler.handler.apply(typedContext, stateInstance).join();
                }

                // Execute conditional activities
                for (ConditionalActivity<TEvent, TState> conditional : conditionalActivities) {
                    if (conditional.condition.test(typedContext, stateInstance)) {
                        if (conditional.thenBinder != null) {
                            conditional.thenBinder.execute(context, stateInstance, saga, correlationId);
                        }
                    } else {
                        if (conditional.elseBinder != null) {
                            conditional.elseBinder.execute(context, stateInstance, saga, correlationId);
                        }
                    }
                }

            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(typedContext, e);
                } else {
                    throw new RuntimeException("Error executing saga handler", e);
                }
            }

            // Handle cancellation
            if (shouldCancel || typedContext.isCancelled()) {
                saga.cancelledSagas.add(correlationId);
                // saga.sagaInstances.remove(correlationId);
                stateInstance.setCancelled(true);
                stateInstance.setCurrentState("Cancelled");
                if (transitionToState != null) {
                    stateInstance.setCurrentState(transitionToState.getName());
                }
                return;
            }

            // Validate and transition state
            if (transitionToState != null) {
                String currentState = stateInstance.getCurrentState();
                if (currentState == null) currentState = INITIAL_STATE;

                if (saga.strictStateTransitions) {
                    Set<State> allowed = saga.allowedTransitions.get(currentState);
                    if (allowed != null && !allowed.contains(transitionToState)) {
                        throw new InvalidStateTransitionException(
                                "Invalid state transition from " + currentState + " to " + transitionToState.getName()
                        );
                    }
                }

                stateInstance.setCurrentState(transitionToState.getName());
            }

            // Finalize if needed
            if (shouldFinalize) {
                stateInstance.setCompleted(true);
                saga.completedSagas.add(correlationId);
                stateInstance.setCurrentState("Completed");
//                saga.sagaInstances.remove(correlationId);
            }
        }

        public void registerIn(String stateName,
                               Map<String, Map<Class<?>, EventActivityBinder<?, TState>>> handlers) {
            if (stateName == null) stateName = INITIAL_STATE;

            handlers.computeIfAbsent(stateName, k -> new ConcurrentHashMap<>())
                    .put(event.getEventType(), this);
        }

        private static class AsyncHandler<TEvent, TState extends BaseSagaState> {
            private final BiFunction<EventContext<TEvent>, TState, CompletableFuture<Void>> handler;

            public AsyncHandler(BiFunction<EventContext<TEvent>, TState, CompletableFuture<Void>> handler) {
                this.handler = handler;
            }
        }

        private static class ConditionalActivity<TEvent, TState extends BaseSagaState> {
            private final BiPredicate<EventContext<TEvent>, TState> condition;
            @Setter
            private EventActivityBinder<TEvent, TState> thenBinder;
            @Setter
            private EventActivityBinder<TEvent, TState> elseBinder;

            public ConditionalActivity(BiPredicate<EventContext<TEvent>, TState> condition) {
                this.condition = condition;
            }
        }
    }
}
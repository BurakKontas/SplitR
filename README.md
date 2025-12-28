# Splitr: Distributed Synchronous Query Bus

**Splitr** is a lightweight, high-performance Spring Boot library designed to implement the **Request-Response pattern over Kafka**. It allows microservices to execute synchronous, typed queries across distributed boundaries while maintaining idempotency and resilience.

---

## 🔄 Workflow Models

Splitr supports three distinct communication patterns: **Sync Request-Response**, **Async Broadcast (Kafka)**, and **In-Memory Events**.

### 1. Command & Query Workflow (Sync/Async Request-Response)

This model bridges the gap between asynchronous messaging and synchronous execution requirements by creating a temporary bridge between two services.

```mermaid
sequenceDiagram
    participant User
    participant ServiceA as Service-A (Publisher)
    participant Kafka as Kafka Topic
    participant ServiceB as Service-B (Consumer)

    User->>ServiceA: GET /orders/1 (Query)
    ServiceA->>ServiceA: Register ID in SyncRegistry
    ServiceA->>Kafka: Send Request (ID, callbackUrl, payload)
    Note over ServiceA: Thread Blocked (Waiting...)
    
    Kafka->>ServiceB: Consume Message
    ServiceB->>ServiceB: Idempotency Check & Handling
    ServiceB->>ServiceA: HTTP POST (callbackUrl/query) with Result
    
    ServiceA->>ServiceA: Match ID & Complete Promise
    Note over ServiceA: Thread Released
    ServiceA->>User: 200 OK (Data)

```

### 2. Distributed Event Workflow (Pub/Sub over Kafka)

Used to broadcast state changes across the system. It follows a "fire-and-forget" approach where no response is expected from the consumers. Multiple services can listen to the same event independently.

```mermaid
graph LR
    Publisher[Service-A Publisher] -- "publish(Event)" --> Topic((Kafka Event Topic))
    Topic -- "Broadcast" --> Consumer1[Service-B: Handler 1]
    Topic -- "Broadcast" --> Consumer2[Service-C: Handler 1]
    Topic -- "Broadcast" --> Consumer3[Service-C: Handler 2]
    
    subgraph Service-C Logic
    Consumer2
    Consumer3
    end

```

### 3. Domain Event Workflow (In-Memory Pub/Sub)

Designed for internal application logic triggers. It executes entirely **In-Memory** within the same JVM, providing the benefits of loose coupling without network overhead.

```mermaid
sequenceDiagram
    participant App as Business Logic
    participant DEBus as DomainEventBus
    participant H1 as Local Handler A
    participant H2 as Local Handler B

    App->>DEBus: arise(DomainEvent)
    Note right of DEBus: "ARISE!" - Local Dispatch
    par Execute Handlers
        DEBus->>H1: handle(event)
        DEBus->>H2: handle(event)
    end
    DEBus-->>App: Execution Finished

```

---

## 🛠 Usage Examples

### Commands & Queries

```java
// Publisher: Blocking call
String result = queryBus.publishSync(new OrderQuery("123"), String.class);

// Consumer: Implementation
@Component
public class OrderQueryHandler extends BaseQueryHandler<OrderQuery> {
    @Override
    public String handle(OrderQuery q) { return "Data: " + q.orderId(); }
}

```

```java
// Publisher: Blocking call
String result = commandBus.publishSync(new OrderCommand("123"), String.class);

// Consumer: Implementation
@Component
public class OrderCommandHandler extends BaseCommandHandler<OrderCommand> {
    @Override
    public String handle(OrderCommand q) { return "Data: " + q.orderId(); }
}

```

### Distributed Events

```java
// Publisher: Fire-and-forget to Kafka
eventBus.publish(new OrderProcessedEvent("123")); 

@Component
public class OrderEventHandler extends BaseEventHandler<OrderProcessedEvent> {
    @Override
    public void onEvent(OrderProcessedEvent payload) {
        log.info("OrderProcessedEvent in OrderEventHandler processed with id: " + payload.orderId());
    }
}

```

### Domain Events (In-Memory)

```java
// Publisher: Trigger local handlers
domainEventBus.arise(new OrderDomainEvent("123"));

// Consumer: Local Listener
@Component
public class InventoryHandler extends BaseDomainEventHandler<OrderDomainEvent> {
  @Override
  public void onEvent(OrderDomainEvent event) {
    log.info("Adjusting local inventory for: {}", event.getId());
  }
}

```

---

## ✈ How To Send Message From Outside

##### Kafka messages sent by SplitR are structured as below.

### Key (Idempotency aware)
```json
a45db290-b9d4-4cb4-a5cd-ae19bd352b1b
```

### Value
```json
{
	"id": "a45db290-b9d4-4cb4-a5cd-ae19bd352b1b",
	"type": "tr.kontas.splitr.test.CreateOrderCommand",
	"payload": "{\"productName\":\"Laptop\",\"quantity\":1}",
	"callbackUrl": "http://localhost:8083/internal/query/callback",
	"sentAtEpochMs": 1766863517525,
	"timeoutMs": 3000,
	"sync": true
}
```

### Headers
```json
{
	"__TypeId__": "tr.kontas.splitr.dto.CommandRequest"
}
```

##### Produce a message like this and SplitR will dispatch and send post request to callback.

---

## 🚀 Features

* **Synchronous over Kafka:** Blocking local threads for remote responses, mimicking REST over message brokers.
* **Automatic Dispatching:** Just implement `BaseQueryHandler<T>`, and Splitr handles the routing.
* **Idempotency Engine:** Built-in LRU cache to prevent "at-least-once" delivery side effects.
* **Type Safety:** Full support for polymorphic queries via Jackson Type Headers.
* **Configurable Timeouts:** Global or per-request timeout management.

---

## 📦 Installation & Configuration

### 1. Publisher Side (Service A)

Enable the bus and specify where to receive results.

```yaml
splitr:
  publisher:
    enabled: true
  callback-url: http://service-a:8080/internal/query/callback
  bus:
    kafka:
      topic: tr.kontas.splitr.query.topic
    default-timeout: 5000 # ms

```

### 2. Consumer Side (Service B)

Enable the processor and set idempotency limits.

```yaml
splitr:
  consumer:
    enabled: true
  idempotency:
    max-size: 1000 # record size

```

---

## 🛠 Usage

### Step 1: Define Your Query

```java
public record OrderQuery(String orderId) { }

```

### Step 2: Implement the Handler (Consumer)

```java
@Component
public class OrderQueryHandler implements BaseQueryHandler<OrderQuery> {
    @Override
    public Object handle(OrderQuery q) {
        return "ORDER-DETAILS-" + q.orderId();
    }
}

```

### Step 3: Execute the Query (Publisher)

```java
@RestController
@RequiredArgsConstructor
public class OrderController {
    private final QueryBus queryBus;

    @GetMapping("/order/{id}")
    public String get(@PathVariable String id) {
        return queryBus.publishSync(new OrderQuery(id), String.class);
    }
}

```

---

## ⚙️ Configuration Properties

| Property                         | Default                          | Description                                          |
|----------------------------------|----------------------------------|------------------------------------------------------|
| `splitr.publisher.enabled`       | `false`                          | Enables QueryBus and Callback endpoint.              |
| `splitr.consumer.enabled`        | `false`                          | Enables Kafka listeners and Dispatcher.              |
| `splitr.domainevents.enabled`    | `false`                          | Enables domain events.                               |
| `splitr.callback-url`            | - (Required)                     | The HTTP endpoint for the publisher's callback.      |
| `splitr.bus.default-timeout`     | `10000`                          | Default wait time in ms for sync query and commands. |
| `splitr.bus.kafka.command.topic` | `tr.kontas.splitr.command.topic` | Kafka Command topic.                                 |
| `splitr.bus.kafka.query.topic`   | `tr.kontas.splitr.query.topic`   | Kafka query topic.                                   |
| `splitr.bus.kafka.event.topic`   | `tr.kontas.splitr.event.topic`   | Kafka event topic.                                   |

## 📑 Roadmap & TODO's

### 🚀 High Priority (Core Engine)

* [x] **Query Bus:** Distributed request-response pattern.
* [x] **Command Bus:** Asynchronous command dispatching.
* [x] **Event Bus:** Pub/Sub broadcast support for events over queue/bus.
  * *Note:* Fan-out pattern implementation. Multiple listeners for a single event with independent consumer groups.
* [x] **Domain Events InMemory Bus:** Pub/Sub broadcast support for domain events.
* [ ] **Dead Letter Queue (DLQ):** Automatic failure routing to `.DLT` topics for commands and events.
  * *Note:* Catch-all error handling in listeners to prevent infinite retry loops and partition blocking.
* [ ] **DLQ Retry Jobs:** Scheduled background jobs to consume from DLQ and re-publish to main topics.
  * *Customization:* User-defined **Cron Expressions** for retry intervals.
  * *Logic:* Smart back-off strategy; use `x-retry-count` headers to prevent "poison pill" messages from circulating forever.
* [ ] **Saga Support:** Orchestration-based distributed transaction management.
  * *Note:* State machine implementation to manage compensations (undo operations) when a step in the flow fails.

### 🛡 Resilience & Security

* [x] **Idempotency Guard:** Distributed store to prevent duplicate processing.
* [ ] **Circuit Breaker:** Resilience4j integration to protect the bus.
  * *Note:* Automatically trip the circuit if the `callback-url` or target microservice is down, preventing resource exhaustion.
* [ ] **Payload Encryption:** Optional AES encryption for sensitive Kafka record data.
  * *Note:* Field-level or full-body encryption to ensure PII (Personally Identifiable Information) security at rest in Kafka brokers.
* [ ] **Schema Validation:** JSR-303 (Hibernate Validator) support for incoming DTOs.
  * *Note:* Fail-fast mechanism; validate the command/query payload before it ever touches the Kafka topic.

### 📊 Observability

* [ ] **Distributed Tracing:** Micrometer/Brave/Zipkin integration.
  * *Note:* Propagation of `Span-ID` and `Trace-ID` across different services to visualize the entire request flow.
* [ ] **Metrics & Dashboards:** Micrometer-based Prometheus metrics.
  * *Note:* Real-time tracking of "Bus Throughput", "Average Response Latency", and "DLQ Error Rates".
* [ ] **Audit Log:** Persistent storage for all dispatched messages.
  * *Note:* A separate database or search index (Elasticsearch) to search historical commands and see who triggered what, when.

### 💾 Storage & Transports

* [x] **Kafka Transport:** Primary high-throughput transport layer.
* [ ] **RabbitMQ Transport:** AMQP-based alternative.
  * *Note:* Support for environments where lightweight broker logic and complex routing (Exchange types) are preferred.

---

## 🤝 Contributing

Developed and maintained by **BurakKontas**. Feel free to submit issues or pull requests to improve the bus performance or add new features.

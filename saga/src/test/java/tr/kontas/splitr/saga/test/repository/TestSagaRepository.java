package tr.kontas.splitr.saga.test.repository;

import org.springframework.data.jpa.repository.Query;
import tr.kontas.splitr.saga.repositories.SagaRepository;
import tr.kontas.splitr.saga.test.data.TestSagaState;

import java.util.Optional;

public interface TestSagaRepository extends SagaRepository<TestSagaState> {
    @Query("SELECT COUNT(s) FROM TestSagaState s WHERE s.currentState = 'Completed'")
    long countCompletedSagas();

    Optional<TestSagaState> findByOrderId(String orderId);
}

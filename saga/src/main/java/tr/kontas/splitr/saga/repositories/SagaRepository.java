package tr.kontas.splitr.saga.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import tr.kontas.splitr.saga.entities.abstracts.BaseSagaState;
import java.util.Optional;

@NoRepositoryBean
public interface SagaRepository<TState extends BaseSagaState> extends JpaRepository<TState, String> {
}
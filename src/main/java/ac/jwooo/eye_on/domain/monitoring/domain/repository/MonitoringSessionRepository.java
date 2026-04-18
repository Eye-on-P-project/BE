package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.util.Optional;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringSessionRepository extends JpaRepository<MonitoringSession, Long> {

    Optional<MonitoringSession> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUserIdAndEndedAtServerIsNullAndDeletedAtIsNull(Long userId);
}

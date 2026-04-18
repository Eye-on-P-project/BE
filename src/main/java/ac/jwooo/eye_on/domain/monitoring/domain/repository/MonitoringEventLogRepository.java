package ac.jwooo.eye_on.domain.monitoring.domain.repository;

import java.util.Optional;

import ac.jwooo.eye_on.domain.monitoring.domain.entity.MonitoringEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringEventLogRepository extends JpaRepository<MonitoringEventLog, Long> {

    Optional<MonitoringEventLog> findByIdAndSessionIdAndDeletedAtIsNull(Long id, Long sessionId);
}

package ac.jwooo.eye_on.domain.monitoring.domain.entity;

public enum NotificationType {
    DROWSY,
    SLEEP;

    public static NotificationType fromMonitoringEventType(MonitoringEventType eventType) {
        if (eventType == MonitoringEventType.DROWSY) {
            return DROWSY;
        }
        if (eventType == MonitoringEventType.SLEEP) {
            return SLEEP;
        }
        throw new IllegalArgumentException("알림으로 변환할 수 없는 이벤트 타입입니다: " + eventType);
    }
}

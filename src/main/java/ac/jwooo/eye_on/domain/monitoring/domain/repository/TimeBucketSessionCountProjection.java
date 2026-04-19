package ac.jwooo.eye_on.domain.monitoring.domain.repository;

public interface TimeBucketSessionCountProjection {

    Integer getYear();

    Integer getMonth();

    Integer getDay();

    Integer getHour();

    Long getSessionCount();
}

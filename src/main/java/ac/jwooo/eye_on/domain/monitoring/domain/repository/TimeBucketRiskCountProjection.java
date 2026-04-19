package ac.jwooo.eye_on.domain.monitoring.domain.repository;

public interface TimeBucketRiskCountProjection {

    Integer getYear();

    Integer getMonth();

    Integer getDay();

    Integer getHour();

    Long getDrowsyCount();

    Long getSleepCount();

    Long getTotalRiskCount();
}

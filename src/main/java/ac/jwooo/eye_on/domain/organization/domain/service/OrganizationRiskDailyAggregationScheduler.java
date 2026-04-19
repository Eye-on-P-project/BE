package ac.jwooo.eye_on.domain.organization.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationRiskDailyAggregationScheduler {

    private final OrganizationRiskDailyAggregationService organizationRiskDailyAggregationService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void aggregateDailyStats() {
        log.info("[OrganizationRiskDailyAggregationScheduler] Start daily aggregation for D-1 and D-2");
        organizationRiskDailyAggregationService.aggregateRecentDays(2);
        log.info("[OrganizationRiskDailyAggregationScheduler] Daily aggregation finished");
    }
}

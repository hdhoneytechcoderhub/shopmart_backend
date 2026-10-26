package com.shopmart.config.alert;

import com.shopmart.common.alert.AlertSender;
import com.shopmart.common.alert.AlertSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Wraps Spring Boot Actuator's own HealthEndpoint (DB, disk space, etc. — whatever
 * HealthIndicators are on the classpath) and alerts on transitions into a non-UP status,
 * rather than alerting on every poll while already down (which would spam the channel).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemHealthMonitor {

    private final HealthEndpoint healthEndpoint;
    private final AlertSender alertSender;

    private Status lastStatus = Status.UP;

    @Scheduled(fixedDelayString = "${app.alerts.health-poll-interval-ms:120000}") // every 2 min
    @SchedulerLock(name = "systemHealthMonitor", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void checkHealth() {
        Status current;
        try {
            current = healthEndpoint.health().getStatus();
        } catch (Exception e) {
            current = Status.DOWN;
            log.error("[HEALTH] health check itself failed: {}", e.getMessage());
        }

        if (!current.equals(lastStatus)) {
            if (Status.UP.equals(current)) {
                alertSender.send("System health recovered", "Health status returned to UP", AlertSeverity.INFO);
            } else {
                AlertSeverity severity = Status.DOWN.equals(current) ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
                alertSender.send("System health degraded", "Health status is now " + current, severity);
            }
        }
        lastStatus = current;
    }
}

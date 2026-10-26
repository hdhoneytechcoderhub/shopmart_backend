package com.shopmart.config.monitoring;

import com.shopmart.common.alert.AlertSender;
import com.shopmart.common.alert.AlertSeverity;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lightweight slow-query visibility built on Hibernate's own Statistics API
 * (spring.jpa.properties.hibernate.generate_statistics=true — see application.yml).
 *
 * This is NOT full query-level tracing (that needs a DB-side tool like Postgres'
 * pg_stat_statements, or a JDBC proxy like p6spy/datasource-proxy) — it's a cheap,
 * dependency-free early-warning signal: every polling interval, it logs the single
 * slowest query Hibernate has seen since the last poll and exposes it as a gauge so
 * it shows up in /actuator/prometheus. For real production query analysis, enable
 * pg_stat_statements on the Railway Postgres instance directly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlowQueryMonitor {

    private final EntityManagerFactory entityManagerFactory;
    private final MeterRegistry meterRegistry;
    private final AlertSender alertSender;

    @Value("${app-slow-query-threshold-ms:1000}")
    private long thresholdMs;

    private long lastMaxTime = -1;

    @Scheduled(fixedDelayString = "${app.monitoring.slow-query-poll-ms:60000}")
    @SchedulerLock(name = "slowQueryMonitor", lockAtMostFor = "PT2M", lockAtLeastFor = "PT30S")
    public void pollSlowestQuery() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        if (!stats.isStatisticsEnabled()) return;

        long maxTime = stats.getQueryExecutionMaxTime();
        String maxTimeQuery = stats.getQueryExecutionMaxTimeQueryString();

        meterRegistry.gauge("shopmart.db.query.max_execution_ms", maxTime);

        if (maxTime >= thresholdMs && maxTime != lastMaxTime) {
            log.warn("[SLOW-QUERY] {}ms (threshold {}ms) query={}", maxTime, thresholdMs, maxTimeQuery);
            alertSender.send("Slow query detected",
                    maxTime + "ms (threshold " + thresholdMs + "ms): " + maxTimeQuery,
                    AlertSeverity.WARNING);
        }
        lastMaxTime = maxTime;
    }
}

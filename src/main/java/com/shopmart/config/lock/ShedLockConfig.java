package com.shopmart.config.lock;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Distributed locking for @Scheduled jobs (Phase: "Distributed Locking"). Without this, every
 * Railway replica runs each @Scheduled method independently — fine at 1 replica (today), but
 * the moment this scales to >1, the payment reconciliation job, campaign sender, report
 * scheduler, and both monitors would all run duplicated on every instance simultaneously
 * (duplicate campaign sends, duplicate reconciliation corrections, redundant alerts).
 *
 * Uses the JDBC provider against the existing Postgres DataSource — no new infrastructure
 * (no Redis/ZooKeeper requirement) since the app already has a DB connection. The lock table
 * is created by Flyway (see V10__shedlock.sql).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }
}

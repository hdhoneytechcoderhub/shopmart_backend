package com.shopmart.module.order.repository;

import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("select coalesce(sum(o.total), 0) from Order o " +
           "where o.userId = :userId and o.paymentStatus = com.shopmart.module.order.entity.PaymentStatus.PAID")
    BigDecimal totalSpentByUser(@Param("userId") Long userId);

    long countByUserIdAndPaymentStatus(Long userId, PaymentStatus paymentStatus);

    Optional<Order> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    // ---- analytics (Phase 2) ----
    @Query("select coalesce(sum(o.total), 0) from Order o " +
           "where o.paymentStatus = com.shopmart.module.order.entity.PaymentStatus.PAID")
    BigDecimal totalRevenue();

    long countByStatus(OrderStatus status);

    long countByStatusIn(List<OrderStatus> statuses);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countGroupedByStatus();

    List<Order> findByCreatedAtGreaterThanEqual(Instant since);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    // ---- reports (Phase 3) ----
    List<Order> findByCreatedAtBetween(Instant from, Instant to);

    /** Phase 6 optimization: revenue + order count grouped by calendar month, computed in
     *  Postgres (date_trunc + GROUP BY) instead of pulling every order row into the JVM and
     *  grouping in Java. Used by growth trend / revenue forecast instead of findByCreatedAtBetween. */
    @Query(value = """
            select date_trunc('month', created_at) as bucketMonth,
                   coalesce(sum(case when payment_status = 'PAID' then total else 0 end), 0) as revenue,
                   count(*) as orderCount
            from orders
            where created_at >= :from
            group by 1
            order by 1
            """, nativeQuery = true)
    List<MonthlyOrderAggregate> monthlyOrderAggregates(@Param("from") Instant from);

    interface MonthlyOrderAggregate {
        java.sql.Timestamp getBucketMonth();
        BigDecimal getRevenue();
        Long getOrderCount();
    }

    /** Phase 6 optimization (2nd pass): cohort retention computed entirely in Postgres instead
     *  of loading every user and every order into the JVM and joining them in Java. One row per
     *  (cohort month, month offset) pair with the distinct count of that cohort's customers who
     *  placed a paid order in that offset month. Cohort *sizes* (denominator) come from the
     *  existing UserRepository.monthlyNewCustomers() query — same definition, no need to
     *  duplicate it here. */
    @Query(value = """
            select date_trunc('month', u.created_at) as cohortMonth,
                   cast(
                     (extract(year from date_trunc('month', o.created_at)) - extract(year from date_trunc('month', u.created_at))) * 12 +
                     (extract(month from date_trunc('month', o.created_at)) - extract(month from date_trunc('month', u.created_at)))
                     as integer
                   ) as monthOffset,
                   count(distinct o.user_id) as retainedCount
            from users u
            join user_roles ur on ur.user_id = u.id and ur.role = 'ROLE_CUSTOMER'
            join orders o on o.user_id = u.id and o.payment_status = 'PAID'
            where u.created_at >= :cohortFrom
            group by 1, 2
            having (extract(year from date_trunc('month', o.created_at)) - extract(year from date_trunc('month', u.created_at))) * 12 +
                   (extract(month from date_trunc('month', o.created_at)) - extract(month from date_trunc('month', u.created_at)))
                   between 0 and :maxOffset
            order by 1, 2
            """, nativeQuery = true)
    List<CohortRetentionRow> cohortRetention(@Param("cohortFrom") Instant cohortFrom, @Param("maxOffset") int maxOffset);

    interface CohortRetentionRow {
        java.sql.Timestamp getCohortMonth();
        Integer getMonthOffset();
        Long getRetainedCount();
    }

    @Query("""
            select o.userId, count(o), sum(o.total)
            from Order o
            where o.paymentStatus = com.shopmart.module.order.entity.PaymentStatus.PAID
              and o.createdAt between :from and :to
            group by o.userId order by sum(o.total) desc
            """)
    List<Object[]> topCustomers(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("select count(distinct o.userId) from Order o")
    long countDistinctCustomers();

    @Query("select o.userId from Order o group by o.userId having count(o) > 1")
    java.util.List<Long> repeatCustomerIds();
}

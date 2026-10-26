package com.shopmart.module.user.repository;

import com.shopmart.module.user.entity.Role;
import com.shopmart.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByCreatedAtAfter(java.time.Instant since);

    @Query("select count(u) from User u join u.roles r where r = :role")
    long countByRole(@Param("role") Role role);

    @Query("select distinct u from User u join u.roles r where r = :role")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);

    /** Phase 6 optimization: new-customer counts grouped by calendar month, computed in
     *  Postgres instead of loading every user into the JVM to group by YearMonth in Java. */
    @Query(value = """
            select date_trunc('month', u.created_at) as bucketMonth, count(*) as newCustomers
            from users u
            join user_roles ur on ur.user_id = u.id
            where ur.role = 'ROLE_CUSTOMER' and u.created_at >= :from
            group by 1
            order by 1
            """, nativeQuery = true)
    java.util.List<MonthlyNewCustomers> monthlyNewCustomers(@Param("from") java.time.Instant from);

    interface MonthlyNewCustomers {
        java.sql.Timestamp getBucketMonth();
        Long getNewCustomers();
    }
}

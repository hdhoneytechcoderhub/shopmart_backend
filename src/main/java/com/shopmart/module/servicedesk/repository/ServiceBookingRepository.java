package com.shopmart.module.servicedesk.repository;

import com.shopmart.module.servicedesk.entity.BookingStatus;
import com.shopmart.module.servicedesk.entity.ServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {
    List<ServiceBooking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ServiceBooking> findByEngineerIdOrderByCreatedAtDesc(Long engineerId);
    List<ServiceBooking> findByEngineerIdAndStatusOrderByCreatedAtDesc(Long engineerId, BookingStatus status);
    List<ServiceBooking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    List<ServiceBooking> findAllByOrderByCreatedAtDesc();

    long countByStatus(BookingStatus status);

    @Query("""
            select coalesce(sum(si.price), 0) from ServiceBooking b
            join com.shopmart.module.servicedesk.entity.ServiceItem si on si.id = b.serviceItemId
            where b.status = com.shopmart.module.servicedesk.entity.BookingStatus.COMPLETED
            """)
    java.math.BigDecimal completedRevenue();

    @Query("select avg(b.rating) from ServiceBooking b where b.rating is not null")
    Double averageRating();
}

package com.shopmart.module.servicedesk.repository;

import com.shopmart.module.servicedesk.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    List<ServiceItem> findByActiveTrue();
    List<ServiceItem> findByActiveTrueAndCategoryId(Long categoryId);
}

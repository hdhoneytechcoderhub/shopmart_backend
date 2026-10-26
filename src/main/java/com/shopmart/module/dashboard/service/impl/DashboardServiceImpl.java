package com.shopmart.module.dashboard.service.impl;

import com.shopmart.module.category.repository.CategoryRepository;
import com.shopmart.module.dashboard.dto.DashboardStatsResponse;
import com.shopmart.module.dashboard.service.DashboardService;
import com.shopmart.module.machine.repository.MachineRepository;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.product.repository.ProductRepository;
import com.shopmart.module.servicedesk.repository.ServiceItemRepository;
import com.shopmart.module.user.entity.Role;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MachineRepository machineRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        BigDecimal revenue = orderRepository.totalRevenue();
        return new DashboardStatsResponse(
                userRepository.countByRole(Role.ROLE_ADMIN),
                productRepository.count(),
                categoryRepository.count(),
                machineRepository.countByDeletedFalse(),
                serviceItemRepository.count(),
                orderRepository.count(),
                orderRepository.countDistinctCustomers(),
                revenue != null ? revenue : BigDecimal.ZERO
        );
    }
}

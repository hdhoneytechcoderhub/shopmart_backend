package com.shopmart.module.machine.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.machine.dto.MachineRequest;
import com.shopmart.module.machine.dto.MachineResponse;
import org.springframework.data.domain.Pageable;

public interface MachineService {
    PageResponse<MachineResponse> list(Pageable pageable);
    MachineResponse get(Long id);
    MachineResponse create(MachineRequest req);
    MachineResponse update(Long id, MachineRequest req);
    void delete(Long id);
}

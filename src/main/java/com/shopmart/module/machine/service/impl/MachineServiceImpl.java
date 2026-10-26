package com.shopmart.module.machine.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.audit.service.AuditService;
import com.shopmart.module.machine.dto.MachineRequest;
import com.shopmart.module.machine.dto.MachineResponse;
import com.shopmart.module.machine.entity.Machine;
import com.shopmart.module.machine.repository.MachineRepository;
import com.shopmart.module.machine.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class MachineServiceImpl implements MachineService {

    private final MachineRepository repository;
    private final AuditService auditService;

    @Override @Transactional(readOnly = true)
    public PageResponse<MachineResponse> list(Pageable pageable) {
        return PageResponse.from(repository.findByDeletedFalse(pageable).map(this::toResponse));
    }

    @Override @Transactional(readOnly = true)
    public MachineResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override @Transactional
    public MachineResponse create(MachineRequest req) {
        Machine m = new Machine();
        apply(m, req);
        return toResponse(repository.save(m));
    }

    @Override @Transactional
    public MachineResponse update(Long id, MachineRequest req) {
        Machine m = find(id);
        apply(m, req);
        return toResponse(repository.save(m));
    }

    @Override @Transactional
    public void delete(Long id) {
        Machine m = find(id);
        m.setDeleted(true);               // soft delete
        repository.save(m);
        auditService.log(null, "MACHINE_DELETED", "Machine", m.getId(), m.getName());
    }

    private void apply(Machine m, MachineRequest req) {
        m.setName(req.name());
        m.setModelNumber(req.modelNumber());
        m.setBrand(req.brand());
        m.setDescription(req.description());
        m.setImages(req.images() == null ? new ArrayList<>() : req.images());
    }

    private Machine find(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + id));
    }

    private MachineResponse toResponse(Machine m) {
        return new MachineResponse(m.getId(), m.getName(), m.getModelNumber(),
                m.getBrand(), m.getDescription(), m.getImages());
    }
}

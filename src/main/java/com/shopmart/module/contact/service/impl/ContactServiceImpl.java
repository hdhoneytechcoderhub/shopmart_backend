package com.shopmart.module.contact.service.impl;

import com.shopmart.module.contact.dto.ContactRequest;
import com.shopmart.module.contact.dto.ContactResponse;
import com.shopmart.module.contact.entity.ContactMessage;
import com.shopmart.module.contact.repository.ContactMessageRepository;
import com.shopmart.module.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMessageRepository repository;

    @Override
    @Transactional
    public ContactResponse submit(ContactRequest req) {
        ContactMessage m = new ContactMessage();
        m.setName(req.name());
        m.setEmail(req.email());
        m.setPhone(req.phone());
        m.setSubject(req.subject());
        m.setMessage(req.message());
        return toResponse(repository.save(m));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private ContactResponse toResponse(ContactMessage m) {
        return new ContactResponse(m.getId(), m.getName(), m.getEmail(), m.getPhone(),
                m.getSubject(), m.getMessage(), m.isHandled(), m.getCreatedAt());
    }
}

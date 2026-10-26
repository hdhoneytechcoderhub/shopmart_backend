package com.shopmart.module.contact.service;

import com.shopmart.module.contact.dto.ContactRequest;
import com.shopmart.module.contact.dto.ContactResponse;

import java.util.List;

public interface ContactService {
    ContactResponse submit(ContactRequest request);
    List<ContactResponse> list();
}

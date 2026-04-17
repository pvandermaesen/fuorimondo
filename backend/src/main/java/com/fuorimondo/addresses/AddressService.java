package com.fuorimondo.addresses;

import com.fuorimondo.addresses.dto.AddressRequest;
import com.fuorimondo.users.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Address> listForUser(UUID userId) {
        return addressRepository.findByUserId(userId);
    }

    @Transactional
    public Address create(UUID userId, AddressRequest req) {
        Address a = new Address();
        a.setUser(userRepository.findById(userId).orElseThrow());
        apply(a, req);
        if (req.isDefault()) unsetOtherDefaults(userId, req.type(), null);
        a.setDefault(req.isDefault());
        return addressRepository.save(a);
    }

    @Transactional
    public Address update(UUID userId, UUID addressId, AddressRequest req) {
        Address a = fetchOwned(userId, addressId);
        apply(a, req);
        if (req.isDefault()) unsetOtherDefaults(userId, req.type(), addressId);
        a.setDefault(req.isDefault());
        return a;
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address a = fetchOwned(userId, addressId);
        addressRepository.delete(a);
    }

    private Address fetchOwned(UUID userId, UUID addressId) {
        Address a = addressRepository.findById(addressId).orElseThrow();
        if (!a.getUser().getId().equals(userId)) throw new AccessDeniedException("not owner");
        return a;
    }

    private void unsetOtherDefaults(UUID userId, AddressType type, UUID excludeId) {
        for (Address other : addressRepository.findByUserIdAndType(userId, type)) {
            if (excludeId != null && other.getId().equals(excludeId)) continue;
            other.setDefault(false);
        }
    }

    private void apply(Address a, AddressRequest req) {
        a.setType(req.type());
        a.setFullName(req.fullName());
        a.setStreet(req.street());
        a.setStreetExtra(req.streetExtra());
        a.setPostalCode(req.postalCode());
        a.setCity(req.city());
        a.setCountry(req.country());
    }
}

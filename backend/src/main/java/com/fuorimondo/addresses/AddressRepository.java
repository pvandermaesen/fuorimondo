package com.fuorimondo.addresses;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdAndType(UUID userId, AddressType type);

    List<Address> findByUserId(UUID userId);
}

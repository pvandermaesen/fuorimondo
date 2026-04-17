package com.fuorimondo.addresses.dto;

import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressType;

import java.util.UUID;

public record AddressResponse(
    UUID id,
    AddressType type,
    String fullName,
    String street,
    String streetExtra,
    String postalCode,
    String city,
    String country,
    boolean isDefault
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getType(), a.getFullName(), a.getStreet(),
            a.getStreetExtra(), a.getPostalCode(), a.getCity(), a.getCountry(), a.isDefault());
    }
}

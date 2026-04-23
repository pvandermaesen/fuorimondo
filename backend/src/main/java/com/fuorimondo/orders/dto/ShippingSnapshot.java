package com.fuorimondo.orders.dto;

import com.fuorimondo.addresses.Address;

public record ShippingSnapshot(
    String fullName,
    String street,
    String streetExtra,
    String postalCode,
    String city,
    String country
) {
    public static ShippingSnapshot from(Address a) {
        return new ShippingSnapshot(
            a.getFullName(),
            a.getStreet(),
            a.getStreetExtra(),
            a.getPostalCode(),
            a.getCity(),
            a.getCountry()
        );
    }
}

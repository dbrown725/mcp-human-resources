package com.megacorp.humanresources.service;

import java.util.List;

import com.megacorp.humanresources.entity.Address;

public interface AddressService {
    Address saveAddress(Address address);

    Address getAddressById(Long addressId);

    void deleteAddressById(Long addressId);

    List<Address> fetchAddressList();

    List<Address> searchAddresses(String city, String state, String postalCode, Boolean isRemote);
}

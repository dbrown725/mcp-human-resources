package com.megacorp.humanresources.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.megacorp.humanresources.entity.Address;
import com.megacorp.humanresources.service.AddressService;

import jakarta.validation.Valid;

@RestController
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping("/addresses")
    public Address saveAddress(@Valid @RequestBody Address address) {
        return addressService.saveAddress(address);
    }

    @GetMapping("/addresses/{id}")
    public Address addressById(@PathVariable("id") Long addressId) {
        return addressService.getAddressById(addressId);
    }

    @DeleteMapping("/addresses/{id}")
    public String deleteAddressById(@PathVariable("id") Long addressId) {
        addressService.deleteAddressById(addressId);
        return "Deleted Successfully";
    }

    @GetMapping("/addresses")
    public List<Address> searchAddresses(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) Boolean isRemote) {
        return addressService.searchAddresses(city, state, postalCode, isRemote);
    }
}

package com.megacorp.humanresources.service;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.megacorp.humanresources.entity.Address;
import com.megacorp.humanresources.exceptions.ResourceNotFoundException;
import com.megacorp.humanresources.repository.AddressRepository;
import com.megacorp.humanresources.specifications.AddressSpecifications;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    @Tool(name = "save_address", description = "Creates a new address based on the passed address object.")
    public Address saveAddress(Address address) {
        address.setAddressId(null);
        return addressRepository.save(address);
    }

    @Override
    @Tool(name = "get_address_with_id", description = "Get a single address by ID")
    public Address getAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    @Override
    @Tool(name = "delete_address_with_id", description = "Delete a single address by ID")
    public void deleteAddressById(Long addressId) {
        if (!addressRepository.existsById(addressId)) {
            throw new ResourceNotFoundException("Address", addressId);
        }
        addressRepository.deleteById(addressId);
    }

    @Override
    @Tool(name = "fetch_address_list", description = "Get a list of all addresses")
    public List<Address> fetchAddressList() {
        return addressRepository.findAll();
    }

    @Override
    @Tool(name = "search_addresses", description = "Search addresses by optional city, state, postalCode, and isRemote.")
    public List<Address> searchAddresses(
            @ToolParam(required = false) String city,
            @ToolParam(required = false) String state,
            @ToolParam(required = false) String postalCode,
            @ToolParam(required = false) Boolean isRemote) {

        Specification<Address> specification = Specification.where(null);

        if (city != null && !city.isEmpty()) {
            specification = specification.and(AddressSpecifications.hasCity(city));
        }
        if (state != null && !state.isEmpty()) {
            specification = specification.and(AddressSpecifications.hasState(state));
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            specification = specification.and(AddressSpecifications.hasPostalCode(postalCode));
        }
        if (isRemote != null) {
            specification = specification.and(AddressSpecifications.isRemote(isRemote));
        }

        return addressRepository.findAll(specification);
    }
}

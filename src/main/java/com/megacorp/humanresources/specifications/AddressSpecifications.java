package com.megacorp.humanresources.specifications;

import org.springframework.data.jpa.domain.Specification;

import com.megacorp.humanresources.entity.Address;

public class AddressSpecifications {

    public static Specification<Address> hasCity(String city) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("city"), city);
    }

    public static Specification<Address> hasState(String state) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("state"), state);
    }

    public static Specification<Address> hasPostalCode(String postalCode) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("postalCode"), postalCode);
    }

    public static Specification<Address> isRemote(Boolean isRemote) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isRemote"), isRemote);
    }
}

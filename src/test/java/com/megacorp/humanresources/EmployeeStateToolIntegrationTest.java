package com.megacorp.humanresources;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.megacorp.humanresources.model.EmployeeCount;
import com.megacorp.humanresources.service.EmployeeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmployeeStateToolIntegrationTest {

    @Autowired
    private EmployeeServiceImpl employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void countEmployeesInStateAcceptsFullStateName() {
        EmployeeCount count = employeeService.countEmployeesInState("Georgia");
        assertThat(count).isNotNull();
        assertThat(count.getCount()).isNotNull();
        assertThat(count.getCount()).isGreaterThan(0L);
    }

    @Test
    void searchEmployeesInStateAcceptsFullStateName() throws Exception {
        String json = employeeService.searchEmployeesInState("Georgia", 1, 10, "employeeId", "asc");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("totalElements").asInt()).isGreaterThan(0);
        assertThat(root.path("content").isArray()).isTrue();
    }

    @Test
    void countEmployeesInCityReturnsMatches() {
        EmployeeCount count = employeeService.countEmployeesInCity("Atlanta");
        assertThat(count).isNotNull();
        assertThat(count.getCount()).isNotNull();
        assertThat(count.getCount()).isGreaterThan(0L);
    }

    @Test
    void searchEmployeesInCityReturnsMatches() throws Exception {
        String json = employeeService.searchEmployeesInCity("Atlanta", 1, 10, "employeeId", "asc");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("totalElements").asInt()).isGreaterThan(0);
        assertThat(root.path("content").isArray()).isTrue();
    }

    @Test
    void countEmployeesInZipcodeReturnsMatches() {
        EmployeeCount count = employeeService.countEmployeesInZipcode("33101");
        assertThat(count).isNotNull();
        assertThat(count.getCount()).isNotNull();
        assertThat(count.getCount()).isGreaterThan(0L);
    }

    @Test
    void searchEmployeesInZipcodeReturnsMatches() throws Exception {
        String json = employeeService.searchEmployeesInZipcode("33101", 1, 10, "employeeId", "asc");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("totalElements").asInt()).isGreaterThan(0);
        assertThat(root.path("content").isArray()).isTrue();
    }
}

package com.megacorp.humanresources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeSearchDateFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchByExactTerminationDateReturnsMatchingEmployee() throws Exception {
        MvcResult result = mockMvc.perform(get("/employees/search")
                .param("terminationDate", "2018-09-29"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(root.path("totalElements").asInt()).isGreaterThan(0);
        assertThat(root.path("content").isArray()).isTrue();

        boolean containsEmployee5000 = false;
        for (JsonNode employee : root.path("content")) {
            if (employee.path("employeeId").asLong() == 5000L) {
                containsEmployee5000 = true;
                break;
            }
        }

        assertThat(containsEmployee5000).isTrue();
    }

    @Test
    void searchByExactHireDateReturnsMatchingEmployee() throws Exception {
        MvcResult result = mockMvc.perform(get("/employees/search")
                .param("hireDate", "2017-09-11"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(root.path("totalElements").asInt()).isGreaterThan(0);
        assertThat(root.path("content").isArray()).isTrue();

        boolean containsEmployee5000 = false;
        for (JsonNode employee : root.path("content")) {
            if (employee.path("employeeId").asLong() == 5000L) {
                containsEmployee5000 = true;
                break;
            }
        }

        assertThat(containsEmployee5000).isTrue();
    }
}

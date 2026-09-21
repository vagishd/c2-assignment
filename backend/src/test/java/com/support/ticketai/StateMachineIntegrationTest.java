package com.support.ticketai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.ticketai.support.TestAiConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification of the ticket state machine enforced by the backend (NFR-4).
 * Valid transitions succeed; invalid ones return 409 INVALID_TRANSITION and leave state unchanged.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAiConfig.class)
class StateMachineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long createOpenTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Test ticket","description":"desc","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.ticketRef").exists())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void transition(long id, String target, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/tickets/" + id + "/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"" + target + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void validPath_openToClosed_succeeds() throws Exception {
        long id = createOpenTicket();
        transition(id, "IN_PROGRESS", 200);
        transition(id, "RESOLVED", 200);
        transition(id, "CLOSED", 200);

        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void openToCancelled_succeeds() throws Exception {
        long id = createOpenTicket();
        transition(id, "CANCELLED", 200);
    }

    @Test
    void inProgressToCancelled_succeeds() throws Exception {
        long id = createOpenTicket();
        transition(id, "IN_PROGRESS", 200);
        transition(id, "CANCELLED", 200);
    }

    @Test
    void invalidTransition_openToClosed_isRejectedWith409_andStateUnchanged() throws Exception {
        long id = createOpenTicket();

        mockMvc.perform(post("/api/tickets/" + id + "/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        // State must be unchanged.
        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void invalidTransition_closedToOpen_isRejectedWith409() throws Exception {
        long id = createOpenTicket();
        transition(id, "IN_PROGRESS", 200);
        transition(id, "RESOLVED", 200);
        transition(id, "CLOSED", 200);

        transition(id, "OPEN", 409);
    }

    @Test
    void invalidTransition_resolvedToOpen_isRejectedWith409() throws Exception {
        long id = createOpenTicket();
        transition(id, "IN_PROGRESS", 200);
        transition(id, "RESOLVED", 200);
        transition(id, "OPEN", 409);
    }

    @Test
    void createTicket_blankTitle_returns400WithValidationError() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"x\",\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void search_and_statusFilter_work() throws Exception {
        long id = createOpenTicket();
        assertThat(id).isPositive();

        mockMvc.perform(get("/api/tickets").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/tickets").param("q", "Test ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test ticket"));
    }
}

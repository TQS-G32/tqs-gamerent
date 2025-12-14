package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.Dispute;
import gamerent.data.DisputeReason;
import gamerent.data.DisputeStatus;
import gamerent.service.DisputeService;
import gamerent.config.DisputeValidationException;
import gamerent.config.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DisputeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("DISPUTE-API")
class DisputeControllerTest {

    @MockBean
    private DisputeService disputeService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;
    private MockHttpSession adminSession;
    private Dispute dispute;

    private static final String ADMIN = "ADMIN";
    private static final String BOOKID = "bookingId";
    private static final String REASON = "reason";
    private static final String DAMAGED_ITEM = "DAMAGED_ITEM";
    private static final String DESCRIPTION = "description";

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute("userId", 2L);
        session.setAttribute("userRole", "USER");

        adminSession = new MockHttpSession();
        adminSession.setAttribute("userId", 1L);
        adminSession.setAttribute("userRole", ADMIN);

        dispute = new Dispute();
        dispute.setId(1L);
        dispute.setBookingId(100L);
        dispute.setReporterId(2L);
        dispute.setReason(DisputeReason.DAMAGED_ITEM);
        dispute.setDescription("Test dispute");
        dispute.setStatus(DisputeStatus.SUBMITTED);
        dispute.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-1")
    @Tag("unit")
    void createDispute_ShouldReturnDispute_WhenValid() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, 100,
            REASON, DAMAGED_ITEM,
            DESCRIPTION, "Item was damaged during rental"
        );

        when(disputeService.createDispute(anyLong(), anyLong(), any(DisputeReason.class), 
            anyString(), any())).thenReturn(dispute);

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reason").value(DAMAGED_ITEM))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @XrayTest(key = "DISPUTE-API-2")
    @Tag("unit")
    void createDispute_ShouldReturn400_WhenReasonMissing() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, 100,
            DESCRIPTION, "Test"
        );

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-3")
    @Tag("unit")
    void createDispute_ShouldReturn400_WhenReasonInvalid() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, 100,
            REASON, "INVALID_REASON",
            DESCRIPTION, "Test"
        );

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-4")
    @Tag("unit")
    void createDispute_ShouldReturn404_WhenBookingNotFound() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, 999,
            REASON, DAMAGED_ITEM,
            DESCRIPTION, "Test"
        );

        when(disputeService.createDispute(anyLong(), anyLong(), any(DisputeReason.class), 
            anyString(), any())).thenThrow(new NoSuchElementException("Booking not found"));

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-5")
    @Tag("unit")
    void createDispute_ShouldReturn403_WhenUnauthorized() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, 100,
            REASON, DAMAGED_ITEM,
            DESCRIPTION, "Test"
        );

        when(disputeService.createDispute(anyLong(), anyLong(), any(DisputeReason.class), 
            anyString(), any())).thenThrow(new UnauthorizedException("Not authorized"));

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-6")
    @Tag("unit")
    void createDispute_ShouldReturn400_WhenValidationFails() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, 100,
            REASON, "OTHER",
            DESCRIPTION, ""
        );

        when(disputeService.createDispute(anyLong(), anyLong(), any(DisputeReason.class), 
            anyString(), any())).thenThrow(new DisputeValidationException("Description required"));

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-7")
    @Tag("unit")
    void getMyDisputes_ShouldReturnList_WhenUser() throws Exception {
        when(disputeService.getUserDisputes(2L, "USER")).thenReturn(List.of(dispute));

        mockMvc.perform(get("/api/disputes/my-disputes")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].reason").value(DAMAGED_ITEM));
    }

    @Test
    @XrayTest(key = "DISPUTE-API-8")
    @Tag("unit")
    void getMyDisputes_ShouldReturnAll_WhenAdmin() throws Exception {
        when(disputeService.getUserDisputes(1L, ADMIN)).thenReturn(List.of(dispute));

        mockMvc.perform(get("/api/disputes/my-disputes")
                .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @XrayTest(key = "DISPUTE-API-9")
    @Tag("unit")
    void getDisputeById_ShouldReturnDispute_WhenAuthorized() throws Exception {
        when(disputeService.getDisputeById(1L, 2L, "USER")).thenReturn(dispute);

        mockMvc.perform(get("/api/disputes/1")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingId").value(100));
    }

    @Test
    @XrayTest(key = "DISPUTE-API-10")
    @Tag("unit")
    void getDisputeById_ShouldReturn404_WhenNotFound() throws Exception {
        when(disputeService.getDisputeById(999L, 2L, "USER"))
            .thenThrow(new NoSuchElementException("Dispute not found"));

        mockMvc.perform(get("/api/disputes/999")
                .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-11")
    @Tag("unit")
    void getDisputeById_ShouldReturn403_WhenUnauthorized() throws Exception {
        when(disputeService.getDisputeById(1L, 2L, "USER"))
            .thenThrow(new UnauthorizedException("Not authorized"));

        mockMvc.perform(get("/api/disputes/1")
                .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-12")
    @Tag("unit")
    void getDisputesByBooking_ShouldReturnList() throws Exception {
        when(disputeService.getDisputesByBooking(100L, 2L, "USER")).thenReturn(List.of(dispute));

        mockMvc.perform(get("/api/disputes/booking/100")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(100));
    }

    @Test
    @XrayTest(key = "DISPUTE-API-13")
    @Tag("unit")
    void updateDisputeStatus_ShouldSucceed_WhenAdmin() throws Exception {
        Map<String, String> payload = Map.of(
            "status", "RESOLVED",
            "adminNotes", "Issue has been resolved"
        );

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setAdminNotes("Issue has been resolved");

        when(disputeService.updateDisputeStatus(1L, DisputeStatus.RESOLVED, 
            "Issue has been resolved", ADMIN)).thenReturn(dispute);

        mockMvc.perform(put("/api/disputes/1/status")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminNotes").value("Issue has been resolved"));
    }

    @Test
    @XrayTest(key = "DISPUTE-API-14")
    @Tag("unit")
    void updateDisputeStatus_ShouldReturn403_WhenNotAdmin() throws Exception {
        Map<String, String> payload = Map.of(
            "status", "RESOLVED"
        );

        when(disputeService.updateDisputeStatus(anyLong(), any(DisputeStatus.class), 
            any(), eq("USER"))).thenThrow(new UnauthorizedException("Only admins can update"));

        mockMvc.perform(put("/api/disputes/1/status")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-15")
    @Tag("unit")
    void updateDisputeStatus_ShouldReturn400_WhenStatusMissing() throws Exception {
        Map<String, String> payload = Map.of("adminNotes", "Test");

        mockMvc.perform(put("/api/disputes/1/status")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-16")
    @Tag("unit")
    void updateDisputeStatus_ShouldReturn400_WhenStatusInvalid() throws Exception {
        Map<String, String> payload = Map.of("status", "INVALID_STATUS");

        mockMvc.perform(put("/api/disputes/1/status")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-17")
    @Tag("unit")
    void updateDisputeStatus_ShouldReturn404_WhenDisputeNotFound() throws Exception {
        Map<String, String> payload = Map.of("status", "RESOLVED");

        when(disputeService.updateDisputeStatus(eq(999L), any(DisputeStatus.class), 
            any(), eq(ADMIN))).thenThrow(new NoSuchElementException("Dispute not found"));

        mockMvc.perform(put("/api/disputes/999/status")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-18")
    @Tag("unit")
    void createDispute_ShouldHandleIntegerId_InPayload() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, 100,
            REASON, "NO_SHOW",
            DESCRIPTION, "Renter did not show up"
        );

        when(disputeService.createDispute(anyLong(), eq(100L), eq(DisputeReason.NO_SHOW), 
            anyString(), any())).thenReturn(dispute);

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-19")
    @Tag("unit")
    void createDispute_ShouldReturn400_WhenBookingIdInvalid() throws Exception {
        Map<String, Object> payload = Map.of(
            BOOKID, "invalid",
            REASON, DAMAGED_ITEM
        );

        mockMvc.perform(post("/api/disputes")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @XrayTest(key = "DISPUTE-API-20")
    @Tag("unit")
    void getMyDisputes_ShouldReturn500_WhenServiceError() throws Exception {
        when(disputeService.getUserDisputes(anyLong(), anyString()))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/disputes/my-disputes")
                .session(session))
                .andExpect(status().isInternalServerError());
    }
}

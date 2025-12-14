package gamerent.boundary;

import gamerent.data.Dispute;
import gamerent.data.DisputeReason;
import gamerent.data.DisputeStatus;
import gamerent.service.DisputeService;
import gamerent.config.DisputeValidationException;
import gamerent.config.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/disputes")
@CrossOrigin(origins = "*")
public class DisputeController {
    private static final Logger logger = Logger.getLogger(DisputeController.class.getName());
    private static final String USER_ID_KEY = "userId";
    private static final String USER_ROLE_KEY = "userRole";

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public Dispute createDispute(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Long userId = resolveUserId(request, payload);
        Long bookingId = getLongValue(payload, "bookingId");
        String reasonStr = (String) payload.get("reason");
        String description = (String) payload.get("description");
        String evidenceUrls = (String) payload.get("evidenceUrls");

        if (reasonStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason is required");
        }

        DisputeReason reason;
        try {
            reason = DisputeReason.valueOf(reasonStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dispute reason");
        }

        logger.log(Level.INFO, "Dispute creation attempt - User: {0}, Booking: {1}, Reason: {2}", 
            new Object[]{userId, bookingId, reason});

        try {
            Dispute dispute = disputeService.createDispute(userId, bookingId, reason, description, evidenceUrls);
            logger.log(Level.INFO, "Dispute created successfully - ID: {0}", dispute.getId());
            return dispute;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (DisputeValidationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/my-disputes")
    public List<Dispute> getMyDisputes(HttpServletRequest request) {
        Long userId = resolveUserId(request, null);
        String userRole = resolveUserRole(request);

        try {
            return disputeService.getUserDisputes(userId, userRole);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error fetching user disputes - {0}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching disputes");
        }
    }

    @GetMapping("/{id}")
    public Dispute getDisputeById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = resolveUserId(request, null);
        String userRole = resolveUserRole(request);

        try {
            return disputeService.getDisputeById(id, userId, userRole);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @GetMapping("/booking/{bookingId}")
    public List<Dispute> getDisputesByBooking(@PathVariable Long bookingId, HttpServletRequest request) {
        Long userId = resolveUserId(request, null);
        String userRole = resolveUserRole(request);

        try {
            return disputeService.getDisputesByBooking(bookingId, userId, userRole);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Dispute updateDisputeStatus(@PathVariable Long id, 
                                      @RequestBody Map<String, String> payload,
                                      HttpServletRequest request) {
        String userRole = resolveUserRole(request);
        String statusStr = payload.get("status");
        String adminNotes = payload.get("adminNotes");

        if (statusStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        DisputeStatus status;
        try {
            status = DisputeStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dispute status");
        }

        logger.log(Level.INFO, "Dispute status update attempt - ID: {0}, New Status: {1}, Role: {2}", 
            new Object[]{id, status, userRole});

        try {
            Dispute updated = disputeService.updateDisputeStatus(id, status, adminNotes, userRole);
            logger.log(Level.INFO, "Dispute status updated successfully - ID: {0}, Status: {1}", 
                new Object[]{id, status});
            return updated;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    private Long resolveUserId(HttpServletRequest request, Map<String, Object> payload) {
        Object uid = request.getSession(false) != null ? 
            request.getSession(false).getAttribute(USER_ID_KEY) : null;
        
        if (uid instanceof Long longValue) return longValue;
        if (uid instanceof Integer intValue) return intValue.longValue();
        
        if (payload != null && payload.containsKey(USER_ID_KEY)) {
            return getLongValue(payload, USER_ID_KEY);
        }
        
        return 1L; // Default fallback
    }

    private String resolveUserRole(HttpServletRequest request) {
        Object role = request.getSession(false) != null ? 
            request.getSession(false).getAttribute(USER_ROLE_KEY) : null;
        
        return role != null ? role.toString() : "RENTER";
    }

    private Long getLongValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " must be a valid number");
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " is required");
    }
}

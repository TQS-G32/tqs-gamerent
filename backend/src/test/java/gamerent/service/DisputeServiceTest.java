package gamerent.service;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import gamerent.data.*;
import gamerent.config.DisputeValidationException;
import gamerent.config.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Requirement("DISPUTE-FEATURE")
class DisputeServiceTest {
    
    @Mock
    private DisputeRepository disputeRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private ItemRepository itemRepository;
    
    @InjectMocks
    private DisputeService disputeService;
    
    private User owner;
    private User renter;
    private Item item;
    private BookingRequest booking;
    private Dispute dispute;
    
    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Owner");
        owner.setEmail("owner@test.com");
        
        renter = new User();
        renter.setId(2L);
        renter.setName("Renter");
        renter.setEmail("renter@test.com");
        
        item = new Item();
        item.setId(1L);
        item.setName("Test Item");
        item.setOwner(owner);
        
        booking = new BookingRequest();
        booking.setId(1L);
        booking.setItemId(1L);
        booking.setUserId(2L);
        booking.setStartDate(LocalDate.now().minusDays(2));
        booking.setEndDate(LocalDate.now().plusDays(2));
        
        dispute = new Dispute();
        dispute.setId(1L);
        dispute.setBookingId(1L);
        dispute.setReporterId(2L);
        dispute.setReason(DisputeReason.DAMAGED_ITEM);
        dispute.setDescription("Item was damaged");
        dispute.setStatus(DisputeStatus.SUBMITTED);
        dispute.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-1")
    @Tag("unit")
    void createDispute_ShouldSucceed_WhenRenterReportsIssue() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
            Dispute d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        
        Dispute result = disputeService.createDispute(2L, 1L, DisputeReason.DAMAGED_ITEM, 
            "Item was damaged", null);
        
        assertNotNull(result);
        assertEquals(DisputeReason.DAMAGED_ITEM, result.getReason());
        verify(disputeRepository, times(1)).save(any(Dispute.class));
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-2")
    @Tag("unit")
    void createDispute_ShouldSucceed_WhenOwnerReportsIssue() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
            Dispute d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        
        Dispute result = disputeService.createDispute(1L, 1L, DisputeReason.NO_SHOW, 
            "Renter did not show up", null);
        
        assertNotNull(result);
        assertEquals(DisputeReason.NO_SHOW, result.getReason());
        verify(disputeRepository, times(1)).save(any(Dispute.class));
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-3")
    @Tag("unit")
    void createDispute_ShouldThrowUnauthorized_WhenUserNotInvolved() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(UnauthorizedException.class, () -> 
            disputeService.createDispute(999L, 1L, DisputeReason.DAMAGED_ITEM, 
                "Attempted report", null)
        );
        
        verify(disputeRepository, never()).save(any());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-4")
    @Tag("unit")
    void createDispute_ShouldThrowNotFound_WhenBookingDoesNotExist() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(NoSuchElementException.class, () -> 
            disputeService.createDispute(2L, 1L, DisputeReason.DAMAGED_ITEM, 
                "Report", null)
        );
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-5")
    @Tag("unit")
    void createDispute_ShouldThrowValidation_WhenBookingTooOld() {
        booking.setEndDate(LocalDate.now().minusDays(31));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(DisputeValidationException.class, () -> 
            disputeService.createDispute(2L, 1L, DisputeReason.DAMAGED_ITEM, 
                "Report", null)
        );
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-6")
    @Tag("unit")
    void createDispute_ShouldThrowValidation_WhenReasonIsNull() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(DisputeValidationException.class, () -> 
            disputeService.createDispute(2L, 1L, null, "Report", null)
        );
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-7")
    @Tag("unit")
    void createDispute_ShouldThrowValidation_WhenOtherReasonWithoutDescription() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(DisputeValidationException.class, () -> 
            disputeService.createDispute(2L, 1L, DisputeReason.OTHER, null, null)
        );
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-8")
    @Tag("unit")
    void createDispute_ShouldThrowValidation_WhenDescriptionTooLong() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        String longDescription = "x".repeat(501);
        
        assertThrows(DisputeValidationException.class, () -> 
            disputeService.createDispute(2L, 1L, DisputeReason.OTHER, longDescription, null)
        );
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-9")
    @Tag("unit")
    void updateDisputeStatus_ShouldSucceed_WhenAdmin() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
        
        Dispute result = disputeService.updateDisputeStatus(1L, DisputeStatus.RESOLVED, 
            "Issue resolved", "ADMIN");
        
        assertNotNull(result);
        assertEquals(DisputeStatus.RESOLVED, result.getStatus());
        assertEquals("Issue resolved", result.getAdminNotes());
        assertNotNull(result.getUpdatedAt());
        verify(disputeRepository, times(1)).save(any(Dispute.class));
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-10")
    @Tag("unit")
    void updateDisputeStatus_ShouldThrowUnauthorized_WhenNotAdmin() {
        assertThrows(UnauthorizedException.class, () -> 
            disputeService.updateDisputeStatus(1L, DisputeStatus.RESOLVED, 
                "Attempt", "USER")
        );
        
        verify(disputeRepository, never()).save(any());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-11")
    @Tag("unit")
    void getUserDisputes_ShouldReturnAll_WhenAdmin() {
        List<Dispute> allDisputes = List.of(dispute);
        when(disputeRepository.findAll()).thenReturn(allDisputes);
        
        List<Dispute> result = disputeService.getUserDisputes(1L, "ADMIN");
        
        assertEquals(1, result.size());
        verify(disputeRepository, times(1)).findAll();
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-12")
    @Tag("unit")
    void getUserDisputes_ShouldFilterByUser_WhenNotAdmin() {
        when(bookingRepository.findByUserId(2L)).thenReturn(List.of(booking));
        when(itemRepository.findByOwnerId(2L)).thenReturn(new ArrayList<>());
        when(disputeRepository.findAll()).thenReturn(List.of(dispute));
        
        List<Dispute> result = disputeService.getUserDisputes(2L, "USER");
        
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getReporterId());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-13")
    @Tag("unit")
    void getDisputeById_ShouldReturnDispute_WhenAdmin() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        
        Dispute result = disputeService.getDisputeById(1L, 999L, "ADMIN");
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-14")
    @Tag("unit")
    void getDisputeById_ShouldReturnDispute_WhenRenter() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        Dispute result = disputeService.getDisputeById(1L, 2L, "USER");
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-15")
    @Tag("unit")
    void getDisputeById_ShouldReturnDispute_WhenOwner() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        Dispute result = disputeService.getDisputeById(1L, 1L, "USER");
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-16")
    @Tag("unit")
    void getDisputeById_ShouldThrowUnauthorized_WhenUserNotInvolved() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(UnauthorizedException.class, () -> 
            disputeService.getDisputeById(1L, 999L, "USER")
        );
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-17")
    @Tag("unit")
    void getDisputesByBooking_ShouldReturnDisputes_WhenAdmin() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(disputeRepository.findByBookingId(1L)).thenReturn(List.of(dispute));
        
        List<Dispute> result = disputeService.getDisputesByBooking(1L, 999L, "ADMIN");
        
        assertEquals(1, result.size());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-18")
    @Tag("unit")
    void getDisputesByBooking_ShouldReturnDisputes_WhenRenter() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(disputeRepository.findByBookingId(1L)).thenReturn(List.of(dispute));
        
        List<Dispute> result = disputeService.getDisputesByBooking(1L, 2L, "USER");
        
        assertEquals(1, result.size());
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-19")
    @Tag("unit")
    void getDisputesByBooking_ShouldThrowUnauthorized_WhenUserNotInvolved() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(UnauthorizedException.class, () -> 
            disputeService.getDisputesByBooking(1L, 999L, "USER")
        );
    }
    
    @Test
    @XrayTest(key = "DISPUTE-SERVICE-20")
    @Tag("unit")
    void createDispute_ShouldAccept_WhenBookingActive() {
        booking.setEndDate(LocalDate.now().plusDays(5));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
            Dispute d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        
        Dispute result = disputeService.createDispute(2L, 1L, DisputeReason.INCORRECT_LISTING, 
            "Item not as described", null);
        
        assertNotNull(result);
        verify(disputeRepository, times(1)).save(any(Dispute.class));
    }
}

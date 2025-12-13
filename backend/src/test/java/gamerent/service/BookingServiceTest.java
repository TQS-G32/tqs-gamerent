package gamerent.service;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import gamerent.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Requirement("US-1")
class BookingServiceTest {
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private ItemRepository itemRepository;
    
    @InjectMocks
    private BookingService bookingService;
    
    private BookingRequest booking;
    private Item item;
    private User owner;
    
    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Owner");
        owner.setEmail("owner@test.com");
        owner.setRole("OWNER");
        
        item = new Item();
        item.setId(1L);
        item.setName("Test Item");
        item.setOwner(owner);
        item.setPricePerDay(15.0);
        item.setAvailable(true);
        
        booking = new BookingRequest();
        booking.setId(1L);
        booking.setItemId(1L);
        booking.setUserId(2L);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(5));
        booking.setStatus(BookingStatus.PENDING);
    }
    
    @Test
    void createBooking_ShouldCreateWhenNoConflict() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemIdAndStatus(1L, BookingStatus.APPROVED))
            .thenReturn(List.of());
        when(bookingRepository.save(any(BookingRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(5);
        
        BookingRequest result = bookingService.createBooking(1L, 2L, start, end);

        
        assertNotNull(result);
        assertEquals(BookingStatus.PENDING, result.getStatus());
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.of(2025,12,1), LocalDate.of(2025,12,5)) + 1;
        assertEquals(15.0 * (double) days, result.getTotalPrice());
        verify(bookingRepository, times(1)).save(any(BookingRequest.class));
    }
    
    @Test
    void createBooking_ShouldThrowWhenOverlapping() {
        BookingRequest existing = new BookingRequest();
        existing.setStartDate(LocalDate.now().plusDays(2));
        existing.setEndDate(LocalDate.now().plusDays(3));
        
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemIdAndStatus(1L, BookingStatus.APPROVED))
            .thenReturn(List.of(existing));
        
        assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(
                1L, 2L, 
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5)
            );
        }, "Should throw exception for overlapping dates");
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ShouldValidateStartBeforeEnd() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(
                1L, 2L,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(1)
            );
        }, "Should throw exception when start is after end");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ShouldValidateDatesNotInPast() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        LocalDate pastDate = LocalDate.now().minusDays(1);

        assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(
                1L, 2L,
                pastDate,
                pastDate.plusDays(3)
            );
        }, "Should throw exception when start date is in the past");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ShouldThrowWhenUserIsOwner() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        // item owner id is 1L (set in setUp)
        assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(
                1L, 1L, 
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 5)
            );
        }, "Should throw exception when user rents their own item");
        
        verify(bookingRepository, never()).save(any());
    }
    
    @Test
    void updateStatus_ShouldUpdateWhenOwnerMatches() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(BookingRequest.class))).thenReturn(booking);
        
        booking.setStatus(BookingStatus.APPROVED);
        BookingRequest result = bookingService.updateStatus(1L, BookingStatus.APPROVED, 1L);
        
        assertEquals(BookingStatus.APPROVED, result.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }
    
    @Test
    void updateStatus_ShouldThrowWhenOwnerMismatch() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(RuntimeException.class, () ->
            bookingService.updateStatus(1L, BookingStatus.APPROVED, 999L),
            "Should throw exception when non-owner updates booking"
        );
        
        verify(bookingRepository, never()).save(any());
    }
    
    @Test
    void getUserBookings_ShouldReturnUserBookings() {
        when(bookingRepository.findByUserId(2L)).thenReturn(List.of(booking));
        
        List<BookingRequest> result = bookingService.getUserBookings(2L);
        
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getUserId());
        verify(bookingRepository, times(1)).findByUserId(2L);
    }
    
    @Test
    void getOwnerBookings_ShouldReturnBookingsForOwnerItems() {
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(item));
        when(bookingRepository.findByItemId(1L)).thenReturn(List.of(booking));
        
        List<BookingRequest> result = bookingService.getOwnerBookings(1L);
        
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getItemId());
        verify(itemRepository, times(1)).findByOwnerId(1L);
    }

    @Test
    void createBooking_NullStartDate_ShouldThrowException() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(1L, 2L, null, LocalDate.now().plusDays(5));
        }, "Should throw exception when start date is null");
    }

    @Test
    void createBooking_NullEndDate_ShouldThrowException() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(1L, 2L, LocalDate.now().plusDays(1), null);
        }, "Should throw exception when end date is null");
    }

    @Test
    void createBooking_ItemNotFound_ShouldThrowException() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(999L, 2L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
        });
    }

    @Test
    void getItemBookings_ShouldReturnBookingsForItem() {
        when(bookingRepository.findByItemId(1L)).thenReturn(List.of(booking));
        
        List<BookingRequest> result = bookingService.getItemBookings(1L);
        
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getItemId());
    }
}

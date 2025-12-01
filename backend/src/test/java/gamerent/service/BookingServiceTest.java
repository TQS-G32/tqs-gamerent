package gamerent.service;

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
        
        booking = new BookingRequest();
        booking.setId(1L);
        booking.setItemId(1L);
        booking.setUserId(2L);
        booking.setStartDate(LocalDate.of(2025, 12, 1));
        booking.setEndDate(LocalDate.of(2025, 12, 5));
        booking.setStatus(BookingStatus.PENDING);
    }
    
    @Test
    void createBooking_ShouldCreateWhenNoConflict() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemIdAndStatus(1L, BookingStatus.APPROVED))
            .thenReturn(List.of());
        when(bookingRepository.save(any(BookingRequest.class)))
            .thenReturn(booking);
        
        BookingRequest result = bookingService.createBooking(
            1L, 2L, 
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 5)
        );
        
        assertNotNull(result);
        assertEquals(BookingStatus.PENDING, result.getStatus());
        verify(bookingRepository, times(1)).save(any(BookingRequest.class));
    }
    
    @Test
    void createBooking_ShouldThrowWhenOverlapping() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        BookingRequest existing = new BookingRequest();
        existing.setStartDate(LocalDate.of(2025, 12, 2));
        existing.setEndDate(LocalDate.of(2025, 12, 4));
        
        when(bookingRepository.findByItemIdAndStatus(1L, BookingStatus.APPROVED))
            .thenReturn(List.of(existing));
        
        assertThrows(RuntimeException.class, () ->
            bookingService.createBooking(
                1L, 2L, 
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 5)
            ),
            "Should throw exception for overlapping dates"
        );
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ShouldThrowWhenUserIsOwner() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        
        // item owner id is 1L (set in setUp)
        assertThrows(RuntimeException.class, () ->
            bookingService.createBooking(
                1L, 1L, 
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 5)
            ),
            "Should throw exception when user rents their own item"
        );
        
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
}

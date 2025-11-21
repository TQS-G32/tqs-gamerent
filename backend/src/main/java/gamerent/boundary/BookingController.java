package gamerent.boundary;

import gamerent.data.BookingRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import gamerent.data.BookingRequest;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {
	private final BookingRepository bookingRepository;

	public BookingController(BookingRepository bookingRepository) {
		this.bookingRepository = bookingRepository;
	}

	@GetMapping
	public List<BookingRequest> getAllBookings() {
		return bookingRepository.findAll();
	}

	@PostMapping
	public BookingRequest addBooking(@RequestBody BookingRequest booking) {
		return bookingRepository.save(booking);
	}
}
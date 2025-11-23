package gamerent.boundary;

import gamerent.service.IgdbService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/igdb")
@CrossOrigin(origins = "*")
public class IgdbController {
    private final IgdbService igdbService;

    public IgdbController(IgdbService igdbService) {
        this.igdbService = igdbService;
    }

    @GetMapping("/search")
    public String search(@RequestParam String q, @RequestParam(required = false, defaultValue = "Game") String type) {
        return igdbService.search(q, type);
    }
}

package gamerent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class IgdbService {
    private static final String GAMES_URL = "https://api.igdb.com/v4/games";
    private static final String PLATFORMS_URL = "https://api.igdb.com/v4/platforms";
    private static final Logger logger = Logger.getLogger(IgdbService.class.getName());
    
    @Value("${igdb.client-id}")
    private String clientId;
    
    @Value("${igdb.auth-token}")
    private String authToken;
    
    private final RestTemplate restTemplate;

    public IgdbService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public String search(String query, String type) {
        if (isConfigMissing()) return "[]";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-ID", clientId);
        headers.set("Authorization", authToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        String url;
        String body;
        
        if ("Console".equalsIgnoreCase(type)) {
            url = PLATFORMS_URL;
            body = "search \"" + query + "\"; fields name,platform_logo.url; limit 10;";
        } else {
            // For games, fetch cover and platforms
            url = GAMES_URL;
            body = "search \"" + query + "\"; fields name,cover.url,platforms.name; limit 10;";
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Error searching " + type + ": " + e.getMessage());
            return "[]";
        }
    }
    
    // Kept for compatibility if needed, defaults to Game
    public String searchGames(String query) {
        return search(query, "Game");
    }

    public List<JsonNode> getPopularGames(int limit) {
        if (isConfigMissing()) {
            logger.log(Level.INFO, "IGDB credentials missing. Skipping API call.");
            return Collections.emptyList();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-ID", clientId);
        headers.set("Authorization", authToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Fetch ALL games with no limit (IGDB max is 500 per request, so we fetch the max)
        String body = "fields id,name,cover.url,summary; where rating > 0 & cover.url != null; sort rating desc; limit " + (limit > 0 ? limit : 500) + ";";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(GAMES_URL, HttpMethod.POST, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode[] games = mapper.readValue(response.getBody(), JsonNode[].class);
            return List.of(games);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Error fetching popular games: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private boolean isConfigMissing() {
        return clientId == null || authToken == null || clientId.contains("dummy") || authToken.contains("dummy");
    }
}

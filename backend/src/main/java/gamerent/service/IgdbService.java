package gamerent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.List;

@Service
public class IgdbService {
    private static final String API_URL = "https://api.igdb.com/v4/games";
    private final String clientId;
    private final String authToken;
    private final RestTemplate restTemplate;

    public IgdbService(RestTemplateBuilder builder) {
        this.clientId = System.getenv("IGDB_CLIENT_ID");
        this.authToken = System.getenv("IGDB_AUTH_TOKEN");
        this.restTemplate = builder.build();
    }

    public String searchGames(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-ID", clientId);
        headers.set("Authorization", authToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>("search \"" + query + "\"; fields name,cover.url; limit 10;", headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error searching games: " + e.getMessage());
            return "[]";
        }
    }

    public List<JsonNode> getPopularGames(int limit) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-ID", clientId);
        headers.set("Authorization", authToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String body = "fields id,name,cover.url,summary; where rating > 70 & cover.url != null; sort popularity desc; limit " + limit + ";";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode[] games = mapper.readValue(response.getBody(), JsonNode[].class);
            return List.of(games);
        } catch (Exception e) {
            System.err.println("Error fetching popular games: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}

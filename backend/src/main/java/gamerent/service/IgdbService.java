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

@Service
public class IgdbService {
    private static final String GAMES_URL = "https://api.igdb.com/v4/games";
    private static final String PLATFORMS_URL = "https://api.igdb.com/v4/platforms";
    private static final String PLATFORM_LOGOS_URL = "https://api.igdb.com/v4/platform_logos";
    
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
            body = "search \"" + query + "\"; fields name,platform_logo; limit 10;";
        } else {
            // For games, fetch cover and platforms
            url = GAMES_URL;
            body = "search \"" + query + "\"; fields name,cover.url,platforms.name; limit 10;";
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();
            
            // For consoles, resolve platform_logo IDs to actual URLs
            if ("Console".equalsIgnoreCase(type)) {
                responseBody = resolvePlatformLogos(responseBody, headers);
            }
            
            return responseBody;
        } catch (Exception e) {
            System.err.println("Error searching " + type + ": " + e.getMessage());
            return "[]";
        }
    }
    
    private String resolvePlatformLogos(String platformsJson, HttpHeaders headers) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode[] platforms = mapper.readValue(platformsJson, JsonNode[].class);
            
            for (JsonNode platform : platforms) {
                if (platform.has("platform_logo")) {
                    long logoId = platform.get("platform_logo").asLong();
                    String logoUrl = fetchPlatformLogoUrl(logoId, headers);
                    
                    // Create a new object node to replace platform_logo with the URL
                    com.fasterxml.jackson.databind.node.ObjectNode logoNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                    logoNode.put("url", logoUrl);
                    ((com.fasterxml.jackson.databind.node.ObjectNode) platform).set("platform_logo", logoNode);
                }
            }
            
            return mapper.writeValueAsString(platforms);
        } catch (Exception e) {
            System.err.println("Error resolving platform logos: " + e.getMessage());
            return platformsJson;
        }
    }
    
    private String fetchPlatformLogoUrl(long logoId, HttpHeaders headers) {
        try {
            String body = "where id = " + logoId + "; fields url;";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(PLATFORM_LOGOS_URL, HttpMethod.POST, entity, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode[] logos = mapper.readValue(response.getBody(), JsonNode[].class);
            
            if (logos.length > 0 && logos[0].has("url")) {
                return logos[0].get("url").asText();
            }
        } catch (Exception e) {
            System.err.println("Error fetching platform logo " + logoId + ": " + e.getMessage());
        }
        return null;
    }
    
    // Kept for compatibility if needed, defaults to Game
    public String searchGames(String query) {
        return search(query, "Game");
    }

    public List<JsonNode> getPopularGames(int limit) {
        if (isConfigMissing()) {
            System.out.println("IGDB credentials missing. Skipping API call.");
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
            System.err.println("Error fetching popular games: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private boolean isConfigMissing() {
        return clientId == null || authToken == null || clientId.contains("dummy") || authToken.contains("dummy");
    }
}
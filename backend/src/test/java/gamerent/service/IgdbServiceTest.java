package gamerent.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IgdbServiceTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private IgdbService igdbService;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        igdbService = new IgdbService(restTemplateBuilder);
        
        ReflectionTestUtils.setField(igdbService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(igdbService, "authToken", "Bearer test-token");
    }

    @Test
    void getPopularGames_Success_ShouldReturnGames() throws Exception {
        String jsonResponse = """
            [
                {
                    "id": 1,
                    "name": "Test Game",
                    "summary": "A test game",
                    "cover": {
                        "url": "//images.igdb.com/igdb/image/upload/t_thumb/test.jpg"
                    }
                }
            ]
            """;

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        List<JsonNode> result = igdbService.getPopularGames(10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test Game", result.get(0).get("name").asText());
    }

    @Test
    void getPopularGames_ApiError_ShouldReturnEmptyList() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenThrow(new RestClientException("API Error"));

        List<JsonNode> result = igdbService.getPopularGames(10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPopularGames_EmptyResponse_ShouldReturnEmptyList() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>("[]", HttpStatus.OK));

        List<JsonNode> result = igdbService.getPopularGames(10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPopularGames_NullResponse_ShouldReturnEmptyList() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        List<JsonNode> result = igdbService.getPopularGames(10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPopularGames_MissingCredentials_ShouldReturnEmptyList() {
        // Reset credentials to trigger isConfigMissing
        ReflectionTestUtils.setField(igdbService, "clientId", null);
        ReflectionTestUtils.setField(igdbService, "authToken", null);

        List<JsonNode> result = igdbService.getPopularGames(10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPopularGames_WrongCredentials_ShouldReturnEmptyList() {
        ReflectionTestUtils.setField(igdbService, "clientId", "false-client-id");
        ReflectionTestUtils.setField(igdbService, "authToken", "false-token");

        List<JsonNode> result = igdbService.getPopularGames(10);

        assertTrue(result.isEmpty());
    }
}
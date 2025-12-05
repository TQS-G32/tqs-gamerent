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

    @Test
    void search_ConsoleType_ShouldResolvePlatformLogos() throws Exception {
        String consoleResponse = """
            [
                {
                    "id": 167,
                    "name": "PlayStation 5",
                    "platform_logo": 892
                }
            ]
            """;

        String logoResponse = """
            [
                {
                    "id": 892,
                    "url": "//images.igdb.com/igdb/image/upload/t_thumb/plos.jpg"
                }
            ]
            """;

        when(restTemplate.exchange(
            contains("platforms"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(consoleResponse, HttpStatus.OK));

        when(restTemplate.exchange(
            contains("platform_logos"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(logoResponse, HttpStatus.OK));

        String result = igdbService.search("PlayStation 5", "Console");

        assertNotNull(result);
        assertTrue(result.contains("plos.jpg"));
        assertTrue(result.contains("platform_logo"));
    }

    @Test
    void search_ConsoleType_WithMissingLogo_ShouldHandleGracefully() throws Exception {
        String consoleResponse = """
            [
                {
                    "id": 167,
                    "name": "PlayStation 5",
                    "platform_logo": 999
                }
            ]
            """;

        String emptyLogoResponse = "[]";

        when(restTemplate.exchange(
            contains("platforms"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(consoleResponse, HttpStatus.OK));

        when(restTemplate.exchange(
            contains("platform_logos"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(emptyLogoResponse, HttpStatus.OK));

        String result = igdbService.search("PlayStation 5", "Console");

        assertNotNull(result);
        assertTrue(result.contains("PlayStation 5"));
    }

    @Test
    void search_GameType_ShouldNotResolvePlatformLogos() throws Exception {
        String gameResponse = """
            [
                {
                    "id": 119133,
                    "name": "Elden Ring",
                    "cover": {
                        "url": "//images.igdb.com/igdb/image/upload/t_thumb/co4jni.jpg"
                    },
                    "platforms": []
                }
            ]
            """;

        when(restTemplate.exchange(
            contains("games"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(gameResponse, HttpStatus.OK));

        String result = igdbService.search("Elden Ring", "Game");

        assertNotNull(result);
        assertTrue(result.contains("Elden Ring"));
        assertTrue(result.contains("co4jni.jpg"));
    }

    @Test
    void search_PlatformLogoResolution_ApiError_ShouldReturnOriginalJson() throws Exception {
        String consoleResponse = """
            [
                {
                    "id": 167,
                    "name": "PlayStation 5",
                    "platform_logo": 892
                }
            ]
            """;

        when(restTemplate.exchange(
            contains("platforms"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(consoleResponse, HttpStatus.OK));

        when(restTemplate.exchange(
            contains("platform_logos"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenThrow(new RestClientException("API Error"));

        String result = igdbService.search("PlayStation 5", "Console");

        assertNotNull(result);
        assertTrue(result.contains("PlayStation 5"));
    }
}
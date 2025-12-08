package gamerent.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class IgdbServiceLightTest {

    @Test
    void getPopularGames_NullResponse_ShouldReturnEmptyList() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        IgdbService service = new IgdbService(builder);

        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenReturn(null);

        assertTrue(service.getPopularGames(0).isEmpty());
    }

    @Test
    void getPopularGames_ApiError_ShouldReturnEmptyList() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        IgdbService service = new IgdbService(builder);

        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("API Error", HttpStatus.UNAUTHORIZED));

        assertTrue(service.getPopularGames(0).isEmpty());
    }
}


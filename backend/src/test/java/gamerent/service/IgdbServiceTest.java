package gamerent.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IgdbServiceTest {

    @Autowired
    private IgdbService igdbService;

    @Test
    void searchGames_returnsResults() {
        String result = igdbService.searchGames("sonic");
        assertThat(result).isNotNull();
        assertThat(result).contains("name");
    }
}

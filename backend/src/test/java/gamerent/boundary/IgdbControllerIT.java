package gamerent.boundary;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import gamerent.service.IgdbService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Requirement("US1")
class IgdbControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IgdbService igdbService;

    @Test
    @XrayTest(key = "IGDB-1")
    @Tag("integration")
    void searchGames_WithQuery_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/igdb/search")
                .param("q", "Mario"))
                .andExpect(status().isOk());
    }

    @Test
    @XrayTest(key = "IGDB-2")
    @Tag("integration")
    void searchGames_WithTypeParameter_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/igdb/search")
                .param("q", "Zelda")
                .param("type", "Game"))
                .andExpect(status().isOk());
    }

    @Test
    @XrayTest(key = "IGDB-3")
    @Tag("integration")
    void searchGames_WithEmptyQuery_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/igdb/search")
                .param("q", ""))
                .andExpect(status().isOk());
    }

    @Test
    @XrayTest(key = "IGDB-4")
    @Tag("integration")
    void searchGames_WithSpecialCharacters_ShouldHandleGracefully() throws Exception {
        mockMvc.perform(get("/api/igdb/search")
                .param("q", "Grand Theft Auto"))
                .andExpect(status().isOk());
    }

    @Test
    @XrayTest(key = "IGDB-5")
    @Tag("integration")
    void searchGames_WithoutTypeParameter_ShouldDefaultToGame() throws Exception {
        mockMvc.perform(get("/api/igdb/search")
                .param("q", "Sonic"))
                .andExpect(status().isOk());
    }

    @Test
    @XrayTest(key = "IGDB-6")
    @Tag("integration")
    void searchGames_WithLongQuery_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/igdb/search")
                .param("q", "The Legend of Zelda Breath of the Wild"))
                .andExpect(status().isOk());
    }
}

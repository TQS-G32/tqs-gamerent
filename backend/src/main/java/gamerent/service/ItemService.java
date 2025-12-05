package gamerent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final IgdbService igdbService;
    private final Random random = new Random();

    public ItemService(ItemRepository itemRepository, IgdbService igdbService) {
        this.itemRepository = itemRepository;
        this.igdbService = igdbService;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public List<Item> getAllItemsPaginated(int page, int pageSize) {
        List<Item> allItems = itemRepository.findAll();
        int start = page * pageSize;
        int end = Math.min(start + pageSize, allItems.size());
        if (start >= allItems.size()) return new ArrayList<>();
        return allItems.subList(start, end);
    }

    public int getTotalItemCount() {
        return (int) itemRepository.count();
    }

    public List<Item> searchAllItemsByName(String query) {
        if (query != null && !query.isEmpty()) {
            return itemRepository.fuzzySearchByName(query);
        }
        return getAllItems();
    }

    public List<Item> searchAllItemsByNameAndCategory(String query, String category) {
        if (query != null && !query.isEmpty() && category != null && !category.isEmpty()) {
            return itemRepository.fuzzySearchByNameAndCategory(query, category);
        } else if (query != null && !query.isEmpty()) {
            return itemRepository.fuzzySearchByName(query);
        } else if (category != null && !category.isEmpty()) {
            return itemRepository.findByCategoryIgnoreCase(category);
        }
        return getAllItems();
    }

    public List<Item> searchAllItemsPaginated(String query, String category, int page, int pageSize) {
        List<Item> results = searchAllItemsByNameAndCategory(query, category);
        int start = page * pageSize;
        int end = Math.min(start + pageSize, results.size());
        if (start >= results.size()) return new ArrayList<>();
        return results.subList(start, end);
    }

    public int getSearchAllItemsResultCount(String query, String category) {
        return searchAllItemsByNameAndCategory(query, category).size();
    }

    public List<Item> searchItems(String query, String category) {
        if (query != null && !query.isEmpty() && category != null && !category.isEmpty()) {
            // Use fuzzy search for both query and category
            return itemRepository.fuzzySearchByNameAndCategory(query, category);
        } else if (query != null && !query.isEmpty()) {
            // Use fuzzy search for name (handles partial matches like "playstatio")
            return itemRepository.fuzzySearchByName(query);
        } else if (category != null && !category.isEmpty()) {
            return itemRepository.findByCategoryIgnoreCase(category);
        }
        return getAllItems();
    }

    public List<Item> searchItemsPaginated(String query, String category, int page, int pageSize) {
        List<Item> results = searchItems(query, category);
        int start = page * pageSize;
        int end = Math.min(start + pageSize, results.size());
        if (start >= results.size()) return new ArrayList<>();
        return results.subList(start, end);
    }

    public int getSearchResultCount(String query, String category) {
        return searchItems(query, category).size();
    }

    public List<Item> getItemsByOwner(Long ownerId) {
        return itemRepository.findByOwnerId(ownerId);
    }

    public Item addItem(Item item, User owner) {
        item.setOwner(owner);
        return itemRepository.save(item);
    }

    public Item getItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found"));
    }

    public void populateFromIGDB(int limit, User owner) {
        List<Item> items = new ArrayList<>();

        // Carrega jogos da IGDB
        List<JsonNode> games = igdbService.getPopularGames(limit);

        for (int i = 0; i < games.size(); i++) {
            JsonNode game = games.get(i);
            String name = game.get("name").asText();
            String description = game.has("summary") ? game.get("summary").asText() : "A great game";
            // Truncate description to fit database limit (2048 chars)
            if (description.length() > 2048) {
                description = description.substring(0, 2045) + "...";
            }
            String imageUrl = getImageUrl(game);

            Item item = new Item(name, description, null, imageUrl, owner);
            item.setCategory("Game");
            double price = getRandomPrice();
            item.setPricePerDay(price);

            items.add(item);
        }

        // Adiciona consoles com imagens reais do IGDB
        List<JsonNode> platforms = fetchPopularPlatforms();
        for (JsonNode platform : platforms) {
            String consoleName = platform.get("name").asText();
            String imageUrl = null;

            if (platform.has("platform_logo") && platform.get("platform_logo").has("url")) {
                String url = platform.get("platform_logo").get("url").asText();
                if (url.startsWith("//")) {
                    url = "https:" + url;
                }
                imageUrl = url;
            }

            if (imageUrl != null) {
                Item console = new Item(consoleName, "Gaming console for rent. High performance and great selection of games.", null,
                    imageUrl, owner);
                console.setCategory("Console");
                console.setPricePerDay(getRandomPriceForConsole());
                items.add(console);
            }
        }

        // Adiciona acessórios com fundo escuro e nome
        String[] accessories = {
            "PlayStation 5 Controller", "Xbox Controller", "Nintendo Pro Controller",
            "Gaming Headset", "Mechanical Keyboard", "Gaming Mouse",
            "Monitor 144Hz", "USB-C Cable", "HDMI Cable", "Controller Charging Dock",
            "Game Pass Subscription Card", "PlayStation Plus Card", "Gaming Chair",
            "Cooling Fan", "Laptop Stand", "Mouse Pad"
        };

        for (String accessoryName : accessories) {
            String imageUrl = "https://dummyimage.com/400x500/2a2a2a/ffffff?text=" + accessoryName.replace(" ", "+");
            Item accessory = new Item(accessoryName, "Quality gaming accessory to enhance your gaming experience.", null,
                imageUrl, owner);
            accessory.setCategory("Accessory");
            accessory.setPricePerDay(getRandomPriceForAccessory());
            items.add(accessory);
        }

        itemRepository.saveAll(items);
    }

    private List<JsonNode> fetchPopularPlatforms() {
        try {
            HttpHeaders headers = new HttpHeaders();
            String clientId = System.getenv("IGDB_CLIENT_ID");
            String authToken = System.getenv("IGDB_AUTH_TOKEN");
            headers.set("Client-ID", clientId != null ? clientId : "");
            headers.set("Authorization", authToken != null ? authToken : "");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            // Fetch specific modern consoles: PlayStation 5 (167), Xbox Series X|S (169), Nintendo Switch (130)
            String body = "where id = (167, 169, 130, 48, 49); fields id,name,platform_logo.url;";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange("https://api.igdb.com/v4/platforms", HttpMethod.POST, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode[] platforms = mapper.readValue(response.getBody(), JsonNode[].class);

            List<JsonNode> result = new ArrayList<>();
            for (JsonNode platform : platforms) {
                if (platform.has("platform_logo") && platform.get("platform_logo").has("url")) {
                    result.add(platform);
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error fetching popular platforms: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private double getRandomPrice() {
        // Random price between 1.99 and 5.99
        return Math.round((1.99 + (random.nextDouble() * 4)) * 100) / 100.0;
    }

    private double getRandomPriceForConsole() {
        // Random price between 8.99 and 15.99 for consoles
        return Math.round((8.99 + (random.nextDouble() * 7)) * 100) / 100.0;
    }

    private double getRandomPriceForAccessory() {
        // Random price between 0.99 and 4.99 for accessories
        return Math.round((0.99 + (random.nextDouble() * 4)) * 100) / 100.0;
    }

    private String getImageUrl(JsonNode game) {
        if (game.has("cover") && game.get("cover").has("url")) {
            String url = game.get("cover").get("url").asText();
            // IGDB returns URLs starting with //, we need to add https:
            if (url.startsWith("//")) {
                url = "https:" + url;
            }
            // Replace t_thumb with t_720p for better quality
            url = url.replace("t_thumb", "t_720p");
            return url;
        }
        return "https://dummyimage.com/400x500/1a1a1a/ffffff?text=" + game.get("name").asText().replace(" ", "+");
    }
}

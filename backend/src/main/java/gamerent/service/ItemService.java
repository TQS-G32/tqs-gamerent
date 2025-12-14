package gamerent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.BookingRepository;
import gamerent.data.BookingRequest;
import gamerent.data.BookingStatus;
import gamerent.data.User;
import gamerent.config.ItemValidationException;
import gamerent.config.UnauthorizedException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final IgdbService igdbService;
    private final BookingRepository bookingRepository;
    private final Random random = new Random();
    private static final String PLATFORM_LOGO = "platform_logo";
    private static final String COVER = "cover";
    private static final Logger logger = Logger.getLogger(ItemService.class.getName());

    public ItemService(ItemRepository itemRepository, IgdbService igdbService, BookingRepository bookingRepository) {
        this.itemRepository = itemRepository;
        this.igdbService = igdbService;
        this.bookingRepository = bookingRepository;
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
        // Delegate to searchAllItemsByNameAndCategory for consistency
        return searchAllItemsByNameAndCategory(query, category);
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

    public List<Item> getItemsByOwnerPaginated(Long ownerId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(pageSize, 1));
        return itemRepository.findByOwnerId(ownerId, pageable).getContent();
    }

    public int getItemsByOwnerCount(Long ownerId) {
        return itemRepository.findByOwnerId(ownerId).size();
    }

    public Item addItem(Item item, User owner) {
        item.setOwner(owner);
        return itemRepository.save(item);
    }

    public Item getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Item not found"));
    }
    
    public void populateFromIGDB(int limit, User owner) {
        List<Item> items = new ArrayList<>();

        // Carrega jogos da IGDB
        List<JsonNode> games = igdbService.getPopularGames(limit);

        for (int i = 0; i < games.size(); i++) {
            JsonNode game = games.get(i);
            String name = game.get("name").asText();
            String description = "";
            if (game.has("summary")){
                description = game.get("summary").asText();
            } else {
                description = "A great game";
            }
            // Truncate description to fit database limit (2048 chars)
            if (description.length() > 2048) {
                description = description.substring(0, 2045) + "...";
            }
            String imageUrl = getImageUrl(game);

            Item item = new Item(name, description, null, imageUrl, owner);
            item.setCategory("Game");
            // Only the first 20 games are set as rentable by default
            if (i < 20) {
                double price = getRandomPrice();
                item.setPricePerDay(price);
                item.setAvailable(true);
                item.setMinRentalDays(1);
            } else {
                // Appear in catalog but not rentable
                item.setPricePerDay(null);
                item.setAvailable(false);
                item.setMinRentalDays(1);
            }
            
            items.add(item);
        }

        // Adiciona consoles com imagens reais do IGDB
        List<JsonNode> platforms = fetchPopularPlatforms();
        for (JsonNode platform : platforms) {
            String consoleName = platform.get("name").asText();
            String imageUrl = null;

            if (platform.has(PLATFORM_LOGO) && platform.get(PLATFORM_LOGO).has("url")) {
                String url = platform.get(PLATFORM_LOGO).get("url").asText();
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
            // accessories set as rentable by default
            accessory.setPricePerDay(getRandomPriceForAccessory());
            accessory.setAvailable(true);
            accessory.setMinRentalDays(1);
            items.add(accessory);
        }

        itemRepository.saveAll(items);
    }

    // Owner-only update of availability and minimum rental days
    public Item updateItemSettings(Long itemId, Long ownerId, Boolean available, Integer minRentalDays) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new NoSuchElementException("Item not found"));
        if (item.getOwner() == null || !item.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("Unauthorized: You are not the owner of this item");
        }

        updateMinimalRentalDays(item, minRentalDays);
        updateAvailability(item, available, itemId);

        return itemRepository.save(item);
    }

    private void updateMinimalRentalDays(Item item, Integer minRentalDays) {
        if (minRentalDays != null) {
            if (minRentalDays < 1 || minRentalDays > 30) {
                throw new ItemValidationException("Minimum rental days must be between 1 and 30");
            }
            item.setMinRentalDays(minRentalDays);
        }
    }

    private void updateAvailability(Item item, Boolean available, Long itemId) {
        if (available == null) {
            return;
        }

        if (available) {
            item.setAvailable(true);
        } else {
            checkActiveBookingsBeforeDeactivation(itemId);
            item.setAvailable(false);
        }
    }

    private void checkActiveBookingsBeforeDeactivation(Long itemId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        for (BookingRequest b : bookingRepository.findByItemId(itemId)) {
            if ((b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.PENDING) &&
                (b.getEndDate() != null && !b.getEndDate().isBefore(today))) {
                throw new ItemValidationException("Item has active or confirmed bookings and cannot be set to Inactive");
            }
        }
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
                if (platform.has(PLATFORM_LOGO) && platform.get(PLATFORM_LOGO).has("url")) {
                    result.add(platform);
                }
            }

            return result;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error fetching user disputes - {0}", e.getMessage());
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
        if (game.has(COVER) && game.get(COVER).has("url")) {
            String url = game.get(COVER).get("url").asText();
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

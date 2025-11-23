package gamerent.service;

import com.fasterxml.jackson.databind.JsonNode;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
import gamerent.data.User;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
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

    public List<Item> searchItems(String query, String category) {
        if (query != null && !query.isEmpty() && category != null && !category.isEmpty()) {
            return itemRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(query, category);
        } else if (query != null && !query.isEmpty()) {
            return itemRepository.findByNameContainingIgnoreCase(query);
        } else if (category != null && !category.isEmpty()) {
            return itemRepository.findByCategoryIgnoreCase(category);
        }
        return getAllItems();
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
        List<JsonNode> games = igdbService.getPopularGames(limit);
        List<Item> items = new ArrayList<>();

        for (JsonNode game : games) {
            String name = game.get("name").asText();
            String description = game.has("summary") ? game.get("summary").asText() : "A great game";
            double price = getRandomPrice();
            String imageUrl = getImageUrl(game);

            Item item = new Item(name, description, price, imageUrl, owner);
            item.setCategory("Game");
            items.add(item);
        }

        itemRepository.saveAll(items);
    }

    private double getRandomPrice() {
        // Random price between 1.99 and 5.99
        return Math.round((1.99 + (random.nextDouble() * 4)) * 100) / 100.0;
    }

    private String getImageUrl(JsonNode game) {
        if (game.has("cover") && game.get("cover").has("url")) {
            String url = game.get("cover").get("url").asText();
            // IGDB returns URLs starting with //, we need to add https:
            if (url.startsWith("//")) {
                return "https:" + url;
            }
            return url;
        }
        return "https://via.placeholder.com/200x120?text=" + game.get("name").asText().replace(" ", "+");
    }
}

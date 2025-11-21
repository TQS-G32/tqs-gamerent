package gamerent.service;

import com.fasterxml.jackson.databind.JsonNode;
import gamerent.data.Item;
import gamerent.data.ItemRepository;
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

    public Item addItem(Item item) {
        return itemRepository.save(item);
    }

    public void populateFromIGDB(int limit) {
        List<JsonNode> games = igdbService.getPopularGames(limit);
        List<Item> items = new ArrayList<>();

        for (JsonNode game : games) {
            String name = game.get("name").asText();
            String description = game.has("summary") ? game.get("summary").asText() : "A great game";
            double price = getRandomPrice();
            String imageUrl = getImageUrl(game);

            Item item = new Item(name, description, price, imageUrl);
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

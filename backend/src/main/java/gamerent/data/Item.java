package gamerent.data;

import jakarta.persistence.*;

@Entity
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 2048)
    private String description;
    private String category;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    
    private Double pricePerDay;
    // Whether the item is available for renting (Active) or temporarily unavailable (Inactive)
    private Boolean available = false;

    // Minimum rental period in days (1..30). Null or 1 means no special minimum.
    private Integer minRentalDays = 1;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Item() {
    }

    public Item(String name, String description, Double pricePerDay, String imageUrl, User owner) {
        this.name = name;
        this.description = description;
        this.pricePerDay = pricePerDay;
        this.imageUrl = imageUrl;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public Double getPricePerDay() {
        return pricePerDay;
    }
    public void setPricePerDay(Double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }
    public Boolean getAvailable() {
        return available != null && available;
    }
    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Integer getMinRentalDays() {
        return minRentalDays == null ? 1 : minRentalDays;
    }
    public void setMinRentalDays(Integer minRentalDays) {
        this.minRentalDays = minRentalDays;
    }
    public User getOwner() {
        return owner;
    }
    public void setOwner(User owner) {
        this.owner = owner;
    }
}

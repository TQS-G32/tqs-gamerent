package gamerent.boundary.dto;

import java.time.LocalDateTime;

/**
 * DTO for chat responses, containing basic chat information and item details.
 */
public class ChatResponse {
    private Long id;
    private Long renterId;
    private String renterName;
    private String renterEmail;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private Long itemId;
    private String itemName;
    private String itemImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastMessage;
    private Long unreadCount;

    public ChatResponse() {
    }

    public ChatResponse(Long id, Long renterId, String renterName, String renterEmail, 
                       Long ownerId, String ownerName, String ownerEmail,
                       Long itemId, String itemName, String itemImageUrl,
                       LocalDateTime createdAt, LocalDateTime updatedAt,
                       String lastMessage, Long unreadCount) {
        this.id = id;
        this.renterId = renterId;
        this.renterName = renterName;
        this.renterEmail = renterEmail;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemImageUrl = itemImageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRenterId() {
        return renterId;
    }

    public void setRenterId(Long renterId) {
        this.renterId = renterId;
    }

    public String getRenterName() {
        return renterName;
    }

    public void setRenterName(String renterName) {
        this.renterName = renterName;
    }

    public String getRenterEmail() {
        return renterEmail;
    }

    public void setRenterEmail(String renterEmail) {
        this.renterEmail = renterEmail;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemImageUrl() {
        return itemImageUrl;
    }

    public void setItemImageUrl(String itemImageUrl) {
        this.itemImageUrl = itemImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}

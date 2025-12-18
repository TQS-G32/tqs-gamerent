package gamerent.boundary.dto;

/**
 * DTO for creating or initiating a chat.
 */
public class ChatRequest {
    private Long itemId;
    private String initialMessage;

    public ChatRequest() {
    }

    public ChatRequest(Long itemId, String initialMessage) {
        this.itemId = itemId;
        this.initialMessage = initialMessage;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getInitialMessage() {
        return initialMessage;
    }

    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }
}

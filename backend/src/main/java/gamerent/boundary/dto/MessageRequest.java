package gamerent.boundary.dto;

/**
 * DTO for sending a message in a chat.
 */
public class MessageRequest {
    private String content;

    public MessageRequest() {
    }

    public MessageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

package pt.lourenco.optimization.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OllamaChatRequest {

    private String model;
    private boolean stream;
    private List<Message> messages;

    @JsonProperty("keep_alive")
    private Integer keepAlive;

    public OllamaChatRequest() {
    }

    public OllamaChatRequest(String model, boolean stream, List<Message> messages, Integer keepAlive) {
        this.model = model;
        this.stream = stream;
        this.messages = messages;
        this.keepAlive = keepAlive;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Integer getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Integer keepAlive) {
        this.keepAlive = keepAlive;
    }

    public static class Message {
        private String role;
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
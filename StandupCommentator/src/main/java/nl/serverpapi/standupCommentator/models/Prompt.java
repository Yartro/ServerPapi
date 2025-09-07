package nl.serverpapi.standupCommentator.models;

import org.bukkit.event.Event;

import java.time.LocalDateTime;

@Deprecated
public record Prompt(String aiHint, String data, Event event, long createdAt) {

    public Prompt(String aiHint, String data) {
        this(aiHint, data, null, System.currentTimeMillis());
    }
    public Prompt(String aiHint, String data, Event event) {
        this(aiHint, data, event, System.currentTimeMillis());
    }

    public String getPrompt() {
        return "Dit gebeurde er: %s Dit is de data hiermee geassocieerd: %s".formatted(aiHint, data);
    }

    public String getSummary() {
        return "Prompt: %s with event data %s".formatted(getPrompt(), (event != null) ? event.getClass().getName() : "<Empty>");
    }

}

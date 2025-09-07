package nl.serverpapi.standupCommentator.models;

import org.bukkit.event.Event;

public record GamePrompt(String prompt, Event event, long createdAt) {
    public GamePrompt(String prompt) {
        this(prompt, null, System.currentTimeMillis());
    }
    public GamePrompt(String prompt, Event event) {
        this(prompt, event, System.currentTimeMillis());
    }

    public boolean hasEvent() {
        return event != null;
    }
    public String getSummary() {
        return (hasEvent()) ?
                "Event name: %s with prompt: %s".formatted(event.getClass().getName(), prompt) :
                "Prompt: %s".formatted(prompt);
    }
}

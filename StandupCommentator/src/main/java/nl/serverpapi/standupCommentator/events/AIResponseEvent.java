package nl.serverpapi.standupCommentator.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AIResponseEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final String response;

    public AIResponseEvent(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
package nl.serverpapi.standupCommentator.utils;

import nl.serverpapi.standupCommentator.models.GamePrompt;
import nl.serverpapi.standupCommentator.models.Prompt;
import org.bukkit.event.Event;

public class ResponseChance {
    protected final double chancePercent;

    public ResponseChance(double chancePercent) {
        this.chancePercent = chancePercent;
    }

    public boolean shouldRespond() { return Math.random() <= chancePercent; }

    public boolean shouldRespond(Event event) {
        // Add some more logic depending on event or prompt
        return shouldRespond();
    }
}
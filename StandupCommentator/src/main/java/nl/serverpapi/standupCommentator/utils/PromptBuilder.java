package nl.serverpapi.standupCommentator.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import nl.serverpapi.standupCommentator.models.GamePrompt;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PromptBuilder {

    private static final Map<Class<? extends Event>, Function<Event, GamePrompt>> handlers = new HashMap<>();

    static {
        handlers.put(PlayerJoinEvent.class, e -> {
            PlayerJoinEvent event = (PlayerJoinEvent) e;
            String prompt = "Speler " + event.getPlayer().getName() +
                    " heeft zojuist de server betreden.";
            return new GamePrompt(prompt, event);
        });

        handlers.put(PlayerQuitEvent.class, e -> {
            PlayerQuitEvent event = (PlayerQuitEvent) e;
            String prompt = "Speler " + event.getPlayer().getName() +
                    " heeft de server verlaten.";
            return new GamePrompt(prompt, event);
        });

        // Speler voltooit een achievement
        handlers.put(PlayerAdvancementDoneEvent.class, e -> {
            PlayerAdvancementDoneEvent event = (PlayerAdvancementDoneEvent) e;
            String prompt = "Speler " + event.getPlayer().getName() +
                    " heeft de prestatie '" + StringUtils.componentToString(event.getAdvancement().displayName()) +
                    "' behaald.";
            return new GamePrompt(prompt, event);
        });

        // Speler stuurt chatbericht
        handlers.put(AsyncChatEvent.class, e -> {
            AsyncChatEvent event = (AsyncChatEvent) e;
            String prompt = "Speler " + event.getPlayer().getName() +
                    " zegt: '" + StringUtils.componentToString(event.message()) +
                    "'. Reageer grappig of sarcastisch in het Nederlands.";
            return new GamePrompt(prompt, event);
        });

        // Speler gaat dood
        handlers.put(PlayerDeathEvent.class, e -> {
            PlayerDeathEvent event = (PlayerDeathEvent) e;
            String prompt = "Speler " + event.getEntity().getName() +
                    " is doodgegaan. Doodsoorzaak: " + StringUtils.componentToString(event.deathMessage()) +
                    ". Maak er een humoristische reactie van.";
            return new GamePrompt(prompt, event);
        });
    }

    public static GamePrompt buildPrompt(Event event) {
        Function<Event, GamePrompt> handler = handlers.get(event.getClass());
        if (handler != null) {
            return handler.apply(event);
        }
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
    }

    public static GamePrompt buildPrompt(String prompt) {
        return new GamePrompt(prompt);
    }
}

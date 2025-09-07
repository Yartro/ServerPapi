package nl.serverpapi.standupCommentator.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import nl.serverpapi.standupCommentator.ai.AIController;
import nl.serverpapi.standupCommentator.models.Prompt;
import nl.serverpapi.standupCommentator.utils.PromptBuilder;
import nl.serverpapi.standupCommentator.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {

    private final AIController aiController;
    public PlayerEventListener(AIController aiController) {
        this.aiController = aiController;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        aiController.offerPrompt(PromptBuilder.buildPrompt(event));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        aiController.offerPrompt(PromptBuilder.buildPrompt(event));
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        aiController.offerPrompt(PromptBuilder.buildPrompt(event));
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        final String message = StringUtils.componentToString(event.message());
        final Player player = event.getPlayer();
        if (message.startsWith("@ai")) {
            // respond
            return;
        }
        aiController.offerPrompt(PromptBuilder.buildPrompt(event));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Component deathMsgComp = event.deathMessage();

        if (deathMsgComp != null) {
            aiController.offerPrompt(PromptBuilder.buildPrompt(event));
        }
    }
}
package nl.serverpapi.standupCommentator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.serverpapi.standupCommentator.ai.AIController;
import nl.serverpapi.standupCommentator.ai.AIRunnable;
import nl.serverpapi.standupCommentator.events.AIResponseEvent;
import nl.serverpapi.standupCommentator.events.PlayerEventListener;
import nl.serverpapi.standupCommentator.models.Prompt;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public final class StandupCommentator extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("StandupCommentator enabled!");

        saveResource("config.yml", false);

        AIController aiController = new AIController(this, getConfig().getString("ollama-url"), getConfig().getString("model", "tinyllama"));
        getServer().getPluginManager().registerEvents(new PlayerEventListener(aiController), this);
        getServer().getPluginManager().registerEvents(this, this);
        AIRunnable aiRunnable = new AIRunnable(this, aiController);
        aiRunnable.runTaskTimer(this, 60 * 20L, 10 * 20L);
    }

    @EventHandler
    public void onAIResponse(AIResponseEvent event) {
        sendAIBroadcast(event.getResponse());
    }

    private void sendAIBroadcast(String msg) {
        Bukkit.broadcast(
                Component.text()
                        .append(Component.text("%s: ".formatted(getConfig().getString("chat-prefix", "AI"))).color(NamedTextColor.AQUA))
                        .append(Component.text(msg).color(NamedTextColor.YELLOW))
                        .build()
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("StandupCommentator disabled!");

    }
}

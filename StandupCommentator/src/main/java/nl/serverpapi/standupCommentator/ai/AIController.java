package nl.serverpapi.standupCommentator.ai;

import nl.serverpapi.standupCommentator.events.AIResponseEvent;
import nl.serverpapi.standupCommentator.models.GamePrompt;
import nl.serverpapi.standupCommentator.models.Prompt;
import nl.serverpapi.standupCommentator.utils.CooldownManager;
import nl.serverpapi.standupCommentator.utils.ResponseChance;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AIController {
    private final CooldownManager cooldownManager;
    private final ResponseChance respChance;
    private final JavaPlugin plugin;
    private final String aiModel;
    private final String modelURL;
    private final List<GamePrompt> promptHistory;

    private CompletableFuture<?> currentAITask;

    public AIController(JavaPlugin plugin, String modelURL, String aiModel) {
        this.plugin = plugin;
        this.aiModel = aiModel;
        this.modelURL = modelURL;
        this.cooldownManager = new CooldownManager(plugin.getConfig().getInt("cooldown", 60));
        this.respChance = new ResponseChance(plugin.getConfig().getDouble("response-chance", 0.5));
        this.promptHistory = new ArrayList<>();
    }

    public void offerPrompt(GamePrompt prompt) {
        java.util.logging.Logger logger = plugin.getLogger();

        // Task is already running
        if (currentAITask != null && !currentAITask.isDone()) return;
        if (cooldownManager.isOnCooldown()) return;
        if (!respChance.shouldRespond(prompt.event())) return;

        executePrompt(prompt);
    }

    public void executePrompt(GamePrompt prompt) {
        //Class<? extends Event> event = (prompt.event() != null) ? prompt.event().getClass() : null;

        plugin.getLogger().info("AI is responding to " + prompt.getSummary());

        generateResponse(prompt);
    }

    private void generateResponse(GamePrompt prompt) {
        promptHistory.add(prompt);

        OllamaClient client = new OllamaClient(modelURL, aiModel);

        currentAITask = CompletableFuture.supplyAsync(() -> {
            try {
                return client.doOllamaRequest(prompt.prompt());
            } catch (IOException | InterruptedException ex) {
                plugin.getLogger().severe("ollama request failed, error: " + ex.getMessage());
                return null;
            }}).thenAcceptAsync(response -> {
            if (response != null) {
                AIResponseEvent responseEvent = new AIResponseEvent(response);
                Bukkit.getPluginManager().callEvent(responseEvent);
                cooldownManager.resetCooldown();
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(plugin));
    }

    public List<GamePrompt> getPromptHistory() {
        return promptHistory;
    }
}

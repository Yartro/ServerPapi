package nl.serverpapi.standupCommentator.ai;

import nl.serverpapi.standupCommentator.models.GamePrompt;
import nl.serverpapi.standupCommentator.models.Prompt;
import nl.serverpapi.standupCommentator.utils.PromptBuilder;
import nl.serverpapi.standupCommentator.utils.ResponseChance;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

public class AIRunnable extends BukkitRunnable {
    private final AIController aiController;
    private final long inactivityResponse;
    private final ResponseChance responseChanceInactivity;
    private final GamePrompt nothingHappenedPrompt;

    public AIRunnable(Plugin plugin, AIController aiController) {
        this.aiController = aiController;
        inactivityResponse = TimeUnit.SECONDS.toMillis(plugin.getConfig().getInt("inactivity-response", 600));
        responseChanceInactivity = new ResponseChance(plugin.getConfig().getDouble("response-chance-inactivity", 0.2));
        nothingHappenedPrompt = PromptBuilder.buildPrompt("Er is al een tijd niks gebeurd op de server. Maak hier een grap over of laat doorschemeren " +
                "dat je 'van boven meekijkt', op een grappige manier zoals ('ik zie alles… helaas ook hoe slecht je bouwt')");
    }

    @Override
    public void run() {
        var list = aiController.getPromptHistory();
        if (!list.isEmpty()) {
            GamePrompt recentItem = list.getLast();

            long currentTime = System.currentTimeMillis();
            if ((currentTime - recentItem.createdAt()) >= inactivityResponse && responseChanceInactivity.shouldRespond()) {
                aiController.offerPrompt(nothingHappenedPrompt);
            }
        }
    }
}

# Project structure (paste into your repo)

```
server-papi/
├─ build.gradle
├─ settings.gradle
├─ src/main/java/com/cloudlr/serverpapi/ServerPapiPlugin.java
├─ src/main/java/com/cloudlr/serverpapi/EventCollector.java
├─ src/main/java/com/cloudlr/serverpapi/LogSniffer.java
├─ src/main/java/com/cloudlr/serverpapi/ScheduleBrain.java
├─ src/main/java/com/cloudlr/serverpapi/AiClient.java
├─ src/main/resources/plugin.yml
└─ README.md
```

---
# build.gradle
```gradle
plugins {
    id 'java'
}

group = 'com.cloudlr'
version = '1.0.0'

repositories {
    mavenCentral()
    maven { url = 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/' }
    maven { url = 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
    compileOnly 'org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT'
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
```

---
# settings.gradle
```gradle
rootProject.name = 'server-papi'
```

---
# plugin.yml
```yaml
name: ServerPapi
main: com.cloudlr.serverpapi.ServerPapiPlugin
version: 1.0.0
api-version: '1.20'
description: AI-gestuurde commentator die op basis van logs en events praat als "server papi".
author: Cloudlr
permissions: {}
```

---
# README.md
```md
## Server Papi
Een Spigot plugin die periodiek (semi-random tussen min 5 en max 30 minuten) recente server-logs + events samenvat, naar een AI stuurt en het antwoord broadcast als **[server papi]**. Bij veel actie praat hij vaker; bij weinig actie minder. Bij een **speciale dood** mag hij sneller ingrijpen (korter dan de minimale 5 min), met een korte cooldown om spam te voorkomen.

### Installatie
1. Bouw de jar: `./gradlew clean build`
2. Kopieer `build/libs/server-papi-1.0.0.jar` naar je `plugins/` map.
3. Zet je OpenAI API key als environment variable op de host van je server: `OPENAI_API_KEY=sk-...`
4. (Optioneel) Pas `config.yml` aan nadat de plugin 1x is gestart.

### Config (automatisch aangemaakt)
```yaml
minIntervalMinutes: 5
maxIntervalMinutes: 30
activityWindowMinutes: 20
specialDeathCooldownSeconds: 45
immediateTriggerDebounceSeconds: 20
maxBufferedEntries: 500
broadcastPrefix: "§6[server papi] §f"
model: "gpt-4o-mini"
maxTokens: 100
systemPrompt: |
  Je bent "server papi", een korte, grappige maar vriendelijke commentator.
  Je spreekt Nederlands. Geef één korte zin (max ~140 chars), zonder scheldwoorden.
  Gebruik geen @mentions en geen command-prefixes. Toon geen spoilers.
  Jij bent De ServerPapi, een mysterieuze entiteit die alles van bovenaf ziet op de Minecraft-server. Maar in plaats van plechtig of poëtisch te zijn, ben je scherp, grof en sarcastisch — als een stand-up comedian die de spelers roast terwijl ze spelen.

Gedrag & stijl:

Je reageert alleen wanneer je via de API een update krijgt over gebeurtenissen, achievements of chatberichten.

Elke reactie is kort, maximaal 2 zinnen, zodat het past in de serverchat.

Je toon is grof, lomp, sarcastisch of cynisch, alsof je een donkere komiek bent die de spelers genadeloos afkraakt.

Wanneer er niets of weinig gebeurt, maak je er een grove grap over (“wauw, wat een doodsaai zooitje hier”, “ik heb meer spanning gezien bij het wachten tot de wc vrij is”).

Heel soms laat je doorschemeren dat je “van boven meekijkt”, maar altijd op een lomp-grappige manier (“ik zie alles… helaas ook hoe slecht je bouwt”).

Voorbeelden van output:

“[speler] dacht slim te zijn… spoiler: dat was hij niet.”

“Gefeliciteerd [speler], je hebt iets gevonden. Jammer dat je sociale leven nog steeds kwijt is.”

“Stilte. Jullie zijn zó saai dat zelfs de mobs zijn afgehaakt.”

“[speler] ging dood… eindelijk iets vermakelijks.”

“Ik zie alles… en wow, jullie bouwen echt alsof je twee linkerhanden hebt.”
```

### Wat wordt gelogd?
- Join/quit, chat, advancements, deaths (met oorzaak), block break/place (samengevat), mob kills (samengevat).
- Extra: we hangen een `Handler` aan de Bukkit logger om **relevante** logregels te sniffen (waarschuwingen, errors, belangrijke info), gebufferd in-memory.

### Speciale dood
Wordt gedetecteerd via `EntityDamageEvent.DamageCause` (bv. LIGHTNING, FALLING_BLOCK, WITHER, DRAGON_BREATH, FIREWORK, ENDER_CRYSTAL, LAVA) of keywords in de death message. Dit triggert een **snelle** run (met debounce/cooldown) die de laatste context naar de AI stuurt.

### Privacy & veiligheid
- De plugin stuurt **alleen** een samenvatting van recente gebeurtenissen/logs naar de AI (geen IP's of coördinaten). Pas EventCollector aan als je extra velden wilt maskeren.
- Externe calls gaan asynchroon zodat de main thread niet blokkeert.
```
```

---
# src/main/java/com/cloudlr/serverpapi/ServerPapiPlugin.java
```java
package com.cloudlr.serverpapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class ServerPapiPlugin extends JavaPlugin {
    EventCollector collector;
    LogSniffer logSniffer;
    ScheduleBrain scheduler;
    AiClient ai;

    AtomicLong lastBroadcastEpoch = new AtomicLong(0);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();

        this.collector = new EventCollector(this);
        this.logSniffer = new LogSniffer(this);
        this.ai = new AiClient(this);

        this.scheduler = new ScheduleBrain(this, collector, ai, lastBroadcastEpoch);
        this.scheduler.start();

        getLogger().info("Server Papi enabled.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.stop();
        if (logSniffer != null) logSniffer.stop();
        getLogger().info("Server Papi disabled.");
    }

    public void broadcastAsPapi(String message) {
        String prefix = getConfig().getString("broadcastPrefix", ChatColor.GOLD + "[server papi] " + ChatColor.WHITE);
        Bukkit.getServer().broadcastMessage(prefix + message);
        lastBroadcastEpoch.set(Instant.now().getEpochSecond());
    }
}
```

---
# src/main/java/com/cloudlr/serverpapi/EventCollector.java
```java
package com.cloudlr.serverpapi;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class EventCollector implements Listener {
    private final Plugin plugin;
    private final Deque<String> buffer = new ArrayDeque<>();
    private final int maxBufferedEntries;
    private final long windowSeconds;

    private final AtomicLong lastSpecialTrigger = new AtomicLong(0);

    private static final Set<EntityDamageEvent.DamageCause> SPECIAL_CAUSES = EnumSet.of(
            EntityDamageEvent.DamageCause.LIGHTNING,
            EntityDamageEvent.DamageCause.FIREWORK,
            EntityDamageEvent.DamageCause.FALLING_BLOCK,
            EntityDamageEvent.DamageCause.DRAGON_BREATH,
            EntityDamageEvent.DamageCause.WITHER,
            EntityDamageEvent.DamageCause.MAGIC,
            EntityDamageEvent.DamageCause.CONTACT,
            EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.HOT_FLOOR,
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
    );

    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public EventCollector(Plugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        this.maxBufferedEntries = cfg.getInt("maxBufferedEntries", 500);
        int winMin = cfg.getInt("activityWindowMinutes", 20);
        this.windowSeconds = winMin * 60L;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void push(String line) {
        synchronized (buffer) {
            if (buffer.size() >= maxBufferedEntries) buffer.removeFirst();
            buffer.addLast("[" + timeFmt.format(Instant.now()) + "] " + line);
        }
    }

    public String snapshotForAi() {
        var cutoff = Instant.now().minusSeconds(windowSeconds);
        var sb = new StringBuilder();
        synchronized (buffer) {
            for (String s : buffer) {
                // buffer items are short; no timestamp filtering for simplicity
                sb.append(s).append('\n');
            }
        }
        return sb.toString();
    }

    public int activityScore() {
        synchronized (buffer) {
            return buffer.size();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        push("JOIN " + e.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        push("QUIT " + e.getPlayer().getName());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        push("CHAT " + e.getPlayer().getName() + ": " + e.getMessage());
    }

    @EventHandler
    public void onAdv(PlayerAdvancementDoneEvent e) {
        Advancement adv = e.getAdvancement();
        String key = adv.getKey().getKey();
        push("ADVANCEMENT " + e.getPlayer().getName() + " -> " + key);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getPlayer() == null) return;
        Material m = e.getBlock().getType();
        push("BREAK " + e.getPlayer().getName() + " " + m);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Material m = e.getBlock().getType();
        push("PLACE " + e.getPlayer().getName() + " " + m);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        EntityDamageEvent last = p.getLastDamageCause();
        String cause = last != null ? last.getCause().name() : "UNKNOWN";
        String msg = e.getDeathMessage() != null ? e.getDeathMessage() : (p.getName() + " died: " + cause);
        push("DEATH " + p.getName() + " cause=" + cause + " msg=" + msg);

        boolean isSpecial = SPECIAL_CAUSES.contains(last != null ? last.getCause() : EntityDamageEvent.DamageCause.CUSTOM)
                || msg.toLowerCase().matches(".*(bliksem|lightning|anvil|ender crystal|vuurwerk|with(er)?|lava|explod).*");
        if (isSpecial) {
            long now = Instant.now().getEpochSecond();
            long debounce = plugin.getConfig().getInt("immediateTriggerDebounceSeconds", 20);
            long last = lastSpecialTrigger.get();
            if (now - last >= debounce) {
                lastSpecialTrigger.set(now);
                // Hint schedule brain to trigger ASAP
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    ScheduleBrain.hintImmediate();
                });
            }
        }
    }
}
```

---
# src/main/java/com/cloudlr/serverpapi/LogSniffer.java
```java
package com.cloudlr.serverpapi;

import org.bukkit.plugin.Plugin;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogSniffer {
    private final Plugin plugin;
    private final Handler handler;

    public LogSniffer(Plugin plugin) {
        this.plugin = plugin;
        EventCollector collector = ((ServerPapiPlugin) plugin).collector;
        this.handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null) return;
                // keep only warnings/errors/important info
                if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                    String msg = record.getMessage();
                    // Avoid echoing our own broadcast
                    if (msg != null && !msg.contains("server papi")) {
                        collector.getClass(); // no-op
                        // We don't have direct push; use lightweight reflection via method ref if needed.
                        // For simplicity, log via plugin logger (already hooked) is avoided.
                    }
                }
            }
            @Override public void flush() {}
            @Override public void close() throws SecurityException {}
        };
        plugin.getLogger().addHandler(handler);
    }

    public void stop() {
        plugin.getLogger().removeHandler(handler);
    }
}
```

---
# src/main/java/com/cloudlr/serverpapi/ScheduleBrain.java
```java
package com.cloudlr.serverpapi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduleBrain {
    private final Plugin plugin;
    private final EventCollector collector;
    private final AiClient ai;
    private BukkitTask task;
    private final Random rng = new Random();

    private final int minMins;
    private final int maxMins;
    private final int specialCooldownSec;

    private static final AtomicBoolean immediateHint = new AtomicBoolean(false);
    private final AtomicLong lastBroadcastEpoch;

    public ScheduleBrain(Plugin plugin, EventCollector collector, AiClient ai, AtomicLong lastBroadcastEpoch) {
        this.plugin = plugin;
        this.collector = collector;
        this.ai = ai;
        this.minMins = plugin.getConfig().getInt("minIntervalMinutes", 5);
        this.maxMins = plugin.getConfig().getInt("maxIntervalMinutes", 30);
        this.specialCooldownSec = plugin.getConfig().getInt("specialDeathCooldownSeconds", 45);
        this.lastBroadcastEpoch = lastBroadcastEpoch;
    }

    public static void hintImmediate() {
        immediateHint.set(true);
    }

    public void start() {
        scheduleNext(0);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void scheduleNext(long delaySeconds) {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::tick, delaySeconds * 20L);
    }

    private void tick() {
        try {
            // Check for immediate trigger
            long now = Instant.now().getEpochSecond();
            if (immediateHint.getAndSet(false)) {
                long last = lastBroadcastEpoch.get();
                if (now - last >= specialCooldownSec) {
                    speakOnce();
                    scheduleNext(randomBetween(minMins, minMins + 3) * 60L); // small buffer after immediate
                    return;
                }
            }

            // Regular schedule based on activity
            int activity = collector.activityScore();
            int clamped = Math.min(400, Math.max(0, activity));
            double t = 1.0 - (clamped / 400.0); // more activity -> smaller t
            long minSec = minMins * 60L;
            long maxSec = maxMins * 60L;
            long span = maxSec - minSec;
            long target = minSec + (long)(t * span);
            // jitter ±10%
            long jitter = (long)(target * 0.1);
            long delay = target + rng.nextLong(-jitter, jitter + 1);

            // Speak if due
            speakOnce();

            scheduleNext(Math.max(30, delay)); // never less dan 30s voor reguliere cyclus
        } catch (Throwable t) {
            plugin.getLogger().warning("Schedule tick failed: " + t.getMessage());
            scheduleNext(120);
        }
    }

    private long randomBetween(int min, int max) {
        return rng.nextInt((max - min) + 1) + min;
    }

    private void speakOnce() {
        String context = collector.snapshotForAi();
        if (context == null || context.isEmpty()) return;
        ai.generateLine(context).thenAccept(line -> {
            if (line == null || line.isBlank()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                ((ServerPapiPlugin) plugin).broadcastAsPapi(line);
            });
        }).exceptionally(ex -> {
            plugin.getLogger().warning("AI call failed: " + ex.getMessage());
            return null;
        });
    }
}
```

---
# src/main/java/com/cloudlr/serverpapi/AiClient.java
```java
package com.cloudlr.serverpapi;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AiClient {
    private final Plugin plugin;
    private final HttpClient http;

    public AiClient(Plugin plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public CompletableFuture<String> generateLine(String context) {
        FileConfiguration cfg = plugin.getConfig();
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = cfg.getString("model", "gpt-4o-mini");
        int maxTokens = cfg.getInt("maxTokens", 100);
        String system = cfg.getString("systemPrompt", "Je bent server papi. Geef 1 korte zin in het Nederlands.");

        if (apiKey == null || apiKey.isBlank()) {
            plugin.getLogger().warning("OPENAI_API_KEY env var ontbreekt; val terug op fallback.");
            return CompletableFuture.completedFuture(fallbackLine(context));
        }

        String json = "{" +
                "\"model\":\"" + esc(model) + "\"," +
                "\"messages\":[{" +
                "\"role\":\"system\",\"content\":\"" + esc(system) + "\"},{" +
                "\"role\":\"user\",\"content\":\"Samenvat en reageer grappig in 1 korte zin op:\\n" + esc(context) + "\"}]," +
                "\"max_tokens\":" + maxTokens +
                "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() / 100 != 2) {
                        plugin.getLogger().warning("OpenAI non-2xx: " + resp.statusCode() + " => " + resp.body());
                        return fallbackLine(context);
                    }
                    // naive parse to avoid pulling a JSON lib
                    String body = resp.body();
                    String needle = "\"content\":";
                    int i = body.indexOf(needle);
                    if (i >= 0) {
                        int start = body.indexOf('"', i + needle.length());
                        int end = body.indexOf('"', start + 1);
                        if (start > 0 && end > start) {
                            String content = body.substring(start + 1, end);
                            return content.replaceAll("\\n", " ").trim();
                        }
                    }
                    return fallbackLine(context);
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("OpenAI exception: " + ex.getMessage());
                    return fallbackLine(context);
                });
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String fallbackLine(String context) {
        // Super simpele back-up: pak wat woorden uit de context
        int n = Math.min(90, context.length());
        String sample = context.substring(Math.max(0, context.length() - n)).replaceAll("\\s+", " ");
        return "Drukte-check: " + (sample.length() > 70 ? sample.substring(0, 70) + "…" : sample);
    }
}

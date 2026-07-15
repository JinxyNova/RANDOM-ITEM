package fr.merci.randomfind;

import fr.merci.randomfind.util.FloodgateUtil;
import fr.merci.randomfind.util.MaterialUtil;
import fr.merci.randomfind.util.TeleportUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameManager implements Listener {

    private final RandomFindPlugin plugin;
    private final Set<UUID> registered = new LinkedHashSet<>();
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, Material> targets = new HashMap<>();
    private final Map<UUID, Material> previousTargets = new HashMap<>();

    private boolean running = false;
    private int round = 0;
    private BukkitTask scanTask;
    private BukkitTask nextRoundTask;
    private BukkitTask timeoutTask;

    private String worldName;
    private double minRadius;
    private double maxRadius;
    private int winScore;
    private int roundTimeSeconds;
    private int rareChance;
    private boolean preventTrading;
    private boolean restrictToSafeList;

    private Scoreboard board;
    private Objective objective;

    private File statsFile;
    private FileConfiguration stats;

    public GameManager(RandomFindPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadConfig();
        loadStats();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.worldName = plugin.getConfig().getString("world", "world");
        this.minRadius = plugin.getConfig().getDouble("min-radius", 1000);
        this.maxRadius = plugin.getConfig().getDouble("max-radius", 10000);
        this.winScore = plugin.getConfig().getInt("win-score", 5);
        this.roundTimeSeconds = plugin.getConfig().getInt("round-time-seconds", 90);
        this.rareChance = plugin.getConfig().getInt("rare-chance", 8);
        this.preventTrading = plugin.getConfig().getBoolean("prevent-trading", true);
        this.restrictToSafeList = plugin.getConfig().getBoolean("restrict-to-safe-list", true);
    }

    private void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Impossible de creer stats.yml : " + e.getMessage());
            }
        }
        stats = YamlConfiguration.loadConfiguration(statsFile);
    }

    private void saveStats() {
        try {
            stats.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder stats.yml : " + e.getMessage());
        }
    }

    private World getGameWorld() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            w = Bukkit.getWorlds().get(0);
        }
        return w;
    }

    public boolean isRunning() {
        return running;
    }

    // ----------------------------------------------------------------
    // Inscription
    // ----------------------------------------------------------------

    public void join(Player p) {
        if (running) {
            p.sendMessage(Component.text("§cUne partie est deja en cours, attends la fin pour rejoindre."));
            return;
        }
        registered.add(p.getUniqueId());
        scores.putIfAbsent(p.getUniqueId(), 0);
        Bukkit.broadcast(Component.text("§a" + p.getName() + " §frejoint RandomFind ! (§e" + registered.size() + " joueur(s)§f)"));
    }

    public void leave(Player p) {
        removePlayer(p.getUniqueId(), p.getName());
    }

    public void kick(CommandSender sender, String name) {
        UUID found = null;
        for (UUID id : registered) {
            Player online = Bukkit.getPlayer(id);
            if (online != null && online.getName().equalsIgnoreCase(name)) {
                found = id;
                break;
            }
        }
        if (found == null) {
            sender.sendMessage(Component.text("§cAucun joueur inscrit ne s'appelle " + name + "."));
            return;
        }
        removePlayer(found, name);
        sender.sendMessage(Component.text("§a" + name + " a ete retire de la partie."));
    }

    private void removePlayer(UUID id, String name) {
        registered.remove(id);
        targets.remove(id);
        previousTargets.remove(id);
        Bukkit.broadcast(Component.text("§c" + name + " §fquitte RandomFind."));
        if (running && registered.size() < 2) {
            stopGame(true, "§cPlus assez de joueurs, partie arretee.");
        }
    }

    // ----------------------------------------------------------------
    // Deroulement de la partie
    // ----------------------------------------------------------------

    public void startGame(CommandSender sender) {
        if (running) {
            sender.sendMessage(Component.text("§cUne partie est deja en cours."));
            return;
        }
        if (registered.size() < 2) {
            sender.sendMessage(Component.text("§cIl faut au moins 2 joueurs inscrits (§e/rf join§c)."));
            return;
        }
        loadConfig();
        running = true;
        round = 0;
        for (UUID id : registered) scores.put(id, 0);
        setupScoreboard();
        Bukkit.broadcast(Component.text("§6§lRandomFind §fdemarre avec " + registered.size() + " joueurs !"));
        startRound();
    }

    private void startRound() {
        round++;
        targets.clear();
        cancelTimers();

        World world = getGameWorld();
        Location center = world.getSpawnLocation();

        List<UUID> current = registered.stream()
                .filter(id -> Bukkit.getPlayer(id) != null)
                .collect(Collectors.toList());

        if (current.size() < 2) {
            stopGame(true, "§cPlus assez de joueurs connectes, partie arretee.");
            return;
        }

        List<Location> usedLocations = new ArrayList<>();

        for (UUID id : current) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            Material target;
            int guard = 0;
            do {
                target = MaterialUtil.randomFindable(rareChance, restrictToSafeList);
                guard++;
            } while (target.equals(previousTargets.get(id)) && guard < 5);
            targets.put(id, target);
            previousTargets.put(id, target);

            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);

            Location loc = TeleportUtil.findSafeRandomLocation(world, center, minRadius, maxRadius, usedLocations, 200);
            usedLocations.add(loc);
            p.teleport(loc);
            p.setScoreboard(board);

            p.sendTitle("§6Manche " + round, "§eTrouve : §f" + MaterialUtil.formatName(target), 10, 70, 20);
        }

        updateScoreboard();
        String timeInfo = roundTimeSeconds > 0 ? (" (§e" + roundTimeSeconds + "s§f)") : "";
        Bukkit.broadcast(Component.text("§6§lManche " + round + " §f- chacun a un objet different a trouver, a vos pioches !" + timeInfo));

        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Material> entry : new HashMap<>(targets).entrySet()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p == null) continue;
                Material target = entry.getValue();
                p.sendActionBar(Component.text("§eObjectif : §f" + MaterialUtil.formatName(target)));
                if (p.getInventory().contains(target)) {
                    handleFound(p, target);
                    break;
                }
            }
        }, 10L, 10L);

        if (roundTimeSeconds > 0) {
            timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.broadcast(Component.text("§7Temps ecoule pour la manche " + round + ", nouveaux objets pour tout le monde !"));
                startRound();
            }, roundTimeSeconds * 20L);
        }
    }

    private void handleFound(Player winner, Material target) {
        cancelTimers();
        UUID id = winner.getUniqueId();
        int newScore = scores.merge(id, 1, Integer::sum);

        winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        spawnFirework(winner.getLocation());

        Bukkit.broadcast(Component.text("§b" + winner.getName() + " §fa trouve son objet (§e"
                + MaterialUtil.formatName(target) + "§f) ! Score : §a" + newScore));
        updateScoreboard();

        if (newScore >= winScore) {
            endGame(winner);
        } else {
            nextRoundTask = Bukkit.getScheduler().runTaskLater(plugin, this::startRound, 60L);
        }
    }

    private void spawnFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.AQUA)
                .with(org.bukkit.FireworkEffect.Type.BALL)
                .trail(true)
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        fw.detonate();
    }

    private void endGame(Player winner) {
        running = false;
        cancelTimers();
        Bukkit.broadcast(Component.text("§6§l" + winner.getName() + " remporte la partie RandomFind !"));
        sendScoresBroadcast();

        int wins = stats.getInt(winner.getUniqueId().toString() + ".wins", 0) + 1;
        stats.set(winner.getUniqueId().toString() + ".wins", wins);
        stats.set(winner.getUniqueId().toString() + ".name", winner.getName());
        saveStats();

        for (UUID id : registered) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        scores.replaceAll((k, v) -> 0);
    }

    public void stopGame(boolean announce) {
        stopGame(announce, "§cPartie RandomFind arretee.");
    }

    public void stopGame(boolean announce, String message) {
        running = false;
        cancelTimers();
        for (UUID id : registered) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        if (announce) Bukkit.broadcast(Component.text(message));
    }

    private void cancelTimers() {
        if (scanTask != null) scanTask.cancel();
        if (nextRoundTask != null) nextRoundTask.cancel();
        if (timeoutTask != null) timeoutTask.cancel();
    }

    // ----------------------------------------------------------------
    // Scoreboard
    // ----------------------------------------------------------------

    private void setupScoreboard() {
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = board.registerNewObjective("rf", Criteria.DUMMY, Component.text("§6§lRandomFind"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void updateScoreboard() {
        if (board == null || objective == null) return;
        for (String entry : new ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }
        objective.getScoreboard().getObjective("rf").displayName(Component.text("§6§lRandomFind - Manche " + round));
        registered.stream()
                .sorted((a, b) -> scores.getOrDefault(b, 0) - scores.getOrDefault(a, 0))
                .limit(15)
                .forEach(id -> {
                    Player p = Bukkit.getPlayer(id);
                    String name = p != null ? p.getName() : "?";
                    objective.getScore("§e" + name).setScore(scores.getOrDefault(id, 0));
                });
    }

    // ----------------------------------------------------------------
    // Informations / commandes de lecture
    // ----------------------------------------------------------------

    public void sendScores(CommandSender sender) {
        if (registered.isEmpty()) {
            sender.sendMessage(Component.text("§7Aucun joueur inscrit."));
            return;
        }
        sender.sendMessage(Component.text("§6=== Scores RandomFind ==="));
        registered.stream()
                .sorted((a, b) -> scores.getOrDefault(b, 0) - scores.getOrDefault(a, 0))
                .forEach(id -> {
                    Player p = Bukkit.getPlayer(id);
                    String name = p != null ? p.getName() : id.toString();
                    sender.sendMessage(Component.text("§e" + name + " §f: §a" + scores.getOrDefault(id, 0)));
                });
    }

    private void sendScoresBroadcast() {
        Bukkit.broadcast(Component.text("§6=== Scores finaux ==="));
        registered.stream()
                .sorted((a, b) -> scores.getOrDefault(b, 0) - scores.getOrDefault(a, 0))
                .forEach(id -> {
                    Player p = Bukkit.getPlayer(id);
                    String name = p != null ? p.getName() : id.toString();
                    Bukkit.broadcast(Component.text("§e" + name + " §f: §a" + scores.getOrDefault(id, 0)));
                });
    }

    public void sendInfo(CommandSender sender) {
        sender.sendMessage(Component.text("§6=== RandomFind - Etat ==="));
        sender.sendMessage(Component.text("§fStatut : " + (running ? "§aen cours (manche " + round + ")" : "§7en attente")));
        sender.sendMessage(Component.text("§fJoueurs inscrits : §e" + registered.size()));
        sender.sendMessage(Component.text("§fMonde : §e" + worldName + " §f| Rayon RTP : §e" + (int) minRadius + " - " + (int) maxRadius));
        sender.sendMessage(Component.text("§fScore pour gagner : §e" + winScore));
        sender.sendMessage(Component.text("§fTemps par manche : §e" + (roundTimeSeconds > 0 ? roundTimeSeconds + "s" : "illimite")));
        sender.sendMessage(Component.text("§fChance objet rare : §e" + rareChance + "%"));
        sender.sendMessage(Component.text("§fMode crossplay (liste equitable Java/Bedrock) : " + (restrictToSafeList ? "§aactive" : "§7desactive")));
        sender.sendMessage(Component.text("§fFloodgate detecte : " + (FloodgateUtil.isFloodgateAvailable() ? "§aoui" : "§7non")));
    }

    public void sendList(CommandSender sender) {
        if (registered.isEmpty()) {
            sender.sendMessage(Component.text("§7Aucun joueur inscrit."));
            return;
        }
        sender.sendMessage(Component.text("§6Inscrits (" + registered.size() + ") :"));
        String names = registered.stream()
                .map(id -> {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) return "?";
                    String tag = FloodgateUtil.isBedrockPlayer(p) ? " §7(Bedrock)" : "";
                    return p.getName() + tag;
                })
                .collect(Collectors.joining("§f, §e"));
        sender.sendMessage(Component.text("§e" + names));
    }

    public void sendTop(CommandSender sender) {
        if (stats.getKeys(false).isEmpty()) {
            sender.sendMessage(Component.text("§7Aucune victoire enregistree pour le moment."));
            return;
        }
        sender.sendMessage(Component.text("§6=== Classement general RandomFind ==="));
        stats.getKeys(false).stream()
                .map(key -> Map.entry(stats.getString(key + ".name", key), stats.getInt(key + ".wins", 0)))
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(10)
                .forEach(e -> sender.sendMessage(Component.text("§e" + e.getKey() + " §f: §a" + e.getValue() + " victoire(s)")));
    }

    public void reload(CommandSender sender) {
        loadConfig();
        sender.sendMessage(Component.text("§aConfiguration RandomFind rechargee."));
    }

    // ----------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------

    public void setRadius(double minRadius, double maxRadius) {
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        plugin.getConfig().set("min-radius", minRadius);
        plugin.getConfig().set("max-radius", maxRadius);
        plugin.saveConfig();
    }

    public void setWorld(String worldName) {
        this.worldName = worldName;
        plugin.getConfig().set("world", worldName);
        plugin.saveConfig();
    }

    public void setWinScore(int score) {
        this.winScore = score;
        plugin.getConfig().set("win-score", score);
        plugin.saveConfig();
    }

    public void setRoundTime(int seconds) {
        this.roundTimeSeconds = seconds;
        plugin.getConfig().set("round-time-seconds", seconds);
        plugin.saveConfig();
    }

    public void setRestrictToSafeList(boolean value) {
        this.restrictToSafeList = value;
        plugin.getConfig().set("restrict-to-safe-list", value);
        plugin.saveConfig();
    }

    // ----------------------------------------------------------------
    // Evenements
    // ----------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (registered.contains(p.getUniqueId())) {
            targets.remove(p.getUniqueId());
            if (running && registered.size() - 1 < 2) {
                stopGame(true, "§cPlus assez de joueurs connectes, partie arretee.");
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!running || !preventTrading) return;
        Material type = event.getItemDrop().getItemStack().getType();
        if (targets.containsValue(type)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("§cTu ne peux pas jeter cet objet pendant la manche (ca eviterait a quelqu'un de te le refiler) !"));
        }
    }
}

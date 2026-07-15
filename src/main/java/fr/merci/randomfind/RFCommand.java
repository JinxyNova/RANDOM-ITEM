package fr.merci.randomfind;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RFCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "join", "leave", "start", "stop", "score", "info", "list", "top",
            "kick", "reload", "setradius", "setworld", "setwinscore", "setroundtime", "crossplay", "help");

    private final RandomFindPlugin plugin;
    private final GameManager gameManager;

    public RFCommand(RandomFindPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "join": {
                if (!requirePlayer(sender)) return true;
                gameManager.join((Player) sender);
                return true;
            }
            case "leave": {
                if (!requirePlayer(sender)) return true;
                gameManager.leave((Player) sender);
                return true;
            }
            case "start": {
                if (!hasAdmin(sender)) return true;
                gameManager.startGame(sender);
                return true;
            }
            case "stop": {
                if (!hasAdmin(sender)) return true;
                gameManager.stopGame(true);
                return true;
            }
            case "score":
            case "scores": {
                gameManager.sendScores(sender);
                return true;
            }
            case "info": {
                gameManager.sendInfo(sender);
                return true;
            }
            case "list": {
                gameManager.sendList(sender);
                return true;
            }
            case "top": {
                gameManager.sendTop(sender);
                return true;
            }
            case "kick": {
                if (!hasAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§cUsage: /rf kick <joueur>"));
                    return true;
                }
                gameManager.kick(sender, args[1]);
                return true;
            }
            case "reload": {
                if (!hasAdmin(sender)) return true;
                gameManager.reload(sender);
                return true;
            }
            case "setradius": {
                if (!hasAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(Component.text("§cUsage: /rf setradius <min> <max>"));
                    return true;
                }
                try {
                    double min = Double.parseDouble(args[1]);
                    double max = Double.parseDouble(args[2]);
                    if (min >= max) {
                        sender.sendMessage(Component.text("§cLe minimum doit etre plus petit que le maximum."));
                        return true;
                    }
                    gameManager.setRadius(min, max);
                    sender.sendMessage(Component.text("§aRayon RTP defini entre " + (int) min + " et " + (int) max + " blocs."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("§cNombre invalide."));
                }
                return true;
            }
            case "setworld": {
                if (!hasAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§cUsage: /rf setworld <nom_du_monde>"));
                    return true;
                }
                gameManager.setWorld(args[1]);
                sender.sendMessage(Component.text("§aMonde defini sur " + args[1] + "."));
                return true;
            }
            case "setwinscore": {
                if (!hasAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§cUsage: /rf setwinscore <nombre>"));
                    return true;
                }
                try {
                    int score = Integer.parseInt(args[1]);
                    gameManager.setWinScore(score);
                    sender.sendMessage(Component.text("§aScore de victoire defini a " + score + "."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("§cNombre invalide."));
                }
                return true;
            }
            case "setroundtime": {
                if (!hasAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§cUsage: /rf setroundtime <secondes> (0 = illimite)"));
                    return true;
                }
                try {
                    int seconds = Integer.parseInt(args[1]);
                    gameManager.setRoundTime(seconds);
                    sender.sendMessage(Component.text("§aTemps par manche defini a " + (seconds > 0 ? seconds + "s" : "illimite") + "."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("§cNombre invalide."));
                }
                return true;
            }
            case "crossplay": {
                if (!hasAdmin(sender)) return true;
                if (args.length < 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
                    sender.sendMessage(Component.text("§cUsage: /rf crossplay <on|off>"));
                    return true;
                }
                boolean on = args[1].equalsIgnoreCase("on");
                gameManager.setRestrictToSafeList(on);
                sender.sendMessage(Component.text(on
                        ? "§aMode crossplay active : seuls des objets equitables Java/Bedrock seront tires."
                        : "§7Mode crossplay desactive : tous les objets du jeu peuvent etre tires."));
                return true;
            }
            case "help": {
                sendHelp(sender);
                return true;
            }
            default: {
                sendUsage(sender);
                return true;
            }
        }
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("§cCommande reservee aux joueurs."));
            return false;
        }
        return true;
    }

    private boolean hasAdmin(CommandSender sender) {
        if (!sender.hasPermission("randomfind.admin")) {
            sender.sendMessage(Component.text("§cTu n'as pas la permission."));
            return false;
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("§6Usage: §e/rf help §fpour la liste complete des commandes"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("§6§l=== RandomFind - Commandes ==="));
        sender.sendMessage(Component.text("§e/rf join §f- s'inscrire pour la prochaine partie"));
        sender.sendMessage(Component.text("§e/rf leave §f- se retirer"));
        sender.sendMessage(Component.text("§e/rf score §f- voir les scores de la partie en cours"));
        sender.sendMessage(Component.text("§e/rf info §f- etat de la partie (manche, joueurs, config)"));
        sender.sendMessage(Component.text("§e/rf list §f- liste des joueurs inscrits"));
        sender.sendMessage(Component.text("§e/rf top §f- classement general (victoires totales)"));
        if (sender.hasPermission("randomfind.admin")) {
            sender.sendMessage(Component.text("§c/rf start §7- lancer la partie"));
            sender.sendMessage(Component.text("§c/rf stop §7- arreter la partie"));
            sender.sendMessage(Component.text("§c/rf kick <joueur> §7- retirer un joueur inscrit"));
            sender.sendMessage(Component.text("§c/rf reload §7- recharger la config"));
            sender.sendMessage(Component.text("§c/rf setradius <min> <max> §7- distance de teleportation (RTP)"));
            sender.sendMessage(Component.text("§c/rf setworld <nom> §7- monde utilise"));
            sender.sendMessage(Component.text("§c/rf setwinscore <n> §7- score pour gagner"));
            sender.sendMessage(Component.text("§c/rf setroundtime <s> §7- temps limite par manche (0 = illimite)"));
            sender.sendMessage(Component.text("§c/rf crossplay <on|off> §7- limiter le tirage aux objets equitables Java/Bedrock"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setworld")) {
                return Bukkit.getWorlds().stream().map(w -> w.getName()).collect(Collectors.toList());
            }
            if (sub.equals("kick")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}

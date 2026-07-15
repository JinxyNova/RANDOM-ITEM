package fr.merci.randomfind.util;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MaterialUtil {

    // Blocs/objets techniques ou non-obtenables en jeu normal : exclus de tout tirage
    private static final Set<Material> BLACKLIST = new HashSet<>(Arrays.asList(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.BARRIER, Material.STRUCTURE_VOID, Material.STRUCTURE_BLOCK,
            Material.JIGSAW, Material.COMMAND_BLOCK, Material.COMMAND_BLOCK_MINECART,
            Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
            Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK, Material.BEDROCK,
            Material.LIGHT, Material.END_PORTAL_FRAME, Material.SPAWNER,
            Material.MOVING_PISTON, Material.END_GATEWAY, Material.REINFORCED_DEEPSLATE,
            Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD
    ));

    // Objets consideres "rares" : tirage moins frequent (voir rare-chance dans config.yml)
    private static final Set<Material> RARE = EnumSet.of(
            Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK, Material.NETHERITE_SCRAP,
            Material.ANCIENT_DEBRIS, Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE,
            Material.NETHERITE_AXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS, Material.ELYTRA, Material.DRAGON_EGG, Material.NETHER_STAR,
            Material.BEACON, Material.ENCHANTED_GOLDEN_APPLE, Material.TOTEM_OF_UNDYING,
            Material.TRIDENT, Material.SHULKER_SHELL, Material.HEART_OF_THE_SEA,
            Material.NAUTILUS_SHELL, Material.DRAGON_HEAD, Material.WITHER_SKELETON_SKULL,
            Material.CONDUIT, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK
    );

    /**
     * Liste volontairement restreinte aux blocs/objets qui existent et
     * s'obtiennent de la meme facon sur Java ET sur Bedrock (via Geyser).
     * Sert a garder le jeu equitable en crossplay : un objet exclusif a une
     * seule edition (ou trop recent pour etre deja traduit par Geyser) ne
     * doit pas tomber, sinon les joueurs de l'autre edition ne pourraient
     * jamais le trouver.
     */
    private static final Set<Material> SAFE_POOL = EnumSet.of(
            // Bois
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS, Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.CRIMSON_PLANKS, Material.WARPED_PLANKS,
            // Blocs communs
            Material.COBBLESTONE, Material.STONE, Material.DIRT, Material.GRASS_BLOCK,
            Material.SAND, Material.RED_SAND, Material.SANDSTONE, Material.GRAVEL, Material.CLAY,
            Material.GLASS, Material.GLASS_PANE, Material.OBSIDIAN, Material.NETHERRACK,
            Material.SOUL_SAND, Material.SOUL_SOIL, Material.END_STONE, Material.BOOKSHELF,
            Material.GLOWSTONE, Material.ICE, Material.PACKED_ICE, Material.SNOW_BLOCK,
            Material.MOSS_BLOCK, Material.MUD, Material.TERRACOTTA, Material.WHITE_WOOL,
            Material.RED_WOOL, Material.BLUE_WOOL, Material.BLACK_WOOL, Material.WHITE_CONCRETE,
            Material.RED_CONCRETE, Material.BLUE_CONCRETE,
            // Minerais / lingots
            Material.COAL, Material.CHARCOAL, Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT, Material.RAW_COPPER, Material.COPPER_INGOT,
            Material.DIAMOND, Material.EMERALD, Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.COAL_ORE, Material.COPPER_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE,
            // Nourriture
            Material.APPLE, Material.BREAD, Material.COOKED_BEEF, Material.COOKED_CHICKEN,
            Material.COOKED_PORKCHOP, Material.CARROT, Material.POTATO, Material.BAKED_POTATO,
            Material.MELON_SLICE, Material.PUMPKIN_PIE, Material.COOKIE, Material.CAKE,
            Material.GOLDEN_CARROT, Material.BEETROOT, Material.BEETROOT_SOUP, Material.MUSHROOM_STEW,
            Material.WHEAT, Material.WHEAT_SEEDS, Material.SUGAR_CANE, Material.SUGAR,
            // Drops de mobs
            Material.LEATHER, Material.FEATHER, Material.BONE, Material.STRING,
            Material.GUNPOWDER, Material.ENDER_PEARL, Material.BLAZE_ROD, Material.SLIME_BALL,
            Material.ROTTEN_FLESH, Material.SPIDER_EYE, Material.RABBIT_FOOT, Material.INK_SAC,
            Material.PHANTOM_MEMBRANE, Material.HONEYCOMB, Material.EGG, Material.MILK_BUCKET,
            // Outils et armes (par tier, hors netherite deja dans la liste rare)
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD,
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL,
            Material.BOW, Material.ARROW, Material.SHIELD, Material.FISHING_ROD,
            Material.LEATHER_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET,
            Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE,
            // Redstone
            Material.REDSTONE_TORCH, Material.REPEATER, Material.COMPARATOR, Material.PISTON,
            Material.STICKY_PISTON, Material.LEVER, Material.STONE_BUTTON, Material.STONE_PRESSURE_PLATE,
            Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.OBSERVER, Material.TNT,
            // Utilitaire
            Material.CHEST, Material.FURNACE, Material.CRAFTING_TABLE, Material.LADDER,
            Material.BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.SHEARS,
            Material.FLINT_AND_STEEL, Material.COMPASS, Material.CLOCK, Material.MAP,
            Material.BOOK, Material.PAPER, Material.SADDLE, Material.LEAD, Material.NAME_TAG,
            Material.OAK_BOAT, Material.MINECART, Material.RAIL, Material.TORCH, Material.LANTERN,
            // Plantes / nature
            Material.OAK_SAPLING, Material.CACTUS, Material.PUMPKIN, Material.MELON,
            Material.COCOA_BEANS, Material.SWEET_BERRIES, Material.KELP, Material.BAMBOO,
            Material.DANDELION, Material.POPPY, Material.SUNFLOWER, Material.LILY_PAD,
            // Nether / End
            Material.NETHER_WART, Material.QUARTZ, Material.END_ROD, Material.CHORUS_FRUIT,
            Material.MAGMA_CREAM, Material.GLOWSTONE_DUST, Material.BLAZE_POWDER
    );

    private static final List<Material> NORMAL_POOL = Arrays.stream(Material.values())
            .filter(m -> !m.isLegacy())
            .filter(Material::isItem)
            .filter(m -> !BLACKLIST.contains(m))
            .filter(m -> !RARE.contains(m))
            .collect(Collectors.toList());

    private static final List<Material> RARE_POOL = Arrays.stream(Material.values())
            .filter(m -> !m.isLegacy())
            .filter(Material::isItem)
            .filter(m -> !BLACKLIST.contains(m))
            .filter(RARE::contains)
            .collect(Collectors.toList());

    private static final List<Material> SAFE_NORMAL_POOL = SAFE_POOL.stream()
            .filter(m -> !RARE.contains(m))
            .collect(Collectors.toList());

    private static final List<Material> SAFE_RARE_POOL = SAFE_POOL.stream()
            .filter(RARE::contains)
            .collect(Collectors.toList());

    /**
     * Tire un materiau. rareChancePercent (0-100) est la probabilite de tomber
     * sur un objet "rare". Si restrictToSafeList est vrai, le tirage se limite
     * a la liste equitable Java/Bedrock (recommande des qu'il y a des joueurs
     * Bedrock via Geyser/Floodgate sur le serveur).
     */
    public static Material randomFindable(int rareChancePercent, boolean restrictToSafeList) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Material> normal = restrictToSafeList ? SAFE_NORMAL_POOL : NORMAL_POOL;
        List<Material> rare = restrictToSafeList ? SAFE_RARE_POOL : RARE_POOL;

        if (rareChancePercent > 0 && !rare.isEmpty() && rnd.nextInt(100) < rareChancePercent) {
            return rare.get(rnd.nextInt(rare.size()));
        }
        return normal.get(rnd.nextInt(normal.size()));
    }

    /**
     * Transforme DIAMOND_SWORD en "Diamond Sword" pour l'affichage.
     */
    public static String formatName(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}

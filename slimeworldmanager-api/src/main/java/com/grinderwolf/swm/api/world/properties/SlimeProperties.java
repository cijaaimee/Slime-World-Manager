package com.grinderwolf.swm.api.world.properties;

/**
 * Class with all existing properties.
 */
public class SlimeProperties {

    public static final SlimeProperty SPAWN_X = new SlimeProperty("spawnX", PropertyType.INT, 0);
    public static final SlimeProperty SPAWN_Y = new SlimeProperty("spawnY", PropertyType.INT, 255);
    public static final SlimeProperty SPAWN_Z = new SlimeProperty("spawnZ", PropertyType.INT, 0);

    public static final SlimeProperty DIFFICULTY = new SlimeProperty("difficulty", PropertyType.STRING, "peaceful", (value) -> {

        String difficulty = (String) value;
        return difficulty.equalsIgnoreCase("peaceful") || difficulty.equalsIgnoreCase("easy")
                || difficulty.equalsIgnoreCase("normal") || difficulty.equalsIgnoreCase("hard");

    });

    public static final SlimeProperty ALLOW_MONSTERS = new SlimeProperty("allowMonsters", PropertyType.BOOLEAN, true);
    public static final SlimeProperty ALLOW_ANIMALS = new SlimeProperty("allowAnimals", PropertyType.BOOLEAN, true);

    public static final SlimeProperty PVP = new SlimeProperty("pvp", PropertyType.BOOLEAN, true);

    public static final SlimeProperty ENVIRONMENT = new SlimeProperty("environment", PropertyType.STRING, "normal", (value) -> {

        String env = (String) value;
        return env.equalsIgnoreCase("normal") || env.equalsIgnoreCase("nether") || env.equalsIgnoreCase("the_end");

    });

    public static final SlimeProperty WORLD_TYPE = new SlimeProperty("worldtype", PropertyType.STRING, "default", (value) -> {

        String worldType = (String) value;
        return worldType.equalsIgnoreCase("default") || worldType.equalsIgnoreCase("flat") || worldType.equalsIgnoreCase("large_biomes")
                || worldType.equalsIgnoreCase("amplified") || worldType.equalsIgnoreCase("customized")
                || worldType.equalsIgnoreCase("debug_all_block_states") || worldType.equalsIgnoreCase("default_1_1");

    });

    public static final SlimeProperty[] VALUES = { SPAWN_X, SPAWN_Y, SPAWN_Z, DIFFICULTY, ALLOW_MONSTERS, ALLOW_ANIMALS, PVP, ENVIRONMENT, WORLD_TYPE };
}

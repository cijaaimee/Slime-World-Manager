package com.grinderwolf.swm.plugin.config;

import com.grinderwolf.swm.api.world.SlimeWorld;
import lombok.Getter;
import lombok.Setter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.bukkit.Difficulty;

@Getter
@ConfigSerializable
public class WorldData {

    @Setting("source")
    @Setter
    private String dataSource = "file";

    @Setting("spawn")
    private String spawn = "0, 255, 0";

    @Setting("difficulty")
    private String difficulty = "peaceful";

    @Setting("allowMonsters")
    private boolean allowMonsters = true;
    @Setting("allowAnimals")
    private boolean allowAnimals = true;

    @Setting("pvp")
    private boolean pvp = true;

    @Setting("loadOnStartup")
    private boolean loadOnStartup = true;
    @Setting("readOnly")
    private boolean readOnly = false;

    public SlimeWorld.SlimeProperties toProperties() {
        Difficulty difficulty;

        try {
            difficulty = Enum.valueOf(Difficulty.class, this.difficulty.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown difficulty '" + this.difficulty + "'");
        }

        String[] spawnLocationSplit = spawn.split(", ");

        double spawnX, spawnY, spawnZ;

        try {
            spawnX = Double.parseDouble(spawnLocationSplit[0]);
            spawnY = Double.parseDouble(spawnLocationSplit[1]);
            spawnZ = Double.parseDouble(spawnLocationSplit[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("invalid spawn location '" + spawn + "'");
        }

        return SlimeWorld.SlimeProperties.builder().spawnX(spawnX).spawnY(spawnY).spawnZ(spawnZ).difficulty(difficulty.getValue())
                .allowMonsters(allowMonsters).allowAnimals(allowAnimals).pvp(pvp).readOnly(readOnly).build();
    }
}

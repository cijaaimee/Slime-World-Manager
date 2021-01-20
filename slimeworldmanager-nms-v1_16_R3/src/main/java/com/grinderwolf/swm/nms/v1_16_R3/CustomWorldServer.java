package com.grinderwolf.swm.nms.v1_16_R3;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_16_R3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.SpigotTimings;
import org.bukkit.event.world.WorldSaveEvent;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

public class CustomWorldServer extends WorldServer {

    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());
    private static final TicketType<Unit> SWM_TICKET = TicketType.a("swm-chunk", (a, b) -> 0);

    @Getter
    private final CraftSlimeWorld slimeWorld;
    private final Object saveLock = new Object();
    private final BiomeBase defaultBiome;

    @Getter
    @Setter
    private boolean ready = false;

    public CustomWorldServer(CraftSlimeWorld world, IWorldDataServer worldData,
                             ResourceKey<World> worldKey, ResourceKey<WorldDimension> dimensionKey,
                             DimensionManager dimensionManager, ChunkGenerator chunkGenerator,
                             org.bukkit.World.Environment environment) throws IOException {
        super(MinecraftServer.getServer(), MinecraftServer.getServer().executorService,
                v1_16_R3SlimeNMS.CONVERTABLE.c(world.getName(), dimensionKey),
                worldData, worldKey, dimensionManager, MinecraftServer.getServer().worldLoadListenerFactory.create(11),
                chunkGenerator, false, 0, new ArrayList<>(), true, environment, null);

        this.slimeWorld = world;

        SlimePropertyMap propertyMap = world.getPropertyMap();

        worldDataServer.setDifficulty(EnumDifficulty.valueOf(propertyMap.getValue(SlimeProperties.DIFFICULTY).toUpperCase()));
        worldDataServer.setSpawn(new BlockPosition(propertyMap.getValue(SlimeProperties.SPAWN_X), propertyMap.getValue(SlimeProperties.SPAWN_Y), propertyMap.getValue(SlimeProperties.SPAWN_Z)), 0);
        super.setSpawnFlags(propertyMap.getValue(SlimeProperties.ALLOW_MONSTERS), propertyMap.getValue(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getValue(SlimeProperties.PVP);

        String biomeStr = slimeWorld.getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME);
        ResourceKey<BiomeBase> biomeKey = ResourceKey.a(IRegistry.ay, new MinecraftKey(biomeStr));
        defaultBiome = MinecraftServer.getServer().getCustomRegistry().b(IRegistry.ay).a(biomeKey);
    }

    @Override
    public void save(IProgressUpdate progressUpdate, boolean forceSave, boolean flag1) {
        if (!slimeWorld.isReadOnly() && !flag1) {
            if (forceSave) { // TODO Is this really 'forceSave'? Doesn't look like it tbh
                Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));
            }

            this.getChunkProvider().save(forceSave);
            this.worldDataServer.a(this.getWorldBorder().t());
            this.worldDataServer.setCustomBossEvents(MinecraftServer.getServer().getBossBattleCustomData().save());

            // Update level data
            NBTTagCompound compound = new NBTTagCompound();
            worldDataServer.a(MinecraftServer.getServer().getCustomRegistry(), compound);
            slimeWorld.getExtraData().getValue().put(Converter.convertTag("LevelData", compound));

            if (MinecraftServer.getServer().isStopped()) { // Make sure the world gets saved before stopping the server by running it from the main thread
                save();

                // Have to manually unlock the world as well
                try {
                    slimeWorld.getLoader().unlockWorld(slimeWorld.getName());
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (UnknownWorldException ignored) {

                }
            } else {
                WORLD_SAVER_SERVICE.execute(this::save);
            }
        }
    }

    private void save() {
        synchronized (saveLock) { // Don't want to save the SlimeWorld from multiple threads simultaneously
            try {
                Bukkit.getLogger().log(Level.INFO, "Saving world " + slimeWorld.getName() + "...");
                long start = System.currentTimeMillis();
                byte[] serializedWorld = slimeWorld.serialize();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld, false);
                Bukkit.getLogger().log(Level.INFO, "World " + slimeWorld.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    ProtoChunkExtension getChunk(int x, int z) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(x, z);
        Chunk chunk;

        if (slimeChunk instanceof NMSSlimeChunk) {
            chunk = ((NMSSlimeChunk) slimeChunk).getChunk();
        } else {
            if (slimeChunk == null) {
                ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);

                // Biomes
                BiomeBase[] biomes = new BiomeBase[BiomeStorage.a];
                Arrays.fill(biomes, defaultBiome);
                BiomeStorage biomeStorage = new BiomeStorage(r().b(IRegistry.ay), biomes);

                // Tick lists
                ProtoChunkTickList<Block> blockTickList = new ProtoChunkTickList<>((block) ->
                        block == null || block.getBlockData().isAir(), pos);
                ProtoChunkTickList<FluidType> fluidTickList = new ProtoChunkTickList<>((type) ->
                        type == null || type == FluidTypes.EMPTY, pos);

                chunk = new Chunk(this, pos, biomeStorage, ChunkConverter.a, blockTickList, fluidTickList,
                        0L, null, null);

                // Height Maps
//                HeightMap.a(chunk, ChunkStatus.FULL.h());
            } else {
                chunk = createChunk(slimeChunk);
            }

            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }

        return new ProtoChunkExtension(chunk);
    }

    private Chunk createChunk(SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);

        // Biomes
        int[] biomeIntArray = chunk.getBiomes();

        BiomeStorage biomeStorage = new BiomeStorage(MinecraftServer.getServer().getCustomRegistry().b(IRegistry.ay), pos,
                getChunkProvider().getChunkGenerator().getWorldChunkManager(), biomeIntArray);

        // Tick lists
        ProtoChunkTickList<Block> blockTickList = new ProtoChunkTickList<>(
                (block) -> block == null || block.getBlockData().isAir(), pos);
        ProtoChunkTickList<FluidType> fluidTickList = new ProtoChunkTickList<>(
                (type) -> type == null || type == FluidTypes.EMPTY, pos);

        // Chunk sections
        ChunkSection[] sections = new ChunkSection[16];
        LightEngine lightEngine = getChunkProvider().getLightEngine();

        lightEngine.b(pos, true);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                ChunkSection section = new ChunkSection(sectionId << 4);

                section.getBlocks().a((NBTTagList) Converter.convertTag(slimeSection.getPalette()), slimeSection.getBlockStates());

                if (slimeSection.getBlockLight() != null) {
                    lightEngine.a(EnumSkyBlock.BLOCK, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getBlockLight()), true);
                }

                if (slimeSection.getSkyLight() != null) {
                    lightEngine.a(EnumSkyBlock.SKY, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getSkyLight()), true);
                }

                section.recalcBlockCounts();
                sections[sectionId] = section;
            }
        }

        // Keep the chunk loaded at level 33 to avoid light glitches
        // Such a high level will let the server not tick the chunk,
        // but at the same time it won't be completely unloaded from memory
//        getChunkProvider().addTicket(SWM_TICKET, pos, 33, Unit.INSTANCE);

        Consumer<Chunk> loadEntities = (nmsChunk) -> {

            // Load tile entities
            List<CompoundTag> tileEntities = chunk.getTileEntities();
            int loadedEntities = 0;

            if (tileEntities != null) {
                for (CompoundTag tag : tileEntities) {
                    Optional<String> type = tag.getStringValue("id");

                    // Sometimes null tile entities are saved
                    if (type.isPresent()) {
                        BlockPosition blockPosition = new BlockPosition(tag.getIntValue("x").get(), tag.getIntValue("y").get(), tag.getIntValue("z").get());
                        IBlockData blockData = nmsChunk.getType(blockPosition);
                        TileEntity entity = TileEntity.create(blockData, (NBTTagCompound) Converter.convertTag(tag));
                        if (blockData != null) {
                            NBTTagCompound nbtTagCompound = (NBTTagCompound) Converter.convertTag(tag);
                            if(tag.getAsCompoundTag("SkullOwner").isPresent()) {
                                if(tag.getAsCompoundTag("SkullOwner").get().getAsStringTag("Name").isPresent()) {
                                    if(tag.getAsCompoundTag("SkullOwner").get().getAsCompoundTag("Properties").isPresent()) {
                                        String skullName = tag.getAsCompoundTag("SkullOwner").get().getAsStringTag("Name").get().getValue();
                                        TileEntitySkull entitySkull = (TileEntitySkull) entity;

                                        nbtTagCompound.getCompound("SkullOwner").setString("Name", NBTTagString.b(uuid.toString()));
                                        NBTTagCompound properties = nbtTagCompound.getCompound("SkullOwner").getCompound("Properties");
                                        NBTTagList tagList = properties.getList("textures", 10);
                                        String finalTexture = "ITEM MISSING!";
                                        String finalSignature = "ITEM MISSING!";

                                        for(NBTBase nbtBase : tagList) {
                                            NBTTagCompound nbtComp = (NBTTagCompound) nbtBase;
                                            finalTexture = nbtComp.getString("Value");
                                            finalSignature = nbtComp.getString("Signature");
                                            nbtComp.setString("Signature", finalSignature);
                                        }

                                        if(!finalSignature.equals("")) {
                                            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), skullName);
                                            Property property = new Property("textures", finalTexture, finalSignature);
                                            gameProfile.getProperties().put("textures", property);
                                            entitySkull.setGameProfile(gameProfile);

                                            entitySkull.update();
                                        }else{
                                            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), skullName);
                                            Property property = new Property("textures", finalTexture);
                                            gameProfile.getProperties().put("textures", property);
                                            entitySkull.setGameProfile(gameProfile);

                                            entitySkull.update();
                                        }

                                        entitySkull.save(nbtTagCompound);

                                        entitySkull.load(blockData, nbtTagCompound);
                                    }
                                }else{
                                    if(tag.getAsCompoundTag("SkullOwner").get().getAsCompoundTag("Properties").isPresent()) {
                                        UUID uuid = UUID.randomUUID();
                                        NBTTagCompound properties = nbtTagCompound.getCompound("SkullOwner").getCompound("Properties");
                                        NBTTagList tagList = properties.getList("textures", 10);

                                        nbtTagCompound.getCompound("SkullOwner").setString("Name", NBTTagString.b("Paul19988"));

                                        String finalTexture = "ITEM MISSING!";
                                        String signature = "uhqzJ9HI6RH47VxjSToEiYTxNmp2vNdzM0y2E9YZE6wd49qADF4Hv9aau6lR7V7x2kyie9tRFHhNc8vIxBMBTtOIVPjAM5m0+y6mkm5hbV6rZSgG+OW/p27+Bun0Cq90o8N+sFBnzKsKL9o/3BMdYUmNaYzzK9RD5ymuPDQWnsJKf+ms+TH6lJL2ltt33BdYD3KiY8e7DXywjjH6htmcm/QHAWKIiWg9zX/X5iwjUux+kiT5EJTnQrNIei0v7XsEHJGPSjhAkCrwsHb+Bvjp6TcOUcpPRmRZXUwBRmngwXQEweymVLW/qdGLyBrsW6SZVxrb9/B4IBcNm73Kb50DTeGxuI+GPqxWBcKNBvzfyI6A2+GjScnp0zNYfk9od/Q7jezLZKTJWjDhYcPbdmc0FCHpbOBIqmNDRRykWYT/0M23tXmb2Dk6ADVO0weNdRqUpYo+3FhDjBdoM+BSlTcH7NbvhplEApaRrHzjlalGIi7FTC7cf5yw2Kv64YmEuvRoLHAFVOAnVY77dHGxshnLShBKu2uKsApqKvRqNiw2ULAnpvPiFjkjvI8UpbfPp5KdwldKY8rHcjvu8ux1vPiAM0x4yHAP+ho0hNfQu0F57pyBawpWQPGFjgCLlKwwSODpGL6cdyg3MCJVtofmlFY2oQjiUowS0Wc1kn4GejG1uKY=";

                                        for(NBTBase nbtBase : tagList) {
                                            NBTTagCompound nbtComp = (NBTTagCompound) nbtBase;
                                            finalTexture = nbtComp.getString("Value");
                                            nbtComp.setString("Signature", signature);
                                        }

                                        TileEntitySkull entitySkull = (TileEntitySkull) entity;

                                        GameProfile gameProfile = new GameProfile(uuid, uuid.toString());
                                        Property property = new Property("textures", finalTexture, signature);
                                        gameProfile.getProperties().put("textures", property);
                                        entitySkull.setGameProfile(gameProfile);

                                        entitySkull.update();
                                        entitySkull.save(nbtTagCompound);

                                        entitySkull.load(blockData, nbtTagCompound);
                                    }
                                }
                            }
                            nmsChunk.a(entity);
                            loadedEntities++;
                        }
                    }
                }
            }

            // Load entities
            List<CompoundTag> entities = chunk.getEntities();
            loadedEntities = 0;

            if (entities != null) {
                for (CompoundTag tag : entities) {
                    EntityTypes.a((NBTTagCompound) Converter.convertTag(tag), nmsChunk.world, (entity) -> {

                        nmsChunk.a(entity);
                        return entity;

                    });

                    nmsChunk.d(true);
                    loadedEntities++;
                }
            }
        };

        CompoundTag upgradeDataTag = ((CraftSlimeChunk) chunk).getUpgradeData();
        Chunk nmsChunk = new Chunk(this, pos, biomeStorage, upgradeDataTag == null ? ChunkConverter.a : new ChunkConverter((NBTTagCompound)
                Converter.convertTag(upgradeDataTag)), blockTickList, fluidTickList, 0L, sections, loadEntities);

        // Height Maps
        EnumSet<HeightMap.Type> heightMapTypes = nmsChunk.getChunkStatus().h();
        CompoundMap heightMaps = chunk.getHeightMaps().getValue();
        EnumSet<HeightMap.Type> unsetHeightMaps = EnumSet.noneOf(HeightMap.Type.class);

        for (HeightMap.Type type : heightMapTypes) {
            String name = type.getName();

            if (heightMaps.containsKey(name)) {
                LongArrayTag heightMap = (LongArrayTag) heightMaps.get(name);
                nmsChunk.a(type, heightMap.getValue());
            } else {
                unsetHeightMaps.add(type);
            }
        }

        HeightMap.a(nmsChunk, unsetHeightMaps);

        return nmsChunk;
    }

    void saveChunk(Chunk chunk) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(chunk.getPos().x, chunk.getPos().z);

        // In case somehow the chunk object changes (might happen for some reason)
        if (slimeChunk instanceof NMSSlimeChunk) {
            ((NMSSlimeChunk) slimeChunk).setChunk(chunk);
        } else {
            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }
    }
}
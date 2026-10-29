package it.hurts.shatterbyte.reanimal.fabric;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.effect.QuillMobEffect;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HeadHedgehogData;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HeadHedgehogs;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureEntity;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntityDataSerializers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

public class ReAnimalFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ReAnimal.init();

        FabricEntityDataRegistry.register(Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_state"), ReAnimalEntityDataSerializers.HEDGEHOG_STATE);

        var headHedgehog = AttachmentRegistry.<HeadHedgehogData>create(Identifier.fromNamespaceAndPath(ReAnimal.MODID, "head_hedgehog"), builder -> builder
                .persistent(HeadHedgehogData.CODEC)
                .syncWith(HeadHedgehogData.STREAM_CODEC, AttachmentSyncPredicate.all()));

        HeadHedgehogs.init(new HeadHedgehogs.Accessor() {
            @Override
            public HeadHedgehogData get(Player player) {
                return player.getAttached(headHedgehog);
            }

            @Override
            public void set(Player player, HeadHedgehogData data) {
                if (data == null)
                    player.removeAttached(headHedgehog);
                else
                    player.setAttached(headHedgehog, data);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof Player player)
                HedgehogEntity.dropFromHead(player);
        });

        ReAnimalEntities.registerAttributes(FabricDefaultAttributeRegistry::register);

        registerSpawnPlacements();
        registerSpawns();

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> HedgehogEntity.onEntityJoin(entity));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && HedgehogEntity.tryDropFromBlock(player, hand, hitResult.getBlockPos()))
                return InteractionResult.SUCCESS_SERVER;
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            var attacker = source.getEntity();
            QuillMobEffect.onLivingHurt(entity, source, amount);
            return !HedgehogEntity.tryBlockPhantomDamage(entity, attacker);
        });
    }

    private static void registerSpawnPlacements() {
        SpawnPlacements.register(ReAnimalEntities.HEDGEHOG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.OSTRICH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.KIWI, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.PIGEON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.BUTTERFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ButterflyEntity::checkButterflySpawnRules);
        SpawnPlacements.register(ReAnimalEntities.VULTURE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, VultureEntity::checkVultureSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.CAPYBARA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.RED_PANDA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.MEERKAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.SEAL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SealEntity::checkSealSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.HIPPOPOTAMUS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.CROCODILE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.GIRAFFE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.DRAGONFLY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.PENGUIN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ReAnimalEntities.SEA_URCHIN, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, (type, level, spawnType, pos, random) -> true);
        SpawnPlacements.register(ReAnimalEntities.STARFISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, (type, level, spawnType, pos, random) -> true);
        SpawnPlacements.register(ReAnimalEntities.JELLYFISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, spawnType, pos, random) -> true);
    }

    private static void registerSpawns() {
        addSpawn("minecraft:is_overworld", ReAnimalEntities.BUTTERFLY, 20, 3, 7);
        addSpawn("minecraft:is_jungle", ReAnimalEntities.CAPYBARA, 10, 2, 4);
        addSpawn("c:is_swamp", ReAnimalEntities.CROCODILE, 10, 2, 5);
        addSpawn("c:is_swamp", ReAnimalEntities.DRAGONFLY, 10, 2, 5);
        addSpawn("minecraft:is_savanna", ReAnimalEntities.GIRAFFE, 10, 1, 3);
        addSpawn("minecraft:is_forest", ReAnimalEntities.HEDGEHOG, 10, 2, 5);
        addSpawn("c:is_swamp", ReAnimalEntities.HIPPOPOTAMUS, 10, 2, 5);
        addSpawn("minecraft:is_ocean", ReAnimalEntities.JELLYFISH, 25, 2, 10);
        addSpawn("minecraft:is_jungle", ReAnimalEntities.KIWI, 10, 3, 5);
        addSpawn("minecraft:is_savanna", ReAnimalEntities.OSTRICH, 10, 1, 3);
        addSpawn("minecraft:is_savanna", ReAnimalEntities.MEERKAT, 8, 1, 3);
        addSpawn("c:is_cold/overworld", ReAnimalEntities.PENGUIN, 10, 3, 7);
        addSpawn("minecraft:is_overworld", ReAnimalEntities.PIGEON, 10, 5, 15);
        addSpawn("minecraft:is_jungle", ReAnimalEntities.RED_PANDA, 8, 1, 3);
        addSpawn("minecraft:is_beach", ReAnimalEntities.SEA_URCHIN, 25, 5, 10);
        addSpawn("minecraft:is_ocean", ReAnimalEntities.SEA_URCHIN, 25, 5, 15);
        addSpawn("reanimal:spawns_seal", ReAnimalEntities.SEAL, 20, 3, 6);
        addSpawn("minecraft:is_beach", ReAnimalEntities.STARFISH, 25, 5, 10);
        addSpawn("minecraft:is_ocean", ReAnimalEntities.STARFISH, 25, 5, 15);
        addSpawn("c:is_desert", ReAnimalEntities.VULTURE, 10, 1, 5);
    }

    private static void addSpawn(String biomeTag, EntityType<?> type, int weight, int minCount, int maxCount) {
        var tag = TagKey.create(Registries.BIOME, Identifier.parse(biomeTag));

        BiomeModifications.addSpawn(BiomeSelectors.tag(tag), type.getCategory(), type, weight, minCount, maxCount);
    }
}

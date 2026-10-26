package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.capybara.CapybaraEntity;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.crocodile.CrocodileEntity;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.giraffe.GiraffeEntity;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.QuillArrowEntity;
import it.hurts.shatterbyte.reanimal.common.entity.hippopotamus.HippopotamusEntity;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.kiwi.KiwiEntity;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEntity;
import it.hurts.shatterbyte.reanimal.common.entity.penguin.PenguinEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.penguin.PenguinEntity;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
import it.hurts.shatterbyte.reanimal.common.entity.red_panda.RedPandaEntity;
import it.hurts.shatterbyte.reanimal.common.entity.meerkat.MeerkatEntity;
import it.hurts.shatterbyte.reanimal.common.entity.sea_urchin.SeaUrchinEntity;
import it.hurts.shatterbyte.reanimal.common.entity.seal.SealEntity;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureEggEntity;
import it.hurts.shatterbyte.reanimal.common.entity.vulture.VultureEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.BiConsumer;

public class ReAnimalEntities {
    public static EntityType<HedgehogEntity> HEDGEHOG;
    public static EntityType<OstrichEntity> OSTRICH;
    public static EntityType<KiwiEntity> KIWI;
    public static EntityType<PigeonEntity> PIGEON;
    public static EntityType<ButterflyEntity> BUTTERFLY;
    public static EntityType<CapybaraEntity> CAPYBARA;
    public static EntityType<RedPandaEntity> RED_PANDA;
    public static EntityType<MeerkatEntity> MEERKAT;
    public static EntityType<SealEntity> SEAL;
    public static EntityType<HippopotamusEntity> HIPPOPOTAMUS;
    public static EntityType<CrocodileEntity> CROCODILE;
    public static EntityType<GiraffeEntity> GIRAFFE;
    public static EntityType<DragonflyEntity> DRAGONFLY;
    public static EntityType<VultureEntity> VULTURE;
    public static EntityType<PenguinEntity> PENGUIN;
    public static EntityType<SeaUrchinEntity> SEA_URCHIN;
    public static EntityType<StarfishEntity> STARFISH;
    public static EntityType<JellyfishEntity> JELLYFISH;
    public static EntityType<KiwiEggEntity> KIWI_EGG;
    public static EntityType<OstrichEggEntity> OSTRICH_EGG;
    public static EntityType<PigeonEggEntity> PIGEON_EGG;
    public static EntityType<VultureEggEntity> VULTURE_EGG;
    public static EntityType<PenguinEggEntity> PENGUIN_EGG;
    public static EntityType<CrocodileEggEntity> CROCODILE_EGG;
    public static EntityType<GlowStickEntity> GLOW_STICK;
    public static EntityType<QuillArrowEntity> QUILL_ARROW;

    public static void init() {
        HEDGEHOG = register("hedgehog", EntityType.Builder.of(HedgehogEntity::new, MobCategory.CREATURE).sized(0.6F, 0.5F));
        OSTRICH = register("ostrich", EntityType.Builder.of(OstrichEntity::new, MobCategory.CREATURE).sized(1.25F, 2.75F));
        KIWI = register("kiwi", EntityType.Builder.of(KiwiEntity::new, MobCategory.CREATURE).sized(0.5F, 0.6F));
        PIGEON = register("pigeon", EntityType.Builder.of(PigeonEntity::new, MobCategory.CREATURE).sized(0.5F, 0.65F));
        BUTTERFLY = register("butterfly", EntityType.Builder.of(ButterflyEntity::new, MobCategory.CREATURE).sized(0.5F, 0.45F));
        CAPYBARA = register("capybara", EntityType.Builder.of(CapybaraEntity::new, MobCategory.CREATURE).sized(1.1F, 1F));
        RED_PANDA = register("red_panda", EntityType.Builder.of(RedPandaEntity::new, MobCategory.CREATURE).sized(0.8F, 0.65F));
        MEERKAT = register("meerkat", EntityType.Builder.of(MeerkatEntity::new, MobCategory.CREATURE).sized(0.6F, 0.55F));
        SEAL = register("seal", EntityType.Builder.of(SealEntity::new, MobCategory.CREATURE).sized(1.1F, 0.9F));
        HIPPOPOTAMUS = register("hippopotamus", EntityType.Builder.of(HippopotamusEntity::new, MobCategory.CREATURE).sized(1.5F, 1.75F));
        CROCODILE = register("crocodile", EntityType.Builder.of(CrocodileEntity::new, MobCategory.CREATURE).sized(1.5F, 0.6F));
        GIRAFFE = register("giraffe", EntityType.Builder.of(GiraffeEntity::new, MobCategory.CREATURE).sized(1.75F, 6F));
        DRAGONFLY = register("dragonfly", EntityType.Builder.of(DragonflyEntity::new, MobCategory.CREATURE).sized(0.5F, 0.45F));
        VULTURE = register("vulture", EntityType.Builder.of(VultureEntity::new, MobCategory.CREATURE).sized(0.9F, 1.75F));
        PENGUIN = register("penguin", EntityType.Builder.of(PenguinEntity::new, MobCategory.CREATURE).sized(0.6F, 1.2F));
        SEA_URCHIN = register("sea_urchin", EntityType.Builder.of(SeaUrchinEntity::new, MobCategory.CREATURE).sized(0.5F, 0.65F));
        STARFISH = register("starfish", EntityType.Builder.of(StarfishEntity::new, MobCategory.CREATURE).sized(0.75F, 0.35F));
        JELLYFISH = register("jellyfish", EntityType.Builder.<JellyfishEntity>of(JellyfishEntity::new, MobCategory.WATER_CREATURE).sized(1F, 1F).eyeHeight(0.5F));
        KIWI_EGG = register("kiwi_egg", EntityType.Builder.of(KiwiEggEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
        OSTRICH_EGG = register("ostrich_egg", EntityType.Builder.of(OstrichEggEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
        PIGEON_EGG = register("pigeon_egg", EntityType.Builder.of(PigeonEggEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
        VULTURE_EGG = register("vulture_egg", EntityType.Builder.of(VultureEggEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
        PENGUIN_EGG = register("penguin_egg", EntityType.Builder.of(PenguinEggEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
        CROCODILE_EGG = register("crocodile_egg", EntityType.Builder.of(CrocodileEggEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
        GLOW_STICK = register("glow_stick", EntityType.Builder.<GlowStickEntity>of(GlowStickEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(10));
        QUILL_ARROW = register("quill_arrow", EntityType.Builder.<QuillArrowEntity>of(QuillArrowEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20));
    }

    public static void registerAttributes(BiConsumer<EntityType<? extends net.minecraft.world.entity.LivingEntity>, AttributeSupplier> register) {
        register.accept(HEDGEHOG, HedgehogEntity.createAttributes().build());
        register.accept(OSTRICH, OstrichEntity.createAttributes().build());
        register.accept(KIWI, KiwiEntity.createAttributes().build());
        register.accept(PIGEON, PigeonEntity.createAttributes().build());
        register.accept(BUTTERFLY, ButterflyEntity.createAttributes().build());
        register.accept(CAPYBARA, CapybaraEntity.createAttributes().build());
        register.accept(RED_PANDA, RedPandaEntity.createAttributes().build());
        register.accept(MEERKAT, MeerkatEntity.createAttributes().build());
        register.accept(SEAL, SealEntity.createAttributes().build());
        register.accept(HIPPOPOTAMUS, HippopotamusEntity.createAttributes().build());
        register.accept(CROCODILE, CrocodileEntity.createAttributes().build());
        register.accept(GIRAFFE, GiraffeEntity.createAttributes().build());
        register.accept(DRAGONFLY, DragonflyEntity.createAttributes().build());
        register.accept(VULTURE, VultureEntity.createAttributes().build());
        register.accept(PENGUIN, PenguinEntity.createAttributes().build());
        register.accept(SEA_URCHIN, SeaUrchinEntity.createAttributes().build());
        register.accept(STARFISH, StarfishEntity.createAttributes().build());
        register.accept(JELLYFISH, JellyfishEntity.createAttributes().build());
    }

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, id));

        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }
}

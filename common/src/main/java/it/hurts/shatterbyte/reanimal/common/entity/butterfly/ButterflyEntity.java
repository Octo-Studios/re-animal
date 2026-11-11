package it.hurts.shatterbyte.reanimal.common.entity.butterfly;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.common.entity.insect.FlyingInsectEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class ButterflyEntity extends FlyingInsectEntity implements GeoEntity {
    private static final int WATER_DAMAGE_DELAY = 100;
    private static final int WATER_DAMAGE_INTERVAL = 20;

    private static final TagKey<Biome> IS_SNOWY = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_snowy"));
    private static final TagKey<Biome> IS_DESERT = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_desert"));
    private static final TagKey<Biome> IS_SWAMP = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_swamp"));
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.butterfly.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.butterfly.walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.butterfly.fly");

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(ButterflyEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int ticksInWater;
    private boolean hoveringNearFlower;

    public int getVariant() {
        return this.getEntityData().get(VARIANT);
    }

    public void setVariant(int variant) {
        this.getEntityData().set(VARIANT, variant);
    }

    public void setHoveringNearFlower(boolean hoveringNearFlower) {
        this.hoveringNearFlower = hoveringNearFlower;
    }

    public ButterflyEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.lookControl = new LookControl(this) {
            @Override
            public void tick() {
                super.tick();

                if (ButterflyEntity.this.hoveringNearFlower) {
                    ButterflyEntity.this.setYRot(ButterflyEntity.this.yHeadRot);
                    ButterflyEntity.this.yBodyRot = ButterflyEntity.this.yHeadRot;
                }
            }
        };

        if (!level.isClientSide())
            this.setVariant(this.getRandom().nextInt(5));
    }

    @Override
    public boolean causeFallDamage(double distance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.resetFallDistance();
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        if (this.isInWater()) {
            ++this.ticksInWater;

            if (this.ticksInWater >= WATER_DAMAGE_DELAY) {
                this.hurtServer(serverLevel, this.damageSources().drown(), 1F);
                this.ticksInWater -= WATER_DAMAGE_INTERVAL;
            }
        } else if (this.ticksInWater > 0)
            --this.ticksInWater;

        if (this.updateShelterNavigation(serverLevel))
            ButterflyAI.updateActivity(this);

        if (!this.isExposedToPrecipitation())
            this.tickScared(serverLevel);

        if (!this.isPerched()) {
            ((Brain<ButterflyEntity>) this.getBrain()).tick(serverLevel, this);

            ButterflyAI.updateActivity(this);
        }

        super.customServerAiStep(serverLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(VARIANT, 0);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setVariant(tag.getIntOr("variant", 0));
    }

    protected Brain.Provider<ButterflyEntity> brainProvider() {
        return ButterflyAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return ButterflyAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.BUTTERFLY_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.BUTTERFLY_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.BUTTERFLY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.BUTTERFLY_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ReAnimalEntities.BUTTERFLY.create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private void tickScared(ServerLevel level) {
        LivingEntity scary = null;

        for (var entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5D, 2D, 5D), entity -> entity != this)) {
            if (!this.isScaredBy(entity))
                continue;

            scary = entity;

            break;
        }

        if (scary == null)
            return;

        this.stopResting();

        if (this.getNavigation().isDone()) {
            var dir = this.getDeltaMovement().multiply(1, 0, 1).normalize();
            var away = this.position().subtract(scary.position()).multiply(1.0D, 0.0D, 1.0D).normalize();

            if (dir.dot(away) < 0D)
                dir = dir.scale(-1D);

            dir = away.scale(0.5D).add(dir.scale(0.5D)).normalize();

            var center = this.position().add(dir.scale(16));
            var ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(center));

            var target = Vec3.atCenterOf(ground);

            this.getNavigation().moveTo(target.x, target.y, target.z, 1D);
        }
    }

    public boolean isScaredBy(LivingEntity entity) {
        if (!this.getBoundingBox().inflate(5F, 2F, 5F).intersects(entity.getBoundingBox()))
            return false;
        else if (entity instanceof Frog || entity instanceof HedgehogEntity)
            return true;
        else if (this.getLastHurtByMob() == entity)
            return true;
        else if (entity instanceof Player player)
            return !player.isSpectator() && (player.isSprinting() || player.isPassenger());
        else
            return false;
    }

    private PlayState mainPredicate(AnimationTest<ButterflyEntity> state) {
        var controller = state.controller();
        var entity = state.animatable();

        if (entity.isResting() || entity.isExposedToPrecipitation())
            controller.setAnimation((entity.isResting() || entity.onGround()) && state.isMoving() ? WALK : IDLE);
        else if (!entity.isPerched() && (state.isMoving() || !entity.onGround()))
            controller.setAnimation(FLY);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static boolean checkButterflySpawnRules(EntityType<ButterflyEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        var biome = level.getBiome(pos);

        if (biome.is(IS_SNOWY) || biome.is(IS_DESERT) || biome.is(IS_SWAMP))
            return false;

        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 2D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.FLYING_SPEED, 0.16D)
                .add(Attributes.FOLLOW_RANGE, 8D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }
}

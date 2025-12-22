package it.hurts.shatterbyte.reanimal.common.entity.dragonfly;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DragonflyEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.dragonfly.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.dragonfly.fly");

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(DragonflyEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int getVariant() {
        return this.getEntityData().get(VARIANT);
    }

    public void setVariant(int variant) {
        this.getEntityData().set(VARIANT, variant);
    }

    public DragonflyEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.navigation = new FlyingPathNavigation(this, this.level());

        this.getNavigation().setCanFloat(true);

        this.setNoGravity(true);
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();

        if (!(level instanceof ServerLevel serverLevel)) {
            super.customServerAiStep();

            return;
        }

        this.tickScared(serverLevel);

        var profiler = level.getProfiler();

        profiler.push("dragonflyBrain");
        ((Brain<DragonflyEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("dragonflyActivityUpdate");
        DragonflyAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(VARIANT, this.getRandom().nextInt(5));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.setVariant(tag.getInt("variant"));
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.resetFallDistance();
    }

    @Override
    protected Brain.Provider<DragonflyEntity> brainProvider() {
        return DragonflyAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return DragonflyAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.DRAGONFLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.DRAGONFLY_DEATH.get();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
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

        if (this.getNavigation().isDone()) {
            var dir = this.getDeltaMovement().multiply(1, 0, 1).normalize();
            var away = this.position().subtract(scary.position()).multiply(1.0D, 0.0D, 1.0D).normalize();

            if (dir.dot(away) < 0D)
                dir = dir.scale(-1D);

            dir = away.scale(0.5D).add(dir.scale(0.5D)).normalize();

            var center = this.position().add(dir.scale(16));
            var ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(center));

            var target = ground.getCenter();

            this.getNavigation().moveTo(target.x, target.y, target.z, 1D);
        }
    }

    public boolean isScaredBy(LivingEntity entity) {
        if (!this.getBoundingBox().inflate(5F, 2F, 5F).intersects(entity.getBoundingBox()))
            return false;
        else if (entity instanceof Frog)
            return true;
        else if (this.getLastHurtByMob() == entity)
            return true;
        else if (entity instanceof Player player)
            return !player.isSpectator() && (player.isSprinting() || player.isPassenger());
        else
            return false;
    }

    private PlayState mainPredicate(AnimationState<DragonflyEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

        if (state.isMoving() || !entity.onGround())
            controller.setAnimation(FLY);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 2D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }
}
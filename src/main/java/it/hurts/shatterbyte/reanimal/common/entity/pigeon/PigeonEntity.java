package it.hurts.shatterbyte.reanimal.common.entity.pigeon;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.gameevent.GameEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PigeonEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.pigeon.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.pigeon.walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.pigeon.fly");
    private static final RawAnimation PECK = RawAnimation.begin().thenLoop("animation.pigeon.peck");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private long scaredTicks;

    private int peckTicks;
    private int peckCooldown;
    private int eggTime = this.random.nextInt(6000) + 6000;

    public PigeonEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.updateMoveControl();
    }

    public boolean isPecking() {
        return peckTicks > 0;
    }

    public boolean isScared() {
        return this.scaredTicks > 0;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        var navigation = new FlyingPathNavigation(this, level);

        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        navigation.setCanFloat(true);

        return navigation;
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();

        if (!(level instanceof ServerLevel serverLevel)) {
            super.customServerAiStep();

            return;
        }

        this.tickScared(serverLevel);

        if (!isScared()) {
            var profiler = serverLevel.getProfiler();

            profiler.push("pigeonBrain");
            ((Brain<PigeonEntity>) this.getBrain()).tick(serverLevel, this);
            profiler.pop();

            profiler.push("pigeonActivityUpdate");
            PigeonAI.updateActivity(this);
            profiler.pop();
        }

        super.customServerAiStep();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.onGround()) {
            var motion = this.getDeltaMovement();

            if (motion.y < -0.1D)
                this.setDeltaMovement(motion.x, -0.1D, motion.z);
        }

        if (this.isPecking()) {
            var bodyYaw = this.getYRot();

            this.yBodyRot = bodyYaw;
            this.yHeadRot = bodyYaw;

            this.setXRot(0F);
        }

        if (this.level().isClientSide()) {
            if (this.onGround() && !this.isScared() && this.getDeltaMovement().horizontalDistanceSqr() < 0.0001D) {
                if (peckTicks > 0)
                    peckTicks--;
                else if (peckCooldown > 0)
                    peckCooldown--;
                else {
                    if (this.random.nextInt(120) == 0) {
                        peckTicks = 60;
                        peckCooldown = 40 + this.random.nextInt(60);
                    }
                }
            } else
                peckTicks = 0;
        }
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(baby);

        this.updateMoveControl();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("eggTime", this.eggTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.eggTime = tag.contains("eggTime") ? tag.getInt("eggTime") : this.random.nextInt(6000) + 6000;

        this.updateMoveControl();
    }

    @Override
    public void tick() {
        super.tick();

        var level = this.level();

        if (level.isClientSide())
            return;

        this.tickEggLaying();

        if (scaredTicks > 0)
            --scaredTicks;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (PigeonEntity.this.isPecking())
                    return;

                super.clientTick();
            }
        };
    }

    private void tickScared(ServerLevel level) {
        LivingEntity scary = null;

        for (var entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(7D, 3D, 7D), entity -> entity != this)) {
            if (!this.isScaredBy(entity))
                continue;

            scary = entity;

            break;
        }

        if (scary == null)
            return;

        this.scaredTicks = 100;

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

    public void updateMoveControl() {
        if (this.isBaby()) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigation(this, this.level());
        } else {
            this.moveControl = new FlyingMoveControl(this, 10, false);
            this.navigation = new FlyingPathNavigation(this, this.level());
        }
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
    protected Brain.Provider<PigeonEntity> brainProvider() {
        return PigeonAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return PigeonAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.PIGEON.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    public boolean isScaredBy(LivingEntity entity) {
        if (!this.getBoundingBox().inflate(7F, 3F, 7F).intersects(entity.getBoundingBox()))
            return false;
        else if (entity.getType().is(EntityTypeTags.UNDEAD))
            return true;
        else if (this.getLastHurtByMob() == entity)
            return true;
        else if (entity instanceof Player player)
            return !player.isSpectator() && (player.isSprinting() || player.isPassenger());
        else
            return false;
    }

    private PlayState mainPredicate(AnimationState<PigeonEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

        if (state.isMoving()) {
            if (entity.onGround())
                controller.setAnimation(WALK);
            else
                controller.setAnimation(FLY);
        } else {
            if (entity.isPecking())
                controller.setAnimation(PECK);
            else
                controller.setAnimation(IDLE);
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 4D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 16D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }

    private void tickEggLaying() {
        if (this.level().isClientSide() || this.isBaby())
            return;

        if (--this.eggTime > 0)
            return;

        this.playSound(SoundEvents.CHICKEN_EGG, 1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1F);
        this.spawnAtLocation(ReAnimalItems.PIGEON_EGG.get());
        this.gameEvent(GameEvent.ENTITY_PLACE, this);

        this.eggTime = this.random.nextInt(6000) + 6000;
    }
}
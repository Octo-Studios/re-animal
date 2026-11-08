package it.hurts.shatterbyte.reanimal.common.entity.red_panda;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;

public class RedPandaEntity extends Animal implements GeoEntity {
    private static final RawAnimation ADULT_IDLE = RawAnimation.begin().thenLoop("animation.red_panda.idle");
    private static final RawAnimation ADULT_WALK = RawAnimation.begin().thenLoop("animation.red_panda.walk");
    private static final RawAnimation ADULT_ASCEND = RawAnimation.begin().then("animation.red_panda.ascend", LoopType.PLAY_ONCE);
    private static final RawAnimation ADULT_STANDING = RawAnimation.begin().thenLoop("animation.red_panda.standing");
    private static final RawAnimation ADULT_DESCEND = RawAnimation.begin().then("animation.red_panda.descend", LoopType.PLAY_ONCE);

    private static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("animation.red_panda_baby.idle");
    private static final RawAnimation BABY_WALK = RawAnimation.begin().thenLoop("animation.red_panda_baby.walk");
    private static final RawAnimation BABY_ASCEND = RawAnimation.begin().then("animation.red_panda_baby.ascend", LoopType.PLAY_ONCE);
    private static final RawAnimation BABY_STANDING = RawAnimation.begin().thenLoop("animation.red_panda_baby.standing");
    private static final RawAnimation BABY_DESCEND = RawAnimation.begin().then("animation.red_panda_baby.descend", LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> STAND_STATE = SynchedEntityData.defineId(RedPandaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> CLIMBING_SIDE = SynchedEntityData.defineId(RedPandaEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Vector3fc> CLIMBING_FACING = SynchedEntityData.defineId(RedPandaEntity.class, EntityDataSerializers.VECTOR3);

    private static final int STAND_TRANSITION_TICKS = 10;
    private static final double STAND_PLAYER_RADIUS = 6D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final Quaternionf surfaceRotation = new Quaternionf();
    private final Quaternionf previousSurfaceRotation = new Quaternionf();

    private int standTransitionTicks;

    public RedPandaEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new RedPandaNavigation(this, level);
    }

    public Direction getClimbingSide() {
        return this.entityData.get(CLIMBING_SIDE);
    }

    public boolean isClimbing() {
        return this.getClimbingSide() != Direction.DOWN;
    }

    public boolean isInLeaves(Vec3 position) {
        var box = this.getBoundingBox().move(position.subtract(this.position())).deflate(Shapes.EPSILON);

        for (var pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (this.level().getBlockState(pos).is(BlockTags.LEAVES))
                return true;
        }

        return false;
    }

    public void setClimbingSide(Direction side, Vec3 movement) {
        var facing = side == Direction.DOWN && this.isInLeaves(this.position()) ? movement : movement.with(side.getAxis(), 0D);

        if (facing.lengthSqr() > 0D)
            this.entityData.set(CLIMBING_FACING, facing.normalize().toVector3f());
        else if (side != this.getClimbingSide())
            this.entityData.set(CLIMBING_FACING, side.getOpposite().getRotation().transform(new Vector3f(0F, 0F, -1F)));

        this.entityData.set(CLIMBING_SIDE, side);
    }

    @Override
    public void tick() {
        this.previousSurfaceRotation.set(this.surfaceRotation);

        super.tick();

        var rotation = new Quaternionf().rotationY((180F - this.yBodyRot) * Mth.DEG_TO_RAD);

        if (this.isClimbing()) {
            rotation = this.getClimbingSide().getOpposite().getRotation();

            var facing = new Vector3f(this.entityData.get(CLIMBING_FACING));

            new Quaternionf(rotation).conjugate().transform(facing);
            rotation.rotateY((float) Mth.atan2(-facing.x, -facing.z));
        } else if (this.isInLeaves(this.position())) {
            var facing = this.entityData.get(CLIMBING_FACING);
            var horizontal = Math.sqrt(facing.x() * facing.x() + facing.z() * facing.z());

            if (horizontal > 0D)
                rotation.rotationY((float) Mth.atan2(-facing.x(), -facing.z()));

            rotation.rotateX((float) Mth.atan2(facing.y(), horizontal));
        }

        this.surfaceRotation.slerp(rotation, 0.25F);
    }

    public Quaternionf getSurfaceRotation(float partialTick) {
        return new Quaternionf(this.previousSurfaceRotation).slerp(this.surfaceRotation, partialTick);
    }

    @Override
    public void calculateEntityAnimation(boolean useY) {
        super.calculateEntityAnimation(useY || this.isClimbing()
                || this.onGround() && this.isInLeaves(this.position()));
    }

    @Override
    public void travel(Vec3 input) {
        var navigation = (RedPandaNavigation) this.getNavigation();

        if (this.isEffectiveAi() && !this.isNoAi() && this.isAlive() && !this.isInLiquid() && !this.isPassenger() && navigation.isOnSurfacePath()) {
            this.setDeltaMovement(navigation.getMotion());
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setOnGround(this.getClimbingSide() == Direction.DOWN);
            this.resetFallDistance();
        } else {
            if (!this.level().isClientSide()) {
                if (this.isClimbing())
                    this.setDeltaMovement(Vec3.ZERO);

                this.setClimbingSide(Direction.DOWN, Vec3.ZERO);
            }

            super.travel(input);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        ((Brain<RedPandaEntity>) this.getBrain()).tick(serverLevel, this);

        RedPandaAI.updateActivity(this);

        super.customServerAiStep(serverLevel);

        ((RedPandaNavigation) this.getNavigation()).recover();
    }
    protected Brain.Provider<RedPandaEntity> brainProvider() {
        return RedPandaAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return RedPandaAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        var targetPlayer = this.findNearbyPlayer();
        var shouldStand = this.onGround() && !this.isClimbing() && !this.isInLeaves(this.position())
                && targetPlayer != null && !this.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);

        if (!this.level().isClientSide())
            this.updateStandState(shouldStand);

        if (this.getStandState() != StandState.ON_ALL_FOURS) {
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.setJumping(false);

            this.zza = 0F;
            this.xxa = 0F;

            if (this.onGround())
                this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);

            if (targetPlayer != null) {
                this.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(targetPlayer, true));
                this.getLookControl().setLookAt(targetPlayer, 8F, 0F);
            } else
                this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.RED_PANDA_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.RED_PANDA_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.RED_PANDA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.RED_PANDA_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.RED_PANDA.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            if (this.getStandState() != StandState.ON_ALL_FOURS) {
                this.setStandState(StandState.DESCENDING);
                this.standTransitionTicks = STAND_TRANSITION_TICKS;
            }

            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            RedPandaAI.updateActivity(this);
        }

        return result;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(STAND_STATE, StandState.ON_ALL_FOURS.ordinal());
        builder.define(CLIMBING_SIDE, Direction.DOWN);
        builder.define(CLIMBING_FACING, new Vector3f(0F, 0F, -1F));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<RedPandaEntity> state) {
        var controller = state.controller();
        var idle = this.isBaby() ? BABY_IDLE : ADULT_IDLE;
        var walk = this.isBaby() ? BABY_WALK : ADULT_WALK;
        var ascend = this.isBaby() ? BABY_ASCEND : ADULT_ASCEND;
        var standing = this.isBaby() ? BABY_STANDING : ADULT_STANDING;
        var descend = this.isBaby() ? BABY_DESCEND : ADULT_DESCEND;

        switch (this.getStandState()) {
            case ASCENDING -> controller.setAnimation(ascend);
            case STANDING -> controller.setAnimation(standing);
            case DESCENDING -> controller.setAnimation(descend);
            case ON_ALL_FOURS -> {
                if (state.isMoving())
                    controller.setAnimation(walk);
                else
                    controller.setAnimation(idle);
            }
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("StandState", this.entityData.get(STAND_STATE));
        tag.putInt("ClimbingSide", this.getClimbingSide().get3DDataValue());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setStandState(StandState.byId(tag.getIntOr("StandState", 0)));
        this.setClimbingSide(Direction.from3DDataValue(tag.getIntOr("ClimbingSide", Direction.DOWN.get3DDataValue())), Vec3.ZERO);
    }

    private Player findNearbyPlayer() {
        return this.level().getNearestPlayer(this, STAND_PLAYER_RADIUS);
    }

    public StandState getStandState() {
        return StandState.byId(this.entityData.get(STAND_STATE));
    }

    public void setStandState(StandState state) {
        this.entityData.set(STAND_STATE, state.ordinal());
    }

    private void updateStandState(boolean shouldStand) {
        var standState = this.getStandState();

        if (shouldStand) {
            if (standState == StandState.ON_ALL_FOURS || standState == StandState.DESCENDING) {
                this.setStandState(StandState.ASCENDING);
                this.standTransitionTicks = STAND_TRANSITION_TICKS;
            }
        } else if (standState == StandState.STANDING || standState == StandState.ASCENDING) {
            this.setStandState(StandState.DESCENDING);
            this.standTransitionTicks = STAND_TRANSITION_TICKS;
        }

        standState = this.getStandState();

        if (standState == StandState.ASCENDING || standState == StandState.DESCENDING) {
            if (this.standTransitionTicks > 0)
                this.standTransitionTicks--;

            if (this.standTransitionTicks == 0) {
                if (standState == StandState.ASCENDING)
                    this.setStandState(StandState.STANDING);
                else
                    this.setStandState(StandState.ON_ALL_FOURS);
            }
        }
    }

    public enum StandState {
        ON_ALL_FOURS,
        ASCENDING,
        STANDING,
        DESCENDING;

        public static StandState byId(int id) {
            var values = values();

            if (id < 0 || id >= values.length)
                return ON_ALL_FOURS;

            return values[id];
        }
    }
}

package it.hurts.shatterbyte.reanimal.common.entity.capybara;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class CapybaraEntity extends TamableAnimal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.capybara.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.capybara.walk");
    private static final RawAnimation IDLE_SIT = RawAnimation.begin().thenLoop("animation.capybara.idle_sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().then("animation.capybara.sitting", LoopType.PLAY_ONCE).thenLoop("animation.capybara.idle_sit");
    private static final RawAnimation GETTING_UP = RawAnimation.begin().then("animation.capybara.getting_up", LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Integer> SIT_STATE = SynchedEntityData.defineId(CapybaraEntity.class, EntityDataSerializers.INT);

    private static final int SIT_TRANSITION_TICKS = 10;
    private static final int STAND_TRANSITION_TICKS = 5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int sitTransitionTicks;

    public CapybaraEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        ((Brain<CapybaraEntity>) this.getBrain()).tick(serverLevel, this);

        CapybaraAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }

    protected Brain.Provider<CapybaraEntity> brainProvider() {
        return CapybaraAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return CapybaraAI.makeBrain(this.brainProvider().makeBrain(this, packed));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide())
            this.tickSitTransition();

        if (this.getSitState() != SitState.STANDING) {
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.setJumping(false);

            this.zza = 0F;
            this.xxa = 0F;

            if (this.onGround())
                this.setDeltaMovement(0D, this.getDeltaMovement().y, 0D);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var level = this.level();
        var held = player.getItemInHand(hand);

        if (this.isFood(held)) {
            if (!this.isTame()) {
                if (!level.isClientSide()) {
                    if (!player.getAbilities().instabuild)
                        held.shrink(1);

                    this.tame(player);
                    this.setTarget(null);
                    this.getNavigation().stop();

                    level.broadcastEntityEvent(this, (byte) 7);
                }

                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }

            if (this.isOwnedBy(player) && this.getHealth() < this.getMaxHealth()) {
                if (!level.isClientSide()) {
                    if (!player.getAbilities().instabuild)
                        held.shrink(1);

                    this.heal(2F);
                }

                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }

            return super.mobInteract(player, hand);
        }

        if (hand == InteractionHand.MAIN_HAND && this.isTame() && this.isOwnedBy(player)) {
            if (level.isClientSide())
                this.setSitState(this.isSittingDownOrSitting() ? SitState.GETTING_UP : SitState.SITTING_DOWN);
            else
                this.toggleSitting();

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    public void toggleSitting() {
        this.setSitting(!this.isSittingDownOrSitting());
    }

    private boolean isSittingDownOrSitting() {
        return this.getSitState() == SitState.SITTING_DOWN || this.getSitState() == SitState.SITTING;
    }

    private void setSitting(boolean sitting) {
        this.setOrderedToSit(sitting);
        this.setInSittingPose(sitting);
        this.setSitState(sitting ? SitState.SITTING_DOWN : SitState.GETTING_UP);

        this.sitTransitionTicks = SIT_TRANSITION_TICKS;

        this.getNavigation().stop();
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private void tickSitTransition() {
        var state = this.getSitState();

        if (state != SitState.SITTING_DOWN && state != SitState.GETTING_UP)
            return;

        if (this.sitTransitionTicks > 0)
            this.sitTransitionTicks--;

        if (this.sitTransitionTicks > 0)
            return;

        this.setSitState(state == SitState.SITTING_DOWN ? SitState.SITTING : SitState.STANDING);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.CAPYBARA_FOOD);
    }

    @Override
    public boolean canMate(Animal other) {
        return !this.isOrderedToSit() && super.canMate(other);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ReAnimalSoundEvents.CAPYBARA_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.CAPYBARA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.CAPYBARA_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.CAPYBARA.create(level, EntitySpawnReason.BREEDING);

        if (baby != null) {
            baby.setBaby(true);

            var thisOwnerRef = this.isTame() ? this.getOwnerReference() : null;
            var otherOwnerRef = partner instanceof CapybaraEntity other && other.isTame() ? other.getOwnerReference() : null;

            var ownerRef = thisOwnerRef;

            if (thisOwnerRef != null && otherOwnerRef != null)
                ownerRef = thisOwnerRef.getUUID().equals(otherOwnerRef.getUUID()) || this.random.nextBoolean() ? thisOwnerRef : otherOwnerRef;
            else if (ownerRef == null)
                ownerRef = otherOwnerRef;

            if (ownerRef != null) {
                baby.setOwnerReference(ownerRef);
                baby.setTame(true, true);
            }
        }

        return baby;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result && !this.isOrderedToSit()) {
            this.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);

            CapybaraAI.updateActivity(this);
        }

        return result;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(SIT_STATE, SitState.STANDING.ordinal());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setSitState(this.isOrderedToSit() ? SitState.SITTING : SitState.STANDING);
    }

    public SitState getSitState() {
        return SitState.byId(this.getEntityData().get(SIT_STATE));
    }

    public void setSitState(SitState state) {
        this.getEntityData().set(SIT_STATE, state.ordinal());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<CapybaraEntity> state) {
        var controller = state.controller();

        controller.setTransitionTicks(this.getSitState() == SitState.STANDING ? STAND_TRANSITION_TICKS : 0);

        switch (this.getSitState()) {
            case SITTING_DOWN -> controller.setAnimation(SIT_DOWN);
            case SITTING -> controller.setAnimation(controller.getCurrentRawAnimation() == SIT_DOWN ? SIT_DOWN : IDLE_SIT);
            case GETTING_UP -> controller.setAnimation(GETTING_UP);
            case STANDING -> {
                if (state.isMoving())
                    controller.setAnimation(WALK);
                else
                    controller.setAnimation(IDLE);
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

    public enum SitState {
        STANDING,
        SITTING_DOWN,
        SITTING,
        GETTING_UP;

        public static SitState byId(int id) {
            var values = values();

            if (id < 0 || id >= values.length)
                return STANDING;

            return values[id];
        }
    }
}

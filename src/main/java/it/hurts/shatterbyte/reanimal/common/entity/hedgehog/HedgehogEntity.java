package it.hurts.shatterbyte.reanimal.common.entity.hedgehog;

import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import it.hurts.shatterbyte.reanimal.init.ReAnimalDamageTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntityDataSerializers;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Direction;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.IntFunction;

public class HedgehogEntity extends TamableAnimal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hedgehog.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hedgehog.walk");
    private static final RawAnimation ROLL = RawAnimation.begin().then("animation.hedgehog.roll", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation UNROLL = RawAnimation.begin().then("animation.hedgehog.unroll", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SCARED = RawAnimation.begin().thenLoop("animation.hedgehog.scared");

    private static final EntityDataAccessor<HedgehogState> STATE = SynchedEntityData.defineId(HedgehogEntity.class, ReAnimalEntityDataSerializers.HEDGEHOG_STATE.get());
    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.ITEM_STACK);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public long inStateTicks = 0L;

    public HedgehogState getState() {
        return this.getEntityData().get(STATE);
    }

    public void setState(HedgehogState state) {
        this.getEntityData().set(STATE, state);
    }

    public ItemStack getStack() {
        return this.getEntityData().get(STACK);
    }

    public void setStack(ItemStack stack) {
        this.getEntityData().set(STACK, stack);
    }

    public boolean isScared() {
        return this.entityData.get(STATE) != HedgehogState.IDLE;
    }

    public HedgehogEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);

        this.getNavigation().setCanFloat(true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var level = this.level();

        var held = player.getItemInHand(hand);
        var owner = this.getOwner();
        var back = this.getStack();

        if (this.isTame() && this.isOwnedBy(player) && player.isShiftKeyDown() && held.isEmpty() && !this.isPassenger()) {
            if (this.isScared())
                this.rollOut();

            this.getNavigation().stop();
            this.startRiding(player, true);

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!back.isEmpty()) {
            if (player.isShiftKeyDown())
                return super.mobInteract(player, hand);

            if (!level.isClientSide()) {
                var toGive = back.copy();

                this.setStack(ItemStack.EMPTY);

                if (!player.addItem(toGive))
                    this.spawnAtLocation(toGive);
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!held.isEmpty()) {
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

                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                if (owner == player && this.getHealth() < this.getMaxHealth()) {
                    if (!level.isClientSide()) {
                        if (!player.getAbilities().instabuild)
                            held.shrink(1);

                        this.heal(2F);
                    }

                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                var result = super.mobInteract(player, hand);

                if (result.consumesAction())
                    return result;
            }

            if (!level.isClientSide()) {
                this.setStack(held.copy());

                if (!player.getAbilities().instabuild)
                    held.shrink(held.getCount());
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();

        var level = this.level();

        if (level.isClientSide())
            return;

        ++this.inStateTicks;

        if (!this.isBaby()) {
            var targets = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), (candidate) -> !(candidate instanceof HedgehogEntity));
            var owner = this.getOwner();

            var damagedSomeone = false;

            for (var target : targets) {
                if (target == owner || target == this.getVehicle())
                    continue;

                var damaged = target.hurt(level.damageSources().source(ReAnimalDamageTypes.HEDGEHOG_SPIKES, this), this.getState() == HedgehogState.SCARED ? 5 : 1);

                damagedSomeone = damagedSomeone || damaged;
            }

            if (damagedSomeone)
                this.getBrain().setMemoryWithExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY, true, 80);
        }

        if (this.getState() == HedgehogState.UNROLLING
                && this.inStateTicks == (long) HedgehogState.UNROLLING.getAnimationDuration() - 10) {
            this.playSound(ReAnimalSoundEvents.HEDGEHOG_UNROLL.get(), 1F, 1F);
        }
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("hedgehogBrain");
        ((Brain<HedgehogEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("hedgehogActivityUpdate");
        HedgehogAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        var result = super.hurt(source, amount);

        if (result && !this.level().isClientSide()) {
            var attacker = source.getEntity();

            if (attacker instanceof LivingEntity entity && entity.getMainHandItem().isEmpty())
                entity.hurt(this.damageSources().source(ReAnimalDamageTypes.HEDGEHOG_SPIKES, this), 1F);
        }

        return result;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (HedgehogEntity.this.isScared())
                    return;

                super.clientTick();
            }
        };
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        var stack = this.getStack();

        if (!stack.isEmpty())
            this.spawnAtLocation(stack);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putString("state", this.getState().getName());

        var stack = this.getStack();

        if (!stack.isEmpty())
            tag.put("stack", stack.save(this.level().registryAccess(), new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.setState(HedgehogState.fromName(tag.getString("state")));

        if (tag.contains("stack"))
            this.setStack(ItemStack.parse(this.level().registryAccess(), tag.getCompound("stack")).orElse(ItemStack.EMPTY));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (STATE.equals(data))
            this.inStateTicks = 0L;

        super.onSyncedDataUpdated(data);
    }

    @Override
    protected Brain.Provider<HedgehogEntity> brainProvider() {
        return HedgehogAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return HedgehogAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder buidler) {
        super.defineSynchedData(buidler);

        buidler.define(STATE, HedgehogState.IDLE);
        buidler.define(STACK, ItemStack.EMPTY);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.HEDGEHOG_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isScared())
            return null;

        return ReAnimalSoundEvents.HEDGEHOG_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.HEDGEHOG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.HEDGEHOG_DEATH.get();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.HEDGEHOG.get().create(level);

        if (baby != null) {
            baby.setBaby(true);

            var thisOwnerUUID = this.isTame() ? this.getOwnerUUID() : null;
            var otherOwnerUUID = partner instanceof HedgehogEntity other && other.isTame() ? other.getOwnerUUID() : null;

            var ownerUUID = thisOwnerUUID;

            if (thisOwnerUUID != null && otherOwnerUUID != null)
                ownerUUID = thisOwnerUUID.equals(otherOwnerUUID) || this.random.nextBoolean() ? thisOwnerUUID : otherOwnerUUID;
            else if (ownerUUID == null)
                ownerUUID = otherOwnerUUID;

            if (ownerUUID != null) {
                baby.setOwnerUUID(ownerUUID);
                baby.setTame(true, true);
            }
        }

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<HedgehogEntity> state) {
        var controller = state.getController();

        var hedgehogState = this.getState();

        if (hedgehogState != HedgehogState.IDLE) {
            switch (hedgehogState) {
                case ROLLING -> controller.setAnimation(ROLL);
                case SCARED -> controller.setAnimation(SCARED);
                case UNROLLING -> controller.setAnimation(UNROLL);
            }

            return PlayState.CONTINUE;
        }

        if (state.isMoving())
            controller.setAnimation(WALK);
        else
            controller.setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 8D);
    }

    public boolean canStayRolledUp() {
        return !this.isPanicking() && !this.isInLiquid() && !this.isLeashed() && !this.isPassenger() && !this.isVehicle();
    }

    public boolean isScaredBy(LivingEntity entity) {
        if (!this.getBoundingBox().inflate(5F, 2F, 5F).intersects(entity.getBoundingBox()))
            return false;
        else if (entity.getType().is(EntityTypeTags.UNDEAD))
            return true;
        else if (this.getLastHurtByMob() == entity)
            return true;
        else if (entity instanceof Player player) {
            if (player.isSpectator())
                return false;

            if (this.isTame() && this.isOwnedBy(player))
                return false;

            return player.isSprinting() || player.isPassenger();
        }
        else
            return false;
    }

    @Override
    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        if (vehicle instanceof Player)
            return new Vec3(0,-0.1,0);

        return super.getVehicleAttachmentPoint(vehicle);
    }

    public void rollUp() {
        if (this.isScared())
            return;

        this.stopInPlace();

        this.resetLove();

        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(ReAnimalSoundEvents.HEDGEHOG_ROLL.get());
        this.setState(HedgehogState.ROLLING);
    }

    public void rollOut() {
        if (!this.isScared())
            return;

        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.setState(HedgehogState.IDLE);
    }

    public boolean isOnHeadOf(Player player) {
        return this.isPassenger() && this.getVehicle() == player;
    }

    @Nullable
    public static HedgehogEntity getHeadPassenger(Player player) {
        for (var passenger : player.getPassengers()) {
            if (passenger instanceof HedgehogEntity hedgehog && hedgehog.isOnHeadOf(player))
                return hedgehog;
        }

        return null;
    }

    @EventBusSubscriber
    public static class CommonEvents {
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            var player = event.getEntity();

            if (!player.isShiftKeyDown())
                return;

            if (event.getHand() != InteractionHand.MAIN_HAND)
                return;

            if (!player.getItemInHand(event.getHand()).isEmpty())
                return;

            var hedgehog = HedgehogEntity.getHeadPassenger(player);

            if (hedgehog == null)
                return;

            var level = player.level();
            var clickedPos = event.getPos();
            var state = level.getBlockState(clickedPos);

            var topY = clickedPos.getY() + state.getCollisionShape(level, clickedPos).max(Direction.Axis.Y);

            if (topY <= clickedPos.getY())
                topY = clickedPos.getY() + 1D;

            var x = clickedPos.getX() + 0.5D;
            var y = topY;
            var z = clickedPos.getZ() + 0.5D;

            hedgehog.stopRiding();
            hedgehog.getNavigation().stop();
            hedgehog.absMoveTo(x, y, z, player.getYRot(), hedgehog.getXRot());
            hedgehog.setDeltaMovement(Vec3.ZERO);

            if (!level.noCollision(hedgehog, hedgehog.getBoundingBox())) {
                var fallback = player.blockPosition().above();
                hedgehog.absMoveTo(fallback.getX() + 0.5D, fallback.getY(), fallback.getZ() + 0.5D, player.getYRot(), hedgehog.getXRot());
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide())
                return;

            if (!(event.getEntity() instanceof Spider spider))
                return;

            var hasAvoidGoal = spider.goalSelector.getAvailableGoals()
                    .stream()
                    .anyMatch(goal -> goal.getGoal() instanceof SpiderAvoidHedgehogGoal);

            if (!hasAvoidGoal)
                spider.goalSelector.addGoal(2, new SpiderAvoidHedgehogGoal(spider));
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (event.getEntity().level().isClientSide())
                return;

            if (!(event.getEntity() instanceof Player player))
                return;

            if (!(event.getSource().getEntity() instanceof Phantom phantom))
                return;

            if (!phantom.isAlive())
                return;

            var hedgehog = HedgehogEntity.getHeadPassenger(player);

            if (hedgehog == null)
                return;

            event.setCanceled(true);

            phantom.hurt(player.damageSources().thorns(hedgehog), 5F);
        }

        private static class SpiderAvoidHedgehogGoal extends AvoidEntityGoal<HedgehogEntity> {
            public SpiderAvoidHedgehogGoal(Spider spider) {
                super(spider, HedgehogEntity.class, 6F, 1D, 1.2D, living -> living instanceof HedgehogEntity hedgehog && !hedgehog.isBaby());
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum HedgehogState implements StringRepresentable {
        IDLE("idle", false, 0, 0),
        ROLLING("rolling", true, 23, 1),
        SCARED("scared", true, 50, 2),
        UNROLLING("unrolling", true, 23, 3);

        private static final IntFunction<HedgehogState> BY_ID = ByIdMap.continuous(HedgehogState::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

        private static final StringRepresentable.EnumCodec<HedgehogState> CODEC = StringRepresentable.fromEnum(HedgehogState::values);
        public static final StreamCodec<ByteBuf, HedgehogState> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, HedgehogState::getId);

        private final String name;
        private final boolean isThreatened;
        private final int animationDuration;
        private final int id;

        public static HedgehogState fromName(String name) {
            return CODEC.byName(name, IDLE);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}

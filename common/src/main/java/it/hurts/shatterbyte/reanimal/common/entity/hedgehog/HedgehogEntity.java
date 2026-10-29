package it.hurts.shatterbyte.reanimal.common.entity.hedgehog;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.netty.buffer.ByteBuf;
import it.hurts.shatterbyte.reanimal.init.ReAnimalDamageTypes;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntityDataSerializers;
import it.hurts.shatterbyte.reanimal.init.ReAnimalSoundEvents;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import java.util.function.IntFunction;

public class HedgehogEntity extends TamableAnimal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hedgehog.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hedgehog.walk");
    private static final RawAnimation ROLL = RawAnimation.begin().thenPlayAndHold("animation.hedgehog.roll");
    private static final RawAnimation UNROLL = RawAnimation.begin().thenPlayAndHold("animation.hedgehog.unroll");
    private static final RawAnimation SCARED = RawAnimation.begin().thenLoop("animation.hedgehog.scared");

    private static final EntityDataAccessor<HedgehogState> STATE = SynchedEntityData.defineId(HedgehogEntity.class, ReAnimalEntityDataSerializers.HEDGEHOG_STATE);
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

        if (this.isTame() && this.isOwnedBy(player) && player.isShiftKeyDown() && held.isEmpty() && !HeadHedgehogs.has(player)) {
            if (!level.isClientSide()) {
                if (this.isScared())
                    this.rollOut();

                this.getNavigation().stop();

                HeadHedgehogs.set(player, HeadHedgehogData.of(this));

                this.discard();
            }

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (!back.isEmpty()) {
            if (player.isShiftKeyDown())
                return super.mobInteract(player, hand);

            if (!level.isClientSide()) {
                var toGive = back.copy();

                this.setStack(ItemStack.EMPTY);

                if (!player.addItem(toGive))
                    this.spawnAtLocation((ServerLevel) level, toGive);
            }

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
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

                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
                }

                if (owner == player && this.getHealth() < this.getMaxHealth()) {
                    if (!level.isClientSide()) {
                        if (!player.getAbilities().instabuild)
                            held.shrink(1);

                        this.heal(2F);
                    }

                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
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

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
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
                if (target == owner)
                    continue;

                var damaged = target.hurtOrSimulate(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.HEDGEHOG_SPIKES), this.getState() == HedgehogState.SCARED ? 5 : 1);

                damagedSomeone = damagedSomeone || damaged;
            }

            if (damagedSomeone)
                this.getBrain().setMemoryWithExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY, true, 80);
        }

        if (this.getState() == HedgehogState.UNROLLING
                && this.inStateTicks == (long) HedgehogState.UNROLLING.getAnimationDuration() - 10) {
            this.playSound(ReAnimalSoundEvents.HEDGEHOG_UNROLL, 1F, 1F);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        var level = this.level();

        ((Brain<HedgehogEntity>) this.getBrain()).tick(serverLevel, this);

        HedgehogAI.updateActivity(this);

        super.customServerAiStep(serverLevel);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var result = super.hurtServer(level, source, amount);

        if (result) {
            var attacker = source.getEntity();

            if (attacker instanceof LivingEntity entity && entity.getMainHandItem().isEmpty())
                entity.hurt(ReAnimalDamageTypes.source(this, ReAnimalDamageTypes.HEDGEHOG_SPIKES), 1F);
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
            this.spawnAtLocation(level, stack);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putString("state", this.getState().getName());

        var stack = this.getStack();

        if (!stack.isEmpty())
            tag.store("stack", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC, stack);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.setState(HedgehogState.fromName(tag.getStringOr("state", "")));

        tag.read("stack", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC).ifPresent(this::setStack);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (STATE.equals(data))
            this.inStateTicks = 0L;

        super.onSyncedDataUpdated(data);
    }
    protected Brain.Provider<HedgehogEntity> brainProvider() {
        return HedgehogAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packed) {
        return HedgehogAI.makeBrain(this.brainProvider().makeBrain(this, packed));
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

        return ReAnimalSoundEvents.HEDGEHOG_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ReAnimalSoundEvents.HEDGEHOG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ReAnimalSoundEvents.HEDGEHOG_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.HEDGEHOG.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);

        if (baby != null) {
            baby.setBaby(true);

            var thisOwnerRef = this.isTame() ? this.getOwnerReference() : null;
            var otherOwnerRef = partner instanceof HedgehogEntity other && other.isTame() ? other.getOwnerReference() : null;

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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<HedgehogEntity> state) {
        var controller = state.controller();

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
                .add(Attributes.TEMPT_RANGE, 10D)
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
        else if (entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD))
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

    public void rollUp() {
        if (this.isScared())
            return;

        this.stopInPlace();

        this.resetLove();

        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(ReAnimalSoundEvents.HEDGEHOG_ROLL);
        this.setState(HedgehogState.ROLLING);
    }

    public void rollOut() {
        if (!this.isScared())
            return;

        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.setState(HedgehogState.IDLE);
    }

    public static boolean tryDropFromBlock(Player player, InteractionHand hand, BlockPos pos) {
        if (!player.isShiftKeyDown())
            return false;

        if (hand != InteractionHand.MAIN_HAND)
            return false;

        if (!player.getItemInHand(hand).isEmpty())
            return false;

        var data = HeadHedgehogs.get(player);

        if (data == null)
            return false;

        if (!(player.level() instanceof ServerLevel level))
            return true;

        var hedgehog = data.create(level);

        HeadHedgehogs.set(player, null);

        if (hedgehog == null)
            return true;

        var state = level.getBlockState(pos);
        var topY = pos.getY() + state.getCollisionShape(level, pos).max(Direction.Axis.Y);

        if (topY <= pos.getY())
            topY = pos.getY() + 1D;

        hedgehog.absSnapTo(pos.getX() + 0.5D, topY, pos.getZ() + 0.5D, player.getYRot(), hedgehog.getXRot());
        hedgehog.setDeltaMovement(Vec3.ZERO);

        if (!level.noCollision(hedgehog, hedgehog.getBoundingBox())) {
            var fallback = player.blockPosition().above();
            hedgehog.absSnapTo(fallback.getX() + 0.5D, fallback.getY(), fallback.getZ() + 0.5D, player.getYRot(), hedgehog.getXRot());
        }

        level.addFreshEntity(hedgehog);

        return true;
    }

    public static void dropFromHead(Player player) {
        var data = HeadHedgehogs.get(player);

        if (data == null || !(player.level() instanceof ServerLevel level))
            return;

        HeadHedgehogs.set(player, null);

        var hedgehog = data.create(level);

        if (hedgehog == null)
            return;

        hedgehog.absSnapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0F);

        level.addFreshEntity(hedgehog);
    }

    public static void onEntityJoin(Entity entity) {
        if (!(entity instanceof Spider spider))
            return;

        var hasAvoidGoal = spider.getGoalSelector().getAvailableGoals()
                .stream()
                .anyMatch(goal -> goal.getGoal() instanceof SpiderAvoidHedgehogGoal);

        if (!hasAvoidGoal)
            spider.getGoalSelector().addGoal(2, new SpiderAvoidHedgehogGoal(spider));
    }

    public static boolean tryBlockPhantomDamage(Entity target, Entity attacker) {
        if (!(target instanceof Player player))
            return false;

        if (!(attacker instanceof Phantom phantom))
            return false;

        if (!phantom.isAlive())
            return false;

        if (!HeadHedgehogs.has(player))
            return false;

        phantom.hurt(player.damageSources().thorns(player), 5F);

        return true;
    }

    public static class SpiderAvoidHedgehogGoal extends AvoidEntityGoal<HedgehogEntity> {
        public SpiderAvoidHedgehogGoal(Spider spider) {
            super(spider, HedgehogEntity.class, 6F, 1D, 1.2D, living -> living instanceof HedgehogEntity hedgehog && !hedgehog.isBaby());
        }
    }

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

        HedgehogState(String name, boolean isThreatened, int animationDuration, int id) {
            this.name = name;
            this.isThreatened = isThreatened;
            this.animationDuration = animationDuration;
            this.id = id;
        }

        public String getName() { return this.name; }
        public boolean isThreatened() { return this.isThreatened; }
        public int getAnimationDuration() { return this.animationDuration; }
        public int getId() { return this.id; }

        public static HedgehogState fromName(String name) {
            return CODEC.byName(name, IDLE);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}

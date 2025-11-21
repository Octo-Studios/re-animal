package it.hurts.shatterbyte.reanimal.world.entity;

import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.registry.ReAnimalEntityDataSerializers;
import it.hurts.shatterbyte.reanimal.world.entity.ai.HedgehogAI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.IntFunction;

public class HedgehogEntity extends Animal implements GeoEntity {
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

    public HedgehogEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var level = this.level();

        var back = this.getStack();
        var held = player.getItemInHand(hand);

        if (!back.isEmpty()) {
            if (!level.isClientSide()) {
                var toGive = back.copy();

                this.setStack(ItemStack.EMPTY);

                if (!player.addItem(toGive))
                    this.spawnAtLocation(toGive);
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!held.isEmpty()) {
            if (!level.isClientSide()) {
                this.setStack(held.copy());

                if (!player.getAbilities().instabuild)
                    held.shrink(1);
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

            for (var target : targets) {
                target.hurt(level.damageSources().thorns(this), this.getState() == HedgehogState.SCARED ? 5 : 1);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("armadilloBrain");
        ((Brain<HedgehogEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("armadilloActivityUpdate");
        HedgehogAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
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
        return stack.is(Items.SPIDER_EYE);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.HEDGEHOG.get().create(level);

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
        else if (entity instanceof Player player)
            return !player.isSpectator() && (player.isSprinting() || player.isPassenger());
        else
            return false;
    }

    public void rollUp() {
        if (this.isScared())
            return;

        this.stopInPlace();

        this.resetLove();

        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(SoundEvents.ARMADILLO_ROLL);
        this.setState(HedgehogState.ROLLING);
    }

    public void rollOut() {
        if (!this.isScared())
            return;

        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.setState(HedgehogState.IDLE);
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
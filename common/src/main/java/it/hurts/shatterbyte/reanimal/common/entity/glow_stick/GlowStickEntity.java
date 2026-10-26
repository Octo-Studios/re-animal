package it.hurts.shatterbyte.reanimal.common.entity.glow_stick;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import it.hurts.shatterbyte.shatterlib.module.particle.trail.EntityTrailProvider;
import it.hurts.shatterbyte.reanimal.common.block.GlowLightBlock;
import it.hurts.shatterbyte.reanimal.common.blockentity.GlowLightBlockEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalBlocks;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class GlowStickEntity extends ThrowableItemProjectile implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.glow_stick.idle");
    private static final double BOUNCE_DAMPING = 0.7D;
    private static final double BOUNCE_Y_CUTOFF = 0.18D;
    private static final int MAX_LIFETIME = 6000;

    private static final EntityDataAccessor<Float> RENDER_YAW = SynchedEntityData.defineId(GlowStickEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RENDER_TUMBLE = SynchedEntityData.defineId(GlowStickEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(GlowStickEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float renderYaw;
    private float renderYawO;
    private float renderTumble;
    private float renderTumbleO;
    private float renderHeight = 0.07F;
    private float renderHeightO = 0.07F;
    private float tumbleSpeed;
    private int restTicks;

    public GlowStickEntity(EntityType<? extends GlowStickEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(RENDER_YAW, 0.0F);
        builder.define(RENDER_TUMBLE, 90.0F);
        builder.define(SETTLED, false);
    }

    @Override
    protected Item getDefaultItem() {
        return ReAnimalItems.GLOW_STICK_WHITE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        var motion = this.getDeltaMovement();
        var hitPos = result.getLocation();
        var normal = Vec3.atLowerCornerOf(result.getDirection().getUnitVec3i());

        var bounce = motion.subtract(normal.scale(2.0D * motion.dot(normal))).scale(BOUNCE_DAMPING);

        if (result.getDirection().getAxis() == Direction.Axis.Y && Math.abs(bounce.y) < BOUNCE_Y_CUTOFF)
            bounce = new Vec3(bounce.x, 0.0D, bounce.z);

        var pos = new Vec3(hitPos.x + normal.x * 0.01D, hitPos.y + normal.y * 0.01D, hitPos.z + normal.z * 0.01D);

        for (int i = 0; i < 2 && bounce.lengthSqr() > 1.0E-8D; i++) {
            var hit = this.level().clip(new ClipContext(pos, pos.add(bounce), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            if (hit.getType() != HitResult.Type.BLOCK)
                break;

            var hitLocation = hit.getLocation();

            if (hitLocation.distanceToSqr(pos) < 1.0E-10D)
                break;

            var hitNormal = Vec3.atLowerCornerOf(hit.getDirection().getStep());

            bounce = bounce.subtract(hitNormal.scale(2.0D * bounce.dot(hitNormal))).scale(BOUNCE_DAMPING);
            if (hit.getDirection().getAxis() == Direction.Axis.Y && Math.abs(bounce.y) < BOUNCE_Y_CUTOFF)
                bounce = new Vec3(bounce.x, 0.0D, bounce.z);

            pos = new Vec3(
                    hitLocation.x + hitNormal.x * 0.01D,
                    hitLocation.y + hitNormal.y * 0.01D,
                    hitLocation.z + hitNormal.z * 0.01D
            );
        }

        this.setPos(pos.x, pos.y, pos.z);
        this.setDeltaMovement(bounce);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 20)
            for (var entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox()))
                this.setDeltaMovement(this.position().subtract(entity.position()).normalize().multiply(0.35F, 0F, 0.35F).add(0F, 0.2F, 0F));

        this.renderYawO = this.renderYaw;
        this.renderTumbleO = this.renderTumble;
        this.renderHeightO = this.renderHeight;

        var motion = this.getDeltaMovement();

        if (this.tickCount > 1)
            tryPlaceGlowLight(this.level(), BlockPos.containing(this.position()));

        var horSqr = motion.x * motion.x + motion.z * motion.z;
        var grounded = this.isGrounded();

        var tumbleRad = this.renderTumble * Mth.DEG_TO_RAD;
        var targetHeight = grounded
                ? 0.3125F * Math.abs(Mth.cos(tumbleRad)) + 0.07F * Math.abs(Mth.sin(tumbleRad))
                : 0.15F;

        this.renderHeight = Mth.lerp(0.3F, this.renderHeight, targetHeight);

        if (this.getEntityData().get(SETTLED)) {
            if (!this.level().isClientSide() && (horSqr > 0.0025D || !grounded || motion.y > 0.05D)) {
                this.getEntityData().set(SETTLED, false);
                this.restTicks = 0;
            } else {
                this.renderYaw = Mth.rotLerp(0.3F, this.renderYaw, this.getEntityData().get(RENDER_YAW));
                this.renderTumble = Mth.lerp(0.3F, this.renderTumble, this.getEntityData().get(RENDER_TUMBLE));
                this.tumbleSpeed = 0.0F;
            }
        }

        if (!this.getEntityData().get(SETTLED)) {
            if (horSqr > 4.0E-4D) {
                var targetYaw = (float) (Mth.atan2(motion.x, motion.z) * (180.0F / (float) Math.PI));

                this.renderYaw = Mth.rotLerp(grounded ? 0.15F : 0.3F, this.renderYaw, targetYaw);
            }

            if (!grounded) {
                var targetSpin = Mth.clamp((float) motion.length() * 130.0F, 10.0F, 40.0F);

                this.tumbleSpeed = Mth.lerp(0.25F, this.tumbleSpeed, targetSpin);
                this.renderTumble += this.tumbleSpeed;
            } else {
                this.tumbleSpeed *= 0.7F;
                this.renderTumble += this.tumbleSpeed;

                var flat = Math.round((this.renderTumble - 90.0F) / 180.0F) * 180.0F + 90.0F;

                this.renderTumble = Mth.lerp(this.tumbleSpeed < 3.0F ? 0.3F : 0.08F, this.renderTumble, flat);

                if (horSqr < 4.0E-4D && Math.abs(this.renderTumble - flat) < 1.5F)
                    this.restTicks++;
                else
                    this.restTicks = 0;

                if (!this.level().isClientSide() && this.restTicks > 5) {
                    this.renderTumble = flat;
                    this.getEntityData().set(RENDER_YAW, this.renderYaw);
                    this.getEntityData().set(RENDER_TUMBLE, flat);
                    this.getEntityData().set(SETTLED, true);
                }
            }

            if (this.renderTumble > 1800.0F) {
                this.renderTumble -= 1800.0F;
                this.renderTumbleO -= 1800.0F;
            }
        }

        if (!this.level().isClientSide() && this.tickCount > MAX_LIFETIME)
            this.expireToItem();
    }

    private boolean isGrounded() {
        var pos = this.position();
        var box = new AABB(pos.x - 0.05D, pos.y - 0.06D, pos.z - 0.05D, pos.x + 0.05D, pos.y + 0.01D, pos.z + 0.05D);

        return !this.level().noBlockCollision(this, box);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putFloat("renderYaw", this.renderYaw);
        tag.putFloat("renderTumble", this.renderTumble);
        tag.putBoolean("settled", this.getEntityData().get(SETTLED));
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        this.renderYaw = tag.getFloatOr("renderYaw", 0F);
        this.renderTumble = tag.getFloatOr("renderTumble", 90F);
        this.renderYawO = this.renderYaw;
        this.renderTumbleO = this.renderTumble;

        var flat = Math.round((this.renderTumble - 90.0F) / 180.0F) * 180.0F + 90.0F;

        this.getEntityData().set(RENDER_YAW, this.renderYaw);
        this.getEntityData().set(RENDER_TUMBLE, flat);
        this.getEntityData().set(SETTLED, tag.getBooleanOr("settled", false));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (this.tickCount == 0 && this.level().isClientSide() && this.getEntityData().get(SETTLED)) {
            this.renderYaw = this.getEntityData().get(RENDER_YAW);
            this.renderTumble = this.getEntityData().get(RENDER_TUMBLE);
            this.renderYawO = this.renderYaw;
            this.renderTumbleO = this.renderTumble;
        }
    }

    public static void tryPlaceGlowLight(Level level, BlockPos basePos) {
        if (level.isClientSide())
            return;

        BlockPos pos = findLightPos(level, basePos);
        if (pos == null)
            return;

        var fluidState = level.getFluidState(pos);
        BlockState glowState = ReAnimalBlocks.GLOW_LIGHT.defaultBlockState()
                .setValue(GlowLightBlock.WATERLOGGED, fluidState.is(FluidTags.WATER));

        level.setBlock(pos, glowState, 3);
    }

    private static BlockPos findLightPos(Level level, BlockPos base) {
        BlockPos[] candidates = new BlockPos[]{
                base,
                base.above(),
                base.below(),
                base.north(),
                base.south(),
                base.east(),
                base.west()
        };

        for (var pos : candidates) {
            var state = level.getBlockState(pos);
            if (state.is(ReAnimalBlocks.GLOW_LIGHT)) {
                var blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof GlowLightBlockEntity glowLight)
                    glowLight.refresh();

                return null;
            }
        }

        for (var pos : candidates) {
            if (canPlaceLightAt(level, pos))
                return pos;
        }

        return null;
    }

    private static boolean canPlaceLightAt(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.is(ReAnimalBlocks.GLOW_LIGHT))
            return true;

        if (state.isAir())
            return true;

        if (state.is(Blocks.WATER))
            return true;

        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (this.tickCount > 20 && player.getInventory().add(this.getItem().copy())) {
            this.discard();

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.0F;
    }

    private void expireToItem() {
        if (!this.level().isClientSide())
            this.spawnAtLocation((ServerLevel) this.level(), this.getItem().copy());

        this.discard();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationTest<GlowStickEntity> state) {
        state.controller().setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public float getRenderYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.renderYawO, this.renderYaw);
    }

    public float getRenderHeight(float partialTick) {
        return Mth.lerp(partialTick, this.renderHeightO, this.renderHeight);
    }

    public float getRenderTumble(float partialTick) {
        return Mth.lerp(partialTick, this.renderTumbleO, this.renderTumble);
    }

    public static class TrailProvider extends EntityTrailProvider<GlowStickEntity> {

        private static int toColorWithAlpha(int alpha, int rgb) {
            return (alpha << 24) | (rgb & 0xFFFFFF);
        }

        private static int getBaseColor(Item item) {
            return getGlowStickBaseColor(item);
        }

        private static int getGlowStickBaseColor(Item item) {
            if (item == ReAnimalItems.GLOW_STICK_WHITE)
                return 0xFFFFFF;
            if (item == ReAnimalItems.GLOW_STICK_ORANGE)
                return 0xFFA500;
            if (item == ReAnimalItems.GLOW_STICK_MAGENTA)
                return 0xFF00FF;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_BLUE)
                return 0x55FFFF;
            if (item == ReAnimalItems.GLOW_STICK_YELLOW)
                return 0xFFFF55;
            if (item == ReAnimalItems.GLOW_STICK_LIME)
                return 0x55FF55;
            if (item == ReAnimalItems.GLOW_STICK_PINK)
                return 0xFF55FF;
            if (item == ReAnimalItems.GLOW_STICK_GRAY)
                return 0x555555;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_GRAY)
                return 0xAAAAAA;
            if (item == ReAnimalItems.GLOW_STICK_CYAN)
                return 0x00AAAA;
            if (item == ReAnimalItems.GLOW_STICK_PURPLE)
                return 0xAA00AA;
            if (item == ReAnimalItems.GLOW_STICK_BLUE)
                return 0x5555FF;
            if (item == ReAnimalItems.GLOW_STICK_BROWN)
                return 0xAA5500;
            if (item == ReAnimalItems.GLOW_STICK_GREEN)
                return 0x00AA00;
            if (item == ReAnimalItems.GLOW_STICK_RED)
                return 0xFF0000;
            if (item == ReAnimalItems.GLOW_STICK_BLACK)
                return 0x000000;

            return 0xFFFFFF;
        }

        private static int getTailColor(Item item) {
            if (item == ReAnimalItems.GLOW_STICK_WHITE)
                return 0x66E0FF;
            if (item == ReAnimalItems.GLOW_STICK_ORANGE)
                return 0xFF6B2A;
            if (item == ReAnimalItems.GLOW_STICK_MAGENTA)
                return 0xFF7FB8;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_BLUE)
                return 0x66FFC9;
            if (item == ReAnimalItems.GLOW_STICK_YELLOW)
                return 0xFF9A2E;
            if (item == ReAnimalItems.GLOW_STICK_LIME)
                return 0x55FF7A;
            if (item == ReAnimalItems.GLOW_STICK_PINK)
                return 0xFF66B3;
            if (item == ReAnimalItems.GLOW_STICK_GRAY)
                return 0xDADADA;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_GRAY)
                return 0xFFFFFF;
            if (item == ReAnimalItems.GLOW_STICK_CYAN)
                return 0x66E0FF;
            if (item == ReAnimalItems.GLOW_STICK_PURPLE)
                return 0xFF66D6;
            if (item == ReAnimalItems.GLOW_STICK_BLUE)
                return 0x66FFE6;
            if (item == ReAnimalItems.GLOW_STICK_BROWN)
                return 0xFF9A2E;
            if (item == ReAnimalItems.GLOW_STICK_GREEN)
                return 0x7CFF4A;
            if (item == ReAnimalItems.GLOW_STICK_RED)
                return 0xFF9A2E;
            if (item == ReAnimalItems.GLOW_STICK_BLACK)
                return 0x5B2A90;

            return 0x66E0FF;
        }

        private int getFadeInColorForItem() {
            var item = this.entity.getItem().getItem();
            return toColorWithAlpha(0xFF, getBaseColor(item));
        }

        private int getFadeOutColorForItem() {
            var item = this.entity.getItem().getItem();
            return toColorWithAlpha(0x80, getTailColor(item));
        }

        public TrailProvider(GlowStickEntity entity) {
            super(entity);
        }

        @Override
        public Vec3 getTrailPosition(float partialTicks) {
            return this.entity.getPosition(partialTicks).add(0F, 0.15F, 0F);
        }

        @Override
        public int getTrailUpdateFrequency() {
            return 1;
        }

        @Override
        public boolean isTrailAlive() {
            return this.entity.isAlive();
        }

        @Override
        public boolean isTrailGrowing() {
            return this.entity.tickCount > 1;
        }

        @Override
        public int getTrailMaxLength() {
            return 5;
        }

        @Override
        public int getTrailFadeInColor() {
            return getFadeInColorForItem();
        }

        @Override
        public int getTrailFadeOutColor() {
            return getFadeOutColorForItem();
        }

        @Override
        public double getTrailScale() {
            return 0.1F;
        }
    }
}

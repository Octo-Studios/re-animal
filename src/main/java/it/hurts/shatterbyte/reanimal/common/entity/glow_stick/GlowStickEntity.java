package it.hurts.shatterbyte.reanimal.common.entity.glow_stick;

import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import it.hurts.shatterbyte.reanimal.common.block.GlowLightBlock;
import it.hurts.shatterbyte.reanimal.common.blockentity.GlowLightBlockEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalBlocks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GlowStickEntity extends ThrowableItemProjectile implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.glow_stick.idle");
    private static final double BOUNCE_DAMPING = 0.7D;
    private static final double BOUNCE_Y_CUTOFF = 0.18D;
    private static final double STOP_SPEED_SQR = 0.03D * 0.03D;
    private static final int MAX_LIFETIME = 6000;
    private static final int PICKUP_DELAY_TICKS = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float renderYaw;
    private float renderYawO;
    private float renderPitch;
    private float renderPitchO;
    private Vec3 renderMotion = Vec3.ZERO;
    private Vec3 renderMotionO = Vec3.ZERO;
    private float renderRoll;
    private float renderRollO;
    private float rollSpeed;

    public GlowStickEntity(EntityType<? extends GlowStickEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ReAnimalItems.GLOW_STICK_WHITE.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        var motion = this.getDeltaMovement();
        var normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();
        var reflected = motion.subtract(normal.scale(2.0D * motion.dot(normal)));
        motion = reflected.scale(BOUNCE_DAMPING);

        if (result.getDirection().getAxis() == net.minecraft.core.Direction.Axis.Y && Math.abs(motion.y) < BOUNCE_Y_CUTOFF)
            motion = new Vec3(motion.x, 0.0D, motion.z);

        if (motion.lengthSqr() < STOP_SPEED_SQR) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        this.setDeltaMovement(motion);
        this.hasImpulse = true;
    }

    @Override
    public void tick() {
        super.tick();

        this.renderYawO = this.renderYaw;
        this.renderPitchO = this.renderPitch;
        this.renderMotionO = this.renderMotion;
        this.renderRollO = this.renderRoll;
        var motion = this.getDeltaMovement();
        if (this.tickCount > 1)
            this.tryPlaceGlowLight();

        double speedSqr = motion.lengthSqr();
        double lerp = speedSqr > 0.0025D ? 0.25D : 0.12D;
        var targetMotion = speedSqr > STOP_SPEED_SQR ? motion : Vec3.ZERO;
        this.renderMotion = new Vec3(
                Mth.lerp(lerp, this.renderMotion.x, targetMotion.x),
                Mth.lerp(lerp, this.renderMotion.y, targetMotion.y),
                Mth.lerp(lerp, this.renderMotion.z, targetMotion.z)
        );

        double horSqr = this.renderMotion.x * this.renderMotion.x + this.renderMotion.z * this.renderMotion.z;
        if (horSqr > 1.0E-4D) {
            var dir = this.renderMotion.normalize();
            var targetYaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0F / (float) Math.PI));

            this.renderYaw = Mth.rotLerp(0.18F, this.renderYaw, targetYaw);
            this.renderPitch = Mth.lerp(0.12F, this.renderPitch, 0.0F);
        }

        if (speedSqr < STOP_SPEED_SQR && this.onGround()) {
            this.rollSpeed = 0.0F;
            this.renderMotion = Vec3.ZERO;
            this.renderRoll = 0.0F;
        } else {
            float speed = (float) this.renderMotion.length();
            float targetSpin = Mth.clamp(speed * 25.0F, speed > 0.02F ? 1.0F : 0.0F, 12.0F);
            this.rollSpeed = Mth.lerp(0.2F, this.rollSpeed, targetSpin);
            this.rollSpeed *= this.onGround() ? 0.9F : 0.97F;
            this.renderRoll += this.rollSpeed;
        }

        if (!this.level().isClientSide() && this.isNoGravity())
            this.setNoGravity(false);

        if (!this.level().isClientSide() && this.tickCount > MAX_LIFETIME)
            this.expireToItem();
    }

    private void tryPlaceGlowLight() {
        if (this.level().isClientSide())
            return;

        BlockPos pos = this.findLightPos();
        if (pos == null)
            return;

        var fluidState = this.level().getFluidState(pos);
        BlockState glowState = ReAnimalBlocks.GLOW_LIGHT.get().defaultBlockState()
                .setValue(GlowLightBlock.WATERLOGGED, fluidState.is(FluidTags.WATER));

        this.level().setBlock(pos, glowState, 3);
    }

    private BlockPos findLightPos() {
        var base = BlockPos.containing(this.position());
        BlockPos[] candidates = new BlockPos[] {
                base,
                base.above(),
                base.below(),
                base.north(),
                base.south(),
                base.east(),
                base.west()
        };

        for (var pos : candidates) {
            var state = this.level().getBlockState(pos);
            if (state.is(ReAnimalBlocks.GLOW_LIGHT.get())) {
                var blockEntity = this.level().getBlockEntity(pos);
                if (blockEntity instanceof GlowLightBlockEntity glowLight)
                    glowLight.refresh();

                return null;
            }
        }

        for (var pos : candidates) {
            if (this.canPlaceLightAt(pos))
                return pos;
        }

        return null;
    }

    private boolean canPlaceLightAt(BlockPos pos) {
        var state = this.level().getBlockState(pos);
        if (state.is(ReAnimalBlocks.GLOW_LIGHT.get()))
            return true;

        if (state.isAir())
            return true;

        if (state.is(Blocks.WATER))
            return true;

        return false;
    }

    @Override
    public void playerTouch(net.minecraft.world.entity.player.Player player) {
        super.playerTouch(player);

        if (this.level().isClientSide())
            return;

        if (this.tickCount < PICKUP_DELAY_TICKS)
            return;

        if (player.getInventory().add(this.getItem().copy())) {
            this.discard();
        }
    }

    private void expireToItem() {
        if (!this.level().isClientSide())
            this.spawnAtLocation(this.getItem().copy());

        this.discard();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<GlowStickEntity> state) {
        state.getController().setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    public float getRenderYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.renderYawO, this.renderYaw);
    }

    public float getRenderPitch(float partialTick) {
        return Mth.lerp(partialTick, this.renderPitchO, this.renderPitch);
    }

    public Vec3 getRenderMotion(float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, this.renderMotionO.x, this.renderMotion.x),
                Mth.lerp(partialTick, this.renderMotionO.y, this.renderMotion.y),
                Mth.lerp(partialTick, this.renderMotionO.z, this.renderMotion.z)
        );
    }

    public float getRenderRoll(float partialTick) {
        return Mth.lerp(partialTick, this.renderRollO, this.renderRoll);
    }
}

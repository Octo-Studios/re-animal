package it.hurts.shatterbyte.reanimal.common.entity.glow_stick;

import it.hurts.octostudios.octolib.module.particle.trail.EntityTrailProvider;
import it.hurts.shatterbyte.reanimal.common.block.GlowLightBlock;
import it.hurts.shatterbyte.reanimal.common.blockentity.GlowLightBlockEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalBlocks;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GlowStickEntity extends ThrowableItemProjectile implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.glow_stick.idle");
    private static final double BOUNCE_DAMPING = 0.7D;
    private static final double BOUNCE_Y_CUTOFF = 0.18D;
    private static final double STOP_SPEED_SQR = 0.03D * 0.03D;
    private static final int MAX_LIFETIME = 6000;

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

        if (this.tickCount > 20)
            for (var entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox()))
                this.setDeltaMovement(this.position().subtract(entity.position()).normalize().multiply(0.35F, 0F, 0.35F).add(0F, 0.2F, 0F));

        this.renderYawO = this.renderYaw;
        this.renderPitchO = this.renderPitch;
        this.renderMotionO = this.renderMotion;
        this.renderRollO = this.renderRoll;

        var motion = this.getDeltaMovement();

        if (this.tickCount > 1)
            tryPlaceGlowLight(this.level(), BlockPos.containing(this.position()));

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

    public static void tryPlaceGlowLight(Level level, BlockPos basePos) {
        if (level.isClientSide())
            return;

        BlockPos pos = findLightPos(level, basePos);
        if (pos == null)
            return;

        var fluidState = level.getFluidState(pos);
        BlockState glowState = ReAnimalBlocks.GLOW_LIGHT.get().defaultBlockState()
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
            if (state.is(ReAnimalBlocks.GLOW_LIGHT.get())) {
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
        if (state.is(ReAnimalBlocks.GLOW_LIGHT.get()))
            return true;

        if (state.isAir())
            return true;

        if (state.is(Blocks.WATER))
            return true;

        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide())
            return InteractionResult.SUCCESS;

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

    @OnlyIn(Dist.CLIENT)
    public static class TrailProvider extends EntityTrailProvider<GlowStickEntity> {

        private static int toColorWithAlpha(int alpha, int rgb) {
            return (alpha << 24) | (rgb & 0xFFFFFF);
        }

        private static int getBaseColor(Item item) {
            return getGlowStickBaseColor(item);
        }

        private static int getGlowStickBaseColor(Item item) {
            if (item == ReAnimalItems.GLOW_STICK_WHITE.get())
                return 0xFFFFFF;
            if (item == ReAnimalItems.GLOW_STICK_ORANGE.get())
                return 0xFFA500;
            if (item == ReAnimalItems.GLOW_STICK_MAGENTA.get())
                return 0xFF00FF;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_BLUE.get())
                return 0x55FFFF;
            if (item == ReAnimalItems.GLOW_STICK_YELLOW.get())
                return 0xFFFF55;
            if (item == ReAnimalItems.GLOW_STICK_LIME.get())
                return 0x55FF55;
            if (item == ReAnimalItems.GLOW_STICK_PINK.get())
                return 0xFF55FF;
            if (item == ReAnimalItems.GLOW_STICK_GRAY.get())
                return 0x555555;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_GRAY.get())
                return 0xAAAAAA;
            if (item == ReAnimalItems.GLOW_STICK_CYAN.get())
                return 0x00AAAA;
            if (item == ReAnimalItems.GLOW_STICK_PURPLE.get())
                return 0xAA00AA;
            if (item == ReAnimalItems.GLOW_STICK_BLUE.get())
                return 0x5555FF;
            if (item == ReAnimalItems.GLOW_STICK_BROWN.get())
                return 0xAA5500;
            if (item == ReAnimalItems.GLOW_STICK_GREEN.get())
                return 0x00AA00;
            if (item == ReAnimalItems.GLOW_STICK_RED.get())
                return 0xFF0000;
            if (item == ReAnimalItems.GLOW_STICK_BLACK.get())
                return 0x000000;

            return 0xFFFFFF;
        }

        private static int getTailColor(Item item) {
            if (item == ReAnimalItems.GLOW_STICK_WHITE.get())
                return 0x66E0FF;
            if (item == ReAnimalItems.GLOW_STICK_ORANGE.get())
                return 0xFF6B2A;
            if (item == ReAnimalItems.GLOW_STICK_MAGENTA.get())
                return 0xFF7FB8;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_BLUE.get())
                return 0x66FFC9;
            if (item == ReAnimalItems.GLOW_STICK_YELLOW.get())
                return 0xFF9A2E;
            if (item == ReAnimalItems.GLOW_STICK_LIME.get())
                return 0x55FF7A;
            if (item == ReAnimalItems.GLOW_STICK_PINK.get())
                return 0xFF66B3;
            if (item == ReAnimalItems.GLOW_STICK_GRAY.get())
                return 0xDADADA;
            if (item == ReAnimalItems.GLOW_STICK_LIGHT_GRAY.get())
                return 0xFFFFFF;
            if (item == ReAnimalItems.GLOW_STICK_CYAN.get())
                return 0x66E0FF;
            if (item == ReAnimalItems.GLOW_STICK_PURPLE.get())
                return 0xFF66D6;
            if (item == ReAnimalItems.GLOW_STICK_BLUE.get())
                return 0x66FFE6;
            if (item == ReAnimalItems.GLOW_STICK_BROWN.get())
                return 0xFF9A2E;
            if (item == ReAnimalItems.GLOW_STICK_GREEN.get())
                return 0x7CFF4A;
            if (item == ReAnimalItems.GLOW_STICK_RED.get())
                return 0xFF9A2E;
            if (item == ReAnimalItems.GLOW_STICK_BLACK.get())
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
            return this.entity.getPosition(partialTicks).add(0F, 0.35F, 0F);
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

package it.hurts.shatterbyte.reanimal.common.entity.ball;

import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import it.hurts.shatterbyte.reanimal.init.ReAnimalParticleTypes;
import it.hurts.shatterbyte.shatterlib.module.particle.trail.EntityTrailProvider;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BallEntity extends Entity implements ItemSupplier, GeoEntity {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Vector3fc> IMPACT = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> IMPACT_ID = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.INT);

    private static final double GRAVITY = 0.04D;
    private static final double AIR_DRAG = 0.99D;
    private static final double BOUNCE_DAMPING = 0.55D;
    private static final double GROUND_BOUNCE_DAMPING = 0.75D;
    private static final double FOOT_KICK_SPEED = 0.28D;

    private static final int[] DEFAULT_COLORS = {15961002, 16701501, 15961002, 16701501};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf previousRotation = new Quaternionf();
    private Vec3 previousPosition = Vec3.ZERO;
    private int impactAge = 10;
    private int footKickCooldown;

    @Nullable
    private EntityReference<Player> thrower;
    @Nullable
    private Mob playingAnimal;
    private int returnTicks;
    @Getter
    private long lastThrownAt;
    public int playHits;

    public BallEntity(EntityType<? extends BallEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.previousRotation.set(this.rotation);

            var movement = this.position().subtract(this.previousPosition);
            var horizontal = movement.horizontalDistance();

            if (this.getVehicle() instanceof Wolf wolf)
                this.rotation.rotationYXZ(-wolf.getViewYRot(1F) * Mth.DEG_TO_RAD, wolf.getViewXRot(1F) * Mth.DEG_TO_RAD, 0F);
            else if (this.tickCount > 1 && horizontal > 0D) {
                var angle = (float) Math.min(horizontal / (this.getBbWidth() * 0.5D), Mth.PI * 0.25F);

                this.rotation.premul(new Quaternionf().rotationAxis(angle, (float) (movement.z / horizontal), 0F, (float) (-movement.x / horizontal))).normalize();
            }

            this.previousPosition = this.position();
            this.impactAge++;

            return;
        }

        if (this.isPassenger())
            return;

        if (this.footKickCooldown > 0)
            this.footKickCooldown--;

        var movement = this.getDeltaMovement();

        if (this.isInWater())
            movement = new Vec3(movement.x * 0.97D, movement.y * 0.8D + (this.getFluidHeight(FluidTags.WATER) - this.getBbHeight() * 0.5D) * 0.08D, movement.z * 0.97D);
        else
            movement = movement.add(0D, -GRAVITY, 0D);

        var start = this.position();

        this.setDeltaMovement(movement);
        this.move(MoverType.SELF, movement);

        if (!this.bounceOffBalls(start, movement)) {
            var collided = this.getDeltaMovement();
            var hitX = this.horizontalCollision && collided.x == 0D;
            var hitZ = this.horizontalCollision && collided.z == 0D;
            var bounceY = this.verticalCollision && Math.abs(movement.y) > GRAVITY * 2D;
            var friction = this.onGround() && !bounceY ? 0.94D : AIR_DRAG;

            this.setDeltaMovement(
                    hitX ? -movement.x * BOUNCE_DAMPING : movement.x * friction,
                    this.verticalCollision ? (bounceY ? -movement.y * (movement.y < 0D ? GROUND_BOUNCE_DAMPING : BOUNCE_DAMPING) : 0D) : movement.y,
                    hitZ ? -movement.z * BOUNCE_DAMPING : movement.z * friction
            );

            if (hitX || bounceY || hitZ)
                this.impact(new Vec3(hitX ? -movement.x : 0D, bounceY ? -movement.y : 0D, hitZ ? -movement.z : 0D));
        }

        if (this.returnTicks > 0)
            this.returnTicks--;
    }

    private boolean bounceOffBalls(Vec3 start, Vec3 movement) {
        var traveled = this.position().subtract(start);
        var distanceSqr = traveled.lengthSqr();
        var velocity = this.onGround() && traveled.y == 0D ? movement.multiply(1D, 0D, 1D) : movement;
        BallEntity hitBall = null;
        var hitTime = 1D;

        for (var ball : this.level().getEntitiesOfClass(BallEntity.class, this.getBoundingBox().expandTowards(start.subtract(this.position())), ball -> ball != this && ball.isAlive() && !ball.isPassenger())) {
            var offset = start.subtract(ball.position());
            var radius = (this.getBbWidth() + ball.getBbWidth()) * 0.5D;
            var separation = offset.lengthSqr() - radius * radius;
            var time = 0D;

            if (separation > 0D) {
                var projection = offset.dot(traveled);
                var discriminant = projection * projection - distanceSqr * separation;

                if (projection >= 0D || discriminant < 0D)
                    continue;

                time = (-projection - Math.sqrt(discriminant)) / distanceSqr;
            }

            if (time > hitTime)
                continue;

            var normal = offset.add(traveled.scale(time)).normalize();

            if (velocity.subtract(ball.getDeltaMovement()).dot(normal) >= 0D)
                continue;

            hitBall = ball;
            hitTime = time;
        }

        if (hitBall == null)
            return false;

        var contact = start.add(traveled.scale(hitTime));
        var normal = contact.subtract(hitBall.position()).normalize();
        var impulse = normal.scale(hitBall.getDeltaMovement().subtract(velocity).dot(normal) * (1D + BOUNCE_DAMPING) * 0.5D);

        this.setPos(contact);
        this.setDeltaMovement(velocity.add(impulse));
        hitBall.setDeltaMovement(hitBall.getDeltaMovement().subtract(impulse));
        this.markHurt();
        hitBall.markHurt();
        this.impact(impulse);
        hitBall.impact(impulse.reverse());

        return true;
    }

    public void kick(Vec3 movement) {
        this.stopRiding();
        this.setDeltaMovement(movement);
        this.markHurt();
        this.footKickCooldown = 8;

        if (this.tickCount > 0)
            this.impact(movement);
    }

    private void impact(Vec3 impulse) {
        var speed = impulse.length();

        if (speed <= GRAVITY * 2D)
            return;

        this.entityData.set(IMPACT, impulse.normalize().scale(Math.min(speed, 1D)).toVector3f());
        this.entityData.set(IMPACT_ID, this.entityData.get(IMPACT_ID) + 1);
        this.markHurt();
        this.playSound(SoundEvents.SLIME_BLOCK_HIT, (float) Math.min(speed * 0.35D, 0.35D), 1.3F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (IMPACT_ID.equals(key) && this.level().isClientSide() && this.tickCount > 0) {
            this.impactAge = 0;

            var impulse = this.getImpact();
            var contact = this.position().add(0D, this.getBbHeight() * 0.5D, 0D).subtract(impulse.normalize().scale(this.getBbWidth() * 0.45D));

            this.level().addParticle(ReAnimalParticleTypes.BALL_IMPACT, contact.x, contact.y, contact.z, impulse.x, impulse.y, impulse.z);
        }
    }

    public Quaternionf getRenderRotation(float partialTick) {
        if (this.getVehicle() instanceof Wolf wolf)
            return new Quaternionf().rotationYXZ(-wolf.getViewYRot(partialTick) * Mth.DEG_TO_RAD, wolf.getViewXRot(partialTick) * Mth.DEG_TO_RAD, 0F);

        return new Quaternionf(this.previousRotation).slerp(this.rotation, partialTick);
    }

    public Vec3 getCarriedPosition(Wolf wolf, float partialTick) {
        var head = new Vec3(wolf.isBaby() ? 0D : 1D / 16D, (wolf.isBaby() ? 5.75D : 10.5D) / 16D, (wolf.isBaby() ? 4D : 7D) / 16D)
                .scale(wolf.getScale()).yRot(-Mth.rotLerp(partialTick, wolf.yBodyRotO, wolf.yBodyRot) * Mth.DEG_TO_RAD);
        var mouth = new Vec3(wolf.isBaby() ? 0D : -1D / 16D, -(wolf.isBaby() ? 0.75D : 1.5D) / 16D, 5D / 16D)
                .scale(wolf.getScale()).add(0D, 0D, this.getBbWidth() * 0.3D)
                .xRot(-wolf.getViewXRot(partialTick) * Mth.DEG_TO_RAD).yRot(-wolf.getViewYRot(partialTick) * Mth.DEG_TO_RAD);

        return wolf.getPosition(partialTick).add(head).add(mouth).add(0D, -this.getBbHeight() * 0.5D, 0D);
    }

    public Vec3 getImpact() {
        return new Vec3(this.entityData.get(IMPACT));
    }

    public float getSquash(float partialTick) {
        var time = Math.max(0F, this.impactAge - 1F + partialTick);

        return time < 10F ? (float) (this.getImpact().length() * 0.34D * Math.exp(-time * 0.35D) * Math.cos(time * 0.7D)) : 0F;
    }

    public int getPanelColor(int part) {
        var data = this.getItem().get(DataComponents.CUSTOM_MODEL_DATA);
        var color = data == null ? null : data.getColor(part);

        return color == null ? DEFAULT_COLORS[part] : color;
    }

    public void setThrower(Player player) {
        this.thrower = EntityReference.of(player);
        this.playHits = 0;
        this.returnTicks = 0;
        this.lastThrownAt = this.level().getGameTime();
    }

    boolean isAvailableFor(Mob mob) {
        return (!this.isPassenger() || this.getVehicle() == mob) && (this.playingAnimal == null || !this.playingAnimal.isAlive() || this.playingAnimal == mob);
    }

    void setPlayingAnimal(@Nullable Mob mob) {
        this.playingAnimal = mob;
    }

    @Nullable
    public Player getThrower() {
        return EntityReference.getPlayer(this.thrower, this.level());
    }

    public boolean isReturning() {
        return this.returnTicks > 0;
    }

    public void returnTo(Player player) {
        var flightTicks = 20 + Mth.ceil(this.distanceTo(player) * 0.5D);
        var target = player.position().add(player.getKnownMovement().multiply(1D, 0D, 1D).scale(flightTicks))
                .add(0D, player.getBbHeight() * 0.5D - this.getBbHeight() * 0.5D, 0D);
        var delta = target.subtract(this.position());
        var horizontalDrag = (1D - Math.pow(AIR_DRAG, flightTicks)) / (1D - AIR_DRAG);
        var movement = new Vec3(delta.x / horizontalDrag, delta.y / flightTicks + GRAVITY * (flightTicks + 1) * 0.5D, delta.z / horizontalDrag);

        if (this.isInWater())
            movement = new Vec3(movement.x / 0.97D,
                    (movement.y - GRAVITY - (this.getFluidHeight(FluidTags.WATER) - this.getBbHeight() * 0.5D) * 0.08D) / 0.8D,
                    movement.z / 0.97D);

        this.kick(movement);
        this.returnTicks = 100;
        this.playHits = 0;
        this.playSound(SoundEvents.SNOWBALL_THROW, 0.5F, 1F);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return !this.isPassenger();
    }

    @Override
    public void playerTouch(Player player) {
        if (this.getBoundingBox().intersects(player.getBoundingBox().expandTowards(player.getKnownMovement().multiply(1D, 0D, 1D))))
            this.push(player);
    }

    @Override
    public void push(Entity entity) {
        if (this.level().isClientSide() || this.isPassenger() || this.footKickCooldown > 0 || !(entity instanceof Player player)
                || this.getY() > player.getY() + this.getBbHeight() * 0.5D)
            return;

        var direction = this.position().subtract(player.position()).multiply(1D, 0D, 1D).normalize();
        var movement = this.getDeltaMovement();
        var playerMovement = player.getKnownMovement();

        if (playerMovement.subtract(movement).dot(direction) <= 0D)
            return;

        this.setThrower(player);
        this.kick(direction.scale(FOOT_KICK_SPEED + Math.max(0D, playerMovement.dot(direction))).add(0D, movement.y, 0D));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            this.setThrower(player);
            this.kick(player.getLookAngle().scale(0.7D).add(0D, 0.18D, 0D));

            return true;
        }

        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!this.level().isClientSide() && player.getInventory().add(this.getItem().copy()))
            this.discard();

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ITEM, stack.copyWithCount(1));
    }

    @Override
    public ItemStack getPickResult() {
        return this.getItem().copy();
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, new ItemStack(ReAnimalItems.BALL));
        builder.define(IMPACT, new Vector3f());
        builder.define(IMPACT_ID, 0);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        this.setItem(tag.read("Item", ItemStack.CODEC).orElseGet(() -> new ItemStack(ReAnimalItems.BALL)));
        this.thrower = EntityReference.read(tag, "Thrower");
        this.playHits = tag.getIntOr("PlayHits", 0);
        this.returnTicks = tag.getIntOr("ReturnTicks", 0);
        this.lastThrownAt = tag.getLongOr("LastThrownAt", 0L);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        tag.store("Item", ItemStack.CODEC, this.getItem());
        EntityReference.store(this.thrower, tag, "Thrower");
        tag.putInt("PlayHits", this.playHits);
        tag.putInt("ReturnTicks", this.returnTicks);
        tag.putLong("LastThrownAt", this.lastThrownAt);
    }

    public static class TrailProvider extends EntityTrailProvider<BallEntity> {
        public TrailProvider(BallEntity entity) {
            super(entity);
        }

        @Override
        public Vec3 getTrailPosition(float partialTick) {
            return this.entity.getPosition(partialTick).add(0D, this.entity.getBbHeight() * 0.5D, 0D);
        }

        @Override
        public boolean isTrailGrowing() {
            return !this.entity.isPassenger() && this.entity.tickCount > 1 && this.entity.getDeltaMovement().lengthSqr() > 0.12D * 0.12D;
        }

        @Override
        public int getTrailMaxLength() {
            return 4;
        }

        @Override
        public int getTrailFadeInColor() {
            return 0x55FFFFFF;
        }

        @Override
        public int getTrailFadeOutColor() {
            return 0x00FFFFFF;
        }

        @Override
        public double getTrailScale() {
            return 0.1D;
        }
    }
}

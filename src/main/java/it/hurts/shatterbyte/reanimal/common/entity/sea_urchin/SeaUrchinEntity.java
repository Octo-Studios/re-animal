package it.hurts.shatterbyte.reanimal.common.entity.sea_urchin;

import com.mojang.serialization.Dynamic;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.hurts.shatterbyte.reanimal.init.ReAnimalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SeaUrchinEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.sea_urchin.idle");
    private static final RawAnimation WOBBLE = RawAnimation.begin().thenLoop("animation.sea_urchin.wobble");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SeaUrchinEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);

        this.setPathfindingMalus(PathType.WATER, 0F);
    }

    @Override
    protected void customServerAiStep() {
        var level = this.level();
        var profiler = level.getProfiler();

        profiler.push("seaUrchinBrain");
        ((Brain<SeaUrchinEntity>) this.getBrain()).tick((ServerLevel) this.level(), this);
        profiler.pop();

        profiler.push("seaUrchinActivityUpdate");
        SeaUrchinAI.updateActivity(this);
        profiler.pop();

        super.customServerAiStep();
    }

    @Override
    public void travel(Vec3 vec) {
        if (this.isEffectiveAi() && this.isInWater() && this.onGround()) {
            var speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 25;

            this.moveRelative(speed * 0.1F, vec);
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else
            super.travel(vec);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType type) {
        return true;
    }


    @Override
    protected Brain.Provider<SeaUrchinEntity> brainProvider() {
        return SeaUrchinAI.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return SeaUrchinAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ReAnimalTags.Items.SEA_URCHIN_FOOD);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var baby = ReAnimalEntities.SEA_URCHIN.get().create(level);

        if (baby != null)
            baby.setBaby(true);

        return baby;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }

    private PlayState mainPredicate(AnimationState<SeaUrchinEntity> state) {
        var controller = state.getController();
        var entity = state.getAnimatable();

        if (state.isMoving()) {
            controller.setAnimation(WOBBLE);
        } else {
            controller.setAnimation(IDLE);
        }

        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 4D)
                .add(Attributes.MOVEMENT_SPEED, 0.025D)
                .add(NeoForgeMod.SWIM_SPEED, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 4D);
    }
}
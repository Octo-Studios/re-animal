package it.hurts.shatterbyte.reanimal.common.block;

import it.hurts.shatterbyte.reanimal.common.entity.ostrich.OstrichEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OstrichEggBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4.5D, 0.0D, 4.5D, 11.5D, 10.0D, 11.5D);
    private static final float HATCH_CHANCE = 0.5F;
    private static final int HATCH_TICK_CHANCE = 3;
    private static final int STEP_BREAK_CHANCE = 100;
    private static final int FALL_BREAK_CHANCE = 3;

    public OstrichEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully())
            this.destroyEgg(level, state, pos, entity, STEP_BREAK_CHANCE);

        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (!(entity instanceof Zombie))
            this.destroyEgg(level, state, pos, entity, FALL_BREAK_CHANCE);

        super.fallOn(level, state, pos, entity, fallDistance);
    }

    private void destroyEgg(Level level, BlockState state, BlockPos pos, Entity entity, int chance) {
        if (!(level instanceof ServerLevel serverLevel) || !this.canDestroyEgg(serverLevel, entity) || level.getRandom().nextInt(chance) != 0)
            return;

        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        level.levelEvent(2001, pos, Block.getId(state));
        level.destroyBlock(pos, false);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
    }

    private boolean canDestroyEgg(ServerLevel level, Entity entity) {
        if (entity instanceof OstrichEntity || entity instanceof Bat || !(entity instanceof LivingEntity))
            return false;

        return entity instanceof Player || level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(HATCH_TICK_CHANCE) != 0)
            return;

        level.removeBlock(pos, false);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
        level.levelEvent(2001, pos, Block.getId(state));

        if (random.nextFloat() >= HATCH_CHANCE) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);

            return;
        }

        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);

        var ostrich = ReAnimalEntities.OSTRICH.create(level, EntitySpawnReason.BREEDING);

        if (ostrich == null)
            return;

        ostrich.setBaby(true);
        ostrich.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);

        level.addFreshEntity(ostrich);
    }
}

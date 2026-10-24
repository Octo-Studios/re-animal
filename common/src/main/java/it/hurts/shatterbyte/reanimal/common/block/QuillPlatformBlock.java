package it.hurts.shatterbyte.reanimal.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class QuillPlatformBlock extends FaceAttachedHorizontalDirectionalBlock {
    private static final VoxelShape FLOOR_SHAPE = FaceAttachedHorizontalDirectionalBlock.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    private static final VoxelShape CEILING_SHAPE = FaceAttachedHorizontalDirectionalBlock.box(0.0D, 9.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_SHAPE = FaceAttachedHorizontalDirectionalBlock.box(0.0D, 0.0D, 9.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE = FaceAttachedHorizontalDirectionalBlock.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 7.0D);
    private static final VoxelShape WEST_SHAPE = FaceAttachedHorizontalDirectionalBlock.box(9.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_SHAPE = FaceAttachedHorizontalDirectionalBlock.box(0.0D, 0.0D, 0.0D, 7.0D, 16.0D, 16.0D);

    public QuillPlatformBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(FACE, AttachFace.WALL).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var support = FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state).getOpposite();
        return FaceAttachedHorizontalDirectionalBlock.canAttach(level, pos, support);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource random) {
        return FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state).getOpposite() == direction && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getPlatformShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private VoxelShape getPlatformShape(BlockState state) {
        var face = state.getValue(FACE);
        var direction = state.getValue(FACING);

        return switch (face) {
            case FLOOR -> FLOOR_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case WALL -> switch (direction) {
                case NORTH -> NORTH_SHAPE;
                case SOUTH -> SOUTH_SHAPE;
                case WEST -> WEST_SHAPE;
                case EAST -> EAST_SHAPE;
                default -> FLOOR_SHAPE;
            };
        };
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);

        var shape = this.getPlatformShape(state).move(pos.getX(), pos.getY(), pos.getZ());
        var entityBox = entity.getBoundingBox();

        boolean intersects = false;

        for (var box : shape.toAabbs()) {
            if (box.intersects(entityBox)) {
                intersects = true;

                break;
            }
        }

        if (!intersects)
            return;

        if (entity instanceof LivingEntity living) {
            living.makeStuckInBlock(state, new Vec3(0.75D, 1D, 0.75D));

            if (!(level instanceof ServerLevel serverLevel))
                return;

            var motion = living.getDeltaMovement();

            if (living.hurtOrSimulate(serverLevel.damageSources().cactus(), 1F)) {
                living.setDeltaMovement(motion);
                living.setLastHurtByMob(null);
            }
        }
    }
}
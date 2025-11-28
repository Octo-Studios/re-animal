package it.hurts.shatterbyte.reanimal.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class QuillPlatformBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<QuillPlatformBlock> CODEC = BlockBehaviour.simpleCodec(QuillPlatformBlock::new);

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
    public MapCodec<QuillPlatformBlock> codec() {
        return CODEC;
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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state).getOpposite() == direction && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
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
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        if (!level.isClientSide()) {
            entity.hurt(entity.damageSources().generic(), 1.0F);
        }
    }
}

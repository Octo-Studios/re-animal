package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BallItem extends Item {
    public BallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            var ball = ReAnimalEntities.BALL.create(level, EntitySpawnReason.TRIGGERED);

            ball.setItem(stack);
            ball.setThrower(player);
            ball.setPos(player.getEyePosition().add(0D, -ball.getBbHeight() * 0.5D, 0D));
            ball.kick(player.getLookAngle().scale(0.8D).add(player.getDeltaMovement()));
            level.addFreshEntity(ball);

            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 1F);
        player.awardStat(Stats.ITEM_USED.get(this));

        return InteractionResult.SUCCESS;
    }
}

package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

public class BallItem extends Item {
    public static final CustomModelData DEFAULT_MODEL_DATA = new CustomModelData(List.of(), List.of(), List.of("pink", "yellow", "pink", "yellow"), List.of());

    public BallItem(Properties properties) {
        super(properties.component(DataComponents.CUSTOM_MODEL_DATA, DEFAULT_MODEL_DATA));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);

        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player))
            return false;

        var power = BowItem.getPowerForTime(this.getUseDuration(stack, entity) - remainingTime);

        if (!level.isClientSide()) {
            var ball = ReAnimalEntities.BALL.create(level, EntitySpawnReason.TRIGGERED);

            ball.setItem(stack);
            ball.setThrower(player);
            ball.setPos(player.getEyePosition().add(0D, -ball.getBbHeight() * 0.5D, 0D));
            ball.kick(player.getLookAngle().scale(0.25D + power * 0.75D).add(player.getDeltaMovement()));
            level.addFreshEntity(ball);

            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }

        player.awardStat(Stats.ITEM_USED.get(this));

        return true;
    }
}

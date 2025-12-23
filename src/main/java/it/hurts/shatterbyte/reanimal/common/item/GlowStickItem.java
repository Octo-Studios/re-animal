package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GlowStickItem extends Item {
    public GlowStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);

        if (!level.isClientSide()) {
            var projectile = ReAnimalEntities.GLOW_STICK.get().create(level);

            if (projectile instanceof GlowStickEntity glowStick) {
                glowStick.setItem(stack.copyWithCount(1));
                glowStick.setOwner(player);
                glowStick.setPos(player.getEyePosition());
                glowStick.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, 1.0F, 0.8F);

                level.addFreshEntity(glowStick);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));

        if (!player.getAbilities().instabuild)
            stack.shrink(1);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(entity instanceof Player player))
            return;

        boolean isHeld = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!isHeld)
            return;

        GlowStickEntity.tryPlaceGlowLight(level, BlockPos.containing(player.position()));
    }
}

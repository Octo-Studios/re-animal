package it.hurts.shatterbyte.reanimal.common.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class EggItem<T extends ThrowableItemProjectile> extends net.minecraft.world.item.EggItem {
    private final Supplier<EntityType<T>> projectile;

    public EggItem(Properties properties, Supplier<EntityType<T>> projectile) {
        super(properties);

        this.projectile = projectile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            var projectile = this.projectile.get().create(level);

            if (projectile instanceof ThrowableItemProjectile egg) {
                egg.setItem(stack);
                egg.setOwner(player);
                egg.setPos(player.getEyePosition());
                egg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, 1.5F, 1F);

                level.addFreshEntity(egg);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));

        if (!player.getAbilities().instabuild)
            stack.shrink(1);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
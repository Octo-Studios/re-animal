package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GlowStickItem extends Item {
    private final GlowStickItem base;

    public GlowStickItem(Properties properties) {
        this(properties, null);
    }

    public GlowStickItem(Properties properties, GlowStickItem base) {
        super(properties);

        this.base = base;
    }

    public boolean isSticky() {
        return this.base != null;
    }

    public GlowStickItem getBase() {
        return this.base == null ? this : this.base;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.9F + level.getRandom().nextFloat() * 0.2F);

        if (!level.isClientSide()) {
            var projectile = ReAnimalEntities.GLOW_STICK.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);

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

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof Player player))
            return;

        boolean isHeld = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!isHeld)
            return;

        GlowStickEntity.tryPlaceGlowLight(level, BlockPos.containing(player.position()));
    }
}

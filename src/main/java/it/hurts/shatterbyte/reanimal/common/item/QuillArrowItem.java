package it.hurts.shatterbyte.reanimal.common.item;

import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.QuillArrowEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class QuillArrowItem extends ArrowItem {
    public QuillArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public QuillArrowEntity createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        return new QuillArrowEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        var arrow = new QuillArrowEntity(level, position.x(), position.y(), position.z(), stack.copyWithCount(1), ItemStack.EMPTY);

        arrow.pickup = AbstractArrow.Pickup.ALLOWED;

        return arrow;
    }
}

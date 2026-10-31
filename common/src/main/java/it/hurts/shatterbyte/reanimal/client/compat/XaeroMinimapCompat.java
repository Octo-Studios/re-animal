package it.hurts.shatterbyte.reanimal.client.compat;

import it.hurts.shatterbyte.reanimal.common.entity.butterfly.ButterflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.dragonfly.DragonflyEntity;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import it.hurts.shatterbyte.reanimal.common.entity.pigeon.PigeonEntity;
import it.hurts.shatterbyte.reanimal.common.entity.starfish.StarfishEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class XaeroMinimapCompat {
    private static final String[] PIGEON_COLORS = {"gray", "black", "brown", "white"};

    private XaeroMinimapCompat() {
    }

    public static Object getAgeVariant(Identifier texture, EntityRenderer<?, ?> renderer, Entity entity) {
        var baby = entity instanceof LivingEntity living && living.isBaby();

        return getTexture(entity, baby ? "baby" : "adult");
    }

    public static Object getPigeonVariant(Identifier texture, EntityRenderer<?, ?> renderer, Entity entity) {
        if (!(entity instanceof PigeonEntity pigeon) || pigeon.isBaby())
            return getTexture(entity, "baby");

        return getTexture(entity, "adult_" + PIGEON_COLORS[Math.floorMod(pigeon.getVariant(), PIGEON_COLORS.length)]);
    }

    public static Object getColorVariant(Identifier texture, EntityRenderer<?, ?> renderer, Entity entity) {
        var variant = switch (entity) {
            case ButterflyEntity butterfly -> butterfly.getVariant();
            case DragonflyEntity dragonfly -> dragonfly.getVariant();
            case JellyfishEntity jellyfish -> jellyfish.getVariant();
            case StarfishEntity starfish -> starfish.getVariant();
            default -> 0;
        };

        return getTexture(entity, String.valueOf(variant));
    }

    private static Identifier getTexture(Entity entity, String suffix) {
        var id = EntityType.getKey(entity.getType());

        return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/entity/" + id.getPath() + "_" + suffix + ".png");
    }
}

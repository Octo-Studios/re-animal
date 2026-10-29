package it.hurts.shatterbyte.reanimal.common.entity.hedgehog;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class HeadHedgehogs {
    private static Accessor accessor;

    private HeadHedgehogs() {
    }

    public static void init(Accessor accessor) {
        HeadHedgehogs.accessor = accessor;
    }

    public static @Nullable HeadHedgehogData get(Player player) {
        return accessor.get(player);
    }

    public static boolean has(Player player) {
        return accessor.get(player) != null;
    }

    public static void set(Player player, @Nullable HeadHedgehogData data) {
        accessor.set(player, data);
    }

    public interface Accessor {
        @Nullable
        HeadHedgehogData get(Player player);

        void set(Player player, @Nullable HeadHedgehogData data);
    }
}

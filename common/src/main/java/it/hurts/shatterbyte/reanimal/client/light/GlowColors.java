package it.hurts.shatterbyte.reanimal.client.light;

import it.hurts.shatterbyte.reanimal.common.item.GlowStickItem;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;

import java.util.IdentityHashMap;
import java.util.Map;

public final class GlowColors {
    private static final int FALLBACK = 0xF2F7FF;

    private static Map<GlowStickItem, Integer> table;

    private GlowColors() {
    }

    public static int of(GlowStickItem item) {
        if (table == null)
            table = build();

        return table.getOrDefault(item.getBase(), FALLBACK);
    }

    private static Map<GlowStickItem, Integer> build() {
        var map = new IdentityHashMap<GlowStickItem, Integer>();

        map.put(ReAnimalItems.GLOW_STICK_WHITE, 0xF2F7FF);
        map.put(ReAnimalItems.GLOW_STICK_ORANGE, 0xFF8A1F);
        map.put(ReAnimalItems.GLOW_STICK_MAGENTA, 0xE02EFF);
        map.put(ReAnimalItems.GLOW_STICK_LIGHT_BLUE, 0x4FC3FF);
        map.put(ReAnimalItems.GLOW_STICK_YELLOW, 0xFFE82E);
        map.put(ReAnimalItems.GLOW_STICK_LIME, 0x7CFF2E);
        map.put(ReAnimalItems.GLOW_STICK_PINK, 0xFF6FB5);
        map.put(ReAnimalItems.GLOW_STICK_GRAY, 0x9AA3A8);
        map.put(ReAnimalItems.GLOW_STICK_LIGHT_GRAY, 0xCDD5DA);
        map.put(ReAnimalItems.GLOW_STICK_CYAN, 0x1FE0E0);
        map.put(ReAnimalItems.GLOW_STICK_PURPLE, 0xA23BFF);
        map.put(ReAnimalItems.GLOW_STICK_BLUE, 0x2E4DFF);
        map.put(ReAnimalItems.GLOW_STICK_BROWN, 0xC27A32);
        map.put(ReAnimalItems.GLOW_STICK_GREEN, 0x2EE05A);
        map.put(ReAnimalItems.GLOW_STICK_RED, 0xFF2A2A);
        map.put(ReAnimalItems.GLOW_STICK_BLACK, 0x6A33B8);

        return map;
    }
}

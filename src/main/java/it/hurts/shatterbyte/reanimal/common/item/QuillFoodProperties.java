package it.hurts.shatterbyte.reanimal.common.item;

import net.minecraft.world.food.FoodProperties;

import java.util.List;
import java.util.Optional;

public class QuillFoodProperties {
    public static final FoodProperties RAW_HEDGEHOG_MEAT = new FoodProperties(3, 0.3F, false, 0.0F, Optional.empty(), List.of());
    public static final FoodProperties COOKED_HEDGEHOG_MEAT = new FoodProperties(7, 0.8F, false, 0.0F, Optional.empty(), List.of());
    public static final FoodProperties RAW_OSTRICH_MEAT = new FoodProperties(3, 0.3F, false, 0.0F, Optional.empty(), List.of());
    public static final FoodProperties COOKED_OSTRICH_MEAT = new FoodProperties(7, 0.8F, false, 0.0F, Optional.empty(), List.of());
}

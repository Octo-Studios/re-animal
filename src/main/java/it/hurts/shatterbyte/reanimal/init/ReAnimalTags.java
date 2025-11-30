package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ReAnimalTags {
    public static final class Items {
        public static final TagKey<Item> PIGEON_FOOD = Items.create("pigeon_food");
        public static final TagKey<Item> KIWI_FOOD = Items.create("kiwi_food");
        public static final TagKey<Item> OSTRICH_FOOD = Items.create("ostrich_food");
        public static final TagKey<Item> CAPYBARA_FOOD = Items.create("capybara_food");
        public static final TagKey<Item> SEAL_FOOD = Items.create("seal_food");
        public static final TagKey<Item> HEDGEHOG_FOOD = Items.create("hedgehog_food");
        public static final TagKey<Item> HIPPOPOTAMUS_FOOD = Items.create("hippopotamus_food");
        public static final TagKey<Item> GIRAFFE_FOOD = Items.create("giraffe_food");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ReAnimal.MODID, name));
        }
    }
}

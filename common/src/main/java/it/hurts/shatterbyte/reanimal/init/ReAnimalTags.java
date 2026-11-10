package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ReAnimalTags {
    public static final class EntityTypes {
        public static final TagKey<EntityType<?>> HEDGEHOG_PREY = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_prey"));
        public static final TagKey<EntityType<?>> DRAGONFLY_PREY = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "dragonfly_prey"));
    }

    public static final class Blocks {
        public static final TagKey<Block> RED_PANDA_CLIMBABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "red_panda_climbable"));
        public static final TagKey<Block> SEA_URCHIN_FOOD = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "sea_urchin_food"));
        public static final TagKey<Block> GIRAFFE_FOOD = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "giraffe_food"));
        public static final TagKey<Block> HEDGEHOG_DIGGABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "hedgehog_diggable"));
    }

    public static final class Items {
        public static final TagKey<Item> BUTTERFLY_FOOD = Items.create("butterfly_food");
        public static final TagKey<Item> PIGEON_FOOD = Items.create("pigeon_food");
        public static final TagKey<Item> TOUCAN_FOOD = Items.create("toucan_food");
        public static final TagKey<Item> KIWI_FOOD = Items.create("kiwi_food");
        public static final TagKey<Item> OSTRICH_FOOD = Items.create("ostrich_food");
        public static final TagKey<Item> CAPYBARA_FOOD = Items.create("capybara_food");
        public static final TagKey<Item> RED_PANDA_FOOD = Items.create("red_panda_food");
        public static final TagKey<Item> MEERKAT_FOOD = Items.create("meerkat_food");
        public static final TagKey<Item> SEAL_FOOD = Items.create("seal_food");
        public static final TagKey<Item> HEDGEHOG_FOOD = Items.create("hedgehog_food");
        public static final TagKey<Item> HIPPOPOTAMUS_FOOD = Items.create("hippopotamus_food");
        public static final TagKey<Item> CROCODILE_FOOD = Items.create("crocodile_food");
        public static final TagKey<Item> GIRAFFE_FOOD = Items.create("giraffe_food");
        public static final TagKey<Item> VULTURE_FOOD = Items.create("vulture_food");
        public static final TagKey<Item> PENGUIN_FOOD = Items.create("penguin_food");
        public static final TagKey<Item> SEA_URCHIN_FOOD = Items.create("sea_urchin_food");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ReAnimal.MODID, name));
        }
    }
}

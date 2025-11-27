package it.hurts.shatterbyte.reanimal.registry;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ReAnimalItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ReAnimal.MODID);

    public static final DeferredHolder<Item, SpawnEggItem> HEDGEHOG_SPAWN_EGG = ITEMS.register("hedgehog_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.HEDGEHOG.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> OSTRICH_SPAWN_EGG = ITEMS.register("ostrich_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.OSTRICH.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> KIWI_SPAWN_EGG = ITEMS.register("kiwi_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.KIWI.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> PIGEON_SPAWN_EGG = ITEMS.register("pigeon_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.PIGEON.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> BUTTERFLY_SPAWN_EGG = ITEMS.register("butterfly_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.BUTTERFLY.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> CAPYBARA_SPAWN_EGG = ITEMS.register("capybara_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.CAPYBARA.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> HIPPOPOTAMUS_SPAWN_EGG = ITEMS.register("hippopotamus_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.HIPPOPOTAMUS.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> GIRAFFE_SPAWN_EGG = ITEMS.register("giraffe_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.GIRAFFE.get(), 0xffffff, 0xffffff, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> DRAGONFLY_SPAWN_EGG = ITEMS.register("dragonfly_spawn_egg", () -> new SpawnEggItem(ReAnimalEntities.DRAGONFLY.get(), 0xffffff, 0xffffff, new Item.Properties()));
}
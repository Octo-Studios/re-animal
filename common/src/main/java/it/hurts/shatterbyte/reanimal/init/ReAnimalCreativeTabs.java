package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;

public class ReAnimalCreativeTabs {
    public static CreativeModeTab REANIMAL_TAB;

    public static void init() {
        REANIMAL_TAB = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(ReAnimal.MODID, ReAnimal.MODID),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup." + ReAnimal.MODID))
                        .icon(() -> ReAnimalItems.BUTTERFLY_SPAWN_EGG.getDefaultInstance())
                        .displayItems((params, output) -> {
                            output.acceptAll(ReAnimalItems.ALL_ITEMS.stream()
                                    .map(item -> item.getDefaultInstance())
                                    .toList());

                            output.acceptAll(List.of(
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.CRAMPS),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.LONG_CRAMPS),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.STRONG_CRAMPS),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.GLOWING),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.LONG_GLOWING),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.STRONG_GLOWING),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.QUILL),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.LONG_QUILL),
                                    PotionContents.createItemStack(Items.POTION, ReAnimalPotions.STRONG_QUILL),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.CRAMPS),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.LONG_CRAMPS),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.STRONG_CRAMPS),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.GLOWING),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.LONG_GLOWING),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.STRONG_GLOWING),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.QUILL),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.LONG_QUILL),
                                    PotionContents.createItemStack(Items.SPLASH_POTION, ReAnimalPotions.STRONG_QUILL),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.CRAMPS),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.LONG_CRAMPS),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.STRONG_CRAMPS),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.GLOWING),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.LONG_GLOWING),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.STRONG_GLOWING),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.QUILL),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.LONG_QUILL),
                                    PotionContents.createItemStack(Items.LINGERING_POTION, ReAnimalPotions.STRONG_QUILL),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.CRAMPS),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.LONG_CRAMPS),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.STRONG_CRAMPS),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.GLOWING),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.LONG_GLOWING),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.STRONG_GLOWING),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.QUILL),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.LONG_QUILL),
                                    PotionContents.createItemStack(Items.TIPPED_ARROW, ReAnimalPotions.STRONG_QUILL)
                            ));
                        })
                        .build()
        );
    }
}

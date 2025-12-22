package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

@EventBusSubscriber
public class ReAnimalCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReAnimal.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> REANIMAL_TAB = CREATIVE_TABS.register(ReAnimal.MODID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ReAnimal.MODID))
            .icon(() -> ReAnimalItems.BUTTERFLY_SPAWN_EGG.get().getDefaultInstance())
            .build());

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }

    @SubscribeEvent
    public static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == REANIMAL_TAB.get()) {
            event.acceptAll(ReAnimalItems.ITEMS.getEntries().stream().map(entry -> entry.get().getDefaultInstance()).toList(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            var quillHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.QUILL.getKey());
            var longQuillHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.LONG_QUILL.getKey());
            var strongQuillHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.STRONG_QUILL.getKey());
            var crampsHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.CRAMPS.getKey());
            var longCrampsHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.LONG_CRAMPS.getKey());
            var strongCrampsHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.STRONG_CRAMPS.getKey());
            var glowingHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.GLOWING.getKey());
            var longGlowingHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.LONG_GLOWING.getKey());
            var strongGlowingHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.STRONG_GLOWING.getKey());

            var quillPotion = PotionContents.createItemStack(Items.POTION, quillHolder);
            var longQuillPotion = PotionContents.createItemStack(Items.POTION, longQuillHolder);
            var strongQuillPotion = PotionContents.createItemStack(Items.POTION, strongQuillHolder);
            var crampsPotion = PotionContents.createItemStack(Items.POTION, crampsHolder);
            var longCrampsPotion = PotionContents.createItemStack(Items.POTION, longCrampsHolder);
            var strongCrampsPotion = PotionContents.createItemStack(Items.POTION, strongCrampsHolder);
            var glowingPotion = PotionContents.createItemStack(Items.POTION, glowingHolder);
            var longGlowingPotion = PotionContents.createItemStack(Items.POTION, longGlowingHolder);
            var strongGlowingPotion = PotionContents.createItemStack(Items.POTION, strongGlowingHolder);

            var quillSplash = PotionContents.createItemStack(Items.SPLASH_POTION, quillHolder);
            var longQuillSplash = PotionContents.createItemStack(Items.SPLASH_POTION, longQuillHolder);
            var strongQuillSplash = PotionContents.createItemStack(Items.SPLASH_POTION, strongQuillHolder);
            var crampsSplash = PotionContents.createItemStack(Items.SPLASH_POTION, crampsHolder);
            var longCrampsSplash = PotionContents.createItemStack(Items.SPLASH_POTION, longCrampsHolder);
            var strongCrampsSplash = PotionContents.createItemStack(Items.SPLASH_POTION, strongCrampsHolder);
            var glowingSplash = PotionContents.createItemStack(Items.SPLASH_POTION, glowingHolder);
            var longGlowingSplash = PotionContents.createItemStack(Items.SPLASH_POTION, longGlowingHolder);
            var strongGlowingSplash = PotionContents.createItemStack(Items.SPLASH_POTION, strongGlowingHolder);

            var quillLingering = PotionContents.createItemStack(Items.LINGERING_POTION, quillHolder);
            var longQuillLingering = PotionContents.createItemStack(Items.LINGERING_POTION, longQuillHolder);
            var strongQuillLingering = PotionContents.createItemStack(Items.LINGERING_POTION, strongQuillHolder);
            var crampsLingering = PotionContents.createItemStack(Items.LINGERING_POTION, crampsHolder);
            var longCrampsLingering = PotionContents.createItemStack(Items.LINGERING_POTION, longCrampsHolder);
            var strongCrampsLingering = PotionContents.createItemStack(Items.LINGERING_POTION, strongCrampsHolder);
            var glowingLingering = PotionContents.createItemStack(Items.LINGERING_POTION, glowingHolder);
            var longGlowingLingering = PotionContents.createItemStack(Items.LINGERING_POTION, longGlowingHolder);
            var strongGlowingLingering = PotionContents.createItemStack(Items.LINGERING_POTION, strongGlowingHolder);

            var quillArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, quillHolder);
            var longQuillArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, longQuillHolder);
            var strongQuillArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, strongQuillHolder);
            var crampsArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, crampsHolder);
            var longCrampsArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, longCrampsHolder);
            var strongCrampsArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, strongCrampsHolder);
            var glowingArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, glowingHolder);
            var longGlowingArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, longGlowingHolder);
            var strongGlowingArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, strongGlowingHolder);

            event.acceptAll(
                    List.of(
                            crampsPotion, longCrampsPotion, strongCrampsPotion,
                            glowingPotion, longGlowingPotion, strongGlowingPotion,
                            quillPotion, longQuillPotion, strongQuillPotion,
                            crampsSplash, longCrampsSplash, strongCrampsSplash,
                            glowingSplash, longGlowingSplash, strongGlowingSplash,
                            quillSplash, longQuillSplash, strongQuillSplash,
                            crampsLingering, longCrampsLingering, strongCrampsLingering,
                            glowingLingering, longGlowingLingering, strongGlowingLingering,
                            quillLingering, longQuillLingering, strongQuillLingering,
                            crampsArrow, longCrampsArrow, strongCrampsArrow,
                            glowingArrow, longGlowingArrow, strongGlowingArrow,
                            quillArrow, longQuillArrow, strongQuillArrow
                    )
            );
        }
    }
}

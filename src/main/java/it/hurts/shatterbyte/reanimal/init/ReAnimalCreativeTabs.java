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
            event.acceptAll(ReAnimalItems.ITEMS.getEntries().stream().map(entry -> entry.get().getDefaultInstance()).toList(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);

            var quillHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.QUILL.getKey());
            var longQuillHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.LONG_QUILL.getKey());
            var strongQuillHolder = BuiltInRegistries.POTION.getHolderOrThrow(ReAnimalPotions.STRONG_QUILL.getKey());

            var quillPotion = PotionContents.createItemStack(Items.POTION, quillHolder);
            var longQuillPotion = PotionContents.createItemStack(Items.POTION, longQuillHolder);
            var strongQuillPotion = PotionContents.createItemStack(Items.POTION, strongQuillHolder);

            var quillSplash = PotionContents.createItemStack(Items.SPLASH_POTION, quillHolder);
            var longQuillSplash = PotionContents.createItemStack(Items.SPLASH_POTION, longQuillHolder);
            var strongQuillSplash = PotionContents.createItemStack(Items.SPLASH_POTION, strongQuillHolder);

            var quillLingering = PotionContents.createItemStack(Items.LINGERING_POTION, quillHolder);
            var longQuillLingering = PotionContents.createItemStack(Items.LINGERING_POTION, longQuillHolder);
            var strongQuillLingering = PotionContents.createItemStack(Items.LINGERING_POTION, strongQuillHolder);

            var quillArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, quillHolder);
            var longQuillArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, longQuillHolder);
            var strongQuillArrow = PotionContents.createItemStack(Items.TIPPED_ARROW, strongQuillHolder);

            event.acceptAll(
                    List.of(
                            quillPotion, longQuillPotion, strongQuillPotion,
                            quillSplash, longQuillSplash, strongQuillSplash,
                            quillLingering, longQuillLingering, strongQuillLingering,
                            quillArrow, longQuillArrow, strongQuillArrow
                    )
            );
        }
    }
}

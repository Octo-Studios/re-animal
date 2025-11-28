package it.hurts.shatterbyte.reanimal.init;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
        if (event.getTab() == REANIMAL_TAB.get())
            event.acceptAll(ReAnimalItems.ITEMS.getEntries().stream().map(entry -> entry.get().getDefaultInstance()).toList(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
    }
}
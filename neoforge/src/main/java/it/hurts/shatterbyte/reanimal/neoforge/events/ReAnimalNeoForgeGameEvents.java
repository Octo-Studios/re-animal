package it.hurts.shatterbyte.reanimal.neoforge.events;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.effect.QuillMobEffect;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HedgehogEntity;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ReAnimal.MODID)
public class ReAnimalNeoForgeGameEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (HedgehogEntity.tryDropFromBlock(event.getEntity(), event.getHand(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult((event.getEntity().level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME));
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide())
            HedgehogEntity.onEntityJoin(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        var attacker = event.getSource().getEntity();

        if (HedgehogEntity.tryBlockPhantomDamage(event.getEntity(), attacker))
            event.setCanceled(true);

        QuillMobEffect.onLivingHurt(event.getEntity(), event.getSource(), event.getAmount());
    }
}

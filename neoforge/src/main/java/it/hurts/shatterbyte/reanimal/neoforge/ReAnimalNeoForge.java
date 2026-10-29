package it.hurts.shatterbyte.reanimal.neoforge;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HeadHedgehogData;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.HeadHedgehogs;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntityDataSerializers;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(ReAnimal.MODID)
public class ReAnimalNeoForge {
    public ReAnimalNeoForge(IEventBus modBus) {
        var initialized = new java.util.concurrent.atomic.AtomicBoolean();

        modBus.addListener(RegisterEvent.class, event -> {
            if (initialized.compareAndSet(false, true))
                ReAnimal.init();
        });

        var serializers = DeferredRegister.<EntityDataSerializer<?>>create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, ReAnimal.MODID);
        serializers.register("hedgehog_state", () -> ReAnimalEntityDataSerializers.HEDGEHOG_STATE);
        serializers.register(modBus);

        var attachments = DeferredRegister.<AttachmentType<?>>create(NeoForgeRegistries.ATTACHMENT_TYPES, ReAnimal.MODID);
        var headHedgehog = attachments.register("head_hedgehog", () -> AttachmentType.<HeadHedgehogData>builder(() -> null)
                .serialize(HeadHedgehogData.MAP_CODEC)
                .sync(HeadHedgehogData.STREAM_CODEC)
                .build());
        attachments.register(modBus);

        HeadHedgehogs.init(new HeadHedgehogs.Accessor() {
            @Override
            public HeadHedgehogData get(Player player) {
                return player.getExistingDataOrNull(headHedgehog.get());
            }

            @Override
            public void set(Player player, HeadHedgehogData data) {
                if (data == null)
                    player.removeData(headHedgehog.get());
                else
                    player.setData(headHedgehog.get(), data);
            }
        });

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            new ReAnimalNeoForgeClient(modBus);
        }
    }
}

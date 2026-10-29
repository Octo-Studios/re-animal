package it.hurts.shatterbyte.reanimal.common.entity.hedgehog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public record HeadHedgehogData(CompoundTag tag, boolean baby, ItemStack stack) {
    public static final MapCodec<HeadHedgehogData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("entity").forGetter(HeadHedgehogData::tag),
            Codec.BOOL.optionalFieldOf("baby", false).forGetter(HeadHedgehogData::baby),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("stack", ItemStack.EMPTY).forGetter(HeadHedgehogData::stack)
    ).apply(instance, HeadHedgehogData::new));

    public static final Codec<HeadHedgehogData> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, HeadHedgehogData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, HeadHedgehogData::tag,
            ByteBufCodecs.BOOL, HeadHedgehogData::baby,
            ItemStack.OPTIONAL_STREAM_CODEC, HeadHedgehogData::stack,
            HeadHedgehogData::new
    );

    public static HeadHedgehogData of(HedgehogEntity hedgehog) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, hedgehog.level().registryAccess());

        hedgehog.saveWithoutId(output);

        return new HeadHedgehogData(output.buildResult(), hedgehog.isBaby(), hedgehog.getStack().copy());
    }

    public HedgehogEntity create(ServerLevel level) {
        var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), this.tag);

        return EntityType.create(ReAnimalEntities.HEDGEHOG, input, level, EntitySpawnReason.LOAD)
                .map(HedgehogEntity.class::cast)
                .orElse(null);
    }
}

package it.hurts.shatterbyte.reanimal.common.recipe;

import com.mojang.serialization.MapCodec;
import it.hurts.shatterbyte.reanimal.init.ReAnimalItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

public class BallRecipe extends CustomRecipe {
    private static final BallRecipe INSTANCE = new BallRecipe();

    public static final RecipeSerializer<BallRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3 || !input.getItem(4).isEmpty())
            return false;

        for (var slot = 0; slot < 9; slot += 2) {
            if (slot != 4 && !input.getItem(slot).has(DataComponents.DYE))
                return false;
        }

        for (var slot = 1; slot < 9; slot += 2) {
            if (!input.getItem(slot).is(Items.LEATHER))
                return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        var ball = new ItemStack(ReAnimalItems.BALL);
        var dyes = List.of(0, 2, 8, 6).stream().map(slot -> input.getItem(slot).get(DataComponents.DYE)).toList();

        ball.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), dyes.stream().map(DyeColor::getName).toList(), List.of()));

        return ball;
    }

    @Override
    public RecipeSerializer<BallRecipe> getSerializer() {
        return SERIALIZER;
    }
}

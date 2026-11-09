package it.hurts.shatterbyte.reanimal.client.compat.jei;

import it.hurts.shatterbyte.reanimal.client.compat.BallRecipeDisplays;
import it.hurts.shatterbyte.reanimal.common.recipe.BallRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class BallCraftingExtension implements ICraftingCategoryExtension<BallRecipe> {
    private final List<List<ItemStack>> inputs;
    private final List<ItemStack> outputs;

    public BallCraftingExtension() {
        var variants = BallRecipeDisplays.createRecipes().stream().map(RecipeHolder::value).toList();

        this.inputs = IntStream.range(0, 9)
                .mapToObj(slot -> variants.stream()
                        .flatMap(recipe -> recipe.getIngredients().get(slot).stream().flatMap(Ingredient::items))
                        .distinct()
                        .map(ItemStack::new)
                        .toList())
                .toList();
        this.outputs = variants.stream().map(recipe -> recipe.assemble(CraftingInput.EMPTY)).toList();
    }

    @Override
    public List<SlotDisplay> getIngredients(RecipeHolder<BallRecipe> recipe) {
        return this.inputs.stream().<SlotDisplay>map(stacks -> new SlotDisplay.Composite(stacks.stream()
                        .<SlotDisplay>map(stack -> new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack)))
                        .toList()))
                .toList();
    }

    @Override
    public int getWidth(RecipeHolder<BallRecipe> recipe) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<BallRecipe> recipe) {
        return 3;
    }

    @Override
    public void setRecipe(RecipeHolder<BallRecipe> recipe, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
        craftingGridHelper.createAndSetInputs(builder, this.inputs, 3, 3);
        craftingGridHelper.createAndSetOutputs(builder, this.outputs);
    }

    @Override
    public void onDisplayedIngredientsUpdate(RecipeHolder<BallRecipe> recipe, List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
        var dyes = new ArrayList<>(this.inputs.getFirst().stream().map(ItemStack::getItem).toList());

        Collections.shuffle(dyes);

        focuses.getItemStackFocuses(RecipeIngredientRole.INPUT)
                .map(focus -> focus.getTypedValue().getIngredient().getItem())
                .filter(dyes::contains)
                .findFirst()
                .ifPresent(dye -> Collections.swap(dyes, 0, dyes.indexOf(dye)));

        var leather = this.inputs.get(1).getFirst();
        var input = CraftingInput.of(3, 3, List.of(new ItemStack(dyes.get(0)), leather, new ItemStack(dyes.get(1)), leather,
                ItemStack.EMPTY, leather, new ItemStack(dyes.get(2)), leather, new ItemStack(dyes.get(3))));

        for (var slot : List.of(0, 2, 6, 8))
            slots.get(slot).createDisplayOverrides().add(input.getItem(slot));

        slots.getLast().createDisplayOverrides().add(recipe.value().assemble(input));
    }
}

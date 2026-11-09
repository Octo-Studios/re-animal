package it.hurts.shatterbyte.reanimal.client.compat;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.recipe.BallRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BallRecipeDisplays {
    private BallRecipeDisplays() {
    }

    public static List<RecipeHolder<ShapedRecipe>> createRecipes() {
        var recipes = new ArrayList<RecipeHolder<ShapedRecipe>>();

        for (var color : DyeColor.values())
            recipes.add(createRecipe(color.getName(), List.of(color, color, color, color)));

        recipes.add(createRecipe("multicolor", List.of(DyeColor.RED, DyeColor.WHITE, DyeColor.YELLOW, DyeColor.BLUE)));

        return recipes;
    }

    private static RecipeHolder<ShapedRecipe> createRecipe(String name, List<DyeColor> colors) {
        var dyes = colors.stream()
                .map(color -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_dye"))))
                .toList();
        var leather = new ItemStack(Items.LEATHER);
        var input = CraftingInput.of(3, 3, List.of(dyes.get(0), leather, dyes.get(1), leather, ItemStack.EMPTY, leather, dyes.get(3), leather, dyes.get(2)));
        var ingredients = input.items().stream()
                .map(stack -> stack.isEmpty() ? Optional.<Ingredient>empty() : Optional.of(Ingredient.of(stack.getItem())))
                .toList();
        var recipe = new ShapedRecipe(new Recipe.CommonInfo(false), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "ball"),
                new ShapedRecipePattern(3, 3, ingredients, Optional.empty()), ItemStackTemplate.fromNonEmptyStack(new BallRecipe().assemble(input)));

        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ReAnimal.MODID, "ball/" + name)), recipe);
    }
}

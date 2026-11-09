package it.hurts.shatterbyte.reanimal.client.compat.rei;

import it.hurts.shatterbyte.reanimal.client.compat.BallRecipeDisplays;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;

public class ReAnimalReiPlugin implements REIClientPlugin {
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (var recipe : BallRecipeDisplays.createRecipes())
            registry.add(DefaultCraftingDisplay.of(recipe));
    }
}

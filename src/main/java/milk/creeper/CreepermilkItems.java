package milk.creeper;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import milk.creeper.recipe.ModRecipes;
import milk.creeper.criterion.ModCriteria;

public class CreepermilkItems implements ModInitializer {
    @Override
    public void onInitialize() {
        ModRegistry.registerItems();
        ModRecipes.registerRecipes();
        ModCriteria.registerCriteria();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.addAfter(Items.MILK_BUCKET, ModRegistry.CREEPER_MILK_BUCKET));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.addAfter(Items.MILK_BUCKET, ModRegistry.CREEPER_MILK_BUCKET));
    }
} 
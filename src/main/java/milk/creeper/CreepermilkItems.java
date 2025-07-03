package milk.creeper;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import milk.creeper.recipe.ModRecipes;
import milk.creeper.item.CreepermilkBucketItem;
import net.minecraft.item.ItemGroups;

public class CreepermilkItems implements ModInitializer {
    public static final Item CREEPER_MILK_BUCKET = new CreepermilkBucketItem(new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET));

    @Override
    public void onInitialize() {
        registerItems();
        ModRecipes.registerRecipes();
        registerItemGroups();
    }

    public static void registerItems() {
        Registry.register(Registries.ITEM, new Identifier(ModRegistry.MOD_ID, "creeper_milk_bucket"), CREEPER_MILK_BUCKET);
    }

    public static void registerItemGroups() {
        // Add to FOOD_AND_DRINK item group
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(content -> {
            content.addAfter(Items.MILK_BUCKET, CREEPER_MILK_BUCKET);
        });
        
        // Add to TOOLS item group (like normal milk bucket)
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.addAfter(Items.MILK_BUCKET, CREEPER_MILK_BUCKET);
        });
    }
} 
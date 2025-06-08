package milk.creeper;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import milk.creeper.item.CreepermilkBucketItem;

import java.util.function.Function;

public class ModRegistry {

    // Define MOD_ID here for consistency, or reference from CreepermilkItems if preferred
    public static final String MOD_ID = "creepermilk";

    public static Item CREEPER_MILK_BUCKET;

    public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        // Create the item key.
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name));

        // Create the item instance.
        Item item = itemFactory.apply(settings.registryKey(itemKey));

        // Register the item.
        Registry.register(Registries.ITEM, itemKey, item);

        return item;
    }

    public static void registerItems() {
        CREEPER_MILK_BUCKET = register("creeper_milk_bucket", CreepermilkBucketItem::new, 
                new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET));
    }
} 
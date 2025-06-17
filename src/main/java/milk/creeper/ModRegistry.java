package milk.creeper;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import milk.creeper.item.CreepermilkBucketItem;

public class ModRegistry {

    // Define MOD_ID here for consistency, or reference from CreepermilkItems if preferred
    public static final String MOD_ID = "creepermilk";

    public static Item CREEPER_MILK_BUCKET;

    public static void registerItems() {
        CREEPER_MILK_BUCKET = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "creeper_milk_bucket"),
                new CreepermilkBucketItem(new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)));
    }
} 
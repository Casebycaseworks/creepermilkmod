package milk.creeper

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object Creepermilk : ModInitializer {
    private val logger = LoggerFactory.getLogger("creepermilk")

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Initializing Creeper Milk!")
        
        // Register items, recipes, and criteria
        ModRegistry.registerItems()
        milk.creeper.recipe.ModRecipes.registerRecipes()
        milk.creeper.criterion.ModCriteria.registerCriteria()
        
        // Register items in creative tabs
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.item.ItemGroups.FOOD_AND_DRINK).register { entries ->
            entries.addAfter(net.minecraft.item.Items.MILK_BUCKET, ModRegistry.CREEPER_MILK_BUCKET)
        }
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.item.ItemGroups.TOOLS).register { entries ->
            entries.addAfter(net.minecraft.item.Items.MILK_BUCKET, ModRegistry.CREEPER_MILK_BUCKET)
        }
        
        logger.info("Creeper Milk registration complete!")
    }
} 
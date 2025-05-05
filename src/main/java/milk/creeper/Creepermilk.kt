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
    }
}
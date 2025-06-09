package milk.creeper.recipe;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {
    
    public static final RecipeSerializer<ShapelessRecipe> CREEPER_MILK_TO_MILK_SERIALIZER = 
        CreeperMilkToMilkRecipeSerializer.INSTANCE;
    
    public static void registerRecipes() {
        Registry.register(Registries.RECIPE_SERIALIZER, 
            Identifier.of("creepermilk", "creeper_milk_to_milk"), 
            CREEPER_MILK_TO_MILK_SERIALIZER);
    }
} 
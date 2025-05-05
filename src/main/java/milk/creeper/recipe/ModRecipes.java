package milk.creeper.recipe;

import milk.creeper.ModRegistry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {
    public static RecipeSerializer<ConsumeBucketShapelessRecipe> CONSUME_BUCKET_SHAPELESS_SERIALIZER;
    public static RecipeType<ConsumeBucketShapelessRecipe> CONSUME_BUCKET_RECIPE_TYPE;

    public static void registerRecipes() {
        CONSUME_BUCKET_SHAPELESS_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER, new Identifier(ModRegistry.MOD_ID, "crafting_shapeless_consume_bucket"),
                new ConsumeBucketShapelessRecipeSerializer()
        );

        CONSUME_BUCKET_RECIPE_TYPE = Registry.register(
                Registries.RECIPE_TYPE, new Identifier(ModRegistry.MOD_ID, "crafting_shapeless_consume_bucket"),
                new RecipeType<ConsumeBucketShapelessRecipe>() { // Anonymous class implementing RecipeType
                    @Override
                    public String toString() {
                        return ModRegistry.MOD_ID + ":crafting_shapeless_consume_bucket";
                    }
                }
        );
    }
} 
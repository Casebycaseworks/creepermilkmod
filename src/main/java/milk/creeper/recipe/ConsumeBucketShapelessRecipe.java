package milk.creeper.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.util.collection.DefaultedList;

public class ConsumeBucketShapelessRecipe extends ShapelessRecipe {

    public ConsumeBucketShapelessRecipe(String group, CraftingRecipeCategory category, ItemStack output, DefaultedList<Ingredient> input) {
        super(group, category, output, input);
    }

    public DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
        DefaultedList<ItemStack> list = DefaultedList.ofSize(input.size(), ItemStack.EMPTY);
        
        for (int i = 0; i < list.size(); ++i) {
            ItemStack item = input.getStackInSlot(i);
            // In 1.21.5, we just add empty stacks as remainders since the hasRecipeRemainder methods don't exist
            // The recipe remainder logic is now handled differently through components or other mechanisms
            list.set(i, ItemStack.EMPTY);
        }
        
        return list;
    }

    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return ModRecipes.CONSUME_BUCKET_SHAPELESS_SERIALIZER;
    }
} 
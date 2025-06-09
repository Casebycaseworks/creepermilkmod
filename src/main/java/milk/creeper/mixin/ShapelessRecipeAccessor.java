package milk.creeper.mixin;

import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ShapelessRecipe.class)
public interface ShapelessRecipeAccessor {
    
    @Accessor("group")
    String getGroupAccessor();
    
    @Accessor("category")
    CraftingRecipeCategory getCategoryAccessor();
    
    @Accessor("result")
    ItemStack getResultAccessor();
    
    @Accessor("ingredients")
    List<Ingredient> getIngredientsAccessor();
} 
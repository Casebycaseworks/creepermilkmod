package milk.creeper.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import milk.creeper.recipe.CreeperMilkToMilkRecipe;
import milk.creeper.ModRegistry;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {
    
    @Shadow @Final private RecipeInputInventory input;
    
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void modifyRemainders(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        // Create CraftingRecipeInput from the inventory
        CraftingRecipeInput recipeInput = input.createRecipeInput();
        
        // Check if the current recipe is our custom recipe
        Recipe<?> recipe = player.getWorld().getRecipeManager()
            .getFirstMatch(RecipeType.CRAFTING, recipeInput, player.getWorld())
            .map(entry -> entry.value())
            .orElse(null);
            
        if (recipe instanceof CreeperMilkToMilkRecipe) {
            // For our custom recipe, manually handle remainders
            DefaultedList<ItemStack> remainders = DefaultedList.ofSize(input.size(), ItemStack.EMPTY);
            
            for (int i = 0; i < input.size(); i++) {
                ItemStack inputStack = input.getStack(i);
                if (inputStack.getItem() == ModRegistry.CREEPER_MILK_BUCKET) {
                    // Don't leave any remainder for creeper milk bucket in this recipe
                    remainders.set(i, ItemStack.EMPTY);
                } else {
                    // Use default remainder for other items
                    remainders.set(i, inputStack.getRecipeRemainder());
                }
            }
            
            // Apply the custom remainders
            for (int i = 0; i < remainders.size(); i++) {
                ItemStack remainder = remainders.get(i);
                ItemStack currentStack = input.getStack(i);
                
                if (!currentStack.isEmpty()) {
                    input.removeStack(i, 1);
                    currentStack = input.getStack(i);
                }
                
                if (!remainder.isEmpty()) {
                    if (currentStack.isEmpty()) {
                        input.setStack(i, remainder);
                    } else if (ItemStack.areItemsAndComponentsEqual(currentStack, remainder)) {
                        remainder.increment(currentStack.getCount());
                        input.setStack(i, remainder);
                    } else if (!player.getInventory().insertStack(remainder)) {
                        player.dropItem(remainder, false);
                    }
                }
            }
        }
    }
} 
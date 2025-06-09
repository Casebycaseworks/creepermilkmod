package milk.creeper.recipe;

import com.mojang.serialization.MapCodec;
import milk.creeper.mixin.ShapelessRecipeAccessor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.ShapelessRecipe;

public class CreeperMilkToMilkRecipeSerializer extends ShapelessRecipe.Serializer {
    
    public static final CreeperMilkToMilkRecipeSerializer INSTANCE = new CreeperMilkToMilkRecipeSerializer();

    @Override
    public MapCodec<ShapelessRecipe> codec() {
        return super.codec().xmap(
            shapelessRecipe -> {
                // Convert ShapelessRecipe to CreeperMilkToMilkRecipe
                ShapelessRecipeAccessor accessor = (ShapelessRecipeAccessor) shapelessRecipe;
                return new CreeperMilkToMilkRecipe(
                    accessor.getGroupAccessor(), 
                    accessor.getCategoryAccessor(), 
                    accessor.getResultAccessor(), 
                    accessor.getIngredientsAccessor()
                );
            },
            // Identity function - just return the input
            recipe -> recipe
        );
    }

    @Override
    public PacketCodec<RegistryByteBuf, ShapelessRecipe> packetCodec() {
        return super.packetCodec().xmap(
            shapelessRecipe -> {
                // Convert ShapelessRecipe to CreeperMilkToMilkRecipe
                ShapelessRecipeAccessor accessor = (ShapelessRecipeAccessor) shapelessRecipe;
                return new CreeperMilkToMilkRecipe(
                    accessor.getGroupAccessor(), 
                    accessor.getCategoryAccessor(), 
                    accessor.getResultAccessor(), 
                    accessor.getIngredientsAccessor()
                );
            },
            // Identity function - just return the input
            recipe -> recipe
        );
    }
} 
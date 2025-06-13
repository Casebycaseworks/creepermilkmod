package milk.creeper.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.RecipeSerializer;

public class CreeperMilkToMilkRecipeSerializer implements RecipeSerializer<CreeperMilkToMilkRecipe> {
    
    private static final ShapelessRecipe.Serializer SHAPELESS_SERIALIZER = new ShapelessRecipe.Serializer();
    
    @Override
    public MapCodec<CreeperMilkToMilkRecipe> codec() {
        return SHAPELESS_SERIALIZER.codec().xmap(
            shapelessRecipe -> new CreeperMilkToMilkRecipe(
                shapelessRecipe.getGroup(),
                shapelessRecipe.getCategory(),
                shapelessRecipe.getResult(null),
                shapelessRecipe.getIngredients()
            ),
            creeperMilkRecipe -> new ShapelessRecipe(
                creeperMilkRecipe.getGroup(),
                creeperMilkRecipe.getCategory(),
                creeperMilkRecipe.getResult(null),
                creeperMilkRecipe.getIngredients()
            )
        );
    }
    
    @Override
    public PacketCodec<RegistryByteBuf, CreeperMilkToMilkRecipe> packetCodec() {
        return SHAPELESS_SERIALIZER.packetCodec().xmap(
            shapelessRecipe -> new CreeperMilkToMilkRecipe(
                shapelessRecipe.getGroup(),
                shapelessRecipe.getCategory(),
                shapelessRecipe.getResult(null),
                shapelessRecipe.getIngredients()
            ),
            creeperMilkRecipe -> new ShapelessRecipe(
                creeperMilkRecipe.getGroup(),
                creeperMilkRecipe.getCategory(),
                creeperMilkRecipe.getResult(null),
                creeperMilkRecipe.getIngredients()
            )
        );
    }
} 
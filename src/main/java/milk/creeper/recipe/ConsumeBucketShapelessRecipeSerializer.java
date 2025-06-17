package milk.creeper.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.collection.DefaultedList;

public class ConsumeBucketShapelessRecipeSerializer implements RecipeSerializer<ConsumeBucketShapelessRecipe> {

    private static final int MAX_INGREDIENTS = 9;

    private static final MapCodec<ConsumeBucketShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
            CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapelessRecipe::getCategory),
            ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.getResult(null)),
            Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients").flatXmap(ingredients -> {
                Ingredient[] ingredientsArray = ingredients.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
                if (ingredientsArray.length == 0) {
                    return DataResult.error(() -> "No ingredients for shapeless recipe");
                }
                if (ingredientsArray.length > MAX_INGREDIENTS) {
                    return DataResult.error(() -> "Too many ingredients for shapeless recipe. The maximum is " + MAX_INGREDIENTS);
                }
                return DataResult.success(DefaultedList.copyOf(Ingredient.EMPTY, ingredientsArray));
            }, DataResult::success).forGetter(ShapelessRecipe::getIngredients)
    ).apply(instance, ConsumeBucketShapelessRecipe::new));

    private static final PacketCodec<RegistryByteBuf, ConsumeBucketShapelessRecipe> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ShapelessRecipe::getGroup,
            CraftingRecipeCategory.PACKET_CODEC, ShapelessRecipe::getCategory,
            ItemStack.PACKET_CODEC, recipe -> recipe.getResult(null),
            Ingredient.PACKET_CODEC.collect(PacketCodecs.toList()).xmap(
                ingredients -> {
                    Ingredient[] ingredientsArray = ingredients.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
                    return DefaultedList.copyOf(Ingredient.EMPTY, ingredientsArray);
                },
                defaultedList -> defaultedList.stream().toList()
            ), ShapelessRecipe::getIngredients,
            ConsumeBucketShapelessRecipe::new
    );

    @Override
    public MapCodec<ConsumeBucketShapelessRecipe> codec() {
        return CODEC;
    }

    @Override
    public PacketCodec<RegistryByteBuf, ConsumeBucketShapelessRecipe> packetCodec() {
        return PACKET_CODEC;
    }
} 
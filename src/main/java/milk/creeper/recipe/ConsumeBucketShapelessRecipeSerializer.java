package milk.creeper.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;

public class ConsumeBucketShapelessRecipeSerializer implements RecipeSerializer<ConsumeBucketShapelessRecipe> {

    private static final int MAX_INGREDIENTS = 9;

    private static final MapCodec<ConsumeBucketShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
            CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapelessRecipe::getCategory),
            ItemStack.RECIPE_RESULT_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResult(null)),
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

    @Override
    public Codec<ConsumeBucketShapelessRecipe> codec() {
        return CODEC.codec();
    }

    @Override
    public ConsumeBucketShapelessRecipe read(PacketByteBuf buf) {
        String group = buf.readString();
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        int i = buf.readVarInt();
        DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(i, Ingredient.EMPTY);
        for (int j = 0; j < ingredients.size(); ++j) {
            ingredients.set(j, Ingredient.fromPacket(buf));
        }
        ItemStack itemStack = buf.readItemStack();
        return new ConsumeBucketShapelessRecipe(group, category, itemStack, ingredients);
    }

    @Override
    public void write(PacketByteBuf buf, ConsumeBucketShapelessRecipe recipe) {
        buf.writeString(recipe.getGroup());
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.write(buf);
        }
        buf.writeItemStack(recipe.getResult(null));
    }
} 
package milk.creeper.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.collection.DefaultedList;

public class ConsumeBucketShapelessRecipeSerializer implements RecipeSerializer<ConsumeBucketShapelessRecipe> {

    private static final int MAX_INGREDIENTS = 9;

    // Create a custom codec for recipe results that handles "count" and "item" format
    private static final Codec<ItemStack> RECIPE_RESULT_CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(ItemStack::getItem),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemStack::getCount)
        ).apply(instance, ItemStack::new)
    );

    public static final Codec<ConsumeBucketShapelessRecipe> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.getGroup()),
            CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(recipe -> recipe.getCategory()),
            RECIPE_RESULT_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResult(null)),
            Ingredient.ALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients").flatXmap(
                ingredients -> {
                    if (ingredients.isEmpty()) {
                        return DataResult.error(() -> "No ingredients for shapeless recipe");
                    } else if (ingredients.size() > MAX_INGREDIENTS) {
                        return DataResult.error(() -> "Too many ingredients for shapeless recipe");
                    } else {
                        return DataResult.success(DefaultedList.copyOf(Ingredient.EMPTY, ingredients.toArray(new Ingredient[0])));
                    }
                }, DataResult::success
            ).forGetter(recipe -> recipe.getIngredients())
        ).apply(instance, ConsumeBucketShapelessRecipe::new)
    );

    @Override
    public Codec<ConsumeBucketShapelessRecipe> codec() {
        return CODEC;
    }

    @Override
    public ConsumeBucketShapelessRecipe read(PacketByteBuf buf) {
        String group = buf.readString();
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        ItemStack result = buf.readItemStack();
        int ingredientCount = buf.readVarInt();
        DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(ingredientCount, Ingredient.EMPTY);

        for (int i = 0; i < ingredients.size(); ++i) {
            ingredients.set(i, Ingredient.fromPacket(buf));
        }

        return new ConsumeBucketShapelessRecipe(group, category, result, ingredients);
    }

    @Override
    public void write(PacketByteBuf buf, ConsumeBucketShapelessRecipe recipe) {
        buf.writeString(recipe.getGroup());
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.getResult(null));
        buf.writeVarInt(recipe.getIngredients().size());

        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.write(buf);
        }
    }
} 
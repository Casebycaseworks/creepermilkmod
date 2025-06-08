package milk.creeper.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.collection.DefaultedList;

public class ConsumeBucketShapelessRecipeSerializer implements RecipeSerializer<ShapelessRecipe> {

    private static final MapCodec<ShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
            CraftingRecipeCategory.CODEC.fieldOf("category").forGetter(ShapelessRecipe::getCategory),
            ItemStack.CODEC.fieldOf("result").forGetter(recipe -> {
                // Create a dummy result for codec purposes
                return new ItemStack(Items.DIRT);
            }),
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> {
                // Create a basic ingredient list for codec purposes
                DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(1, Ingredient.ofItems(Items.DIRT));
                return ingredients;
            })
        ).apply(instance, (group, category, result, ingredients) -> {
            // Fixed: Create the list directly without problematic AIR ingredients
            DefaultedList<Ingredient> defaultedIngredients = DefaultedList.ofSize(ingredients.size());
            for (int i = 0; i < ingredients.size(); i++) {
                defaultedIngredients.set(i, ingredients.get(i));
            }
            return new ConsumeBucketShapelessRecipe(group, category, result, defaultedIngredients);
        })
    );

    private static final PacketCodec<RegistryByteBuf, ShapelessRecipe> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, ShapelessRecipe::getGroup,
        CraftingRecipeCategory.PACKET_CODEC, ShapelessRecipe::getCategory,
        ItemStack.PACKET_CODEC, recipe -> new ItemStack(Items.DIRT),
        Ingredient.PACKET_CODEC.collect(PacketCodecs.toList()), recipe -> {
            DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(1, Ingredient.ofItems(Items.DIRT));
            return ingredients;
        },
        (group, category, result, ingredients) -> {
            // Fixed: Create the list directly without problematic AIR ingredients
            DefaultedList<Ingredient> defaultedIngredients = DefaultedList.ofSize(ingredients.size());
            for (int i = 0; i < ingredients.size(); i++) {
                defaultedIngredients.set(i, ingredients.get(i));
            }
            return new ConsumeBucketShapelessRecipe(group, category, result, defaultedIngredients);
        }
    );

    @Override
    public MapCodec<ShapelessRecipe> codec() {
        return CODEC;
    }

    @Override
    public PacketCodec<RegistryByteBuf, ShapelessRecipe> packetCodec() {
        return PACKET_CODEC;
    }
} 
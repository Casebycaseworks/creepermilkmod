package milk.creeper;

import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

/** Exactly four server-side contract tests; this jar is never part of the release. */
public final class CreepermilkGameTests {
    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    public void survivalMilkingInventory(TestContext c) {
        PlayerEntity p = c.createMockSurvivalPlayer();
        p.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.BUCKET));
        CreeperEntity e = c.spawnMob(EntityType.CREEPER, new BlockPos(1, 1, 1));
        c.assertTrue(p.interact(e, Hand.MAIN_HAND).isAccepted(), "milking accepted");
        c.assertTrue(hasItem(p, ModRegistry.CREEPER_MILK_BUCKET), "milk received");
        c.assertFalse(hasItem(p, Items.BUCKET), "bucket consumed");
        c.complete();
    }

    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    public void milkClearsMultipleEffectsReturnsBucket(TestContext c) {
        PlayerEntity p = c.createMockSurvivalPlayer();
        ItemStack milk = new ItemStack(ModRegistry.CREEPER_MILK_BUCKET);
        p.setStackInHand(Hand.MAIN_HAND, milk);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200));
        c.assertTrue(milk.use(c.getWorld(), p, Hand.MAIN_HAND).getResult().isAccepted(), "drinkable");
        ItemStack remainder = milk.finishUsing(c.getWorld(), p);
        c.assertTrue(p.getStatusEffects().isEmpty(), "effects cleared");
        c.assertTrue(remainder.isOf(Items.BUCKET), "bucket returned");
        c.complete();
    }

    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    public void creeperMilkCraftsCake(TestContext c) {
        CraftingInventory in = grid(3, 3, List.of(milk(), milk(), milk(), new ItemStack(Items.SUGAR), new ItemStack(Items.EGG), new ItemStack(Items.SUGAR), new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT)));
        RecipeEntry<CraftingRecipe> r = c.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, in, c.getWorld()).orElseThrow();
        c.assertTrue(r.id().equals(Identifier.of(ModRegistry.MOD_ID, "cake_with_creeper_milk")), "recipe selected");
        c.assertTrue(r.value().craft(in, c.getWorld().getRegistryManager()).isOf(Items.CAKE), "cake crafted");
        c.complete();
    }

    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    public void conversionConsumesInputNoRemainder(TestContext c) {
        CraftingInventory in = grid(1, 1, List.of(milk()));
        RecipeEntry<CraftingRecipe> r = c.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, in, c.getWorld()).orElseThrow();
        c.assertTrue(r.id().equals(Identifier.of(ModRegistry.MOD_ID, "creeper_milk_to_milk")), "recipe selected");
        c.assertTrue(r.value().craft(in, c.getWorld().getRegistryManager()).isOf(Items.MILK_BUCKET), "milk crafted");
        c.assertTrue(r.value().getRemainder(in).stream().allMatch(ItemStack::isEmpty), "no remainder");
        c.complete();
    }

    private static ItemStack milk() { return new ItemStack(ModRegistry.CREEPER_MILK_BUCKET); }
    private static boolean hasItem(PlayerEntity p, net.minecraft.item.Item item) {
        for (int i = 0; i < p.getInventory().size(); i++) if (p.getInventory().getStack(i).isOf(item)) return true;
        return false;
    }
    private static CraftingInventory grid(int w, int h, List<ItemStack> stacks) {
        DefaultedList<ItemStack> slots = DefaultedList.ofSize(w * h, ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) slots.set(i, stacks.get(i));
        return new CraftingInventory(null, w, h, slots);
    }
}

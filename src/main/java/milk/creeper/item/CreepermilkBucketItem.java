package milk.creeper.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class CreepermilkBucketItem extends Item {
    public CreepermilkBucketItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger(serverPlayerEntity, stack);
            serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        if (!world.isClient) {
            user.clearStatusEffects();
        }

        // Store the original stack for creative mode check
        ItemStack originalStack = stack.copy(); 

        // Only decrement if not in creative mode
        PlayerEntity playerEntity = user instanceof PlayerEntity ? (PlayerEntity) user : null;
        if (playerEntity == null || !playerEntity.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        // If the original stack was creeper milk and is now empty (meaning it was consumed by a non-creative player)
        // return a bucket. Otherwise (creative mode), return the original stack.
        if (stack.isEmpty() && (playerEntity == null || !playerEntity.getAbilities().creativeMode)) {
             return new ItemStack(Items.BUCKET);
        }
        
        return stack; // Return the stack (which might be unchanged for creative or empty if something else modified it)
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
} 
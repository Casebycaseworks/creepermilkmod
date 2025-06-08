package milk.creeper.mixin;

import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemUsage;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import milk.creeper.CreepermilkItems;
import milk.creeper.ModRegistry;
import milk.creeper.criterion.ModCriteria;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void creepermilk$onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // System.out.println("[Creepermilk] Mixin loaded and active"); // Optional: Keep if needed for very basic load check
        ItemStack stack = player.getStackInHand(hand);
        // System.out.println("[Creepermilk] Player is holding: " + stack.getItem());
        
        if (stack.getItem() == Items.BUCKET) {
            // System.out.println("[Creepermilk] Player is holding a bucket!");
            if (!player.getWorld().isClient) {
                // System.out.println("[Creepermilk] Server-side interaction detected");
                ItemStack milk = new ItemStack(ModRegistry.CREEPER_MILK_BUCKET);
                // System.out.println("[Creepermilk] Created milk item stack: " + milk);
                
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                    if (!player.getInventory().insertStack(milk)) {
                        // System.out.println("[Creepermilk] Inventory full, dropping item");
                        player.dropItem(milk, false);
                    } else {
                        // System.out.println("[Creepermilk] Added milk to inventory");
                    }
                }
                player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
                // System.out.println("[Creepermilk] Played milk sound");

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ModCriteria.MILK_CREEPER.trigger(serverPlayer);
                    // System.out.println("[Creepermilk] Triggered milk_creeper criterion for " + serverPlayer.getName().getString());
                }
            }
            player.swingHand(hand);
            cir.setReturnValue(ActionResult.SUCCESS);
            // System.out.println("[Creepermilk] Interaction successful!");
        }
    }
} 
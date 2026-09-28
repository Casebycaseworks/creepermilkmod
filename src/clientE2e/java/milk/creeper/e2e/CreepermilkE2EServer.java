package milk.creeper.e2e;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import milk.creeper.ModRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates the deterministic dedicated-server scene used by the real client. */
public final class CreepermilkE2EServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("creepermilk-client-e2e-server");
    private static final Map<UUID, Integer> SECOND_EFFECT_DELAY = new HashMap<>();
    private static final Set<UUID> WAITING_FOR_ADVANCEMENT = new HashSet<>();
    static final BlockPos TABLE_POS = new BlockPos(2, 80, 0);

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            server.execute(() -> prepare(server, handler.player)));
        ServerTickEvents.END_SERVER_TICK.register(CreepermilkE2EServer::verifyAdvancement);
        ServerTickEvents.END_SERVER_TICK.register(CreepermilkE2EServer::reapplyEffects);
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_SERVER_HARNESS_LOADED minecraft=1.20.4");
    }

    private static void prepare(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = server.getOverworld();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 5; z++) {
                world.setBlockState(new BlockPos(x, 79, z), Blocks.STONE.getDefaultState());
                world.setBlockState(new BlockPos(x, 80, z), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(x, 81, z), Blocks.AIR.getDefaultState());
            }
        }
        world.setBlockState(TABLE_POS, Blocks.CRAFTING_TABLE.getDefaultState());

        player.changeGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.clearStatusEffects();
        player.teleport(world, 0.5, 80.0, 0.5, Set.<PositionFlag>of(), 0.0F, 0.0F);
        player.getInventory().setStack(0, new ItemStack(Items.BUCKET));
        player.getInventory().setStack(1, new ItemStack(Items.MILK_BUCKET));
        for (int slot = 2; slot <= 14; slot++) {
            player.getInventory().setStack(slot, new ItemStack(ModRegistry.CREEPER_MILK_BUCKET));
        }
        for (int slot = 15; slot <= 26; slot++) {
            player.getInventory().setStack(slot, new ItemStack(Items.MILK_BUCKET));
        }
        player.getInventory().setStack(27, new ItemStack(Items.SUGAR, 16));
        player.getInventory().setStack(28, new ItemStack(Items.EGG, 8));
        player.getInventory().setStack(29, new ItemStack(Items.WHEAT, 24));
        player.getInventory().selectedSlot = 0;
        applyEffects(player);
        SECOND_EFFECT_DELAY.put(player.getUuid(), -1);
        WAITING_FOR_ADVANCEMENT.add(player.getUuid());

        CreeperEntity creeper = EntityType.CREEPER.create(world);
        if (creeper == null) {
            throw new IllegalStateException("Could not create the E2E creeper");
        }
        creeper.refreshPositionAndAngles(0.5, 80.0, 3.5, 180.0F, 0.0F);
        creeper.setAiDisabled(true);
        creeper.setSilent(true);
        creeper.setInvulnerable(true);
        creeper.setPersistent();
        if (!world.spawnEntity(creeper)) {
            throw new IllegalStateException("Could not spawn the E2E creeper");
        }

        player.sendMessage(Text.literal("Creeper Milk 1.20.4 visible E2E ready."), false);
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_SERVER_READY player={} creeper_ai_disabled=true",
            player.getName().getString());
    }

    private static void verifyAdvancement(MinecraftServer server) {
        for (UUID playerId : Set.copyOf(WAITING_FOR_ADVANCEMENT)) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                WAITING_FOR_ADVANCEMENT.remove(playerId);
                continue;
            }
            if (count(player, ModRegistry.CREEPER_MILK_BUCKET) != 14
                    || count(player, Items.BUCKET) != 0) {
                continue;
            }
            AdvancementEntry advancement = server.getAdvancementLoader().get(
                Identifier.of(ModRegistry.MOD_ID, "milk_creeper"));
            if (advancement != null
                    && player.getAdvancementTracker().getProgress(advancement).isDone()) {
                WAITING_FOR_ADVANCEMENT.remove(playerId);
                LOGGER.info("CREEPER_MILK_CLIENT_E2E_SERVER_ADVANCEMENT_PASS advancement={}",
                    advancement.id());
            }
        }
    }

    private static void reapplyEffects(MinecraftServer server) {
        for (Map.Entry<UUID, Integer> entry : Map.copyOf(SECOND_EFFECT_DELAY).entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                SECOND_EFFECT_DELAY.remove(entry.getKey());
                continue;
            }
            int delay = entry.getValue();
            if (delay < 0 && player.getStatusEffects().isEmpty()
                    && count(player, ModRegistry.CREEPER_MILK_BUCKET) == 13
                    && count(player, Items.BUCKET) == 1) {
                SECOND_EFFECT_DELAY.put(entry.getKey(), 40);
                LOGGER.info("CREEPER_MILK_CLIENT_E2E_SERVER_CREEPER_EFFECTS_CLEARED");
            } else if (delay > 0) {
                SECOND_EFFECT_DELAY.put(entry.getKey(), delay - 1);
            } else if (delay == 0) {
                applyEffects(player);
                SECOND_EFFECT_DELAY.remove(entry.getKey());
                LOGGER.info("CREEPER_MILK_CLIENT_E2E_SERVER_VANILLA_EFFECTS_READY");
            }
        }
    }

    private static void applyEffects(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 20 * 300));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 300));
    }

    private static int count(ServerPlayerEntity player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}

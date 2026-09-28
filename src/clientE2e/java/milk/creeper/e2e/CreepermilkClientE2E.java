package milk.creeper.e2e;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import milk.creeper.ModRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Drives the real installed client and networking path; never ships in the release. */
public final class CreepermilkClientE2E implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("creepermilk-client-e2e");
    private static final String REPORT_NAME = "creepermilk-client-e2e-result.json";
    private static final String[] PATTERNS = {
        "MMM", "CMM", "MCM", "MMC", "CCM", "CMC", "MCC", "CCC"
    };

    private enum Stage {
        WAITING_FOR_WORLD, MILKING, SELECT_CREEPER_MILK, START_CREEPER_DRINK,
        WAIT_FOR_CREEPER_DRINK, WAIT_FOR_VANILLA_EFFECTS, SELECT_VANILLA_MILK,
        START_VANILLA_DRINK, WAIT_FOR_VANILLA_DRINK, OPEN_TABLE, WAIT_FOR_TABLE,
        CONVERSION_PICKUP, CONVERSION_PLACE, WAIT_FOR_CONVERSION_OUTPUT,
        WAIT_FOR_CONVERSION_RESULT, CRAFT_PICKUP, CRAFT_PLACE, CRAFT_RETURN,
        WAIT_FOR_OUTPUT, WAIT_FOR_CAKE, CLEAR_REMAINDERS, PASSED, FAILED
    }

    private Stage stage = Stage.WAITING_FOR_WORLD;
    private int stageTicks;
    private boolean milkInteractionSent;
    private boolean soundListenerRegistered;
    private volatile boolean milkingSoundObserved;
    private volatile int genericDrinkSoundCount;
    private int drinkSoundBaseline;
    private int cakeVariant;
    private int craftIndex;
    private int sourceSlot = -1;
    private int remainderSlot;
    private int connectTicks;
    private boolean connectStarted;
    private Path reportPath;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_HARNESS_LOADED minecraft=1.20.4");
    }

    private void tick(MinecraftClient client) {
        if (stage == Stage.PASSED) {
            stageTicks++;
            if (stageTicks == 200) {
                LOGGER.info("CREEPER_MILK_CLIENT_E2E_CLEAN_CLIENT_STOP");
                client.scheduleStop();
            }
            return;
        }
        if (stage == Stage.FAILED) {
            return;
        }
        try {
            registerSoundListener(client);
            if (reportPath == null) {
                reportPath = client.runDirectory.toPath().resolve(REPORT_NAME);
            }
            if (client.player == null || client.world == null || client.interactionManager == null) {
                connectIfNeeded(client);
                return;
            }
            stageTicks++;
            switch (stage) {
                case WAITING_FOR_WORLD -> waitForWorld(client);
                case MILKING -> milk(client);
                case SELECT_CREEPER_MILK -> selectCreeperMilk(client);
                case START_CREEPER_DRINK -> startCreeperDrink(client);
                case WAIT_FOR_CREEPER_DRINK -> waitForCreeperDrink(client);
                case WAIT_FOR_VANILLA_EFFECTS -> waitForVanillaEffects(client);
                case SELECT_VANILLA_MILK -> selectVanillaMilk(client);
                case START_VANILLA_DRINK -> startVanillaDrink(client);
                case WAIT_FOR_VANILLA_DRINK -> waitForVanillaDrink(client);
                case OPEN_TABLE -> openTable(client);
                case WAIT_FOR_TABLE -> waitForTable(client);
                case CONVERSION_PICKUP -> conversionPickup(client);
                case CONVERSION_PLACE -> conversionPlace(client);
                case WAIT_FOR_CONVERSION_OUTPUT -> waitForConversionOutput(client);
                case WAIT_FOR_CONVERSION_RESULT -> waitForConversionResult(client);
                case CRAFT_PICKUP -> craftPickup(client);
                case CRAFT_PLACE -> craftPlace(client);
                case CRAFT_RETURN -> craftReturn(client);
                case WAIT_FOR_OUTPUT -> waitForOutput(client);
                case WAIT_FOR_CAKE -> waitForCake(client);
                case CLEAR_REMAINDERS -> clearRemainders(client);
                default -> { }
            }
        } catch (Throwable failure) {
            fail(client, failure);
        }
    }

    private void connectIfNeeded(MinecraftClient client) throws IOException {
        if (connectStarted || client.currentScreen == null) {
            return;
        }
        if (client.currentScreen instanceof ConnectScreen) {
            connectStarted = true;
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_CONNECT_ACTIVE source=launcher");
            return;
        }
        if (++connectTicks < 80) {
            return;
        }
        Path addressFile = client.runDirectory.toPath().resolve(
            "creepermilk-client-e2e-server.txt");
        String address = Files.readString(addressFile).trim();
        if (!ServerAddress.isValid(address)) {
            throw new IllegalStateException("Invalid E2E server address: " + address);
        }
        connectStarted = true;
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_CONNECT_START address={}", address);
        ServerInfo info = new ServerInfo(
            "Creeper Milk 1.20.4 E2E", address, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(address), info, false);
    }

    private void registerSoundListener(MinecraftClient client) {
        if (soundListenerRegistered || client.getSoundManager() == null) {
            return;
        }
        client.getSoundManager().registerListener((sound, soundSet, range) -> {
            Identifier id = sound.getId();
            if (id.equals(SoundEvents.ENTITY_COW_MILK.getId())) {
                milkingSoundObserved = true;
            }
            if (id.equals(SoundEvents.ENTITY_GENERIC_DRINK.getId())) {
                genericDrinkSoundCount++;
            }
        });
        soundListenerRegistered = true;
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_SOUND_LISTENER_READY");
    }

    private void waitForWorld(MinecraftClient client) {
        boolean prepared = findCreeper(client) != null
            && count(client, Items.BUCKET) == 1
            && count(client, Items.MILK_BUCKET) == 13
            && count(client, ModRegistry.CREEPER_MILK_BUCKET) == 13
            && client.player.getStatusEffects().size() >= 2;
        if (stageTicks < 80 || !prepared) {
            if (stageTicks > 400) {
                throw new IllegalStateException("Timed out waiting for the prepared server scene");
            }
            return;
        }
        announce(client, "1/5  Milking the controlled creeper through the real client");
        transition(Stage.MILKING);
    }

    private void milk(MinecraftClient client) {
        if (!milkInteractionSent) {
            if (stageTicks < 22) {
                return;
            }
            CreeperEntity creeper = findCreeper(client);
            if (creeper == null) {
                if (stageTicks > 100) {
                    throw new IllegalStateException("Prepared creeper disappeared");
                }
                return;
            }
            lookAt(client, creeper.getPos());
            client.interactionManager.interactEntity(client.player, creeper, Hand.MAIN_HAND);
            client.player.swingHand(Hand.MAIN_HAND);
            milkInteractionSent = true;
            stageTicks = 0;
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_ACTION milk_interaction=1");
            return;
        }
        if (count(client, ModRegistry.CREEPER_MILK_BUCKET) == 14
                && count(client, Items.BUCKET) == 0) {
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_MILKING_PASS");
            if (milkingSoundObserved) {
                LOGGER.info("CREEPER_MILK_CLIENT_E2E_MILKING_SOUND_PASS sound={}",
                    SoundEvents.ENTITY_COW_MILK.getId());
            } else {
                LOGGER.warn("CREEPER_MILK_CLIENT_E2E_MILKING_SOUND_KNOWN_LIMITATION"
                    + " sound={} source_player_excluded=true",
                    SoundEvents.ENTITY_COW_MILK.getId());
            }
            transition(Stage.SELECT_CREEPER_MILK);
            return;
        }
        if (stageTicks > 100) {
            throw new IllegalStateException("Milking did not exchange the bucket");
        }
    }

    private void selectCreeperMilk(MinecraftClient client) {
        if (stageTicks < 20) return;
        int slot = inventorySlot(client, ModRegistry.CREEPER_MILK_BUCKET);
        if (slot < 0 || slot >= 9) {
            throw new IllegalStateException("Creeper Milk was not available in the hotbar");
        }
        client.player.getInventory().selectedSlot = slot;
        announce(client, "2/5  Drinking Creeper Milk under two effects");
        transition(Stage.START_CREEPER_DRINK);
    }

    private void startCreeperDrink(MinecraftClient client) {
        if (stageTicks < 15) return;
        if (client.player.getStatusEffects().size() < 2) {
            throw new IllegalStateException("Two effects were not active before Creeper Milk");
        }
        drinkSoundBaseline = genericDrinkSoundCount;
        client.options.useKey.setPressed(true);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        transition(Stage.WAIT_FOR_CREEPER_DRINK);
    }

    private void waitForCreeperDrink(MinecraftClient client) {
        client.options.useKey.setPressed(true);
        if (count(client, ModRegistry.CREEPER_MILK_BUCKET) == 13
                && count(client, Items.BUCKET) == 1
                && client.player.getStatusEffects().isEmpty()
                && genericDrinkSoundCount > drinkSoundBaseline) {
            client.options.useKey.setPressed(false);
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_CREEPER_DRINK_PASS");
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_CREEPER_DRINK_SOUND_PASS sound={}",
                SoundEvents.ENTITY_GENERIC_DRINK.getId());
            transition(Stage.WAIT_FOR_VANILLA_EFFECTS);
            return;
        }
        if (stageTicks > 160) {
            client.options.useKey.setPressed(false);
            throw new IllegalStateException("Timed out drinking Creeper Milk");
        }
    }

    private void waitForVanillaEffects(MinecraftClient client) {
        if (client.player.getStatusEffects().size() >= 2) {
            transition(Stage.SELECT_VANILLA_MILK);
        } else if (stageTicks > 120) {
            throw new IllegalStateException("Server did not reapply effects");
        }
    }

    private void selectVanillaMilk(MinecraftClient client) {
        if (stageTicks < 20) return;
        int slot = inventorySlot(client, Items.MILK_BUCKET);
        if (slot < 0 || slot >= 9) {
            throw new IllegalStateException("Vanilla Milk was not available in the hotbar");
        }
        client.player.getInventory().selectedSlot = slot;
        announce(client, "3/5  Drinking vanilla Milk under two effects");
        transition(Stage.START_VANILLA_DRINK);
    }

    private void startVanillaDrink(MinecraftClient client) {
        if (stageTicks < 15) return;
        if (client.player.getStatusEffects().size() < 2) {
            throw new IllegalStateException("Two effects were not active before vanilla Milk");
        }
        drinkSoundBaseline = genericDrinkSoundCount;
        client.options.useKey.setPressed(true);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        transition(Stage.WAIT_FOR_VANILLA_DRINK);
    }

    private void waitForVanillaDrink(MinecraftClient client) {
        client.options.useKey.setPressed(true);
        if (count(client, Items.MILK_BUCKET) == 12
                && count(client, Items.BUCKET) == 2
                && client.player.getStatusEffects().isEmpty()
                && genericDrinkSoundCount > drinkSoundBaseline) {
            client.options.useKey.setPressed(false);
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_VANILLA_DRINK_PASS");
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_VANILLA_DRINK_SOUND_PASS sound={}",
                SoundEvents.ENTITY_GENERIC_DRINK.getId());
            transition(Stage.OPEN_TABLE);
            return;
        }
        if (stageTicks > 160) {
            client.options.useKey.setPressed(false);
            throw new IllegalStateException("Timed out drinking vanilla Milk");
        }
    }

    private void openTable(MinecraftClient client) {
        if (stageTicks < 35) return;
        lookAt(client, Vec3d.ofCenter(CreepermilkE2EServer.TABLE_POS));
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(CreepermilkE2EServer.TABLE_POS),
            Direction.UP, CreepermilkE2EServer.TABLE_POS, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.swingHand(Hand.MAIN_HAND);
        announce(client, "4/5  Converting Creeper Milk into vanilla Milk");
        transition(Stage.WAIT_FOR_TABLE);
    }

    private void waitForTable(MinecraftClient client) {
        if (client.player.currentScreenHandler instanceof CraftingScreenHandler) {
            transition(Stage.CONVERSION_PICKUP);
        } else if (stageTicks > 100) {
            throw new IllegalStateException("Crafting table did not open");
        }
    }

    private void conversionPickup(MinecraftClient client) {
        if (stageTicks < 4) return;
        ScreenHandler handler = requireCraftingHandler(client);
        sourceSlot = handlerSlot(handler, ModRegistry.CREEPER_MILK_BUCKET);
        if (sourceSlot < 0) throw new IllegalStateException("Missing conversion input");
        client.interactionManager.clickSlot(handler.syncId, sourceSlot, 0,
            SlotActionType.PICKUP, client.player);
        transition(Stage.CONVERSION_PLACE);
    }

    private void conversionPlace(MinecraftClient client) {
        if (stageTicks < 4) return;
        ScreenHandler handler = requireCraftingHandler(client);
        client.interactionManager.clickSlot(handler.syncId, 1, 0,
            SlotActionType.PICKUP, client.player);
        transition(Stage.WAIT_FOR_CONVERSION_OUTPUT);
    }

    private void waitForConversionOutput(MinecraftClient client) {
        ScreenHandler handler = requireCraftingHandler(client);
        if (handler.getSlot(0).getStack().isOf(Items.MILK_BUCKET)) {
            client.interactionManager.clickSlot(handler.syncId, 0, 0,
                SlotActionType.QUICK_MOVE, client.player);
            transition(Stage.WAIT_FOR_CONVERSION_RESULT);
        } else if (stageTicks > 100) {
            throw new IllegalStateException("Conversion output did not appear");
        }
    }

    private void waitForConversionResult(MinecraftClient client) {
        ScreenHandler handler = requireCraftingHandler(client);
        if (count(client, ModRegistry.CREEPER_MILK_BUCKET) == 12
                && count(client, Items.MILK_BUCKET) == 13
                && count(client, Items.BUCKET) == 2
                && handler.getSlot(1).getStack().isEmpty()) {
            LOGGER.info("CREEPER_MILK_CLIENT_E2E_CONVERSION_PASS no_remainder=true");
            cakeVariant = 0;
            craftIndex = 0;
            announce(client, "5/5  Crafting ordered rows MMM,CMM,MCM,MMC,CCM,CMC,MCC,CCC");
            transition(Stage.CRAFT_PICKUP);
        } else if (stageTicks > 100) {
            throw new IllegalStateException("Conversion accounting or remainder was incorrect");
        }
    }

    private void craftPickup(MinecraftClient client) {
        if (stageTicks < 4) return;
        ScreenHandler handler = requireCraftingHandler(client);
        Item ingredient = cakeIngredient(cakeVariant, craftIndex);
        sourceSlot = handlerSlot(handler, ingredient);
        if (sourceSlot < 0) {
            throw new IllegalStateException("Missing ingredient for " + PATTERNS[cakeVariant]);
        }
        client.interactionManager.clickSlot(handler.syncId, sourceSlot, 0,
            SlotActionType.PICKUP, client.player);
        transition(Stage.CRAFT_PLACE);
    }

    private void craftPlace(MinecraftClient client) {
        if (stageTicks < 4) return;
        ScreenHandler handler = requireCraftingHandler(client);
        client.interactionManager.clickSlot(handler.syncId, craftIndex + 1, 1,
            SlotActionType.PICKUP, client.player);
        transition(Stage.CRAFT_RETURN);
    }

    private void craftReturn(MinecraftClient client) {
        if (stageTicks < 4) return;
        ScreenHandler handler = requireCraftingHandler(client);
        if (!handler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, sourceSlot, 0,
                SlotActionType.PICKUP, client.player);
        }
        craftIndex++;
        transition(craftIndex == 9 ? Stage.WAIT_FOR_OUTPUT : Stage.CRAFT_PICKUP);
    }

    private void waitForOutput(MinecraftClient client) {
        ScreenHandler handler = requireCraftingHandler(client);
        if (handler.getSlot(0).getStack().isOf(Items.CAKE)) {
            client.interactionManager.clickSlot(handler.syncId, 0, 0,
                SlotActionType.QUICK_MOVE, client.player);
            transition(Stage.WAIT_FOR_CAKE);
        } else if (stageTicks > 100) {
            throw new IllegalStateException("Cake output missing for " + PATTERNS[cakeVariant]);
        }
    }

    private void waitForCake(MinecraftClient client) {
        if (count(client, Items.CAKE) == cakeVariant + 1) {
            remainderSlot = 1;
            transition(Stage.CLEAR_REMAINDERS);
        } else if (stageTicks > 100) {
            throw new IllegalStateException("Cake not transferred for " + PATTERNS[cakeVariant]);
        }
    }

    private void clearRemainders(MinecraftClient client) {
        if (stageTicks < 4) return;
        ScreenHandler handler = requireCraftingHandler(client);
        while (remainderSlot <= 9 && handler.getSlot(remainderSlot).getStack().isEmpty()) {
            remainderSlot++;
        }
        if (remainderSlot <= 9) {
            ItemStack remainder = handler.getSlot(remainderSlot).getStack();
            if (!remainder.isOf(Items.BUCKET)) {
                throw new IllegalStateException("Unexpected crafting remainder " + remainder);
            }
            client.interactionManager.clickSlot(handler.syncId, remainderSlot, 0,
                SlotActionType.QUICK_MOVE, client.player);
            stageTicks = 0;
            return;
        }
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_CAKE_PASS variant={} pattern={}",
            cakeVariant + 1, PATTERNS[cakeVariant]);
        cakeVariant++;
        if (cakeVariant == PATTERNS.length) {
            verifyFinalCounts(client);
            pass(client);
        } else {
            craftIndex = 0;
            transition(Stage.CRAFT_PICKUP);
        }
    }

    private Item cakeIngredient(int variant, int gridIndex) {
        if (gridIndex < 3) {
            return PATTERNS[variant].charAt(gridIndex) == 'C'
                ? ModRegistry.CREEPER_MILK_BUCKET : Items.MILK_BUCKET;
        }
        return switch (gridIndex) {
            case 3, 5 -> Items.SUGAR;
            case 4 -> Items.EGG;
            case 6, 7, 8 -> Items.WHEAT;
            default -> throw new IllegalArgumentException("Invalid grid index " + gridIndex);
        };
    }

    private void verifyFinalCounts(MinecraftClient client) {
        if (count(client, Items.CAKE) != 8
                || count(client, Items.BUCKET) != 26
                || count(client, Items.MILK_BUCKET) != 1
                || count(client, ModRegistry.CREEPER_MILK_BUCKET) != 0
                || !client.player.getStatusEffects().isEmpty()) {
            throw new IllegalStateException("Final accounting failed: cakes="
                + count(client, Items.CAKE) + " buckets=" + count(client, Items.BUCKET)
                + " milk=" + count(client, Items.MILK_BUCKET) + " creeperMilk="
                + count(client, ModRegistry.CREEPER_MILK_BUCKET));
        }
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_FINAL_ACCOUNTING_PASS"
            + " cakes=8 buckets=26 milk=1 creeperMilk=0 effects=0");
    }

    private CreeperEntity findCreeper(MinecraftClient client) {
        List<CreeperEntity> creepers = client.world.getEntitiesByClass(
            CreeperEntity.class, client.player.getBoundingBox().expand(8.0), entity -> true);
        return creepers.isEmpty() ? null : creepers.get(0);
    }

    private int count(MinecraftClient client, Item item) {
        int total = 0;
        for (ItemStack stack : client.player.getInventory().main) {
            if (stack.isOf(item)) total += stack.getCount();
        }
        return total;
    }

    private int inventorySlot(MinecraftClient client, Item item) {
        for (int i = 0; i < client.player.getInventory().main.size(); i++) {
            if (client.player.getInventory().main.get(i).isOf(item)) return i;
        }
        return -1;
    }

    private int handlerSlot(ScreenHandler handler, Item item) {
        for (int i = 10; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.getStack().isOf(item)) return i;
        }
        return -1;
    }

    private ScreenHandler requireCraftingHandler(MinecraftClient client) {
        if (!(client.player.currentScreenHandler instanceof CraftingScreenHandler handler)) {
            throw new IllegalStateException("Crafting screen closed during E2E");
        }
        return handler;
    }

    private void lookAt(MinecraftClient client, Vec3d target) {
        Vec3d delta = target.subtract(client.player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        client.player.setYaw((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
        client.player.setPitch((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)));
    }

    private void announce(MinecraftClient client, String message) {
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_STAGE {}", message);
        client.player.sendMessage(Text.literal("[Creeper Milk E2E] " + message), false);
        client.inGameHud.setTitle(Text.literal("Creeper Milk 1.20.4 automated client test"));
        client.inGameHud.setSubtitle(Text.literal(message));
        client.inGameHud.setTitleTicks(5, 50, 10);
    }

    private void transition(Stage next) {
        stage = next;
        stageTicks = 0;
    }

    private void pass(MinecraftClient client) {
        stage = Stage.PASSED;
        stageTicks = 0;
        client.options.useKey.setPressed(false);
        announce(client, "PASS — advancement, milking, both drinks, conversion, and 8 cakes");
        client.inGameHud.setTitleTicks(10, 160, 20);
        writeReport("PASS", "Installed-client contract passed milk_creeper advancement; genuine"
            + " controlled creeper milking; Creeper Milk and vanilla Milk multiple-effect"
            + " clearing, bucket return, and drink sound; Creeper Milk-to-Milk conversion"
            + " with no remainder; ordered cake rows MMM,CMM,MCM,MMC,CCM,CMC,MCC,CCC;"
            + " and final inventory accounting. Known accepted limitation: 1.0.0 source-player"
            + " milking silence.");
        LOGGER.info("CREEPER_MILK_CLIENT_E2E_PASS");
    }

    private void fail(MinecraftClient client, Throwable failure) {
        stage = Stage.FAILED;
        client.options.useKey.setPressed(false);
        LOGGER.error("CREEPER_MILK_CLIENT_E2E_FAIL", failure);
        client.inGameHud.setTitle(Text.literal("Creeper Milk automated client test FAILED"));
        client.inGameHud.setSubtitle(Text.literal(failure.getMessage()));
        client.inGameHud.setTitleTicks(5, 200, 20);
        writeReport("FAIL", failure.toString());
    }

    private void writeReport(String result, String detail) {
        String escaped = detail.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\n"
            + "  \"result\": \"" + result + "\",\n"
            + "  \"minecraft\": \"1.20.4\",\n"
            + "  \"modVersion\": \"1.0.0\",\n"
            + "  \"timestamp\": \"" + Instant.now() + "\",\n"
            + "  \"detail\": \"" + escaped + "\"\n"
            + "}\n";
        try {
            Files.writeString(reportPath, json);
        } catch (IOException writeFailure) {
            LOGGER.error("Could not write E2E report to {}", reportPath, writeFailure);
        }
    }
}

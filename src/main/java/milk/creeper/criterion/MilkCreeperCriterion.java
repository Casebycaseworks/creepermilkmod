package milk.creeper.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import milk.creeper.ModRegistry;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.predicate.entity.LootContextPredicate;

import java.util.Optional;

public class MilkCreeperCriterion extends AbstractCriterion<MilkCreeperCriterion.Conditions> {
    public static final Identifier ID = new Identifier(ModRegistry.MOD_ID, "milk_creeper");

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> true); // Always trigger if the player matches
    }

    public record Conditions(Optional<LootContextPredicate> playerPredicate) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                LootContextPredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::playerPredicate)
                        )
                        .apply(instance, Conditions::new)
        );

        @Override
        public Optional<LootContextPredicate> player() {
            return playerPredicate;
        }
    }
} 
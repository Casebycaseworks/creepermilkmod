package milk.creeper.criterion;

import com.google.gson.JsonObject;
import milk.creeper.ModRegistry;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class MilkCreeperCriterion extends AbstractCriterion<MilkCreeperCriterion.Conditions> {
    public static final Identifier ID = new Identifier(ModRegistry.MOD_ID, "milk_creeper");

    @Override
    protected Conditions conditionsFromJson(JsonObject jsonObject, Optional<LootContextPredicate> optional, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        return new Conditions(optional);
    }

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> true);
    }

    public static class Conditions extends AbstractCriterionConditions {
        public Conditions(Optional<LootContextPredicate> player) {
            super(player);
        }
    }
} 
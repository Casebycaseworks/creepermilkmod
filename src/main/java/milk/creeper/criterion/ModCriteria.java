package milk.creeper.criterion;

import milk.creeper.ModRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.util.Identifier;

public class ModCriteria {

    public static final MilkCreeperCriterion MILK_CREEPER = Criteria.register(new Identifier(ModRegistry.MOD_ID, "milk_creeper").toString(), new MilkCreeperCriterion());
    // Older versions used Criteria.register(Identifier, Criterion) or Criteria.register(String, Criterion)
    

    public static void registerCriteria() {
        // This method ensures the static initializer runs and registers the criteria.
        // Call this from your main mod initializer.
    }
} 
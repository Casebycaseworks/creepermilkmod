package milk.creeper.criterion;

import milk.creeper.ModRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.util.Identifier;

public class ModCriteria {

    public static final MilkCreeperCriterion MILK_CREEPER = new MilkCreeperCriterion();

    public static void registerCriteria() {
        Criteria.register(ModRegistry.MOD_ID + ":milk_creeper", MILK_CREEPER);
    }
} 
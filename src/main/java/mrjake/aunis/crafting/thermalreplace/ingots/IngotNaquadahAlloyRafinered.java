package mrjake.aunis.crafting.thermalreplace.ingots;

import mrjake.aunis.crafting.thermalreplace.ThermalAbstractRecipe;
import mrjake.aunis.item.AunisItems;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.HashMap;

public class IngotNaquadahAlloyRafinered extends ThermalAbstractRecipe {

    // CONFIGURATION
    public static final int MINIMAL_SLOTS = 4;
    public static final int MAXIMAL_SLOTS = 9;
    public static final String NAME = "thermal_ingot_naquadah_alloy_rafinered";
    public static final ItemStack OUTPUT_ITEM = new ItemStack(AunisItems.NAQUADAH_ALLOY);

    // items / patterns
    public HashMap<ItemStack, int[]> PATTERN_LIST = new HashMap<ItemStack, int[]>() {
        {
            put(new ItemStack(AunisItems.NAQUADAH_SHARD), new int[]{
                    0, 1, 0,
                    1, 0, 0,
                    0, 0, 0
            });
            put(new ItemStack(AunisItems.NAQUADAH_ALLOY_RAW), new int[]{
                    1, 0, 0,
                    0, 0, 0,
                    0, 0, 0
            });
        }
    };

    // REGISTER / CONSTRUCTOR
    public IngotNaquadahAlloyRafinered() {
        super(NAME, MINIMAL_SLOTS, MAXIMAL_SLOTS, OUTPUT_ITEM);
        setPatterns(PATTERN_LIST);
    }
}

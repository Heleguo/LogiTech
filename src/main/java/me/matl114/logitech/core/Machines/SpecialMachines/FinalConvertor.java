package me.matl114.logitech.core.Machines.SpecialMachines;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.matl114.logitech.core.Blocks.Laser;
import me.matl114.logitech.core.Blocks.MultiBlock.FinalAltarCore;
import me.matl114.logitech.core.Machines.Abstracts.AbstractMachine;
import me.matl114.logitech.core.Registries.FinalFeature;
import me.matl114.logitech.utils.AddUtils;
import me.matl114.logitech.utils.Debug;
import me.matl114.logitech.utils.MachineRecipeUtils;
import me.matl114.logitech.utils.UtilClass.ItemClass.RandOutItem;
import me.matl114.logitech.utils.UtilClass.ItemClass.RandomItemStack;
import me.matl114.logitech.utils.Utils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class FinalConvertor extends AbstractMachine implements FinalAltarCore.FinalAltarChargable, Laser.LaserChargable {
    // 有效的材料缓存
    private static final Material[] VALID_MATERIALS;
    private static final Map<Material, Integer> MATERIAL_INDEX_MAP = new HashMap<>();
    private static final int MATERIAL_COUNT;
    private static final Random rand = new Random();
    private static final int MAX_NEIGHBOR_RANGE = 64; // 最大随机偏移量
    private static final int MAX_ATTEMPTS = 36;       // 最大尝试次数

    static {
        // 收集所有有效的材料（非空气、可堆叠的物品）
        List<Material> validMaterialList = new ArrayList<>();
        for (Material material : Material.values()) {
            // 排除技术性方块、空气和不可堆叠物品
            if (material.isItem() && !material.isAir() && material.getMaxStackSize() > 0) {
                validMaterialList.add(material);
            }
        }
        
        // 转换为数组并创建索引映射
        VALID_MATERIALS = validMaterialList.toArray(new Material[0]);
        MATERIAL_COUNT = VALID_MATERIALS.length;
        
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            MATERIAL_INDEX_MAP.put(VALID_MATERIALS[i], i);
        }
        
        Debug.debug("Loaded " + MATERIAL_COUNT + " valid materials for FinalConvertor");
    }

    public static Material getRandomMaterial(Material baseMaterial) {
        // 获取基础材料的索引
        Integer baseIndex = MATERIAL_INDEX_MAP.get(baseMaterial);
        
        // 如果基础材料无效，随机返回一个有效材料
        if (baseIndex == null) {
            Debug.debug("Base material not in valid set: " + baseMaterial);
            return VALID_MATERIALS[rand.nextInt(MATERIAL_COUNT)];
        }

        // 在基础索引附近随机搜索
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            // 生成 -MAX_NEIGHBOR_RANGE 到 +MAX_NEIGHBOR_RANGE 的随机偏移
            int offset = rand.nextInt(MAX_NEIGHBOR_RANGE * 2 + 1) - MAX_NEIGHBOR_RANGE;
            int targetIndex = baseIndex + offset;
            
            // 检查索引是否在有效范围内
            if (targetIndex >= 0 && targetIndex < MATERIAL_COUNT) {
                Material candidate = VALID_MATERIALS[targetIndex];
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        
        // 多次尝试失败后返回 null
        return null;
    }

    // 界面槽位定义
    protected final int[] BORDER = {13, 22, 31, 40, 49};
    protected final int[] INPUT_BORDER = {0, 1, 2, 3};
    protected final int[] OUTPUT_BORDER = {5, 6, 7, 8};
    protected final int[] INPUT_SLOT = {
        9, 10, 11, 12,
        18, 19, 20, 21,
        27, 28, 29, 30,
        36, 37, 38, 39,
        45, 46, 47, 48
    };
    protected final int[] OUTPUT_SLOT = {
        14, 15, 16, 17,
        23, 24, 25, 26,
        32, 33, 34, 35,
        41, 42, 43, 44,
        50, 51, 52, 53
    };
    
    protected final int STATUS_SLOT = 4;
    protected final ItemStack STATUS_ON = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&6机器信息", "&7状态: &a已激活");
    protected final ItemStack STATUS_OFF = new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&6机器信息", "&7状态: &c未激活");
    
    protected final ItemStack NULL_OUTPUT;
    protected final RandOutItem NULL_OUT;
    protected final MachineRecipe RECIPE_FOR_DISPLAY;

    public FinalConvertor(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                          int energybuffer, int energyConsumption, RandomItemStack nullOutput) {
        super(category, item, recipeType, recipe, energybuffer, energyConsumption);
        this.NULL_OUTPUT = nullOutput;
        this.NULL_OUT = nullOutput;
        
        this.setDisplayRecipes(
            Utils.list(
                AddUtils.getInfoShow("&f机制 - &c充能",
                    "&7当置于贰级终极祭坛上时",
                    "&7且机器被终极祭坛结构中的所有宏激光发射器充能时",
                    "&7即终极祭坛中四个宏激光发射器分别位于四个壹级以上终极祭坛上时",
                    "&7机器激活,进行运转"
                ),
                null,
                AddUtils.getInfoShow("&f机制 - &c随机波动",
                    "&7当机器运转时,",
                    "&7机器会随机波动输入物品的材质",
                    "&7并尝试将其转为其他材质的原版物品",
                    "&7同时赋予其随机的数量",
                    "&7当转换失败时,机器会随机从下方可能的输出中选择一项",
                    "&7进行输出"
                )
            )
        );
        
        this.RECIPE_FOR_DISPLAY = MachineRecipeUtils.stackFrom(-1,
            new ItemStack[]{AddUtils.getInfoShow("&f可能的输出", "&7如下所示")},
            new ItemStack[]{NULL_OUTPUT});
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOT;
    }

    @Override
    public int[] getOutputSlots() {
        return OUTPUT_SLOT;
    }

    @Override
    public void constructMenu(BlockMenuPreset preset) {
        // 设置边界槽位
        for (int slot : BORDER) {
            preset.addItem(slot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        
        // 设置输入区边界
        for (int slot : INPUT_BORDER) {
            preset.addItem(slot, ChestMenuUtils.getInputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
        }
        
        // 设置输出区边界
        for (int slot : OUTPUT_BORDER) {
            preset.addItem(slot, ChestMenuUtils.getOutputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
        }
        
        // 设置状态槽位
        preset.addItem(STATUS_SLOT, STATUS_OFF, ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public List<MachineRecipe> getMachineRecipes() {
        return Collections.singletonList(RECIPE_FOR_DISPLAY);
    }

    @Override
    public void tick(Block b, @Nullable BlockMenu menu, SlimefunBlockData data, int tickCount) {
        if (menu == null) return;
        
        boolean isCharged = FinalFeature.isFinalAltarCharged(this, data);
        boolean conditionsMet = conditionHandle(b, menu) && isCharged;
        
        if (menu.hasViewer()) {
            menu.replaceExistingItem(STATUS_SLOT, conditionsMet ? STATUS_ON : STATUS_OFF);
        }
        
        if (conditionsMet) {
            process(b, menu, data);
        }
    }

    @Override
    public void process(Block b, BlockMenu inv, SlimefunBlockData data) {
        int inputIndex = 0;
        int outputIndex = 0;
        
        // 遍历所有输入输出槽位
        while (inputIndex < INPUT_SLOT.length && outputIndex < OUTPUT_SLOT.length) {
            int inputSlot = INPUT_SLOT[inputIndex];
            int outputSlot = OUTPUT_SLOT[outputIndex];
            
            ItemStack input = inv.getItemInSlot(inputSlot);
            ItemStack output = inv.getItemInSlot(outputSlot);
            
            // 跳过空输入槽
            if (input == null || input.getType() == Material.AIR) {
                inputIndex++;
                continue;
            }
            
            // 跳过非空输出槽
            if (output != null && output.getType() != Material.AIR) {
                outputIndex++;
                continue;
            }
            
            // 处理物品转换
            Material newMaterial = getRandomMaterial(input.getType());
            ItemStack result;
            
            if (newMaterial != null) {
                int maxStack = newMaterial.getMaxStackSize();
                int amount = maxStack > 1 ? rand.nextInt(maxStack) + 1 : 1;
                result = new ItemStack(newMaterial, amount);
            } else {
                result = NULL_OUT.getInstance();
                Debug.debug("Material conversion failed for: " + input.getType());
            }
            
            // 更新物品
            inv.replaceExistingItem(outputSlot, result);
            inv.replaceExistingItem(inputSlot, null);
            
            // 消耗能量
            progressorCost(b, inv);
            
            // 处理完一组后退出（一次只处理一个物品）
            break;
        }
    }
}

package com.github.alexnijjar.the_extractinator.util;

import com.github.alexnijjar.the_extractinator.TheExtractinator;
import com.github.alexnijjar.the_extractinator.config.ExtractinatorConfig;
import com.github.alexnijjar.the_extractinator.data.LootSlot;
import com.github.alexnijjar.the_extractinator.data.LootTable;
import com.github.alexnijjar.the_extractinator.data.SupportedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootUtils {

    // Obtains loot from a loot table for the extractinator to spit out.
    public static List<ItemStack> extractMaterials(BlockState block, Random random) {

        List<ItemStack> output = new ArrayList<>();
        SupportedBlock supportedBlock = null;
        List<LootSlot> lootTableSlots = new ArrayList<>();
        Identifier blockId = Registry.BLOCK.getId(block.getBlock());

        for (SupportedBlock entry : TheExtractinator.supportedBlocks) {
            if (entry.id.equals(blockId)) {
                supportedBlock = entry;
                break;
            }
        }

        if (supportedBlock == null) {
            TheExtractinator.LOGGER.error("Error generating extractinator loot block!");
            return output;
        }

        for (LootTable entry : TheExtractinator.lootTables) {
            if (ModUtils.modLoaded(entry.namespace) && supportedBlock.tier.equals(entry.tier)) {
                lootTableSlots.addAll(entry.slots);
            }
        }

        if (lootTableSlots.isEmpty()) {
            TheExtractinator.LOGGER.error("Error generating extractinator loot table!");
            return output;
        }

        // Random chance that the extractinator will not drop anything.
        float yield = supportedBlock.yield / 100;
        if (random.nextFloat() > yield) {
            return output;
        }

        List<LootSlot> commonSlots = new ArrayList<>();
        List<LootSlot> uncommonSlots = new ArrayList<>();
        List<LootSlot> rareSlots = new ArrayList<>();
        List<LootSlot> veryRareSlots = new ArrayList<>();
        List<LootSlot> extremelyRareSlots = new ArrayList<>();

        for (LootSlot slot : lootTableSlots) {
            switch (slot.rarity) {
            case COMMON -> commonSlots.add(slot);
            case UNCOMMON -> uncommonSlots.add(slot);
            case RARE -> rareSlots.add(slot);
            case VERY_RARE -> veryRareSlots.add(slot);
            case EXTREMELY_RARE -> extremelyRareSlots.add(slot);
            }
        }

        // Remove disabled drops.
        for (Identifier drop : supportedBlock.disabledDrops) {
            output.removeIf(stack -> Registry.ITEM.getId(stack.getItem()).equals(drop));
        }

        // Add additional drops. Note: disabled drops does not affect this.
        for (LootSlot drop : supportedBlock.additionalDrops) {
            switch (drop.rarity) {
            case COMMON -> {
                if (!commonSlots.contains(drop)) {
                    commonSlots.add(drop);
                }
            }
            case UNCOMMON -> {
                if (!uncommonSlots.contains(drop)) {
                    uncommonSlots.add(drop);
                }
            }
            case RARE -> {
                if (!rareSlots.contains(drop)) {
                    rareSlots.add(drop);
                }
            }
            case VERY_RARE -> {
                if (!veryRareSlots.contains(drop)) {
                    veryRareSlots.add(drop);
                }
            }
            case EXTREMELY_RARE -> {
                if (!extremelyRareSlots.contains(drop)) {
                    extremelyRareSlots.add(drop);
                }
            }
            }
        }

        ExtractinatorConfig config = TheExtractinator.CONFIG.extractinatorConfig;

        output.addAll(addRandomLoot(config.commonItemChance, commonSlots, random));
        output.addAll(addRandomLoot(config.uncommonItemChance, uncommonSlots, random));
        output.addAll(addRandomLoot(config.rareItemChance, rareSlots, random));
        output.addAll(addRandomLoot(config.veryRareItemChance, veryRareSlots, random));
        output.addAll(addRandomLoot(config.extremelyRareItemChance, extremelyRareSlots, random));

        return output;
    }

    private static List<ItemStack> addRandomLoot(float chance, List<LootSlot> slots, Random random) {
        List<ItemStack> drops = new ArrayList<>();

        if (slots.size() > 0) {
            if (random.nextFloat(100) < chance) {
                LootSlot slot = slots.get(random.nextInt(slots.size()));
                Item item = Registry.ITEM.get(slot.id);
                int min = slot.range.getMinimum();
                int max = slot.range.getMaximum();

                int count = min == max ? min : random.nextInt(max - min) + min;
                drops.add(new ItemStack(item, count));
            }
        }
        return drops;
    }
}

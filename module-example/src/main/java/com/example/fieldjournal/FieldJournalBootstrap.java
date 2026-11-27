package com.example.fieldjournal;

import com.ultikits.ultitools.bootstrap.ModuleBootstrap;
import com.ultikits.ultitools.bootstrap.ModuleBootstrapContext;
import com.ultikits.ultitools.text.MessageKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;

public final class FieldJournalBootstrap implements ModuleBootstrap {
    @Override
    public void bootstrap(ModuleBootstrapContext context) {
        Key trailMark = Key.key(MessageKey.namespaceFor(context.moduleName()), "trail_mark");
        Key shovel = Key.key("minecraft:diamond_shovel");
        context.enchantments().register(trailMark, (builder, tags) -> builder
                .description(Component.text("Trail Mark"))
                .supportedItems(RegistrySet.keySet(RegistryKey.ITEM, RegistryKey.ITEM.typedKey(shovel)))
                .weight(5).maxLevel(1)
                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(21, 0))
                .anvilCost(1).activeSlots(EquipmentSlotGroup.MAINHAND));
        context.logger().info("Declared Trail Mark (" + trailMark.asString() + ").");
    }
}

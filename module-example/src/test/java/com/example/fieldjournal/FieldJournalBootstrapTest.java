package com.example.fieldjournal;

import com.ultikits.ultitools.bootstrap.EnchantmentChannel;
import com.ultikits.ultitools.bootstrap.EnchantmentDefinition;
import com.ultikits.ultitools.bootstrap.ModuleBootstrapContext;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FieldJournalBootstrapTest {
    @Test
    void declaresOneEnchantmentAndSuppliesItsDefinitionToPaper() {
        ModuleBootstrapContext context = mock(ModuleBootstrapContext.class);
        EnchantmentChannel channel = mock(EnchantmentChannel.class);
        Logger logger = mock(Logger.class);
        when(context.moduleName()).thenReturn("FieldJournal");
        when(context.enchantments()).thenReturn(channel);
        when(context.logger()).thenReturn(logger);

        new FieldJournalBootstrap().bootstrap(context);

        ArgumentCaptor<EnchantmentDefinition> definition = ArgumentCaptor.forClass(EnchantmentDefinition.class);
        verify(channel).register(eq(Key.key("fieldjournal:trail_mark")), definition.capture());
        verifyNoMoreInteractions(channel);
        verify(logger).info("Declared Trail Mark (fieldjournal:trail_mark).");

        var builder = mock(EnchantmentRegistryEntry.Builder.class, RETURNS_SELF);
        definition.getValue().define(builder, null);
        verify(builder).description(Component.text("Trail Mark"));
        verify(builder).supportedItems(any());
        verify(builder).maxLevel(1);
        verify(builder).activeSlots(EquipmentSlotGroup.MAINHAND);
    }
}

package net.poptart.poptarts_mod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.poptart.poptarts_mod.PoptartsMod;
import net.poptart.poptarts_mod.block.ModBlocks;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PoptartsMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> POPTARTS_TAB = CREATIVE_MODE_TAB.register("poptarts_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BLANK_TABLET.get()))
                    .title(Component.translatable("creativetab.poptarts_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        //Items
                        output.accept(ModItems.BLANK_TABLET.get());
                        output.accept(ModItems.AQUATIC_TABLET.get());
                        //Blocks
                        output.accept(ModBlocks.FORGE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}

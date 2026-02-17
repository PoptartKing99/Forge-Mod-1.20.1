package net.poptart.poptarts_mod.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.poptart.poptarts_mod.PoptartsMod;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PoptartsMod.MOD_ID);

    public static final RegistryObject<Item> BLANK_TABLET = ITEMS.register("blank_tablet",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AQUATIC_TABLET = ITEMS.register("aquatic_tablet",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

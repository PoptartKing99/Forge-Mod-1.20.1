package net.poptart.poptarts_mod.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.poptart.poptarts_mod.PoptartsMod;
import net.poptart.poptarts_mod.block.ModBlocks;
import net.minecraftforge.registries.RegistryObject;
import net.poptart.poptarts_mod.entity.custom.ForgeBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PoptartsMod.MOD_ID);


    public static final RegistryObject<BlockEntityType<ForgeBlockEntity>> FORGE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("forge_block_entity", () ->
                    BlockEntityType.Builder.of(ForgeBlockEntity::new,
                            ModBlocks.FORGE.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

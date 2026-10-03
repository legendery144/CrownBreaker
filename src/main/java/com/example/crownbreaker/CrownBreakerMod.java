package com.example.crownbreaker;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class CrownBreakerMod implements ModInitializer {
    public static final String MOD_ID = "crownbreaker";

    public static final Item CROWN_BREAKER = new Item(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "crown_breaker"), CROWN_BREAKER);
    }
}

package com.example.crownbreaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.List;

public class CrownBreakerMod implements ModInitializer {
    public static final String MOD_ID = "crownbreaker";

    public static final Item CROWN_BREAKER = new MaceItem(
            new Item.Settings().maxDamage(2000).fireproof()
    );

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "crown_breaker"), CROWN_BREAKER);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (stack.isOf(CROWN_BREAKER) && !world.isClient() && entity instanceof LivingEntity target) {
                
                // Ability 1: Armor Shatter Debuff (Every Hit)
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2));

                // Ability 2 & 3: Ground Slam Shockwave & Lightning Strike (On Jump Attack)
                if (player.fallDistance > 1.5f) {
                    float extraDamage = player.fallDistance * 4.0f;
                    target.damage(world.getDamageSources().playerAttack(player), extraDamage);

                    if (world instanceof ServerWorld serverWorld) {
                        // Ability 2: Lightning Strike during Weather
                        if (world.isThundering() || world.isRaining()) {
                            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
                            if (lightning != null) {
                                lightning.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
                                world.spawnEntity(lightning);
                            }
                        }

                        // Ability 3: Area Shockwave (Aaspas ke saare mobs knockback honge)
                        Box area = new Box(target.getBlockPos()).expand(5.0);
                        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class, area, e -> e != player && e != target);

                        for (LivingEntity nearby : nearbyEntities) {
                            nearby.damage(world.getDamageSources().playerAttack(player), extraDamage * 0.5f);
                            nearby.addVelocity(0, 0.6, 0); // Hawa me fekna
                        }

                        // FX & Particles
                        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY(), target.getZ(), 1, 0, 0, 0, 0);
                        serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY(), target.getZ(), 3, 0.5, 0.5, 0.5, 0.1);
                    }

                    world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 2.0f, 0.8f);
                }
            }
            return ActionResult.PASS;
        });
    }
}

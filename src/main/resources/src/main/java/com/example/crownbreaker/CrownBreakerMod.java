package com.example.crownbreaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World.ExplosionSourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrownBreakerMod implements ModInitializer {

    public static final String MOD_ID = "crownbreaker";

    public static final Item CROWN_BREAKER = new MaceItem(
            new Item.Settings().maxDamage(3000)
    );

    private final Map<UUID, Long> skyLauncherCooldown = new HashMap<>();
    private final Map<UUID, Long> waterDashCooldown = new HashMap<>();
    private final Map<UUID, Integer> ultCharge = new HashMap<>();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "crown_breaker"), CROWN_BREAKER);

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(CROWN_BREAKER)) return TypedActionResult.pass(stack);

            long currentTime = System.currentTimeMillis();
            UUID uuid = player.getUuid();

            // 1. Sky Launcher [Shift + Right Click]
            if (player.isSneaking()) {
                long lastUsed = skyLauncherCooldown.getOrDefault(uuid, 0L);
                if (currentTime - lastUsed < 10000) {
                    if (!world.isClient()) {
                        long remaining = (10000 - (currentTime - lastUsed)) / 1000;
                        player.sendMessage(Text.literal("§cSky Launcher Cooldown: " + remaining + "s"), true);
                    }
                    return TypedActionResult.fail(stack);
                }

                skyLauncherCooldown.put(uuid, currentTime);
                player.addVelocity(0, 1.8, 0);
                player.velocityModified = true;

                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 1.0f);

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 25, 0.2, 0.2, 0.2, 0.1);
                }

                return TypedActionResult.success(stack);
            }

            // 2. Water Dash [Right Click in Water/Rain]
            if (player.isTouchingWater() || player.isRainLooking()) {
                long lastUsed = waterDashCooldown.getOrDefault(uuid, 0L);
                if (currentTime - lastUsed < 5000) {
                    if (!world.isClient()) {
                        long remaining = (5000 - (currentTime - lastUsed)) / 1000;
                        player.sendMessage(Text.literal("§cWater Dash Cooldown: " + remaining + "s"), true);
                    }
                    return TypedActionResult.fail(stack);
                }

                waterDashCooldown.put(uuid, currentTime);
                Vec3d lookDir = player.getRotationVector();
                player.addVelocity(lookDir.x * 2.5, 0.3, lookDir.z * 2.5);
                player.velocityModified = true;

                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.8f, 1.2f);

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.SPLASH, player.getX(), player.getY(), player.getZ(), 50, 0.5, 0.5, 0.5, 0.2);
                }

                return TypedActionResult.success(stack);
            }

            return TypedActionResult.pass(stack);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(CROWN_BREAKER)) return ActionResult.PASS;

            UUID uuid = player.getUuid();

            // 3. Ultimate: Soul Reaper [Shift + Left Click]
            if (player.isSneaking()) {
                int currentUlt = ultCharge.getOrDefault(uuid, 0);

                if (currentUlt >= 25) {
                    if (!world.isClient()) {
                        ultCharge.put(uuid, 0);
                        world.createExplosion(player, player.getX(), player.getY(), player.getZ(), 3.5f, ExplosionSourceType.MOB);

                        Box aoeBox = player.getBoundingBox().expand(3.5, 3.5, 3.5);
                        for (Entity target : world.getOtherEntities(player, aoeBox)) {
                            if (target instanceof LivingEntity living) {
                                living.damage(world.getDamageSources().playerAttack(player), 100.0f);
                            }
                        }

                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(), player.getZ(), 3, 0, 0, 0, 0);
                        }

                        player.sendMessage(Text.literal("§d§lSOUL REAPER UNLEASHED! (100 Raw Damage)"), true);
                    }
                    return ActionResult.SUCCESS;
                } else {
                    if (!world.isClient()) {
                        player.sendMessage(Text.literal("§eULT Charge: " + currentUlt + "/25 (Land Smash Attacks to charge!)"), true);
                    }
                }
            } else {
                // Land Smash Attack to Charge ULT
                if (player.fallDistance > 1.5f && entity instanceof LivingEntity) {
                    int currentUlt = ultCharge.getOrDefault(uuid, 0);
                    if (currentUlt < 25) {
                        currentUlt++;
                        ultCharge.put(uuid, currentUlt);
                        if (!world.isClient()) {
                            player.sendMessage(Text.literal("§aCROWNBREAKER ULT: " + currentUlt + "/25"), true);
                        }
                    }
                }
            }

            return ActionResult.PASS;
        });
    }
}

package musheor.utils.manager;

import java.util.List;
import java.util.function.BooleanSupplier;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import musheor.modules.automation.ElytraTweakz;
import musheor.utils.HighwayLogic;
import musheor.utils.system.MusheorSystem;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import musheor.utils.PacketBudget;

public class ElytraFireworkManager {
   private static BooleanSupplier fakeFlySetting;
   private static Module ownerModule;
   private static boolean pendingRocketAfterSwap;
   private static final Object glideLock = new Object();
   private static volatile boolean fakeFlying;
   private static volatile boolean sawGlideFlag;
   private static volatile boolean holdingGlidePacket;
   private static int ticksSinceAction;
   private static int holdTicks;
   private static int launchTickCounter;
   private static int rocketCooldownTicks;
   private static long lastRocketTime;
   private static boolean boostActive;
   private static boolean launched;

   public static void enable(Module module, BooleanSupplier setting) {
      ownerModule = module;
      fakeFlySetting = setting;
      pendingRocketAfterSwap = false;
      resetFakeFlyState();
      if (isFakeFlyEnabled()) {
         unequipElytra();
      }
   }

   public static void disable(Module module) {
      if (ownerModule == module) {
         releaseHeldGlide(true);
         ownerModule = null;
         fakeFlySetting = null;
         pendingRocketAfterSwap = false;
         resetFakeFlyState();
      }
   }

   public static boolean isFakeFlyEnabled() {
      return fakeFlySetting != null && fakeFlySetting.getAsBoolean();
   }

   private static boolean tickFakeFly() {
      if (!isFakeFlyEnabled()) {
         return false;
      } else if (HighwayLogic.mc.player.isOnGround()) {
         cancelFakeFly();
         return true;
      } else {
         ticksSinceAction++;
         if (holdingGlidePacket && ++holdTicks > 8) {
            releaseHeldGlide(true);
         }

         if (shouldStartFakeFly() && PacketBudget.getRemainingInvBudget() > 4) {
            boolean wearingElytra = HighwayLogic.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
            int elytraSlot = wearingElytra ? -1 : findUsableElytraHotbarSlot();
            if (wearingElytra || elytraSlot != -1 && HighwayLogic.mc.player.currentScreenHandler == HighwayLogic.mc.player.playerScreenHandler) {
               boolean swapped = false;
               releaseHeldGlide(false);
               sawGlideFlag = false;
               fakeFlying = true;
               ticksSinceAction = 0;
               if (!wearingElytra) {
                  swapChestWithHotbar(elytraSlot);
                  swapped = true;
               }

               startGliding();
               if (pendingRocketAfterSwap) {
                  pendingRocketAfterSwap = false;
                  if (InventoryManager.findItemSlot(Items.FIREWORK_ROCKET) != -1) {
                     fireRocket();
                  }
               }

               if (swapped) {
                  swapChestWithHotbar(elytraSlot);
               }

               return true;
            } else {
               return true;
            }
         } else {
            return true;
         }
      }
   }

   private static boolean shouldStartFakeFly() {
      if (ticksSinceAction < 2) {
         return false;
      } else if (TickRate.INSTANCE.getTimeSinceLastTick() > 1.5F) {
         return false;
      } else {
         return fakeFlying && !sawGlideFlag ? ticksSinceAction > glideConfirmTicks() : true;
      }
   }

   private static int glideConfirmTicks() {
      float tps = TickRate.INSTANCE.getTickRate();
      if (tps <= 0.0F) {
         tps = 20.0F;
      }

      int ping = 0;
      if (HighwayLogic.mc.getNetworkHandler() != null) {
         PlayerListEntry entry = HighwayLogic.mc.getNetworkHandler().getPlayerListEntry(HighwayLogic.mc.player.getUuid());
         if (entry != null) {
            ping = entry.getLatency();
         }
      }

      return 2 + (int)Math.ceil((ping + 2000.0 / tps) / 50.0);
   }

   public static void onPacketReceive(Receive event) {
      if (isFakeFlyEnabled()) {
         if (!(event.packet instanceof PlayerPositionLookS2CPacket)) {
            if (event.packet instanceof EntityTrackerUpdateS2CPacket packet && fakeFlying) {
               ClientPlayerEntity player = HighwayLogic.mc.player;
               if (player != null && packet.id() == player.getId()) {
                  List<SerializedEntry<?>> values = packet.trackedValues();

                  for (int i = 0; i < values.size(); i++) {
                     SerializedEntry<?> entry = values.get(i);
                     if (entry.id() == 0 && entry.value() instanceof Byte flags) {
                        if ((flags & -128) != 0) {
                           return;
                        }

                        sawGlideFlag = true;
                        if (!player.isGliding()) {
                           return;
                        }

                        TrackedDataHandler<Byte> handler = (TrackedDataHandler)entry.handler();
                        synchronized (glideLock) {
                           try {
                              values.set(i, new SerializedEntry(entry.id(), handler, (byte)(flags | -128)));
                           } catch (UnsupportedOperationException var11) {
                              return;
                           }

                           holdTicks = 0;
                           holdingGlidePacket = true;
                           CommandQueue.beginCapture();
                           return;
                        }
                     }
                  }
               }
            }
         } else {
            if (holdingGlidePacket || CommandQueue.isCapturing()) {
               CommandQueue.flush();
               HighwayLogic.mc.execute(() -> {
                  releaseHeldGlide(true);
                  fakeFlying = false;
               });
            }
         }
      }
   }

   public static void cancelFakeFly() {
      releaseHeldGlide(true);
      fakeFlying = false;
   }

   public static void reset() {
      CommandQueue.clear();
      resetFakeFlyState();
   }

   private static void releaseHeldGlide(boolean dropClientGlide) {
      boolean held;
      synchronized (glideLock) {
         held = holdingGlidePacket;
         holdingGlidePacket = false;
         holdTicks = 0;
         CommandQueue.flush();
      }

      if (held && dropClientGlide && HighwayLogic.mc.player != null && HighwayLogic.mc.player.isGliding()) {
         HighwayLogic.mc.player.stopGliding();
      }
   }

   private static void resetFakeFlyState() {
      synchronized (glideLock) {
         holdingGlidePacket = false;
         holdTicks = 0;
      }

      fakeFlying = false;
      sawGlideFlag = false;
      ticksSinceAction = 2;
   }

   private static void swapChestWithHotbar(int hotbarSlot) {
      HighwayLogic.mc
         .interactionManager
         .clickSlot(HighwayLogic.mc.player.playerScreenHandler.syncId, 6, hotbarSlot, SlotActionType.SWAP, HighwayLogic.mc.player);
   }

   public static void unequipElytra() {
      if (HighwayLogic.mc.player != null && HighwayLogic.mc.interactionManager != null) {
         if (HighwayLogic.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            if (HighwayLogic.mc.player.currentScreenHandler == HighwayLogic.mc.player.playerScreenHandler) {
               int chestplate = InventoryManager.findChestArmorSlot();
               if (chestplate >= 0 && chestplate <= 8) {
                  swapChestWithHotbar(chestplate);
               } else {
                  int free = findEmptyHotbarSlot();
                  if (free == -1) {
                     MusheorSystem.debug("%sFake-fly: no free hotbar slot to stow the equipped elytra.", Formatting.RED);
                  } else {
                     swapChestWithHotbar(free);
                     if (chestplate != -1) {
                        InvUtils.move().from(chestplate).toArmor(2);
                     }
                  }
               }
            }
         }
      }
   }

   public static int findUsableElytraHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = HighwayLogic.mc.player.getInventory().getStack(i);
         if (stack.isOf(Items.ELYTRA) && stack.getMaxDamage() - stack.getDamage() > 1) {
            return i;
         }
      }

      return -1;
   }

   private static int findEmptyHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         if (HighwayLogic.mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static void beginBoost() {
      launchTickCounter = -1;
      rocketCooldownTicks = 0;
      lastRocketTime = 0L;
      boostActive = true;
      launched = false;
   }

   public static boolean needsElytra() {
      if (HighwayLogic.mc.player == null) {
         return true;
      } else {
         ItemStack worn = HighwayLogic.mc.player.getEquippedStack(EquipmentSlot.CHEST);
         return worn.isOf(Items.ELYTRA) && worn.getMaxDamage() - worn.getDamage() > ElytraTweakz.INSTANCE.durabilityThreshold.get()
            ? false
            : InventoryManager.findUsableElytraSlot() == -1;
      }
   }

   public static void endBoost() {
      boostActive = false;
      launched = false;
   }

   public static boolean isBoostActive() {
      return boostActive;
   }

   public static boolean isLaunched() {
      return launched && boostActive;
   }

   public static void markLaunched() {
      launched = true;
   }

   public static void tickLaunch() {
      launchTickCounter++;
      if (launchTickCounter >= (Integer)MusheorSystem.Manager.launchDelay.get()) {
         if (HighwayLogic.mc.player != null) {
            boolean gliding = HighwayLogic.mc.player.isGliding();
            if (gliding) {
               launched = true;
            } else if (!boostActive) {
               launched = false;
            }
         }

         if (rocketCooldownTicks > 0) {
            rocketCooldownTicks--;
         }
      }
   }

   public static boolean hasActiveRocket() {
      if ((Boolean)MusheorSystem.Manager.autoRocket.get()) {
         for (Entity entity : HighwayLogic.mc.world.getEntities()) {
            if (entity instanceof FireworkRocketEntity rocket && rocket.getOwner() == HighwayLogic.mc.player) {
               return true;
            }
         }

         return false;
      } else {
         return System.currentTimeMillis() - lastRocketTime < (Double)MusheorSystem.Manager.rocketDelay.get() * 1000.0;
      }
   }

   public static void tryFireRocket() {
      if (!RotationManager.outranksMovement()) {
         if (rocketCooldownTicks <= 0 && !hasActiveRocket()) {
            if (InventoryManager.findItemSlot(Items.FIREWORK_ROCKET) != -1) {
               if (!HighwayLogic.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                  if (isFakeFlyEnabled()) {
                     pendingRocketAfterSwap = true;
                  }
               } else {
                  fireRocket();
               }
            }
         }
      }
   }

   public static void fireRocket() {
      if (!RotationManager.outranksMovement()) {
         int rocket = InventoryManager.findItemSlot(Items.FIREWORK_ROCKET);
         if (rocket != -1) {
            float yaw = RotationManager.yawOr(HighwayLogic.mc.player.getYaw());
            float pitch = RotationManager.pitchOr(HighwayLogic.mc.player.getPitch());
            InventoryManager.withHotbarSwap(
               rocket,
               () -> HighwayLogic.mc
                  .interactionManager
                  .sendSequencedPacket(HighwayLogic.mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch))
            );
            MusheorSystem.debug("%sAttempted to fire a rocket!", Formatting.AQUA);
            rocketCooldownTicks = (Integer)MusheorSystem.Manager.rocketTimeout.get();
            lastRocketTime = System.currentTimeMillis();
         }
      }
   }

   public static void tickElytra() {
      if (HighwayLogic.mc.player != null && HighwayLogic.mc.getNetworkHandler() != null && HighwayLogic.mc.interactionManager != null) {
         if (!HighwayLogic.mc.player.isSubmergedInWater()) {
            if (!tickFakeFly()) {
               if (!HighwayLogic.mc.player.isGliding() && !HighwayLogic.mc.player.isOnGround()) {
                  startGliding();
                  markLaunched();
               }
            }
         }
      }
   }

   public static void startGliding() {
      HighwayLogic.mc
         .interactionManager
         .sendSequencedPacket(HighwayLogic.mc.world, sequence -> new ClientCommandC2SPacket(HighwayLogic.mc.player, Mode.START_FALL_FLYING));
      HighwayLogic.mc.player.startGliding();
      markLaunched();
   }
}

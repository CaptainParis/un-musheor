package musheor.modules.features;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.utils.manager.RotationManager;
import musheor.utils.manager.ElytraFireworkManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class CreativeFly extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgGrim = this.settings.createGroup("Grim");
   private final Setting<CreativeFly.HoverMode> hover2 = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("hover")).defaultValue(CreativeFly.HoverMode.OFF)).build());
   private final Setting<Boolean> autoSwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-swap"))
                  .description("Automatically swaps to your elytra when activating"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("swap-back"))
                     .description("Automatically swaps back to your chestplate when deactivating"))
                  .defaultValue(true))
               .visible(this.autoSwap::get))
            .build()
      );
   private final Setting<Boolean> pauseOnGround = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("pause-on-ground"))
                  .description("Pauses when the player is touching the ground"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> fakeFly = this.sgGrim
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("fake-fly"))
                  .description("Allows you to fly without having an elytra equipped (requires elytra in the hotbar)"))
               .defaultValue(false))
            .build()
      );
   public static CreativeFly INSTANCE;
   private boolean hoverFlip;
   private Vec3d hoverAnchor;
   private boolean moving;
   private int hoverTicks;

   public CreativeFly() {
      super(Musheor.MAIN, "creative-fly", "Creative flight, but in survival, optimized for GrimAC");
      INSTANCE = this;
   }

   public void onActivate() {
      ElytraFireworkManager.beginBoost();
      if (ElytraFireworkManager.needsElytra()) {
         ChatUtils.info("§cNo elytra found, disabling...", new Object[0]);
         this.toggle();
      } else {
         ElytraFireworkManager.enable(this, this.fakeFly::get);
         if (this.mc.player != null && this.mc.world != null) {
            if ((Boolean)this.autoSwap.get()
               && !(Boolean)this.fakeFly.get()
               && !this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)) {
               ((ChestSwap)Modules.get().get(ChestSwap.class)).swap();
            }

            if (this.mc.player.isOnGround()) {
               this.mc.player.jump();
            }

            this.hoverFlip = false;
            this.hoverAnchor = null;
            this.moving = false;
         }
      }
   }

   public void onDeactivate() {
      RotationManager.clear();
      ElytraFireworkManager.endBoost();
      if (this.mc.player != null && this.mc.world != null) {
         this.mc.player.setNoGravity(false);
         this.hoverAnchor = null;
         if ((Boolean)this.autoSwap.get() && (Boolean)this.swapBack.get() && !(Boolean)this.fakeFly.get()) {
            ((ChestSwap)Modules.get().get(ChestSwap.class)).swap();
         } else {
            ElytraFireworkManager.tickElytra();
         }

         ElytraFireworkManager.disable(this);
      } else {
         ElytraFireworkManager.disable(this);
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      ElytraFireworkManager.onPacketReceive(event);
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      ElytraFireworkManager.reset();
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null) {
         RotationManager.release(0);
         if ((Boolean)this.pauseOnGround.get() && this.mc.player.isOnGround()) {
            ElytraFireworkManager.cancelFakeFly();
         } else {
            ElytraFireworkManager.tickLaunch();
            boolean up = this.mc.options.jumpKey.isPressed();
            boolean down = this.mc.options.sneakKey.isPressed();
            boolean w = this.mc.options.forwardKey.isPressed();
            boolean a = this.mc.options.leftKey.isPressed();
            boolean s = this.mc.options.backKey.isPressed();
            boolean d = this.mc.options.rightKey.isPressed();
            boolean any = up || down || w || a || s || d;
            this.moving = any;
            float cameraYaw = this.mc.player.getYaw();
            if (any) {
               if (this.hoverAnchor != null) {
                  this.mc.player.setPosition(this.hoverAnchor.x, this.hoverAnchor.y, this.hoverAnchor.z);
                  this.mc.player.setVelocity(Vec3d.ZERO);
                  this.mc.player.setNoGravity(false);
                  this.mc.player.resetPosition();
                  this.hoverAnchor = null;
               }

               float flightYaw = cameraYaw;
               if (w && a) {
                  flightYaw = cameraYaw - 45.0F;
               } else if (w && d) {
                  flightYaw = cameraYaw + 45.0F;
               } else if (s && a) {
                  flightYaw = cameraYaw - 135.0F;
               } else if (s && d) {
                  flightYaw = cameraYaw + 135.0F;
               } else if (a) {
                  flightYaw = cameraYaw - 90.0F;
               } else if (d) {
                  flightYaw = cameraYaw + 90.0F;
               } else if (s) {
                  flightYaw = cameraYaw + 180.0F;
               }

               float flightPitch = -1.0F;
               if (!up || !w && !a && !s && !d) {
                  if (up) {
                     flightPitch = -90.0F;
                  } else if (!down || !w && !a && !s && !d) {
                     if (down) {
                        flightPitch = 90.0F;
                     }
                  } else {
                     flightPitch = 45.0F;
                  }
               } else {
                  flightPitch = -45.0F;
               }

               RotationManager.setServerYaw(flightYaw);
               RotationManager.setServerPitch(flightPitch);
               RotationManager.setMoveMode(RotationManager.MoveMode.FORWARD, 0);
            }

            if (this.hoverAnchor == null || (Boolean)this.fakeFly.get()) {
               ElytraFireworkManager.tickElytra();
            }

            if (!any) {
               if (this.hover2.get() != CreativeFly.HoverMode.OFF) {
                  if (ElytraFireworkManager.hasActiveRocket()) {
                     float flightYawx = cameraYaw;
                     float flightPitch = -1.0F;
                     if (this.hover2.get() == CreativeFly.HoverMode.HOLD) {
                        if (this.hoverFlip) {
                           flightYawx = cameraYaw + 180.0F;
                        }
                     } else if (this.hover2.get() == CreativeFly.HoverMode.TOGGLE) {
                        flightPitch = this.hoverFlip ? -90.0F : 90.0F;
                     }

                     this.hoverFlip = !this.hoverFlip;
                     RotationManager.setServerYaw(flightYawx);
                     RotationManager.setServerPitch(flightPitch);
                  } else {
                     ElytraFireworkManager.tryFireRocket();
                  }
               } else {
                  this.updateHoverAnchor();
               }
            } else {
               ElytraFireworkManager.tryFireRocket();
            }
         }
      }
   }

   private void updateHoverAnchor() {
      if (this.hoverAnchor == null) {
         this.hoverAnchor = VersionHelper.get().getPlayerPos();
         this.hoverTicks = 0;
      }

      if (++this.hoverTicks >= 60 && !ElytraFireworkManager.hasActiveRocket()) {
         this.hoverTicks = 0;
         this.hoverAnchor = this.hoverAnchor.subtract(0.0, 0.0313, 0.0);
      }

      this.mc.player.setVelocity(Vec3d.ZERO);
      this.mc.player.setNoGravity(true);
      this.mc.player.setPosition(this.hoverAnchor.x, this.hoverAnchor.y, this.hoverAnchor.z);
      ElytraFireworkManager.tryFireRocket();
   }

   public static enum HoverMode {
      OFF,
      HOLD,
      TOGGLE;}
}

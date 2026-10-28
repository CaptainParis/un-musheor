package musheor.modules.features;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.manager.ElytraFireworkManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

public class KekFly extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgGrim = this.settings.createGroup("Grim");
   private final Setting<Boolean> autoSwap = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("auto-swap")).description("Automatically swaps to your elytra when activating")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("swap-back")).description("Automatically swaps back to your chestplate when deactivating"))
                  .defaultValue(true))
               .visible(this.autoSwap::get))
            .build()
      );
   public final Setting<Boolean> lockY = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("lock-y"))
                  .description("Steer the player's pitch each tick to maintain a target Y level while gliding."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Integer> yLevel = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("y-level"))
                     .description("The Y level to maintain when lock-y is enabled."))
                  .defaultValue(350))
               .sliderRange(64, 512)
               .visible(this.lockY::get))
            .build()
      );
   private final Setting<Boolean> fakeFly = this.sgGrim
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("fake-fly")).description("Allows you to fly while wearing a chestplate")).defaultValue(false))
            .build()
      );
   public static KekFly INSTANCE;

   public KekFly() {
      super(Musheor.MAIN, "kek-fly", "ZOOM thru the air, or even on ground...");
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
            if ((Boolean)this.autoSwap.get() && !(Boolean)this.fakeFly.get() && !this.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
               )
             {
               ((ChestSwap)Modules.get().get(ChestSwap.class)).swap();
            }

            if (this.mc.player.isOnGround()) {
               this.mc.player.jump();
            }
         }
      }
   }

   public void onDeactivate() {
      ElytraFireworkManager.endBoost();
      if (this.mc.player != null && this.mc.world != null) {
         this.mc.player.setNoGravity(false);
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
         ElytraFireworkManager.tickLaunch();
         ElytraFireworkManager.tickElytra();
         if ((Boolean)this.lockY.get()) {
            this.steerPitchToYLevel();
         }

         ElytraFireworkManager.tryFireRocket();
      }
   }

   private void steerPitchToYLevel() {
      if (!this.mc.player.isOnGround()) {
         if (PlayerUtils.getDimension() != Dimension.Nether) {
            double deltaY = ((Integer)this.yLevel.get()).intValue() - this.mc.player.getY();
            float desiredPitch = (float)Math.toDegrees(Math.atan2(-deltaY, 16.0));
            desiredPitch = Math.max(-45.0F, Math.min(45.0F, desiredPitch));
            this.mc.player.setPitch(desiredPitch);
         }
      }
   }
}

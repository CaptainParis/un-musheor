package musheor.modules.automation;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.manager.ElytraFireworkManager;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ElytraTweakz extends Module {
   private final Setting<Boolean> doubleJumpEquipElytra = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("double-jump-equip-elytra"))
                  .description("Equip your elytra and start gliding when double-tapping spacebar while airborne"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> landEquipChestplate = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("land-equip-chestplate"))
                  .description("Automatically equip your chestplate when landing after gliding"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoSwapBroken = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("auto-swap-broken"))
                  .description("Automatically swap with a new elytra when your current one is broken / damaged"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Integer> durabilityThreshold = this.settings
      .getDefaultGroup()
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("durability-threshold"))
                  .defaultValue(10))
               .sliderRange(1, 100)
               .visible(this.autoSwapBroken::get))
            .build()
      );
   private final Setting<Keybind> fireRocketKeybind = this.settings
      .getDefaultGroup()
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("fire-rocket-keybind"))
                  .description("Fires a rocket when pressed/held. Does nothing if there is already an active rocket"))
               .defaultValue(Keybind.none()))
            .build()
      );
   public static ElytraTweakz INSTANCE;
   private boolean wasJumpPressed;
   private boolean wasOnGround;
   private boolean wasGliding;
   private long lastJumpTime;
   private int equipGlideTicks;
   private int rocketCooldown;

   public ElytraTweakz() {
      super(Musheor.MAIN, "elytra-tweakz", "Useful elytra enhancements and tweaks");
      INSTANCE = this;
   }

   public void onActivate() {
      this.wasJumpPressed = false;
      this.wasOnGround = false;
      this.wasGliding = false;
      this.lastJumpTime = 0L;
      this.equipGlideTicks = 0;
      this.rocketCooldown = 0;
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         boolean onGround = this.mc.player.isOnGround();
         boolean gliding = this.mc.player.isGliding();
         boolean jumpPressed = this.mc.options.jumpKey.isPressed();
         boolean jumpJustPressed = jumpPressed && !this.wasJumpPressed;
         if ((Boolean)this.autoSwapBroken.get()) {
            ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (chest.isOf(Items.ELYTRA)) {
               int dura = chest.getMaxDamage() - chest.getDamage();
               if (dura <= (Integer)this.durabilityThreshold.get()) {
                  for (int i = 0; i < this.mc.player.getInventory().main.size(); i++) {
                     ItemStack stack = this.mc.player.getInventory().getStack(i);
                     if (stack.isOf(Items.ELYTRA) && stack.getMaxDamage() - stack.getDamage() > (Integer)this.durabilityThreshold.get()) {
                        InvUtils.move().from(i).toArmor(2);
                        break;
                     }
                  }
               }
            }
         }

         if (!ElytraFireworkManager.isBoostActive() && !((KekBounce)Modules.get().get(KekBounce.class)).isActive()) {
            ElytraFireworkManager.tickLaunch();
            if ((Boolean)this.doubleJumpEquipElytra.get()) {
               if (jumpJustPressed) {
                  long now = System.currentTimeMillis();
                  if (!onGround && now - this.lastJumpTime < 300L) {
                     if (this.mc.player.isCreative() || this.mc.player.getAbilities().flying) {
                        return;
                     }

                     ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
                     if (!chest.isOf(Items.ELYTRA)) {
                        boolean hasElytra = this.mc
                           .player
                           .getInventory()
                           .main
                           .stream()
                           .anyMatch(s -> s.isOf(Items.ELYTRA) && s.getMaxDamage() - s.getDamage() > (Integer)this.durabilityThreshold.get());
                        if (hasElytra) {
                           InventoryManager.swapElytraChestplate();
                           this.equipGlideTicks = 1;
                           return;
                        }
                     } else if (chest.getMaxDamage() - chest.getDamage() > (Integer)this.durabilityThreshold.get()) {
                        ElytraFireworkManager.tickElytra();
                     }
                  }

                  this.lastJumpTime = now;
               }

               if (this.equipGlideTicks > 0) {
                  this.equipGlideTicks--;
                  ElytraFireworkManager.tickElytra();
               }
            }

            if ((Boolean)this.landEquipChestplate.get()
               && !this.wasOnGround
               && onGround
               && this.wasGliding
               && this.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
               ((ChestSwap)Modules.get().get(ChestSwap.class)).swap();
            }

            if (this.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) && this.mc.player.isGliding()) {
               if (this.rocketCooldown > 0) {
                  this.rocketCooldown--;
               }

               if (((Keybind)this.fireRocketKeybind.get()).isPressed() && this.rocketCooldown == 0) {
                  ElytraFireworkManager.tryFireRocket();
                  this.rocketCooldown = (Integer)MusheorSystem.Manager.rocketTimeout.get();
               }
            }

            this.wasJumpPressed = jumpPressed;
            this.wasOnGround = onGround;
            this.wasGliding = gliding;
         } else {
            this.wasJumpPressed = jumpPressed;
            this.wasOnGround = onGround;
            this.wasGliding = gliding;
         }
      }
   }
}

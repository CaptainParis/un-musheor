package musheor.modules.features;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.ModuleControl;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

public class LogoutGoal extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Dimension> dimension2 = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("dimension")).description("Dimension player is inside.")).defaultValue(Dimension.Nether)).build());
   private final Setting<Integer> xCoord = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("x-coord"))
                  .description("The X coordinate (world border is at ~ 29.999.983)."))
               .defaultValue(0))
            .range(-29999949, 29999949)
            .noSlider()
            .build()
      );
   private final Setting<Integer> zCoord = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("z-coord"))
                  .description("The Z coordinate (world border is at ~ 29.999.983)."))
               .defaultValue(0))
            .range(-29999949, 29999949)
            .noSlider()
            .build()
      );
   private final Setting<Boolean> disableAutoReconnect = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disable_auto-reconnect"))
                  .description("Turns off AutoReconnect when logging out."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoDisable = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-disable"))
                  .description("Automatically disables itself when reaching your destination."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> range2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("range"))
                  .description("Distance from the goal at which the player will log out."))
               .defaultValue(32))
            .min(0)
            .sliderRange(0, 200)
            .build()
      );
   private final Setting<String> text2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("text"))
                  .description("Text displayed on disconnect."))
               .defaultValue("Arrived at destination."))
            .build()
      );
   private final Setting<Boolean> coords = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Coords"))
                  .description("Display coordinates on logout screen."))
               .defaultValue(true))
            .build()
      );

   public LogoutGoal() {
      super(Musheor.MAIN, "logout-goal", "Automatically disconnect when arriving at a specific location.");
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         int playerX = (int)Math.round(this.mc.player.getX());
         int playerZ = (int)Math.round(this.mc.player.getZ());
         if (this.isInXRange() && this.isInZRange() && PlayerUtils.getDimension() == this.dimension2.get()) {
            String message = (String)this.text2.get();
            if ((Boolean)this.disableAutoReconnect.get() && Modules.get().isActive(AutoReconnect.class)) {
               ((AutoReconnect)Modules.get().get(AutoReconnect.class)).toggle();
            }

            if ((Boolean)this.autoDisable.get()) {
               this.toggle();
            }

            if ((Boolean)this.coords.get()) {
               message = message + " X: " + playerX + " Z: " + playerZ;
            }

            ModuleControl.disconnect(message);
            this.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal(message)));
         }
      }
   }

   private boolean isInXRange() {
      assert this.mc.player != null;

      return this.mc.player.getX() <= (Integer)this.xCoord.get() + (Integer)this.range2.get()
         && this.mc.player.getX() >= (Integer)this.xCoord.get() - (Integer)this.range2.get();
   }

   private boolean isInZRange() {
      assert this.mc.player != null;

      return this.mc.player.getZ() <= (Integer)this.zCoord.get() + (Integer)this.range2.get()
         && this.mc.player.getZ() >= (Integer)this.zCoord.get() - (Integer)this.range2.get();
   }
}

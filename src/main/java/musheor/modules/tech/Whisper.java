package musheor.modules.tech;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.ModuleControl;
import net.minecraft.client.MinecraftClient;

public class Whisper extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public final Setting<Integer> antiSpamLength = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("anti-spam-length")).description("The length of the randomly generated message")).defaultValue(12))
            .min(1)
            .sliderRange(1, 31)
            .build()
      );
   private final Setting<String> username2 = this.settings
      .getDefaultGroup()
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("username"))
                  .description("IGN of the player you want to send a message to."))
               .defaultValue(""))
            .build()
      );

   public Whisper() {
      super(Musheor.MAIN, "TPA", "Used for sending TP requests to stasis-bots");
   }

   @EventHandler
   public void onTickPre(Pre event) {
      if (this.isActive()) {
         ModuleControl.sendTpCommand((String)this.username2.get(), (Integer)this.antiSpamLength.get());
         this.toggle();
      }
   }
}

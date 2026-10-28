package musheor.utils.hud;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import musheor.utils.hud.screen.CustomButtonManagerScreen;
import musheor.utils.system.MusheorSystem;
import net.minecraft.client.MinecraftClient;

public class HudEditorKeybind {
   private final MinecraftClient mc = MinecraftClient.getInstance();

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.currentScreen == null) {
         if (((Keybind)MusheorSystem.get().openEditor.get()).isPressed()) {
            this.mc.execute(() -> this.mc.setScreen(new CustomButtonManagerScreen(this.mc.currentScreen)));
         }
      }
   }
}

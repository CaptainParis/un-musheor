package musheor.utils.system;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import musheor.compat.VersionHelper;
import musheor.utils.hud.screen.CustomButtonManagerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class MusheorTab extends Tab {
   public MusheorTab() {
      super("Musheor");
   }

   public TabScreen createScreen(GuiTheme theme) {
      return new MusheorTab.MusheorScreen(theme, this);
   }

   public boolean isScreen(Screen screen) {
      return screen instanceof MusheorTab.MusheorScreen;
   }

   public static class MusheorScreen extends WindowTabScreen {
      public MusheorScreen(GuiTheme theme, Tab tab) {
         super(theme, tab);
      }

      public void initWidgets() {
         this.add(this.theme.settings(MusheorSystem.get().getSettings())).expandX();
         if (VersionHelper.get().supportsHudButtons()) {
            ((WButton)this.add(this.theme.button("Custom HUD Buttons")).widget()).action = () -> MinecraftClient.getInstance()
               .setScreen(new CustomButtonManagerScreen(MinecraftClient.getInstance().currentScreen));
         }
      }

      public void tick() {
         if (MusheorSystem.Manager.update) {
            MusheorSystem.Manager.update = false;
            this.reload();
         }
      }

      public boolean toClipboard() {
         return NbtUtils.toClipboard(MusheorSystem.get());
      }

      public boolean fromClipboard() {
         return NbtUtils.fromClipboard(MusheorSystem.get());
      }
   }
}

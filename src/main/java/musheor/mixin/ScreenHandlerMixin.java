package musheor.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import musheor.modules.tech.ContainerTweaks;
import musheor.utils.hud.CustomHudButton;
import musheor.utils.hud.DraggableButtonWidget;
import musheor.utils.system.MusheorSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import musheor.utils.PacketBudget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HandledScreen.class})
public abstract class ScreenHandlerMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
   @Shadow
   protected T field_2797;
   @Shadow
   protected int field_2776;
   @Shadow
   protected int field_2800;

   @Shadow
   public abstract T getScreenHandler();

   public ScreenHandlerMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"method_25426"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo info) {
      ContainerTweaks ct = (ContainerTweaks)Modules.get().get(ContainerTweaks.class);
      if (ct.isActive() && (Boolean)ct.showButtons.get()) {
         int stealX = ct.stealOffsetX.get() != 0 ? (Integer)ct.stealOffsetX.get() : this.field_2776;
         int stealY = ct.stealOffsetY.get() != 0 ? (Integer)ct.stealOffsetY.get() : this.field_2800 - 22;
         int dumpX = ct.dumpOffsetX.get() != 0 ? (Integer)ct.dumpOffsetX.get() : this.field_2776 + 42;
         int dumpY = ct.dumpOffsetY.get() != 0 ? (Integer)ct.dumpOffsetY.get() : this.field_2800 - 22;
         this.addDrawableChild(
            new DraggableButtonWidget(
               stealX, stealY, 40, 20, Text.literal("Steal"), button -> ct.steal(this.getScreenHandler()), ct.stealOffsetX, ct.stealOffsetY
            )
         );
         this.addDrawableChild(
            new DraggableButtonWidget(
               dumpX, dumpY, 40, 20, Text.literal("Dump"), button -> ct.dump(this.getScreenHandler()), ct.dumpOffsetX, ct.dumpOffsetY
            )
         );
      }

      for (CustomHudButton btn : MusheorSystem.get().customButtons) {
         if (btn.screenTarget.matches(CustomHudButton.ScreenTarget.NONE)) {
            this.addDrawableChild(btn.createWidget(this.getScreenHandler()));
         }
      }
   }

   @Inject(
      method = {"method_2383"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
      if (ContainerTweaks.INSTANCE.isActive() && (Boolean)MusheorSystem.Manager.noPacketKick.get() && !PacketBudget.canSendInvPacket()) {
         ci.cancel();
      } else if (ContainerTweaks.INSTANCE != null && ContainerTweaks.INSTANCE.isActive()) {
         if (slot != null && slot.hasStack()) {
            if (((Keybind)ContainerTweaks.INSTANCE.moveAll.get()).isPressed()) {
               ContainerTweaks.INSTANCE.moveAllItems(this.field_2797, slot);
               ci.cancel();
            }

            if (((Keybind)ContainerTweaks.INSTANCE.moveMatching.get()).isPressed()) {
               ContainerTweaks.INSTANCE.moveMatchingItems(this.field_2797, slot);
               ci.cancel();
            }
         }
      }
   }
}

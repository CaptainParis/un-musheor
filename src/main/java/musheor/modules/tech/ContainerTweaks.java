package musheor.modules.tech;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import musheor.Musheor;
import musheor.utils.manager.InventoryManager;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import musheor.utils.PacketBudget;

public class ContainerTweaks extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final MinecraftClient mc = MinecraftClient.getInstance();
   public static ContainerTweaks INSTANCE;
   private final Setting<Boolean> onlyShulkers = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("only-shulkers")).description("Only moves shulkers.")).defaultValue(true)).build());
   public final Setting<Boolean> showButtons = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("show-buttons")).description("Shows stealing and dumping buttons.")).defaultValue(false)).build());
   public final Setting<Keybind> moveMatching = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("move-matching"))
                  .description("Moves all matching items between inventories."))
               .defaultValue(Keybind.none()))
            .build()
      );
   public final Setting<Keybind> moveAll = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("move-all"))
                  .description("Moves all items from one inventory to the other."))
               .defaultValue(Keybind.none()))
            .build()
      );
   public final Setting<List<Item>> bundleItemWhitelist = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("bundle-item-whitelist"))
                  .description("Items that are allowed to be bundled / unbundled"))
               .defaultValue(new ArrayList()))
            .build()
      );
   public final Setting<Integer> stealOffsetX = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("steal-offset-x"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> stealOffsetY = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("steal-offset-y"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> dumpOffsetX = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("dump-offset-x"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> dumpOffsetY = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("dump-offset-y"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> bundleOffsetX = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("bundle-offset-x"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> bundleOffsetY = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("bundle-offset-y"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> unbundleOffsetX = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("unbundle-offset-x"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> unbundleOffsetY = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("unbundle-offset-y"))
                  .defaultValue(0))
               .visible(() -> false))
            .build()
      );

   public ContainerTweaks() {
      super(Musheor.MAIN, "container-tweaks", "A more advanced way to steal and store items in containers");
      INSTANCE = this;
   }

   private void moveSlots(ScreenHandler handler, int start, int end, boolean steal) {
      for (int i = start;
         i < end && !this.shouldCancelDueToPacketBudget() && (InventoryManager.countEmptySlots() != 0 || !steal) && (InventoryManager.countEmptyContainerSlots(handler) != 0 || steal);
         i++
      ) {
         if (handler.getSlot(i).hasStack()) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (this.isShulker(stack) || !(Boolean)this.onlyShulkers.get()) {
               if (this.mc.currentScreen == null || !Utils.canUpdate()) {
                  break;
               }

               this.mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, this.mc.player);
            }
         }
      }
   }

   public void moveMatchingItems(ScreenHandler handler, Slot clickedSlot) {
      ItemStack target = clickedSlot.getStack().copy();
      if (!target.isEmpty()) {
         boolean clickedInPlayerInv = clickedSlot.inventory == this.mc.player.getInventory();
         MeteorExecutor.execute(
            () -> {
               for (int i = 0;
                  i < handler.slots.size()
                     && (InventoryManager.countEmptySlots() != 0 || clickedInPlayerInv)
                     && (InventoryManager.countEmptyContainerSlots(handler) != 0 || !clickedInPlayerInv)
                     && !this.shouldCancelDueToPacketBudget();
                  i++
               ) {
                  Slot slot = handler.getSlot(i);
                  if ((clickedInPlayerInv ? slot.inventory == this.mc.player.getInventory() : slot.inventory != this.mc.player.getInventory())
                     && slot.hasStack()
                     && (this.isShulker(slot.getStack()) || !(Boolean)this.onlyShulkers.get())
                     && ItemStack.areItemsAndComponentsEqual(slot.getStack(), target)) {
                     if (this.mc.currentScreen == null || !Utils.canUpdate()) {
                        break;
                     }

                     this.mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                  }
               }
            }
         );
      }
   }

   public void moveAllItems(ScreenHandler handler, Slot clickedSlot) {
      ItemStack target = clickedSlot.getStack();
      if (!target.isEmpty()) {
         boolean clickedInPlayerInv = clickedSlot.inventory == this.mc.player.getInventory();
         MeteorExecutor.execute(
            () -> {
               for (int i = 0;
                  i < handler.slots.size()
                     && (InventoryManager.countEmptySlots() != 0 || clickedInPlayerInv)
                     && (InventoryManager.countEmptyContainerSlots(handler) != 0 || !clickedInPlayerInv)
                     && !this.shouldCancelDueToPacketBudget();
                  i++
               ) {
                  Slot slot = handler.getSlot(i);
                  if ((clickedInPlayerInv ? slot.inventory == this.mc.player.getInventory() : slot.inventory != this.mc.player.getInventory())
                     && slot.hasStack()
                     && (this.isShulker(slot.getStack()) || !(Boolean)this.onlyShulkers.get())) {
                     if (this.mc.currentScreen == null || !Utils.canUpdate()) {
                        break;
                     }

                     this.mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                  }
               }
            }
         );
      }
   }

   public void steal(ScreenHandler handler) {
      MeteorExecutor.execute(() -> this.moveSlots(handler, 0, SlotUtils.indexToId(9), true));
   }

   public void dump(ScreenHandler handler) {
      int playerInvOffset = SlotUtils.indexToId(9);
      MeteorExecutor.execute(() -> this.moveSlots(handler, playerInvOffset, playerInvOffset + 36, false));
   }

   private boolean shouldCancelDueToPacketBudget() {
      if (!PacketBudget.canSendInvPacket()) {
         this.warning("§cCancelled slot movement to prevent packet kick!", new Object[0]);
         return true;
      } else {
         return false;
      }
   }

   boolean isShulker(ItemStack stack) {
      return stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock;
   }
}

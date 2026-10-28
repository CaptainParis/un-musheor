package musheor.modules.automation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.ModuleControl;
import musheor.utils.HighwayUtils;
import musheor.utils.manager.InventoryManager;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;

public class InventoryCleaner extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> dropEmptyShulker = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("drop-empty-shulker")).description("Allow the paver to dispose of empty shulkerboxes."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> shulkerGraceTicks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("shulker-grace-ticks"))
                     .description(
                        "Ticks a shulker must appear empty before it is dropped. Prevents dropping freshly picked-up shulkers whose contents haven't synced from the server yet."
                     ))
                  .defaultValue(5))
               .sliderRange(2, 20)
               .visible(this.dropEmptyShulker::get))
            .build()
      );
   private final Setting<Boolean> ignoreHotbar = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("ignore-hotbar")).description("Ignore items in the player's hotbar.")).defaultValue(true)).build());
   private final Setting<Boolean> rotateDrop = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("rotate-drop")).description("Drop items behind you")).defaultValue(false)).build());
   private final Setting<InventoryCleaner.ItemFilterList> itemFilter = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("item-filter"))
                  .description("Whitelist keeps selected items in the inventory, Blacklist throws selected items out."))
               .defaultValue(InventoryCleaner.ItemFilterList.WHITELIST))
            .build()
      );
   private final Setting<List<Item>> dropItemList = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("drop-item-list"))
               .description("List of items."))
            .defaultValue(new Item[]{Items.NETHERRACK})
            .build()
      );
   private final Setting<Integer> dropDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("drop-delay"))
                  .description("Delay between dropping items."))
               .defaultValue(2))
            .sliderRange(1, 20)
            .build()
      );
   private int dropCooldown = 0;
   private final Map<Integer, Integer> emptyShulkerTicks = new HashMap<>();

   public InventoryCleaner() {
      super(Musheor.AUTOMATION, "inv-cleaner", "Automatically dispose of unwanted items.");
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      WButton scanInventoryAndSetItems = (WButton)list.add(theme.button("Blacklist inventory")).widget();
      scanInventoryAndSetItems.action = this::blacklistInventory;
      return list;
   }

   public void onDeactivate() {
      this.dropCooldown = 0;
      this.emptyShulkerTicks.clear();
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (mc.player != null && mc.world != null) {
         int firstSlot = !this.ignoreHotbar.get() ? 0 : 9;

         for (int i = firstSlot; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock && InventoryManager.isEmptyShulker(stack)) {
               this.emptyShulkerTicks.merge(i, 1, Integer::sum);
            } else {
               this.emptyShulkerTicks.remove(i);
            }
         }

         for (int ix = firstSlot; ix < mc.player.getInventory().main.size(); ix++) {
            if (this.dropCooldown < (Integer)this.dropDelay.get()) {
               this.dropCooldown++;
               return;
            }

            ItemStack itemStack = mc.player.getInventory().getStack(ix);
            if (!(itemStack.getItem() instanceof BlockItem) || !(((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock)) {
               if (itemStack.getItem() != Items.AIR
                  && (this.itemFilter.get() != InventoryCleaner.ItemFilterList.WHITELIST || !((List)this.dropItemList.get()).contains(itemStack.getItem()))
                  && (this.itemFilter.get() != InventoryCleaner.ItemFilterList.BLACKLIST || ((List)this.dropItemList.get()).contains(itemStack.getItem()))) {
                  if ((Boolean)this.rotateDrop.get()) {
                     this.dropWithRotation(ix);
                     this.dropCooldown = 0;
                  } else {
                     InvUtils.drop().slot(ix);
                     this.dropCooldown = 0;
                  }
               }
            } else if ((Boolean)this.dropEmptyShulker.get()
               && InventoryManager.isEmptyShulker(itemStack)
               && this.emptyShulkerTicks.getOrDefault(ix, 0) >= (Integer)this.shulkerGraceTicks.get()) {
               this.emptyShulkerTicks.remove(ix);
               if ((Boolean)this.rotateDrop.get()) {
                  this.dropWithRotation(ix);
               } else {
                  InvUtils.drop().slot(ix);
               }

               this.dropCooldown = 0;
            }
         }
      }
   }

   private void blacklistInventory() {
      if (mc.player != null && mc.world != null) {
         ((List)this.dropItemList.get()).clear();
         this.itemFilter.set(InventoryCleaner.ItemFilterList.WHITELIST);

         for (int i = 0; i <= mc.player.getInventory().size(); i++) {
            Item currentItem = mc.player.getInventory().getStack(i).getItem();
            if (!((List)this.dropItemList.get()).contains(currentItem)) {
               ((List)this.dropItemList.get()).add(currentItem);
            }
         }

         ChatUtils.info("Inventory scanned and blacklisted!", new Object[0]);
      }
   }

   private void dropWithRotation(int i) {
      assert mc.player != null;

      float previousYaw = mc.player.getYaw();
      ModuleControl.setAutoWalk(false);
      mc.player
         .networkHandler
         .sendPacket(
            new LookAndOnGround(HighwayUtils.HighwayDirection.oppositeYaw(mc.player.getYaw()), -20.0F, mc.player.isOnGround(), false)
         );
      InvUtils.drop().slot(i);
      mc.player.setYaw(previousYaw);
   }

   public static enum ItemFilterList {
      WHITELIST,
      BLACKLIST;}
}

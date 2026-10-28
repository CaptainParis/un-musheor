package musheor.modules.automation;

import java.util.List;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class HotbarReplenish extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final Setting<Integer> threshold2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("threshold")).description("The threshold of items left to trigger replenishment.")).defaultValue(16))
            .sliderRange(1, 63)
            .build()
      );
   private final Setting<Integer> delay2 = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("delay")).description("Delay between slot movements in ticks")).defaultValue(2)).build());
   private final Setting<List<Item>> offhandItems = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("offhand-item"))
               .description("Item to maintain offhand slot.."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot1Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-1"))
               .description("Item to maintain in the first hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot2Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-2"))
               .description("Item to maintain in the second hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot3Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-3"))
               .description("Item to maintain in the third hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot4Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-4"))
               .description("Item to maintain in the fourth hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot5Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-5"))
               .description("Item to maintain in the fifth hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot6Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-6"))
               .description("Item to maintain in the sixth hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot7Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-7"))
               .description("Item to maintain in the seventh hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot8Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-8"))
               .description("Item to maintain in the eighth hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<List<Item>> slot9Items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("slot-9"))
               .description("Item to maintain in the ninth hotbar slot."))
            .defaultValue(new Item[0])
            .build()
      );
   private static final int[] MANAGED_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 40};
   private int delayTicks;

   public HotbarReplenish() {
      super(Musheor.AUTOMATION, "hotbar-replenish", "Automatically refills specific items in each hotbar slot. Each slot independently configurable.");
   }

   public void onActivate() {
      this.delayTicks = 0;
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.isActive()) {
         if (mc.player != null && mc.world != null) {
            if (((HighwayBuilder)Modules.get().get(HighwayBuilder.class)).isActive() && !((List)this.slot9Items.get()).isEmpty()) {
               this.slot9Items.reset();
               ChatUtils.warning("Warning: Reset last slot in Hotbar-Replenish because HighwayBuilder restocking reserves this for restocking!", new Object[0]);
            }

            if (this.delayTicks > 0) {
               this.delayTicks--;
            } else {
               for (int slot : MANAGED_SLOTS) {
                  if (this.tryReplenishSlot(slot)) {
                     this.delayTicks = (Integer)this.delay2.get();
                     return;
                  }
               }
            }
         }
      }
   }

   private boolean tryReplenishSlot(int slot) {
      List<Item> allowed = this.getAllowedItems(slot);
      if (allowed.isEmpty()) {
         return false;
      } else {
         ItemStack current = this.getStackInSlot(slot);
         if (!current.isEmpty() && allowed.contains(current.getItem())) {
            if (current.isStackable() && current.getCount() <= (Integer)this.threshold2.get()) {
               int source = this.findSourceSlot(stack -> ItemStack.areItemsAndComponentsEqual(stack, current), slot);
               if (source == -1) {
                  return false;
               } else {
                  InvUtils.move().from(source).to(slot);
                  return true;
               }
            } else {
               return false;
            }
         } else {
            int source = this.findSourceSlot(stack -> allowed.contains(stack.getItem()), slot);
            if (source == -1) {
               return false;
            } else {
               InvUtils.move().from(source).to(slot);
               return true;
            }
         }
      }
   }

   private int findSourceSlot(Predicate<ItemStack> wanted, int excludedSlot) {
      assert mc.player != null;

      int best = -1;
      int bestCount = Integer.MAX_VALUE;

      for (int i = 0; i <= 35; i++) {
         if (i != excludedSlot && this.getAllowedItems(i).isEmpty()) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && wanted.test(stack) && stack.getCount() < bestCount) {
               best = i;
               bestCount = stack.getCount();
            }
         }
      }

      return best;
   }

   private ItemStack getStackInSlot(int slot) {
      assert mc.player != null;

      return slot == 40 ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slot);
   }

   private List<Item> getAllowedItems(int slot) {
      if (slot == 40) {
         return (List<Item>)this.offhandItems.get();
      } else {
         return switch (slot) {
            case 0 -> (List)this.slot1Items.get();
            case 1 -> (List)this.slot2Items.get();
            case 2 -> (List)this.slot3Items.get();
            case 3 -> (List)this.slot4Items.get();
            case 4 -> (List)this.slot5Items.get();
            case 5 -> (List)this.slot6Items.get();
            case 6 -> (List)this.slot7Items.get();
            case 7 -> (List)this.slot8Items.get();
            case 8 -> (List)this.slot9Items.get();
            default -> List.of();
         };
      }
   }
}

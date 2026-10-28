package musheor.modules.automation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import musheor.utils.PacketBudget;

public class MapDuper extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBundles = this.settings.createGroup("Bundles");
   private final Setting<Integer> copies2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("copies")).description("How many duplicates to make of each map. Each one costs one empty map."))
               .defaultValue(1))
            .sliderRange(1, 16)
            .build()
      );
   private final Setting<Integer> delay2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("delay")).description("Extra ticks to wait between slot actions. 0 acts once every tick."))
               .defaultValue(3))
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Boolean> disableWhenDone = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disable-when-done"))
                  .description("Turn the module off once a run finishes."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> closeTableWhenDone = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("close-table-when-done"))
                  .description("Close the cartography table once a run finishes."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> chatFeedback = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("chat-feedback"))
                  .description("Reports what was duplicated, and why a run stopped early."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> useBundles = this.sgBundles
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("use-bundles"))
                  .description("Take the maps to copy out of a bundle first. Off, the loose maps in your inventory are the job."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> dropOverflow = this.sgBundles
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("drop-overflow"))
                     .description(
                        "A bundle holds more maps than the inventory has slots, so once it is full the rest are thrown on the floor — looking straight down, so they land at your feet."
                     ))
                  .defaultValue(false))
               .visible(this.useBundles::get))
            .build()
      );
   private final Setting<Boolean> originalsToSource = this.sgBundles
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("originals-to-source"))
                     .description("Put one of each map back into the bundle it came out of."))
                  .defaultValue(true))
               .visible(this.useBundles::get))
            .build()
      );
   private final Setting<Boolean> copiesToBundles = this.sgBundles
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("copies-to-bundles"))
                     .description(
                        "Put the new copies into the other bundles in your inventory, filling them in slot order. Off, or with no bundle free, copies are left loose."
                     ))
                  .defaultValue(true))
               .visible(this.useBundles::get))
            .build()
      );
   private final Setting<Integer> mapsPerBundle = this.sgBundles
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("maps-per-bundle"))
                     .description("How many maps to put in a copy bundle before moving on to the next one. A bundle holds 64."))
                  .defaultValue(64))
               .min(1)
               .sliderRange(1, 64)
               .visible(() -> (Boolean)this.useBundles.get() && (Boolean)this.copiesToBundles.get()))
            .build()
      );
   private MapDuper.Stage stage = MapDuper.Stage.DONE;
   private final Set<MapIdComponent> jobMapIds = new LinkedHashSet<>();
   private final Deque<MapIdComponent> pendingQueue = new ArrayDeque<>();
   private int copiesRemaining;
   private boolean awaitingResult;
   private final Set<MapIdComponent> originalsReturned = new LinkedHashSet<>();
   private final Set<MapIdComponent> completedIds = new LinkedHashSet<>();
   private int sourceBundleSlot = -1;
   private int pendingBundleDest = -1;
   private int delayTicks;
   private int jobCount;
   private boolean lookingDownForDrop;
   private int finishedSyncId = -1;

   public MapDuper() {
      super(Musheor.AUTOMATION, "map-duper", "Duplicates maparts at a cartography table, optionally straight out of and back into bundles.");
   }

   public void onActivate() {
      this.resetAll();
   }

   public void onDeactivate() {
      this.resetAll();
   }

   private void resetRunState() {
      this.stage = MapDuper.Stage.DONE;
      this.jobMapIds.clear();
      this.pendingQueue.clear();
      this.copiesRemaining = 0;
      this.awaitingResult = false;
      this.originalsReturned.clear();
      this.completedIds.clear();
      this.sourceBundleSlot = -1;
      this.pendingBundleDest = -1;
      this.delayTicks = 0;
      this.jobCount = 0;
      this.lookingDownForDrop = false;
   }

   private void resetAll() {
      this.resetRunState();
      this.finishedSyncId = -1;
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.interactionManager != null) {
         CartographyTableScreenHandler handler = this.getTable();
         if (handler == null) {
            if (this.stage != MapDuper.Stage.DONE) {
               this.finishRun("Cartography table closed");
            }
         } else {
            if (this.stage == MapDuper.Stage.DONE) {
               if (handler.syncId == this.finishedSyncId) {
                  return;
               }

               this.beginRun(handler);
               if (this.stage == MapDuper.Stage.DONE) {
                  return;
               }
            }

            if (--this.delayTicks <= 0) {
               this.delayTicks = (Integer)this.delay2.get() + 1;
               if (PacketBudget.canSendInvPacket()) {
                  this.tickStage(handler);
               }
            }
         }
      }
   }

   private CartographyTableScreenHandler getTable() {
      if (this.mc.player == null) {
         return null;
      } else {
         return this.mc.player.currentScreenHandler instanceof CartographyTableScreenHandler table ? table : null;
      }
   }

   private void beginRun(CartographyTableScreenHandler handler) {
      this.jobMapIds.clear();
      this.pendingQueue.clear();
      this.copiesRemaining = 0;
      this.awaitingResult = false;
      this.originalsReturned.clear();
      this.completedIds.clear();
      this.pendingBundleDest = -1;
      this.lookingDownForDrop = false;
      this.sourceBundleSlot = this.useBundles.get() ? this.findSourceBundleSlot(handler) : -1;
      boolean bundled = this.sourceBundleSlot >= 0;
      this.jobMapIds.addAll(bundled ? this.collectBundleMapIds(handler) : this.collectInventoryMapIds(handler));
      if (this.jobMapIds.isEmpty()) {
         this.sourceBundleSlot = -1;
      } else {
         this.jobCount = this.jobMapIds.size();
         if (bundled) {
            this.stage = MapDuper.Stage.IDLE;
         } else {
            this.startCopyPhase(handler);
         }

         if ((Boolean)this.chatFeedback.get()) {
            this.info(
               "Duplicating §b%d§r map(s) §b%d§r time(s) each%s.",
               new Object[]{this.jobMapIds.size(), this.copies2.get(), bundled ? " from a bundle" : ""}
            );
         }
      }
   }

   private void startCopyPhase(CartographyTableScreenHandler handler) {
      this.pendingQueue.clear();
      this.awaitingResult = false;

      for (MapIdComponent id : this.jobMapIds) {
         if (!this.completedIds.contains(id) && this.countMapInInv(handler, id) > 0) {
            this.pendingQueue.addLast(id);
         }
      }

      this.copiesRemaining = (Integer)this.copies2.get();
      this.stage = MapDuper.Stage.OPEN;
   }

   private boolean tickStage(CartographyTableScreenHandler handler) {
      return switch (this.stage) {
         case IDLE-> this.tickIdleUnpack(handler);
         case OPEN-> this.tickOpenCopy(handler);
         case COPY-> this.tickCopyStow(handler);
         case DONE-> false;
      };
   }

   private boolean tickIdleUnpack(CartographyTableScreenHandler handler) {
      boolean acted = false;

      for (int i = 0; i < 160 && PacketBudget.canSendInvPacket(); i++) {
         boolean hadPitch = this.lookingDownForDrop;
         if (!this.unpackSourceStep(handler)) {
            break;
         }

         acted = true;
         if (this.getTable() == null || this.stage != MapDuper.Stage.IDLE || !hadPitch && this.lookingDownForDrop) {
            break;
         }
      }

      return acted;
   }

   private boolean unpackSourceStep(CartographyTableScreenHandler handler) {
      if (getMapId(handler.getCursorStack()) != null) {
         int free = this.countEmptySlots(handler) > 4 ? this.findEmptySlot(handler) : -1;
         if (free >= 0) {
            this.clickSlot(handler, free, 0, SlotActionType.PICKUP);
            return true;
         } else if (!(Boolean)this.dropOverflow.get()) {
            return this.failWithReason("Inventory full and drop-overflow is off");
         } else if (!this.lookingDownForDrop) {
            this.mc.player.setPitch(90.0F);
            this.lookingDownForDrop = true;
            return true;
         } else {
            this.clickSlot(handler, -999, 0, SlotActionType.PICKUP);
            return true;
         }
      } else if (this.depositCursor(handler)) {
         return true;
      } else if (!this.bundleHasMaps(handler, this.sourceBundleSlot)) {
         this.startCopyPhase(handler);
         return true;
      } else {
         this.clickSlot(handler, this.sourceBundleSlot, 1, SlotActionType.PICKUP);
         return true;
      }
   }

   private boolean tickOpenCopy(CartographyTableScreenHandler handler) {
      if (this.depositCursor(handler)) {
         return true;
      } else if (this.countEmptySlots(handler) < 2) {
         return this.stowMapsLoop(handler, true) ? true : this.failWithReason("Inventory full and nothing left that can be put away");
      } else {
         MapIdComponent id = this.pendingQueue.peekFirst();
         if (id == null) {
            this.stage = MapDuper.Stage.COPY;
            return true;
         } else if (handler.getSlot(2).hasStack()) {
            this.clickSlot(handler, 2, 0, SlotActionType.QUICK_MOVE);
            if (this.awaitingResult) {
               this.awaitingResult = false;
               if (--this.copiesRemaining <= 0) {
                  this.markMapDone(id);
               }
            }

            return true;
         } else if (getMapId(handler.getSlot(0).getStack()) != null) {
            if (this.ensureEmptyMap(handler)) {
               return true;
            } else {
               this.awaitingResult = true;
               return false;
            }
         } else if (this.ensureEmptyMap(handler)) {
            return true;
         } else {
            int slot = this.findSlot(handler, stack -> id.equals(getMapId(stack)));
            if (slot < 0) {
               this.markMapDone(id);
               return true;
            } else {
               this.clickSlot(handler, slot, 0, SlotActionType.QUICK_MOVE);
               this.awaitingResult = true;
               return true;
            }
         }
      }
   }

   private void markMapDone(MapIdComponent id) {
      this.completedIds.add(id);
      this.pendingQueue.removeFirstOccurrence(id);
      this.copiesRemaining = (Integer)this.copies2.get();
      this.awaitingResult = false;
   }

   private boolean ensureEmptyMap(CartographyTableScreenHandler handler) {
      if (handler.getSlot(1).hasStack()) {
         return false;
      } else {
         int empty = this.findSlot(handler, stack -> stack.isOf(Items.MAP));
         if (empty < 0) {
            this.failWithReason("Out of empty maps");
            return true;
         } else {
            this.clickSlot(handler, empty, 0, SlotActionType.QUICK_MOVE);
            return true;
         }
      }
   }

   private boolean tickCopyStow(CartographyTableScreenHandler handler) {
      if (this.stowMapsLoop(handler, false)) {
         return true;
      } else if (this.sourceBundleHasPending(handler) && this.countEmptySlots(handler) > 4) {
         this.stage = MapDuper.Stage.IDLE;
         return true;
      } else {
         this.finishRun((String)null);
         return false;
      }
   }

   private boolean stowMapsLoop(CartographyTableScreenHandler handler, boolean onlyCompleted) {
      boolean acted = false;

      for (int i = 0; i < 160 && PacketBudget.canSendInvPacket() && this.stowMapsStep(handler, onlyCompleted); i++) {
         acted = true;
         if (this.getTable() == null) {
            break;
         }
      }

      return acted;
   }

   private boolean stowMapsStep(CartographyTableScreenHandler handler, boolean onlyCompleted) {
      ItemStack cursor = handler.getCursorStack();
      if (getMapId(cursor) != null) {
         if (cursor.getCount() > 1) {
            int free = this.findEmptySlot(handler);
            if (free >= 0) {
               this.clickSlot(handler, free, 1, SlotActionType.PICKUP);
               return true;
            }
         }

         if (this.pendingBundleDest >= 0 && this.canAcceptIntoBundle(handler, this.pendingBundleDest)) {
            this.clickSlot(handler, this.pendingBundleDest, 0, SlotActionType.PICKUP);
            this.pendingBundleDest = -1;
            return true;
         } else {
            this.pendingBundleDest = -1;
            return this.depositCursor(handler);
         }
      } else if (this.depositCursor(handler)) {
         return true;
      } else {
         for (int slot = 3; slot < handler.slots.size(); slot++) {
            MapIdComponent id = getMapId(handler.getSlot(slot).getStack());
            if (id != null && this.jobMapIds.contains(id) && (!onlyCompleted || this.completedIds.contains(id))) {
               int destination = this.chooseBundleDest(handler, id);
               if (destination >= 0) {
                  this.pendingBundleDest = destination;
                  this.clickSlot(handler, slot, 0, SlotActionType.PICKUP);
                  return true;
               }
            }
         }

         return false;
      }
   }

   private int chooseBundleDest(CartographyTableScreenHandler handler, MapIdComponent id) {
      if ((Boolean)this.originalsToSource.get()
         && this.sourceBundleSlot >= 0
         && !this.originalsReturned.contains(id)
         && this.isBundleSlot(handler, this.sourceBundleSlot)
         && this.countMapsInBundle(handler, this.sourceBundleSlot) < 64) {
         this.originalsReturned.add(id);
         return this.sourceBundleSlot;
      } else {
         return this.copiesToBundles.get() ? this.findCopyBundleSlot(handler) : -1;
      }
   }

   private int findCopyBundleSlot(CartographyTableScreenHandler handler) {
      for (int slot = 3; slot < handler.slots.size(); slot++) {
         if (slot != this.sourceBundleSlot
            && this.isBundleSlot(handler, slot)
            && this.isMapsOnlyBundle(handler, slot)
            && this.countMapsInBundle(handler, slot) < (Integer)this.mapsPerBundle.get()) {
            return slot;
         }
      }

      return -1;
   }

   private boolean canAcceptIntoBundle(CartographyTableScreenHandler handler, int slot) {
      if (!this.isBundleSlot(handler, slot)) {
         return false;
      } else {
         int limit = slot == this.sourceBundleSlot ? 64 : (Integer)this.mapsPerBundle.get();
         return this.countMapsInBundle(handler, slot) < limit;
      }
   }

   private boolean depositCursor(CartographyTableScreenHandler handler) {
      if (handler.getCursorStack().isEmpty()) {
         return false;
      } else {
         int free = this.findEmptySlot(handler);
         if (free < 0) {
            return false;
         } else {
            this.clickSlot(handler, free, 0, SlotActionType.PICKUP);
            return true;
         }
      }
   }

   private void clickSlot(ScreenHandler handler, int slot, int button, SlotActionType action) {
      this.mc.interactionManager.clickSlot(handler.syncId, slot, button, action, this.mc.player);
   }

   private boolean failWithReason(String reason) {
      this.finishRun(reason);
      return false;
   }

   private void finishRun(String reason) {
      if ((Boolean)this.chatFeedback.get()) {
         if (reason == null) {
            this.info("Done — §b%d§r mapart(s) duplicated.", new Object[]{this.jobCount});
         } else {
            this.warning("Stopped: %s.", new Object[]{reason});
         }
      }

      CartographyTableScreenHandler handler = this.getTable();
      if (handler != null) {
         this.finishedSyncId = handler.syncId;
      }

      this.resetRunState();
      if ((Boolean)this.closeTableWhenDone.get() && this.mc.player != null) {
         this.mc.player.closeHandledScreen();
      }

      if ((Boolean)this.disableWhenDone.get()) {
         this.toggle();
      }
   }

   private static MapIdComponent getMapId(ItemStack stack) {
      return !stack.isEmpty() && stack.isOf(Items.FILLED_MAP) ? (MapIdComponent)stack.get(DataComponentTypes.MAP_ID) : null;
   }

   private int countMapInInv(ScreenHandler handler, MapIdComponent id) {
      int count = 0;

      for (int slot = 3; slot < handler.slots.size(); slot++) {
         if (id.equals(getMapId(handler.getSlot(slot).getStack()))) {
            count++;
         }
      }

      return count;
   }

   private int findSlot(ScreenHandler handler, Predicate<ItemStack> test) {
      for (int slot = 3; slot < handler.slots.size(); slot++) {
         ItemStack stack = handler.getSlot(slot).getStack();
         if (!stack.isEmpty() && test.test(stack)) {
            return slot;
         }
      }

      return -1;
   }

   private int countEmptySlots(ScreenHandler handler) {
      int count = 0;

      for (int slot = 3; slot < handler.slots.size(); slot++) {
         if (!handler.getSlot(slot).hasStack()) {
            count++;
         }
      }

      return count;
   }

   private int findEmptySlot(ScreenHandler handler) {
      for (int slot = 3; slot < handler.slots.size(); slot++) {
         if (!handler.getSlot(slot).hasStack()) {
            return slot;
         }
      }

      return -1;
   }

   private boolean isBundleSlot(ScreenHandler handler, int slot) {
      return slot >= 3 && slot < handler.slots.size() ? handler.getSlot(slot).getStack().getItem() instanceof BundleItem : false;
   }

   private static BundleContentsComponent getBundleContents(ScreenHandler handler, int slot) {
      return (BundleContentsComponent)handler.getSlot(slot).getStack().get(DataComponentTypes.BUNDLE_CONTENTS);
   }

   private int countMapsInBundle(ScreenHandler handler, int slot) {
      BundleContentsComponent contents = getBundleContents(handler, slot);
      if (contents == null) {
         return 0;
      } else {
         int count = 0;

         for (ItemStack inner : contents.iterate()) {
            if (getMapId(inner) != null) {
               count += inner.getCount();
            }
         }

         return count;
      }
   }

   private boolean isMapsOnlyBundle(ScreenHandler handler, int slot) {
      BundleContentsComponent contents = getBundleContents(handler, slot);
      if (contents != null && !contents.isEmpty()) {
         for (ItemStack inner : contents.iterate()) {
            if (getMapId(inner) == null) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private boolean bundleHasMaps(ScreenHandler handler, int slot) {
      return this.isBundleSlot(handler, slot) && this.countMapsInBundle(handler, slot) > 0;
   }

   private boolean sourceBundleHasPending(ScreenHandler handler) {
      if (!this.isBundleSlot(handler, this.sourceBundleSlot)) {
         return false;
      } else {
         BundleContentsComponent contents = getBundleContents(handler, this.sourceBundleSlot);
         if (contents == null) {
            return false;
         } else {
            for (ItemStack inner : contents.iterate()) {
               MapIdComponent id = getMapId(inner);
               if (id != null && !this.completedIds.contains(id)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private int findSourceBundleSlot(CartographyTableScreenHandler handler) {
      for (int slot = 3; slot < handler.slots.size(); slot++) {
         if (this.bundleHasMaps(handler, slot)) {
            return slot;
         }
      }

      return -1;
   }

   private Set<MapIdComponent> collectBundleMapIds(CartographyTableScreenHandler handler) {
      Set<MapIdComponent> ids = new LinkedHashSet<>();
      if (this.sourceBundleSlot < 0) {
         return ids;
      } else {
         BundleContentsComponent contents = getBundleContents(handler, this.sourceBundleSlot);
         if (contents == null) {
            return ids;
         } else {
            for (ItemStack inner : contents.iterate()) {
               MapIdComponent id = getMapId(inner);
               if (id != null) {
                  ids.add(id);
               }
            }

            return ids;
         }
      }
   }

   private Set<MapIdComponent> collectInventoryMapIds(CartographyTableScreenHandler handler) {
      Set<MapIdComponent> ids = new LinkedHashSet<>();

      for (int slot = 3; slot < handler.slots.size(); slot++) {
         MapIdComponent id = getMapId(handler.getSlot(slot).getStack());
         if (id != null) {
            ids.add(id);
         }
      }

      return ids;
   }

   public String getInfoString() {
      return this.stage == MapDuper.Stage.DONE ? null : this.stage.name().toLowerCase();
   }

   private static enum Stage {
      IDLE,
      OPEN,
      COPY,
      DONE;}
}

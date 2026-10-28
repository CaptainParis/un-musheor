package musheor.modules.automation;

import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ItemSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.HighwayUtils;
import musheor.utils.manager.InventoryManager;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Refill extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final MinecraftClient mc = MinecraftClient.getInstance();
   public static Refill INSTANCE;
   public final Setting<Item> item2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("item")).description("What block to steal / loot from shulker.")).defaultValue(Items.OBSIDIAN))
            .build()
      );
   private final Setting<Boolean> fillAll = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("fill-all"))
                  .description("When this setting is enabled, the inventory will be completely filled up until there are no empty slots"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> maxSlots = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("max-slots"))
                     .description("How many slots can maximumly be filled up"))
                  .defaultValue(10))
               .visible(() -> !(Boolean)this.fillAll.get()))
            .build()
      );
   private final Setting<Boolean> placeUpsideDownBelow = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("place-upside-down-below"))
               .defaultValue(true))
            .build()
      );
   boolean shulkerSelected;
   boolean placed;
   boolean opened;
   int lootSlotIndex;
   int filledSlots;
   int openDelayTicks;
   BlockPos placePos;
   Direction placeSide;

   public Refill() {
      super(Musheor.AUTOMATION, "refill", "Places a shulkerbox and refills the inventory with items.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.shulkerSelected = false;
      this.placePos = null;
      this.placeSide = Direction.DOWN;
      this.placed = false;
      this.opened = false;
      this.lootSlotIndex = 0;
      this.filledSlots = 0;
      this.openDelayTicks = 5;
   }

   @EventHandler
   private void onTickPost(Post event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.isActive()) {
            if (!this.shulkerSelected) {
               ItemStack lookingFor = InventoryManager.findSmallestShulkerWithItem((Item)this.item2.get());
               if (lookingFor != null) {
                  int slot = this.mc.player.getInventory().getSlotWithStack(lookingFor);
                  if (this.mc.player.getInventory().selectedSlot == slot) {
                     this.shulkerSelected = true;
                     return;
                  }

                  for (int i = 0; i < 9; i++) {
                     if (this.mc.player.getInventory().getStack(i) == lookingFor) {
                        InventoryManager.selectHotbarSlot(i);
                        this.shulkerSelected = true;
                        return;
                     }
                  }
               }

               int emptySlot = InventoryManager.findEmptyHotbarSlot();
               if (InventoryManager.moveShulkerWithItemToHotbar((Item)this.item2.get(), emptySlot)) {
                  InventoryManager.selectHotbarSlot(emptySlot);
                  this.shulkerSelected = true;
                  return;
               }
            }

            if (this.placePos == null) {
               if ((Boolean)this.placeUpsideDownBelow.get()) {
                  this.placePos = this.mc.player.getBlockPos().withY(this.mc.player.getBlockPos().getY() - 2);
               } else if (this.mc.crosshairTarget instanceof BlockHitResult bhr) {
                  BlockPos adjacent = bhr.getBlockPos();
                  this.placeSide = bhr.getSide();
                  this.placePos = adjacent.offset(this.placeSide);
               }

               return;
            }

            if (!this.placed) {
               if (BlockUtils.canPlace(this.placePos, true)) {
                  this.placed = HighwayUtils.placeBlock(this.placePos, this.placeSide);
               } else {
                  this.toggle();
                  this.info("Cannot place shulkerbox at desired position", new Object[0]);
               }

               return;
            }

            if (this.mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock) {
               if (!this.opened) {
                  if (!(this.mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
                     this.mc
                        .player
                        .networkHandler
                        .sendPacket(
                           new PlayerInteractBlockC2SPacket(
                              Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(this.placePos), Direction.UP, this.placePos, false), 0
                           )
                        );
                  } else {
                     this.opened = true;
                  }
               } else {
                  ScreenHandler handler = this.mc.player.currentScreenHandler;
                  if (this.openDelayTicks > 0) {
                     this.openDelayTicks--;
                     return;
                  }

                  if ((Boolean)this.fillAll.get()) {
                     for (int ix = 0; ix < 27 && InventoryManager.countEmptySlots() > 0; ix++) {
                        if (handler.getSlot(this.lootSlotIndex).getStack().getItem() == this.item2.get()) {
                           InvUtils.shiftClick().slotId(this.lootSlotIndex);
                        }

                        this.lootSlotIndex++;
                     }

                     this.mc.player.closeHandledScreen();
                     this.toggle();
                  } else if (this.filledSlots >= (Integer)this.maxSlots.get() && this.lootSlotIndex >= 27) {
                     this.mc.player.closeHandledScreen();
                     this.toggle();
                  } else {
                     if (handler.getSlot(this.lootSlotIndex).getStack().getItem() == this.item2.get()) {
                        InvUtils.shiftClick().slotId(this.lootSlotIndex);
                        this.filledSlots++;
                     }

                     this.lootSlotIndex++;
                  }
               }
            }
         }
      }
   }
}

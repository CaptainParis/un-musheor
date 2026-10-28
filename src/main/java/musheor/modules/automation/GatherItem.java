package musheor.modules.automation;

import baritone.api.pathing.goals.GoalBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ItemSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.ModuleControl;
import musheor.utils.HighwayUtils;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import musheor.utils.BaritoneHelper;

public class GatherItem extends Module {
   private static final Set<Block> UNSAFE_GROUND = Set.of(
      Blocks.MAGMA_BLOCK,
      Blocks.SLIME_BLOCK,
      Blocks.HONEY_BLOCK,
      Blocks.POWDER_SNOW,
      Blocks.SCAFFOLDING,
      Blocks.CACTUS,
      Blocks.FIRE,
      Blocks.SOUL_FIRE,
      Blocks.NETHER_PORTAL
   );
   private static final Set<Block> BLOCKING_BLOCKS = Set.of(
      Blocks.COBWEB, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.POWDER_SNOW, Blocks.SWEET_BERRY_BUSH
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Item> item2 = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("item")).description("Item to gather")).defaultValue(Items.OBSIDIAN)).build());
   private final Setting<Integer> searchRadius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("search-radius"))
                  .description("Maximum radius to scan for items"))
               .defaultValue(16))
            .sliderRange(1, 128)
            .build()
      );
   private final Setting<Integer> stuckTimeout = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("stuck-timeout"))
                  .description("Seconds to reach a stand position before giving up on it and trying the next one"))
               .defaultValue(5))
            .sliderRange(1, 30)
            .build()
      );
   private final Setting<Integer> unreachableCooldown = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("unreachable-cooldown"))
                  .description("Seconds an item is ignored after every position around it failed"))
               .defaultValue(10))
            .sliderRange(3, 60)
            .build()
      );
   private final Setting<Boolean> ignoreBelowY119 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ignore-below-y119"))
                  .description("Ignores items below Y-level 119 to prevent the paver from digging underground"))
               .defaultValue(true))
            .build()
      );
   private ItemEntity targetItem = null;
   private BlockPos targetItemPos = null;
   private BlockPos standPos = null;
   private final Deque<BlockPos> standCandidates = new ArrayDeque<>();
   private final Map<UUID, Integer> unreachableUntil = new HashMap<>();
   private int tickCounter = 0;
   private int rescanTimer = 20;
   private int standTicks = 0;
   private boolean hadUnreachable = false;

   public GatherItem() {
      super(Musheor.AUTOMATION, "pickup-item", "A helper module that finds and picks up nearby items.");
   }

   public void onActivate() {
      this.clearTarget();
      this.unreachableUntil.clear();
      this.rescanTimer = 20;
      this.hadUnreachable = false;
   }

   public void onDeactivate() {
      ModuleControl.cancelBaritone();
      this.clearTarget();
   }

   private void clearTarget() {
      this.targetItem = null;
      this.targetItemPos = null;
      this.standPos = null;
      this.standCandidates.clear();
      this.standTicks = 0;
   }

   private ItemEntity findNearestItem() {
      if (this.mc.player != null && this.mc.world != null) {
         int radius = (Integer)this.searchRadius.get();
         Box searchArea = new Box(
            this.mc.player.getX() - radius,
            this.mc.player.getY() - radius,
            this.mc.player.getZ() - radius,
            this.mc.player.getX() + radius,
            this.mc.player.getY() + radius,
            this.mc.player.getZ() + radius
         );
         double radiusSq = (double)radius * radius;
         return this.mc
            .world
            .getEntitiesByClass(
               ItemEntity.class,
               searchArea,
               e -> e.getStack().getItem() == this.item2.get()
                  && e.squaredDistanceTo(this.mc.player) <= radiusSq
                  && !this.unreachableUntil.containsKey(e.getUuid())
                  && (!(Boolean)this.ignoreBelowY119.get() || e.getBlockY() >= 119)
            )
            .stream()
            .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(this.mc.player)))
            .orElse(null);
      } else {
         return null;
      }
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         Module echestFarmer = Modules.get().get(EchestFarmer.class);
         Module hwb = Modules.get().get(HighwayBuilder.class);
         if (echestFarmer == null || !echestFarmer.isActive() || !HighwayBuilder.isEchestFarmerActive()) {
            if (InventoryManager.countEmptySlots() == 0) {
               if (!HighwayUtils.isOnHighway() && hwb.isActive()) {
                  ModuleControl.setAutoWalk(false);
                  if (!BaritoneHelper.isPathing()) {
                     HighwayUtils.returnToLane();
                  }
               }

               MusheorSystem.debug("Inventory full, toggling off gather item...");
               this.toggle();
            } else {
               this.tickCounter++;
               this.unreachableUntil.values().removeIf(expiry -> expiry <= this.tickCounter);
               this.rescanTimer++;
               boolean targetGone = this.targetItem == null || !this.targetItem.isAlive() || this.targetItem.isRemoved();
               if (targetGone || this.rescanTimer >= 20) {
                  ItemEntity found = this.findNearestItem();
                  if (found != this.targetItem) {
                     this.clearTarget();
                     this.targetItem = found;
                  }

                  this.rescanTimer = 0;
               }

               if (this.targetItem == null) {
                  ChatUtils.info(
                     this.hadUnreachable
                        ? "No reachable " + ((Item)this.item2.get()).getName().getString() + " found nearby."
                        : "No more " + ((Item)this.item2.get()).getName().getString() + " found nearby.",
                     new Object[0]
                  );
                  if (!HighwayUtils.isOnHighway() && hwb.isActive()) {
                     ModuleControl.setAutoWalk(false);
                     if (!BaritoneHelper.isPathing()) {
                        HighwayUtils.returnToLane();
                     }
                  }

                  this.toggle();
               } else {
                  BlockPos itemPos = this.targetItem.getBlockPos();
                  if (!itemPos.equals(this.targetItemPos)) {
                     this.targetItemPos = itemPos;
                     this.standCandidates.clear();
                     this.standCandidates.addAll(this.collectStandPositions(this.targetItem));
                     this.pollNextStand();
                  }

                  if (this.standPos != null) {
                     if (!this.isSafeStand(this.standPos)) {
                        this.failStand("stand position is no longer safe");
                     } else if (++this.standTicks > (Integer)this.stuckTimeout.get() * 20) {
                        this.failStand("timed out");
                     } else if (!this.mc.player.getBlockPos().equals(this.standPos) && !this.isNearTarget()) {
                        if (this.standTicks > 5 && !BaritoneHelper.isBusy()) {
                           this.failStand("no path to it");
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void failStand(String reason) {
      if (reason != null && this.standPos != null) {
         MusheorSystem.debug("Stand position %s failed (%s), trying the next one...", this.standPos.toShortString(), reason);
      }

      this.pollNextStand();
   }

   private void pollNextStand() {
      this.standPos = this.standCandidates.poll();
      this.standTicks = 0;
      if (this.standPos == null) {
         MusheorSystem.debug("No reachable position around the %s, skipping it...", ((Item)this.item2.get()).getName().getString());
         if (this.targetItem != null) {
            this.unreachableUntil.put(this.targetItem.getUuid(), this.tickCounter + (Integer)this.unreachableCooldown.get() * 20);
         }

         this.hadUnreachable = true;
         ModuleControl.cancelBaritone();
         this.clearTarget();
         this.rescanTimer = 20;
      } else {
         BaritoneHelper.setGoalAndPath(new GoalBlock(this.standPos));
      }
   }

   private List<BlockPos> collectStandPositions(ItemEntity target) {
      List<BlockPos> positions = new ArrayList<>();
      BlockPos itemPos = target.getBlockPos();

      for (int dy = 1; dy >= -2; dy--) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               BlockPos stand = new BlockPos(itemPos.getX() + dx, itemPos.getY() + dy, itemPos.getZ() + dz);
               if (this.isSafeStand(stand) && this.canReachItemFrom(stand, target)) {
                  positions.add(stand);
               }
            }
         }
      }

      positions.sort(Comparator.comparingDouble(pos -> target.squaredDistanceTo(Vec3d.ofCenter(pos))));
      return positions;
   }

   private boolean isNearTarget() {
      return this.mc.player.getBoundingBox().expand(0.8, 0.4, 0.8).intersects(this.targetItem.getBoundingBox());
   }

   private boolean canReachItemFrom(BlockPos stand, ItemEntity target) {
      double cx = stand.getX() + 0.5;
      double cz = stand.getZ() + 0.5;
      double y = stand.getY();
      Box playerBox = new Box(cx - 0.3, y, cz - 0.3, cx + 0.3, y + 1.8, cz + 0.3).expand(0.8, 0.4, 0.8);
      return playerBox.intersects(target.getBoundingBox());
   }

   private boolean isSafeStand(BlockPos pos) {
      if (this.mc.world == null) {
         return false;
      } else if (pos.getY() <= this.mc.world.getBottomY()) {
         return false;
      } else {
         BlockPos groundPos = pos.down();
         BlockState ground = this.mc.world.getBlockState(groundPos);
         if (ground.isAir()) {
            return false;
         } else if (!ground.getFluidState().isEmpty()) {
            return false;
         } else if (UNSAFE_GROUND.contains(ground.getBlock())) {
            return false;
         } else if (ground.getBlock() instanceof AbstractChestBlock) {
            return false;
         } else if (ground.getBlock() instanceof BedBlock) {
            return false;
         } else {
            return !ground.isSideSolidFullSquare(this.mc.world, groundPos, Direction.UP) ? false : this.isPassable(pos) && this.isPassable(pos.up());
         }
      }
   }

   private boolean isPassable(BlockPos pos) {
      BlockState state = this.mc.world.getBlockState(pos);
      if (!state.getCollisionShape(this.mc.world, pos).isEmpty()) {
         return false;
      } else if (BLOCKING_BLOCKS.contains(state.getBlock())) {
         return false;
      } else {
         FluidState fluid = state.getFluidState();
         return fluid.getFluid() != Fluids.LAVA && fluid.getFluid() != Fluids.FLOWING_LAVA;
      }
   }
}

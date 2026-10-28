package musheor.modules.automation;

import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.modules.features.KekMine;
import musheor.utils.ModuleControl;
import musheor.utils.HighwayUtils;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import musheor.utils.BaritoneHelper;
import musheor.utils.HighwayBuilderState;

public class EchestFarmer extends Module {
   private final Setting<Boolean> selfToggle = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("self-toggle")).description("Disables when you reach the desired amount of ender chests."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> amount2 = this.settings
      .getDefaultGroup()
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("amount"))
                     .description("The amount of ender chests to farm."))
                  .defaultValue(64))
               .sliderRange(1, 64)
               .visible(this.selfToggle::get))
            .build()
      );
   private final Setting<Boolean> superFarm20bps = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("super-farm-20bps"))
                  .description("Uses instant rebreak when mining enderchests to speed up the farming process up to 20 blocks per second."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> rebreakStallTimeout = this.settings
      .getDefaultGroup()
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("rebreak-stall-timeout"))
                     .description("Ticks without progress during instant rebreak before falling back to one normal break."))
                  .defaultValue(40))
               .min(5)
               .sliderRange(10, 200)
               .visible(this.superFarm20bps::get))
            .build()
      );
   public static BlockPos farmPos;
   private int farmedCount;
   private boolean initialized = false;
   private boolean justPlaced = false;
   private int swapCooldown;
   private int placeCooldown;
   private Block lastRebreakBlock;
   private int rebreakStallTicks;
   private int fallbackBreakTicks;
   public static EchestFarmer INSTANCE;

   public EchestFarmer() {
      super(Musheor.AUTOMATION, "obi-farmer", "An enderchest farmer that considers the player's inventory space when mining for obsidian, its also really fast");
      INSTANCE = this;
   }

   public void onDeactivate() {
      farmPos = null;
      ModuleControl.cancelBaritone();
      this.swapCooldown = 0;
   }

   public void onActivate() {
      farmPos = null;
      this.farmedCount = 0;
      this.initialized = false;
      this.justPlaced = false;
      this.placeCooldown = 0;
      this.lastRebreakBlock = null;
      this.rebreakStallTicks = 0;
      this.fallbackBreakTicks = 0;
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         HighwayBuilderState state = HighwayBuilderState.getInstance();
         if (!HighwayBuilder.isEating() && !HighwayBuilder.isKillAuraAttacking() && !SourceRemover.isBusy() && !BaritoneHelper.isPathing()) {
            if (this.swapCooldown > 0) {
               this.swapCooldown--;
            } else {
               if (this.placeCooldown > 0 && this.justPlaced) {
                  this.placeCooldown--;
               }

               if (!this.initialized) {
                  state.setEchestReturnPos(this.mc.player.getBlockPos());
                  if (((HighwayBuilder)Modules.get().get(HighwayBuilder.class)).isActive()) {
                     farmPos = HighwayUtils.getBehindPos(HighwayBuilder.getHighwayDirection());
                  } else {
                     farmPos = HighwayUtils.getBehindPos(HighwayUtils.directionFromYaw());
                  }

                  this.initialized = true;
               } else {
                  boolean isEchestHere = this.mc.world.getBlockState(farmPos).getBlock() == Blocks.ENDER_CHEST;
                  boolean isEmpty = this.mc.world.getBlockState(farmPos).isReplaceable();
                  boolean isObstructed = !isEchestHere && !isEmpty;
                  boolean notAligned = HighwayUtils.distanceXZ(state.getEchestReturnPos(), this.mc.player.getBlockPos()) > 1.25;
                  if (notAligned) {
                     BaritoneHelper.setGoalAndPath(new GoalBlock(state.getEchestReturnPos()));
                  } else if ((!(Boolean)this.selfToggle.get() || this.farmedCount < (Integer)this.amount2.get()) && InventoryManager.countPickaxes(false) != 0) {
                     FindItemResult echest = InvUtils.findInHotbar(new Item[]{Items.ENDER_CHEST});
                     if (!echest.found()) {
                        if (InventoryManager.countItem(Items.ENDER_CHEST) < 8) {
                           state.setEchestFarmBusy(false);
                           this.toggle();
                        } else {
                           InventoryManager.moveItemToHotbar(Items.ENDER_CHEST);
                        }
                     } else {
                        if (this.mc.currentScreen instanceof GenericContainerScreen) {
                           this.mc.currentScreen.close();
                        }

                        if (isObstructed && this.farmedCount < 2) {
                           if (KekMine.INSTANCE.isActive()) {
                              KekMine.INSTANCE.startBreak(farmPos, this.mc.world.getBlockState(farmPos), false);
                           } else {
                              BlockUtils.breakBlock(farmPos, true);
                           }
                        } else {
                           if ((Boolean)this.superFarm20bps.get()) {
                              if (this.farmedCount < 2) {
                                 if (isEmpty && (!this.justPlaced || this.placeCooldown == 0)) {
                                    if (!InventoryManager.ensureHolding(Items.ENDER_CHEST)) {
                                       this.swapCooldown = (Integer)MusheorSystem.Manager.swapDelay.get();
                                       return;
                                    }

                                    this.placeEchest();
                                    this.justPlaced = true;
                                    this.placeCooldown = 10;
                                 } else if (isEchestHere) {
                                    if (KekMine.INSTANCE.isActive()) {
                                       KekMine.INSTANCE.startBreak(farmPos, this.mc.world.getBlockState(farmPos), false);
                                    } else {
                                       InventoryManager.selectBestPickaxeForEchest(false);
                                       BlockUtils.breakBlock(farmPos, true);
                                    }

                                    this.justPlaced = false;
                                    this.placeCooldown = 0;
                                 }
                              } else {
                                 Block here = this.mc.world.getBlockState(farmPos).getBlock();
                                 if (here != this.lastRebreakBlock) {
                                    this.lastRebreakBlock = here;
                                    this.rebreakStallTicks = 0;
                                 } else if (!isEmpty && this.fallbackBreakTicks == 0) {
                                    this.rebreakStallTicks++;
                                 }

                                 if (this.fallbackBreakTicks > 0) {
                                    if (!isEmpty) {
                                       this.fallbackBreakTicks--;
                                       InventoryManager.selectBestPickaxeForEchest(false);
                                       if (KekMine.INSTANCE.isActive()) {
                                          KekMine.INSTANCE.startBreak(farmPos, this.mc.world.getBlockState(farmPos), false);
                                       } else {
                                          BlockUtils.breakBlock(farmPos, true);
                                       }

                                       return;
                                    }

                                    this.fallbackBreakTicks = 0;
                                 } else if (this.rebreakStallTicks >= (Integer)this.rebreakStallTimeout.get()) {
                                    MusheorSystem.debug("Rebreak stalled for %s ticks, falling back to a normal break", this.rebreakStallTicks);
                                    this.rebreakStallTicks = 0;
                                    this.fallbackBreakTicks = 40;
                                    return;
                                 }

                                 if (isEmpty && this.placeCooldown == 0) {
                                    this.placeEchest();
                                 }

                                 InventoryManager.selectBestPickaxeForEchest(false);
                                 KekMine.INSTANCE.sendAction(Action.STOP_DESTROY_BLOCK, farmPos);
                              }
                           } else {
                              if (isEchestHere) {
                                 InventoryManager.selectBestPickaxeForEchest(false);
                                 if (KekMine.INSTANCE.isActive()) {
                                    KekMine.INSTANCE.startBreak(farmPos, this.mc.world.getBlockState(farmPos), false);
                                 } else {
                                    BlockUtils.breakBlock(farmPos, true);
                                 }

                                 this.justPlaced = false;
                                 this.placeCooldown = 0;
                                 return;
                              }

                              if (isEmpty && (!this.justPlaced || this.placeCooldown == 0)) {
                                 if (!InventoryManager.ensureHolding(Items.ENDER_CHEST)) {
                                    this.swapCooldown = (Integer)MusheorSystem.Manager.swapDelay.get();
                                    return;
                                 }

                                 this.placeEchest();
                                 this.justPlaced = true;
                                 this.placeCooldown = 10;
                              }
                           }
                        }
                     }
                  } else {
                     MusheorSystem.debug("Mining last enderchest...");
                     if (!isEchestHere && !isObstructed) {
                        state.setEchestFarmBusy(false);
                        this.toggle();
                     } else {
                        InventoryManager.selectBestPickaxeForEchest(false);
                        if (KekMine.INSTANCE.isActive()) {
                           KekMine.INSTANCE.startBreak(farmPos, this.mc.world.getBlockState(farmPos), false);
                        } else {
                           BlockUtils.breakBlock(farmPos, true);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void placeEchest() {
      InventoryManager.swapToItemSilent(Items.ENDER_CHEST, (Boolean)MusheorSystem.Manager.silentPlace.get());
      if (HighwayBuilderState.getInstance().getPlacementType().equals(MusheorSystem.PlacementType.Rotate)) {
         HighwayUtils.lookAtBlockFace(farmPos, Direction.DOWN);
      }

      musheor.utils.BlockPlacer.place(farmPos, Direction.DOWN, HighwayBuilderState.getInstance().getPlacementType(), (Boolean)MusheorSystem.Manager.silentPlace.get());
      this.farmedCount++;
   }
}

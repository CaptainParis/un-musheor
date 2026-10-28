package musheor.utils;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import musheor.modules.automation.EchestFarmer;
import musheor.modules.automation.HighwayBuilder;
import musheor.modules.automation.KekBounce;
import musheor.modules.automation.KekNuker;
import musheor.modules.automation.SourceRemover;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class HighwayLogic {
   public static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int UNUSED_CONST_2 = 2;
   private static final int UNUSED_CONST_1 = 1;
   private static boolean bouncing = false;
   private static int bounceCooldown = 0;
   private static final int BOUNCE_COOLDOWN_TICKS = 20;
   private static final int LEG_END_MARGIN = 32;
   private static boolean recoveringFromFall = false;
   private static int fallDetectTicks = 0;
   private static int repathCooldown = 0;
   private static final int FALL_DETECT_THRESHOLD = 15;
   private static final int REPATH_INTERVAL_TICKS = 40;
   private static final int PAVEMENT_SEARCH_RANGE = 24;

   public static void runEchestFarm() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      ModuleControl.setModuleSetting(EchestFarmer.class, "self-toggle", true);
      state.setEchestReturnPos(mc.player.getBlockPos());
      if (!HighwayUtils.isOnHighway()) {
         ModuleControl.setAutoWalk(false);
         if (!musheor.utils.BaritoneHelper.isPathing()) {
            HighwayUtils.returnToLane();
         }
      } else {
         if (!state.isEchestFarmBusy()) {
            assert mc.player != null;

            int remaining = musheor.utils.manager.InventoryManager.countItem(Items.ENDER_CHEST) - 8;
            if (remaining > 0) {
               ModuleControl.setModuleSetting(EchestFarmer.class, "amount", Math.min(remaining, musheor.utils.manager.InventoryManager.countEmptySlots() * 8));
            }

            ModuleControl.setAutoWalk(false);
            state.setEchestFarmBusy(true);
            Module BetterEchestFarmer = Modules.get().get(EchestFarmer.class);
            if (!BetterEchestFarmer.isActive()) {
               state.setEchestFarmActive(true);
               BetterEchestFarmer.toggle();
            }
         }
      }
   }

   public static void finishEchestFarm() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      if (!HighwayBuilder.isEchestFarmerActive() && state.isEchestFarmActive()) {
         if (EchestFarmer.farmPos != null && mc.world.getBlockState(EchestFarmer.farmPos).getBlock() == Blocks.ENDER_CHEST) {
            MusheorSystem.debug("Attempting to break incorrect block...");
            BlockUtils.breakBlock(EchestFarmer.farmPos, true);
            return;
         }

         if (ModuleControl.isGatherItemActive()) {
            return;
         }

         ModuleControl.startGatherItem(Items.OBSIDIAN, false);
         state.setEchestFarmBusy(false);
         state.setEchestFarmActive(false);
      }
   }

   public static void tickPaveCardinal() {
      assert mc.player != null && mc.world != null;

      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      if (state.getDirection() != null && state.getRailX() != null && state.getRailY() != null && state.getRailZ() != null) {
         if (!tickFallRecovery(state)) {
            Module BetterEChestFarmer = Modules.get().get(EchestFarmer.class);
            Map<String, Boolean> conditions = new LinkedHashMap<>();
            conditions.put("BetterEchestFarmer", BetterEChestFarmer.isActive());
            conditions.put("Gathering Items", ModuleControl.isGatherItemActive());
            conditions.put("Removing Lava", SourceRemover.isBusy());
            if (!BetterEChestFarmer.isActive() && !ModuleControl.isGatherItemActive() && !SourceRemover.isBusy()) {
               if (KekNuker.isBreakingHighwayZone() || HighwayBuilder.isEating() || HighwayBuilder.isKillAuraAttacking() || musheor.utils.manager.InventoryManager.isBusy) {
                  stopBounce();
                  ModuleControl.setAutoWalk(false);
               }

               if (HighwayBuilder.isAdvancedSourceFiller()) {
                  HighwayUtils.tickLavaRemoval();
               }

               if (HighwayBuilder.shouldAutoBounce() && HighwayBuilder.getMode() == HighwayBuilder.Mode.SEMI) {
                  if (bounceCooldown > 0) {
                     bounceCooldown--;
                     return;
                  }

                  boolean interrupted = KekNuker.isBreakingHighwayZone()
                     || HighwayBuilder.isEating()
                     || HighwayBuilder.isKillAuraAttacking()
                     || musheor.utils.manager.InventoryManager.isBusy
                     || ModuleControl.isGatherItemActive()
                     || SourceRemover.isBusy()
                     || BetterEChestFarmer.isActive()
                     || musheor.utils.BaritoneHelper.isPathing();
                  if (!interrupted && isBouncePathClear(true, HighwayBuilder.getBounceDistanceCheck())) {
                     startBounce();
                  } else {
                     stopBounce();
                  }

                  if (bouncing) {
                     ModuleControl.faceHighwayDirection();
                     return;
                  }
               }

               if (HighwayBuilder.getMode() == HighwayBuilder.Mode.SEMI) {
                  ModuleControl.alignCardinalLane();
                  HighwayUtils.checkFrontCollision();
                  ModuleControl.faceHighwayDirectionWithPitch();
                  ModuleControl.strafeAlignCardinal();
               }

               BlockPos[] positions = HighwayPositions.getCardinalFloor(2, 3, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail());
               if ((Boolean)HighwayBuilder.INSTANCE.toggleKekNuker.get()) {
                  KekNuker.targetBlocks.clear();
               }

               state.setSuppressPlacement(false);
               HighwayUtils.breakBlockingEntities(positions);
               List<Block> blacklistedBlocks = KekNuker.getBlacklist();
               state.setHasMissingBlocks(false);
               BlockPos nonObsidianBlock = null;
               boolean foundBlockage = false;
               BlockPos blockage = null;
               if (HighwayBuilder.getMode() == HighwayBuilder.Mode.SEMI) {
                  for (BlockPos currentPos : HighwayPositions.getCardinalFloor(-3, 5, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail())) {
                     Block block = mc.world.getBlockState(currentPos).getBlock();
                     if ((HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
                        && block != HighwayBuilder.getPavementBlock()
                        && HighwayStats.getDistanceTravelled() > 10
                        && !blacklistedBlocks.contains(block)) {
                        state.setHasMissingBlocks(true);
                        nonObsidianBlock = currentPos;
                        break;
                     }
                  }

                  if (HighwayBuilder.isFillCeiling()) {
                     for (BlockPos currentPosx : HighwayPositions.getCardinalCeiling(-3, 4, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail())) {
                        Block block = mc.world.getBlockState(currentPosx).getBlock();
                        if ((HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
                           && block != Blocks.BEDROCK
                           && block == Blocks.AIR
                           && HighwayStats.getDistanceTravelled() > 10
                           && !blacklistedBlocks.contains(block)) {
                           state.setHasMissingBlocks(true);
                           nonObsidianBlock = currentPosx;
                           break;
                        }
                     }
                  }

                  for (BlockPos currentPosxx : HighwayPositions.getCardinalBehindTunnel()) {
                     Block block = mc.world.getBlockState(currentPosxx).getBlock();
                     if (!(block instanceof AirBlock) && HighwayStats.getDistanceTravelled() > 10 && !blacklistedBlocks.contains(block)) {
                        foundBlockage = true;
                        blockage = currentPosxx;
                        break;
                     }
                  }
               }

               if (foundBlockage) {
                  ModuleControl.setAutoWalk(false);
                  MusheorSystem.debug("Detected blockage behind player... %s", mc.world.getBlockState(blockage).getBlock().getName());

                  assert blockage != null;

                  Goal baritoneGoal = switch (HighwayBuilder.getHighwayDirection()) {
                     case NORTH -> new GoalBlock(state.getRailX(), state.getRailY(), blockage.getZ() + 2);
                     case EAST -> new GoalBlock(blockage.getX() - 2, state.getRailY(), state.getRailZ());
                     case SOUTH -> new GoalBlock(state.getRailX(), state.getRailY(), blockage.getZ() - 2);
                     case WEST -> new GoalBlock(blockage.getX() + 2, state.getRailY(), state.getRailZ());
                     default -> new GoalBlock(state.getRailX(), state.getRailY(), state.getRailZ());
                  };
                  musheor.utils.BaritoneHelper.setGoalAndPath(baritoneGoal);
               } else {
                  if (state.hasMissingBlocks()) {
                     ModuleControl.setAutoWalk(false);
                     MusheorSystem.debug("Found missing block, going back...");

                     assert nonObsidianBlock != null;
                     Goal baritoneGoal = switch (HighwayBuilder.getHighwayDirection()) {
                        case NORTH, SOUTH -> new GoalBlock(state.getRailX(), state.getRailY(), nonObsidianBlock.getZ());
                        case EAST, WEST -> new GoalBlock(nonObsidianBlock.getX(), state.getRailY(), state.getRailZ());
                        default -> new GoalBlock(nonObsidianBlock.getX(), state.getRailY(), nonObsidianBlock.getZ());
                     };
                     musheor.utils.BaritoneHelper.setGoalAndPath(baritoneGoal);
                  }

                  if ((Boolean)HighwayBuilder.INSTANCE.toggleKekNuker.get()) {
                     for (BlockPos pos : positions) {
                        Block block = mc.world.getBlockState(pos).getBlock();
                        if (block != Blocks.NETHER_PORTAL
                           && block != Blocks.BEDROCK
                           && (HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
                           && HighwayUtils.needsReplacement(pos, HighwayBuilder.getPavementBlock())) {
                           KekNuker.targetBlocks.add(pos.toImmutable());
                        }
                     }

                     for (BlockPos posx : HighwayPositions.getCardinalTunnel(3, 2)) {
                        Block block = mc.world.getBlockState(posx).getBlock();
                        if (block != Blocks.NETHER_PORTAL && block != Blocks.BEDROCK && HighwayUtils.needsReplacement(posx, Blocks.AIR)) {
                           KekNuker.targetBlocks.add(posx.toImmutable());
                        }
                     }
                  }

                  state.setPlacing(false);
                  HighwayUtils.placeAlongHighway(positions, false);
                  if (HighwayBuilder.isFillCeiling()) {
                     HighwayUtils.placeAlongHighway(HighwayPositions.getCardinalCeiling(2, 2, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail()), false);
                  }

                  if (!state.isPlacing() || !state.isInPlacementRange() || !state.hasMissingBlocks() || !KekNuker.isBreakingHighwayZone() || !state.isSuppressPlacement()) {
                     ModuleControl.setAutoWalk(true);
                  }
               }
            } else {
               ModuleControl.setAutoWalk(false);

               for (Entry<String, Boolean> entry : conditions.entrySet()) {
                  if (entry.getValue()) {
                     MusheorSystem.debug(entry.getKey() + " was triggered.");
                  }
               }
            }
         }
      }
   }

   public static void tickPaveDiagonal() {
      assert mc.player != null && mc.world != null;

      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      if (state.getDirection() != null && state.getRailX() != null && state.getRailY() != null && state.getRailZ() != null) {
         if (!tickFallRecovery(state)) {
            double dx = mc.player.getBlockX() - state.getOriginPos().getX();
            double dz = mc.player.getBlockZ() - state.getOriginPos().getZ();
            if (Math.abs(dx) == Math.abs(dz)) {
               state.setRailZ(Integer.valueOf(mc.player.getBlockZ()));
               state.setRailX(Integer.valueOf(mc.player.getBlockX()));
            }

            if (!HighwayUtils.isOnHighway()) {
               ModuleControl.setAutoWalk(false);
               if (!musheor.utils.BaritoneHelper.isPathing() && state.getLavaReturnPos() == null && state.getLavaTarget() == null) {
                  HighwayUtils.returnToLane();
               }
            }

            Module BetterEChestFarmer = Modules.get().get(EchestFarmer.class);
            if (!BetterEChestFarmer.isActive() && !ModuleControl.isGatherItemActive() && !SourceRemover.isBusy()) {
               if (KekNuker.isBreakingHighwayZone() || HighwayBuilder.isEating() || HighwayBuilder.isKillAuraAttacking() || musheor.utils.manager.InventoryManager.isBusy) {
                  stopBounce();
                  ModuleControl.setAutoWalk(false);
               }

               if (HighwayBuilder.isAdvancedSourceFiller()) {
                  HighwayUtils.tickLavaRemoval();
               }

               if (HighwayBuilder.shouldAutoBounce() && HighwayBuilder.getMode() == HighwayBuilder.Mode.SEMI) {
                  if (bounceCooldown > 0) {
                     bounceCooldown--;
                     return;
                  }

                  boolean interrupted = KekNuker.isBreakingHighwayZone()
                     || HighwayBuilder.isEating()
                     || HighwayBuilder.isKillAuraAttacking()
                     || musheor.utils.manager.InventoryManager.isBusy
                     || ModuleControl.isGatherItemActive()
                     || SourceRemover.isBusy()
                     || BetterEChestFarmer.isActive()
                     || musheor.utils.BaritoneHelper.isPathing();
                  if (!interrupted && isBouncePathClear(false, HighwayBuilder.getBounceDistanceCheck())) {
                     startBounce();
                  } else {
                     stopBounce();
                  }

                  if (bouncing) {
                     ModuleControl.faceHighwayDirection();
                     return;
                  }
               }

               if (HighwayBuilder.getMode() == HighwayBuilder.Mode.SEMI) {
                  ModuleControl.alignDiagonalAxis();
                  HighwayUtils.checkFrontCollision();
                  ModuleControl.faceHighwayDirectionWithPitch();
               }

               List<Block> blacklistedBlocks = KekNuker.getBlacklist();
               boolean foundBlockage = false;
               BlockPos blockage = null;
               if (HighwayBuilder.getMode() == HighwayBuilder.Mode.SEMI) {
                  for (BlockPos currentPos : HighwayPositions.getDiagonalTunnel(-2, 5)) {
                     Block block = mc.world.getBlockState(currentPos).getBlock();
                     if (!(block instanceof AirBlock) && HighwayStats.getDistanceTravelled() > 10 && !blacklistedBlocks.contains(block)) {
                        foundBlockage = true;
                        blockage = currentPos;
                        break;
                     }
                  }

                  if (foundBlockage) {
                     ModuleControl.setAutoWalk(false);
                     MusheorSystem.debug(
                        "Detected blockage behind player... " + Registries.BLOCK.getId(mc.world.getBlockState(blockage).getBlock()).getPath()
                     );

                     assert blockage != null;

                     int distance = 4;

                     Goal baritoneGoal = switch (HighwayBuilder.getHighwayDirection()) {
                        case NORTHEAST -> new GoalBlock(state.getRailX() - distance, state.getRailY(), state.getRailZ() + distance);
                        case NORTHWEST -> new GoalBlock(state.getRailX() + distance, state.getRailY(), state.getRailZ() + distance);
                        case SOUTHEAST -> new GoalBlock(state.getRailX() - distance, state.getRailY(), state.getRailZ() - distance);
                        case SOUTHWEST -> new GoalBlock(state.getRailX() + distance, state.getRailY(), state.getRailZ() - distance);
                        default -> new GoalBlock(blockage.getX(), state.getRailY(), blockage.getZ());
                     };
                     musheor.utils.BaritoneHelper.setGoalAndPath(baritoneGoal);
                     return;
                  }

                  state.setHasMissingBlocks(false);
                  BlockPos nonObsidianBlock = null;

                  for (BlockPos currentPosx : HighwayPositions.getDiagonalFloor(-3, 4, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail())) {
                     Block block = mc.world.getBlockState(currentPosx).getBlock();
                     if ((HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
                        && block != HighwayBuilder.getPavementBlock()
                        && HighwayStats.getDistanceTravelled() > 10
                        && (block != Blocks.CRYING_OBSIDIAN || HighwayBuilder.isReplaceCryingObsidian())
                        && block != Blocks.BEDROCK
                        && !blacklistedBlocks.contains(block)) {
                        state.setHasMissingBlocks(true);
                        nonObsidianBlock = currentPosx;
                        break;
                     }
                  }

                  if (HighwayBuilder.isFillCeiling()) {
                     for (BlockPos currentPosxx : HighwayPositions.getDiagonalCeiling(-3, 4, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail())) {
                        Block block = mc.world.getBlockState(currentPosxx).getBlock();
                        if ((HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
                           && block == Blocks.AIR
                           && HighwayStats.getDistanceTravelled() > 10
                           && !blacklistedBlocks.contains(block)) {
                           state.setHasMissingBlocks(true);
                           nonObsidianBlock = currentPosxx;
                           break;
                        }
                     }
                  }

                  if (state.hasMissingBlocks()) {
                     ModuleControl.setAutoWalk(false);
                     MusheorSystem.debug("Found missing block, going back...");

                     assert nonObsidianBlock != null;

                     RenderUtils.renderTickingBlock(nonObsidianBlock, Color.WHITE, Color.WHITE, ShapeMode.Lines, 0, 20, true, false);

                     Goal baritoneGoal = switch (HighwayBuilder.getHighwayDirection()) {
                        case NORTHEAST -> new GoalBlock(state.getRailX() - 8, state.getRailY(), state.getRailZ() + 8);
                        case NORTHWEST -> new GoalBlock(state.getRailX() + 8, state.getRailY(), state.getRailZ() + 8);
                        case SOUTHEAST -> new GoalBlock(state.getRailX() - 8, state.getRailY(), state.getRailZ() - 8);
                        case SOUTHWEST -> new GoalBlock(state.getRailX() + 8, state.getRailY(), state.getRailZ() - 8);
                        default -> new GoalBlock(nonObsidianBlock.getX(), state.getRailY(), nonObsidianBlock.getZ());
                     };
                     musheor.utils.BaritoneHelper.setGoalAndPath(baritoneGoal);
                     return;
                  }
               }

               BlockPos[] positions = HighwayPositions.getDiagonalFloor(2, 2, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail());
               if ((Boolean)HighwayBuilder.INSTANCE.toggleKekNuker.get()) {
                  KekNuker.targetBlocks.clear();
               }

               state.setSuppressPlacement(false);
               HighwayUtils.breakBlockingEntities(positions);
               if ((Boolean)HighwayBuilder.INSTANCE.toggleKekNuker.get()) {
                  for (BlockPos pos : positions) {
                     Block block = mc.world.getBlockState(pos).getBlock();
                     if (block != Blocks.NETHER_PORTAL
                        && block != Blocks.BEDROCK
                        && (HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
                        && HighwayUtils.needsReplacement(pos, HighwayBuilder.getPavementBlock())) {
                        KekNuker.targetBlocks.add(pos.toImmutable());
                     }
                  }

                  for (BlockPos posx : HighwayPositions.getDiagonalTunnel(2, 1)) {
                     Block block = mc.world.getBlockState(posx).getBlock();
                     if (block != Blocks.NETHER_PORTAL && block != Blocks.BEDROCK && HighwayUtils.needsReplacement(posx, Blocks.AIR)) {
                        KekNuker.targetBlocks.add(posx.toImmutable());
                     }
                  }
               }

               state.setPlacing(false);
               HighwayUtils.placeAlongHighway(positions, false);
               BlockPos[] ceilingPositions = HighwayPositions.getDiagonalCeiling(2, 1, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail());
               if (HighwayBuilder.isFillCeiling()) {
                  for (BlockPos posxx : ceilingPositions) {
                     if (HighwayUtils.isInPlacementRange(posxx) && BlockUtils.canPlace(posxx, true)) {
                        ModuleControl.setAutoWalk(false);
                     }
                  }

                  HighwayUtils.placeAlongHighway(ceilingPositions, false);
               }

               if (!state.isPlacing() || !state.isInPlacementRange() || !state.hasMissingBlocks() || !KekNuker.isBreakingHighwayZone() || !state.isSuppressPlacement()) {
                  ModuleControl.setAutoWalk(true);
               }
            } else {
               stopBounce();
               ModuleControl.setAutoWalk(false);
            }
         }
      }
   }

   public static void tickDigCardinal() {
      if (mc.player != null && mc.world != null) {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         if (!ModuleControl.isGatherItemActive()
            && !HighwayBuilder.isEating()
            && !musheor.utils.manager.InventoryManager.isBusy
            && !HighwayBuilder.isKillAuraAttacking()
            && (!SourceRemover.isBusy() || !Modules.get().get("source-remover").isActive())) {
            if (mc.player.getY() < state.getRailY().intValue()) {
               ModuleControl.setAutoWalk(false);
            }

            if (HighwayBuilder.isAdvancedSourceFiller()) {
               HighwayUtils.tickLavaRemoval();
            }

            ModuleControl.faceHighwayDirectionWithPitch();
            ModuleControl.strafeAlignCardinal();
            HighwayUtils.checkFrontCollision();
            if (HighwayBuilder.getScaffoldMode() != HighwayBuilder.ScaffoldMode.NORMAL) {
               if (musheor.utils.manager.InventoryManager.findStack(HighwayBuilder.getScaffoldBlock().asItem()) == null) {
                  state.setDigReturnPos(mc.player.getBlockPos());
                  HighwayUtils.goToNearestItem(HighwayBuilder.getScaffoldBlock().asItem());
                  return;
               }

               if (state.getDigReturnPos() != null && musheor.utils.manager.InventoryManager.findStack(HighwayBuilder.getScaffoldBlock().asItem()) != null) {
                  musheor.utils.BaritoneHelper.goTo(state.getDigReturnPos());
               }

               state.setDigReturnPos(null);
               if (musheor.utils.BaritoneHelper.isPathing()) {
                  return;
               }
            }

            if (HighwayBuilder.getScaffoldMode() == HighwayBuilder.ScaffoldMode.NONE) {
               HighwayUtils.placeAlongHighway(HighwayPositions.getCardinalFloor(3, 2, HighwayBuilder.isScaffoldLeftRail(), HighwayBuilder.isScaffoldRightRail()), true);
            }

            if (HighwayBuilder.getScaffoldMode() == HighwayBuilder.ScaffoldMode.FAST) {
               HighwayUtils.fastScaffold();
            }

            for (BlockPos currentPos : HighwayPositions.getCardinalTunnel(3, 1)) {
               BlockState blockState = mc.world.getBlockState(currentPos);
               if (!(mc.world.getBlockState(currentPos).getBlock() instanceof AirBlock)
                  && blockState.getBlock() != Blocks.NETHER_PORTAL
                  && blockState.getBlock() != Blocks.BEDROCK
                  && HighwayUtils.needsReplacement(currentPos, Blocks.AIR)) {
                  KekNuker.targetBlocks.add(currentPos.toImmutable());
               }
            }

            for (BlockPos currentPosx : HighwayPositions.getCardinalBehindTunnel()) {
               if (!(mc.world.getBlockState(currentPosx).getBlock() instanceof AirBlock) && HighwayStats.cachedDistanceTravelled > 5) {
                  boolean hasLavaNeighbor = false;

                  for (Direction direction : Direction.values()) {
                     BlockPos neighborPos = currentPosx.offset(direction);
                     FluidState neighborFluid = mc.world.getFluidState(neighborPos);
                     if (neighborFluid.getFluid() == Fluids.LAVA) {
                        hasLavaNeighbor = true;
                        break;
                     }
                  }

                  if (!hasLavaNeighbor) {
                     ModuleControl.setAutoWalk(false);

                     GoalXZ goal = switch (HighwayBuilder.getHighwayDirection()) {
                        case NORTH, SOUTH -> new GoalXZ(state.getRailX(), currentPosx.getZ());
                        case EAST, WEST -> new GoalXZ(currentPosx.getX(), state.getRailZ());
                        default -> new GoalXZ(currentPosx.getX(), currentPosx.getZ());
                     };
                     musheor.utils.BaritoneHelper.setGoalAndPath(goal);
                     return;
                  }
               } else {
                  ModuleControl.setAutoWalk(true);
               }
            }
         } else {
            ModuleControl.setAutoWalk(false);
         }
      }
   }

   public static void tickDigDiagonal() {
      if (mc.player != null && mc.world != null) {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         if (state.getOriginPos() == null) {
            ChatUtils.error("Set player center using .center", new Object[0]);
            ModuleControl.toggleHighwayBuilder();
         } else {
            double dx = mc.player.getBlockX() - state.getOriginPos().getX();
            double dz = mc.player.getBlockZ() - state.getOriginPos().getZ();
            if (Math.abs(dx) == Math.abs(dz)) {
               state.setRailX(Integer.valueOf(mc.player.getBlockX()));
               state.setRailZ(Integer.valueOf(mc.player.getBlockZ()));
            }

            List<Block> nukerBlacklist = KekNuker.getBlacklist();
            Map<String, Boolean> conditions = new LinkedHashMap<>();
            conditions.put("Gathering Items", ModuleControl.isGatherItemActive());
            conditions.put("Removing Lava", SourceRemover.isBusy());
            conditions.put("Eating", HighwayBuilder.isEating());
            if (!HighwayBuilder.isEating() && !ModuleControl.isGatherItemActive() && !SourceRemover.isBusy() && !HighwayBuilder.isKillAuraAttacking()) {
               if (HighwayBuilder.isAdvancedSourceFiller()) {
                  HighwayUtils.tickLavaRemoval();
               }

               ModuleControl.faceHighwayDirectionWithPitch();
               ModuleControl.alignDiagonalAxis();
               HighwayUtils.checkFrontCollision();
               if (HighwayBuilder.getScaffoldMode() == HighwayBuilder.ScaffoldMode.NONE) {
                  if (musheor.utils.manager.InventoryManager.findStack(Items.NETHERRACK) == null) {
                     HighwayUtils.goToNearestItem(Items.NETHERRACK);
                  }

                  HighwayUtils.placeAlongHighway(HighwayPositions.getDiagonalFloor(2, 1, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail()), true);
               }

               if (HighwayBuilder.getScaffoldMode() == HighwayBuilder.ScaffoldMode.FAST) {
                  if (musheor.utils.manager.InventoryManager.findStack(Items.NETHERRACK) == null) {
                     HighwayUtils.goToNearestItem(Items.NETHERRACK);
                  }

                  HighwayUtils.fastScaffold();
               }

               if (!KekNuker.isBreakingHighwayZone()) {
                  if (HighwayUtils.placeSafetyBlock()) {
                     MusheorSystem.debug("Placing block so player doesn't fall...");
                     ModuleControl.setAutoWalk(false);
                  } else {
                     for (BlockPos currentPos : HighwayPositions.getDiagonalTunnel(3, 1)) {
                        BlockState blockState = mc.world.getBlockState(currentPos);
                        if (!(mc.world.getBlockState(currentPos).getBlock() instanceof AirBlock)
                           && blockState.getBlock() != Blocks.NETHER_PORTAL
                           && blockState.getBlock() != Blocks.BEDROCK
                           && HighwayUtils.needsReplacement(currentPos, Blocks.AIR)) {
                           KekNuker.targetBlocks.add(currentPos.toImmutable());
                        }
                     }

                     for (BlockPos currentPosx : HighwayPositions.getDiagonalTunnel(-2, 5)) {
                        if (!(mc.world.getBlockState(currentPosx).getBlock() instanceof AirBlock) && HighwayStats.cachedDistanceTravelled > 5) {
                           boolean hasLavaNeighbor = false;

                           for (Direction direction : Direction.values()) {
                              BlockPos neighborPos = currentPosx.offset(direction);
                              FluidState neighborFluid = mc.world.getFluidState(neighborPos);
                              if (neighborFluid.getFluid() == Fluids.LAVA) {
                                 hasLavaNeighbor = true;
                                 break;
                              }
                           }

                           if (!hasLavaNeighbor && !nukerBlacklist.contains(mc.world.getBlockState(currentPosx).getBlock())) {
                              MusheorSystem.debug(
                                 "Detected obstruction behind player... "
                                    + Registries.BLOCK.getId(mc.world.getBlockState(currentPosx).getBlock()).getPath()
                                    + "at %s %s %s",
                                 currentPosx.getX(),
                                 currentPosx.getY(),
                                 currentPosx.getZ()
                              );
                              ModuleControl.setAutoWalk(false);
                              musheor.utils.BaritoneHelper.setGoalAndPath(new GoalXZ(currentPosx.getX(), currentPosx.getX()));
                              return;
                           }
                        } else {
                           ModuleControl.setAutoWalk(true);
                        }
                     }
                  }
               }
            } else {
               ModuleControl.setAutoWalk(false);

               for (Entry<String, Boolean> entry : conditions.entrySet()) {
                  if (entry.getValue()) {
                     MusheorSystem.debug(entry.getKey() + " was triggered.");
                  }
               }
            }
         }
      }
   }

   private static boolean isPastLegCorner(musheor.utils.HighwayDetector.DetectedHighway d, int px, int pz) {
      double D = Math.abs(d.axisValue);
      double K = d.axisValue;
      switch (d.direction) {
         case NORTH:
            return pz <= -D + 2.0;
         case EAST:
            return px >= D - 2.0;
         case SOUTH:
            return pz >= D - 2.0;
         case WEST:
            return px <= -D + 2.0;
         case NORTHEAST:
            return K > 0.0 ? pz <= 2 : px >= -2;
         case NORTHWEST:
            return K > 0.0 ? px <= 2 : pz <= 2;
         case SOUTHEAST:
            return K > 0.0 ? pz >= -2 : px >= -2;
         case SOUTHWEST:
            return K > 0.0 ? px <= 2 : pz >= -2;
         default:
            return false;
      }
   }

   private static int[] getLegCorner(musheor.utils.HighwayDetector.DetectedHighway d) {
      int D = (int)Math.abs(d.axisValue);
      int K = (int)d.axisValue;
      int ax = (int)d.axisValue;
      switch (d.direction) {
         case NORTH:
            return new int[]{ax, -D};
         case EAST:
            return new int[]{D, ax};
         case SOUTH:
            return new int[]{ax, D};
         case WEST:
            return new int[]{-D, ax};
         case NORTHEAST:
            return K > 0 ? new int[]{K, 0} : new int[]{0, K};
         case NORTHWEST:
            return K > 0 ? new int[]{0, -K} : new int[]{K, 0};
         case SOUTHEAST:
            return K > 0 ? new int[]{K, 0} : new int[]{0, -K};
         case SOUTHWEST:
            return K > 0 ? new int[]{0, K} : new int[]{K, 0};
         default:
            return null;
      }
   }

   private static HighwayUtils.HighwayDirection nextLegDirection(musheor.utils.HighwayDetector.DetectedHighway d) {
      boolean pos = d.axisValue > 0.0;
      switch (d.direction) {
         case NORTH:
         case SOUTH:
            return pos ? HighwayUtils.HighwayDirection.WEST : HighwayUtils.HighwayDirection.EAST;
         case EAST:
         case WEST:
            return pos ? HighwayUtils.HighwayDirection.NORTH : HighwayUtils.HighwayDirection.SOUTH;
         case NORTHEAST:
         case SOUTHWEST:
            return pos ? HighwayUtils.HighwayDirection.NORTHWEST : HighwayUtils.HighwayDirection.SOUTHEAST;
         case NORTHWEST:
         case SOUTHEAST:
            return pos ? HighwayUtils.HighwayDirection.SOUTHWEST : HighwayUtils.HighwayDirection.NORTHEAST;
         default:
            return null;
      }
   }

   private static boolean handleLegTransition(musheor.utils.HighwayDetector.DetectedHighway detected, musheor.utils.HighwayBuilderState state) {
      assert mc.player != null;

      int px = mc.player.getBlockX();
      int pz = mc.player.getBlockZ();
      if (state.getRailY() == null) {
         return false;
      } else {
         int py = state.getRailY();
         if (!isPastLegCorner(detected, px, pz)) {
            return false;
         } else {
            int[] corner = getLegCorner(detected);
            if (corner == null) {
               return false;
            } else {
               int cx = corner[0];
               int cz = corner[1];
               double dist = Math.hypot(px - cx, pz - cz);
               if (dist > 1.0) {
                  ModuleControl.setAutoWalk(false);
                  musheor.utils.BaritoneHelper.setGoalAndPath(new GoalBlock(cx, py, cz));
                  return true;
               } else {
                  HighwayUtils.HighwayDirection nextDir = nextLegDirection(detected);
                  if (nextDir == null) {
                     return false;
                  } else {
                     musheor.utils.HighwayDetector.DetectedHighway newDetected = musheor.utils.HighwayDetector.detect(
                        mc.player.getBlockX(), mc.player.getBlockZ(), py, nextDir
                     );
                     if (newDetected == null) {
                        return false;
                     } else {
                        state.setDetectedHighway(newDetected);
                        state.setDirection(newDetected.direction);
                        state.setStartX(newDetected.startX);
                        state.setStartZ(newDetected.startZ);
                        state.setAlignX(newDetected.alignX);
                        state.setAlignZ(newDetected.alignZ);
                        state.setRailX(Integer.valueOf(mc.player.getBlockX()));
                        state.setRailZ(Integer.valueOf(mc.player.getBlockZ()));
                        ModuleControl.faceHighwayDirectionWithPitch();
                        return true;
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean isBouncing() {
      return bouncing;
   }

   private static boolean isBouncePathClear(boolean isCardinal, int distance) {
      if (mc.world == null) {
         return false;
      } else {
         List<Block> blacklist = KekNuker.getBlacklist();
         BlockPos[] floor = isCardinal
            ? HighwayPositions.getCardinalFloor(distance, 1, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail())
            : HighwayPositions.getDiagonalFloor(distance, 1, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail());

         for (BlockPos pos : floor) {
            Block block = mc.world.getBlockState(pos).getBlock();
            if (!blacklist.contains(block)
               && (HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
               && block != Blocks.BEDROCK
               && block != HighwayBuilder.getPavementBlock()) {
               return false;
            }
         }

         BlockPos[] tunnel = isCardinal ? HighwayPositions.getCardinalTunnel(distance, 1) : HighwayPositions.getDiagonalTunnel(distance, 1);

         for (BlockPos posx : tunnel) {
            Block block = mc.world.getBlockState(posx).getBlock();
            if (!blacklist.contains(block) && block != Blocks.BEDROCK && block != Blocks.NETHER_PORTAL && !mc.world.getBlockState(posx).isAir()) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isNearLegEnd(musheor.utils.HighwayDetector.DetectedHighway d) {
      if (mc.player == null) {
         return false;
      } else {
         int px = mc.player.getBlockX();
         int pz = mc.player.getBlockZ();
         double D = Math.abs(d.axisValue);
         double K = d.axisValue;
         int m = LEG_END_MARGIN;

         return switch (d.direction) {
            case NORTH -> pz <= -D + m;
            case EAST -> px >= D - m;
            case SOUTH -> pz >= D - m;
            case WEST -> px <= -D + m;
            case NORTHEAST -> K > 0.0 ? pz <= m : px >= -m;
            case NORTHWEST -> K > 0.0 ? px <= m : pz <= m;
            case SOUTHEAST -> K > 0.0 ? pz >= -m : px >= -m;
            case SOUTHWEST -> K > 0.0 ? px <= m : pz >= -m;
            default -> false;
         };
      }
   }

   private static void configureBounce() {
      ModuleControl.setModuleSetting(KekBounce.class, "mode", KekBounce.Mode.SIMPLE);
      ModuleControl.setModuleSetting(KekBounce.class, "y-motion", false);
      if (mc.player != null) {
         ModuleControl.setModuleSetting(KekBounce.class, "y-level", mc.player.getBlockY());
      }
   }

   private static void startBounce() {
      if (!bouncing) {
         if (mc.player != null) {
            boolean hasElytra = false;

            for (int i = 0; i < mc.player.getInventory().size(); i++) {
               if (mc.player.getInventory().getStack(i).getItem() == Items.ELYTRA) {
                  hasElytra = true;
                  break;
               }
            }

            if (hasElytra) {
               configureBounce();
               Module kekBounce = Modules.get().get(KekBounce.class);
               if (!kekBounce.isActive()) {
                  kekBounce.toggle();
               }

               bouncing = true;
               bounceCooldown = 0;
            }
         }
      }
   }

   public static void stopBounce() {
      if (bouncing) {
         Module kekBounce = Modules.get().get(KekBounce.class);
         if (kekBounce.isActive()) {
            kekBounce.toggle();
         }

         bouncing = false;
         bounceCooldown = BOUNCE_COOLDOWN_TICKS;
      }
   }

   public static void resetBounceState() {
      bouncing = false;
      bounceCooldown = 0;
      recoveringFromFall = false;
      fallDetectTicks = 0;
      repathCooldown = 0;
   }

   public static boolean tickFallRecovery(musheor.utils.HighwayBuilderState state) {
      if (mc.player == null || mc.world == null) {
         return false;
      } else if (state.getRailY() != null && state.getDirection() != null) {
         Module echestFarmer = Modules.get().get(EchestFarmer.class);
         if (!echestFarmer.isActive()
            && !ModuleControl.isGatherItemActive()
            && !SourceRemover.isBusy()
            && !musheor.utils.manager.InventoryManager.isBusy
            && !state.needsRestock()
            && !state.isRestockingShulkerFromEchest()
            && !state.isRestockingFromInventory()
            && !state.isPostRestocking()) {
            if ((bouncing || bounceCooldown > 0) && !recoveringFromFall) {
               fallDetectTicks = 0;
               return false;
            } else {
               int targetY = state.getRailY();
               boolean atCorrectY = mc.player.getBlockY() == targetY;
               if (!recoveringFromFall) {
                  if (atCorrectY) {
                     fallDetectTicks = 0;
                     return false;
                  }

                  if (++fallDetectTicks < FALL_DETECT_THRESHOLD) {
                     return false;
                  }

                  recoveringFromFall = true;
                  repathCooldown = 0;
                  stopBounce();
                  ModuleControl.setAutoWalk(false);
                  MusheorSystem.debug("Fell off the highway (Y %s != %s) — pathing back...", mc.player.getBlockY(), targetY);
               }

               if (atCorrectY && mc.player.isOnGround()) {
                  recoveringFromFall = false;
                  fallDetectTicks = 0;
                  if (musheor.utils.BaritoneHelper.isPathing()) {
                     musheor.utils.BaritoneHelper.cancel();
                  }

                  MusheorSystem.debug("Back on the highway — resuming paving.");
                  return false;
               } else {
                  if (repathCooldown <= 0) {
                     pathToPavement(state, targetY);
                     repathCooldown = REPATH_INTERVAL_TICKS;
                  } else {
                     repathCooldown--;
                  }

                  return true;
               }
            }
         } else {
            fallDetectTicks = 0;
            recoveringFromFall = false;
            return false;
         }
      } else {
         return false;
      }
   }

   private static void pathToPavement(musheor.utils.HighwayBuilderState state, int targetY) {
      BlockPos nearest = findNearestPavementStand(targetY, PAVEMENT_SEARCH_RANGE);
      if (nearest != null) {
         musheor.utils.BaritoneHelper.setGoalAndPath(new GoalBlock(nearest));
      } else {
         if (state.getRailX() != null && state.getRailZ() != null) {
            musheor.utils.BaritoneHelper.setGoalAndPath(new GoalBlock(state.getRailX(), targetY, state.getRailZ()));
         }
      }
   }

   private static BlockPos findNearestPavementStand(int targetY, int range) {
      if (mc.player != null && mc.world != null) {
         BlockPos p = mc.player.getBlockPos();
         Block pavement = HighwayBuilder.getPavementBlock();
         BlockPos best = null;
         double bestDistSq = Double.MAX_VALUE;

         for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
               BlockPos stand = new BlockPos(p.getX() + x, targetY, p.getZ() + z);
               if (mc.world.getBlockState(stand.down()).getBlock() == pavement
                  && mc.world.getBlockState(stand).isAir()
                  && mc.world.getBlockState(stand.up()).isAir()) {
                  double distSq = p.getSquaredDistance(stand);
                  if (distSq < bestDistSq) {
                     bestDistSq = distSq;
                     best = stand;
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   public static void tickPaveAuto() {
      assert mc.player != null && mc.world != null;

      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      if (state.getRailY() != null && state.getDirection() != null) {
         musheor.utils.HighwayDetector.DetectedHighway detected = state.getDetectedHighway();
         if (detected != null) {
            if (!tickFallRecovery(state)) {
               boolean isCardinal = detected.type == HighwayBuilder.HighwayType.CARDINAL;
               boolean isRingOrDiamond = detected.category == musheor.utils.HighwayDetector.Category.RING
                  || detected.category == musheor.utils.HighwayDetector.Category.DIAMOND;
               if (isRingOrDiamond && handleLegTransition(detected, state)) {
                  stopBounce();
               }

               Module betterEChestFarmer = Modules.get().get(EchestFarmer.class);
               if (!betterEChestFarmer.isActive() && !ModuleControl.isGatherItemActive() && !SourceRemover.isBusy()) {
                  if (KekNuker.isBreakingHighwayZone() || HighwayBuilder.isEating() || HighwayBuilder.isKillAuraAttacking() || musheor.utils.manager.InventoryManager.isBusy) {
                     stopBounce();
                     ModuleControl.setAutoWalk(false);
                  }

                  if (HighwayBuilder.isAdvancedSourceFiller()) {
                     HighwayUtils.tickLavaRemoval();
                  }

                  if (HighwayBuilder.shouldAutoBounce()) {
                     if (bounceCooldown > 0) {
                        bounceCooldown--;
                        return;
                     }

                     boolean nearLegEnd = isRingOrDiamond && isNearLegEnd(detected);
                     boolean interrupted = KekNuker.isBreakingHighwayZone()
                        || HighwayBuilder.isEating()
                        || HighwayBuilder.isKillAuraAttacking()
                        || musheor.utils.manager.InventoryManager.isBusy
                        || ModuleControl.isGatherItemActive()
                        || SourceRemover.isBusy()
                        || betterEChestFarmer.isActive()
                        || musheor.utils.BaritoneHelper.isPathing();
                     if (!nearLegEnd && !interrupted && isBouncePathClear(isCardinal, HighwayBuilder.getBounceDistanceCheck())) {
                        startBounce();
                     } else {
                        stopBounce();
                     }

                     if (bouncing) {
                        ModuleControl.faceHighwayDirection();
                        ModuleControl.setAutoWalk(false);
                        return;
                     }
                  }

                  if (isCardinal) {
                     HighwayUtils.checkFrontCollision();
                     ModuleControl.faceHighwayDirectionWithPitch();
                     ModuleControl.strafeAlignCardinal();
                  } else {
                     boolean isNESW = state.getDirection() == HighwayUtils.HighwayDirection.NORTHEAST || state.getDirection() == HighwayUtils.HighwayDirection.SOUTHWEST;
                     int playerDiagVal = isNESW
                        ? mc.player.getBlockX() + mc.player.getBlockZ()
                        : mc.player.getBlockX() - mc.player.getBlockZ();
                     if (playerDiagVal == (int)detected.axisValue) {
                        state.setRailX(Integer.valueOf(mc.player.getBlockX()));
                        state.setRailZ(Integer.valueOf(mc.player.getBlockZ()));
                     }

                     ModuleControl.alignDiagonalAxis();
                     HighwayUtils.checkFrontCollision();
                     ModuleControl.faceHighwayDirectionWithPitch();
                  }

                  BlockPos[] allPositions = isCardinal
                     ? HighwayPositions.getCardinalFloor(
                        (Integer)MusheorSystem.Manager.highwayForwardRange.get(), 2, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail()
                     )
                     : HighwayPositions.getDiagonalFloor((Integer)MusheorSystem.Manager.highwayForwardRange.get(), 2, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail());
                  List<BlockPos> placePositions = new ArrayList<>();
                  List<BlockPos> clearPositions = new ArrayList<>();

                  for (BlockPos pos : allPositions) {
                     if (pos.getY() != state.getRailY() || !musheor.utils.HighwayDetector.isCrossingHighway(pos, detected)) {
                        placePositions.add(pos);
                     } else if (musheor.utils.HighwayDetector.isOnDetectedHighway(pos, detected)) {
                        clearPositions.add(pos);
                     }
                  }

                  if ((Boolean)HighwayBuilder.INSTANCE.toggleKekNuker.get()) {
                     KekNuker.targetBlocks.clear();
                  }

                  state.setSuppressPlacement(false);
                  HighwayUtils.breakBlockingEntities(allPositions);
                  if ((Boolean)HighwayBuilder.INSTANCE.toggleKekNuker.get()) {
                     for (BlockPos posx : placePositions) {
                        Block block = mc.world.getBlockState(posx).getBlock();
                        if (block != Blocks.NETHER_PORTAL
                           && block != Blocks.BEDROCK
                           && (HighwayBuilder.isReplaceCryingObsidian() || block != Blocks.CRYING_OBSIDIAN)
                           && HighwayUtils.needsReplacement(posx, HighwayBuilder.getPavementBlock())) {
                           KekNuker.targetBlocks.add(posx.toImmutable());
                        }
                     }

                     BlockPos[] tunnelClear = isCardinal
                        ? HighwayPositions.getCardinalTunnel((Integer)MusheorSystem.Manager.highwayForwardRange.get(), 1)
                        : HighwayPositions.getDiagonalTunnel((Integer)MusheorSystem.Manager.highwayForwardRange.get(), 1);

                     for (BlockPos posxx : tunnelClear) {
                        Block block = mc.world.getBlockState(posxx).getBlock();
                        if (block != Blocks.NETHER_PORTAL && block != Blocks.BEDROCK && HighwayUtils.needsReplacement(posxx, Blocks.AIR)) {
                           KekNuker.targetBlocks.add(posxx.toImmutable());
                        }
                     }

                     for (BlockPos posxxx : clearPositions) {
                        Block block = mc.world.getBlockState(posxxx).getBlock();
                        if (block != Blocks.NETHER_PORTAL && block != Blocks.BEDROCK && HighwayUtils.needsReplacement(posxxx, Blocks.AIR)) {
                           KekNuker.targetBlocks.add(posxxx.toImmutable());
                        }
                     }
                  }

                  state.setPlacing(false);
                  HighwayUtils.placeAlongHighway(placePositions.toArray(new BlockPos[0]), false);
                  if (HighwayBuilder.isFillCeiling()) {
                     BlockPos[] ceilingPositions = isCardinal
                        ? HighwayPositions.getCardinalCeiling(
                           (Integer)MusheorSystem.Manager.highwayForwardRange.get(), 2, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail()
                        )
                        : HighwayPositions.getDiagonalCeiling(
                           (Integer)MusheorSystem.Manager.highwayForwardRange.get(), 1, HighwayBuilder.isPlaceLeftRail(), HighwayBuilder.isPlaceRightRail()
                        );
                     HighwayUtils.placeAlongHighway(ceilingPositions, false);
                  }

                  if (!state.isPlacing()
                     || !state.isInPlacementRange()
                     || !HighwayUtils.hasPlaceableInZone()
                     || !state.hasMissingBlocks()
                     || !KekNuker.isBreakingHighwayZone()
                     || !state.isPlacing()) {
                     ModuleControl.setAutoWalk(true);
                  }
               } else {
                  stopBounce();
                  ModuleControl.setAutoWalk(false);
               }
            }
         }
      }
   }
}

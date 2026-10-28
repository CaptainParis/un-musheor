package musheor.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import musheor.compat.VersionHelper;
import musheor.modules.automation.HighwayBuilder;
import musheor.modules.automation.KekNuker;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;

public class HighwayUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean isStandingOn(Block block) {
      if (mc.player != null && mc.world != null) {
         BlockPos posBelow = mc.player.getBlockPos().down();
         return mc.world.getBlockState(posBelow).getBlock() == block;
      } else {
         return false;
      }
   }

   public static BlockPos getBehindPos(HighwayUtils.HighwayDirection dir) {
      int x = mc.player.getBlockX();
      int y = mc.player.getBlockY();
      int z = mc.player.getBlockZ();
      int dx = 0;
      int dz = 0;
      switch (dir) {
         case SOUTH:
            dz = -2;
            break;
         case SOUTHWEST:
            dx = 2;
            dz = -2;
            break;
         case WEST:
            dx = 2;
            break;
         case NORTHWEST:
            dx = 2;
            dz = 2;
            break;
         case NORTH:
            dz = 2;
            break;
         case NORTHEAST:
            dx = -2;
            dz = 2;
            break;
         case EAST:
            dx = -2;
            break;
         case SOUTHEAST:
            dx = -2;
            dz = -2;
      }

      return new BlockPos(x + dx, y, z + dz);
   }

   public static boolean isOnHighway() {
      if (mc.player != null && mc.world != null) {
         musheor.utils.HighwayDetector.DetectedHighway detected = getDetectedHighway();
         if (detected != null) {
            return musheor.utils.HighwayDetector.isOnDetectedHighway(mc.player.getBlockPos(), detected);
         } else {
            int py = musheor.utils.HighwayBuilderState.getInstance().getRailY() != null ? musheor.utils.HighwayBuilderState.getInstance().getRailY() : mc.player.getBlockY();
            BlockPos below = new BlockPos(mc.player.getBlockX(), py - 1, mc.player.getBlockZ());
            return mc.world.getBlockState(below).getBlock() == Blocks.OBSIDIAN;
         }
      } else {
         return false;
      }
   }

   public static musheor.utils.HighwayDetector.DetectedHighway getDetectedHighway() {
      if (mc.player == null) {
         return null;
      } else {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         musheor.utils.HighwayDetector.DetectedHighway detected = state.getDetectedHighway();
         if (detected != null) {
            return detected;
         } else {
            HighwayUtils.HighwayDirection dir = state.getDirection() != null ? state.getDirection() : directionFromYaw();
            if (dir == null) {
               return null;
            } else {
               int py = state.getRailY() != null ? state.getRailY() : mc.player.getBlockY();
               return musheor.utils.HighwayDetector.detect(mc.player.getBlockX(), mc.player.getBlockZ(), py, dir);
            }
         }
      }
   }

   public static BlockPos getLanePos() {
      musheor.utils.HighwayDetector.DetectedHighway detected = getDetectedHighway();
      if (detected != null) {
         return getLanePos(detected);
      } else if (mc.player == null) {
         return null;
      } else {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         HighwayUtils.HighwayDirection dir = state.getDirection() != null ? state.getDirection() : directionFromYaw();
         if (dir == null) {
            return null;
         } else {
            int ay = state.getRailY() != null ? state.getRailY() : mc.player.getBlockY();
            if (dir != HighwayUtils.HighwayDirection.EAST && dir != HighwayUtils.HighwayDirection.WEST) {
               if (dir != HighwayUtils.HighwayDirection.NORTH && dir != HighwayUtils.HighwayDirection.SOUTH) {
                  return null;
               } else {
                  return state.getAlignX() == null ? null : new BlockPos((int)Math.floor(state.getAlignX()), ay, mc.player.getBlockZ());
               }
            } else {
               return state.getAlignZ() == null ? null : new BlockPos(mc.player.getBlockX(), ay, (int)Math.floor(state.getAlignZ()));
            }
         }
      }
   }

   public static BlockPos getLanePos(musheor.utils.HighwayDetector.DetectedHighway detected) {
      if (mc.player != null && detected != null) {
         int ay = musheor.utils.HighwayBuilderState.getInstance().getRailY() != null ? musheor.utils.HighwayBuilderState.getInstance().getRailY() : mc.player.getBlockY();
         int px = mc.player.getBlockX();
         int pz = mc.player.getBlockZ();
         boolean cardinal = detected.direction == HighwayUtils.HighwayDirection.NORTH
            || detected.direction == HighwayUtils.HighwayDirection.SOUTH
            || detected.direction == HighwayUtils.HighwayDirection.EAST
            || detected.direction == HighwayUtils.HighwayDirection.WEST;
         if (cardinal) {
            boolean travelX = detected.direction == HighwayUtils.HighwayDirection.EAST || detected.direction == HighwayUtils.HighwayDirection.WEST;
            int axis = (int)detected.axisValue;
            return travelX ? new BlockPos(px, ay, axis) : new BlockPos(axis, ay, pz);
         } else {
            boolean isNESW = detected.direction == HighwayUtils.HighwayDirection.NORTHEAST || detected.direction == HighwayUtils.HighwayDirection.SOUTHWEST;
            int K = (int)detected.axisValue;
            int tx;
            int tz;
            if (isNESW) {
               tx = (int)Math.round((px - pz + K) / 2.0);
               tz = K - tx;
            } else {
               tx = (int)Math.round((px + pz + K) / 2.0);
               tz = tx - K;
            }

            return new BlockPos(tx, ay, tz);
         }
      } else {
         return null;
      }
   }

   public static void returnToLane() {
      if (mc.player != null && mc.world != null) {
         musheor.utils.HighwayDetector.DetectedHighway detected = getDetectedHighway();
         if (detected != null) {
            BlockPos lane = getLanePos(detected);
            if (lane != null) {
               if (!mc.player.getBlockPos().equals(lane)) {
                  musheor.utils.BaritoneHelper.goTo(lane);
                  MusheorSystem.debug("Returning to the %s lane...", detected.label);
               }

               return;
            }
         }

         BlockPos playerPos = mc.player.getBlockPos();
         int py = musheor.utils.HighwayBuilderState.getInstance().getRailY() != null ? musheor.utils.HighwayBuilderState.getInstance().getRailY() : playerPos.getY();
         BlockPos nearest = null;
         double nearestDistSq = Double.MAX_VALUE;
         int range = 20;

         for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
               BlockPos pos = new BlockPos(playerPos.getX() + x, py, playerPos.getZ() + z);
               if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN) {
                  double distSq = pos.getSquaredDistance(playerPos);
                  if (distSq < nearestDistSq) {
                     nearestDistSq = distSq;
                     nearest = pos;
                  }
               }
            }
         }

         if (nearest != null) {
            musheor.utils.BaritoneHelper.goTo(nearest);
            MusheorSystem.debug("Going back onto the highway...");
         }
      }
   }

   public static boolean hasAdjacentLiquid(BlockPos pos) {
      return Arrays.stream(Direction.values())
         .filter(dir -> dir != Direction.DOWN)
         .anyMatch(dir -> mc.world.getBlockState(pos.offset(dir)).isLiquid());
   }

   public static void tickLavaRemoval() {
      if (mc.player != null && mc.world != null) {
         if (musheor.utils.HighwayBuilderState.getInstance().getLavaTarget() != null) {
            if (mc.world.getFluidState(musheor.utils.HighwayBuilderState.getInstance().getLavaTarget()).getFluid() != Fluids.LAVA) {
               musheor.utils.BaritoneHelper.cancel();
               musheor.utils.HighwayBuilderState.getInstance().setLavaTarget((BlockPos)null);
            } else {
               musheor.utils.BaritoneHelper.goTo(musheor.utils.HighwayBuilderState.getInstance().getLavaTarget().withY(musheor.utils.HighwayBuilderState.getInstance().getRailY()));
            }
         } else if (musheor.utils.HighwayBuilderState.getInstance().getLavaReturnPos() != null) {
            if (mc.player.getBlockPos().equals(musheor.utils.HighwayBuilderState.getInstance().getLavaReturnPos())) {
               musheor.utils.BaritoneHelper.cancel();
               musheor.utils.HighwayBuilderState.getInstance().setLavaReturnPos(null);
            } else {
               musheor.utils.BaritoneHelper.goTo(musheor.utils.HighwayBuilderState.getInstance().getLavaReturnPos());
            }
         } else {
            if (HighwayBuilder.getHighwayType() == HighwayBuilder.HighwayType.CARDINAL) {
               for (BlockPos pos : musheor.utils.HighwayPositions.getCardinalAheadZone()) {
                  if (musheor.utils.BaritoneHelper.isPathing()) {
                     return;
                  }

                  if (mc.world.getFluidState(pos).getFluid() != Fluids.FLOWING_LAVA
                     && mc.world.getFluidState(pos).getFluid() == Fluids.LAVA) {
                     MusheorSystem.debug("Found obstructing lava ahead, removing...");
                     ModuleControl.setAutoWalk(false);
                     musheor.utils.HighwayBuilderState.getInstance().setLavaTarget(pos);
                     musheor.utils.HighwayBuilderState.getInstance().setLavaReturnPos(mc.player.getBlockPos().toImmutable());
                     return;
                  }
               }
            }

            if (HighwayBuilder.getHighwayType() == HighwayBuilder.HighwayType.DIAGONAL) {
               for (BlockPos pos : musheor.utils.HighwayPositions.getDiagonalAheadZone()) {
                  if (musheor.utils.BaritoneHelper.isPathing()) {
                     return;
                  }

                  if (mc.world.getFluidState(pos).getFluid() != Fluids.FLOWING_LAVA
                     && mc.world.getFluidState(pos).getFluid() == Fluids.LAVA) {
                     MusheorSystem.debug("Found obstructing lava ahead, removing...");
                     ModuleControl.setAutoWalk(false);
                     musheor.utils.HighwayBuilderState.getInstance().setLavaTarget(pos);
                     musheor.utils.HighwayBuilderState.getInstance().setLavaReturnPos(mc.player.getBlockPos().toImmutable());
                     return;
                  }
               }
            }
         }
      }
   }

   public static List<BlockPos> getEntitySupportBlocks(Entity entity) {
      assert mc.world != null;

      List<BlockPos> blocks = new ArrayList<>();
      Box box = entity.getBoundingBox();
      int y = (int)Math.floor(box.minY - 0.35);
      int minX = (int)Math.floor(box.minX);
      int maxX = (int)Math.floor(box.maxX);
      int minZ = (int)Math.floor(box.minZ);
      int maxZ = (int)Math.floor(box.maxZ);
      Mutable pos = new Mutable();

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            pos.set(x, y, z);
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty()) {
               blocks.add(pos.toImmutable());
            }
         }
      }

      return blocks;
   }

   public static Entity getEntityAt(World world, BlockPos pos) {
      Box box = new Box(pos);
      List<Entity> entities = world.getEntitiesByClass(Entity.class, box, e -> true);
      return entities.isEmpty() ? null : entities.getFirst();
   }

   public static boolean placeBlock(BlockPos pos, Direction direction) {
      if (mc.player != null && mc.getNetworkHandler() != null && mc.interactionManager != null) {
         swapOffhand();
         sendPlacePacket(Hand.OFF_HAND, hitResult(pos, direction));
         swapOffhand();
         return true;
      } else {
         return false;
      }
   }

   public static BlockHitResult hitResult(BlockPos pos, Direction direction) {
      return new BlockHitResult(Vec3d.ofCenter(pos), direction, pos, false);
   }

   public static void sendPlacePacket(Hand hand, BlockHitResult hit) {
      mc.interactionManager.sendSequencedPacket(mc.world, sequence -> new PlayerInteractBlockC2SPacket(hand, hit, sequence));
   }

   public static void swapOffhand() {
      if (mc.player != null && mc.world != null) {
         mc.interactionManager
            .sendSequencedPacket(mc.world, sequence -> new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
         ItemStack mainHandStack = mc.player.getMainHandStack();
         ItemStack offHandStack = mc.player.getOffHandStack();
         mc.player.getInventory().setStack(mc.player.getInventory().selectedSlot, offHandStack);
         mc.player.getInventory().setStack(40, mainHandStack);
      }
   }

   public static boolean placeItem(Item item, BlockPos pos, Direction direction) {
      if (!BlockUtils.canPlaceBlock(pos, true, Block.getBlockFromItem(item))) {
         return false;
      } else {
         boolean silent = (Boolean)MusheorSystem.Manager.silentPlace.get();
         musheor.utils.manager.InventoryManager.swapToItemSilent(item, silent);
         boolean placed = placeBlock(pos, direction);
         if (silent) {
            InvUtils.swapBack();
         }

         return placed;
      }
   }

   public static void lookAtBlock(BlockPos blockPos) {
      Vec3d hitPos = Vec3d.ofCenter(blockPos);
      Direction side = BlockUtils.getPlaceSide(blockPos);
      if (side != null) {
         blockPos.offset(side);
         hitPos = hitPos.add(Vec3d.of(side.getVector()).multiply(0.5));
      }

      assert mc.player != null;

      float[] rotation = getLookAngles(mc.player, hitPos);
      mc.player.setYaw(rotation[0]);
      mc.player.setPitch(rotation[1]);
   }

   public static void lookAtBlockFace(BlockPos pos, Direction side) {
      Vec3d center = Vec3d.ofCenter(pos);
      float[] rotation = getLookAngles(mc.player, center.add(Vec3d.of(side.getVector()).multiply(0.5)));
      mc.player.setYaw(rotation[0]);
      mc.player.setPitch(rotation[1]);
   }

   static float[] getLookAngles(ClientPlayerEntity player, Vec3d vec) {
      Vec3d eyesPos = player.getEyePos();
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double r = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
      double yaw = -Math.atan2(diffX, diffZ) / Math.PI * 180.0;
      double pitch = -Math.asin(diffY / r) / Math.PI * 180.0;
      return new float[]{(float)yaw, (float)pitch};
   }

   public static boolean hasGroundSupport(List<BlockPos> locations) {
      if (locations.isEmpty()) {
         return false;
      } else {
         assert mc.player != null;

         for (BlockPos pos : locations) {
            boolean hasGroundBelow = false;

            for (int x = -1; x <= 1 && !hasGroundBelow; x++) {
               for (int z = -1; z <= 1 && !hasGroundBelow; z++) {
                  BlockPos checkPos = pos.add(x, -1, z);
                  if (!mc.world.getBlockState(checkPos).isAir()) {
                     hasGroundBelow = true;
                  }
               }
            }

            if (!hasGroundBelow) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean needsReplacement(BlockPos pos, Block block) {
      assert mc.world != null;

      return !mc.world.getBlockState(pos).isAir()
         && (
            !mc.world.getBlockState(pos).isReplaceable()
               || mc.world.getBlockState(pos).getBlock() instanceof VineBlock
               || mc.world.getBlockState(pos).getBlock() instanceof SnowBlock
         )
         && mc.world.getBlockState(pos).getBlock() != block;
   }

   public static void breakBlockingEntities(BlockPos[] positions) {
      Set<BlockPos> placementTargets = new HashSet<>(Arrays.asList(positions));

      for (BlockPos currentPos : positions) {
         if (BlockUtils.canPlace(currentPos, false) && !BlockUtils.canPlace(currentPos, true)) {
            if (distanceToMissingBlock(HighwayBuilder.getPavementBlock()) < 1.4) {
               ModuleControl.setAutoWalk(false);
            }

            Entity entity = getEntityAt(mc.world, currentPos);
            if (entity != null
               && entity != mc.player
               && entity.isAlive()
               && !(entity instanceof EndCrystalEntity)
               && !(entity instanceof BoatEntity)) {
               List<BlockPos> blocksToBreak = getEntitySupportBlocks(entity);
               if (!blocksToBreak.isEmpty()) {
                  for (BlockPos spleefPos : blocksToBreak) {
                     if (!placementTargets.contains(spleefPos)) {
                        KekNuker.targetBlocks.add(spleefPos);
                        KekNuker.targetBlocks.add(spleefPos.down());
                        KekNuker.targetBlocks.add(spleefPos.down().down());
                        MusheorSystem.debug(
                           "Spleefing %s at x: %s y: %s z: %s",
                           entity.getName().getString(),
                           entity.getBlockPos().getX(),
                           entity.getBlockPos().getY(),
                           entity.getBlockPos().getZ()
                        );
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean isInPlacementRange(BlockPos pos) {
      return mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ())
         <= (Double)MusheorSystem.Manager.placementRange.get() * (Double)MusheorSystem.Manager.placementRange.get();
   }

   public static boolean isWithinRange(BlockPos pos, double range) {
      return mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) <= range * range;
   }

   public static void pruneRecentPlacements(int currentTick, Block block) {
      musheor.utils.HighwayBuilderState.getInstance()
         .getRecentPlacementAttempts()
         .entrySet()
         .removeIf(entry -> currentTick - entry.getValue() > (Integer)MusheorSystem.Manager.placementTimeout.get());
   }

   public static void placeAlongHighway(BlockPos[] positions, boolean scaffold) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      if (mc.player != null && mc.world != null && !state.isSuppressPlacement() && !HighwayBuilder.isKillAuraAttacking()) {
         boolean diagonal = switch (HighwayBuilder.getHighwayDirection()) {
            case SOUTH, WEST, NORTH, EAST -> false;
            default -> true;
         };
         double distance = distanceToMissingBlock(HighwayBuilder.getPavementBlock());
         MusheorSystem.debug("Distance %s", Math.round(distance * 100.0) / 100.0);
         if (diagonal) {
            if (!(distance <= 2.0) && !hasPlaceableInZone()) {
               state.setInPlacementRange(false);
               ModuleControl.setAutoWalk(true);
            } else {
               ModuleControl.setAutoWalk(false);
               state.setInPlacementRange(true);
            }
         } else if (!(distance <= 1.5) && !hasPlaceableInZone()) {
            state.setInPlacementRange(false);
            ModuleControl.setAutoWalk(true);
         } else {
            ModuleControl.setAutoWalk(false);
            state.setInPlacementRange(true);
         }

         if (!KekNuker.isBusy()) {
            musheor.utils.BlockPlacer.placeBlocks(positions, scaffold ? HighwayBuilder.getScaffoldBlock() : HighwayBuilder.getPavementBlock(), Direction.UP, state.getPlacementType());
         }
      }
   }

   public static boolean hasPlaceableInZone() {
      HighwayUtils.HighwayDirection dir = HighwayBuilder.getHighwayDirection();
      if (dir == null) {
         return false;
      } else {
         boolean leftRail = HighwayBuilder.isPlaceLeftRail();
         boolean rightRail = HighwayBuilder.isPlaceRightRail();
         boolean cardinal = dir == HighwayUtils.HighwayDirection.NORTH
            || dir == HighwayUtils.HighwayDirection.SOUTH
            || dir == HighwayUtils.HighwayDirection.EAST
            || dir == HighwayUtils.HighwayDirection.WEST;
         BlockPos[] zone = cardinal
            ? musheor.utils.HighwayPositions.getCardinalFloor(0, 1, leftRail, rightRail)
            : musheor.utils.HighwayPositions.getDiagonalFloor(0, 1, leftRail, rightRail);
         if (HighwayBuilder.isFillCeiling()) {
            BlockPos[] ceiling = cardinal
               ? musheor.utils.HighwayPositions.getCardinalCeiling(0, 1, leftRail, rightRail)
               : musheor.utils.HighwayPositions.getDiagonalCeiling(0, 1, leftRail, rightRail);
            BlockPos[] combined = new BlockPos[zone.length + ceiling.length];
            System.arraycopy(zone, 0, combined, 0, zone.length);
            System.arraycopy(ceiling, 0, combined, zone.length, ceiling.length);
            zone = combined;
         }

         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         musheor.utils.HighwayDetector.DetectedHighway detected = state.getDetectedHighway();
         int railY = state.getRailY() != null ? state.getRailY() : Integer.MIN_VALUE;

         for (BlockPos pos : zone) {
            if ((detected == null || pos.getY() != railY || !musheor.utils.HighwayDetector.isCrossingHighway(pos, detected)) && BlockUtils.canPlace(pos, true)) {
               return true;
            }
         }

         return false;
      }
   }

   public static void placeBlocksOffhand(List<BlockPos> list, Block block) {
      boolean silent = (Boolean)MusheorSystem.Manager.silentPlace.get();
      musheor.utils.manager.InventoryManager.swapToItemSilent(block.asItem(), silent);
      swapOffhand();

      for (BlockPos currentPos : list) {
         if (BlockUtils.canPlace(currentPos, true) && PlayerUtils.isWithin(currentPos, 4.5)) {
            if (!musheor.utils.PacketBudget.canPlace()) {
               break;
            }

            sendPlacePacket(Hand.OFF_HAND, hitResult(currentPos, Direction.DOWN));
         }
      }

      swapOffhand();
      if (silent) {
         InvUtils.swapBack();
      }
   }

   public static void fastScaffold() {
      if (mc.player != null && mc.world != null) {
         BlockPos playerPos = mc.player.getBlockPos();

         for (int offset = 1; offset <= 4; offset++) {
            BlockPos targetPos = switch (HighwayBuilder.getHighwayDirection()) {
               case SOUTH -> playerPos.add(0, -1, offset);
               case SOUTHWEST -> playerPos.add(-offset, -1, offset);
               case WEST -> playerPos.add(-offset, -1, 0);
               case NORTHWEST -> playerPos.add(-offset, -1, -offset);
               case NORTH -> playerPos.add(0, -1, -offset);
               case NORTHEAST -> playerPos.add(offset, -1, -offset);
               case EAST -> playerPos.add(offset, -1, 0);
               case SOUTHEAST -> playerPos.add(offset, -1, offset);
            };
            if (targetPos == null) {
               return;
            }

            if (mc.getNetworkHandler() == null) {
               return;
            }

            if (mc.world.getBlockState(targetPos).getBlock() == Blocks.SOUL_SAND) {
               BlockUtils.breakBlock(targetPos, false);
            }

            if (mc.world.getBlockState(targetPos).isAir()) {
               placeItem(Items.NETHERRACK, targetPos, Direction.DOWN);
            }
         }
      }
   }

   public static double distanceToMissingBlock(Block block) {
      int offsetX = 0;
      int offsetZ = 0;
      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      switch (direction) {
         case SOUTH:
            offsetZ = 1;
            break;
         case SOUTHWEST:
            offsetX = -1;
            offsetZ = 1;
            break;
         case WEST:
            offsetX = -1;
            break;
         case NORTHWEST:
            offsetX = -1;
            offsetZ = -1;
            break;
         case NORTH:
            offsetZ = -1;
            break;
         case NORTHEAST:
            offsetX = 1;
            offsetZ = -1;
            break;
         case EAST:
            offsetX = 1;
            break;
         case SOUTHEAST:
            offsetX = 1;
            offsetZ = 1;
      }

      assert mc.player != null;

      assert mc.world != null;

      Vec3d playerPos = VersionHelper.get().getPlayerPos();
      Vec3d blockPos = null;

      for (int i = 0; i <= 5; i++) {
         double pX = VersionHelper.get().getPlayerPos().getX() + offsetX * i;
         double pZ = VersionHelper.get().getPlayerPos().getZ() + offsetZ * i;
         BlockPos centerPos = BlockPos.ofFloored(new Vec3d(pX, playerPos.getY() - 1.0, pZ));
         if (block == Blocks.OBSIDIAN) {
            if (mc.world.getBlockState(centerPos).getBlock() != Blocks.OBSIDIAN
               && mc.world.getBlockState(centerPos).getBlock() != Blocks.CRYING_OBSIDIAN) {
               blockPos = Vec3d.ofCenter(centerPos);
               break;
            }
         } else if (!mc.world.getBlockState(centerPos).getBlock().getDefaultState().isSolid()) {
            blockPos = Vec3d.ofCenter(centerPos);
            break;
         }
      }

      return blockPos == null ? 10.0 : distanceXZ(blockPos, playerPos);
   }

   public static Block getBlockAt(BlockPos pos) {
      assert mc.world != null && mc.player != null;

      return mc.world.getBlockState(pos).getBlock();
   }

   public static void stopBreaking(BlockPos pos, Direction face) {
      Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, face));
   }

   public static double distanceXZ(BlockPos bP, BlockPos pP) {
      double dx;
      if (bP.getX() > pP.getX()) {
         dx = bP.getX() - pP.getX();
      } else {
         dx = pP.getX() - bP.getX();
      }

      double dz;
      if (bP.getZ() > pP.getZ()) {
         dz = bP.getZ() - pP.getZ();
      } else {
         dz = pP.getZ() - bP.getZ();
      }

      return Math.sqrt(dx * dx + dz * dz);
   }

   public static double distanceXZ(Vec3d pos1, Vec3d pos2) {
      double dx;
      if (pos1.getX() > pos2.getX()) {
         dx = pos1.getX() - pos2.getX();
      } else {
         dx = pos2.getX() - pos1.getX();
      }

      double dz;
      if (pos1.getZ() > pos2.getZ()) {
         dz = pos1.getZ() - pos2.getZ();
      } else {
         dz = pos2.getZ() - pos1.getZ();
      }

      return Math.sqrt(dx * dx + dz * dz);
   }

   public static void checkFrontCollision() {
      assert mc.player != null && mc.world != null;

      int offset = 1;
      int offsetX = 0;
      int offsetZ = 0;
      boolean isDiagonal = false;
      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      switch (direction) {
         case SOUTH:
            offsetZ = offset;
            break;
         case SOUTHWEST:
            offsetX = -offset;
            offsetZ = offset;
            isDiagonal = true;
            break;
         case WEST:
            offsetX = -offset;
            break;
         case NORTHWEST:
            offsetX = -offset;
            offsetZ = -offset;
            isDiagonal = true;
            break;
         case NORTH:
            offsetZ = -offset;
            break;
         case NORTHEAST:
            offsetX = offset;
            offsetZ = -offset;
            isDiagonal = true;
            break;
         case EAST:
            offsetX = offset;
            break;
         case SOUTHEAST:
            offsetX = offset;
            offsetZ = offset;
            isDiagonal = true;
      }

      int[][] checks = isDiagonal ? new int[][]{{offsetX, offsetZ}, {offsetX, 0}, {0, offsetZ}} : new int[][]{{offsetX, offsetZ}};
      int height = 0;
      if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.PAVE) {
         height = 3;
      }

      if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.DIG) {
         height = 4;
      }

      for (int[] check : checks) {
         for (int i = 0; i < height; i++) {
            BlockPos checkPos = mc.player.getBlockPos().add(check[0], i, check[1]);
            if (!mc.world.getBlockState(checkPos).getCollisionShape(mc.world, checkPos).isEmpty()) {
               ModuleControl.setAutoWalk(false);
               MusheorSystem.debug("Collision detected in front of the player");
            }
         }
      }
   }

   public static boolean placeSafetyBlock() {
      assert mc.player != null && mc.world != null;

      int offsetX = 0;
      int offsetZ = 0;
      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      switch (direction) {
         case SOUTHWEST:
            offsetX = -1;
            offsetZ = 1;
         case WEST:
         case NORTH:
         case EAST:
         default:
            break;
         case NORTHWEST:
            offsetX = -1;
            offsetZ = -1;
            break;
         case NORTHEAST:
            offsetX = 1;
            offsetZ = -1;
            break;
         case SOUTHEAST:
            offsetX = 1;
            offsetZ = 1;
      }

      if (mc.world.getBlockState(mc.player.getBlockPos().add(offsetX, -1, offsetZ)).isAir()) {
         BlockPos position = mc.player.getBlockPos().add(offsetX, -1, offsetZ);
         boolean silent = (Boolean)MusheorSystem.Manager.silentPlace.get();
         musheor.utils.manager.InventoryManager.swapToItemSilent(Items.NETHERRACK, silent);
         placeBlock(position, Direction.DOWN);
         if (silent) {
            InvUtils.swapBack();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean hasNearbyItem(Item targetItem) {
      if (mc.world != null && mc.player != null) {
         int SEARCH_RADIUS = 10;
         int MAX_Y = 122;
         Box searchArea = new Box(
            mc.player.getX() - SEARCH_RADIUS,
            mc.player.getY() - SEARCH_RADIUS,
            mc.player.getZ() - SEARCH_RADIUS,
            mc.player.getX() + SEARCH_RADIUS,
            MAX_Y,
            mc.player.getZ() + SEARCH_RADIUS
         );
         List<ItemEntity> items = mc.world
            .getEntitiesByClass(ItemEntity.class, searchArea, itemEntity -> itemEntity.getStack().getItem() == targetItem);
         return !items.isEmpty();
      } else {
         return false;
      }
   }

   public static void goToNearestItem(Item item) {
      if (mc.player != null && mc.world != null) {
         ItemEntity nearestItemEntity = null;
         double closestDistanceSq = Double.MAX_VALUE;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity && itemEntity.getStack().getItem() == item) {
               double distSq = entity.squaredDistanceTo(mc.player);
               if (distSq < closestDistanceSq) {
                  closestDistanceSq = distSq;
                  nearestItemEntity = itemEntity;
               }
            }
         }

         if (nearestItemEntity != null) {
            BlockPos targetPos = nearestItemEntity.getBlockPos();
            musheor.utils.BaritoneHelper.goToViaCommand(targetPos);
         } else {
            ChatUtils.error("No matching item found nearby.", new Object[0]);
         }
      }
   }

   public static Direction directionFromLook(float yaw, float pitch) {
      if (pitch > 60.0F) {
         return Direction.DOWN;
      } else if (pitch < -60.0F) {
         return Direction.UP;
      } else {
         yaw %= 360.0F;
         if (yaw < 0.0F) {
            yaw += 360.0F;
         }

         if (yaw >= 315.0F || yaw < 45.0F) {
            return Direction.SOUTH;
         } else if (yaw >= 45.0F && yaw < 135.0F) {
            return Direction.WEST;
         } else {
            return yaw >= 135.0F && yaw < 225.0F ? Direction.NORTH : Direction.EAST;
         }
      }
   }

   public static HighwayUtils.HighwayDirection directionFromYaw() {
      assert mc.player != null;

      return HighwayUtils.HighwayDirection.fromYaw(mc.player.getYaw());
   }

   public static enum HighwayDirection {
      SOUTH,
      SOUTHWEST,
      WEST,
      NORTHWEST,
      NORTH,
      NORTHEAST,
      EAST,
      SOUTHEAST;

      public static HighwayUtils.HighwayDirection fromYaw(float yaw) {
         float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
         int i = Math.round(normalized / 45.0F) % 8;
         return values()[i];
      }

      public HighwayUtils.HighwayDirection opposite() {
         return values()[(this.ordinal() + 4) % 8];
      }

      public static float oppositeYaw(float currentYaw) {
         float normalized = (currentYaw % 360.0F + 360.0F) % 360.0F;
         return (normalized + 180.0F) % 360.0F;
      }

      public static float toYaw(HighwayUtils.HighwayDirection direction) {
         return direction.ordinal() * 45.0F;
      }
   }

   public record XZ(double x, double z) {
   }
}

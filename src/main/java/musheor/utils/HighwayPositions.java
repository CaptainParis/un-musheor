package musheor.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import musheor.modules.automation.HighwayBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class HighwayPositions {
   @NotNull
   public static BlockPos[] getCardinalFloor(int f, int b, boolean leftRail, boolean rightRail) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      boolean placeRails = HighwayBuilder.isPlaceRails();
      int width = HighwayBuilder.getPavementWidth();
      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      int playerX = MeteorClient.mc.player.getBlockX();
      int playerY = state.getRailY();
      int playerZ = MeteorClient.mc.player.getBlockZ();
      int dx;
      int dz;
      int start;
      int end;
      int step;
      int sideSign;
      switch (direction) {
         case NORTH:
            dx = 0;
            dz = 1;
            start = b;
            end = -f;
            step = -1;
            sideSign = 1;
            playerX = state.getStartX();
            break;
         case SOUTH:
            dx = 0;
            dz = 1;
            start = -b;
            end = f;
            step = 1;
            sideSign = -1;
            playerX = state.getStartX();
            break;
         case EAST:
            dx = 1;
            dz = 0;
            start = -b;
            end = f;
            step = 1;
            sideSign = 1;
            playerZ = state.getStartZ();
            break;
         case WEST:
            dx = 1;
            dz = 0;
            start = b;
            end = -f;
            step = -1;
            sideSign = -1;
            playerZ = state.getStartZ();
            break;
         default:
            return new BlockPos[0];
      }

      int leftBound;
      int rightBound;
      if (width % 2 == 0) {
         leftBound = width / 2;
         rightBound = leftBound - 1;
      } else {
         leftBound = rightBound = (width - 1) / 2;
      }

      List<BlockPos> positions = new ArrayList<>();

      for (int i = start; i != end + step; i += step) {
         for (int j = -leftBound; j <= rightBound; j++) {
            int x = playerX + dx * i + dz * j * sideSign;
            int z = playerZ + dz * i + dx * j * sideSign;
            positions.add(new BlockPos(x, playerY - 1, z));
         }

         if (placeRails) {
            int leftRailX = playerX + dx * i + dz * (-leftBound - 1) * sideSign;
            int leftRailZ = playerZ + dz * i + dx * (-leftBound - 1) * sideSign;
            int rightRailX = playerX + dx * i + dz * (rightBound + 1) * sideSign;
            int rightRailZ = playerZ + dz * i + dx * (rightBound + 1) * sideSign;
            if (leftRail) {
               positions.add(new BlockPos(leftRailX, playerY, leftRailZ));
            }

            if (rightRail) {
               positions.add(new BlockPos(rightRailX, playerY, rightRailZ));
            }
         }
      }

      positions.sort(Comparator.comparingDouble(p -> MeteorClient.mc.player.squaredDistanceTo(Vec3d.of(p))));
      return positions.toArray(new BlockPos[0]);
   }

   public static BlockPos[] getCardinalTunnel(int f, int b) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      int width = HighwayBuilder.getPavementWidth();
      int height = 2;
      if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.DIG) {
         height = 3;
      }

      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      int playerX = MeteorClient.mc.player.getBlockX();
      int playerY = state.getRailY();
      int playerZ = MeteorClient.mc.player.getBlockZ();
      int dx;
      int dz;
      int start;
      int end;
      int step;
      int sideSign;
      switch (direction) {
         case NORTH:
            dx = 0;
            dz = 1;
            start = b;
            end = -f;
            step = -1;
            sideSign = 1;
            playerX = state.getStartX();
            break;
         case SOUTH:
            dx = 0;
            dz = 1;
            start = -b;
            end = f;
            step = 1;
            sideSign = -1;
            playerX = state.getStartX();
            break;
         case EAST:
            dx = 1;
            dz = 0;
            start = -b;
            end = f;
            step = 1;
            sideSign = 1;
            playerZ = state.getStartZ();
            break;
         case WEST:
            dx = 1;
            dz = 0;
            start = b;
            end = -f;
            step = -1;
            sideSign = -1;
            playerZ = state.getStartZ();
            break;
         default:
            return new BlockPos[0];
      }

      int leftBound;
      int rightBound;
      if (width % 2 == 0) {
         leftBound = width / 2;
         rightBound = leftBound - 1;
      } else {
         leftBound = rightBound = (width - 1) / 2;
      }

      List<BlockPos> positions = new ArrayList<>();

      for (int i = start; i != end + step; i += step) {
         for (int j = 0; j <= height; j++) {
            for (int k = -leftBound; k <= rightBound; k++) {
               int x = playerX + dx * i + dz * k * sideSign;
               int z = playerZ + dz * i + dx * k * sideSign;
               positions.add(new BlockPos(x, playerY + j, z));
            }

            if (HighwayBuilder.isMineAboveRails() && j > 0) {
               int leftRailX = playerX + dx * i + dz * (-leftBound - 1) * sideSign;
               int leftRailZ = playerZ + dz * i + dx * (-leftBound - 1) * sideSign;
               int rightRailX = playerX + dx * i + dz * (rightBound + 1) * sideSign;
               int rightRailZ = playerZ + dz * i + dx * (rightBound + 1) * sideSign;
               positions.add(new BlockPos(leftRailX, playerY + j, leftRailZ));
               positions.add(new BlockPos(rightRailX, playerY + j, rightRailZ));
            }
         }
      }

      positions.sort(Comparator.comparingDouble(p -> MeteorClient.mc.player.squaredDistanceTo(Vec3d.of(p))));
      return positions.toArray(new BlockPos[0]);
   }

   public static BlockPos[] getCardinalCeiling(int f, int b, boolean leftRail, boolean rightRail) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      boolean placeRails = HighwayBuilder.isPlaceRails();
      int width = HighwayBuilder.getPavementWidth();

      assert MeteorClient.mc.player != null;

      int playerX = MeteorClient.mc.player.getBlockX();
      int playerY = MeteorClient.mc.player.getBlockY();
      int playerZ = MeteorClient.mc.player.getBlockZ();
      int ceilingY = playerY + 3;
      int leftBound;
      int rightBound;
      if (width % 2 == 0) {
         leftBound = width / 2;
         rightBound = leftBound - 1;
      } else {
         leftBound = rightBound = (width - 1) / 2;
      }

      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      int dx;
      int dz;
      int start;
      int end;
      int step;
      int sideSign;
      switch (direction) {
         case NORTH:
            dx = 0;
            dz = 1;
            start = b;
            end = -f;
            step = -1;
            sideSign = 1;
            playerX = state.getStartX();
            break;
         case SOUTH:
            dx = 0;
            dz = 1;
            start = -b;
            end = f;
            step = 1;
            sideSign = -1;
            playerX = state.getStartX();
            break;
         case EAST:
            dx = 1;
            dz = 0;
            start = -b;
            end = f;
            step = 1;
            sideSign = 1;
            playerZ = state.getStartZ();
            break;
         case WEST:
            dx = 1;
            dz = 0;
            start = b;
            end = -f;
            step = -1;
            sideSign = -1;
            playerZ = state.getStartZ();
            break;
         default:
            return new BlockPos[0];
      }

      List<BlockPos> positions = new ArrayList<>();

      for (int i = start; i != end + step; i += step) {
         for (int j = -leftBound; j <= rightBound; j++) {
            int x = playerX + dx * i + dz * j * sideSign;
            int z = playerZ + dz * i + dx * j * sideSign;
            positions.add(new BlockPos(x, ceilingY, z));
         }

         if (placeRails) {
            if (leftRail) {
               int x = playerX + dx * i + dz * (-leftBound - 1) * sideSign;
               int z = playerZ + dz * i + dx * (-leftBound - 1) * sideSign;
               positions.add(new BlockPos(x, ceilingY, z));
            }

            if (rightRail) {
               int x = playerX + dx * i + dz * (rightBound + 1) * sideSign;
               int z = playerZ + dz * i + dx * (rightBound + 1) * sideSign;
               positions.add(new BlockPos(x, ceilingY, z));
            }
         }
      }

      positions.sort(Comparator.comparingDouble(p -> MeteorClient.mc.player.squaredDistanceTo(Vec3d.of(p))));
      return positions.toArray(new BlockPos[0]);
   }

   private static void addDiagonalSlice(int vMin, int vMax, int uMin, int uMax, int K, boolean isNESW, int y, List<BlockPos> out) {
      for (int v = vMin; v <= vMax; v++) {
         for (int u = uMin; u <= uMax; u++) {
            if ((u + K + v & 1) == 0) {
               int x = u + K + v >> 1;
               int z = isNESW ? u + K - v >> 1 : v - u - K >> 1;
               out.add(new BlockPos(x, y, z));
            }
         }
      }
   }

   private static int[] diagonalSpan(musheor.utils.HighwayBuilderState state, int f, int b) {
      HighwayUtils.HighwayDirection dir = HighwayBuilder.getHighwayDirection();
      if (dir != HighwayUtils.HighwayDirection.NORTHEAST
         && dir != HighwayUtils.HighwayDirection.SOUTHWEST
         && dir != HighwayUtils.HighwayDirection.NORTHWEST
         && dir != HighwayUtils.HighwayDirection.SOUTHEAST) {
         return null;
      } else {
         int px = state.getRailX();
         int pz = state.getRailZ();
         boolean isNESW = dir == HighwayUtils.HighwayDirection.NORTHEAST || dir == HighwayUtils.HighwayDirection.SOUTHWEST;
         musheor.utils.HighwayDetector.DetectedHighway detected = state.getDetectedHighway();
         int K = detected != null ? (int)detected.axisValue : (isNESW ? px + pz : px - pz);
         int HW = (HighwayBuilder.getPavementWidth() - 1) / 2;
         int lpx = MeteorClient.mc.player.getBlockX();
         int lpz = MeteorClient.mc.player.getBlockZ();
         int vPlayer = isNESW ? lpx - lpz : lpx + lpz;
         boolean vInc = dir == HighwayUtils.HighwayDirection.NORTHEAST || dir == HighwayUtils.HighwayDirection.SOUTHEAST;
         int vMin = vInc ? vPlayer - 2 * b : vPlayer - 2 * f;
         int vMax = vInc ? vPlayer + 2 * f : vPlayer + 2 * b;
         return new int[]{K, HW, vMin, vMax, state.getRailY(), isNESW ? 1 : 0};
      }
   }

   public static BlockPos[] getDiagonalFloor(int f, int b, boolean leftRail, boolean rightRail) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();

      assert MeteorClient.mc.player != null;

      if (state.getRailX() == null) {
         state.setRailX(Integer.valueOf(MeteorClient.mc.player.getBlockX()));
      }

      if (state.getRailY() == null) {
         state.setRailY(Integer.valueOf(MeteorClient.mc.player.getBlockY()));
      }

      if (state.getRailZ() == null) {
         state.setRailZ(Integer.valueOf(MeteorClient.mc.player.getBlockZ()));
      }

      int[] s = diagonalSpan(state, f, b);
      if (s == null) {
         return new BlockPos[0];
      } else {
         int K = s[0];
         int HW = s[1];
         int vMin = s[2];
         int vMax = s[3];
         int py = s[4];
         boolean isNESW = s[5] == 1;
         List<BlockPos> positions = new ArrayList<>();
         addDiagonalSlice(vMin, vMax, -HW, HW, K, isNESW, py - 1, positions);
         if (HighwayBuilder.isPlaceRails()) {
            int leftU = isNESW ? HW + 1 : -(HW + 1);
            int rightU = isNESW ? -(HW + 1) : HW + 1;
            if (leftRail) {
               addDiagonalSlice(vMin, vMax, leftU, leftU, K, isNESW, py, positions);
            }

            if (rightRail) {
               addDiagonalSlice(vMin, vMax, rightU, rightU, K, isNESW, py, positions);
            }
         }

         positions.sort(Comparator.comparingDouble(p -> MeteorClient.mc.player.squaredDistanceTo(Vec3d.of(p))));
         return positions.toArray(new BlockPos[0]);
      }
   }

   public static BlockPos[] getDiagonalCeiling(int f, int b, boolean leftRail, boolean rightRail) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();

      assert MeteorClient.mc.player != null;

      if (state.getRailX() == null) {
         state.setRailX(Integer.valueOf(MeteorClient.mc.player.getBlockX()));
      }

      if (state.getRailY() == null) {
         state.setRailY(Integer.valueOf(MeteorClient.mc.player.getBlockY()));
      }

      if (state.getRailZ() == null) {
         state.setRailZ(Integer.valueOf(MeteorClient.mc.player.getBlockZ()));
      }

      int[] s = diagonalSpan(state, f, b);
      if (s == null) {
         return new BlockPos[0];
      } else {
         int K = s[0];
         int HW = s[1];
         int vMin = s[2];
         int vMax = s[3];
         int py = s[4];
         boolean isNESW = s[5] == 1;
         int ceilingY = py + 3;
         List<BlockPos> positions = new ArrayList<>();
         addDiagonalSlice(vMin, vMax, -HW, HW, K, isNESW, ceilingY, positions);
         if (HighwayBuilder.isPlaceRails()) {
            int leftU = isNESW ? HW + 1 : -(HW + 1);
            int rightU = isNESW ? -(HW + 1) : HW + 1;
            if (leftRail) {
               addDiagonalSlice(vMin, vMax, leftU, leftU, K, isNESW, ceilingY, positions);
            }

            if (rightRail) {
               addDiagonalSlice(vMin, vMax, rightU, rightU, K, isNESW, ceilingY, positions);
            }
         }

         positions.sort(Comparator.comparingDouble(p -> MeteorClient.mc.player.squaredDistanceTo(Vec3d.of(p))));
         return positions.toArray(new BlockPos[0]);
      }
   }

   public static BlockPos[] getDiagonalTunnel(int f, int b) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      int height = 2;
      if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.DIG) {
         height = 3;
      }

      if (state.getRailX() == null) {
         state.setRailX(Integer.valueOf(MeteorClient.mc.player.getBlockX()));
      }

      if (state.getRailY() == null) {
         state.setRailY(Integer.valueOf(MeteorClient.mc.player.getBlockY()));
      }

      if (state.getRailZ() == null) {
         state.setRailZ(Integer.valueOf(MeteorClient.mc.player.getBlockZ()));
      }

      int[] s = diagonalSpan(state, f, b);
      if (s == null) {
         return new BlockPos[0];
      } else {
         int K = s[0];
         int HW = s[1];
         int vMin = s[2];
         int vMax = s[3];
         int py = s[4];
         boolean isNESW = s[5] == 1;
         List<BlockPos> positions = new ArrayList<>();

         for (int j = 0; j <= height; j++) {
            addDiagonalSlice(vMin, vMax, -HW, HW, K, isNESW, py + j, positions);
            if (HighwayBuilder.isMineAboveRails() && j > 0) {
               addDiagonalSlice(vMin, vMax, -(HW + 1), -(HW + 1), K, isNESW, py + j, positions);
               addDiagonalSlice(vMin, vMax, HW + 1, HW + 1, K, isNESW, py + j, positions);
            }
         }

         positions.sort(Comparator.comparingDouble(p -> MeteorClient.mc.player.squaredDistanceTo(Vec3d.of(p))));
         return positions.toArray(new BlockPos[0]);
      }
   }

   public static BlockPos[] getCardinalBehindTunnel() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      int width = HighwayBuilder.getPavementWidth();
      int leftBound = 0;
      int rightBound = 0;
      if (HighwayBuilder.getHighwayDirection() == null) {
         return new BlockPos[0];
      } else {
         assert MeteorClient.mc.player != null;

         if (state.getRailX() == null) {
            state.setRailX(Integer.valueOf(MeteorClient.mc.player.getBlockX()));
         }

         if (state.getRailY() == null) {
            state.setRailY(Integer.valueOf(MeteorClient.mc.player.getBlockY()));
         }

         if (state.getRailZ() == null) {
            state.setRailZ(Integer.valueOf(MeteorClient.mc.player.getBlockZ()));
         }

         if (width % 2 == 0) {
            leftBound = width / 2 + 1;
         }

         rightBound = leftBound - 1;
         if (width % 2 != 0) {
            leftBound = rightBound = (width - 1) / 2 + 1;
         }

         int height = 2;
         int length = 4;

         return switch (HighwayBuilder.getHighwayDirection()) {
            case NORTH -> {
               List<BlockPos> positions = new ArrayList<>();
               if (HighwayBuilder.isMineAboveRails()) {
                  for (int i = length; i > 0; i--) {
                     for (int k = 0; k < height; k++) {
                        positions.add(new BlockPos(state.getRailX() - leftBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() + rightBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               for (int i = length; i > 0; i--) {
                  for (int j = -leftBound + 1; j < rightBound; j++) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(state.getRailX() + j, state.getRailY() + k, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case SOUTH -> {
               List<BlockPos> positions = new ArrayList<>();
               if (HighwayBuilder.isMineAboveRails()) {
                  for (int i = -length; i < 0; i++) {
                     for (int k = 0; k < height; k++) {
                        positions.add(new BlockPos(state.getRailX() + leftBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() - rightBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               for (int i = -length; i < 0; i++) {
                  for (int j = leftBound - 1; j > -rightBound; j--) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(state.getRailX() + j, state.getRailY() + k, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case EAST -> {
               List<BlockPos> positions = new ArrayList<>();
               if (HighwayBuilder.isMineAboveRails()) {
                  for (int i = -length; i < 0; i++) {
                     for (int k = 0; k < height; k++) {
                        positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() - leftBound));
                        positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + rightBound));
                     }
                  }
               }

               for (int i = -length; i < 0; i++) {
                  for (int j = -leftBound + 1; j < rightBound; j++) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k, state.getRailZ() + j));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case WEST -> {
               List<BlockPos> positions = new ArrayList<>();
               if (HighwayBuilder.isMineAboveRails()) {
                  for (int i = length; i > 0; i--) {
                     for (int k = 0; k < height; k++) {
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, state.getRailZ() + leftBound));
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, state.getRailZ() - rightBound));
                     }
                  }
               }

               for (int i = length; i > 0; i--) {
                  for (int j = leftBound - 1; j > -rightBound; j--) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k, state.getRailZ() + j));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            default -> new BlockPos[0];
         };
      }
   }

   public static BlockPos[] getCardinalBehindFloor() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      int width = HighwayBuilder.getPavementWidth();
      int leftBound = 0;
      int rightBound = 0;
      if (HighwayBuilder.getHighwayDirection() == null) {
         return new BlockPos[0];
      } else {
         assert MeteorClient.mc.player != null;

         if (state.getRailX() == null) {
            state.setRailX(Integer.valueOf(MeteorClient.mc.player.getBlockX()));
         }

         if (state.getRailY() == null) {
            state.setRailY(Integer.valueOf(MeteorClient.mc.player.getBlockY()));
         }

         if (state.getRailY() == null) {
            state.setRailZ(Integer.valueOf(MeteorClient.mc.player.getBlockZ()));
         }

         if (width % 2 == 0) {
            leftBound = width / 2;
         }

         rightBound = leftBound - 1;
         if (width % 2 != 0) {
            leftBound = rightBound = (width - 1) / 2;
         }
         return switch (HighwayBuilder.getHighwayDirection()) {
            case NORTH -> {
               List<BlockPos> positions = new ArrayList<>();

               for (int i = 4; i <= 7; i++) {
                  int z = state.getRailZ() + i;

                  for (int j = -leftBound; j < rightBound; j++) {
                     positions.add(new BlockPos(state.getRailX() + j, state.getRailY() - 1, z));
                  }

                  if (HighwayBuilder.isPlaceRails()) {
                     positions.add(new BlockPos(state.getRailX() - leftBound - 1, state.getRailY(), z));
                     positions.add(new BlockPos(state.getRailX() + rightBound + 1, state.getRailY(), z));
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case SOUTH -> {
               List<BlockPos> positions = new ArrayList<>();

               for (int i = 4; i <= 7; i++) {
                  int z = state.getRailZ() - i;

                  for (int j = leftBound; j > -rightBound; j--) {
                     positions.add(new BlockPos(state.getRailX() + j, state.getRailY() - 1, z));
                  }

                  if (HighwayBuilder.isPlaceRails()) {
                     positions.add(new BlockPos(state.getRailX() + leftBound + 1, state.getRailY(), z));
                     positions.add(new BlockPos(state.getRailX() - rightBound - 1, state.getRailY(), z));
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case EAST -> {
               List<BlockPos> positions = new ArrayList<>();

               for (int i = 4; i <= 7; i++) {
                  int x = state.getRailX() - i;

                  for (int j = -leftBound; j < rightBound; j++) {
                     positions.add(new BlockPos(x, state.getRailY() - 1, state.getRailZ() + j));
                  }

                  if (HighwayBuilder.isPlaceRails()) {
                     positions.add(new BlockPos(x, state.getRailY(), state.getRailZ() - leftBound - 1));
                     positions.add(new BlockPos(x, state.getRailY(), state.getRailZ() + rightBound + 1));
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case WEST -> {
               List<BlockPos> positions = new ArrayList<>();

               for (int i = 4; i <= 7; i++) {
                  int x = state.getRailX() + i;

                  for (int j = leftBound; j > -rightBound; j--) {
                     positions.add(new BlockPos(x, state.getRailY() - 1, state.getRailZ() + j));
                  }

                  if (HighwayBuilder.isPlaceRails()) {
                     positions.add(new BlockPos(x, state.getRailY(), state.getRailZ() + leftBound + 1));
                     positions.add(new BlockPos(x, state.getRailY(), state.getRailZ() - rightBound - 1));
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            default -> new BlockPos[0];
         };
      }
   }

   public static BlockPos[] getCardinalAheadZone() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      int width = HighwayBuilder.getPavementWidth();
      int leftBound = 0;
      int rightBound = 0;
      if (HighwayBuilder.getHighwayDirection() == null) {
         return new BlockPos[0];
      } else {
         assert MeteorClient.mc.player != null;

         if (state.getRailX() == null) {
            state.setRailX(Integer.valueOf(MeteorClient.mc.player.getBlockX()));
         }

         if (state.getRailY() == null) {
            state.setRailY(Integer.valueOf(MeteorClient.mc.player.getBlockY()));
         }

         if (state.getRailZ() == null) {
            state.setRailZ(Integer.valueOf(MeteorClient.mc.player.getBlockZ()));
         }

         if (width % 2 == 0) {
            leftBound = width / 2 + 1;
         }

         rightBound = leftBound - 1;
         if (width % 2 != 0) {
            leftBound = rightBound = (width - 1) / 2 + 1;
         }

         int height = 0;
         if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.PAVE) {
            height = 3;
         }

         if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.DIG) {
            height = 4;
         }
         return switch (HighwayBuilder.getHighwayDirection()) {
            case NORTH -> {
               List<BlockPos> positions = new ArrayList<>();
               if (HighwayBuilder.isMineAboveRails()) {
                  for (int i = 0; i > -10; i--) {
                     for (int k = 0; k < height; k++) {
                        positions.add(new BlockPos(state.getRailX() - leftBound - 1, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() + rightBound + 1, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() - leftBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() + rightBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               for (int i = 0; i >= -10; i--) {
                  for (int j = -leftBound; j < rightBound; j++) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(state.getRailX() + j, state.getRailY() + k, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case SOUTH -> {
               List<BlockPos> positions = new ArrayList<>();
               if (HighwayBuilder.isMineAboveRails()) {
                  for (int i = 10; i >= 0; i--) {
                     for (int k = 0; k < height; k++) {
                        positions.add(new BlockPos(state.getRailX() + leftBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() - rightBound, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() + leftBound + 1, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                        positions.add(new BlockPos(state.getRailX() - rightBound - 1, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               for (int i = 10; i >= 0; i--) {
                  for (int j = leftBound; j > -rightBound; j--) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(state.getRailX() + j, state.getRailY() + k, MeteorClient.mc.player.getBlockZ() + i));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case EAST -> {
               List<BlockPos> positions = new ArrayList<>();

               for (int i = 10; i >= 0; i--) {
                  for (int k = 0; k < height; k++) {
                     positions.add(
                        new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() - leftBound - 1)
                     );
                     positions.add(
                        new BlockPos(
                           MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + rightBound + 1
                        )
                     );
                     positions.add(
                        new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() - leftBound)
                     );
                     positions.add(
                        new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, MeteorClient.mc.player.getBlockZ() + rightBound)
                     );
                  }
               }

               for (int i = 10; i >= 0; i--) {
                  for (int j = -leftBound + 1; j < rightBound; j++) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k, state.getRailZ() + j));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            case WEST -> {
               List<BlockPos> positions = new ArrayList<>();
               if (HighwayBuilder.isMineAboveRails()) {
                  for (int i = -10; i <= 0; i++) {
                     for (int k = 0; k < height; k++) {
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, state.getRailZ() + leftBound));
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, state.getRailZ() - rightBound));
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, state.getRailZ() + leftBound + 1));
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k + 1, state.getRailZ() - rightBound - 1));
                     }
                  }
               }

               for (int i = -10; i <= 0; i++) {
                  for (int j = leftBound; j > -rightBound; j--) {
                     for (int k = 0; k <= height; k++) {
                        positions.add(new BlockPos(MeteorClient.mc.player.getBlockX() + i, state.getRailY() + k, state.getRailZ() + j));
                     }
                  }
               }

               yield positions.toArray(new BlockPos[0]);
            }
            default -> new BlockPos[0];
         };
      }
   }

   public static BlockPos[] getDiagonalAheadZone() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      int width = HighwayBuilder.getPavementWidth();
      int leftBound = 0;
      int rightBound = 0;
      int height = 0;
      if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.PAVE) {
         height = 3;
      }

      if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.DIG) {
         height = 4;
      }

      if (width % 2 == 0) {
         leftBound = width / 2;
      }

      rightBound = leftBound - 1;
      if (width % 2 != 0) {
         leftBound = rightBound = (width - 1) / 2;
      }

      assert MeteorClient.mc.player != null;

      if (state.getRailX() == null) {
         state.setRailX(Integer.valueOf(MeteorClient.mc.player.getBlockX()));
      }

      if (state.getRailY() == null) {
         state.setRailY(Integer.valueOf(MeteorClient.mc.player.getBlockY()));
      }

      if (state.getRailZ() == null) {
         state.setRailZ(Integer.valueOf(MeteorClient.mc.player.getBlockZ()));
      }

      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();

      return switch (direction) {
         case NORTHWEST -> {
            List<BlockPos> positions = new ArrayList<>();

            for (int i = -8; i <= -3; i++) {
               for (int j = 0; j <= height; j++) {
                  for (int left = leftBound; left > 0; left--) {
                     positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + j, state.getRailZ() + left + i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + j, state.getRailZ() + leftBound + 1 + i));
                     positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + j, state.getRailZ() + leftBound + 2 + i));
                  }

                  for (int right = rightBound; right >= 0; right--) {
                     positions.add(new BlockPos(state.getRailX() + right + i, state.getRailY() + j, state.getRailZ() + i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() + rightBound + 1 + i, state.getRailY() + j, state.getRailZ() + i));
                     positions.add(new BlockPos(state.getRailX() + rightBound + 2 + i, state.getRailY() + j, state.getRailZ() + i));
                  }
               }
            }

            yield positions.toArray(new BlockPos[0]);
         }
         case NORTHEAST -> {
            List<BlockPos> positions = new ArrayList<>();

            for (int i = -8; i <= -3; i++) {
               for (int j = 0; j <= height; j++) {
                  for (int left = leftBound; left > 0; left--) {
                     positions.add(new BlockPos(state.getRailX() - i, state.getRailY() + j, state.getRailZ() + left + i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() - i, state.getRailY() + j, state.getRailZ() + leftBound + 1 + i));
                     positions.add(new BlockPos(state.getRailX() - i, state.getRailY() + j, state.getRailZ() + leftBound + 2 + i));
                  }

                  for (int right = rightBound; right >= 0; right--) {
                     positions.add(new BlockPos(state.getRailX() - right - i, state.getRailY() + j, state.getRailZ() + i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() - rightBound - 1 - i, state.getRailY() + j, state.getRailZ() + i));
                     positions.add(new BlockPos(state.getRailX() - rightBound - 2 - i, state.getRailY() + j, state.getRailZ() + i));
                  }
               }
            }

            yield positions.toArray(new BlockPos[0]);
         }
         case SOUTHEAST -> {
            List<BlockPos> positions = new ArrayList<>();

            for (int i = -8; i <= -3; i++) {
               for (int j = 0; j <= height; j++) {
                  for (int left = leftBound; left > 0; left--) {
                     positions.add(new BlockPos(state.getRailX() - i, state.getRailY() + j, state.getRailZ() - left - i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() - i, state.getRailY() + j, state.getRailZ() - leftBound - 1 - i));
                     positions.add(new BlockPos(state.getRailX() - i, state.getRailY() + j, state.getRailZ() - leftBound - 2 - i));
                  }

                  for (int right = rightBound; right >= 0; right--) {
                     positions.add(new BlockPos(state.getRailX() - right - i, state.getRailY() + j, state.getRailZ() - i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() - rightBound - 1 - i, state.getRailY() + j, state.getRailZ() - i));
                     positions.add(new BlockPos(state.getRailX() - rightBound - 2 - i, state.getRailY() + j, state.getRailZ() - i));
                  }
               }
            }

            yield positions.toArray(new BlockPos[0]);
         }
         case SOUTHWEST -> {
            List<BlockPos> positions = new ArrayList<>();

            for (int i = -8; i <= -3; i++) {
               for (int j = 0; j <= height; j++) {
                  for (int left = leftBound; left > 0; left--) {
                     positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + j, state.getRailZ() - left - i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + j, state.getRailZ() - leftBound - 1 - i));
                     positions.add(new BlockPos(state.getRailX() + i, state.getRailY() + j, state.getRailZ() - leftBound - 2 - i));
                  }

                  for (int right = rightBound; right >= 0; right--) {
                     positions.add(new BlockPos(state.getRailX() + right + i, state.getRailY() + j, state.getRailZ() - i));
                  }

                  if (j > 0) {
                     positions.add(new BlockPos(state.getRailX() + rightBound + 1 + i, state.getRailY() + j, state.getRailZ() - i));
                     positions.add(new BlockPos(state.getRailX() + rightBound + 2 + i, state.getRailY() + j, state.getRailZ() - i));
                  }
               }
            }

            yield positions.toArray(new BlockPos[0]);
         }
         default -> new BlockPos[0];
      };
   }
}

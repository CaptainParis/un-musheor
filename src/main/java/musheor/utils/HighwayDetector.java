package musheor.utils;

import musheor.modules.automation.HighwayBuilder;
import musheor.modules.automation.highway.HighwayNetwork;
import musheor.utils.HighwayUtils;
import musheor.utils.system.MusheorSystem;
import net.minecraft.util.math.BlockPos;

public class HighwayDetector {
   private static final double DIAGONAL_MATCH_RADIUS = 8.0 * Math.sqrt(2.0);

   public static musheor.utils.HighwayDetector.DetectedHighway detect(int px, int pz, int py, HighwayUtils.HighwayDirection dir) {
      return isCardinalDirection(dir) ? detectCardinal(px, pz, py, dir) : detectDiagonal(px, pz, py, dir);
   }

   private static boolean isCardinalDirection(HighwayUtils.HighwayDirection dir) {
      return dir == HighwayUtils.HighwayDirection.NORTH
         || dir == HighwayUtils.HighwayDirection.SOUTH
         || dir == HighwayUtils.HighwayDirection.EAST
         || dir == HighwayUtils.HighwayDirection.WEST;
   }

   private static musheor.utils.HighwayDetector.DetectedHighway detectCardinal(int px, int pz, int py, HighwayUtils.HighwayDirection dir) {
      boolean travelX = dir == HighwayUtils.HighwayDirection.EAST || dir == HighwayUtils.HighwayDirection.WEST;
      int perpCoord = travelX ? pz : px;
      double bestDist = 8.0;
      String bestId = null;
      String bestLabel = null;
      musheor.utils.HighwayDetector.Category bestCat = null;
      double bestAxis = 0.0;
      double d = Math.abs(perpCoord);
      if (d < bestDist) {
         bestDist = d;
         bestId = travelX ? (dir == HighwayUtils.HighwayDirection.EAST ? "+X" : "-X") : (dir == HighwayUtils.HighwayDirection.SOUTH ? "+Z" : "-Z");
         bestLabel = travelX
            ? (dir == HighwayUtils.HighwayDirection.EAST ? "+X (East)" : "-X (West)")
            : (dir == HighwayUtils.HighwayDirection.SOUTH ? "+Z (South)" : "-Z (North)");
         bestCat = musheor.utils.HighwayDetector.Category.AXIS;
         bestAxis = 0.0;
      }

      for (double dist : HighwayNetwork.ringDistances()) {
         for (double candidate : new double[]{dist, -dist}) {
            d = Math.abs(perpCoord - candidate);
            if (d < bestDist) {
               int parallelCoord = travelX ? px : pz;
               if (Math.abs(parallelCoord) <= dist + 8.0) {
                  bestDist = d;
                  bestId = "ring_" + formatIdNumber(dist);
                  bestLabel = formatDistance(dist) + " Ring";
                  bestCat = musheor.utils.HighwayDetector.Category.RING;
                  bestAxis = candidate;
               }
            }
         }
      }

      for (int n = 1; n * 5000 <= 50000; n++) {
         int pCoord = n * 5000;
         int nCoord = -(n * 5000 + 1);

         for (double candidatex : new double[]{pCoord, nCoord}) {
            d = Math.abs(perpCoord - candidatex);
            if (d < bestDist) {
               int parallelCoord = travelX ? px : pz;
               if (Math.abs(parallelCoord) <= 50008.0) {
                  bestDist = d;
                  long cLong = (long)(n * 5000) * (candidatex > 0.0 ? 1 : -1);
                  bestId = (travelX ? "grid_ns_" : "grid_ew_") + cLong;
                  bestLabel = (candidatex > 0.0 ? "+" : "-") + formatDistance(n * 5000) + (travelX ? " N/S Grid" : " E/W Grid");
                  bestCat = musheor.utils.HighwayDetector.Category.GRID;
                  bestAxis = candidatex;
               }
            }
         }
      }

      if (bestId == null) {
         return null;
      } else {
         int width = widthFor(bestCat);
         boolean isEvenWidth = width % 2 == 0;
         double alignX;
         double alignZ;
         int startX;
         int startZ;
         if (travelX) {
            alignX = px;
            alignZ = isEvenWidth ? bestAxis : bestAxis + 0.5;
            startX = px;
            startZ = isEvenWidth && dir == HighwayUtils.HighwayDirection.WEST ? (int)bestAxis - 1 : (int)bestAxis;
         } else {
            alignX = isEvenWidth ? bestAxis : bestAxis + 0.5;
            alignZ = pz;
            startX = isEvenWidth && dir == HighwayUtils.HighwayDirection.SOUTH ? (int)bestAxis - 1 : (int)bestAxis;
            startZ = pz;
         }

         return new musheor.utils.HighwayDetector.DetectedHighway(
            bestId, bestLabel, bestCat, HighwayBuilder.HighwayType.CARDINAL, dir, width, bestAxis, alignX, alignZ, startX, startZ
         );
      }
   }

   private static musheor.utils.HighwayDetector.DetectedHighway detectDiagonal(int px, int pz, int py, HighwayUtils.HighwayDirection dir) {
      boolean isNESW = dir == HighwayUtils.HighwayDirection.NORTHEAST || dir == HighwayUtils.HighwayDirection.SOUTHWEST;
      double diagVal = isNESW ? px + pz : px - pz;
      double bestDist = DIAGONAL_MATCH_RADIUS;
      String bestId = null;
      String bestLabel = null;
      musheor.utils.HighwayDetector.Category bestCat = null;
      double bestK = 0.0;
      double d = Math.abs(diagVal);
      if (d < bestDist) {
         bestDist = d;
         bestId = isNESW ? (dir == HighwayUtils.HighwayDirection.NORTHEAST ? "NE" : "SW") : (dir == HighwayUtils.HighwayDirection.NORTHWEST ? "NW" : "SE");
         bestLabel = isNESW
            ? (dir == HighwayUtils.HighwayDirection.NORTHEAST ? "NE Diagonal" : "SW Diagonal")
            : (dir == HighwayUtils.HighwayDirection.NORTHWEST ? "NW Diagonal" : "SE Diagonal");
         bestCat = musheor.utils.HighwayDetector.Category.DIAGONAL;
         bestK = 0.0;
      }

      for (double dist : HighwayNetwork.diamondDistances()) {
         for (double candidate : new double[]{dist, -dist}) {
            d = Math.abs(diagVal - candidate);
            if (d < bestDist) {
               bestDist = d;
               bestId = "diamond_" + formatIdNumber(dist);
               bestLabel = formatDistance(dist) + " Diamond";
               bestCat = musheor.utils.HighwayDetector.Category.DIAMOND;
               bestK = candidate;
            }
         }
      }

      if (bestId == null) {
         return null;
      } else {
         int width = widthFor(bestCat);
         int blockK = isNESW ? (int)bestK - 1 : (int)bestK;
         double cz;
         double cx;
         if (isNESW) {
            cx = (px - pz + blockK) / 2.0;
            cz = blockK - cx;
         } else {
            cx = (px + pz + blockK) / 2.0;
            cz = cx - blockK;
         }

         int startX_i = (int)Math.round(cx);
         int startZ_i = isNESW ? blockK - startX_i : startX_i - blockK;
         return new musheor.utils.HighwayDetector.DetectedHighway(
            bestId, bestLabel, bestCat, HighwayBuilder.HighwayType.DIAGONAL, dir, width, blockK, cx + 0.5, cz + 0.5, startX_i, startZ_i
         );
      }
   }

   private static int widthFor(musheor.utils.HighwayDetector.Category cat) {
      MusheorSystem m = MusheorSystem.Manager;
      if (m == null) {
         return 4;
      } else {
         return switch (cat) {
            case AXIS, RING -> m.cardinalHighwayWidth.get();
            case DIAGONAL, DIAMOND -> m.diagonalHighwayWidth.get();
            case GRID -> m.gridHighwayWidth.get();
         };
      }
   }

   public static boolean isCrossingHighway(BlockPos railPos, musheor.utils.HighwayDetector.DetectedHighway detected) {
      int rx = railPos.getX();
      int rz = railPos.getZ();
      MusheorSystem m = MusheorSystem.Manager;
      int cardinalW = m != null ? (Integer)m.cardinalHighwayWidth.get() : 4;
      int diagonalW = m != null ? (Integer)m.diagonalHighwayWidth.get() : 9;
      int gridW = m != null ? (Integer)m.gridHighwayWidth.get() : 3;
      int diagHalf = diagonalW / 2 + 1;
      boolean travelX = detected.direction == HighwayUtils.HighwayDirection.EAST || detected.direction == HighwayUtils.HighwayDirection.WEST;
      boolean travelZ = detected.direction == HighwayUtils.HighwayDirection.NORTH || detected.direction == HighwayUtils.HighwayDirection.SOUTH;
      boolean travelNESW = detected.direction == HighwayUtils.HighwayDirection.NORTHEAST || detected.direction == HighwayUtils.HighwayDirection.SOUTHWEST;
      boolean travelNWSE = detected.direction == HighwayUtils.HighwayDirection.NORTHWEST || detected.direction == HighwayUtils.HighwayDirection.SOUTHEAST;
      int cardinalHalf = cardinalW / 2;
      int gridHalf = gridW / 2;
      if (!travelZ) {
         if (inWidthBand(rx, 0, cardinalW)) {
            return true;
         }

         for (double dist : HighwayNetwork.ringDistances()) {
            if (Math.abs(rz) <= dist + cardinalHalf) {
               if (inWidthBand(rx, (int)dist, cardinalW)) {
                  return true;
               }

               if (inWidthBand(rx, -((int)dist), cardinalW)) {
                  return true;
               }
            }
         }

         if (Math.abs(rz) <= 50000 + gridHalf) {
            for (int n = 1; n * 5000 <= 50000; n++) {
               if (inWidthBand(rx, n * 5000, gridW)) {
                  return true;
               }

               if (inWidthBand(rx, -(n * 5000 + 1), gridW)) {
                  return true;
               }
            }
         }
      }

      if (!travelX) {
         if (inWidthBand(rz, 0, cardinalW)) {
            return true;
         }

         for (double distx : HighwayNetwork.ringDistances()) {
            if (Math.abs(rx) <= distx + cardinalHalf) {
               if (inWidthBand(rz, (int)distx, cardinalW)) {
                  return true;
               }

               if (inWidthBand(rz, -((int)distx), cardinalW)) {
                  return true;
               }
            }
         }

         if (Math.abs(rx) <= 50000 + gridHalf) {
            for (int n = 1; n * 5000 <= 50000; n++) {
               if (inWidthBand(rz, n * 5000, gridW)) {
                  return true;
               }

               if (inWidthBand(rz, -(n * 5000 + 1), gridW)) {
                  return true;
               }
            }
         }
      }

      if (!travelNESW) {
         int nesw = rx + rz;
         if (Math.abs(nesw + 1) <= diagHalf) {
            return true;
         }

         for (double distxx : HighwayNetwork.diamondDistances()) {
            if (Math.abs(nesw - ((int)distxx - 1)) <= diagHalf && rx >= 0 && rz >= 0) {
               return true;
            }

            if (Math.abs(nesw + (int)distxx + 1) <= diagHalf && rx <= 0 && rz <= 0) {
               return true;
            }
         }
      }

      if (!travelNWSE) {
         int nwse = rx - rz;
         if (Math.abs(nwse) <= diagHalf) {
            return true;
         }

         for (double distxx : HighwayNetwork.diamondDistances()) {
            if (Math.abs(nwse - (int)distxx) <= diagHalf && rx >= 0 && rz <= 0) {
               return true;
            }

            if (Math.abs(nwse + (int)distxx) <= diagHalf && rx <= 0 && rz >= 0) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean isOnDetectedHighway(BlockPos pos, musheor.utils.HighwayDetector.DetectedHighway detected) {
      if (detected.type == HighwayBuilder.HighwayType.CARDINAL) {
         boolean travelX = detected.direction == HighwayUtils.HighwayDirection.EAST || detected.direction == HighwayUtils.HighwayDirection.WEST;
         int perpCoord = travelX ? pos.getZ() : pos.getX();
         return inWidthBand(perpCoord, (int)detected.axisValue, detected.width);
      } else {
         boolean isNESW = detected.direction == HighwayUtils.HighwayDirection.NORTHEAST || detected.direction == HighwayUtils.HighwayDirection.SOUTHWEST;
         int K = (int)detected.axisValue;
         int u = isNESW ? pos.getX() + pos.getZ() - K : pos.getX() - pos.getZ() - K;
         int HW = (detected.width - 1) / 2;
         return Math.abs(u) <= HW;
      }
   }

   private static boolean inWidthBand(int coord, int center, int width) {
      int halfLeft = width / 2;
      int halfRight = width % 2 == 0 ? halfLeft - 1 : halfLeft;
      return coord >= center - halfLeft && coord <= center + halfRight;
   }

   public static String formatDistance(double d) {
      if (d >= 1000000.0) {
         return stripTrailingZeros(String.format("%.3f", d / 1000000.0)) + "M";
      } else {
         return d >= 1000.0 ? stripTrailingZeros(String.format("%.3f", d / 1000.0)) + "k" : String.valueOf((long)d);
      }
   }

   private static String stripTrailingZeros(String s) {
      return s.replaceAll("0+$", "").replaceAll("\\.$", "");
   }

   private static String formatIdNumber(double d) {
      long l = (long)d;
      return l == d ? String.valueOf(l) : String.valueOf(d);
   }

   public static class DetectedHighway {
      public final String id;
      public final String label;
      public final musheor.utils.HighwayDetector.Category category;
      public final HighwayBuilder.HighwayType type;
      public final HighwayUtils.HighwayDirection direction;
      public final int width;
      public final double axisValue;
      public final double alignX;
      public final double alignZ;
      public final int startX;
      public final int startZ;

      public DetectedHighway(
         String id,
         String label,
         musheor.utils.HighwayDetector.Category category,
         HighwayBuilder.HighwayType type,
         HighwayUtils.HighwayDirection direction,
         int width,
         double axisValue,
         double alignX,
         double alignZ,
         int startX,
         int startZ
      ) {
         this.id = id;
         this.label = label;
         this.category = category;
         this.type = type;
         this.direction = direction;
         this.width = width;
         this.axisValue = axisValue;
         this.alignX = alignX;
         this.alignZ = alignZ;
         this.startX = startX;
         this.startZ = startZ;
      }
   }

   public static enum Category {
      AXIS,
      RING,
      DIAGONAL,
      DIAMOND,
      GRID;
   }
}

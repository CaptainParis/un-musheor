package musheor.modules.automation.highway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import musheor.utils.HighwayUtils;
import musheor.utils.system.HighwayNetworkManager;

public class HighwayNetwork {
   public static final List<HighwayNetwork.Highway> HIGHWAYS = buildHighways();
   public static final List<HighwayNetwork.Intersection> INTERSECTIONS = buildIntersections(HIGHWAYS);
   public static final List<HighwayNetwork.Edge> EDGES = buildEdges(HIGHWAYS, INTERSECTIONS);

   public static double[] ringDistances() {
      return HighwayNetworkManager.getInstance().getRingDistances("2b2t");
   }

   public static double[] diamondDistances() {
      return HighwayNetworkManager.getInstance().getDiamondDistances("2b2t");
   }

   private static List<HighwayNetwork.Highway> buildHighways() {
      List<HighwayNetwork.Highway> h = new ArrayList<>();
      h.add(HighwayNetwork.Highway.ofAxisX());
      h.add(HighwayNetwork.Highway.ofAxisZ());
      h.add(HighwayNetwork.Highway.ofDiagXZ());
      h.add(HighwayNetwork.Highway.ofDiagXNegZ());

      for (double d : ringDistances()) {
         h.add(HighwayNetwork.Highway.ofRingZ(d));
         h.add(HighwayNetwork.Highway.ofRingZ(-d));
         h.add(HighwayNetwork.Highway.ofRingX(d));
         h.add(HighwayNetwork.Highway.ofRingX(-d));
      }

      for (double d : diamondDistances()) {
         h.add(HighwayNetwork.Highway.ofDiamond(true, d));
         h.add(HighwayNetwork.Highway.ofDiamond(true, -d));
         h.add(HighwayNetwork.Highway.ofDiamond(false, d));
         h.add(HighwayNetwork.Highway.ofDiamond(false, -d));
      }

      for (int i = 1; i <= 10; i++) {
         double coord = i * 5000.0 + 0.5;
         h.add(HighwayNetwork.Highway.ofGridZ(coord));
         h.add(HighwayNetwork.Highway.ofGridZ(-coord));
         h.add(HighwayNetwork.Highway.ofGridX(coord));
         h.add(HighwayNetwork.Highway.ofGridX(-coord));
      }

      return Collections.unmodifiableList(h);
   }

   private static List<HighwayNetwork.Intersection> buildIntersections(List<HighwayNetwork.Highway> highways) {
      Map<String, List<HighwayNetwork.Highway>> byPos = new LinkedHashMap<>();

      for (int i = 0; i < highways.size(); i++) {
         for (int j = i + 1; j < highways.size(); j++) {
            HighwayNetwork.Highway a = highways.get(i);
            HighwayNetwork.Highway b = highways.get(j);
            HighwayUtils.XZ pt = intersectionOf(a, b);
            if (pt != null && a.contains(pt.x(), pt.z(), 1.0) && b.contains(pt.x(), pt.z(), 1.0)) {
               String key = posKey(pt);
               byPos.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
               byPos.get(key).add(b);
            }
         }
      }

      List<HighwayNetwork.Intersection> nodes = new ArrayList<>();

      for (Entry<String, List<HighwayNetwork.Highway>> entry : byPos.entrySet()) {
         HighwayUtils.XZ pos = parseKey(entry.getKey());
         List<HighwayNetwork.Highway> unique = entry.getValue().stream().distinct().toList();
         nodes.add(new HighwayNetwork.Intersection(pos, unique));
      }

      return Collections.unmodifiableList(nodes);
   }

   private static List<HighwayNetwork.Edge> buildEdges(List<HighwayNetwork.Highway> highways, List<HighwayNetwork.Intersection> nodes) {
      List<HighwayNetwork.Edge> edges = new ArrayList<>();

      for (HighwayNetwork.Highway hw : highways) {
         List<Integer> onHw = new ArrayList<>();

         for (int i = 0; i < nodes.size(); i++) {
            HighwayUtils.XZ p = nodes.get(i).pos();
            if (hw.contains(p.x(), p.z(), 1.0)) {
               onHw.add(i);
            }
         }

         onHw.sort((a, b) -> {
            HighwayUtils.XZ pa = nodes.get(a).pos();
            HighwayUtils.XZ pb = nodes.get(b).pos();
            double ca = sortCoord(hw, pa);
            double cb = sortCoord(hw, pb);
            return Double.compare(ca, cb);
         });

         for (int k = 0; k < onHw.size() - 1; k++) {
            int from = onHw.get(k);
            int to = onHw.get(k + 1);
            HighwayUtils.XZ pf = nodes.get(from).pos();
            HighwayUtils.XZ pt = nodes.get(to).pos();
            double dist = distance(pf, pt);
            edges.add(new HighwayNetwork.Edge(from, to, dist, hw));
            edges.add(new HighwayNetwork.Edge(to, from, dist, hw));
         }
      }

      return Collections.unmodifiableList(edges);
   }

   static HighwayUtils.XZ intersectionOf(HighwayNetwork.Highway a, HighwayNetwork.Highway b) {
      double[] la = lineCoeffs(a);
      double[] lb = lineCoeffs(b);
      if (la != null && lb != null) {
         double det = la[0] * lb[1] - lb[0] * la[1];
         if (Math.abs(det) < 1.0E-9) {
            return null;
         } else {
            double x = (la[2] * lb[1] - lb[2] * la[1]) / det;
            double z = (la[0] * lb[2] - lb[0] * la[2]) / det;
            return new HighwayUtils.XZ(x, z);
         }
      } else {
         return null;
      }
   }

   private static double[] lineCoeffs(HighwayNetwork.Highway hw) {
      return switch (hw.type()) {
         case AXIS, AXIS_BOUNDED, GRID -> hw.fixedAxisIsZ() ? new double[]{0.0, 1.0, hw.fixedCoord()} : new double[]{1.0, 0.0, hw.fixedCoord()};
         case DIAGONAL -> hw.fixedAxisIsZ() ? new double[]{1.0, 1.0, hw.fixedCoord()} : new double[]{1.0, -1.0, hw.fixedCoord()};
         case DIAGONAL_BOUNDED -> hw.fixedAxisIsZ() ? new double[]{1.0, 1.0, hw.fixedCoord()} : new double[]{1.0, -1.0, hw.fixedCoord()};
      };
   }

   private static double sortCoord(HighwayNetwork.Highway hw, HighwayUtils.XZ p) {
      return hw.isDiagonalFamily() ? p.x() : (hw.fixedAxisIsZ() ? p.x() : p.z());
   }

   public static double distance(HighwayUtils.XZ a, HighwayUtils.XZ b) {
      double dx = a.x() - b.x();
      double dz = a.z() - b.z();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static String posKey(HighwayUtils.XZ p) {
      return Math.round(p.x()) + "," + Math.round(p.z());
   }

   private static HighwayUtils.XZ parseKey(String key) {
      String[] parts = key.split(",");
      return new HighwayUtils.XZ(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
   }

   public static List<Integer> nodeIndicesOn(HighwayNetwork.Highway hw) {
      List<Integer> result = new ArrayList<>();

      for (int i = 0; i < INTERSECTIONS.size(); i++) {
         HighwayUtils.XZ p = INTERSECTIONS.get(i).pos();
         if (hw.contains(p.x(), p.z(), 1.0)) {
            result.add(i);
         }
      }

      return result;
   }

   public static Map<Integer, List<HighwayNetwork.Edge>> adjacency() {
      Map<Integer, List<HighwayNetwork.Edge>> adj = new HashMap<>();

      for (int i = 0; i < INTERSECTIONS.size(); i++) {
         adj.put(i, new ArrayList<>());
      }

      for (HighwayNetwork.Edge e : EDGES) {
         adj.get(e.from()).add(e);
      }

      return adj;
   }

   public record Edge(int from, int to, double distance, HighwayNetwork.Highway highway) {}

   public record Highway(String name, HighwayNetwork.HighwayType type, boolean fixedAxisIsZ, double fixedCoord, double minBound, double maxBound) {
      static HighwayNetwork.Highway ofAxisX() {
         return new HighwayNetwork.Highway("+X/-X axis (Z=0)", HighwayNetwork.HighwayType.AXIS, true, 0.0, -Double.MAX_VALUE, Double.MAX_VALUE);
      }

      static HighwayNetwork.Highway ofAxisZ() {
         return new HighwayNetwork.Highway("+Z/-Z axis (X=0)", HighwayNetwork.HighwayType.AXIS, false, 0.0, -Double.MAX_VALUE, Double.MAX_VALUE);
      }

      static HighwayNetwork.Highway ofDiagXZ() {
         return new HighwayNetwork.Highway("diagonal (X=Z)", HighwayNetwork.HighwayType.DIAGONAL, false, 0.0, -Double.MAX_VALUE, Double.MAX_VALUE);
      }

      static HighwayNetwork.Highway ofDiagXNegZ() {
         return new HighwayNetwork.Highway("diagonal (X=-Z)", HighwayNetwork.HighwayType.DIAGONAL, true, 0.0, -Double.MAX_VALUE, Double.MAX_VALUE);
      }

      static HighwayNetwork.Highway ofRingZ(double coord) {
         double limit = Math.abs(coord);
         return new HighwayNetwork.Highway(String.format("ring road Z=%.0f", coord), HighwayNetwork.HighwayType.AXIS_BOUNDED, true, coord, -limit, limit);
      }

      static HighwayNetwork.Highway ofRingX(double coord) {
         double limit = Math.abs(coord);
         return new HighwayNetwork.Highway(String.format("ring road X=%.0f", coord), HighwayNetwork.HighwayType.AXIS_BOUNDED, false, coord, -limit, limit);
      }

      static HighwayNetwork.Highway ofDiamond(boolean isSum, double K) {
         double minX = Math.min(0.0, K);
         double maxX = Math.max(0.0, K);
         String eq = isSum ? String.format("diamond X+Z=%.0f", K) : String.format("diamond X-Z=%.0f", K);
         return new HighwayNetwork.Highway(eq, HighwayNetwork.HighwayType.DIAGONAL_BOUNDED, isSum, K, minX, maxX);
      }

      static HighwayNetwork.Highway ofGridZ(double coord) {
         return new HighwayNetwork.Highway(String.format("grid Z=%.1f", coord), HighwayNetwork.HighwayType.GRID, true, coord, -50000.5, 50000.5);
      }

      static HighwayNetwork.Highway ofGridX(double coord) {
         return new HighwayNetwork.Highway(String.format("grid X=%.1f", coord), HighwayNetwork.HighwayType.GRID, false, coord, -50000.5, 50000.5);
      }

      boolean isDiagonalFamily() {
         return this.type == HighwayNetwork.HighwayType.DIAGONAL || this.type == HighwayNetwork.HighwayType.DIAGONAL_BOUNDED;
      }

      boolean contains(double x, double z, double tol) {
         return switch (this.type) {
            case AXIS, AXIS_BOUNDED, GRID -> this.fixedAxisIsZ
               ? Math.abs(z - this.fixedCoord) <= tol && x >= this.minBound - tol && x <= this.maxBound + tol
               : Math.abs(x - this.fixedCoord) <= tol && z >= this.minBound - tol && z <= this.maxBound + tol;
            case DIAGONAL -> {
               double val = this.fixedAxisIsZ ? x + z : x - z;
               yield Math.abs(val - this.fixedCoord) <= tol;
            }
            case DIAGONAL_BOUNDED -> {
               double val = this.fixedAxisIsZ ? x + z : x - z;
               yield Math.abs(val - this.fixedCoord) <= tol && x >= this.minBound - tol && x <= this.maxBound + tol;
            }
         };
      }

      double clamp(double coord) {
         return Math.max(this.minBound, Math.min(this.maxBound, coord));
      }

      HighwayUtils.XZ project(double x, double z) {
         return switch (this.type) {
            case AXIS, AXIS_BOUNDED, GRID -> {
               if (this.fixedAxisIsZ) {
                  double cx = this.clamp(x);
                  yield new HighwayUtils.XZ(cx, this.fixedCoord);
               } else {
                  double cz = this.clamp(z);
                  yield new HighwayUtils.XZ(this.fixedCoord, cz);
               }
            }
            case DIAGONAL -> {
               if (this.fixedAxisIsZ) {
                  double px = (x - z) / 2.0;
                  double pz = -px;
                  yield new HighwayUtils.XZ(px, pz);
               } else {
                  double px = (x + z) / 2.0;
                  yield new HighwayUtils.XZ(px, px);
               }
            }
            case DIAGONAL_BOUNDED -> {
               boolean isSum = this.fixedAxisIsZ;
               double cx = isSum ? (x - z + this.fixedCoord) / 2.0 : (x + z + this.fixedCoord) / 2.0;
               cx = this.clamp(cx);
               double cz = isSum ? this.fixedCoord - cx : cx - this.fixedCoord;
               yield new HighwayUtils.XZ(cx, cz);
            }
         };
      }
   }

   public static enum HighwayType {
      AXIS,
      DIAGONAL,
      AXIS_BOUNDED,
      DIAGONAL_BOUNDED,
      GRID;
   }

   public record Intersection(HighwayUtils.XZ pos, List<HighwayNetwork.Highway> highways) {}
}

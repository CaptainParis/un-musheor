package musheor.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import musheor.utils.HighwayUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class ItemFrameMapHelper {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final ItemFrameMapHelper INSTANCE = new ItemFrameMapHelper();
   private ItemFrameMapHelper.MapOrigin origin = ItemFrameMapHelper.MapOrigin.FRONT;
   private BlockPos originPos;
   private Direction facing;
   private Direction columnDir;
   private Direction rowDir;
   private int width;
   private int height;
   private int indexBase;
   private final Map<Long, ItemFrameMapHelper.MapCell> cells = new LinkedHashMap<>();
   private final List<String> unmatchedNames = new ArrayList<>();
   private static final Pattern DIGITS = Pattern.compile("\\d{1,4}");
   private static final Pattern BRACKETS = Pattern.compile("[(\\[{]([^)\\]}]*)[)\\]}]");

   public static ItemFrameMapHelper get() {
      return INSTANCE;
   }

   public boolean isReady() {
      return this.originPos != null && this.facing != null;
   }

   public void setOrigin(ItemFrameEntity frame, ItemFrameMapHelper.MapOrigin origin, float playerYaw) {
      this.origin = origin;
      this.originPos = frame.getAttachedBlockPos();
      this.facing = frame.getHorizontalFacing();
      if (this.facing.getAxis().isVertical()) {
         Direction away = HighwayUtils.directionFromLook(playerYaw, 0.0F);
         this.rowDir = origin == ItemFrameMapHelper.MapOrigin.FRONT ? away : away.getOpposite();
         this.columnDir = this.rowDir.rotateYClockwise();
         if (this.facing == Direction.DOWN) {
            this.columnDir = this.columnDir.getOpposite();
         }
      } else {
         this.columnDir = this.facing.rotateYCounterclockwise();
         this.rowDir = origin == ItemFrameMapHelper.MapOrigin.FRONT ? Direction.DOWN : Direction.UP;
      }

      this.cells.clear();
      this.unmatchedNames.clear();
   }

   public void clear() {
      this.originPos = null;
      this.facing = null;
      this.columnDir = null;
      this.rowDir = null;
      this.cells.clear();
      this.unmatchedNames.clear();
   }

   public ItemFrameMapHelper.MapOrigin getOrigin() {
      return this.origin;
   }

   public BlockPos getOriginPos() {
      return this.originPos;
   }

   public Direction getFacing() {
      return this.facing;
   }

   public Direction getColumnDir() {
      return this.columnDir;
   }

   public Direction getRowDir() {
      return this.rowDir;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getIndexBase() {
      return this.indexBase;
   }

   public void setSize(int width, int height) {
      this.width = Math.max(0, width);
      this.height = Math.max(0, height);
      this.cells
         .values()
         .removeIf(cell -> this.width > 0 && cell.col >= this.width || this.height > 0 && cell.row >= this.height);
   }

   public void setIndexBase(int indexBase) {
      this.indexBase = indexBase;
   }

   public int scanFrames() {
      if (this.isReady() && mc.world != null) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity frame && frame.getHorizontalFacing() == this.facing) {
               Vec3i delta = frame.getAttachedBlockPos().subtract(this.originPos);
               if (project(delta, this.facing) == 0) {
                  int col = project(delta, this.columnDir);
                  int row = project(delta, this.rowDir);
                  if (col >= 0 && row >= 0 && (this.width <= 0 || col < this.width) && (this.height <= 0 || row < this.height)) {
                     ItemFrameMapHelper.MapCell cell = this.cells
                        .computeIfAbsent(cellKey(col, row), k -> new ItemFrameMapHelper.MapCell(col, row, frame.getAttachedBlockPos()));
                     cell.entityId = frame.getId();
                     cell.filled = !frame.getHeldItemStack().isEmpty();
                  }
               }
            }
         }

         return this.cells.size();
      } else {
         return this.cells.size();
      }
   }

   public void clearPlacementMarks() {
      for (ItemFrameMapHelper.MapCell cell : this.cells.values()) {
         cell.clearPlacedMark();
      }
   }

   public void refreshFilled() {
      for (ItemFrameMapHelper.MapCell cell : this.cells.values()) {
         ItemFrameEntity frame = cell.getFrame();
         if (frame != null) {
            cell.filled = !frame.getHeldItemStack().isEmpty();
         }
      }
   }

   public Collection<ItemFrameMapHelper.MapCell> getCells() {
      return this.cells.values();
   }

   public int getCellCount() {
      return this.cells.size();
   }

   public long getFilledCount() {
      return this.cells.values().stream().filter(c -> c.filled).count();
   }

   public boolean isComplete() {
      return !this.cells.isEmpty() && this.cells.values().stream().allMatch(c -> c.filled);
   }

   public List<ItemFrameMapHelper.MapCell> getPendingCells() {
      List<ItemFrameMapHelper.MapCell> pending = new ArrayList<>();

      for (ItemFrameMapHelper.MapCell cell : this.cells.values()) {
         if (!cell.filled) {
            pending.add(cell);
         }
      }

      if (mc.player != null) {
         Vec3d eye = mc.player.getEyePos();
         pending.sort(Comparator.comparingDouble(cellx -> Vec3d.ofCenter(cellx.pos).squaredDistanceTo(eye)));
      }

      return pending;
   }

   public void matchInventoryMaps() {
      for (ItemFrameMapHelper.MapCell cell : this.cells.values()) {
         cell.inventorySlot = -1;
      }

      this.unmatchedNames.clear();
      if (mc.player != null) {
         for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.getItem() == Items.FILLED_MAP) {
               String name = stack.getName().getString();
               ItemFrameMapHelper.MapIndex target = parseMapIndex(name);
               long key = target == null ? Long.MIN_VALUE : this.resolveCellKey(target);
               ItemFrameMapHelper.MapCell cell = key == Long.MIN_VALUE ? null : this.cells.get(key);
               if (cell != null && !cell.filled && cell.inventorySlot < 0) {
                  cell.inventorySlot = slot;
               } else if (cell == null) {
                  this.unmatchedNames.add(name);
               }
            }
         }
      }
   }

   public List<String> getUnmatchedNames() {
      return this.unmatchedNames;
   }

   public static ItemFrameMapHelper.MapIndex parseMapIndex(String name) {
      if (name != null && !name.isBlank()) {
         Integer bracketedIndex = null;
         Matcher groups = BRACKETS.matcher(name);

         while (groups.find()) {
            List<Integer> nums = extractNumbers(groups.group(1));
            if (nums.size() >= 2) {
               return new ItemFrameMapHelper.MapIndex.Coord(nums.get(0), nums.get(1));
            }

            if (nums.size() == 1 && bracketedIndex == null) {
               bracketedIndex = nums.getFirst();
            }
         }

         if (bracketedIndex != null) {
            return new ItemFrameMapHelper.MapIndex.Flat(bracketedIndex);
         } else {
            List<Integer> numsx = extractNumbers(name);
            if (numsx.size() >= 2) {
               return new ItemFrameMapHelper.MapIndex.Coord(numsx.get(numsx.size() - 2), numsx.getLast());
            } else {
               return numsx.size() == 1 ? new ItemFrameMapHelper.MapIndex.Flat(numsx.getFirst()) : null;
            }
         }
      } else {
         return null;
      }
   }

   private static List<Integer> extractNumbers(String text) {
      List<Integer> nums = new ArrayList<>();
      Matcher matcher = DIGITS.matcher(text);

      while (matcher.find()) {
         nums.add(Integer.parseInt(matcher.group()));
      }

      return nums;
   }

   private long resolveCellKey(ItemFrameMapHelper.MapIndex target) {
      int col;
      int row;
      if (target instanceof ItemFrameMapHelper.MapIndex.Coord grid) {
         col = grid.col() - this.indexBase;
         row = grid.row() - this.indexBase;
      } else {
         if (this.width <= 0) {
            return Long.MIN_VALUE;
         }

         int index = ((ItemFrameMapHelper.MapIndex.Flat)target).index() - this.indexBase;
         if (index < 0) {
            return Long.MIN_VALUE;
         }

         col = index % this.width;
         row = index / this.width;
      }

      if (col < 0 || row < 0) {
         return Long.MIN_VALUE;
      } else if (this.width > 0 && col >= this.width) {
         return Long.MIN_VALUE;
      } else {
         return this.height > 0 && row >= this.height ? Long.MIN_VALUE : cellKey(col, row);
      }
   }

   public String describeMapName(String name) {
      ItemFrameMapHelper.MapIndex target = parseMapIndex(name);
      if (target == null) {
         return "no numbers in the name";
      } else if (target instanceof ItemFrameMapHelper.MapIndex.Coord grid) {
         return "grid " + grid.col() + "," + grid.row();
      } else {
         int index = ((ItemFrameMapHelper.MapIndex.Flat)target).index();
         if (this.width <= 0) {
            return "index " + index + " (needs .mp size to resolve)";
         } else {
            int n = index - this.indexBase;
            return "index " + index + " -> grid " + (n % this.width + this.indexBase) + "," + (n / this.width + this.indexBase);
         }
      }
   }

   private static long cellKey(int col, int row) {
      return (long)col << 32 | row & 4294967295L;
   }

   private static int project(Vec3i vec, Direction dir) {
      return vec.getX() * dir.getOffsetX() + vec.getY() * dir.getOffsetY() + vec.getZ() * dir.getOffsetZ();
   }

   public static Vec3d frameSurfacePoint(BlockPos pos, Direction facing, double out) {
      double depth = 0.46875 - out;
      return Vec3d.ofCenter(pos).add(-facing.getOffsetX() * depth, -facing.getOffsetY() * depth, -facing.getOffsetZ() * depth);
   }

   public static double distanceToFrame(ItemFrameEntity frame) {
      if (mc.player == null) {
         return Double.MAX_VALUE;
      } else {
         Vec3d eye = mc.player.getEyePos();
         Box box = frame.getBoundingBox();
         double x = Math.max(box.minX, Math.min(eye.x, box.maxX));
         double y = Math.max(box.minY, Math.min(eye.y, box.maxY));
         double z = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
         return eye.distanceTo(new Vec3d(x, y, z));
      }
   }

   public static enum MapOrigin {
      FRONT,
      BACK;
   }

   public static final class MapCell {
      public final int col;
      public final int row;
      public final BlockPos pos;
      private int entityId = -1;
      private boolean filled;
      private int inventorySlot = -1;
      private int placedAtTick = Integer.MIN_VALUE;

      private MapCell(int col, int row, BlockPos pos) {
         this.col = col;
         this.row = row;
         this.pos = pos;
      }

      public boolean isFilled() {
         return this.filled;
      }

      public int getInventorySlot() {
         return this.inventorySlot;
      }

      public void markPlaced(int tick) {
         this.placedAtTick = tick;
      }

      public void clearPlacedMark() {
         this.placedAtTick = Integer.MIN_VALUE;
      }

      public boolean isWithinTimeout(int tick, int timeout) {
         if (this.placedAtTick == Integer.MIN_VALUE) {
            return false;
         } else {
            int elapsed = tick - this.placedAtTick;
            return elapsed >= 0 && elapsed < timeout;
         }
      }

      public ItemFrameEntity getFrame() {
         if (ItemFrameMapHelper.mc.world != null && this.entityId >= 0) {
            return ItemFrameMapHelper.mc.world.getEntityById(this.entityId) instanceof ItemFrameEntity frame && frame.isAlive() ? frame : null;
         } else {
            return null;
         }
      }

      public String formatCoord(int indexBase) {
         return this.col + indexBase + "," + (this.row + indexBase);
      }

      public int flatIndex(int width, int indexBase) {
         return width > 0 ? this.row * width + this.col + indexBase : -1;
      }
   }

   public sealed interface MapIndex permits ItemFrameMapHelper.MapIndex.Coord, ItemFrameMapHelper.MapIndex.Flat {
      public record Coord(int col, int row) implements ItemFrameMapHelper.MapIndex {
      }

      public record Flat(int index) implements ItemFrameMapHelper.MapIndex {
      }
   }
}

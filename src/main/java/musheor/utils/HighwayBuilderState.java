package musheor.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import musheor.utils.HighwayUtils;
import musheor.utils.system.MusheorSystem;
import net.minecraft.util.math.BlockPos;

public class HighwayBuilderState {
   private static final HighwayBuilderState INSTANCE = new HighwayBuilderState();
   private MusheorSystem.PlacementType placementType;
   private boolean unusedToggle;
   private HighwayUtils.HighwayDirection direction;
   private BlockPos originPos;
   private int startX;
   private int startZ;
   private Integer railX;
   private Integer railY;
   private Integer railZ;
   private Double alignX;
   private Double alignZ;
   private boolean unusedRestockFlag;
   private boolean needsRestock;
   private boolean inPlacementRange;
   private boolean isPlacing;
   private boolean hasMissingBlocks;
   private boolean suppressPlacement;
   private boolean unusedBusyFlag;
   private boolean restockingShulkerFromEchest;
   private boolean restockingFromInventory;
   private boolean postRestocking;
   private boolean echestFarmActive;
   private boolean echestFarmBusy;
   private musheor.utils.RestockType restockType;
   private BlockPos echestReturnPos;
   private BlockPos lavaTarget;
   private BlockPos lavaReturnPos;
   private BlockPos digReturnPos;
   private int tickCount;
   private int distanceTickCounter;
   private int pickaxeCount;
   private int silkPickaxeCount;
   private int obsidianPlaced;
   private int obsidianBroken;
   private int unusedBrokenCounter;
   private int netherrackBroken;
   private int enderChestBroken;
   private int placedPerHour;
   private int minedPerHour;
   private int totalBrokenPerHour;
   private int echestsPerHour;
   private int netherrackPerHour;
   private final List<BlockPos> unusedPosListA = new ArrayList<>();
   private final List<BlockPos> pathHistory = new ArrayList<>();
   private final List<BlockPos> unusedPosListB = new ArrayList<>();
   private final List<BlockPos> unusedPosListC = new ArrayList<>();
   private final Map<BlockPos, Integer> recentPlacementAttempts = new HashMap<>();
   private HighwayDetector.DetectedHighway detectedHighway;

   private HighwayBuilderState() {
   }

   public static HighwayBuilderState getInstance() {
      return INSTANCE;
   }
   public MusheorSystem.PlacementType getPlacementType() {
      return this.placementType != null ? this.placementType : (MusheorSystem.PlacementType)MusheorSystem.Manager.placementType.get();
   }

   public void setPlacementType(MusheorSystem.PlacementType val) {
      this.placementType = val;
   }

   public HighwayUtils.HighwayDirection getDirection() {
      return this.direction;
   }

   public void setDirection(HighwayUtils.HighwayDirection val) {
      this.direction = val;
   }

   public BlockPos getOriginPos() {
      return this.originPos;
   }

   public void setOriginPos(BlockPos val) {
      this.originPos = val;
   }

   public int getStartX() {
      return this.startX;
   }

   public void setStartX(int val) {
      this.startX = val;
   }

   public int getStartZ() {
      return this.startZ;
   }

   public void setStartZ(int val) {
      this.startZ = val;
   }

   public Integer getRailX() {
      return this.railX;
   }

   public void setRailX(Integer val) {
      this.railX = val;
   }

   public Integer getRailY() {
      return this.railY;
   }

   public void setRailY(Integer val) {
      this.railY = val;
   }

   public Integer getRailZ() {
      return this.railZ;
   }

   public void setRailZ(Integer val) {
      this.railZ = val;
   }

   public Double getAlignX() {
      return this.alignX;
   }

   public void setAlignX(Double val) {
      this.alignX = val;
   }

   public Double getAlignZ() {
      return this.alignZ;
   }

   public void setAlignZ(Double val) {
      this.alignZ = val;
   }

   public musheor.utils.RestockType getRestockType() {
      return this.restockType;
   }

   public void setRestockType(musheor.utils.RestockType val) {
      this.restockType = val;
   }

   public BlockPos getEchestReturnPos() {
      return this.echestReturnPos;
   }

   public void setEchestReturnPos(BlockPos val) {
      this.echestReturnPos = val;
   }

   public BlockPos getLavaTarget() {
      return this.lavaTarget;
   }

   public void setLavaTarget(BlockPos val) {
      this.lavaTarget = val;
   }

   public BlockPos getLavaReturnPos() {
      return this.lavaReturnPos;
   }

   public void setLavaReturnPos(BlockPos val) {
      this.lavaReturnPos = val;
   }

   public BlockPos getDigReturnPos() {
      return this.digReturnPos;
   }

   public void setDigReturnPos(BlockPos val) {
      this.digReturnPos = val;
   }

   public int getTickCount() {
      return this.tickCount;
   }

   public void setTickCount(int val) {
      this.tickCount = val;
   }

   public int getDistanceTickCounter() {
      return this.distanceTickCounter;
   }

   public void setDistanceTickCounter(int val) {
      this.distanceTickCounter = val;
   }

   public int getPickaxeCount() {
      return this.pickaxeCount;
   }

   public void setPickaxeCount(int val) {
      this.pickaxeCount = val;
   }

   public int getSilkPickaxeCount() {
      return this.silkPickaxeCount;
   }

   public void setSilkPickaxeCount(int val) {
      this.silkPickaxeCount = val;
   }

   public int getObsidianPlaced() {
      return this.obsidianPlaced;
   }

   public int getObsidianBroken() {
      return this.obsidianBroken;
   }

   public int getNetherrackBroken() {
      return this.netherrackBroken;
   }

   public int getEnderChestBroken() {
      return this.enderChestBroken;
   }

   public int getPlacedPerHour() {
      return this.placedPerHour;
   }

   public void setPlacedPerHour(int val) {
      this.placedPerHour = val;
   }

   public int getMinedPerHour() {
      return this.minedPerHour;
   }

   public void setMinedPerHour(int val) {
      this.minedPerHour = val;
   }

   public int getTotalBrokenPerHour() {
      return this.totalBrokenPerHour;
   }

   public void setTotalBrokenPerHour(int val) {
      this.totalBrokenPerHour = val;
   }

   public int getEchestsPerHour() {
      return this.echestsPerHour;
   }

   public void setEchestsPerHour(int val) {
      this.echestsPerHour = val;
   }

   public int getNetherrackPerHour() {
      return this.netherrackPerHour;
   }

   public void setNetherrackPerHour(int val) {
      this.netherrackPerHour = val;
   }

   public HighwayDetector.DetectedHighway getDetectedHighway() {
      return this.detectedHighway;
   }

   public void setDetectedHighway(HighwayDetector.DetectedHighway val) {
      this.detectedHighway = val;
   }

   public void setUnusedToggle(boolean val) {
      this.unusedToggle = val;
   }

   public void setUnusedRestockFlag(boolean val) {
      this.unusedRestockFlag = val;
   }

   public boolean needsRestock() {
      return this.needsRestock;
   }

   public void setNeedsRestock(boolean val) {
      this.needsRestock = val;
   }

   public boolean isInPlacementRange() {
      return this.inPlacementRange;
   }

   public void setInPlacementRange(boolean val) {
      this.inPlacementRange = val;
   }

   public boolean isPlacing() {
      return this.isPlacing;
   }

   public void setPlacing(boolean val) {
      this.isPlacing = val;
   }

   public boolean hasMissingBlocks() {
      return this.hasMissingBlocks;
   }

   public void setHasMissingBlocks(boolean val) {
      this.hasMissingBlocks = val;
   }

   public boolean isSuppressPlacement() {
      return this.suppressPlacement;
   }

   public void setSuppressPlacement(boolean val) {
      this.suppressPlacement = val;
   }

   public boolean isRestockingShulkerFromEchest() {
      return this.restockingShulkerFromEchest;
   }

   public void setRestockingShulkerFromEchest(boolean val) {
      this.restockingShulkerFromEchest = val;
   }

   public boolean isRestockingFromInventory() {
      return this.restockingFromInventory;
   }

   public void setRestockingFromInventory(boolean val) {
      this.restockingFromInventory = val;
   }

   public boolean isPostRestocking() {
      return this.postRestocking;
   }

   public void setPostRestocking(boolean val) {
      this.postRestocking = val;
   }

   public boolean isEchestFarmActive() {
      return this.echestFarmActive;
   }

   public void setEchestFarmActive(boolean val) {
      this.echestFarmActive = val;
   }

   public boolean isEchestFarmBusy() {
      return this.echestFarmBusy;
   }

   public void setEchestFarmBusy(boolean val) {
      this.echestFarmBusy = val;
   }

   public void incrementTickCount() {
      this.tickCount++;
   }

   public void incrementDistanceTickCounter() {
      this.distanceTickCounter++;
   }

   public void incrementObsidianPlaced() {
      this.obsidianPlaced++;
   }

   public void incrementObsidianBroken() {
      this.obsidianBroken++;
   }

   public void incrementNetherrackBroken() {
      this.netherrackBroken++;
   }

   public void incrementEnderChestBroken() {
      this.enderChestBroken++;
   }

   public List<BlockPos> getPathHistory() {
      return this.pathHistory;
   }

   public Map<BlockPos, Integer> getRecentPlacementAttempts() {
      return this.recentPlacementAttempts;
   }

   public void reset() {
      this.direction = null;
      this.originPos = null;
      this.startX = 0;
      this.startZ = 0;
      this.railX = null;
      this.railY = null;
      this.railZ = null;
      this.alignX = null;
      this.alignZ = null;
      this.unusedRestockFlag = false;
      this.needsRestock = false;
      this.inPlacementRange = false;
      this.isPlacing = false;
      this.hasMissingBlocks = false;
      this.suppressPlacement = false;
      this.unusedBusyFlag = false;
      this.restockingShulkerFromEchest = false;
      this.restockingFromInventory = false;
      this.postRestocking = false;
      this.echestFarmActive = false;
      this.echestFarmBusy = false;
      this.restockType = null;
      this.echestReturnPos = null;
      this.lavaTarget = null;
      this.lavaReturnPos = null;
      this.digReturnPos = null;
      this.tickCount = 0;
      this.distanceTickCounter = 0;
      this.pickaxeCount = 0;
      this.silkPickaxeCount = 0;
      this.obsidianPlaced = 0;
      this.obsidianBroken = 0;
      this.unusedBrokenCounter = 0;
      this.netherrackBroken = 0;
      this.enderChestBroken = 0;
      this.unusedPosListA.clear();
      this.pathHistory.clear();
      this.unusedPosListB.clear();
      this.unusedPosListC.clear();
      this.recentPlacementAttempts.clear();
      this.detectedHighway = null;
   }
}

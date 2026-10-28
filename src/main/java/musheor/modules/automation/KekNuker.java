package musheor.modules.automation;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.LitematicaHelper;
import musheor.modules.features.KekMine;
import musheor.modules.hud.HudInfoPlus;
import musheor.utils.HighwayPositions;
import musheor.utils.BlockRenderUtils;
import musheor.utils.HighwayUtils;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.BlockPos.Mutable;
import musheor.utils.BaritoneHelper;
import musheor.utils.HighwayBuilderState;
import musheor.utils.HighwayDetector;

public class KekNuker extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPathfind = this.settings.createGroup("Pathfind");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static KekNuker INSTANCE;
   public final Setting<KekNuker.NukerMode> nukerMode = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("nuker-mode")).defaultValue(KekNuker.NukerMode.NORMAL)).build());
   private final Setting<KekNuker.LayerType> layerType = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("layer-type"))
                     .description("Controls where blocks are allowed to be broken (layer handling)"))
                  .defaultValue(KekNuker.LayerType.ABOVE))
               .visible(() -> this.nukerMode.get() != KekNuker.NukerMode.BOUNCE))
            .build()
      );
   private final Setting<KekNuker.SortMode> sortMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("sort-mode")).description("Choose what order to mine the blocks in"))
               .defaultValue(KekNuker.SortMode.CLOSEST))
            .build()
      );
   public final Setting<KekNuker.Shape> shape2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("shape")).defaultValue(KekNuker.Shape.SPHERE))
               .visible(() -> this.nukerMode.get() != KekNuker.NukerMode.BOUNCE))
            .build()
      );
   public final Setting<Double> range2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("range"))
            .defaultValue(5.5)
            .min(2.0)
            .max(8.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Boolean> belowFeetLast = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("below-feet-last"))
                     .description("Makes it so the block you are standing on is only mined when all other blocks in range are broken"))
                  .defaultValue(false))
               .visible(() -> this.nukerMode.get() != KekNuker.NukerMode.BOUNCE))
            .build()
      );
   private final Setting<Boolean> ignoreAir = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("ignore-air"))
                     .description("Ignores air blocks in the schematic."))
                  .visible(() -> LitematicaHelper.isLoaded() && this.nukerMode.get() == KekNuker.NukerMode.HIGHWAY))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> avoidSpillingLiquids = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("avoid-spilling-liquids"))
                  .description("Prevents you from mining blocks that will cause liquid to flow everywhere"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> preventPacketKick = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("prevent-packet-kick"))
                  .description("+100000 aura setting"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Integer> blocksPerTick = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("blocks-per-tick"))
                  .description("Maximum blocks to try to break per tick. (Only for instant-breakable-blocks)"))
               .defaultValue(35))
            .min(1)
            .sliderRange(1, 100)
            .build()
      );
   private final Setting<KekNuker.ListMode> listMode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("list-mode")).description("Selection mode")).defaultValue(KekNuker.ListMode.BLACKLIST)).build());
   private final Setting<List<Block>> whitelist = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
                     .name("whitelist"))
                  .description("The blocks you want to mine."))
               .defaultValue(new Block[0])
               .visible(() -> this.listMode.get() == KekNuker.ListMode.WHITELIST))
            .build()
      );
   private final Setting<List<Block>> blacklist = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
                     .name("blacklist"))
                  .description("The blocks you don't want to mine."))
               .defaultValue(new Block[0])
               .visible(() -> this.listMode.get() == KekNuker.ListMode.ALL))
            .build()
      );
   private final Setting<Boolean> autoPathfind = this.sgPathfind
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("auto-pathfind"))
                     .description("Automatically pathfinds when no blocks are in range"))
                  .defaultValue(false))
               .visible(() -> this.nukerMode.get() != KekNuker.NukerMode.BOUNCE))
            .build()
      );
   private final Setting<Integer> pathfindChunkRange = this.sgPathfind
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("pathfind-chunk-range"))
                     .description("Maximum range in chunks the pathfinder can scan for more blocks to break"))
                  .defaultValue(4))
               .sliderMax(8)
               .visible(() -> this.nukerMode.get() != KekNuker.NukerMode.BOUNCE))
            .build()
      );
   private final Setting<Integer> chunksPerTick = this.sgPathfind
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("chunks-per-tick"))
                     .description(
                        "How many chunks to scan per tick when searching for more blocks. Higher values find targets faster at the cost of CPU per tick."
                     ))
                  .defaultValue(2))
               .min(1)
               .sliderMax(8)
               .visible(() -> this.nukerMode.get() != KekNuker.NukerMode.BOUNCE))
            .build()
      );
   private final Setting<Boolean> globalRendering = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("global-rendering"))
                  .defaultValue(true))
               .description("Synchronize rendering with Musheor-Tab"))
            .build()
      );
   private final Setting<SettingColor> color2 = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("color"))
                  .defaultValue(new SettingColor(Color.cyan))
                  .description("Custom color for rendering (lines / wireframe)"))
               .visible(() -> !(Boolean)this.globalRendering.get()))
            .build()
      );
   public static List<BlockPos> targetBlocks = new ArrayList<>();
   private static List<BlockPos> breakableBlocks = new ArrayList<>();
   public static final Map<BlockPos, Integer> breakAttemptTicks = new HashMap<>();
   private int tickCounter;
   private int pathfindRing = -1;
   private int pathfindIndex = 0;
   private BlockPos pathfindTarget = null;
   private BlockPos pathfindCandidate = null;
   private boolean wasBreaking = false;

   public KekNuker() {
      super(Musheor.AUTOMATION, "kek-nuker", "Mines blocks in radius of the player");
      INSTANCE = this;
   }

   public static boolean isBusy() {
      return !breakableBlocks.isEmpty() && INSTANCE.isActive() || !KekMine.INSTANCE.isIdle();
   }

   public static List<Block> getBlacklist() {
      return (List<Block>)INSTANCE.blacklist.get();
   }

   public void onActivate() {
      breakAttemptTicks.clear();
      this.tickCounter = 0;
      this.pathfindRing = -1;
      this.pathfindIndex = 0;
      this.pathfindTarget = null;
      this.pathfindCandidate = null;
      this.wasBreaking = false;
   }

   public void onDeactivate() {
      targetBlocks.clear();
      breakableBlocks.clear();
      KekMine.INSTANCE.breakQueue.clear();
      this.pathfindTarget = null;
      this.pathfindCandidate = null;
      this.pathfindRing = -1;
      if ((Boolean)this.autoPathfind.get() && (BaritoneHelper.isPathing() || BaritoneHelper.hasPath())) {
         BaritoneHelper.cancel();
      }
   }

   @EventHandler
   public void onTickPre(Pre event) {
      if (mc.player != null && mc.world != null) {
         if (!HighwayBuilder.isEating()) {
            this.tickCounter++;
            breakAttemptTicks.entrySet()
               .removeIf(
                  entry -> mc.world.getBlockState(entry.getKey()).isAir()
                     ? true
                     : this.tickCounter - entry.getValue() > (Integer)KekMine.INSTANCE.breakTimeout.get()
               );
            Module kekMine = Modules.get().get(KekMine.class);
            if (this.nukerMode.get() == KekNuker.NukerMode.NORMAL) {
               targetBlocks.clear();
               targetBlocks = collectInShape((KekNuker.Shape)this.shape2.get(), (Double)this.range2.get());
            } else if (this.nukerMode.get() == KekNuker.NukerMode.HIGHWAY) {
               if (!LitematicaHelper.isLoaded()) {
                  this.warning("Litematica is not installed. Install Litematica or switch to another mode.", new Object[0]);
                  this.toggle();
                  return;
               }

               targetBlocks.clear();
               targetBlocks.addAll(LitematicaHelper.get().getWrongSchematicBlocks((Double)this.range2.get(), (Boolean)this.ignoreAir.get()));
            } else if (this.nukerMode.get() == KekNuker.NukerMode.PRINTER) {
               targetBlocks.clear();
               targetBlocks.addAll(this.collectPrinterBlocks());
            }

            breakableBlocks.clear();

            for (BlockPos pos : targetBlocks) {
               BlockState state = mc.world.getBlockState(pos);
               if ((
                     this.nukerMode.get() == KekNuker.NukerMode.BOUNCE
                        || (this.layerType.get() != KekNuker.LayerType.ABOVE || pos.getY() >= mc.player.getBlockPos().getY())
                           && (this.layerType.get() != KekNuker.LayerType.BELOW || LitematicaHelper.get().isPositionInRenderLayer(pos))
                  )
                  && !state.isLiquid()
                  && !state.isAir()
                  && BlockUtils.canBreak(pos)
                  && (!(Boolean)this.avoidSpillingLiquids.get() || !HighwayUtils.hasAdjacentLiquid(pos))
                  && !(squaredDistanceToBlock(mc.player.getEyePos(), pos) > (Double)this.range2.get() * (Double)this.range2.get())
                  && (
                     this.listMode.get() == KekNuker.ListMode.BLACKLIST
                        || (this.listMode.get() != KekNuker.ListMode.WHITELIST || ((List)this.whitelist.get()).contains(state.getBlock()))
                           && (this.listMode.get() != KekNuker.ListMode.ALL || !((List)this.blacklist.get()).contains(state.getBlock()))
                  )) {
                  breakableBlocks.add(pos);
               }
            }

            this.sortTargets(breakableBlocks);
            if ((Boolean)this.belowFeetLast.get()) {
               BlockPos p = mc.player.getBlockPos();
               List<BlockPos> below = breakableBlocks.stream()
                  .filter(posx -> posx.getX() == p.getX() && posx.getZ() == p.getZ() && posx.getY() < p.getY())
                  .sorted(Comparator.comparingInt(Vec3i::getY).reversed())
                  .toList();
               breakableBlocks.removeAll(below);
               breakableBlocks.addAll(below);
            }

            if (breakableBlocks.isEmpty() && (Boolean)this.autoPathfind.get() && this.nukerMode.get() != KekNuker.NukerMode.BOUNCE) {
               if (this.wasBreaking) {
                  this.wasBreaking = false;
                  this.pathfindRing = -1;
                  this.pathfindIndex = 0;
                  this.pathfindTarget = null;
                  this.pathfindCandidate = null;
               }

               if (this.pathfindTarget != null) {
                  if (HighwayUtils.isWithinRange(this.pathfindTarget, (Double)this.range2.get())) {
                     BaritoneHelper.cancel();
                     this.pathfindTarget = null;
                     this.pathfindCandidate = null;
                     this.pathfindRing = 0;
                     this.pathfindIndex = 0;
                  } else if (!BaritoneHelper.isPathing()) {
                     BlockPos standPos = this.findStandPos(this.pathfindTarget);
                     if (standPos == null) {
                        this.pathfindTarget = null;
                        this.pathfindCandidate = null;
                     } else {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalNear(standPos, 1));
                     }
                  }
               } else {
                  if (this.pathfindRing == -1) {
                     this.pathfindRing = 0;
                     this.pathfindIndex = 0;
                  }

                  int maxChunkRadius = (Integer)this.pathfindChunkRange.get();
                  ChunkPos playerChunk = mc.player.getChunkPos();

                  for (int i = 0; i < this.chunksPerTick.get() && this.pathfindRing <= maxChunkRadius && this.pathfindTarget == null; i++) {
                     ChunkPos toScan = this.ringChunk(playerChunk, this.pathfindRing, this.pathfindIndex);
                     BlockPos candidate = this.findBreakableInChunk(toScan);
                     if (candidate != null
                        && (
                           this.pathfindCandidate == null
                              || mc.player.squaredDistanceTo(candidate.getX() + 0.5, candidate.getY() + 0.5, candidate.getZ() + 0.5)
                                 < mc.player
                                    .squaredDistanceTo(this.pathfindCandidate.getX() + 0.5, this.pathfindCandidate.getY() + 0.5, this.pathfindCandidate.getZ() + 0.5)
                        )) {
                        this.pathfindCandidate = candidate;
                     }

                     int ringSize = this.pathfindRing == 0 ? 1 : 8 * this.pathfindRing;
                     this.pathfindIndex++;
                     if (this.pathfindIndex >= ringSize) {
                        this.pathfindIndex = 0;
                        this.pathfindRing++;
                        if (this.pathfindCandidate != null) {
                           BlockPos standPos = this.findStandPos(this.pathfindCandidate);
                           if (standPos != null) {
                              this.pathfindTarget = this.pathfindCandidate;
                              BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalNear(standPos, 1));
                           }

                           this.pathfindCandidate = null;
                        }
                     }
                  }
               }
            }

            if (!breakableBlocks.isEmpty()) {
               this.wasBreaking = true;
               int count = 0;

               for (BlockPos posx : breakableBlocks) {
                  BlockState state = mc.world.getBlockState(posx);
                  if (HudInfoPlus.getGlobalPacketCount() >= ((Integer)MusheorSystem.Manager.globalPacketLimit.get()).intValue() * 0.9 && (Boolean)this.preventPacketKick.get()) {
                     break;
                  }

                  if (!breakAttemptTicks.containsKey(posx)) {
                     KekMine.MineContext ctx = new KekMine.MineContext(posx, mc.world.getBlockState(posx), true);
                     if (kekMine.isActive()) {
                        if (!KekMine.INSTANCE.hasQueuedBreaks()) {
                           Direction face = faceToward(mc.player.getEyePos(), posx);
                           if (ctx.isInstaBreak) {
                              KekMine.instantBreak(posx, face, false);
                              count++;
                           } else {
                              KekMine.INSTANCE.startBreak(posx, state, face, false);
                           }

                           breakAttemptTicks.put(posx, this.tickCounter);
                        }
                     } else {
                        KekMine.INSTANCE.swapToBestTool(posx);
                        BlockUtils.breakBlock(posx, true);
                        count++;
                        if (!BlockUtils.canInstaBreak(posx)) {
                           break;
                        }
                     }

                     if (count >= (Integer)this.blocksPerTick.get()) {
                        break;
                     }
                  }
               }

               if (kekMine.isActive() && !KekMine.INSTANCE.breakQueue.isEmpty()) {
                  List<BlockPos> queueList = new ArrayList<>(KekMine.INSTANCE.breakQueue);
                  this.sortTargets(queueList);
                  if ((Boolean)this.belowFeetLast.get()) {
                     BlockPos p = mc.player.getBlockPos();
                     List<BlockPos> below = queueList.stream()
                        .filter(posx -> posx.getX() == p.getX() && posx.getZ() == p.getZ() && posx.getY() < p.getY())
                        .sorted(Comparator.comparingInt(Vec3i::getY).reversed())
                        .toList();
                     queueList.removeAll(below);
                     queueList.addAll(below);
                  }

                  KekMine.INSTANCE.breakQueue.clear();
                  KekMine.INSTANCE.breakQueue.addAll(queueList);
               }
            }
         }
      }
   }

   private void sortTargets(List<BlockPos> list) {
      switch ((KekNuker.SortMode)this.sortMode.get()) {
         case NONE:
            list.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(Vec3d.of(p))));
            break;
         case CLOSEST:
            list.sort(Comparator.comparingDouble(p -> -mc.player.squaredDistanceTo(Vec3d.of(p))));
            break;
         case FURTHEST:
            list.sort(Comparator.comparingDouble(p -> -p.getY()));
            break;
         case TOP_DOWN:
            list.sort(Comparator.comparingDouble(Vec3i::getY));
            break;
         case BOTTOM_UP:
            this.sortByEfficiency(list);
            break;
         case RANDOM:
         case null:
            break;
      }
   }

   private void sortByEfficiency(List<BlockPos> list) {
      if (mc.player != null && mc.world != null && !list.isEmpty()) {
         Map<BlockPos, KekNuker.EffKey> keys = new HashMap<>();

         for (BlockPos pos : list) {
            BlockState state = mc.world.getBlockState(pos);
            boolean toolMatch = KekMine.isHoldingMatchingTool(state);
            KekMine.MineContext ctx = new KekMine.MineContext(pos, state, true);
            boolean insta = ctx.isInstaBreak;
            keys.put(pos, new KekNuker.EffKey(toolMatch, insta, ctx.getBreakSpeed()));
         }

         list.sort(
            Comparator.<BlockPos>comparingInt(p -> keys.get(p).instaBreak() ? 0 : 1)
               .thenComparingInt(p -> keys.get(p).toolMatch() ? 0 : 1)
               .thenComparingDouble(p -> -keys.get(p).breakSpeed())
         );
      }
   }

   public static boolean isBreakingHighwayZone() {
      HighwayUtils.HighwayDirection dir = HighwayBuilder.getHighwayDirection();
      if (dir == null) {
         return false;
      } else {
         boolean isCardinal = dir == HighwayUtils.HighwayDirection.NORTH
            || dir == HighwayUtils.HighwayDirection.SOUTH
            || dir == HighwayUtils.HighwayDirection.EAST
            || dir == HighwayUtils.HighwayDirection.WEST;
         BlockPos[] zone = isCardinal ? HighwayPositions.getCardinalTunnel(0, 2) : HighwayPositions.getDiagonalTunnel(0, 2);
         Set<BlockPos> zoneSet = new HashSet<>(Arrays.asList(zone));
         HighwayBuilderState state = HighwayBuilderState.getInstance();
         HighwayDetector.DetectedHighway detected = state.getDetectedHighway();
         if (detected != null && state.getRailY() != null) {
            int railY = state.getRailY();
            BlockPos[] railZone = isCardinal ? HighwayPositions.getCardinalFloor(0, 2, true, true) : HighwayPositions.getDiagonalFloor(0, 2, true, true);

            for (BlockPos pos : railZone) {
               if (pos.getY() == railY && HighwayDetector.isCrossingHighway(pos, detected)) {
                  zoneSet.add(pos);
               }
            }
         }

         BlockPos p = KekMine.INSTANCE.getPrimaryPos();
         if (p != null && zoneSet.contains(p)) {
            return true;
         } else {
            p = KekMine.INSTANCE.getParkedPos();
            if (p != null && zoneSet.contains(p)) {
               return true;
            } else {
               for (BlockPos posx : breakableBlocks) {
                  if (zoneSet.contains(posx)) {
                     return true;
                  }
               }

               return false;
            }
         }
      }
   }

   public static List<BlockPos> collectInShape(KekNuker.Shape shape, double radius) {
      List<BlockPos> positions = new ArrayList<>();
      Vec3d eyePos = mc.player.getEyePos();
      int blockRadius = (int)Math.ceil(radius);

      for (int x = -blockRadius; x < blockRadius; x++) {
         for (int y = -blockRadius; y < blockRadius; y++) {
            for (int z = -blockRadius; z < blockRadius; z++) {
               BlockPos pos = BlockPos.ofFloored(eyePos.x + x, eyePos.y + y, eyePos.z + z);
               if (BlockUtils.canBreak(pos)) {
                  if (shape == KekNuker.Shape.SPHERE) {
                     if (squaredDistanceToBlock(eyePos, pos) <= radius * radius) {
                        positions.add(pos);
                     }
                  } else if (shape == KekNuker.Shape.CUBE) {
                     positions.add(pos);
                  }
               }
            }
         }
      }

      return positions;
   }

   static double squaredDistanceToBlock(Vec3d eye, BlockPos pos) {
      double cx = Math.max((double)pos.getX(), Math.min(eye.x, pos.getX() + 1.0));
      double cy = Math.max((double)pos.getY(), Math.min(eye.y, pos.getY() + 1.0));
      double cz = Math.max((double)pos.getZ(), Math.min(eye.z, pos.getZ() + 1.0));
      double dx = eye.x - cx;
      double dy = eye.y - cy;
      double dz = eye.z - cz;
      return dx * dx + dy * dy + dz * dz;
   }

   public static Direction faceToward(Vec3d eye, BlockPos pos) {
      double dx = eye.x - (pos.getX() + 0.5);
      double dy = eye.y - (pos.getY() + 0.5);
      double dz = eye.z - (pos.getZ() + 0.5);
      double adx = Math.abs(dx);
      double ady = Math.abs(dy);
      double adz = Math.abs(dz);
      if (adx >= ady && adx >= adz) {
         return dx > 0.0 ? Direction.EAST : Direction.WEST;
      } else if (ady >= adz) {
         return dy > 0.0 ? Direction.UP : Direction.DOWN;
      } else {
         return dz > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private ChunkPos ringChunk(ChunkPos center, int radius, int index) {
      if (radius == 0) {
         return center;
      } else {
         int cx = center.x;
         int cz = center.z;
         if (index < 2 * radius + 1) {
            return new ChunkPos(cx - radius + index, cz - radius);
         } else {
            index -= 2 * radius + 1;
            if (index < 2 * radius) {
               return new ChunkPos(cx + radius, cz - radius + 1 + index);
            } else {
               index -= 2 * radius;
               if (index < 2 * radius) {
                  return new ChunkPos(cx + radius - 1 - index, cz + radius);
               } else {
                  index -= 2 * radius;
                  return new ChunkPos(cx - radius, cz + radius - 1 - index);
               }
            }
         }
      }
   }

   private BlockPos findBreakableInChunk(ChunkPos chunk) {
      if (mc.world == null || mc.player == null) {
         return null;
      } else if (!mc.world.isChunkLoaded(chunk.x, chunk.z)) {
         return null;
      } else {
         int minX = chunk.getStartX();
         int maxX = chunk.getEndX();
         int minZ = chunk.getStartZ();
         int maxZ = chunk.getEndZ();
         BlockPos closest = null;
         double closestDist = Double.MAX_VALUE;
         double px = mc.player.getX();
         double py = mc.player.getY();
         double pz = mc.player.getZ();
         if (this.nukerMode.get() == KekNuker.NukerMode.HIGHWAY) {
            if (!LitematicaHelper.isLoaded()) {
               return null;
            }

            int minY = mc.world.getBottomY();
            int maxY = mc.world.getBottomY() + mc.world.getHeight() - 1;

            for (BlockPos pos : LitematicaHelper.get()
               .getWrongBlocksInBox(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), !(Boolean)this.ignoreAir.get(), 64)) {
               BlockState state = mc.world.getBlockState(pos);
               if (this.passesLayerFilter(pos)
                  && BlockUtils.canBreak(pos)
                  && (!(Boolean)this.avoidSpillingLiquids.get() || !HighwayUtils.hasAdjacentLiquid(pos))
                  && this.passesListFilter(state)
                  && this.hasSupportBelow(pos, 8)) {
                  double dist = pos.getSquaredDistance(px, py, pz);
                  if (dist < closestDist) {
                     closestDist = dist;
                     closest = pos;
                  }
               }
            }
         } else {
            ISelection[] selections = null;
            if (this.nukerMode.get() == KekNuker.NukerMode.PRINTER) {
               selections = BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().getSelections();
               if (selections == null || selections.length == 0) {
                  return null;
               }
            }

            int feetY = mc.player.getBlockPos().getY();
            int r = (int)Math.ceil((Double)this.range2.get());
            int minY = this.layerType.get() == KekNuker.LayerType.ABOVE ? feetY : Math.max(mc.world.getBottomY(), feetY - r);
            int maxY = Math.min(mc.world.getBottomY() + mc.world.getHeight() - 1, feetY + r);
            Mutable mpos = new Mutable();

            for (int x = minX; x <= maxX; x++) {
               for (int z = minZ; z <= maxZ; z++) {
                  for (int y = minY; y <= maxY; y++) {
                     mpos.set(x, y, z);
                     BlockState state = mc.world.getBlockState(mpos);
                     if (!state.isAir()
                        && !state.isLiquid()
                        && BlockUtils.canBreak(mpos)
                        && (!(Boolean)this.avoidSpillingLiquids.get() || !HighwayUtils.hasAdjacentLiquid(mpos))
                        && this.passesLayerFilter(mpos)
                        && this.passesListFilter(state)
                        && (selections == null || inSelections(mpos, selections))
                        && this.hasSupportBelow(mpos, 8)) {
                        double dist = mpos.getSquaredDistance(px, py, pz);
                        if (dist < closestDist) {
                           closestDist = dist;
                           closest = mpos.toImmutable();
                        }
                     }
                  }
               }
            }
         }

         return closest;
      }
   }

   private BlockPos findStandPos(BlockPos candidate) {
      int playerY = mc.player.getBlockY();
      int r = (int)Math.ceil((Double)this.range2.get());
      double rangeSq = (Double)this.range2.get() * (Double)this.range2.get();
      int dy = playerY - candidate.getY();
      if (dy * dy > rangeSq) {
         return null;
      } else {
         BlockPos best = null;
         double bestDist = Double.MAX_VALUE;
         Mutable standMut = new Mutable();
         Mutable groundMut = new Mutable();

         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                  int sx = candidate.getX() + dx;
                  int sz = candidate.getZ() + dz;
                  groundMut.set(sx, playerY - 1, sz);
                  BlockState ground = mc.world.getBlockState(groundMut);
                  if (!ground.isAir() && !ground.isLiquid()) {
                     standMut.set(sx, playerY, sz);
                     double dist = standMut.getSquaredDistance(candidate);
                     if (dist < bestDist) {
                        bestDist = dist;
                        best = standMut.toImmutable();
                     }
                  }
               }
            }
         }

         return best;
      }
   }

   private boolean hasSupportBelow(BlockPos pos, int depth) {
      int bottom = mc.world.getBottomY();
      int x = pos.getX();
      int z = pos.getZ();
      Mutable check = new Mutable(x, 0, z);

      for (int dy = 1; dy <= depth; dy++) {
         int y = pos.getY() - dy;
         if (y <= bottom) {
            break;
         }

         check.set(x, y, z);
         BlockState s = mc.world.getBlockState(check);
         if (!s.isAir() && !s.isLiquid()) {
            return true;
         }
      }

      return false;
   }

   private boolean passesLayerFilter(BlockPos pos) {
      return this.layerType.get() == KekNuker.LayerType.ABOVE && pos.getY() < mc.player.getBlockPos().getY()
         ? false
         : this.layerType.get() != KekNuker.LayerType.BELOW || !LitematicaHelper.isLoaded() || LitematicaHelper.get().isPositionInRenderLayer(pos);
   }

   private boolean passesListFilter(BlockState state) {
      if (this.listMode.get() == KekNuker.ListMode.WHITELIST) {
         return ((List)this.whitelist.get()).contains(state.getBlock());
      } else {
         return this.listMode.get() == KekNuker.ListMode.ALL ? !((List)this.blacklist.get()).contains(state.getBlock()) : true;
      }
   }

   private static boolean inSelections(BlockPos pos, ISelection[] selections) {
      for (ISelection sel : selections) {
         BlockPos min = sel.min();
         BlockPos max = sel.max();
         if (pos.getX() >= min.getX()
            && pos.getX() <= max.getX()
            && pos.getY() >= min.getY()
            && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ()
            && pos.getZ() <= max.getZ()) {
            return true;
         }
      }

      return false;
   }

   private List<BlockPos> collectPrinterBlocks() {
      List<BlockPos> list = new ArrayList<>();
      if (mc.player != null && mc.world != null) {
         IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
         ISelectionManager selectionManager = baritone.getSelectionManager();
         if (selectionManager.getSelections() == null) {
            return list;
         } else {
            for (ISelection selection : selectionManager.getSelections()) {
               BlockPos min = selection.min();
               BlockPos max = selection.max();
               int regionMinX = Math.min(min.getX(), max.getX());
               int regionMinY = Math.min(min.getY(), max.getY());
               int regionMinZ = Math.min(min.getZ(), max.getZ());
               int regionMaxX = Math.max(min.getX(), max.getX());
               int regionMaxY = Math.max(min.getY(), max.getY());
               int regionMaxZ = Math.max(min.getZ(), max.getZ());
               int px = mc.player.getBlockX();
               int py = mc.player.getBlockY();
               int pz = mc.player.getBlockZ();
               int minX = Math.max(regionMinX, px - 12);
               int minY = Math.max(regionMinY, py - 12);
               int minZ = Math.max(regionMinZ, pz - 12);
               int maxX = Math.min(regionMaxX, px + 12);
               int maxY = Math.min(regionMaxY, py + 12);
               int maxZ = Math.min(regionMaxZ, pz + 12);
               if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
                  for (int x = minX; x <= maxX; x++) {
                     for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                           BlockPos pos = new BlockPos(x, y, z);
                           if (!mc.world.getBlockState(pos).isAir()
                              && !mc.world.getBlockState(pos).isLiquid()
                              && !list.contains(pos)
                              && HighwayUtils.isWithinRange(pos, (Double)this.range2.get())) {
                              list.add(pos);
                           }
                        }
                     }
                  }
               }
            }

            return list;
         }
      } else {
         return list;
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.world != null) {
         if ((Boolean)this.globalRendering.get()) {
            BlockRenderUtils.renderPositions(event, breakableBlocks);
         } else {
            BlockRenderUtils.renderPositions(
               event,
               breakableBlocks,
               (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
               (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
               ShapeMode.Lines
            );
         }
      }
   }

   private record EffKey(boolean toolMatch, boolean instaBreak, float breakSpeed) {
   }

   private static enum LayerType {
      ALL,
      ABOVE,
      BELOW;}

   public static enum ListMode {
      BLACKLIST,
      WHITELIST,
      ALL;}

   public static enum NukerMode {
      NORMAL,
      BOUNCE,
      HIGHWAY,
      PRINTER;}

   public static enum Shape {
      CUBE,
      SPHERE;}

   public static enum SortMode {
      NONE,
      CLOSEST,
      FURTHEST,
      TOP_DOWN,
      BOTTOM_UP,
      RANDOM;}
}

package musheor.modules.automation;

import java.util.List;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.utils.manager.RotationManager;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import musheor.utils.ItemFrameMapHelper;
import org.joml.Vector3d;

public class MapPlacer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTiming = this.settings.createGroup("Timing");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> placementRange = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("placement-range"))
               .description("How close a frame's hitbox has to be to your eyes before we try to fill it. The server's own limit is 3."))
            .defaultValue(3.0)
            .min(1.0)
            .sliderRange(1.0, 6.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<MapPlacer.RotateMode> rotate2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("rotate"))
                  .description("How to face the frame being filled. Packet keeps the camera still, Client turns it for real, None sends the interact as-is."))
               .defaultValue(MapPlacer.RotateMode.PACKET))
            .build()
      );
   private final Setting<Boolean> restoreSlot = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("restore-slot"))
                  .description(
                     "Selects the hotbar slot you were on again once placing stops. The swap itself can never be hidden — the server reads the map out of the slot we select."
                  ))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoScan = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-scan"))
                  .description("Keeps picking up more frames of the canvas as you walk, so .mp scan only has to be run once."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> pauseOnScreen = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("pause-on-screen"))
                  .description("Stops placing while any screen or inventory is open."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> disableWhenDone = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disable-when-done"))
                  .description("Turns the module off once every frame of the canvas holds a map."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> chatFeedback = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("chat-feedback"))
                  .description("Reports each placement in chat."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> placementDelay = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("placement-delay"))
                  .description("Ticks to wait between placements."))
               .defaultValue(1))
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Integer> swapDelay = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("swap-delay"))
                  .description("Ticks to wait after selecting a map before interacting. Raise this on high ping."))
               .defaultValue(1))
            .sliderRange(0, 10)
            .build()
      );
   private final Setting<Integer> scanInterval = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("scan-interval"))
                     .description("Ticks between rescans while auto-scan is on."))
                  .defaultValue(50))
               .sliderRange(1, 100)
               .visible(this.autoScan::get))
            .build()
      );
   private final Setting<Boolean> render2 = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render"))
                  .description("Draws the canvas so you can check every coordinate before you start placing."))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("shape-mode"))
                     .description("How the frame outlines are drawn."))
                  .defaultValue(ShapeMode.Lines))
               .visible(this.render2::get))
            .build()
      );
   private final Setting<Double> renderDistance = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("render-distance")).description("Only draw cells this close to you."))
               .defaultValue(32.0)
               .sliderRange(8.0, 128.0)
               .visible(this.render2::get))
            .build()
      );
   private final Setting<Boolean> showPlaced = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("show-placed"))
                     .description("Keep drawing frames that already hold a map. Off by default, so the render empties out as the mapart fills in."))
                  .defaultValue(false))
               .visible(this.render2::get))
            .build()
      );
   private final Setting<MapPlacer.LabelMode> labels2 = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("labels"))
                     .description("What to write on each frame: its grid coordinate, its row-major index, both, or nothing."))
                  .defaultValue(MapPlacer.LabelMode.BOTH))
               .visible(this.render2::get))
            .build()
      );
   private final Setting<Double> labelScale = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("label-scale")).description("Size of the labels."))
               .defaultValue(2.0)
               .sliderRange(0.25, 3.0)
               .decimalPlaces(2)
               .visible(() -> (Boolean)this.render2.get() && this.labels2.get() != MapPlacer.LabelMode.NONE))
            .build()
      );
   private final Setting<Boolean> labelDistanceScaling = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("label-distance-scaling"))
                     .description("Shrink labels with distance instead of keeping them one size on screen."))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.render2.get() && this.labels2.get() != MapPlacer.LabelMode.NONE))
            .build()
      );
   private final Setting<SettingColor> readyColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("ready-color"))
                  .description("A frame whose map is in your inventory."))
               .defaultValue(new SettingColor(25, 225, 125, 180))
               .visible(this.render2::get))
            .build()
      );
   private final Setting<SettingColor> missingColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("missing-color"))
                  .description("A frame whose map is NOT in your inventory."))
               .defaultValue(new SettingColor(225, 75, 50, 180))
               .visible(this.render2::get))
            .build()
      );
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("target-color"))
                  .description("The frame being filled right now."))
               .defaultValue(new SettingColor(255, 210, 40, 220))
               .visible(this.render2::get))
            .build()
      );
   private final Setting<SettingColor> placedColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("placed-color"))
                  .description("A frame that already holds a map."))
               .defaultValue(new SettingColor(120, 120, 120, 110))
               .visible(() -> (Boolean)this.render2.get() && (Boolean)this.showPlaced.get()))
            .build()
      );
   private static final ItemFrameMapHelper canvas = ItemFrameMapHelper.get();
   private static boolean externallyPaused;
   private int tickCounter;
   private int placementCooldown;
   private int swapCooldown;
   private int scanCooldown;
   private int savedHotbarSlot = -1;
   private float lastAimYaw;
   private float lastAimPitch;
   private int lastAimTick = Integer.MIN_VALUE;
   private ItemFrameMapHelper.MapCell currentCell;
   private MapPlacer.Status status = MapPlacer.Status.NO_CANVAS;

   public MapPlacer() {
      super(Musheor.AUTOMATION, "map-placer", "Places a mapart into a wall of item frames, reading each map's position out of its name. Set it up with .mp.");
   }

   public static MapPlacer get() {
      return (MapPlacer)Modules.get().get(MapPlacer.class);
   }

   public static void setExternallyPaused(boolean value) {
      externallyPaused = value;
   }

   public void onActivate() {
      this.tickCounter = 0;
      this.placementCooldown = 0;
      this.swapCooldown = 0;
      this.scanCooldown = 0;
      this.savedHotbarSlot = -1;
      this.lastAimTick = Integer.MIN_VALUE;
      this.currentCell = null;
      if (!canvas.isReady()) {
         this.error("No canvas — look at the mapart's corner frame and run §b.mp origin topleft§r first.", new Object[0]);
         this.toggle();
      } else {
         canvas.scanFrames();
         canvas.clearPlacementMarks();
         canvas.matchInventoryMaps();
         if (canvas.getCellCount() == 0) {
            this.warning("No item frames on the canvas yet. Walk closer so they load — auto-scan will pick them up.", new Object[0]);
         }
      }
   }

   public void onDeactivate() {
      RotationManager.release(10);
      this.restoreSavedSlot();
      this.currentCell = null;
      this.status = canvas.isReady() ? MapPlacer.Status.PAUSED : MapPlacer.Status.NO_CANVAS;
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.interactionManager != null) {
         this.tickCounter++;
         RotationManager.release(10);
         if (!canvas.isReady()) {
            this.status = MapPlacer.Status.NO_CANVAS;
         } else if (!externallyPaused && (!(Boolean)this.pauseOnScreen.get() || this.mc.currentScreen == null)) {
            if ((Boolean)this.autoScan.get() && --this.scanCooldown <= 0) {
               this.scanCooldown = (Integer)this.scanInterval.get();
               canvas.scanFrames();
            }

            canvas.refreshFilled();
            canvas.matchInventoryMaps();
            if (canvas.getCellCount() == 0) {
               this.status = MapPlacer.Status.NO_FRAMES;
            } else if (canvas.isComplete()) {
               boolean announce = this.status != MapPlacer.Status.DONE;
               this.status = MapPlacer.Status.DONE;
               this.currentCell = null;
               this.restoreSavedSlot();
               if (announce) {
                  if ((Boolean)this.chatFeedback.get()) {
                     this.info("Mapart complete — §b%d§r frames filled.", new Object[]{canvas.getCellCount()});
                  }

                  if ((Boolean)this.disableWhenDone.get()) {
                     this.toggle();
                  }
               }
            } else if (this.placementCooldown > 0) {
               this.placementCooldown--;
            } else {
               this.currentCell = this.pickNextCell();
               if (this.currentCell == null) {
                  this.restoreSavedSlot();
               } else {
                  ItemFrameEntity frame = this.currentCell.getFrame();
                  if (frame != null) {
                     if (!this.aimAtFrame(frame)) {
                        this.status = MapPlacer.Status.AIMING;
                     } else {
                        int hotbarSlot = this.ensureMapInHotbar(this.currentCell.getInventorySlot());
                        if (hotbarSlot >= 0) {
                           if (this.swapCooldown > 0) {
                              this.swapCooldown--;
                              this.status = MapPlacer.Status.AIMING;
                           } else {
                              this.placeIntoFrame(frame, this.currentCell);
                           }
                        }
                     }
                  }
               }
            }
         } else {
            this.status = MapPlacer.Status.PAUSED;
            this.currentCell = null;
            this.restoreSavedSlot();
         }
      }
   }

   private ItemFrameMapHelper.MapCell pickNextCell() {
      List<ItemFrameMapHelper.MapCell> pending = canvas.getPendingCells();
      boolean anyInRange = false;
      boolean anyCarried = false;

      for (ItemFrameMapHelper.MapCell cell : pending) {
         ItemFrameEntity frame = cell.getFrame();
         if (frame != null && !(ItemFrameMapHelper.distanceToFrame(frame) > (Double)this.placementRange.get())) {
            anyInRange = true;
            if (cell.getInventorySlot() >= 0) {
               anyCarried = true;
               if (!cell.isWithinTimeout(this.tickCounter, (Integer)MusheorSystem.Manager.placementTimeout.get())) {
                  this.status = MapPlacer.Status.PLACING;
                  return cell;
               }
            }
         }
      }

      this.status = !anyInRange ? MapPlacer.Status.WALK_CLOSER : (anyCarried ? MapPlacer.Status.PLACING : MapPlacer.Status.MAPS_MISSING);
      return null;
   }

   private boolean aimAtFrame(ItemFrameEntity frame) {
      if (this.rotate2.get() == MapPlacer.RotateMode.NONE) {
         return true;
      } else {
         Vec3d aimPoint = frame.getBoundingBox().getCenter();
         Vec3d eye = this.mc.player.getEyePos();
         double dx = aimPoint.x - eye.x;
         double dy = aimPoint.y - eye.y;
         double dz = aimPoint.z - eye.z;
         float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
         if (this.rotate2.get() == MapPlacer.RotateMode.CLIENT) {
            this.mc.player.setYaw(yaw);
            this.mc.player.setPitch(pitch);
            return true;
         } else {
            boolean onTheWire = this.lastAimTick == this.tickCounter - 1
               && Math.abs(MathHelper.wrapDegrees(this.lastAimYaw - yaw)) <= 8.0F
               && Math.abs(this.lastAimPitch - pitch) <= 8.0F;
            if (RotationManager.setYaw(yaw, 10) && RotationManager.setPitch(pitch, 10)) {
               this.lastAimYaw = yaw;
               this.lastAimPitch = pitch;
               this.lastAimTick = this.tickCounter;
            }

            return onTheWire;
         }
      }
   }

   private int ensureMapInHotbar(int slot) {
      int selected = this.mc.player.getInventory().selectedSlot;
      if (slot > 8) {
         ItemStack stack = this.mc.player.getInventory().getStack(slot);
         InvUtils.move().from(slot).to(this.findHotbarDest());
         if ((Boolean)this.chatFeedback.get()) {
            this.info("Moved §b%s§r into the hotbar.", new Object[]{stack.getName().getString()});
         }

         this.swapCooldown = Math.max(1, (Integer)this.swapDelay.get());
         this.status = MapPlacer.Status.AIMING;
         return -1;
      } else {
         if (selected != slot) {
            if (this.savedHotbarSlot < 0) {
               this.savedHotbarSlot = selected;
            }

            InventoryManager.selectHotbarSlot(slot);
            this.swapCooldown = (Integer)this.swapDelay.get();
            if (this.swapCooldown > 0) {
               this.status = MapPlacer.Status.AIMING;
               return -1;
            }
         }

         return slot;
      }
   }

   private void placeIntoFrame(ItemFrameEntity frame, ItemFrameMapHelper.MapCell cell) {
      String name = this.mc.player.getMainHandStack().getName().getString();
      VersionHelper.get().interactEntityAt(frame, Hand.MAIN_HAND);
      this.mc
         .interactionManager
         .sendSequencedPacket(this.mc.world, sequence -> PlayerInteractEntityC2SPacket.interact(frame, this.mc.player.isSneaking(), Hand.MAIN_HAND));
      cell.markPlaced(this.tickCounter);
      this.placementCooldown = (Integer)this.placementDelay.get();
      if ((Boolean)this.chatFeedback.get()) {
         this.info("Placed §b%s§r at §b%s§r.", new Object[]{name, cell.formatCoord(canvas.getIndexBase())});
      }
   }

   private int findHotbarDest() {
      for (int slot = 0; slot < 9; slot++) {
         if (this.mc.player.getInventory().getStack(slot).isEmpty()) {
            return slot;
         }
      }

      for (int slotx = 0; slotx < 9; slotx++) {
         if (this.mc.player.getInventory().getStack(slotx).getItem() == Items.FILLED_MAP) {
            return slotx;
         }
      }

      return InventoryManager.findEmptyHotbarSlot();
   }

   private void restoreSavedSlot() {
      if (this.savedHotbarSlot >= 0) {
         if ((Boolean)this.restoreSlot.get()) {
            InventoryManager.selectHotbarSlot(this.savedHotbarSlot);
         }

         this.savedHotbarSlot = -1;
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render2.get() && canvas.isReady() && this.mc.player != null && !this.mc.options.hudHidden) {
         Direction facing = canvas.getFacing();
         double maxDistSq = (Double)this.renderDistance.get() * (Double)this.renderDistance.get();
         Vec3d eye = this.mc.player.getEyePos();

         for (ItemFrameMapHelper.MapCell cell : canvas.getCells()) {
            if ((!cell.isFilled() || (Boolean)this.showPlaced.get()) && !(Vec3d.ofCenter(cell.pos).squaredDistanceTo(eye) > maxDistSq)) {
               Color lines = this.colorFor(cell);
               Color sides = new Color(lines.r, lines.g, lines.b, lines.a / 3);
               BlockPos pos = cell.pos;
               Vec3d front = ItemFrameMapHelper.frameSurfacePoint(pos, facing, 0.0625);
               Vec3d back = ItemFrameMapHelper.frameSurfacePoint(pos, facing, 0.0);
               double inset = 0.0625;
               boolean onX = facing.getAxis() == Axis.X;
               boolean onY = facing.getAxis() == Axis.Y;
               boolean onZ = facing.getAxis() == Axis.Z;
               event.renderer
                  .box(
                     onX ? Math.min(front.x, back.x) : pos.getX() + inset,
                     onY ? Math.min(front.y, back.y) : pos.getY() + inset,
                     onZ ? Math.min(front.z, back.z) : pos.getZ() + inset,
                     onX ? Math.max(front.x, back.x) : pos.getX() + 1 - inset,
                     onY ? Math.max(front.y, back.y) : pos.getY() + 1 - inset,
                     onZ ? Math.max(front.z, back.z) : pos.getZ() + 1 - inset,
                     lines,
                     sides,
                     (ShapeMode)this.shapeMode.get(),
                     0
                  );
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if ((Boolean)this.render2.get() && this.labels2.get() != MapPlacer.LabelMode.NONE) {
         if (canvas.isReady() && this.mc.player != null && !this.mc.options.hudHidden) {
            Direction facing = canvas.getFacing();
            double maxDistSq = (Double)this.renderDistance.get() * (Double)this.renderDistance.get();
            Vec3d eye = this.mc.player.getEyePos();
            TextRenderer textRenderer = TextRenderer.get();

            for (ItemFrameMapHelper.MapCell cell : canvas.getCells()) {
               if ((!cell.isFilled() || (Boolean)this.showPlaced.get()) && !(Vec3d.ofCenter(cell.pos).squaredDistanceTo(eye) > maxDistSq)) {
                  String text = this.labelFor(cell);
                  if (!text.isEmpty()) {
                     Vec3d world = ItemFrameMapHelper.frameSurfacePoint(cell.pos, facing, 0.1);
                     Vector3d pos = new Vector3d(world.x, world.y, world.z);
                     if (NametagUtils.to2D(pos, (Double)this.labelScale.get(), (Boolean)this.labelDistanceScaling.get())) {
                        NametagUtils.begin(pos);
                        textRenderer.begin();
                        double width = textRenderer.getWidth(text);
                        textRenderer.render(text, -width / 2.0, -textRenderer.getHeight() / 2.0, this.colorFor(cell));
                        textRenderer.end();
                        NametagUtils.end();
                     }
                  }
               }
            }
         }
      }
   }

   private Color colorFor(ItemFrameMapHelper.MapCell cell) {
      if (cell.isFilled()) {
         return (Color)this.placedColor.get();
      } else if (cell == this.currentCell) {
         return (Color)this.targetColor.get();
      } else {
         return cell.getInventorySlot() >= 0 ? (Color)this.readyColor.get() : (Color)this.missingColor.get();
      }
   }

   private String labelFor(ItemFrameMapHelper.MapCell cell) {
      int base = canvas.getIndexBase();
      int index = cell.flatIndex(canvas.getWidth(), base);

      return switch ((MapPlacer.LabelMode)this.labels2.get()) {
         case COORD-> cell.formatCoord(base);
         case INDEX-> index < 0 ? cell.formatCoord(base) : String.valueOf(index);
         case BOTH-> index < 0 ? cell.formatCoord(base) : cell.formatCoord(base) + " #" + index;
         case NONE-> "";
      };
   }

   public String getStatusText() {
      if (!canvas.isReady()) {
         return "§7no canvas";
      } else {
         String state = switch (this.status) {
            case NO_CANVAS-> "§7no canvas";
            case NO_FRAMES-> "§eno frames scanned";
            case PLACING-> "§aplacing";
            case AIMING-> "§aaiming";
            case WALK_CLOSER-> "§ewalk closer";
            case MAPS_MISSING-> "§cmaps missing";
            case PAUSED-> "§epaused";
            case DONE-> "§adone";
         };
         return String.format("%s §7(%d/%d frames)", state, canvas.getFilledCount(), canvas.getCellCount());
      }
   }

   public String getInfoString() {
      return canvas.isReady() ? canvas.getFilledCount() + "/" + canvas.getCellCount() : "no canvas";
   }

   public static int countInventoryMaps() {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null) {
         return 0;
      } else {
         int count = 0;

         for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getStack(slot).getItem() == Items.FILLED_MAP) {
               count++;
            }
         }

         return count;
      }
   }

   public static enum LabelMode {
      COORD,
      INDEX,
      BOTH,
      NONE;}

   public static enum RotateMode {
      PACKET,
      CLIENT,
      NONE;}

   public static enum Status {
      NO_CANVAS,
      NO_FRAMES,
      PLACING,
      AIMING,
      WALK_CLOSER,
      MAPS_MISSING,
      PAUSED,
      DONE;}
}

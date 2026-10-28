package musheor.modules.tech;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.HighwayUtils;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import musheor.utils.HighwayBuilderState;
import musheor.utils.BlockPlacer;
import musheor.utils.PacketBudget;

public class AutoPortal extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final SettingColor BLOCKED_LINE_COLOR = new SettingColor(255, 45, 45, 220);
   private static final SettingColor BLOCKED_SIDE_COLOR = new SettingColor(255, 45, 45, 60);
   private final Setting<Boolean> autoDisable = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("auto-disable")).description("Automatically disables the module when a portal is built"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> pauseOnInput = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("pause-on-input"))
                  .description("Pauses the building process when you are moving, sneaking or jumping"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderPortal = this.settings
      .getDefaultGroup()
      .add(((Builder)((Builder)((Builder)new Builder().name("render-portal")).description("Renders the portal's position")).defaultValue(true)).build());
   private final Setting<SettingColor> renderColor = this.settings
      .getDefaultGroup()
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("render-color"))
                  .defaultValue(new SettingColor(0, 225, 255, 200))
                  .description("Custom color for rendering (lines / wireframe)"))
               .visible(this.renderPortal::get))
            .build()
      );
   private boolean locked;
   private AutoPortal.Phase phase;
   private final List<BlockPos> framePositions = new ArrayList<>();
   private final List<BlockPos> interiorPositions = new ArrayList<>();
   private BlockPos lightPos;
   private int swapCooldown;
   private int tickCounter;

   public AutoPortal() {
      super(Musheor.MAIN, "auto-portal", "Automatically builds and lights a nether portal");
   }

   public void onActivate() {
      this.locked = false;
      this.phase = AutoPortal.Phase.IDLE;
      this.framePositions.clear();
      this.interiorPositions.clear();
      this.lightPos = null;
      this.swapCooldown = 0;
      this.tickCounter = 0;
      HighwayBuilderState.getInstance().getRecentPlacementAttempts().clear();
   }

   public void onDeactivate() {
      this.framePositions.clear();
      this.interiorPositions.clear();
      this.lightPos = null;
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (!this.locked) {
            this.computePortalLayout();
         }

         HighwayBuilderState.getInstance()
            .getRecentPlacementAttempts()
            .entrySet()
            .removeIf(
               entry -> {
                  BlockState targetState = mc.world.getBlockState(entry.getKey());
                  return targetState != null && mc.world.getBlockState(entry.getKey()).getBlock() == Blocks.OBSIDIAN
                     ? true
                     : this.tickCounter - entry.getValue() > (Integer)MusheorSystem.Manager.placementTimeout.get();
               }
            );
         if (this.swapCooldown > 0) {
            this.swapCooldown--;
         }

         if (!(Boolean)this.pauseOnInput.get() || !this.hasPlayerInput()) {
            if (!this.locked) {
               int obsidianCount = InventoryManager.countItemIncludingShulkers(Items.OBSIDIAN);
               if (obsidianCount < 10) {
                  this.error("Not enough obsidian (need %d, have %d), disabling.", new Object[]{10, obsidianCount});
                  this.toggle();
                  return;
               }

               if (!InvUtils.find(stack -> stack.getItem() == Items.FLINT_AND_STEEL).found()) {
                  this.error("No flint and steel found in inventory, disabling.", new Object[0]);
                  this.toggle();
                  return;
               }

               this.locked = true;
               this.phase = AutoPortal.Phase.IDLE;
               this.info("Position locked, building portal...", new Object[0]);
            }

            switch (this.phase) {
               case IDLE:
                  this.placeFrameTick(this.tickCounter++);
                  break;
               case PLACE:
                  this.lightWithFlint();
                  break;
               case LIGHT:
                  this.tickLightPhase();
            }
         }
      }
   }

   private void placeFrameTick(int currentTick) {
      boolean anyPlaceable = false;

      for (BlockPos pos : this.framePositions) {
         if (mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN
            && mc.world.getBlockState(pos).isReplaceable()
            && HighwayUtils.isInPlacementRange(pos)) {
            anyPlaceable = true;
            break;
         }
      }

      if (!anyPlaceable) {
         if (this.isFrameComplete()) {
            this.phase = AutoPortal.Phase.PLACE;
            this.swapCooldown = (Integer)MusheorSystem.Manager.swapDelay.get();
         }
      } else if (this.swapCooldown <= 0) {
         FindItemResult inHotbar = InvUtils.findInHotbar(stack -> stack.getItem() == Items.OBSIDIAN);
         if (!inHotbar.found()) {
            InventoryManager.moveItemToHotbar(Items.OBSIDIAN);
            this.swapCooldown = (Integer)MusheorSystem.Manager.swapDelay.get();
         } else {
            InventoryManager.swapToItemSilent(Items.OBSIDIAN, (Boolean)MusheorSystem.Manager.silentPlace.get());

            for (BlockPos posx : this.framePositions) {
               if (!PacketBudget.canPlace()) {
                  break;
               }

               if (!HighwayBuilderState.getInstance().getRecentPlacementAttempts().containsKey(posx)
                  && mc.world.getBlockState(posx).getBlock() != Blocks.OBSIDIAN
                  && mc.world.getBlockState(posx).isReplaceable()
                  && HighwayUtils.isInPlacementRange(posx)) {
                  HighwayBuilderState.getInstance().getRecentPlacementAttempts().put(posx, currentTick);
                  BlockPlacer.place(posx, Direction.DOWN, HighwayBuilderState.getInstance().getPlacementType(), (Boolean)MusheorSystem.Manager.silentPlace.get());
               }
            }
         }
      }
   }

   private void lightWithFlint() {
      if (this.lightPos != null && HighwayUtils.isInPlacementRange(this.lightPos)) {
         if (this.swapCooldown <= 0 && PacketBudget.canPlace()) {
            FindItemResult inHotbar = InvUtils.findInHotbar(stack -> stack.getItem() == Items.FLINT_AND_STEEL);
            if (inHotbar.found()) {
               InvUtils.swap(inHotbar.slot(), false);
               BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(this.lightPos).add(0.0, 0.5, 0.0), Direction.UP, this.lightPos, false);
               mc.interactionManager
                  .sendSequencedPacket(mc.world, sequence -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, sequence));
               this.phase = AutoPortal.Phase.LIGHT;
            } else if (!InvUtils.find(stack -> stack.getItem() == Items.FLINT_AND_STEEL).found()) {
               this.error("Lost flint and steel during build, disabling.", new Object[0]);
               this.toggle();
            } else {
               InventoryManager.moveItemToHotbar(Items.FLINT_AND_STEEL);
               this.swapCooldown = (Integer)MusheorSystem.Manager.swapDelay.get();
            }
         }
      }
   }

   private void tickLightPhase() {
      if (this.isPortalLit()) {
         this.info("Portal built successfully!", new Object[0]);
         if ((Boolean)this.autoDisable.get()) {
            this.toggle();
         }
      } else {
         this.phase = AutoPortal.Phase.PLACE;
      }
   }

   private boolean isFrameComplete() {
      for (BlockPos pos : this.framePositions) {
         if (mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN) {
            return false;
         }
      }

      return true;
   }

   private boolean isPortalLit() {
      for (BlockPos pos : this.interiorPositions) {
         if (mc.world.getBlockState(pos).getBlock() == Blocks.NETHER_PORTAL) {
            return true;
         }
      }

      return false;
   }

   private boolean hasPlayerInput() {
      return mc.options.forwardKey.isPressed()
         || mc.options.backKey.isPressed()
         || mc.options.leftKey.isPressed()
         || mc.options.rightKey.isPressed()
         || mc.options.jumpKey.isPressed()
         || mc.options.sneakKey.isPressed();
   }

   private Direction directionFromYaw(float yaw) {
      float n = (yaw % 360.0F + 360.0F) % 360.0F;
      if (n >= 315.0F || n < 45.0F) {
         return Direction.SOUTH;
      } else if (n < 135.0F) {
         return Direction.WEST;
      } else {
         return n < 225.0F ? Direction.NORTH : Direction.EAST;
      }
   }

   private int pitchToYOffset(float pitch) {
      float clamped = Math.max(-45.0F, Math.min(45.0F, pitch));
      int step = Math.round(-clamped / 9.0F);
      return Math.max(-5, Math.min(5, step));
   }

   private void computePortalLayout() {
      this.framePositions.clear();
      this.interiorPositions.clear();
      this.lightPos = null;
      BlockPos foot = mc.player.getBlockPos();
      Direction facing = this.directionFromYaw(mc.player.getYaw());
      int yOffset = this.pitchToYOffset(mc.player.getPitch());
      int fx = facing.getOffsetX();
      int fz = facing.getOffsetZ();
      int rx = -fz;
      int rz = fx;
      int planeX = foot.getX() + fx * 2;
      int planeZ = foot.getZ() + fz * 2;
      int baseY = foot.getY() + yOffset;

      for (int h = 0; h < 5; h++) {
         for (int w = -1; w <= 2; w++) {
            BlockPos pos = new BlockPos(planeX + rx * w, baseY + h, planeZ + rz * w);
            boolean isCorner = (h == 0 || h == 4) && (w == -1 || w == 2);
            boolean isInterior = h >= 1 && h <= 3 && w >= 0 && w <= 1;
            if (!isCorner) {
               if (isInterior) {
                  this.interiorPositions.add(pos);
               } else {
                  this.framePositions.add(pos);
               }
            }
         }
      }

      for (BlockPos pos : this.framePositions) {
         if (pos.getY() == baseY) {
            this.lightPos = pos;
            break;
         }
      }
   }

   private boolean isLayoutBlocked() {
      for (BlockPos pos : this.framePositions) {
         BlockState state = mc.world.getBlockState(pos);
         if (state.getBlock() != Blocks.OBSIDIAN && !state.isReplaceable()) {
            return true;
         }
      }

      for (BlockPos posx : this.interiorPositions) {
         BlockState state = mc.world.getBlockState(posx);
         if (state.getBlock() != Blocks.NETHER_PORTAL && state.getBlock() != Blocks.FIRE && !state.isReplaceable()) {
            return true;
         }
      }

      return false;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.world != null) {
         if ((Boolean)this.renderPortal.get()) {
            if (!this.framePositions.isEmpty() || !this.interiorPositions.isEmpty()) {
               boolean blocked = this.isLayoutBlocked();
               SettingColor lineColor = blocked ? BLOCKED_LINE_COLOR : (SettingColor)this.renderColor.get();
               SettingColor sideColor = blocked ? BLOCKED_SIDE_COLOR : (SettingColor)this.renderColor.get();
               List<BlockPos> all = new ArrayList<>(this.framePositions.size() + this.interiorPositions.size());
               all.addAll(this.framePositions);
               all.addAll(this.interiorPositions);
               musheor.utils.BlockRenderUtils.renderPositions(event, all, lineColor, sideColor, ShapeMode.Lines);
            }
         }
      }
   }

   private static enum Phase {
      IDLE,
      PLACE,
      LIGHT;}
}

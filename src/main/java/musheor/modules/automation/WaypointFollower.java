package musheor.modules.automation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.XearoHelper;
import musheor.modules.features.KekFly;
import musheor.utils.ModuleControl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import musheor.utils.BaritoneHelper;

public class WaypointFollower extends Module {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<WaypointFollower.FlightMode> flightMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("flight-mode")).description("Which type of flight mode to use"))
               .defaultValue(WaypointFollower.FlightMode.ELYTRA))
            .build()
      );
   private final Setting<Boolean> drawPath = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("draw-path"))
                     .description("Draws a path between waypoints on Xearo's map"))
                  .defaultValue(true))
               .visible(() -> XearoHelper.isLoaded() && this.flightMode.get() != WaypointFollower.FlightMode.CREATIVE))
            .build()
      );
   private final Setting<Integer> arrivalRadius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("arrival-radius"))
                  .description("XZ distance to a waypoint at which it is considered reached."))
               .defaultValue(32))
            .sliderRange(8, 256)
            .build()
      );
   private final Setting<Double> degreesPerTick = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("degrees-per-tick"))
                  .description("Max yaw rotation per tick — limits how sharply the player turns toward a waypoint."))
               .defaultValue(3.0)
               .sliderRange(0.5, 20.0)
               .decimalPlaces(1)
               .visible(() -> this.flightMode.get() == WaypointFollower.FlightMode.ELYTRA || this.flightMode.get() == WaypointFollower.FlightMode.BARITONE))
            .build()
      );
   private XearoHelper.WaypointData currentWaypoint = null;
   private int lastWaypointCount = -1;

   public WaypointFollower() {
      super(Musheor.AUTOMATION, "waypoint-follower", "Automatically follow waypoints in all dimensions using different flight modes.");
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.world != null) {
         Dimension currentDim = PlayerUtils.getDimension();
         if (this.flightMode.get() == WaypointFollower.FlightMode.CREATIVE && (currentDim == Dimension.End || currentDim == Dimension.Overworld)) {
            Formatting format = currentDim == Dimension.End ? Formatting.DARK_PURPLE : Formatting.RED;
            ModuleControl.setModuleSetting(WaypointFollower.class, "flight-mode", WaypointFollower.FlightMode.ELYTRA);
            ChatUtils.info(
               Formatting.GOLD
                  + "[WaypointFollower] "
                  + Formatting.WHITE
                  + "Automatically switching to RocketFly: "
                  + format
                  + currentDim.name()
                  + Formatting.WHITE
                  + " does not support Baritone Elytra Flight.",
               new Object[0]
            );
            this.toggle();
         }
      }
   }

   public void onDeactivate() {
      if (this.currentWaypoint != null) {
         BaritoneHelper.cancel();
         this.currentWaypoint = null;
      }

      if (XearoHelper.isLoaded()) {
         XearoHelper.get().clearLinesOnMap();
      }

      this.lastWaypointCount = -1;
      KekFly fly = (KekFly)Modules.get().get(KekFly.class);
      if (fly.isActive() && this.flightMode.get() == WaypointFollower.FlightMode.ELYTRA) {
         fly.toggle();
      }
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         switch ((WaypointFollower.FlightMode)this.flightMode.get()) {
            case CREATIVE:
               this.tickCreative();
               break;
            case ELYTRA:
               this.tickElytra();
               break;
            case BARITONE:
               this.tickBaritone();
         }

         if ((Boolean)this.drawPath.get() && this.flightMode.get() != WaypointFollower.FlightMode.CREATIVE) {
            this.updatePathDraw();
         }
      }
   }

   private void tickCreative() {
      if (this.currentWaypoint == null) {
         this.currentWaypoint = XearoHelper.get().getOldestWaypoint(true);
         if (this.currentWaypoint != null) {
            this.startBaritoneTo(this.currentWaypoint);
         }
      } else {
         if (XearoHelper.get().distanceToWaypoint(this.currentWaypoint) < ((Integer)this.arrivalRadius.get()).intValue()) {
            BaritoneHelper.cancel();
            ChatUtils.info(
               Formatting.GREEN + "Arrived at " + Formatting.YELLOW + this.currentWaypoint.name() + Formatting.GREEN + ", moving to next...", new Object[0]
            );
            XearoHelper.get().deleteCurrentWaypoint(this.currentWaypoint);
            this.currentWaypoint = null;
         }
      }
   }

   private void tickElytra() {
      if (((KekFly)Modules.get().get(KekFly.class)).isActive()) {
         if (this.currentWaypoint == null) {
            this.currentWaypoint = XearoHelper.get().getOldestWaypoint(true);
            if (this.currentWaypoint == null) {
               return;
            }

            ChatUtils.info(Formatting.GOLD + "[WaypointFollower] " + Formatting.WHITE + "Flying to: " + Formatting.YELLOW + this.currentWaypoint.name(), new Object[0]);
         }

         float desiredYaw = this.yawToWaypoint(this.currentWaypoint);
         float diff = this.yawDelta(this.mc.player.getYaw(), desiredYaw);
         float maxRot = (float)((Double)this.degreesPerTick.get()).doubleValue();
         if (Math.abs(diff) <= maxRot) {
            this.mc.player.setYaw(desiredYaw);
         } else {
            this.mc.player.setYaw(this.mc.player.getYaw() - Math.signum(diff) * maxRot);
         }

         if (XearoHelper.get().distanceToWaypoint(this.currentWaypoint) < ((Integer)this.arrivalRadius.get()).intValue()) {
            ChatUtils.info(
               Formatting.GOLD
                  + "[WaypointFollower] "
                  + Formatting.WHITE
                  + "Arrived at "
                  + Formatting.YELLOW
                  + this.currentWaypoint.name()
                  + Formatting.WHITE
                  + ", moving to next...",
               new Object[0]
            );
            XearoHelper.get().deleteCurrentWaypoint(this.currentWaypoint);
            this.currentWaypoint = null;
         }
      }
   }

   private void tickBaritone() {
      if (this.currentWaypoint == null) {
         this.currentWaypoint = XearoHelper.get().getOldestWaypoint(true);
         if (this.currentWaypoint == null) {
            return;
         }

         ChatUtils.info(Formatting.GOLD + "[WaypointFollower] " + Formatting.WHITE + "Flying to: " + Formatting.YELLOW + this.currentWaypoint.name(), new Object[0]);
      }

      float desiredYaw = this.yawToWaypoint(this.currentWaypoint);
      float diff = this.yawDelta(this.mc.player.getYaw(), desiredYaw);
      float maxRot = (float)((Double)this.degreesPerTick.get()).doubleValue();
      if (Math.abs(diff) <= maxRot) {
         this.mc.player.setYaw(desiredYaw);
      } else {
         this.mc.player.setYaw(this.mc.player.getYaw() - Math.signum(diff) * maxRot);
      }

      if (XearoHelper.get().distanceToWaypoint(this.currentWaypoint) < ((Integer)this.arrivalRadius.get()).intValue()) {
         ChatUtils.info(
            Formatting.GOLD
               + "[WaypointFollower] "
               + Formatting.WHITE
               + "Arrived at "
               + Formatting.YELLOW
               + this.currentWaypoint.name()
               + Formatting.WHITE
               + ", moving to next...",
            new Object[0]
         );
         XearoHelper.get().deleteCurrentWaypoint(this.currentWaypoint);
         this.currentWaypoint = null;
      }
   }

   private void updatePathDraw() {
      if (XearoHelper.isLoaded()) {
         List<XearoHelper.WaypointData> wps = XearoHelper.get().getWaypoints(true);
         if (wps != null) {
            if (wps.size() != this.lastWaypointCount) {
               this.lastWaypointCount = wps.size();
               if (wps.isEmpty()) {
                  XearoHelper.get().clearLinesOnMap();
               } else {
                  List<XearoHelper.WaypointData> sorted = new ArrayList<>(wps);
                  sorted.sort(Comparator.comparingLong(XearoHelper.WaypointData::createdAt));
                  List<XearoHelper.LineData> lines = new ArrayList<>();
                  double fromX = this.mc.player.getX();
                  double fromZ = this.mc.player.getZ();

                  for (XearoHelper.WaypointData wp : sorted) {
                     lines.add(new XearoHelper.LineData((int)fromX, (int)fromZ, wp.x(), wp.z()));
                     fromX = wp.x();
                     fromZ = wp.z();
                  }

                  XearoHelper.get().drawLinesOnMap(lines, -16733441);
               }
            }
         }
      }
   }

   private void startBaritoneTo(XearoHelper.WaypointData wp) {
      ChatUtils.info(Formatting.GOLD + "[WaypointFollower] " + Formatting.WHITE + "Flying to next waypoint: " + Formatting.GRAY + wp.name(), new Object[0]);
      BaritoneHelper.pathElytraTo(wp.x(), wp.z());
   }

   private float yawToWaypoint(XearoHelper.WaypointData wp) {
      double dx = wp.x() - this.mc.player.getX();
      double dz = wp.z() - this.mc.player.getZ();
      return (float)Math.toDegrees(Math.atan2(-dx, dz));
   }

   private float yawDelta(float a, float b) {
      float d = (a - b) % 360.0F;
      if (d > 180.0F) {
         d -= 360.0F;
      }

      if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   public static enum FlightMode {
      CREATIVE,
      ELYTRA,
      BARITONE;}
}

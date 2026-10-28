package musheor.modules.automation.highway;

import musheor.compat.VersionHelper;
import musheor.utils.HighwayUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import musheor.utils.BaritoneHelper;

public class HighwayNavigator {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final HighwayRouter.Route route;
   private final HighwayNavigator.BounceController bounceController;
   private final HighwayNavigator.Listener listener;
   private final HighwayNavigator.Config config;
   private HighwayNavigator.State state = HighwayNavigator.State.IDLE;
   private int legIndex = 0;
   private int alignTicks = 0;
   private int stuckTicks = 0;
   private int settleTicksRemaining = 0;
   private HighwayUtils.XZ lastPos = null;
   private double closestDist = Double.MAX_VALUE;

   public HighwayNavigator(
      HighwayRouter.Route route, HighwayNavigator.Config config, HighwayNavigator.BounceController bounceController, HighwayNavigator.Listener listener
   ) {
      this.route = route;
      this.config = config;
      this.bounceController = bounceController;
      this.listener = listener;
   }

   public HighwayNavigator.State getState() {
      return this.state;
   }

   public void tick() {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.state != HighwayNavigator.State.AVOIDING && this.state != HighwayNavigator.State.DONE) {
            if (this.route.legs.isEmpty()) {
               this.finishRoute();
            } else {
               switch (this.state) {
                  case IDLE:
                     this.tickIdle();
                     break;
                  case PATHING:
                     this.tickPathing();
                     break;
                  case WAITING:
                     this.tickWaiting();
                     break;
                  case BUILDING:
                     this.tickBuilding();
                     break;
                  case RESTOCKING:
                     this.tickRestocking();
               }
            }
         }
      }
   }

   public void stop() {
      this.stopBounce();
      if (BaritoneHelper.isPathing()) {
         BaritoneHelper.cancel();
      }
   }

   private void tickIdle() {
      HighwayUtils.XZ target = this.currentTarget();
      float desiredYaw = this.yawTo(target);
      this.mc.player.setYaw(desiredYaw);
      float yawDiff = Math.abs(this.yawDiff(this.mc.player.getYaw(), desiredYaw));
      this.alignTicks++;
      if (yawDiff <= this.config.yawTolerance() || this.alignTicks >= this.config.alignTimeout()) {
         this.alignTicks = 0;
         this.closestDist = this.distanceTo(this.currentTarget());
         this.startBounce();
         this.setState(HighwayNavigator.State.PATHING, "bouncing toward " + this.targetLabel());
      }
   }

   private void tickPathing() {
      HighwayUtils.XZ target = this.currentTarget();
      double dist = this.distanceTo(target);
      if (!BaritoneHelper.isBusy()) {
         this.mc.player.setYaw(this.yawTo(target));
      }

      this.updateStuck();
      if (dist < this.config.approachRadius() && dist < this.closestDist) {
         this.setState(HighwayNavigator.State.WAITING, "approaching " + this.targetLabel());
      }
   }

   private void tickWaiting() {
      HighwayUtils.XZ target = this.currentTarget();
      double dist = this.distanceTo(target);
      this.mc.player.setYaw(this.yawTo(target));
      if (dist < this.config.arriveRadius()) {
         this.stopBounce();
         BlockPos goal = this.toBlockPos(target);
         BaritoneHelper.goTo(goal);
         this.setState(HighwayNavigator.State.BUILDING, "transitioning at " + this.targetLabel());
      }
   }

   private void tickBuilding() {
      if (!BaritoneHelper.isBusy()) {
         this.settleTicksRemaining = this.config.settleTicks();
         this.setState(HighwayNavigator.State.RESTOCKING, "settling at " + this.targetLabel());
      }
   }

   private void tickRestocking() {
      int nextWpIdx = this.legIndex + 2;
      HighwayUtils.XZ faceTarget = nextWpIdx < this.route.waypoints.size()
         ? this.route.waypoints.get(nextWpIdx)
         : this.currentTarget();
      this.mc.player.setYaw(this.yawTo(faceTarget));
      if (--this.settleTicksRemaining <= 0) {
         this.advanceLeg();
      }
   }

   private void advanceLeg() {
      this.legIndex++;
      if (this.legIndex >= this.route.legs.size()) {
         this.finishRoute();
      } else {
         this.startBounce();
         this.setState(HighwayNavigator.State.IDLE, "aligning for leg " + this.legIndex + ": " + this.currentLeg().highway().name());
      }
   }

   private void startBounce() {
      this.bounceController.setBouncing(true);
   }

   private void stopBounce() {
      this.bounceController.setBouncing(false);
   }

   private void updateStuck() {
      HighwayUtils.XZ pos = this.playerXZ();
      if (this.lastPos != null) {
         double moved = HighwayNetwork.distance(pos, this.lastPos);
         if (moved < 0.5) {
            this.stuckTicks++;
         } else {
            this.stuckTicks = 0;
         }
      }

      this.lastPos = pos;
   }

   private HighwayRouter.Leg currentLeg() {
      return this.route.legs.get(this.legIndex);
   }

   private HighwayUtils.XZ currentTarget() {
      return this.route.waypoints.get(this.legIndex + 1);
   }

   private String targetLabel() {
      HighwayUtils.XZ wp = this.currentTarget();
      return String.format("(%.0f, %.0f)", wp.x(), wp.z());
   }

   private double distanceTo(HighwayUtils.XZ target) {
      HighwayUtils.XZ pos = this.playerXZ();
      return HighwayNetwork.distance(pos, target);
   }

   private HighwayUtils.XZ playerXZ() {
      return new HighwayUtils.XZ(this.mc.player.getX(), this.mc.player.getZ());
   }

   private BlockPos toBlockPos(HighwayUtils.XZ p) {
      return new BlockPos((int)p.x(), (int)VersionHelper.get().getPlayerPos().getY(), (int)p.z());
   }

   private float yawTo(HighwayUtils.XZ target) {
      double dx = target.x() - this.mc.player.getX();
      double dz = target.z() - this.mc.player.getZ();
      return (float)Math.toDegrees(Math.atan2(-dx, dz));
   }

   private float yawDiff(float a, float b) {
      float diff = (a - b) % 360.0F;
      if (diff > 180.0F) {
         diff -= 360.0F;
      }

      if (diff < -180.0F) {
         diff += 360.0F;
      }

      return diff;
   }

   private void setState(HighwayNavigator.State newState, String description) {
      this.state = newState;
      this.listener.onStateChanged(newState, description);
   }

   private void finishRoute() {
      this.stopBounce();
      this.state = HighwayNavigator.State.AVOIDING;
      this.listener.onRouteFinished();
   }

   public interface BounceController {
      void setBouncing(boolean active);
   }

   public record Config(double approachRadius, double arriveRadius, int alignTimeout, float yawTolerance, int transitionDistance, int settleTicks) {
   }

   public interface Listener {
      void onStateChanged(HighwayNavigator.State state, String description);

      void onRouteFinished();
   }

   public static enum State {
      IDLE,
      PATHING,
      WAITING,
      BUILDING,
      RESTOCKING,
      AVOIDING,
      DONE;}
}

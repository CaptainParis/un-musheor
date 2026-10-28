package musheor.modules.automation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.compat.XearoHelper;
import musheor.modules.automation.highway.HighwayNavigator;
import musheor.modules.automation.highway.HighwayRouter;
import musheor.utils.ModuleControl;
import musheor.utils.HighwayUtils;
import musheor.utils.ElytraPitchOptimizer;
import musheor.utils.manager.RotationManager;
import musheor.utils.manager.CommandQueue;
import musheor.utils.manager.ElytraFireworkManager;
import musheor.utils.system.HighwayNetworkManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import musheor.utils.BaritoneHelper;

public class KekBounce extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgGrim = this.settings.createGroup("Grim");
   private final SettingGroup sgRotation = this.settings.createGroup("Rotation");
   private final SettingGroup sgObstaclePasser = this.settings.createGroup("Obstacle-Passer");
   private final SettingGroup sgAutoRepair = this.settings.createGroup("Auto-Repair");
   private final SettingGroup sg2bnav = this.settings.createGroup("2bNav");
   private final Setting<KekBounce.Mode> mode2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode"))
                  .description("Simple: freeform bounce. Navigate: Set and follows temporary waypoints created using Xearo"))
               .defaultValue(KekBounce.Mode.SIMPLE))
            .build()
      );
   private final Setting<Boolean> muteSwapSounds = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("mute-swap-sounds"))
                  .description("Mutes the armour equip and elytra wind sounds that fake-fly's swaps would play every hop."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> fakeFly = this.sgGrim
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("fake-fly"))
                  .description(
                     "Bounce while wearing a chestplate. Keeps the elytra in your hotbar and only puts it on for the instant the glide is started, so it takes no durability."
                  ))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> toggleElytra = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("toggle-elytra"))
                     .description("Equips an elytra on activate, and a chestplate on deactivate."))
                  .defaultValue(true))
               .visible(() -> !(Boolean)this.fakeFly.get()))
            .build()
      );
   private final Setting<Integer> fakeFlyHold = this.sgGrim
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("fake-fly-hold"))
                     .description("Max ticks to keep gliding after the server ends the glide, holding back pongs meanwhile. 0 re-deploys mid-air instead."))
                  .defaultValue(12))
               .sliderRange(0, 20)
               .min(0)
               .visible(() -> false))
            .build()
      );
   private final Setting<Boolean> lockPitch = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("lock-pitch"))
                  .description("Whether to lock your pitch when bouncing."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> variablePitch = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                           .name("variable-pitch"))
                        .description(
                           "Changes the pitch every tick to the one that builds the most speed (about 18% faster than a fixed 72.4). Only the server-side rotation moves, your camera stays free."
                        ))
                     .defaultValue(false))
                  .visible(this.lockPitch::get))
               .onChanged(value -> {
                  if (value) {
                     getPitchOptimizer();
                  }
               }))
            .build()
      );
   private final Setting<Double> pitch2 = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("pitch"))
                  .description("Degrees of pitch to lock to"))
               .defaultValue(72.4)
               .sliderRange(-90.0, 90.0)
               .decimalPlaces(2)
               .visible(() -> (Boolean)this.lockPitch.get() && !(Boolean)this.variablePitch.get()))
            .build()
      );
   private final Setting<KekBounce.YawLock> yawLock = this.sgRotation
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("yaw-lock"))
                  .description("Snap on activation, Snap to nearest axis, configure custom value or disable."))
               .defaultValue(KekBounce.YawLock.SNAP_ON_ACTIVATION))
            .build()
      );
   private final Setting<Double> yaw2 = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("yaw"))
                  .description("Degrees of yaw to lock to"))
               .defaultValue(0.0)
               .sliderRange(-180.0, 180.0)
               .decimalPlaces(2)
               .visible(() -> this.yawLock.get() == KekBounce.YawLock.CUSTOM))
            .build()
      );
   private final Setting<Boolean> simpleObstaclePasser = this.sgObstaclePasser
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("simple-obstacle-passer"))
                  .description("Uses baritone to pass obstacles and aligns you to your starting position when enabling the module"))
               .defaultValue(false))
            .build()
      );
   private final Setting<KekBounce.StartPos> startPosType = this.sgObstaclePasser
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("start-pos-type"))
                     .description("Automatic or custom starting position for aligning with obstacle-passer"))
                  .defaultValue(KekBounce.StartPos.AUTOMATIC))
               .visible(() -> this.mode2.get() == KekBounce.Mode.SIMPLE && (Boolean)this.simpleObstaclePasser.get()))
            .build()
      );
   private final Setting<KekBounce.Height> targetY = this.sgObstaclePasser
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("target-y")).description("The Y level to bounce at."))
                  .defaultValue(KekBounce.Height.CURRENT))
               .visible(this.simpleObstaclePasser::get))
            .build()
      );
   private final Setting<Integer> yLevel = this.sgObstaclePasser
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("y-level"))
                     .description("The Y level to bounce at."))
                  .defaultValue(120))
               .sliderRange(-64, 120)
               .visible(() -> (Boolean)this.simpleObstaclePasser.get() && this.targetY.get() == KekBounce.Height.CUSTOM))
            .build()
      );
   private final Setting<BlockPos> customStartPos = this.sgObstaclePasser
      .add(
         ((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)new meteordevelopment.meteorclient.settings.BlockPosSetting.Builder()
                           .name("custom-start-pos"))
                        .description("The position to align with when using the custom starting position option."))
                     .defaultValue(new BlockPos(0, 0, 0)))
                  .visible(
                     () -> this.mode2.get() == KekBounce.Mode.SIMPLE
                        && (Boolean)this.simpleObstaclePasser.get()
                        && this.startPosType.get() == KekBounce.StartPos.CUSTOM
                  ))
               .onChanged(value -> this.startPos = value))
            .build()
      );
   private final Setting<Integer> distance2 = this.sgObstaclePasser
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("distance"))
                     .description("The distance to set the baritone goal for path realignment."))
                  .defaultValue(8))
               .visible(this.simpleObstaclePasser::get))
            .build()
      );
   private final Setting<Integer> stuckWindow = this.sgObstaclePasser
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("stuck-window"))
                     .description("Ticks over which your progress is measured to decide whether you are stuck."))
                  .defaultValue(30))
               .sliderRange(10, 100)
               .min(5)
               .visible(this.simpleObstaclePasser::get))
            .build()
      );
   private final Setting<Double> stuckDistance = this.sgObstaclePasser
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("stuck-distance"))
                  .description("You count as stuck when you moved less than this many blocks over the stuck window."))
               .defaultValue(4.0)
               .sliderRange(0.5, 20.0)
               .decimalPlaces(1)
               .min(0.0)
               .visible(this.simpleObstaclePasser::get))
            .build()
      );
   private final Setting<Integer> setbackLimit = this.sgObstaclePasser
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("setback-limit"))
                     .description("You count as stuck after this many server setbacks (rubberbands) within the stuck window. 0 disables."))
                  .defaultValue(3))
               .sliderRange(0, 10)
               .min(0)
               .visible(this.simpleObstaclePasser::get))
            .build()
      );
   private final Setting<Boolean> showEta = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("show-eta"))
                     .description("Displays and calculates arrival time above the player's hotbar."))
                  .defaultValue(true))
               .visible(
                  () -> XearoHelper.isLoaded() && FabricLoader.getInstance().isModLoaded("xaeroplus") && this.mode2.get() == KekBounce.Mode.NAVIGATE
               ))
            .build()
      );
   private final Setting<Boolean> renderPathOnMap = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("render-path-on-map"))
                     .description("Displays your current navigation path on your minimap / worldmap."))
                  .defaultValue(true))
               .visible(
                  () -> XearoHelper.isLoaded() && FabricLoader.getInstance().isModLoaded("xaeroplus") && this.mode2.get() == KekBounce.Mode.NAVIGATE
               ))
            .build()
      );
   private final Setting<Double> approachRadius = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("approach-radius"))
                  .description("Start slowing down when within this many blocks of a waypoint."))
               .defaultValue(64.0)
               .sliderRange(16.0, 256.0)
               .decimalPlaces(0)
               .visible(() -> XearoHelper.isLoaded() && this.mode2.get() == KekBounce.Mode.NAVIGATE))
            .build()
      );
   private final Setting<Double> arriveRadius = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("arrive-radius"))
                  .description("Consider a waypoint reached when within this many blocks."))
               .defaultValue(32.0)
               .sliderRange(8.0, 128.0)
               .decimalPlaces(0)
               .visible(() -> XearoHelper.isLoaded() && this.mode2.get() == KekBounce.Mode.NAVIGATE))
            .build()
      );
   private final Setting<Integer> alignTimeout = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("align-timeout"))
                     .description("Ticks to spend aligning yaw before bouncing anyway."))
                  .defaultValue(40))
               .sliderRange(10, 100)
               .visible(() -> XearoHelper.isLoaded() && this.mode2.get() == KekBounce.Mode.NAVIGATE))
            .build()
      );
   private final Setting<Double> yawTolerance = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("yaw-tolerance"))
                  .description("Degrees of yaw error considered close enough to start bouncing."))
               .defaultValue(4.0)
               .sliderRange(1.0, 20.0)
               .decimalPlaces(1)
               .visible(() -> XearoHelper.isLoaded() && this.mode2.get() == KekBounce.Mode.NAVIGATE))
            .build()
      );
   private final Setting<Integer> transitionDistance = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("transition-distance"))
                     .description("Baritone goal distance when walking an intersection."))
                  .defaultValue(8))
               .sliderRange(1, 32)
               .visible(() -> XearoHelper.isLoaded() && this.mode2.get() == KekBounce.Mode.NAVIGATE))
            .build()
      );
   private final Setting<Integer> settleTicks = this.sg2bnav
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("settle-ticks"))
                     .description("Ticks to wait at an intersection after arriving before resuming bounce."))
                  .defaultValue(10))
               .sliderRange(0, 40)
               .visible(() -> XearoHelper.isLoaded() && this.mode2.get() == KekBounce.Mode.NAVIGATE))
            .build()
      );
   private final Setting<Boolean> repairElytra = this.sgAutoRepair
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("repair-elytra"))
                  .description("Uses experience bottles to repair your elytra."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> damageThreshold = this.sgAutoRepair
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("damage-threshold-%"))
                     .description("At what durability percentage should it start repairing the elytra."))
                  .defaultValue(5))
               .sliderRange(0, 100)
               .visible(this.repairElytra::get))
            .build()
      );
   private final Setting<Integer> repairThreshold = this.sgAutoRepair
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("repair-threshold-%"))
                     .description("At what durability percentage should it stop repairing the elytra."))
                  .defaultValue(90))
               .sliderRange(0, 100)
               .visible(this.repairElytra::get))
            .build()
      );
   private final Setting<String> server2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                           .name("server"))
                        .description("Which server's highway network to use. Must match a key in the remote config (e.g. \"2b2t\")."))
                     .defaultValue("2b2t"))
                  .onChanged(
                     value -> {
                        HighwayNetworkManager mgr = HighwayNetworkManager.getInstance();
                        if (!mgr.isLoaded()) {
                           this.warning("Highway configs are still loading — server key will be validated once loaded.", new Object[0]);
                        } else {
                           if (!mgr.getServerNames().contains(value)) {
                              this.error(
                                 "Unknown server \"(highlight)%s(default)\". Known servers: (highlight)%s(default).",
                                 new Object[]{value, String.join(", ", mgr.getServerNames())}
                              );
                           }
                        }
                     }
                  ))
               .visible(() -> false))
            .build()
      );
   private HighwayUtils.HighwayDirection highwayDirection;
   private int obstaclePathTicks;
   private int obstacleCooldown;
   private boolean obstaclePathing;
   private boolean repairing;
   private Float snappedYaw;
   private HighwayNavigator navigator = null;
   private XearoHelper.WaypointData currentWaypoint = null;
   private int lastWaypointCount = -1;
   private int stuckRetryCount;
   private BlockPos startPos;
   private final ArrayDeque<Vec3d> recentPositions = new ArrayDeque<>();
   private final ArrayDeque<Integer> recentSetbackAges = new ArrayDeque<>();
   private final AtomicInteger pendingSetbacks = new AtomicInteger();
   private int lastStuckAge = Integer.MIN_VALUE;
   private static volatile ElytraPitchOptimizer pitchOptimizer;
   private static boolean pitchSolverStarted;
   private double bounceStartY;
   private boolean pitchRotationHeld;
   private int bounceTick;
   private int lastRedeployTick;
   private int lastSwapTick;
   private int airborneTicks;
   private boolean canRedeploy;
   private volatile boolean fakeFlying;
   private volatile boolean holdingPongs;
   private int holdTicks;
   private final Object holdLock = new Object();
   private int elytraHotbarSlot = -1;
   private int savedHotbarSlot = -1;
   private boolean pausedForEating;

   public KekBounce() {
      super(Musheor.AUTOMATION, "kek-bounce", "Elytra bounce module with some extra juice.");
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.world != null) {
         this.highwayDirection = HighwayUtils.directionFromYaw();
         this.clearStuckHistory();
         this.obstaclePathTicks = 0;
         this.obstacleCooldown = 0;
         this.obstaclePathing = false;
         this.repairing = false;
         this.stuckRetryCount = 0;
         this.bounceStartY = this.mc.player.getY();
         this.pitchRotationHeld = false;
         if ((Boolean)this.lockPitch.get() && (Boolean)this.variablePitch.get()) {
            getPitchOptimizer();
         }

         this.startPos = this.startPosType.get() == KekBounce.StartPos.AUTOMATIC
            ? BlockPos.ofFloored(VersionHelper.get().getPlayerPos())
            : (BlockPos)this.customStartPos.get();
         if (this.targetY.get() == KekBounce.Height.CURRENT) {
            this.yLevel.set(this.mc.player.getBlockY());
         }

         this.bounceTick = 0;
         this.lastRedeployTick = -2;
         this.lastSwapTick = -2;
         this.airborneTicks = 0;
         this.canRedeploy = true;
         this.fakeFlying = false;
         this.releaseFakeFlyHold();
         this.elytraHotbarSlot = -1;
         this.savedHotbarSlot = -1;
         this.pausedForEating = false;
         if ((Boolean)this.fakeFly.get()) {
            this.savedHotbarSlot = this.mc.player.getInventory().selectedSlot;
            ElytraFireworkManager.unequipElytra();
            this.elytraHotbarSlot = ElytraFireworkManager.findUsableElytraHotbarSlot();
            if (this.elytraHotbarSlot == -1) {
               this.warning("Fake-fly needs an elytra in your hotbar — bouncing on whatever is on your chest instead.", new Object[0]);
            }
         } else if ((Boolean)this.toggleElytra.get() && !this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)) {
            ((ChestSwap)Modules.get().get(ChestSwap.class)).swap();
         }

         if (this.yawLock.get() == KekBounce.YawLock.SNAP_ON_ACTIVATION) {
            this.snappedYaw = this.mc.player.getYaw();
         }

         if (this.mode2.get() == KekBounce.Mode.NAVIGATE) {
            if ((Boolean)this.renderPathOnMap.get() && !FabricLoader.getInstance().isModLoaded("xaeroplus")) {
               this.warning("Cannot render path on map, %sXaeroPlus %srequired but not installed!", new Object[]{Formatting.GOLD, Formatting.YELLOW});
               this.renderPathOnMap.set(false);
            }

            if ((Boolean)this.showEta.get() && !FabricLoader.getInstance().isModLoaded("xaeroplus")) {
               this.warning("Cannot show ETA, %sXaeroPlus %srequired but not installed!", new Object[]{Formatting.GOLD, Formatting.YELLOW});
               this.showEta.set(false);
            }

            if (!XearoHelper.isLoaded()) {
               this.warning("AdvancedNavigation requires %sXaero%s. Install it or switch to Simple mode.", new Object[]{Formatting.RED, Formatting.YELLOW});
               this.toggle();
               return;
            }

            this.startNavigate();
         } else {
            this.resetSimpleState();
         }
      }
   }

   public void onDeactivate() {
      if (this.navigator != null) {
         this.navigator.stop();
         this.navigator = null;
      }

      this.currentWaypoint = null;
      this.lastWaypointCount = -1;
      if (XearoHelper.isLoaded()) {
         XearoHelper.get().clearLinesOnMap();
      }

      this.fakeFlying = false;
      this.releaseFakeFlyHold();
      if ((Boolean)this.fakeFly.get()) {
         if (this.savedHotbarSlot >= 0) {
            musheor.utils.manager.InventoryManager.selectHotbarSlot(this.savedHotbarSlot);
         }

         this.savedHotbarSlot = -1;
         this.pausedForEating = false;
      } else if ((Boolean)this.toggleElytra.get()) {
         ((ChestSwap)Modules.get().get(ChestSwap.class)).swap();
      }

      if (BaritoneHelper.isPathing()) {
         BaritoneHelper.cancel();
      }

      ModuleControl.releaseMovementKeys();
      this.snappedYaw = null;
      if (this.pitchRotationHeld) {
         RotationManager.clear();
      }

      this.pitchRotationHeld = false;
   }

   private void resetSimpleState() {
      this.highwayDirection = HighwayUtils.directionFromYaw();
      this.clearStuckHistory();
      this.obstaclePathTicks = 0;
      this.obstacleCooldown = 0;
      this.obstaclePathing = false;
      this.repairing = false;
   }

   private void startNavigate() {
      XearoHelper.WaypointData wp = XearoHelper.get().getOldestWaypoint(true);
      if (wp == null) {
         ChatUtils.info(Formatting.AQUA + "[2bNav] " + Formatting.WHITE + "No temp waypoints found!", new Object[0]);
         this.toggle();
      } else {
         this.navigateToWaypoint(wp);
      }
   }

   private void navigateToWaypoint(XearoHelper.WaypointData wp) {
      this.currentWaypoint = wp;
      double px = this.mc.player.getX();
      double pz = this.mc.player.getZ();
      double gx = wp.x();
      double gz = wp.z();
      ChatUtils.info(
         Formatting.AQUA
            + "[2bNav] "
            + Formatting.WHITE
            + "Navigating to waypoint: "
            + Formatting.YELLOW
            + wp.name()
            + Formatting.WHITE
            + " ("
            + (int)gx
            + ", "
            + (int)gz
            + ")",
         new Object[0]
      );
      HighwayRouter.Route route = HighwayRouter.findRoute(px, pz, gx, gz);
      if (route == null) {
         ChatUtils.info(Formatting.AQUA + "[2bNav] " + Formatting.WHITE + "No highway route found to waypoint: " + wp.name(), new Object[0]);
         this.toggle();
      } else {
         this.drawRouteOnMap(route);
         HighwayNavigator.Config navConfig = new HighwayNavigator.Config(
            (Double)this.approachRadius.get(),
            (Double)this.arriveRadius.get(),
            (Integer)this.alignTimeout.get(),
            ((Double)this.yawTolerance.get()).floatValue(),
            (Integer)this.transitionDistance.get(),
            (Integer)this.settleTicks.get()
         );
         this.navigator = new HighwayNavigator(route, navConfig, new HighwayNavigator.BounceController() {
            @Override
            public void setBouncing(boolean active) {
               KekBounce.this.obstaclePathing = !active;
            }
         }, new HighwayNavigator.Listener() {
            @Override
            public void onStateChanged(HighwayNavigator.State newState, String description) {
               ChatUtils.info(Formatting.AQUA + "[2bNav] " + Formatting.WHITE + description, new Object[0]);
            }

            @Override
            public void onRouteFinished() {
               XearoHelper.get().deleteCurrentWaypoint(KekBounce.this.currentWaypoint);
               KekBounce.this.currentWaypoint = null;
               XearoHelper.WaypointData next = XearoHelper.get().getOldestWaypoint(true);
               if (next == null) {
                  ChatUtils.info(Formatting.AQUA + "[2bNav] " + Formatting.WHITE + "No waypoints found, assuming destination was reached!", new Object[0]);
                  KekBounce.this.toggle();
               } else {
                  KekBounce.this.navigateToWaypoint(next);
               }
            }
         });
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.isActive()) {
         if (this.mc.player != null && this.mc.world != null) {
            this.fakeFlying = false;
            if (this.pitchRotationHeld) {
               RotationManager.release(0);
               this.pitchRotationHeld = false;
            }

            if (this.mc.player.isOnGround()) {
               this.bounceStartY = this.mc.player.getY();
            }

            if (this.mode2.get() == KekBounce.Mode.NAVIGATE) {
               this.tickNavigate();
            } else {
               this.tickSimple();
            }

            if (!this.fakeFlying && (this.holdingPongs || CommandQueue.isCapturing())) {
               this.releaseFakeFlyHold();
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (event.packet instanceof PlayerPositionLookS2CPacket) {
         this.pendingSetbacks.incrementAndGet();
      }

      if (this.fakeFlying) {
         if (!(event.packet instanceof PlayerPositionLookS2CPacket)) {
            if (event.packet instanceof EntityTrackerUpdateS2CPacket packet && (Integer)this.fakeFlyHold.get() > 0) {
               ClientPlayerEntity player = this.mc.player;
               if (player != null && packet.id() == player.getId() && player.isGliding()) {
                  List<SerializedEntry<?>> values = packet.trackedValues();

                  for (int i = 0; i < values.size(); i++) {
                     SerializedEntry<?> entry = values.get(i);
                     if (entry.id() == 0 && entry.value() instanceof Byte flags) {
                        if ((flags & -128) != 0) {
                           return;
                        }

                        TrackedDataHandler<Byte> handler = (TrackedDataHandler)entry.handler();
                        synchronized (this.holdLock) {
                           try {
                              values.set(i, new SerializedEntry(entry.id(), handler, (byte)(flags | -128)));
                           } catch (UnsupportedOperationException var12) {
                              return;
                           }

                           this.holdTicks = 0;
                           this.holdingPongs = true;
                           CommandQueue.beginCapture();
                           return;
                        }
                     }
                  }
               }
            }
         } else {
            if (this.holdingPongs || CommandQueue.isCapturing()) {
               CommandQueue.flush();
               this.mc.execute(this::releaseFakeFlyHold);
            }
         }
      }
   }

   @EventHandler
   private void onPlaySound(PlaySoundEvent event) {
      if (this.fakeFlying && (Boolean)this.muteSwapSounds.get()) {
         String path = event.sound.getId().getPath();
         if (path.startsWith("item.armor.equip_") || path.equals("item.elytra.flying")) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.fakeFlying = false;
      this.holdingPongs = false;
      this.holdTicks = 0;
      CommandQueue.clear();
   }

   private void tickNavigate() {
      if (this.repairing) {
         this.tickRepair();
      } else if (this.shouldStartRepair()) {
         this.repairing = true;
      } else {
         if (this.mode2.get() == KekBounce.Mode.NAVIGATE && (Boolean)this.renderPathOnMap.get()) {
            this.renderAllWaypointPaths();
         }

         if ((Boolean)this.showEta.get()) {
            this.showEtaHud();
         }

         if (this.navigator != null) {
            if ((Boolean)this.simpleObstaclePasser.get() && this.navigator.getState() == HighwayNavigator.State.PATHING) {
               this.tickObstaclePasser();
            }

            this.navigator.tick();
            if (this.isBouncing()) {
               this.mc.player.setSprinting(true);
               if (!BaritoneHelper.isBusy()) {
                  this.applyPitch();
               }

               ModuleControl.setKeyPressed(this.mc.options.jumpKey, true);
               ModuleControl.setKeyPressed(this.mc.options.forwardKey, true);
               this.tickGlide();
            }
         }
      }
   }

   private void tickSimple() {
      if ((Boolean)this.simpleObstaclePasser.get()) {
         this.tickObstaclePasser();
      }

      if (!this.obstaclePathing && !BaritoneHelper.isBusy()) {
         this.mc.player.setSprinting(true);
         if (this.repairing) {
            this.tickRepair();
         } else if (this.shouldStartRepair()) {
            this.repairing = true;
         } else {
            this.applyYawLock();
            this.applyPitch();
            ModuleControl.setKeyPressed(this.mc.options.jumpKey, true);
            ModuleControl.setKeyPressed(this.mc.options.forwardKey, true);
            this.tickGlide();
         }
      } else {
         this.stopGliding(false);
      }
   }

   private void applyPitch() {
      if ((Boolean)this.lockPitch.get()) {
         ElytraPitchOptimizer policy = this.variablePitch.get() ? getPitchOptimizer() : null;
         if (policy != null
            && !this.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
            && !this.mc.player.hasStatusEffect(StatusEffects.LEVITATION)
            && !this.mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            float p = policy.bestPitch(this.mc.player.getY() - this.bounceStartY, this.mc.player.getVelocity().y, this.mc.player.isOnGround());
            if (RotationManager.setPitch(p, 0)) {
               this.pitchRotationHeld = true;
            }
         } else {
            this.mc.player.setPitch(((Double)this.pitch2.get()).floatValue());
         }
      }
   }

   private static ElytraPitchOptimizer getPitchOptimizer() {
      if (pitchOptimizer == null && !pitchSolverStarted) {
         pitchSolverStarted = true;
         Thread solver = new Thread(() -> pitchOptimizer = ElytraPitchOptimizer.create(0.08, 0.42), "musheor-bounce-pitch");
         solver.setDaemon(true);
         solver.start();
      }

      return pitchOptimizer;
   }

   private void tickGlide() {
      if (!(Boolean)this.fakeFly.get() || !this.tickFakeFly()) {
         if (!this.mc.player.isGliding() && !this.mc.player.isOnGround()) {
            this.mc.player.startGliding();
            this.mc
               .getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         }
      }
   }

   private boolean tickFakeFly() {
      if (((AutoEat)Modules.get().get(AutoEat.class)).eating) {
         if (!this.pausedForEating) {
            this.pausedForEating = true;
            if (!this.isWearingElytra() && this.isHoldingElytra()) {
               this.swapChestElytra();
            }

            if (this.savedHotbarSlot >= 0) {
               musheor.utils.manager.InventoryManager.selectHotbarSlot(this.savedHotbarSlot);
            }
         }

         return false;
      } else {
         if (this.pausedForEating) {
            this.pausedForEating = false;
            if (this.resolveElytraSlot() >= 0) {
               musheor.utils.manager.InventoryManager.selectHotbarSlot(this.elytraHotbarSlot);
            }

            if (this.isWearingElytra() && this.isHoldingChestplate()) {
               this.swapChestElytra();
            }
         }

         if (this.resolveElytraSlot() < 0) {
            return false;
         } else {
            musheor.utils.manager.InventoryManager.selectHotbarSlot(this.elytraHotbarSlot);
            this.fakeFlying = true;
            this.bounceTick++;
            boolean ground = this.mc.player.isOnGround();
            if (ground) {
               this.airborneTicks = 0;
               this.canRedeploy = true;
            } else {
               this.airborneTicks++;
            }

            if (this.isWearingElytra() && this.isHoldingChestplate() && this.bounceTick - this.lastSwapTick >= 2) {
               this.lastSwapTick = this.bounceTick;
               this.swapChestElytra();
            }

            if (this.holdingPongs) {
               this.holdTicks++;
               boolean nextHop = this.canRedeploy && this.airborneTicks >= 2;
               if (nextHop || this.holdTicks > (Integer)this.fakeFlyHold.get()) {
                  this.releaseFakeFlyHold();
               }
            }

            if (!ground && this.airborneTicks >= 2 && !this.mc.player.isGliding() && this.bounceTick - this.lastRedeployTick >= 2) {
               this.redeployGlide();
            }

            return true;
         }
      }
   }

   private void redeployGlide() {
      if (this.isHoldingElytra() && !this.isWearingElytra()) {
         this.swapChestElytra();
         if (this.isWearingElytra()) {
            this.mc
               .getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            this.mc.player.startGliding();
            this.swapChestElytra();
            this.lastRedeployTick = this.bounceTick;
            this.canRedeploy = false;
         }
      }
   }

   private void releaseFakeFlyHold() {
      boolean held;
      synchronized (this.holdLock) {
         held = this.holdingPongs;
         this.holdingPongs = false;
         this.holdTicks = 0;
         CommandQueue.flush();
      }

      if (held && this.mc.player != null && this.mc.player.isGliding()) {
         this.mc.player.stopGliding();
      }
   }

   private void swapChestElytra() {
      if (this.mc.interactionManager != null) {
         float savedYaw = this.mc.player.getYaw();
         float savedPitch = this.mc.player.getPitch();
         this.mc.player.setYaw(RotationManager.yawOr(savedYaw));
         this.mc.player.setPitch(RotationManager.pitchOr(savedPitch));

         try {
            this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
         } finally {
            this.mc.player.setYaw(savedYaw);
            this.mc.player.setPitch(savedPitch);
         }
      }
   }

   private int resolveElytraSlot() {
      if (this.elytraHotbarSlot >= 0 && this.elytraHotbarSlot <= 8) {
         ItemStack held = this.mc.player.getInventory().getStack(this.elytraHotbarSlot);
         if (held.isOf(Items.ELYTRA) || isChestArmor(held)) {
            return this.elytraHotbarSlot;
         }
      }

      return this.elytraHotbarSlot = ElytraFireworkManager.findUsableElytraHotbarSlot();
   }

   private boolean isWearingElytra() {
      return this.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   private boolean isHoldingElytra() {
      return this.mc.player.getMainHandStack().isOf(Items.ELYTRA);
   }

   private boolean isHoldingChestplate() {
      return isChestArmor(this.mc.player.getMainHandStack());
   }

   private static boolean isChestArmor(ItemStack stack) {
      if (stack.isOf(Items.ELYTRA)) {
         return false;
      } else {
         EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
         return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
      }
   }

   private void tickObstaclePasser() {
      if (this.obstaclePathing) {
         if (BaritoneHelper.isBusy() && ++this.obstaclePathTicks < 200) {
            this.stopGliding(false);
         } else {
            this.finishObstaclePath();
         }
      } else if (this.obstacleCooldown > 0) {
         this.obstacleCooldown--;
      } else {
         boolean yLevelWrong = this.mode2.get() != KekBounce.Mode.NAVIGATE
            && (
               VersionHelper.get().getPlayerPos().getY() < ((Integer)this.yLevel.get()).intValue()
                  || VersionHelper.get().getPlayerPos().getY() > (Integer)this.yLevel.get() + 2
            );
         if (!yLevelWrong && (!this.mc.player.horizontalCollision || this.mc.player.collidedSoftly)) {
            if (!this.repairing && this.isStuck()) {
               this.startObstaclePath();
            }
         } else {
            this.startObstaclePath();
         }
      }
   }

   private void startObstaclePath() {
      this.obstaclePathing = true;
      this.clearStuckHistory();
      this.obstaclePathTicks = 0;
      this.stuckRetryCount = 0;
      this.stopGliding(false);
      BaritoneHelper.goTo(this.obstacleGoalPos((Integer)this.distance2.get()));
   }

   private void finishObstaclePath() {
      if (BaritoneHelper.isBusy()) {
         BaritoneHelper.cancel();
      }

      this.obstaclePathing = false;
      this.clearStuckHistory();
      this.obstaclePathTicks = 0;
      this.obstacleCooldown = 20;
      this.stuckRetryCount = 0;
      if (this.mode2.get() != KekBounce.Mode.NAVIGATE && this.highwayDirection != null) {
         this.mc.player.setYaw(HighwayUtils.HighwayDirection.toYaw(this.highwayDirection));
      }
   }

   private void applyYawLock() {
      KekBounce.YawLock yawMode = (KekBounce.YawLock)this.yawLock.get();
      switch (yawMode) {
         case SNAP_ON_ACTIVATION:
            if (this.snappedYaw != null) {
               this.mc.player.setYaw(HighwayUtils.HighwayDirection.toYaw(HighwayUtils.HighwayDirection.fromYaw(this.snappedYaw)));
            }
            break;
         case SNAP_NEAREST_AXIS:
            HighwayUtils.HighwayDirection direction = HighwayUtils.directionFromYaw();
            this.mc.player.setYaw(HighwayUtils.HighwayDirection.toYaw(direction));
            break;
         case CUSTOM:
            this.mc.player.setYaw(((Double)this.yaw2.get()).floatValue());
      }
   }

   private void stopGliding(boolean notifyServer) {
      ModuleControl.releaseMovementKeys();
      if (this.mc.player != null && this.mc.player.isGliding()) {
         if (notifyServer && this.mc.getNetworkHandler() != null) {
            this.mc
               .getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         }

         this.mc.player.stopGliding();
      }
   }

   private boolean isStuck() {
      if (this.mc.player == null) {
         return false;
      } else {
         int age = this.mc.player.age;
         if (age != this.lastStuckAge + 1) {
            this.clearStuckHistory();
         }

         this.lastStuckAge = age;
         int window = (Integer)this.stuckWindow.get();

         for (int i = this.pendingSetbacks.getAndSet(0); i > 0; i--) {
            this.recentSetbackAges.addLast(age);
         }

         while (!this.recentSetbackAges.isEmpty() && age - this.recentSetbackAges.peekFirst() >= window) {
            this.recentSetbackAges.removeFirst();
         }

         if ((Integer)this.setbackLimit.get() > 0 && this.recentSetbackAges.size() >= (Integer)this.setbackLimit.get()) {
            return true;
         } else {
            this.recentPositions.addLast(VersionHelper.get().getPlayerPos());
            if (this.recentPositions.size() <= window) {
               return false;
            } else {
               Vec3d then = this.recentPositions.removeFirst();
               Vec3d now = this.recentPositions.peekLast();
               double dx = now.x - then.x;
               double dz = now.z - then.z;
               double min = (Double)this.stuckDistance.get();
               return dx * dx + dz * dz < min * min;
            }
         }
      }
   }

   private void clearStuckHistory() {
      this.recentPositions.clear();
      this.recentSetbackAges.clear();
      this.pendingSetbacks.set(0);
      this.lastStuckAge = Integer.MIN_VALUE;
   }

   private float elytraDurabilityPercent() {
      ItemStack stack = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
      return (1.0F - (float)stack.getDamage() / stack.getMaxDamage()) * 100.0F;
   }

   private boolean shouldStartRepair() {
      if (!(Boolean)this.repairElytra.get()) {
         return false;
      } else if (!this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)) {
         return false;
      } else {
         return ((AutoEat)Modules.get().get(AutoEat.class)).eating ? false : this.elytraDurabilityPercent() <= ((Integer)this.damageThreshold.get()).intValue();
      }
   }

   private void tickRepair() {
      if (!((AutoEat)Modules.get().get(AutoEat.class)).eating) {
         if (this.elytraDurabilityPercent() >= ((Integer)this.repairThreshold.get()).intValue()) {
            this.repairing = false;
         } else {
            int xpSlot = musheor.utils.manager.InventoryManager.findItemSlot(Items.EXPERIENCE_BOTTLE);
            if (xpSlot == -1) {
               ChatUtils.info("%sNo experience bottles found, waiting ...", new Object[]{Formatting.GOLD});
            } else if (xpSlot > 8) {
               musheor.utils.manager.InventoryManager.moveItemToHotbar(Items.EXPERIENCE_BOTTLE);
            } else {
               ModuleControl.releaseMovementKeys();
               this.mc.player.getInventory().selectedSlot = xpSlot;
               this.mc.player.setPitch(90.0F);
               this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            }
         }
      }
   }

   private BlockPos obstacleGoalPos(int distance) {
      int px = this.mc.player.getBlockX();
      int py = (Integer)this.yLevel.get();
      int pz = this.mc.player.getBlockZ();
      if (this.mode2.get() == KekBounce.Mode.NAVIGATE) {
         return switch (this.highwayDirection) {
            case NORTH -> new BlockPos(px, py, pz - distance);
            case SOUTH -> new BlockPos(px, py, pz + distance);
            case EAST -> new BlockPos(px + distance, py, pz);
            case WEST -> new BlockPos(px - distance, py, pz);
            case NORTHEAST -> new BlockPos(px + distance, py, pz - distance);
            case NORTHWEST -> new BlockPos(px - distance, py, pz - distance);
            case SOUTHEAST -> new BlockPos(px + distance, py, pz + distance);
            case SOUTHWEST -> new BlockPos(px - distance, py, pz + distance);
         };
      } else {
         int sx = this.startPos.getX();
         int sz = this.startPos.getZ();

         return switch (this.highwayDirection) {
            case NORTH -> new BlockPos(sx, py, pz - distance);
            case SOUTH -> new BlockPos(sx, py, pz + distance);
            case EAST -> new BlockPos(px + distance, py, sz);
            case WEST -> new BlockPos(px - distance, py, sz);
            case NORTHEAST -> {
               int gx = px + distance;
               yield new BlockPos(gx, py, sx + sz - gx);
            }
            case NORTHWEST -> {
               int gx = px - distance;
               yield new BlockPos(gx, py, gx - (sx - sz));
            }
            case SOUTHEAST -> {
               int gx = px + distance;
               yield new BlockPos(gx, py, gx - (sx - sz));
            }
            case SOUTHWEST -> {
               int gx = px - distance;
               yield new BlockPos(gx, py, sx + sz - gx);
            }
         };
      }
   }

   private void drawRouteOnMap(HighwayRouter.Route route) {
      if (XearoHelper.isLoaded()) {
         List<XearoHelper.LineData> lines = new ArrayList<>();
         List<HighwayUtils.XZ> wps = route.waypoints;

         for (int i = 0; i + 1 < wps.size(); i++) {
            lines.add(
               new XearoHelper.LineData(
                  (int)wps.get(i).x(), (int)wps.get(i).z(), (int)wps.get(i + 1).x(), (int)wps.get(i + 1).z()
               )
            );
         }

         XearoHelper.get().drawLinesOnMap(lines, -16733441);
      }
   }

   private void renderAllWaypointPaths() {
      if (XearoHelper.isLoaded()) {
         List<XearoHelper.WaypointData> wps = XearoHelper.get().getWaypoints(true);
         if (wps != null) {
            if (wps.size() != this.lastWaypointCount) {
               this.lastWaypointCount = wps.size();
               List<XearoHelper.WaypointData> sorted = new ArrayList<>(wps);
               sorted.sort(Comparator.comparingLong(XearoHelper.WaypointData::createdAt));
               List<XearoHelper.LineData> allLines = new ArrayList<>();
               double fromX = this.mc.player.getX();
               double fromZ = this.mc.player.getZ();

               for (XearoHelper.WaypointData wp : sorted) {
                  HighwayRouter.Route route = HighwayRouter.findRoute(fromX, fromZ, wp.x(), wp.z());
                  if (route != null) {
                     List<HighwayUtils.XZ> pts = route.waypoints;

                     for (int i = 0; i + 1 < pts.size(); i++) {
                        allLines.add(
                           new XearoHelper.LineData(
                              (int)pts.get(i).x(),
                              (int)pts.get(i).z(),
                              (int)pts.get(i + 1).x(),
                              (int)pts.get(i + 1).z()
                           )
                        );
                     }
                  }

                  fromX = wp.x();
                  fromZ = wp.z();
               }

               XearoHelper.get().drawLinesOnMap(allLines, -16733441);
            }
         }
      }
   }

   private void showEtaHud() {
      if (this.currentWaypoint != null) {
         String etaStr = XearoHelper.get().getEtaSuffix(this.currentWaypoint);
         if (etaStr == null || etaStr.isBlank()) {
            etaStr = "...";
         }

         this.mc
            .player
            .sendMessage(
               Text.literal("[2bNav] ")
                  .setStyle(Style.EMPTY.withColor(Formatting.AQUA))
                  .append(Text.literal("ETA ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                  .append(Text.literal(etaStr).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true))),
               true
            );
      }
   }

   public boolean isBouncing() {
      return this.isActive() && !this.obstaclePathing && !this.repairing && !BaritoneHelper.isBusy();
   }

   private static enum Height {
      CURRENT,
      CUSTOM;}

   public static enum Mode {
      SIMPLE,
      NAVIGATE;}

   private static enum StartPos {
      AUTOMATIC,
      CUSTOM;}

   public static enum YawLock {
      SNAP_ON_ACTIVATION,
      SNAP_NEAREST_AXIS,
      CUSTOM,
      DISABLED;}
}

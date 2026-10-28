package musheor.modules.automation;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import javax.net.ssl.HttpsURLConnection;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.compat.XearoHelper;
import musheor.modules.features.CoordHider;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.WorldChunk;

public class Dispatcher extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgVisualRange = this.settings.createGroup("Visual Range");
   private final SettingGroup sgStashes = this.settings.createGroup("Stashes");
   private final SettingGroup sgEnderPearls = this.settings.createGroup("Ender Pearls");
   private final Setting<Dispatcher.NotificationType> notificationType = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("notification-type")).description("How notifications are being handled"))
               .defaultValue(Dispatcher.NotificationType.BOTH))
            .build()
      );
   private final Setting<Integer> minimumDistance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("minimum-distance"))
                  .description("Minimum distance away from spawn before alerts are sent"))
               .defaultValue(1000))
            .sliderRange(0, 10000)
            .build()
      );
   private final Setting<Boolean> webhook2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("webhook"))
                     .description("Sends notifications over a discord webhook"))
                  .defaultValue(true))
               .visible(
                  () -> this.notificationType.get() == Dispatcher.NotificationType.BOTH
                     || this.notificationType.get() == Dispatcher.NotificationType.TOAST
               ))
            .build()
      );
   public final Setting<String> webhookLink = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("webhook-link"))
                  .defaultValue(""))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
               ))
            .build()
      );
   public final Setting<Integer> webhookTimeoutMs = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("webhook-timeout-MS"))
                     .description("Timeout for webhook requests in ms, set to 0 to disable, increase when getting rate-limited"))
                  .defaultValue(350))
               .sliderRange(0, 5000)
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
               ))
            .build()
      );
   private final Setting<Boolean> webhookCoords = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("webhook-coords"))
                     .description("Includes coordinates in webhook message"))
                  .defaultValue(true))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
               ))
            .build()
      );
   private final Setting<Boolean> allowPing = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("allow-ping"))
                     .description("Pings a role when a notification is sent"))
                  .defaultValue(false))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
               ))
            .build()
      );
   public final Setting<String> roleId = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("role-ID"))
                  .defaultValue(""))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
                     && (Boolean)this.allowPing.get()
               ))
            .build()
      );
   private final Setting<Boolean> createWaypoints = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                           .name("create-waypoints"))
                        .description("Creates xaeros minimap waypoints"))
                     .defaultValue(false))
                  .onChanged(value -> {
                     if (XearoHelper.isLoaded()) {
                        XearoHelper.get().updateWaypointSettings();
                     }
                  }))
               .visible(XearoHelper::isLoaded))
            .build()
      );
   private final Setting<Boolean> recordSigns = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("record-signs"))
                     .description("Alerts and records signs and their text"))
                  .defaultValue(true))
               .visible(() -> false))
            .build()
      );
   private final Setting<Boolean> playSound = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("play-sound"))
                     .description("Plays a sound when dispatch event is triggered"))
                  .defaultValue(true))
               .visible(
                  () -> this.notificationType.get() == Dispatcher.NotificationType.BOTH
                     || this.notificationType.get() == Dispatcher.NotificationType.CHAT
               ))
            .build()
      );
   private final Setting<Boolean> visualRangeAlerts = this.sgVisualRange
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("visual-range-alerts"))
                  .description("Sends an alert when a player enters or leaves visual range"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Dispatcher.VisualRangeType> visualRangeType = this.sgVisualRange
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("visual-range-type"))
                     .description("Choose what type of visual range alerts you want to receive"))
                  .defaultValue(Dispatcher.VisualRangeType.ENTER))
               .visible(this.visualRangeAlerts::get))
            .build()
      );
   private final Setting<Boolean> webhookVisualRange = this.sgVisualRange
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("webhook-visual-range"))
                     .description("Sends visual range alerts over discord webhook"))
                  .defaultValue(true))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
                     && (Boolean)this.visualRangeAlerts.get()
               ))
            .build()
      );
   private final Setting<Boolean> waypointVisualRange = this.sgVisualRange
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("waypoint-visual-range"))
                     .description("Creates a xaero waypoint when a player enters or leaves visual range"))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.visualRangeAlerts.get() && (Boolean)this.createWaypoints.get()))
            .build()
      );
   private final Setting<Boolean> pingVisualRange = this.sgVisualRange
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("ping-visual-range"))
                     .description("Pings a discord role when a player enters or leaves visual range"))
                  .defaultValue(false))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
                     && (Boolean)this.visualRangeAlerts.get()
                     && (Boolean)this.allowPing.get()
               ))
            .build()
      );
   private final Setting<Boolean> stashAlerts = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("stash-alerts"))
                  .description("Sends an alert when a stash or unnatural blocks are found"))
               .defaultValue(true))
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> containerList = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
                     .name("container-list"))
                  .description("List of container blocks to track or search for"))
               .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
               .visible(this.stashAlerts::get))
            .build()
      );
   private final Setting<Integer> containerThreshold = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("container-threshold"))
                     .description("How many containers need to be in visual range before an alert is sent"))
                  .defaultValue(8))
               .sliderRange(1, 64)
               .visible(this.stashAlerts::get))
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> instantHitList = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
                     .name("instant-hit-list"))
                  .description("List of container blocks to track or search for, when a single one is found it will alert"))
               .defaultValue(new BlockEntityType[]{BlockEntityType.SHULKER_BOX})
               .visible(this.stashAlerts::get))
            .build()
      );
   private final Setting<Boolean> webhookStashAlerts = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("webhook-stash-alerts"))
                     .description("Sends visual range alerts over discord webhook"))
                  .defaultValue(true))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
                     && (Boolean)this.stashAlerts.get()
               ))
            .build()
      );
   private final Setting<Boolean> waypointStash = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("waypoint-stash"))
                     .description("Creates a xaero waypoint when a stash is found"))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.stashAlerts.get() && (Boolean)this.createWaypoints.get()))
            .build()
      );
   private final Setting<Boolean> pingStashAlerts = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("ping-stash-alerts"))
                     .description("Pings a discord role when a player enters or leaves visual range"))
                  .defaultValue(false))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
                     && (Boolean)this.stashAlerts.get()
                     && (Boolean)this.allowPing.get()
               ))
            .build()
      );
   private final Setting<Boolean> illegalBedrock = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("illegal-bedrock"))
                  .description("Sends an alert when illegal bedrock is found"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> waypointIllegalBedrock = this.sgStashes
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("waypoint-illegal-bedrock"))
                     .description("Creates a xaero waypoint when illegal bedrock is found"))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.illegalBedrock.get() && (Boolean)this.createWaypoints.get()))
            .build()
      );
   private final Setting<Boolean> pearlAlerts = this.sgEnderPearls
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("pearl-alerts"))
                  .description("Sends an alert when an enderpearl or stasis-chamber is found"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> webhookPearlAlerts = this.sgEnderPearls
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("webhook-pearl-alerts"))
                     .description("Sends a pearl alerts over discord webhook"))
                  .defaultValue(true))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
                     && (Boolean)this.pearlAlerts.get()
               ))
            .build()
      );
   private final Setting<Boolean> waypointPearl = this.sgEnderPearls
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("waypoint-pearl"))
                     .description("Creates a xaero waypoint when an enderpearl is found"))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.pearlAlerts.get() && (Boolean)this.createWaypoints.get()))
            .build()
      );
   private final Setting<Boolean> pingPearlAlerts = this.sgEnderPearls
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("ping-pearl-alerts"))
                     .description("Pings a discord role when a pearl or stasis chamber is found"))
                  .defaultValue(false))
               .visible(
                  () -> (
                        this.notificationType.get() == Dispatcher.NotificationType.BOTH
                           || this.notificationType.get() == Dispatcher.NotificationType.TOAST
                     )
                     && (Boolean)this.webhook2.get()
                     && (Boolean)this.pearlAlerts.get()
                     && (Boolean)this.allowPing.get()
               ))
            .build()
      );
   private final Map<Integer, Vec3d> seenPearlPositions = new HashMap<>();
   private final Map<Long, List<Vec3d>> pendingPearlAlerts = new HashMap<>();
   private final Set<Long> scannedChunks = new HashSet<>();
   private long pearlAlertReadyAtMs = 0L;
   private Object previousWaypointSet = null;
   private final LinkedBlockingQueue<String> webhookQueue = new LinkedBlockingQueue<>();
   private Thread webhookThread;

   public Dispatcher() {
      super(Musheor.AUTOMATION, "dispatcher", "Records specific data and notifies the player in chat or through a discord webhook");
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.world != null) {
         if (XearoHelper.isLoaded()) {
            this.previousWaypointSet = XearoHelper.get().getCurrentWaypointSetHandle();
            if (this.previousWaypointSet != null) {
               XearoHelper.get().setWaypointSet("Dispatcher");
            }
         }

         this.webhookThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
               try {
                  String json = this.webhookQueue.take();
                  postWebhook((String)this.webhookLink.get(), json);
                  int timeout = (Integer)this.webhookTimeoutMs.get();
                  if (timeout > 0) {
                     Thread.sleep(timeout);
                  }
               } catch (InterruptedException var3) {
                  Thread.currentThread().interrupt();
               }
            }
         });
         this.webhookThread.setDaemon(true);
         this.webhookThread.start();
      }
   }

   public void onDeactivate() {
      this.scannedChunks.clear();
      this.pendingPearlAlerts.clear();
      this.seenPearlPositions.clear();
      this.webhookQueue.clear();
      if (this.webhookThread != null) {
         this.webhookThread.interrupt();
         this.webhookThread = null;
      }

      if (XearoHelper.isLoaded()) {
         XearoHelper.get().restoreWaypointSet(this.previousWaypointSet);
      }
   }

   @EventHandler
   private void onChunkData(ChunkDataEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.stashAlerts.get()) {
            Vec3d playerPos = VersionHelper.get().getPlayerPos();
            if (!(Math.abs(playerPos.x) < ((Integer)this.minimumDistance.get()).intValue()) && !(Math.abs(playerPos.z) < ((Integer)this.minimumDistance.get()).intValue())) {
               WorldChunk chunk = event.chunk();
               long chunkKey = ChunkPos.toLong(chunk.getPos().x, chunk.getPos().z);
               if (this.scannedChunks.add(chunkKey)) {
                  this.scanChunkForStash(chunk.getPos());
               }
            }
         }
      }
   }

   @EventHandler
   private void onEntityAdded(EntityAddedEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         Vec3d playerPos = VersionHelper.get().getPlayerPos();
         if (!(Math.abs(playerPos.x) < ((Integer)this.minimumDistance.get()).intValue()) && !(Math.abs(playerPos.z) < ((Integer)this.minimumDistance.get()).intValue())) {
            if ((Boolean)this.visualRangeAlerts.get()
               && this.visualRangeType.get() != Dispatcher.VisualRangeType.LEAVE
               && event.entity.getUuid() != this.mc.player.getUuid()
               && event.entity instanceof PlayerEntity player) {
               if (this.notificationType.get() == Dispatcher.NotificationType.BOTH || this.notificationType.get() == Dispatcher.NotificationType.CHAT) {
                  if (((CoordHider)Modules.get().get(CoordHider.class)).isActive()) {
                     ChatUtils.sendMsg(
                        player.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has entered visual range!", new Object[]{player.getName().getString()}
                     );
                  } else {
                     ChatUtils.sendMsg(
                        player.getId() + 100,
                        Formatting.GRAY,
                        "(highlight)%s(default) has entered visual range at %d, %d, %d!",
                        new Object[]{player.getName().getString(), player.getBlockX(), player.getBlockY(), player.getBlockZ()}
                     );
                  }
               }

               if ((Boolean)this.webhook2.get() && (Boolean)this.webhookVisualRange.get() && !((String)this.webhookLink.get()).isEmpty()) {
                  String pos = this.webhookCoords.get() ? String.format("||%d, %d, %d||", player.getBlockX(), player.getBlockY(), player.getBlockZ()) : "";
                  this.enqueueWebhook(
                     "Visual Range",
                     player.getName().getString() + " has entered visual range!\n" + pos,
                     16733525,
                     (Boolean)this.pingVisualRange.get() && (Boolean)this.allowPing.get()
                  );
               }

               if ((Boolean)this.createWaypoints.get() && (Boolean)this.waypointVisualRange.get() && XearoHelper.isLoaded()) {
                  XearoHelper.get()
                     .addWaypointToCurrent(
                        "VisualRange E: " + player.getName().getString(), "P", VersionHelper.get().getPlayerPos(), XearoHelper.WaypointColorHint.WHITE
                     );
               }

               if ((Boolean)this.playSound.get()) {
                  this.mc
                     .world
                     .playSoundFromEntity(this.mc.player, this.mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
               }
            }

            if ((Boolean)this.pearlAlerts.get() && event.entity instanceof EnderPearlEntity pearlEntity && !this.seenPearlPositions.containsKey(pearlEntity.getId())) {
               Vec3d pos = new Vec3d(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ());
               this.seenPearlPositions.put(pearlEntity.getId(), pos);
               long chunkKey = ((int)pearlEntity.getX() >> 4) * 1000000L + ((int)pearlEntity.getZ() >> 4);
               this.pendingPearlAlerts.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(pos);
               this.pearlAlertReadyAtMs = System.currentTimeMillis() + 2000L;
            }
         }
      }
   }

   @EventHandler
   private void onEntityRemoved(EntityRemovedEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         Vec3d playerPos = VersionHelper.get().getPlayerPos();
         if (!(Math.abs(playerPos.x) < ((Integer)this.minimumDistance.get()).intValue()) && !(Math.abs(playerPos.z) < ((Integer)this.minimumDistance.get()).intValue())) {
            if ((Boolean)this.visualRangeAlerts.get()
               && this.visualRangeType.get() != Dispatcher.VisualRangeType.ENTER
               && event.entity.getUuid() != this.mc.player.getUuid()
               && event.entity instanceof PlayerEntity player) {
               if (this.notificationType.get() == Dispatcher.NotificationType.BOTH || this.notificationType.get() == Dispatcher.NotificationType.CHAT) {
                  if (((CoordHider)Modules.get().get(CoordHider.class)).isActive()) {
                     ChatUtils.sendMsg(
                        player.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has left visual range!", new Object[]{player.getName().getString()}
                     );
                  } else {
                     ChatUtils.sendMsg(
                        player.getId() + 100,
                        Formatting.GRAY,
                        "(highlight)%s(default) has left visual range at %d, %d, %d!",
                        new Object[]{player.getName().getString(), player.getBlockX(), player.getBlockY(), player.getBlockZ()}
                     );
                  }
               }

               if ((Boolean)this.webhook2.get() && (Boolean)this.webhookVisualRange.get() && !((String)this.webhookLink.get()).isEmpty()) {
                  String pos = this.webhookCoords.get() ? String.format("||%d, %d, %d||", player.getBlockX(), player.getBlockY(), player.getBlockZ()) : "";
                  this.enqueueWebhook(
                     "Visual Range",
                     player.getName().getString() + " has left visual range!\n" + pos,
                     8530731,
                     (Boolean)this.pingVisualRange.get() && (Boolean)this.allowPing.get()
                  );
               }

               if ((Boolean)this.createWaypoints.get() && (Boolean)this.waypointVisualRange.get() && XearoHelper.isLoaded()) {
                  XearoHelper.get()
                     .addWaypointToCurrent(
                        "VisualRange L: " + player.getName().getString(), "P", VersionHelper.get().getPlayerPos(), XearoHelper.WaypointColorHint.WHITE
                     );
               }

               if ((Boolean)this.playSound.get()) {
                  this.mc
                     .world
                     .playSoundFromEntity(this.mc.player, this.mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
               }
            }
         }
      }
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (!this.pendingPearlAlerts.isEmpty() && System.currentTimeMillis() >= this.pearlAlertReadyAtMs) {
         for (Entry<Long, List<Vec3d>> entry : this.pendingPearlAlerts.entrySet()) {
            List<Vec3d> pearls = entry.getValue();
            Vec3d first = pearls.getFirst();
            int count = pearls.size();
            if (count == 1) {
               if (this.notificationType.get() == Dispatcher.NotificationType.BOTH || this.notificationType.get() == Dispatcher.NotificationType.CHAT) {
                  if (((CoordHider)Modules.get().get(CoordHider.class)).isActive()) {
                     ChatUtils.sendMsg(0, Formatting.BLUE, "Found an Enderpearl!", new Object[0]);
                  } else {
                     ChatUtils.sendMsg(
                        0,
                        Formatting.BLUE,
                        "Enderpearl %sfound at [%s%d, %d, %d%s]",
                        new Object[]{Formatting.WHITE, Formatting.GRAY, (int)first.x, (int)first.y, (int)first.z, Formatting.WHITE}
                     );
                  }
               }
            } else if (((CoordHider)Modules.get().get(CoordHider.class)).isActive()) {
               ChatUtils.sendMsg(0, Formatting.BLUE, "Found %d Enderpearls!", new Object[]{count});
            } else {
               ChatUtils.sendMsg(
                  0,
                  Formatting.BLUE,
                  "%d Enderpearls %sfound at [%s%d, %d, %d%s]",
                  new Object[]{count, Formatting.WHITE, Formatting.GRAY, (int)first.x, (int)first.y, (int)first.z, Formatting.WHITE}
               );
            }

            if ((Boolean)this.webhook2.get() && (Boolean)this.webhookPearlAlerts.get() && !((String)this.webhookLink.get()).isEmpty()) {
               String pos = this.webhookCoords.get() ? String.format("||%d, %d, %d||", (int)first.x, (int)first.y, (int)first.z) : "";
               this.enqueueWebhook(
                  "Pearl Detected", "Found " + count + " enderpearls!\n" + pos, 5592575, (Boolean)this.pingPearlAlerts.get() && (Boolean)this.allowPing.get()
               );
            }

            if ((Boolean)this.createWaypoints.get() && (Boolean)this.waypointPearl.get() && XearoHelper.isLoaded()) {
               XearoHelper.get().addWaypointToCurrent("Pearls: " + count, "P", first, XearoHelper.WaypointColorHint.BLUE);
            }
         }

         this.pendingPearlAlerts.clear();
      }
   }

   private void scanChunkForStash(ChunkPos chunkPos) {
      if (this.mc.world != null && this.mc.player != null) {
         Vec3d playerPos = VersionHelper.get().getPlayerPos();
         if (!(Math.abs(playerPos.x) < ((Integer)this.minimumDistance.get()).intValue()) && !(Math.abs(playerPos.z) < ((Integer)this.minimumDistance.get()).intValue())) {
            WorldChunk chunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);
            MusheorSystem.debug("[DEBUG] Scanning chunk " + chunkPos.x + ", " + chunkPos.z + " - block entities: " + chunk.getBlockEntities().size());
            Map<BlockEntityType<?>, Integer> foundContainers = new HashMap<>();
            boolean foundSingleAlert = false;
            BlockPos firstContainerPos = null;

            for (Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
               BlockEntity be = entry.getValue();
               BlockPos pos = entry.getKey();
               boolean isThresholdContainer = ((List)this.containerList.get()).contains(be.getType());
               boolean isSingleContainer = ((List)this.instantHitList.get()).contains(be.getType());
               if (isThresholdContainer || isSingleContainer) {
                  foundContainers.merge(be.getType(), 1, Integer::sum);
                  if (firstContainerPos == null) {
                     firstContainerPos = pos;
                  }
               }

               if (isSingleContainer) {
                  foundSingleAlert = true;
               }
            }

            int totalThresholdContainers = ((List)this.containerList.get()).isEmpty()
               ? 0
               : foundContainers.entrySet().stream().filter(e -> ((List)this.containerList.get()).contains(e.getKey())).mapToInt(Entry::getValue).sum();
            boolean thresholdMet = !((List)this.containerList.get()).isEmpty() && totalThresholdContainers >= (Integer)this.containerThreshold.get();
            if ((thresholdMet || foundSingleAlert) && firstContainerPos != null) {
               StringBuilder summary = new StringBuilder();

               for (Entry<BlockEntityType<?>, Integer> entry : foundContainers.entrySet()) {
                  String name = Registries.BLOCK_ENTITY_TYPE.getId(entry.getKey()).toString().replace("minecraft:", "");
                  summary.append("**[").append(entry.getValue()).append("x]** ").append(name).append("\n");
               }

               int totalContainers = foundContainers.values().stream().mapToInt(Integer::intValue).sum();
               String blockCoords = String.format("|%d, %d, %d|", firstContainerPos.getX(), firstContainerPos.getY(), firstContainerPos.getZ());
               String description = summary.toString().trim() + "\n||" + blockCoords + "||";
               if (this.notificationType.get() == Dispatcher.NotificationType.BOTH || this.notificationType.get() == Dispatcher.NotificationType.CHAT) {
                  if (((CoordHider)Modules.get().get(CoordHider.class)).isActive()) {
                     ChatUtils.sendMsg(
                        0, Formatting.WHITE, "Stash found! %s- %s%d containers", new Object[]{Formatting.WHITE, Formatting.AQUA, totalContainers}
                     );
                  } else {
                     ChatUtils.sendMsg(
                        0,
                        Formatting.WHITE,
                        "Stash found at %s%d, %d, %d %s- %s%d containers",
                        new Object[]{
                           Formatting.GRAY,
                           firstContainerPos.getX(),
                           firstContainerPos.getY(),
                           firstContainerPos.getZ(),
                           Formatting.WHITE,
                           Formatting.AQUA,
                           totalContainers
                        }
                     );
                  }
               }

               if ((Boolean)this.webhook2.get() && (Boolean)this.webhookStashAlerts.get() && !((String)this.webhookLink.get()).isEmpty()) {
                  this.enqueueWebhook("GOLD GOLD GOLD", description, 16759603, (Boolean)this.pingStashAlerts.get() && (Boolean)this.allowPing.get());
               }

               if ((Boolean)this.createWaypoints.get() && (Boolean)this.waypointStash.get() && XearoHelper.isLoaded()) {
                  XearoHelper.get().addWaypointToCurrent("Stash: " + totalContainers, "S", Vec3d.of(firstContainerPos), XearoHelper.WaypointColorHint.GOLD);
               }
            }

            if ((Boolean)this.illegalBedrock.get()) {
               boolean isOverworld = this.mc.world.getRegistryKey() == World.OVERWORLD;
               boolean isNether = this.mc.world.getRegistryKey() == World.NETHER;
               boolean isEnd = this.mc.world.getRegistryKey() == World.END;
               if (isEnd) {
                  for (BlockEntity bex : chunk.getBlockEntities().values()) {
                     if (bex instanceof EndGatewayBlockEntity) {
                        return;
                     }
                  }
               }

               BlockPos illegalPos = null;

               for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
                  for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                     int minY = this.mc.world.getBottomY();
                     int maxY = this.mc.world.getTopY(Type.MOTION_BLOCKING, x, z);

                     for (int y = minY; y < maxY; y++) {
                        BlockPos posx = new BlockPos(x, y, z);
                        if (this.mc.world.getBlockState(posx).getBlock() == Blocks.BEDROCK) {
                           boolean illegal = false;
                           if (isOverworld && y > 5) {
                              illegal = true;
                           } else if (isNether && y > 5 && y < 122) {
                              illegal = true;
                           } else if (isEnd) {
                              illegal = true;
                           }

                           if (illegal) {
                              illegalPos = posx;
                              break;
                           }
                        }
                     }

                     if (illegalPos != null) {
                        break;
                     }
                  }

                  if (illegalPos != null) {
                     break;
                  }
               }

               if (illegalPos != null) {
                  String posx = this.webhookCoords.get() ? String.format("|%d, %d, %d|", illegalPos.getX(), illegalPos.getY(), illegalPos.getZ()) : "";
                  if (this.notificationType.get() == Dispatcher.NotificationType.BOTH || this.notificationType.get() == Dispatcher.NotificationType.CHAT
                     )
                   {
                     if (((CoordHider)Modules.get().get(CoordHider.class)).isActive()) {
                        ChatUtils.sendMsg(0, Formatting.RED, "Illegal bedrock found!", new Object[0]);
                     } else {
                        ChatUtils.sendMsg(
                           0,
                           Formatting.RED,
                           "Illegal bedrock found at %s%d, %d, %d",
                           new Object[]{Formatting.GRAY, illegalPos.getX(), illegalPos.getY(), illegalPos.getZ()}
                        );
                     }
                  }

                  if ((Boolean)this.webhook2.get() && (Boolean)this.webhookStashAlerts.get() && !((String)this.webhookLink.get()).isEmpty()) {
                     this.enqueueWebhook(
                        "Illegal Bedrock", "**Found illegal bedrock**!!\n" + posx, 16711935, (Boolean)this.pingStashAlerts.get() && (Boolean)this.allowPing.get()
                     );
                  }

                  if ((Boolean)this.createWaypoints.get() && (Boolean)this.waypointIllegalBedrock.get() && XearoHelper.isLoaded()) {
                     XearoHelper.get().addWaypointToCurrent("Illegal Bedrock", "B", Vec3d.of(illegalPos), XearoHelper.WaypointColorHint.RED);
                  }
               }
            }
         }
      }
   }

   private void enqueueWebhook(String title, String description, int color, boolean pingRole) {
      String content = pingRole && this.allowPing.get() && !((String)this.roleId.get()).isEmpty()
         ? "\"content\": \"<@&" + (String)this.roleId.get() + ">\","
         : "";
      String json = "{"
         + content
         + "\"embeds\": [{\"title\": \""
         + title.replace("\"", "\\\"")
         + "\",\"description\": \""
         + description.replace("\"", "\\\"").replace("\n", "\\n")
         + "\",\"color\": "
         + color
         + "}]}";
      this.webhookQueue.offer(json);
   }

   private static void postWebhook(String webhookURL, String json) {
      try {
         URL url = new URL(webhookURL);
         HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
         connection.addRequestProperty("Content-Type", "application/json");
         connection.addRequestProperty("User-Agent", "Mozilla");
         connection.setDoOutput(true);
         connection.setRequestMethod("POST");
         OutputStream stream = connection.getOutputStream();
         stream.write(json.getBytes());
         stream.flush();
         stream.close();
         connection.getInputStream().close();
         connection.disconnect();
      } catch (UnknownServiceException | MalformedURLException var5) {
      } catch (Exception var6) {
         var6.printStackTrace();
      }
   }

   private static enum NotificationType {
      CHAT,
      TOAST,
      BOTH;}

   private static enum VisualRangeType {
      ENTER,
      LEAVE,
      BOTH;}
}

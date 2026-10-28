package musheor.modules.tech;

import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.modules.features.MoreTags;
import musheor.utils.PearlStore;
import musheor.utils.HighwayUtils;
import musheor.utils.manager.InventoryManager;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class MessageInteract extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<MessageInteract.Action> action2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("action")).description("What kind of action to perform."))
               .defaultValue(MessageInteract.Action.BOTH))
            .build()
      );
   private final Setting<BlockPos> position2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)((meteordevelopment.meteorclient.settings.BlockPosSetting.Builder)new meteordevelopment.meteorclient.settings.BlockPosSetting.Builder()
                        .name("position"))
                     .description("What block position the action is supposed to be for"))
                  .defaultValue(new BlockPos(0, 0, 0)))
               .visible(() -> this.action2.get() != MessageInteract.Action.BOTH))
            .build()
      );
   private final Setting<Boolean> dropPearl = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("drop-pearl"))
                     .description("Drops a pearl after performing the interaction"))
                  .defaultValue(true))
               .visible(() -> this.action2.get() == MessageInteract.Action.BOTH))
            .build()
      );
   private final Setting<Boolean> returnToStartPos = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("return-to-start-pos"))
                     .description("Pathfinds back to your starting position."))
                  .defaultValue(true))
               .visible(() -> this.action2.get() == MessageInteract.Action.BOTH))
            .build()
      );
   private final Setting<String> allowedPlayer = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("allowed-player"))
                  .description("If set, only reacts to !tp whispers from this player. Leave empty to allow anyone."))
               .defaultValue(""))
            .build()
      );
   boolean pendingAction = false;
   int interactDelayTicks = 0;
   private int lookAlignTicks = 0;
   private BlockPos targetTrapdoor = null;
   private BlockPos startPos = null;
   private static final Pattern WHISPER_PATTERN = Pattern.compile("([A-Za-z0-9_]{3,16}) whispers: ");
   private static final Random RANDOM = new Random();

   public MessageInteract() {
      super(Musheor.MAIN, "message-interact", "Performs a certain action upon receiving a message from a specific player.");
   }

   public void onDeactivate() {
      musheor.utils.BaritoneHelper.cancel();
      this.pendingAction = false;
      this.interactDelayTicks = 0;
      this.lookAlignTicks = 0;
      this.targetTrapdoor = null;
      this.startPos = null;
   }

   public void onActivate() {
      if (this.action2.get() == MessageInteract.Action.BOTH && !((MoreTags)Modules.get().get(MoreTags.class)).isActive()) {
         ((MoreTags)Modules.get().get(MoreTags.class)).toggle();
      }
   }

   private BlockPos findPearlTrapdoor(String ownerName) {
      if (mc.world != null && mc.player != null) {
         BlockPos closest = null;
         double closestDist = Double.MAX_VALUE;

         for (Entry<Integer, String> entry : MoreTags.pearlOwners.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(ownerName) && mc.world.getEntityById(entry.getKey()) instanceof EnderPearlEntity pearl) {
               for (BlockPos candidate : new BlockPos[]{pearl.getBlockPos(), pearl.getBlockPos().down()}) {
                  if (mc.world.getBlockState(candidate).getBlock() instanceof TrapdoorBlock) {
                     double dist = VersionHelper.get().getPlayerPos().squaredDistanceTo(Vec3d.ofCenter(candidate));
                     if (dist < closestDist) {
                        closestDist = dist;
                        closest = candidate;
                     }
                  }
               }
            }
         }

         if (closest != null) {
            return closest;
         }
      }

      List<PearlStore.PearlRecord> saved = PearlStore.findByOwner(ownerName);
      if (saved.isEmpty()) {
         return null;
      } else {
         return mc.player != null && saved.size() != 1
            ? saved.stream()
               .min(Comparator.comparingDouble(r -> VersionHelper.get().getPlayerPos().squaredDistanceTo(Vec3d.ofCenter(r.trapdoorPos))))
               .map(r -> r.trapdoorPos)
               .orElse(null)
            : saved.getFirst().trapdoorPos;
      }
   }

   @EventHandler
   private void onReceiveMessage(ReceiveMessageEvent event) {
      String msg = event.getMessage().getString();
      if (msg.contains("!tp")) {
         Matcher matcher = WHISPER_PATTERN.matcher(msg);
         if (matcher.find()) {
            String senderName = matcher.group(1);
            this.info("Received message from: " + senderName, new Object[0]);
            if (((String)this.allowedPlayer.get()).isEmpty() || senderName.equalsIgnoreCase((String)this.allowedPlayer.get())) {
               if (this.action2.get() == MessageInteract.Action.BOTH) {
                  this.targetTrapdoor = this.findPearlTrapdoor(senderName);
                  if (this.targetTrapdoor == null) {
                     this.info("!tp from " + senderName + " but no tracked pearl with trapdoor found.", new Object[0]);
                     this.sendReply(senderName, "Could not find a tracked pearl or trapdoor for you.", randomAntiSpam(8));
                     return;
                  }

                  if (mc.player.squaredDistanceTo(Vec3d.ofCenter(this.targetTrapdoor)) > 256.0) {
                     this.info("!tp from " + senderName + " but pearl is out of render distance.", new Object[0]);
                     this.sendReply(senderName, "Pearl is out of render distance.", randomAntiSpam(8));
                     return;
                  }

                  this.info("Pearl found at " + this.targetTrapdoor + " — pathfinding to load for " + senderName, new Object[0]);
                  this.sendReply(senderName, "Pearl found, loading...", randomAntiSpam(8));
                  this.startPos = BlockPos.ofFloored(VersionHelper.get().getPlayerPos());
               }

               this.lookAlignTicks = 0;
               this.pendingAction = true;
            }
         }
      }
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (mc.player != null && mc.world != null && this.pendingAction) {
         BlockPos resolvedPos = this.action2.get() == MessageInteract.Action.BOTH ? this.targetTrapdoor : (BlockPos)this.position2.get();
         if (resolvedPos == null) {
            this.pendingAction = false;
         } else if (this.interactDelayTicks > 0) {
            this.interactDelayTicks--;
            if (this.interactDelayTicks == 0) {
               InventoryManager.interactBlock(new BlockHitResult(Vec3d.ofCenter(resolvedPos), Direction.UP, resolvedPos, false));
               if (this.action2.get() == MessageInteract.Action.BOTH && (Boolean)this.dropPearl.get()) {
                  int pearlSlot = InventoryManager.findItemSlot(Items.ENDER_PEARL);
                  if (pearlSlot != -1) {
                     InventoryManager.throwSlot(pearlSlot, false);
                  }
               }

               this.pendingAction = false;
               this.targetTrapdoor = null;
               if ((Boolean)this.returnToStartPos.get() && this.startPos != null) {
                  musheor.utils.BaritoneHelper.setGoalAndPath(new GoalBlock(this.startPos));
               }
            }
         } else if (!HighwayUtils.isWithinRange(resolvedPos, 4.0)) {
            this.lookAlignTicks = 0;
            musheor.utils.BaritoneHelper.setGoalAndPath(new GoalNear(resolvedPos, 2));
         } else {
            musheor.utils.BaritoneHelper.cancel();
            double yaw = Rotations.getYaw(resolvedPos);
            double pitch = Rotations.getPitch(resolvedPos);
            Rotations.rotate(yaw, pitch);
            this.lookAlignTicks++;
            if (this.lookAlignTicks >= 3) {
               if (this.action2.get() == MessageInteract.Action.MESSAGE) {
                  mc.interactionManager.attackBlock(resolvedPos, Direction.UP);
                  this.lookAlignTicks = 0;
                  this.pendingAction = false;
               } else if (this.action2.get() == MessageInteract.Action.PEARL) {
                  InventoryManager.interactBlock(new BlockHitResult(Vec3d.ofCenter(resolvedPos), Direction.UP, resolvedPos, false));
                  this.lookAlignTicks = 0;
                  this.pendingAction = false;
               } else if (mc.world.getBlockState(resolvedPos).getBlock() instanceof TrapdoorBlock) {
                  InventoryManager.interactBlock(new BlockHitResult(Vec3d.ofCenter(resolvedPos), Direction.UP, resolvedPos, false));
                  this.lookAlignTicks = 0;
                  this.interactDelayTicks = 10;
               } else {
                  this.info("Trapdoor no longer present at " + resolvedPos + ", aborting.", new Object[0]);
                  this.lookAlignTicks = 0;
                  this.pendingAction = false;
                  this.targetTrapdoor = null;
               }
            }
         }
      }
   }

   private void sendReply(String playerName, String message, String antiSpam) {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatCommand("msg " + playerName + " " + message + " " + antiSpam);
      }
   }

   public static String randomAntiSpam(int length) {
      StringBuilder sb = new StringBuilder(length);

      for (int i = 0; i < length; i++) {
         sb.append(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
               .charAt(RANDOM.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length()))
         );
      }

      return "[" + sb + "]";
   }

   public static enum Action {
      PEARL,
      MESSAGE,
      BOTH;}
}

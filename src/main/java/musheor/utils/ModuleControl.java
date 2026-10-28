package musheor.utils;

import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.settings.BlockSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWalk;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook.Mode;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import musheor.compat.VersionHelper;
import musheor.modules.automation.GatherItem;
import musheor.modules.automation.HighwayBuilder;
import musheor.modules.tech.MessageInteract;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ModuleControl {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean isInBlockReach(double x, double y, double z) {
      return PlayerUtils.squaredDistance(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), x, y, z)
         <= mc.player.getBlockInteractionRange() * mc.player.getBlockInteractionRange();
   }

   public static boolean isInBlockReach(BlockPos blockPos) {
      return isInBlockReach(blockPos.toCenterPos().getX(), blockPos.toCenterPos().getY(), blockPos.toCenterPos().getZ());
   }

   public static void cancelBaritone() {
      musheor.utils.BaritoneHelper.cancel();
   }

   public static void disconnect(String reason) {
      assert mc.player != null;

      MutableText text = Text.literal("[Musheor]: " + reason);
      if (((AutoReconnect)Modules.get().get(AutoReconnect.class)).isActive()) {
         ((AutoReconnect)Modules.get().get(AutoReconnect.class)).toggle();
      }

      mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));
   }

   public static void setAutoWalk(boolean active) {
      if (HighwayBuilder.getMode() == HighwayBuilder.Mode.AUTO) {
         Module autoWalk = Modules.get().get(AutoWalk.class);
         if (active && !autoWalk.isActive()) {
            autoWalk.toggle();
         }

         if (!active && autoWalk.isActive()) {
            autoWalk.toggle();
         }
      }
   }

   public static void setKeyPressed(KeyBinding key, boolean pressed) {
      key.setPressed(pressed);
      Input.setKeyState(key, pressed);
   }

   public static void releaseMovementKeys() {
      setKeyPressed(mc.options.forwardKey, false);
      setKeyPressed(mc.options.backKey, false);
      setKeyPressed(mc.options.leftKey, false);
      setKeyPressed(mc.options.rightKey, false);
      setKeyPressed(mc.options.jumpKey, false);
   }

   public static void toggleHighwayBuilder() {
      HighwayBuilder.disableCompanionModules();
      Module BetterHighwayBuilder = Modules.get().get(HighwayBuilder.class);
      BetterHighwayBuilder.toggle();
   }

   public static void setupFreeLookCamera() {
      if (mc.player != null && mc.world != null) {
         setModuleSetting(FreeLook.class, "mode", Mode.Camera);
         setModuleSetting(FreeLook.class, "camera-sensitivity", 8.0);
         setModuleSetting(FreeLook.class, "arrows-control-opposite", false);
         mc.player.setPitch(15.0F);
         ((FreeLook)Modules.get().get(FreeLook.class)).toggle();
      }
   }

   public static <M extends Module, T> void setModuleSetting(Class<M> module, String setting, T value) {
      if (Modules.get().get(module) != null) {
         Setting<?> s = Modules.get().get(module).settings.get(setting);
         if (Modules.get().get(module).settings.get(setting) != null) {
            try {
               switch (value) {
                  case Integer integer:
                     ((IntSetting)s).set(integer);
                     break;
                  case Double d:
                     ((DoubleSetting)s).set(d);
                     break;
                  case String string:
                     ((StringSetting)s).set(string);
                     break;
                  case Boolean bool:
                     ((BoolSetting)s).set(bool);
                     break;
                  case Item item:
                     ((ItemSetting)s).set(item);
                     break;
                  case Block block:
                     ((BlockSetting)s).set(block);
                     break;
                  case Enum<?> es:
                     ((EnumSetting)s).set(es);
                     break;
                  case null:
                  default:
               }
            } catch (Exception var13) {
               throw new RuntimeException(var13);
            }
         } else {
            ChatUtils.warning("Setting %d not found", new Object[]{setting});
         }
      } else {
         ChatUtils.warning("Module %d not found", new Object[]{module});
      }
   }

   public static void faceHighwayDirectionWithPitch() {
      assert mc.player != null;

      mc.player.setPitch(30.0F);
      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      mc.player.setYaw(HighwayUtils.HighwayDirection.toYaw(direction));
      mc.player.setHeadYaw(HighwayUtils.HighwayDirection.toYaw(direction));
      mc.player.setBodyYaw(HighwayUtils.HighwayDirection.toYaw(direction));
   }

   public static void faceHighwayDirection() {
      assert mc.player != null;

      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      mc.player.setYaw(HighwayUtils.HighwayDirection.toYaw(direction));
      mc.player.setHeadYaw(HighwayUtils.HighwayDirection.toYaw(direction));
      mc.player.setBodyYaw(HighwayUtils.HighwayDirection.toYaw(direction));
   }

   public static void startGatherItem(Item item, boolean paveAfterwards) {
      setModuleSetting(GatherItem.class, "item", item);
      Module gatherItem = Modules.get().get(GatherItem.class);
      if (!isGatherItemActive()) {
         gatherItem.toggle();
      }
   }

   public static void paveAfterGather(boolean paveAfter) {
   }

   public static void setKeyPressedDirect(KeyBinding key, boolean pressed) {
      key.setPressed(pressed);
      Input.setKeyState(key, pressed);
   }

   public static boolean isGatherItemActive() {
      return ((GatherItem)Modules.get().get(GatherItem.class)).isActive();
   }

   public static void strafeAlignCardinal() {
      if (mc.player != null && mc.world != null) {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         boolean pressD = false;
         boolean pressA = false;
         if (state.getDirection() == HighwayUtils.HighwayDirection.NORTH || state.getDirection() == HighwayUtils.HighwayDirection.SOUTH) {
            double currentX = VersionHelper.get().getPlayerPos().getX();
            if (state.getDirection() == HighwayUtils.HighwayDirection.NORTH) {
               if (currentX > state.getAlignX() + 0.15) {
                  pressA = true;
               } else if (currentX < state.getAlignX() - 0.15) {
                  pressD = true;
               }
            } else if (currentX < state.getAlignX() - 0.15) {
               pressA = true;
            } else if (currentX > state.getAlignX() + 0.15) {
               pressD = true;
            }
         } else if (state.getDirection() == HighwayUtils.HighwayDirection.EAST || state.getDirection() == HighwayUtils.HighwayDirection.WEST) {
            double currentZ = VersionHelper.get().getPlayerPos().getZ();
            if (state.getDirection() == HighwayUtils.HighwayDirection.EAST) {
               if (currentZ > state.getAlignZ() + 0.15) {
                  pressA = true;
               } else if (currentZ < state.getAlignZ() - 0.15) {
                  pressD = true;
               }
            } else if (currentZ < state.getAlignZ() - 0.15) {
               pressA = true;
            } else if (currentZ > state.getAlignZ() + 0.15) {
               pressD = true;
            }
         }

         setKeyPressedDirect(mc.options.rightKey, pressD);
         setKeyPressedDirect(mc.options.leftKey, pressA);
      }
   }

   public static void alignDiagonalAxis() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      if (state.getLavaTarget() == null) {
         assert mc.player != null;

         if (state.getRailX() != null && state.getRailY() != null && state.getRailZ() != null) {
            if (state.getDirection() != null) {
               int ax = state.getRailX();
               int ay = state.getRailY();
               int az = state.getRailZ();
               boolean isNESW = state.getDirection() == HighwayUtils.HighwayDirection.NORTHEAST || state.getDirection() == HighwayUtils.HighwayDirection.SOUTHWEST;
               musheor.utils.HighwayDetector.DetectedHighway detected = state.getDetectedHighway();
               int K = detected != null ? (int)detected.axisValue : (isNESW ? ax + az : ax - az);
               int px = mc.player.getBlockX();
               int pz = mc.player.getBlockZ();
               int diagVal = isNESW ? px + pz : px - pz;
               if (diagVal != K) {
                  int tx;
                  int tz;
                  if (isNESW) {
                     tx = (int)Math.round((px - pz + K) / 2.0);
                     tz = K - tx;
                  } else {
                     tx = (int)Math.round((px + pz + K) / 2.0);
                     tz = tx - K;
                  }

                  musheor.utils.BaritoneHelper.setGoalAndPath(new GoalBlock(tx, ay, tz));
                  MusheorSystem.debug("Aligning player to the diagonal axis at %s %s", tx, tz);
               }
            }
         }
      }
   }

   public static void alignCardinalLane() {
      if (mc.player != null && mc.world != null) {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         if (state.getLavaTarget() == null) {
            HighwayUtils.HighwayDirection dir = state.getDirection();
            if (dir != null) {
               boolean travelX = dir == HighwayUtils.HighwayDirection.EAST || dir == HighwayUtils.HighwayDirection.WEST;
               boolean travelZ = dir == HighwayUtils.HighwayDirection.NORTH || dir == HighwayUtils.HighwayDirection.SOUTH;
               if (travelX || travelZ) {
                  BlockPos lane = HighwayUtils.getLanePos();
                  if (lane != null) {
                     int currentPerp = travelX ? mc.player.getBlockZ() : mc.player.getBlockX();
                     int lanePerp = travelX ? lane.getZ() : lane.getX();
                     if (Math.abs(currentPerp - lanePerp) >= 2) {
                        musheor.utils.BaritoneHelper.setGoalAndPath(new GoalBlock(lane));
                        MusheorSystem.debug("Aligning player to the cardinal highway lane at %s %s", lane.getX(), lane.getZ());
                     }
                  }
               }
            }
         }
      }
   }

   public static void sendTpCommand(String playerIGN, int length) {
      String command = "!tp " + MessageInteract.randomAntiSpam(length);
      if (!VersionHelper.get().onSameServer(playerIGN)) {
         ChatUtils.error("Cannot find %s, not online?!", new Object[]{playerIGN});
      } else {
         try {
            Class.forName("musheor.plus.PlusPlayerUtils").getMethod("trySendIrcPm", String.class, String.class).invoke(null, playerIGN, command);
         } catch (Exception var4) {
            mc.player.networkHandler.sendPacket(new CommandExecutionC2SPacket("msg " + playerIGN + " " + command));
         }
      }
   }
}

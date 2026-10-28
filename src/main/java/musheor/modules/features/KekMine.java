package musheor.modules.features;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.modules.automation.HighwayBuilder;
import musheor.modules.automation.KekNuker;
import musheor.utils.BlockRenderUtils;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class KekMine extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgGrim = this.settings.createGroup("Grim");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   public final Setting<Boolean> preventToolBreaking = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("prevent-tool-breaking")).description("Prevents you from breaking your tool items"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Integer> minToolDurability = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("min-tool-durability"))
                     .description("The minimum durability threshold of a usable tool"))
                  .defaultValue(25))
               .sliderRange(0, 500)
               .visible(this.preventToolBreaking::get))
            .build()
      );
   private final Setting<Boolean> silentSwap = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("silent-swap")).description("Breaks the block without holding the pickaxe")).defaultValue(false))
            .build()
      );
   public final Setting<Integer> holdTicks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("hold-ticks"))
                     .description(
                        "Ticks to keep the tool selected after a silent break before restoring. Prevents wrong-tool drops when sequenced block packets and slot packets race (0 = restore immediately)"
                     ))
                  .defaultValue(3))
               .min(0)
               .max(10)
               .visible(this.silentSwap::get))
            .build()
      );
   private final Setting<Boolean> debug = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("debug"))
                     .description("Logs the break packet sequence (start / burst / park / stop / confirm) to chat"))
                  .defaultValue(false))
               .visible(() -> false))
            .build()
      );
   public final Setting<Boolean> grimBypass = this.sgGrim
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("grim-bypass")).description("Old Grim V3 bypass, no longer works on 2b2t or other Grim V2 servers."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Double> breakThreshold = this.sgGrim
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("break-threshold"))
            .defaultValue(0.7)
            .min(0.1)
            .max(1.0)
            .decimalPlaces(2)
            .build()
      );
   public final Setting<Integer> breakTimeout = this.sgGrim
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("break-timeout"))
                  .description("Time in ticks to wait before attempting to break the same block again (higher = safer for high ping)"))
               .defaultValue(5))
            .sliderRange(0, 20)
            .build()
      );
   public final Setting<Boolean> doubleBreak = this.sgGrim.add(((Builder)((Builder)new Builder().name("double-break")).defaultValue(true)).build());
   public final Setting<Boolean> alternate2 = this.sgGrim
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("alternate"))
                     .description("Alternate between a single and a double break to trick grim's break timeout (prevents you from mine-failing)."))
                  .defaultValue(true))
               .visible(this.doubleBreak::get))
            .build()
      );
   public final Setting<Boolean> validateBreak = this.sgGrim
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("validate-break"))
                  .description("Waits for the server to validate the break. Disable when you have high ping (may cause rubberbanding)"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> autoRebreak = this.sgGrim
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("auto-rebreak")).description("Automatically rebreak the last block in case it gets replaced"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> globalRendering = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("global-rendering")).defaultValue(true)).description("Synchronize rendering with Musheor-Tab"))
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
   public static KekMine INSTANCE;
   private KekMine.MineContext primaryContext;
   private KekMine.MineContext parkedContext;
   public BlockPos lastBrokenPos;
   public final Deque<BlockPos> breakQueue = new ArrayDeque<>();
   private int savedHotbarSlot = -1;
   private int restoreSlotAtTick = -1;
   private int ghostSelectedSlot = -1;
   private int tickCounter = 0;
   private long lastBreakTimeMs = 0L;
   private boolean burstWasUsed = false;
   private long doubleBreakReadyAtMs = 0L;
   private boolean skipNextAlternate = false;
   private boolean alternateAllowsDouble = true;

   public KekMine() {
      super(Musheor.MAIN, "KekMine", "Grim-safe packet miner with START-offset fast break.");
      INSTANCE = this;
   }

   static float calcBreakDelta(BlockState state, BlockPos pos, ItemStack tool) {
      if (mc.player != null && mc.world != null) {
         float hardness = state.getHardness(mc.world, pos);
         int h = state.isToolRequired() && (tool == null || !tool.isSuitableFor(state)) ? 100 : 30;
         float speed = tool != null && !tool.isEmpty() ? tool.getMiningSpeedMultiplier(state) : 1.0F;
         if (speed > 1.0F) {
            int efficiency = Utils.getEnchantmentLevel(tool, Enchantments.EFFICIENCY);
            if (efficiency > 0) {
               speed += efficiency * efficiency + 1;
            }
         }

         if (StatusEffectUtil.hasHaste(mc.player)) {
            speed *= 1.0F + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
         }

         if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float i = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
               case 0 -> 0.3F;
               case 1 -> 0.09F;
               case 2 -> 0.0027F;
               default -> 8.1E-4F;
            };
            speed *= i;
         }

         speed *= (float)mc.player.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
         if (mc.player.isSubmergedIn(FluidTags.WATER)) {
            speed *= (float)mc.player.getAttributeValue(EntityAttributes.SUBMERGED_MINING_SPEED);
         }

         if (!mc.player.isOnGround()) {
            speed /= 5.0F;
         }

         return speed / hardness / h;
      } else {
         return 0.0F;
      }
   }

   public static boolean canInstaBreak(BlockPos pos) {
      if (mc.player != null && mc.world != null) {
         BlockState state = mc.world.getBlockState(pos);
         return state.isAir() ? false : calcBreakDelta(state, pos, findBestTool(state)) >= 1.0F;
      } else {
         return false;
      }
   }

   private static boolean isSuitableTool(ItemStack stack, BlockState state) {
      return !state.isToolRequired() || stack.isSuitableFor(state);
   }

   private static ItemStack findBestTool(BlockState state) {
      if (mc.player == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack hand = mc.player.getMainHandStack();
         ItemStack best = hand;
         float bestSpeed = isSuitableTool(hand, state) ? hand.getMiningSpeedMultiplier(state) : 0.0F;

         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && (!(Boolean)INSTANCE.preventToolBreaking.get() || InventoryManager.isToolDurableEnough(stack)) && isSuitableTool(stack, state)) {
               float speed = stack.getMiningSpeedMultiplier(state);
               if (speed > bestSpeed) {
                  bestSpeed = speed;
                  best = stack;
               }
            }
         }

         return best;
      }
   }

   public static boolean isHoldingMatchingTool(BlockState state) {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack hand = mc.player.getMainHandStack();
         return isSuitableTool(hand, state) && hand.getMiningSpeedMultiplier(state) > 1.0F;
      }
   }

   private boolean isHoldingBestTool(BlockState state) {
      if (mc.player == null) {
         return true;
      } else {
         ItemStack hand = mc.player.getMainHandStack();
         return hand.getMiningSpeedMultiplier(state) >= findBestTool(state).getMiningSpeedMultiplier(state);
      }
   }

   public void swapToBestTool(BlockPos pos) {
      if (mc.player != null && mc.world != null) {
         int slot = this.findBestHotbarSlot(mc.world.getBlockState(pos));
         InventoryManager.selectHotbarSlot(slot);
      }
   }

   private int getEffectiveSelectedSlot() {
      if (mc.player == null) {
         return 0;
      } else {
         return this.savedHotbarSlot >= 0 && this.ghostSelectedSlot >= 0 ? this.ghostSelectedSlot : mc.player.getInventory().selectedSlot;
      }
   }

   private boolean slotHasBestTool(int slot, BlockState state) {
      if (mc.player != null && slot >= 0 && slot <= 8) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         return stack.getMiningSpeedMultiplier(state) >= findBestTool(state).getMiningSpeedMultiplier(state);
      } else {
         return false;
      }
   }

   private void ensureBestToolSelected(KekMine.MineContext ctx) {
      if (mc.player != null && ctx != null) {
         int selected = this.getEffectiveSelectedSlot();
         if (this.slotHasBestTool(selected, ctx.state)) {
            if (this.savedHotbarSlot >= 0) {
               this.restoreSlotAtTick = Math.max(this.restoreSlotAtTick, this.tickCounter + 3);
            }
         } else {
            int realSlot = this.savedHotbarSlot >= 0 ? this.savedHotbarSlot : mc.player.getInventory().selectedSlot;
            int bestSlot = this.findBestHotbarSlot(ctx.state);
            if (bestSlot != selected) {
               this.selectHotbarSlot(bestSlot);
               this.savedHotbarSlot = realSlot;
               this.restoreSlotAtTick = this.tickCounter + 3;
            }
         }
      }
   }

   public void queueOrStartBreak(BlockPos pos) {
      if (mc.world != null) {
         if (BlockUtils.canBreak(pos, mc.world.getBlockState(pos))) {
            if (!this.isOutOfNukerRange(pos)) {
               if (!this.isTargeting(pos)) {
                  this.startBreak(pos, mc.world.getBlockState(pos), this.shouldSilentSwap());
               }
            }
         }
      }
   }

   public boolean isTargeting(BlockPos pos) {
      if (this.primaryContext != null && this.primaryContext.pos.equals(pos)) {
         return true;
      } else {
         return this.parkedContext != null && this.parkedContext.pos.equals(pos) ? true : this.breakQueue.contains(pos);
      }
   }

   public boolean hasQueuedBreaks() {
      return this.primaryContext == null && this.parkedContext == null && !this.breakQueue.isEmpty();
   }

   public boolean isIdle() {
      return this.primaryContext == null && this.parkedContext == null && this.breakQueue.isEmpty();
   }

   private boolean isExternalControlActive() {
      return ((KekNuker)Modules.get().get(KekNuker.class)).isActive() || HighwayBuilder.isEchestFarmerActive() || InventoryManager.isBusy;
   }

   public boolean shouldSilentSwap() {
      return (Boolean)this.silentSwap.get() && !this.isExternalControlActive();
   }

   private boolean canStartDoubleBreak() {
      return this.alternate2.get() && !this.alternateAllowsDouble
         ? false
         : this.parkedContext == null
            && System.currentTimeMillis() >= this.doubleBreakReadyAtMs
            && this.primaryContext != null
            && this.primaryContext.awaitingFinish
            && this.primaryContext.getProgress() < 1.0;
   }

   private void updateAlternateFlag() {
      this.alternateAllowsDouble = !this.skipNextAlternate;
      this.skipNextAlternate = false;
   }

   private void clearParkedContext(boolean confirmedBroken) {
      this.parkedContext = null;
      this.doubleBreakReadyAtMs = System.currentTimeMillis() + (confirmedBroken ? 50L : 290L);
   }

   private boolean toolsMatchForStates(BlockState a, BlockState b) {
      ItemStack ta = findBestTool(a);
      ItemStack tb = findBestTool(b);
      return ta.isEmpty() && tb.isEmpty() ? true : ta.getItem() == tb.getItem();
   }

   public static void instantBreakAt(BlockPos pos) {
      if (pos != null && mc.player != null) {
         instantBreak(pos, INSTANCE.getBestFace(pos), INSTANCE.shouldSilentSwap());
      }
   }

   public static void instantBreak(BlockPos pos, Direction dir, boolean silent) {
      if (pos != null) {
         INSTANCE.doInstantBreak(pos, dir, silent);
      }
   }

   private void doInstantBreak(BlockPos pos, Direction dir, boolean silent) {
      if (mc.world != null && mc.player != null && mc.interactionManager != null) {
         if (!this.isTargeting(pos)) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir()) {
               if (!silent && !this.isHoldingBestTool(state)) {
                  this.swapToBestTool(pos);
                  if (!this.breakQueue.contains(pos)) {
                     this.breakQueue.addFirst(pos);
                  }
               } else {
                  int prevSlot = this.savedHotbarSlot >= 0 ? this.savedHotbarSlot : mc.player.getInventory().selectedSlot;
                  int bestSlot = silent ? this.findBestHotbarSlot(state) : prevSlot;
                  boolean needSwap = silent && bestSlot != prevSlot;
                  if (needSwap) {
                     this.selectHotbarSlot(bestSlot);
                  }

                  mc.interactionManager.sendSequencedPacket(mc.world, sequence -> {
                     this.clientPredictBreak(pos, state);
                     return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, dir, sequence);
                  });
                  if (needSwap) {
                     int hold = (Integer)this.holdTicks.get();
                     if (hold > 0) {
                        this.savedHotbarSlot = prevSlot;
                        this.restoreSlotAtTick = this.tickCounter + hold;
                     } else {
                        this.selectHotbarSlot(prevSlot);
                        this.savedHotbarSlot = -1;
                     }
                  }

                  HighwayBuilder.onBlockBroken(state);
                  this.lastBrokenPos = pos;
                  this.lastBreakTimeMs = System.currentTimeMillis();
                  this.burstWasUsed = false;
               }
            }
         }
      }
   }

   private void clientPredictBreak(BlockPos pos, BlockState state) {
      Block block = state.getBlock();
      state.onBlockBreakStart(mc.world, pos, mc.player);
      block.onBreak(mc.world, pos, state, mc.player);
      FluidState fluid = mc.world.getFluidState(pos);
      boolean removed = mc.world.setBlockState(pos, fluid.getBlockState(), 3);
      if (removed) {
         block.onBroken(mc.world, pos, state);
      }

      mc.world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
   }

   public void onDeactivate() {
      if (this.savedHotbarSlot >= 0) {
         this.selectHotbarSlot(this.savedHotbarSlot);
      }

      this.primaryContext = null;
      this.parkedContext = null;
      this.breakQueue.clear();
      this.lastBrokenPos = null;
      this.savedHotbarSlot = -1;
      this.restoreSlotAtTick = -1;
      this.ghostSelectedSlot = -1;
      this.tickCounter = 0;
      this.lastBreakTimeMs = 0L;
      this.burstWasUsed = false;
      this.doubleBreakReadyAtMs = 0L;
      this.skipNextAlternate = false;
      this.alternateAllowsDouble = true;
   }

   private int findBestHotbarSlot(BlockState state) {
      if (mc.player == null) {
         return 0;
      } else {
         float bestSpeed = 1.0F;
         int bestSlot = mc.player.getInventory().selectedSlot;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if ((!(Boolean)this.preventToolBreaking.get() || InventoryManager.isToolDurableEnough(stack)) && isSuitableTool(stack, state)) {
               float speed = stack.getMiningSpeedMultiplier(state);
               if (speed > bestSpeed) {
                  bestSpeed = speed;
                  bestSlot = i;
               }
            }
         }

         int bestInvSlot = -1;

         for (int ix = 9; ix < mc.player.getInventory().main.size(); ix++) {
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (!stack.isEmpty() && (!(Boolean)this.preventToolBreaking.get() || InventoryManager.isToolDurableEnough(stack)) && isSuitableTool(stack, state)) {
               float speed = stack.getMiningSpeedMultiplier(state);
               if (speed > bestSpeed) {
                  bestSpeed = speed;
                  bestInvSlot = ix;
               }
            }
         }

         if (bestInvSlot != -1) {
            int hotbarSlot = InventoryManager.moveToHotbar(mc.player.getInventory().getStack(bestInvSlot));
            bestSlot = Math.max(0, Math.min(8, hotbarSlot));
         }

         return bestSlot;
      }
   }

   public void startBreak(BlockPos pos, BlockState state, boolean silent) {
      this.startBreak(pos, state, this.getBestFace(pos), silent);
   }

   public void startBreak(BlockPos pos, BlockState state, Direction dir, boolean silent) {
      if (!this.isTargeting(pos)) {
         if (!HighwayBuilder.isEating()) {
            if (this.primaryContext == null || (Boolean)this.doubleBreak.get() && this.canStartDoubleBreak()) {
               if (this.primaryContext == null) {
                  if (!this.isBreakCooldownReady()) {
                     if (!this.breakQueue.contains(pos)) {
                        this.breakQueue.addLast(pos);
                     }

                     return;
                  }

                  if (!silent && !this.isHoldingBestTool(state)) {
                     this.swapToBestTool(pos);
                     if (!this.breakQueue.contains(pos)) {
                        this.breakQueue.addFirst(pos);
                     }

                     return;
                  }

                  this.primaryContext = new KekMine.MineContext(pos, state, true, silent);
                  this.updateAlternateFlag();
                  this.beginContextMining(pos, dir, this.primaryContext);
               } else {
                  this.debugLog("PARK", this.primaryContext.pos);
                  this.sendAction(Action.STOP_DESTROY_BLOCK, this.primaryContext.pos, this.getBestFace(this.primaryContext.pos));
                  this.parkedContext = this.primaryContext.toParkedCopy();
                  this.primaryContext = new KekMine.MineContext(pos, state, true, silent);
                  if (!this.toolsMatchForStates(this.primaryContext.state, this.parkedContext.state)) {
                     this.primaryContext.resetWithTool(this.parkedContext.tool);
                  }

                  this.skipNextAlternate = !this.isSlowBreakTimedOut(this.primaryContext);
                  this.beginContextMining(this.primaryContext.pos, dir, this.primaryContext);
               }
            } else {
               if (!this.breakQueue.contains(pos)) {
                  this.breakQueue.addLast(pos);
               }
            }
         }
      }
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (mc.player != null && mc.world != null) {
         this.tickCounter++;
         if (this.savedHotbarSlot >= 0 && this.tickCounter >= this.restoreSlotAtTick) {
            this.selectHotbarSlot(this.savedHotbarSlot);
            this.savedHotbarSlot = -1;
         }

         if (!HighwayBuilder.isEating() && !mc.player.isUsingItem()) {
            this.processBreakQueue();
            this.tickBurstIfNeeded(this.primaryContext);
            boolean toolConflict = this.primaryContext != null
               && this.parkedContext != null
               && !this.toolsMatchForStates(this.primaryContext.state, this.parkedContext.state);
            if (this.parkedContext != null && this.parkedContext.getProgress() >= 1.0) {
               this.finishContext(this.parkedContext);
            }

            if (this.primaryContext != null) {
               if (toolConflict && this.parkedContext != null) {
                  this.primaryContext.retargetProgress(this.parkedContext.tool);
                  if (this.primaryContext.getProgress() >= 1.0) {
                     this.finishContext(this.primaryContext);
                  }
               } else if (!this.primaryContext.silent && !this.isHoldingBestTool(this.primaryContext.state)) {
                  if (this.primaryContext.getProgress() >= 1.0) {
                     this.finishContext(this.primaryContext);
                  } else {
                     this.swapToBestTool(this.primaryContext.pos);
                  }
               } else {
                  this.primaryContext.retargetProgress(findBestTool(this.primaryContext.state));
                  if (this.primaryContext.getProgress() >= 1.0) {
                     this.finishContext(this.primaryContext);
                  }
               }
            }

            this.cleanupGoneTargets();
            if ((Boolean)this.autoRebreak.get()
               && this.lastBrokenPos != null
               && this.primaryContext == null
               && this.parkedContext == null
               && this.breakQueue.isEmpty()
               && !this.isOutOfNukerRange(this.lastBrokenPos)) {
               this.finishSilentBreak(new KekMine.MineContext(this.lastBrokenPos, mc.world.getBlockState(this.lastBrokenPos), true), this.shouldSilentSwap());
            }
         }
      }
   }

   private void cleanupGoneTargets() {
      if (this.primaryContext != null && this.isBlockGone(this.primaryContext.pos)) {
         this.primaryContext = null;
      }

      if (this.parkedContext != null && this.isBlockGone(this.parkedContext.pos)) {
         this.clearParkedContext(true);
      }

      this.breakQueue.removeIf(this::isBlockGone);
   }

   private boolean isBlockGone(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.isAir() || this.isOutOfNukerRange(pos);
   }

   private void processBreakQueue() {
      if (!this.breakQueue.isEmpty()) {
         if (this.primaryContext == null) {
            if (!this.isBreakCooldownReady()) {
               return;
            }

            BlockPos pos = this.breakQueue.pollFirst();
            BlockState state = mc.world.getBlockState(pos);
            boolean silent = this.shouldSilentSwap();
            if (!silent && !this.isHoldingBestTool(state)) {
               this.swapToBestTool(pos);
               this.breakQueue.addFirst(pos);
               return;
            }

            this.primaryContext = new KekMine.MineContext(pos, state, true, silent);
            this.updateAlternateFlag();
            this.beginContextMining(this.primaryContext.pos, this.getBestFace(this.primaryContext.pos), this.primaryContext);
         }

         if (!this.breakQueue.isEmpty() && (Boolean)this.doubleBreak.get() && this.canStartDoubleBreak()) {
            this.debugLog("PARK", this.primaryContext.pos);
            this.sendAction(Action.STOP_DESTROY_BLOCK, this.primaryContext.pos, this.getBestFace(this.primaryContext.pos));
            BlockPos nextPos = this.breakQueue.pollFirst();
            BlockState nextState = mc.world.getBlockState(nextPos);
            this.parkedContext = this.primaryContext.toParkedCopy();
            this.primaryContext = new KekMine.MineContext(nextPos, nextState, true, this.shouldSilentSwap());
            if (!this.toolsMatchForStates(this.primaryContext.state, this.parkedContext.state)) {
               this.primaryContext.resetWithTool(this.parkedContext.tool);
            }

            this.skipNextAlternate = !this.isSlowBreakTimedOut(this.primaryContext);
            this.beginContextMining(this.primaryContext.pos, this.getBestFace(this.primaryContext.pos), this.primaryContext);
         }
      }
   }

   private void sendStartDestroy(BlockPos pos, Direction dir) {
      if ((Boolean)this.grimBypass.get()) {
         this.sendAction(Action.STOP_DESTROY_BLOCK, pos, dir);
      }

      this.sendAction(Action.START_DESTROY_BLOCK, pos, dir);
   }

   private void sendGrimOffsetStart(BlockPos pos, Direction dir) {
      this.sendAction(Action.START_DESTROY_BLOCK, new BlockPos(pos.getX(), 1337, pos.getZ()), dir);
   }

   private void sendStopDestroy(BlockPos pos, Direction dir) {
      this.sendAction(Action.STOP_DESTROY_BLOCK, pos, dir);
      this.lastBreakTimeMs = System.currentTimeMillis();
   }

   private void tickBurstIfNeeded(KekMine.MineContext ctx) {
      if (this.needsBurst(ctx) && System.currentTimeMillis() - ctx.startTimeMs > 290L) {
         this.sendBurstPackets(ctx);
      }
   }

   private void debugLog(String what, BlockPos pos) {
      if ((Boolean)this.debug.get()) {
         this.info(
            "Tick: %d %s %s [PRIM=%s SEC=%s Q=%d]"
               .formatted(
                  this.tickCounter,
                  what,
                  pos == null ? "-" : pos.toShortString(),
                  this.primaryContext == null ? "-" : this.primaryContext.pos.toShortString() + (this.primaryContext.burstSent ? "*" : ""),
                  this.parkedContext == null ? "-" : this.parkedContext.pos.toShortString() + (this.parkedContext.burstSent ? "*" : ""),
                  this.breakQueue.size()
               ),
            new Object[0]
         );
      }
   }

   private boolean needsBurst(KekMine.MineContext ctx) {
      return ctx != null && !ctx.burstSent && this.isSlowBreakTimedOut(ctx);
   }

   private boolean isSlowBreakTimedOut(KekMine.MineContext ctx) {
      return ctx != null && !ctx.isInstaBreak ? ctx.estimatedMsRemaining() >= 290L : false;
   }

   private void sendBurstPackets(KekMine.MineContext ctx) {
      if (ctx != null && mc.player != null) {
         Direction dir = KekNuker.faceToward(mc.player.getEyePos(), ctx.pos);

         for (int i = 0; i < 22; i++) {
            this.sendGrimOffsetStart(ctx.pos, dir);
         }

         ctx.burstSent = true;
         this.debugLog("BURST", ctx.pos);
      }
   }

   private boolean isBreakCooldownReady() {
      return System.currentTimeMillis() - this.lastBreakTimeMs > 275L;
   }

   private void beginContextMining(BlockPos pos, Direction dir, KekMine.MineContext ctx) {
      this.debugLog("START", pos);
      if (ctx.silent && ctx.isInstaBreak && !ctx.handCanInstaBreak) {
         int prevSlot = this.savedHotbarSlot >= 0 ? this.savedHotbarSlot : mc.player.getInventory().selectedSlot;
         int bestSlot = this.findBestHotbarSlot(ctx.state);
         boolean needSwap = bestSlot != prevSlot;
         if (needSwap) {
            this.selectHotbarSlot(bestSlot);
         }

         this.sendStartDestroy(pos, dir);
         if (needSwap) {
            int hold = (Integer)this.holdTicks.get();
            if (hold > 0) {
               this.savedHotbarSlot = prevSlot;
               this.restoreSlotAtTick = this.tickCounter + hold;
            } else {
               this.selectHotbarSlot(prevSlot);
               this.savedHotbarSlot = -1;
            }
         }
      } else {
         this.sendStartDestroy(pos, dir);
         if (!ctx.isInstaBreak && !ctx.grimOffsetSent) {
            this.sendGrimOffsetStart(pos, dir);
            ctx.grimOffsetSent = true;
         }
      }
   }

   private void finishSilentBreak(KekMine.MineContext ctx, boolean silent) {
      if (mc.world != null && mc.player != null) {
         Direction dir = this.getBestFace(ctx.pos);
         int prevSlot = this.savedHotbarSlot >= 0 ? this.savedHotbarSlot : mc.player.getInventory().selectedSlot;
         int bestSlot = silent ? this.findBestHotbarSlot(ctx.state) : prevSlot;
         boolean needSwap = silent && bestSlot != prevSlot;
         if (needSwap) {
            this.selectHotbarSlot(bestSlot);
         }

         this.sendAction(Action.STOP_DESTROY_BLOCK, ctx.pos, dir);
         if (!ctx.state.isAir()) {
            this.lastBreakTimeMs = System.currentTimeMillis();
         }

         if (needSwap) {
            int hold = (Integer)this.holdTicks.get();
            if (hold > 0) {
               this.savedHotbarSlot = prevSlot;
               this.restoreSlotAtTick = this.tickCounter + hold;
            } else {
               this.selectHotbarSlot(prevSlot);
               this.savedHotbarSlot = -1;
            }
         }

         if (!(Boolean)this.validateBreak.get() && !ctx.state.isAir()) {
            mc.world.syncWorldEvent(2001, ctx.pos, Block.getRawIdFromState(ctx.state));
            mc.world.setBlockState(ctx.pos, Blocks.AIR.getDefaultState(), 3);
         }
      }
   }

   private void finishContext(KekMine.MineContext ctx) {
      if (mc.world != null && mc.player != null) {
         if (ctx.awaitingFinish) {
            ctx.awaitingFinish = false;
            ctx.finishTick = this.tickCounter;
            this.burstWasUsed = ctx.burstSent;
            this.debugLog(ctx.isPrimary ? "FINISH-P" : "FINISH-S", ctx.pos);
            Direction face = this.getBestFace(ctx.pos);
            if (!ctx.isPrimary) {
               if (ctx.silent) {
                  this.ensureBestToolSelected(ctx);
               }

               this.lastBreakTimeMs = System.currentTimeMillis();
            } else if (ctx.silent && ctx.isInstaBreak) {
               this.lastBreakTimeMs = System.currentTimeMillis();
            } else if (ctx.silent) {
               int prevSlot = this.savedHotbarSlot >= 0 ? this.savedHotbarSlot : mc.player.getInventory().selectedSlot;
               int bestSlot = this.findBestHotbarSlot(ctx.state);
               boolean needSwap = bestSlot != prevSlot;
               if (needSwap) {
                  this.selectHotbarSlot(bestSlot);
               }

               this.sendStopDestroy(ctx.pos, face);
               if (needSwap) {
                  int hold = (Integer)this.holdTicks.get();
                  if (hold > 0) {
                     this.savedHotbarSlot = prevSlot;
                     this.restoreSlotAtTick = this.tickCounter + hold;
                  } else {
                     this.selectHotbarSlot(prevSlot);
                     this.savedHotbarSlot = -1;
                  }
               }
            } else {
               if (mc.player.getMainHandStack().getItem() != ctx.tool.getItem()) {
                  this.swapToBestTool(ctx.pos);
               }

               if (!ctx.isInstaBreak) {
                  this.sendStopDestroy(ctx.pos, face);
               } else {
                  this.lastBreakTimeMs = System.currentTimeMillis();
               }
            }

            if (ctx.isInstaBreak && !(Boolean)this.validateBreak.get()) {
               mc.world.syncWorldEvent(2001, ctx.pos, Block.getRawIdFromState(ctx.state));
               mc.world.setBlockState(ctx.pos, Blocks.AIR.getDefaultState(), 3);
            }

            this.lastBrokenPos = ctx.pos;
         }

         boolean broke = this.isBlockGone(ctx.pos);
         if (!broke && !ctx.isPrimary && ctx.silent) {
            this.ensureBestToolSelected(ctx);
         }

         int timeout = ctx.isPrimary ? 5 : 10;
         if (broke || this.tickCounter - ctx.finishTick >= timeout) {
            this.debugLog(broke ? "CONFIRMED" : "TIMEOUT", ctx.pos);
            if (ctx == this.primaryContext) {
               this.primaryContext = null;
            } else if (ctx == this.parkedContext) {
               this.clearParkedContext(broke);
            }

            if (broke) {
               BlockSoundGroup sg = ctx.state.getSoundGroup();
               VersionHelper.get()
                  .playSoundAtBlockCenter(ctx.pos, sg.getBreakSound(), SoundCategory.BLOCKS, (sg.getVolume() + 1.0F) / 2.0F, sg.getPitch() * 0.8F);
               HighwayBuilder.onBlockBroken(ctx.state);
            }
         }
      }
   }

   private void selectHotbarSlot(int slot) {
      if (mc.getNetworkHandler() != null) {
         this.ghostSelectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   public void sendAction(Action action, BlockPos pos) {
      this.sendAction(action, pos, this.getBestFace(pos));
   }

   public Direction getBestFace(BlockPos pos) {
      if (mc.player != null && mc.world != null) {
         Vec3d eye = mc.player.getEyePos();
         Direction best = null;
         double bestDist = Double.MAX_VALUE;

         for (Direction d : Direction.values()) {
            Vec3d faceCenter = Vec3d.ofCenter(pos).add(d.getOffsetX() * 0.5, d.getOffsetY() * 0.5, d.getOffsetZ() * 0.5);
            double dist = eye.squaredDistanceTo(faceCenter);
            if (!(dist >= bestDist) && this.hasLineOfSightToFace(eye, pos, d, faceCenter)) {
               bestDist = dist;
               best = d;
            }
         }

         return best != null ? best : KekNuker.faceToward(eye, pos);
      } else {
         return Direction.UP;
      }
   }

   private boolean hasLineOfSightToFace(Vec3d eye, BlockPos pos, Direction d, Vec3d faceCenter) {
      BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, faceCenter, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
      return hit != null && hit.getType() == Type.BLOCK && hit.getBlockPos().equals(pos) && hit.getSide() == d;
   }

   public void sendAction(Action action, BlockPos pos, Direction dir) {
      if (mc.interactionManager != null && mc.world != null) {
         mc.interactionManager.sendSequencedPacket(mc.world, sequence -> new PlayerActionC2SPacket(action, pos, dir, sequence));
      }
   }

   public boolean isOutOfNukerRange(BlockPos pos) {
      return pos == null
         ? false
         : mc.player.getEyePos().distanceTo(pos.toCenterPos()) > (Double)((KekNuker)Modules.get().get(KekNuker.class)).range2.get() + 1.0;
   }

   public BlockPos getPrimaryPos() {
      return this.primaryContext != null ? this.primaryContext.pos : null;
   }

   public BlockPos getParkedPos() {
      return this.parkedContext != null ? this.parkedContext.pos : null;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!((KekNuker)Modules.get().get(KekNuker.class)).isActive()) {
            if ((Boolean)this.globalRendering.get()) {
               BlockRenderUtils.renderPositions(event, this.breakQueue.stream().toList());
            } else {
               BlockRenderUtils.renderPositions(
                  event,
                  this.breakQueue.stream().toList(),
                  meteordevelopment.meteorclient.utils.render.color.Color.WHITE,
                  meteordevelopment.meteorclient.utils.render.color.Color.WHITE,
                  ShapeMode.Lines
               );
            }
         }

         if (this.parkedContext != null) {
            if ((Boolean)this.globalRendering.get()) {
               this.renderMineProgress(
                  event,
                  this.parkedContext,
                  (meteordevelopment.meteorclient.utils.render.color.Color)MusheorSystem.Manager.renderSideColor.get(),
                  (meteordevelopment.meteorclient.utils.render.color.Color)MusheorSystem.Manager.renderLineColor.get(),
                  (ShapeMode)MusheorSystem.Manager.renderShape.get()
               );
            } else {
               this.renderMineProgress(
                  event,
                  this.parkedContext,
                  (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
                  (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
                  ShapeMode.Lines
               );
            }
         }

         if (this.primaryContext != null) {
            if ((Boolean)this.globalRendering.get()) {
               this.renderMineProgress(
                  event,
                  this.primaryContext,
                  (meteordevelopment.meteorclient.utils.render.color.Color)MusheorSystem.Manager.renderSideColor.get(),
                  (meteordevelopment.meteorclient.utils.render.color.Color)MusheorSystem.Manager.renderLineColor.get(),
                  (ShapeMode)MusheorSystem.Manager.renderShape.get()
               );
            } else {
               this.renderMineProgress(
                  event,
                  this.primaryContext,
                  (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
                  (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
                  ShapeMode.Lines
               );
            }
         }

         if ((Boolean)this.autoRebreak.get() && this.lastBrokenPos != null) {
            if ((Boolean)this.globalRendering.get()) {
               BlockRenderUtils.renderPosition(event, this.lastBrokenPos, mc.world.getBlockState(this.lastBrokenPos).getBlock());
            } else {
               BlockRenderUtils.renderPosition(
                  event,
                  this.lastBrokenPos,
                  (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
                  (meteordevelopment.meteorclient.utils.render.color.Color)this.color2.get(),
                  ShapeMode.Lines
               );
            }
         }
      }
   }

   private void renderMineProgress(
      Render3DEvent event,
      KekMine.MineContext ctx,
      meteordevelopment.meteorclient.utils.render.color.Color side,
      meteordevelopment.meteorclient.utils.render.color.Color line,
      ShapeMode shape
   ) {
      double offset = (1.0 - ctx.getProgress()) / 2.0;
      Box box = new Box(
         ctx.pos.getX() + offset,
         ctx.pos.getY() + offset,
         ctx.pos.getZ() + offset,
         ctx.pos.getX() + 1.0 - offset,
         ctx.pos.getY() + 1.0 - offset,
         ctx.pos.getZ() + 1.0 - offset
      );
      event.renderer.box(box, side, line, shape, 0);
   }

   public static class MineContext {
      public final BlockPos pos;
      public final BlockState state;
      public long startTimeMs;
      public final float hardness;
      public boolean awaitingFinish = true;
      public int finishTick = -1;
      public final boolean isPrimary;
      public final boolean silent;
      public boolean grimOffsetSent = false;
      public boolean burstSent = false;
      public ItemStack tool;
      public boolean isInstaBreak;
      public final boolean handCanInstaBreak;
      public final MinecraftClient mc = MinecraftClient.getInstance();

      public MineContext(BlockPos pos, BlockState state, boolean isPrimary) {
         this(pos, state, isPrimary, false);
      }

      public MineContext(BlockPos pos, BlockState state, boolean isPrimary, boolean silent) {
         this.pos = pos.toImmutable();
         this.state = state;
         this.hardness = state.getHardness(this.mc.world, pos);
         this.isPrimary = isPrimary;
         this.silent = silent;
         this.startTimeMs = System.currentTimeMillis();
         this.tool = KekMine.findBestTool(state);
         this.recomputeInstaBreak();
         this.handCanInstaBreak = this.calcDeltaForTool(this.mc.player.getMainHandStack()) >= 1.0F;
      }

      public KekMine.MineContext toParkedCopy() {
         KekMine.MineContext s = new KekMine.MineContext(this.pos, this.state, false, this.silent);
         s.startTimeMs = System.currentTimeMillis();
         s.grimOffsetSent = this.grimOffsetSent;
         s.burstSent = this.burstSent;
         s.tool = this.tool;
         s.recomputeInstaBreak();
         return s;
      }

      private void recomputeInstaBreak() {
         this.isInstaBreak = this.calcDeltaForTool(this.tool) >= 1.0F;
      }

      public void retargetProgress(ItemStack tool) {
         if (tool != null && (this.tool == null || this.tool.getItem() != tool.getItem())) {
            float oldPerTick = this.calcDeltaForTool(this.tool);
            float newPerTick = this.calcDeltaForTool(tool);
            if (Float.isFinite(oldPerTick) && oldPerTick > 0.0F && Float.isFinite(newPerTick) && newPerTick > 0.0F) {
               float elapsedTicks = Math.max((float)(System.currentTimeMillis() - this.startTimeMs) / 50.0F + 1.0F, 1.0F);
               float rawProgress = oldPerTick * elapsedTicks;
               float newElapsed = rawProgress / newPerTick;
               this.startTimeMs = System.currentTimeMillis() - (long)((newElapsed - 1.0F) * 50.0F);
            }

            this.tool = tool;
            this.recomputeInstaBreak();
         }
      }

      public void resetWithTool(ItemStack tool) {
         if (tool != null) {
            this.tool = tool;
            this.startTimeMs = System.currentTimeMillis();
            this.recomputeInstaBreak();
         }
      }

      private float getBreakDelta() {
         return this.calcDeltaForTool(this.tool);
      }

      private float calcDeltaForTool(ItemStack bestTool) {
         return KekMine.calcBreakDelta(this.state, this.pos, bestTool);
      }

      private double getProgress() {
         if (this.mc.player != null && this.mc.world != null && !(this.hardness < 0.0F)) {
            float perTick = this.getBreakDelta();
            if (perTick <= 0.0F) {
               return 2.147483647E9;
            } else {
               float elapsedTicks = Math.max((float)(System.currentTimeMillis() - this.startTimeMs - 50L) / 50.0F + 1.0F, 1.0F);
               float currentProgress = perTick * elapsedTicks;
               float targetProgress = this.isPrimary ? ((Double)KekMine.INSTANCE.breakThreshold.get()).floatValue() : 1.0F;
               return Math.min((double)(currentProgress / targetProgress), 1.0);
            }
         } else {
            return 0.0;
         }
      }

      public float getBreakSpeed() {
         return this.calcDeltaForTool(this.tool);
      }

      public long estimatedMsRemaining() {
         if (this.mc.player != null && this.mc.world != null) {
            float perTick = this.getBreakDelta();
            if (perTick <= 0.0F) {
               return Long.MAX_VALUE;
            } else {
               float target = this.isPrimary ? ((Double)KekMine.INSTANCE.breakThreshold.get()).floatValue() : 1.0F;
               float ticks = target / perTick;
               return (long)(Math.max(ticks - 1.0F, 0.0F) * 50.0F);
            }
         } else {
            return Long.MAX_VALUE;
         }
      }
   }
}

package musheor.modules.automation;

import com.google.common.collect.BiMap;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BlockSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.mixin.AxeItemAccessor;
import musheor.modules.features.KekMine;
import musheor.utils.HighwayUtils;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Oxidizable;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoneycombItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import musheor.utils.HighwayBuilderState;
import musheor.utils.BlockPlacer;
import musheor.utils.PacketBudget;

public class Stripper extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Block> block2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("block")).description("Block to place and strip."))
                  .filter(Stripper::isTransformable)
                  .defaultValue(Blocks.OAK_LOG))
               .onChanged(block -> {
                  if (this.targetState != null) {
                     List<Block> chain = transformChain(block);
                     if (!chain.isEmpty()) {
                        this.targetState.set(chain.getLast());
                     }
                  }
               }))
            .build()
      );
   private final Setting<Block> targetState = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("target-state")).description("Keep interacting until the block reaches this state."))
                  .filter(block -> transformChain((Block)this.block2.get()).contains(block))
                  .defaultValue(Blocks.STRIPPED_OAK_LOG))
               .visible(() -> transformChain((Block)this.block2.get()).size() > 1))
            .build()
      );
   private final Setting<Integer> stepDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("step-delay"))
                  .description("Ticks to wait between placing, stripping and breaking. 0 runs one step per tick (3 ticks per block)."))
               .defaultValue(0))
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Boolean> selfDisable = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("self-disable"))
                  .description("Disables when you run out of blocks to strip, or when there is no axe left in your inventory."))
               .defaultValue(true))
            .build()
      );
   private Stripper.Stage stage = Stripper.Stage.IDLE;
   private BlockPos workPos;
   private BlockPos lastBreakPos;
   private int breakAttempts;
   private int stepCooldown;

   public Stripper() {
      super(Musheor.AUTOMATION, "stripper", "Automatically places, strips and breaks logs and waxed copper blocks");
   }

   public void onActivate() {
      this.stage = Stripper.Stage.IDLE;
      this.workPos = null;
      this.lastBreakPos = null;
      this.breakAttempts = 0;
      this.stepCooldown = 0;
   }

   public void onDeactivate() {
      this.workPos = null;
      this.lastBreakPos = null;
      this.stepCooldown = 0;
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!HighwayBuilder.isEating() && !HighwayBuilder.isKillAuraAttacking()) {
            if (this.stepCooldown > 0) {
               this.stepCooldown--;
            } else if (!this.hasAxe()) {
               if ((Boolean)this.selfDisable.get()) {
                  this.warning("No axe left in your inventory, disabling.", new Object[0]);
                  this.toggle();
               }
            } else {
               BlockPos pos = this.mc.player.getBlockPos().offset(this.mc.player.getHorizontalFacing());
               if (!pos.equals(this.workPos)) {
                  this.workPos = pos;
                  this.breakAttempts = 0;
                  this.stage = Stripper.Stage.IDLE;
               }

               switch (this.stage) {
                  case IDLE:
                     this.doPlace();
                     break;
                  case STRIP:
                     this.doStrip();
                     break;
                  case DONE:
                     this.doBreak();
               }
            }
         }
      }
   }

   private void doPlace() {
      BlockState state = this.mc.world.getBlockState(this.workPos);
      if (state.getBlock() == this.block2.get()) {
         this.advanceStage(Stripper.Stage.STRIP);
      } else if (!state.isReplaceable()) {
         this.advanceStage(Stripper.Stage.DONE);
      } else if (InventoryManager.countItem(((Block)this.block2.get()).asItem()) == 0) {
         if ((Boolean)this.selfDisable.get()) {
            this.info("§2Out of %s, disabling.", new Object[]{Names.get((Block)this.block2.get())});
            this.toggle();
         }
      } else {
         boolean silent = (Boolean)MusheorSystem.Manager.silentPlace.get();
         if (!InventoryManager.ensureHolding(((Block)this.block2.get()).asItem())) {
            this.stepCooldown = (Integer)MusheorSystem.Manager.swapDelay.get();
         } else {
            MusheorSystem.PlacementType type = HighwayBuilderState.getInstance().getPlacementType();
            if (type.equals(MusheorSystem.PlacementType.Rotate)) {
               HighwayUtils.lookAtBlockFace(this.workPos, Direction.DOWN);
            }

            BlockPlacer.place(this.workPos, Direction.DOWN, type, silent);
            this.advanceStage(Stripper.Stage.STRIP);
         }
      }
   }

   private void doStrip() {
      Block block = this.mc.world.getBlockState(this.workPos).getBlock();
      int needed = this.stepsNeededFrom(block);
      if (needed == 0) {
         this.advanceStage(Stripper.Stage.DONE);
      } else if (!(this.mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
         if (this.swapToAxe()) {
            this.stepCooldown = (Integer)MusheorSystem.Manager.swapDelay.get();
         }
      } else {
         Direction side = this.hitSide(this.workPos);

         for (int i = 0; i < needed; i++) {
            if (!PacketBudget.canPlace()) {
               return;
            }

            this.interactBlock(this.workPos, side);
         }

         this.advanceStage(Stripper.Stage.DONE);
      }
   }

   private void doBreak() {
      BlockState state = this.mc.world.getBlockState(this.workPos);
      if (state.isAir()) {
         this.advanceStage(Stripper.Stage.IDLE);
      } else {
         if (this.workPos.equals(this.lastBreakPos) && this.breakAttempts > 0) {
            InventoryManager.selectBestTool(this.lastBreakPos);
            KekMine.INSTANCE.sendAction(Action.STOP_DESTROY_BLOCK, this.workPos);
            if (!(Boolean)KekMine.INSTANCE.validateBreak.get()) {
               this.mc.world.syncWorldEvent(2001, this.workPos, Block.getRawIdFromState(this.mc.world.getBlockState(this.workPos)));
               this.mc.world.setBlockState(this.workPos, Blocks.AIR.getDefaultState(), 3);
            }
         } else if (KekMine.INSTANCE.isActive()) {
            KekMine.INSTANCE.startBreak(this.workPos, state, false);
         } else {
            BlockUtils.breakBlock(this.workPos, true);
         }

         this.lastBreakPos = this.workPos;
         this.breakAttempts++;
         this.advanceStage(Stripper.Stage.IDLE);
      }
   }

   private void advanceStage(Stripper.Stage next) {
      this.stage = next;
      this.stepCooldown = (Integer)this.stepDelay.get();
   }

   private boolean hasAxe() {
      return InvUtils.find(stack -> stack.getItem() instanceof AxeItem).found();
   }

   private boolean swapToAxe() {
      FindItemResult axe = InvUtils.find(stack -> stack.getItem() instanceof AxeItem);
      if (!axe.found()) {
         return false;
      } else {
         InventoryManager.swapToItem(this.mc.player.getInventory().getStack(axe.slot()).getItem());
         return true;
      }
   }

   private void interactBlock(BlockPos pos, Direction side) {
      if (this.mc.interactionManager != null) {
         Vec3d hitPos = Vec3d.ofCenter(pos).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
         BlockUtils.interact(new BlockHitResult(hitPos, side, pos, false), Hand.MAIN_HAND, true);
      }
   }

   private Direction hitSide(BlockPos pos) {
      return KekMine.INSTANCE != null ? KekMine.INSTANCE.getBestFace(pos) : Direction.UP;
   }

   public static boolean isTransformable(Block block) {
      return nextTransformedBlock(block) != null;
   }

   public static Block nextTransformedBlock(Block block) {
      Block stripped = AxeItemAccessor.getStrippedBlocks().get(block);
      if (stripped != null) {
         return stripped;
      } else {
         Block unwaxed = (Block)((BiMap)HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get()).get(block);
         return unwaxed != null ? unwaxed : (Block)Oxidizable.getDecreasedOxidationBlock(block).orElse(null);
      }
   }

   public static List<Block> transformChain(Block from) {
      List<Block> chain = new ArrayList<>(4);
      Block current = from;

      while ((current = nextTransformedBlock(current)) != null && !chain.contains(current)) {
         chain.add(current);
      }

      return chain;
   }

   private int stepsNeededFrom(Block from) {
      List<Block> chain = transformChain(from);
      int index = chain.indexOf(this.targetState.get());
      return index < 0 ? chain.size() : index + 1;
   }

   public int getStepsNeeded() {
      return this.stepsNeededFrom((Block)this.block2.get());
   }

   public String getInfoString() {
      int needed = this.getStepsNeeded();
      return needed > 1 ? Names.get((Block)this.block2.get()) + " x" + needed : Names.get((Block)this.block2.get());
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null && this.workPos != null) {
         musheor.utils.BlockRenderUtils.renderPosition(event, this.workPos);
      }
   }

   private static enum Stage {
      IDLE,
      STRIP,
      DONE;}
}

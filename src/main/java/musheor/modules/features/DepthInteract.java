package musheor.modules.features;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class DepthInteract extends Module {
   public static DepthInteract INSTANCE;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   public BlockPos targetPos = null;

   public DepthInteract() {
      super(Musheor.MAIN, "depth-interact", "Interact with containers through blocks.");
      INSTANCE = this;
   }

   public void onDeactivate() {
      this.targetPos = null;
   }

   @EventHandler
   private void onTickPost(Post event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.targetPos = this.findContainerAlongLook(this.mc.player.getBlockInteractionRange());
      }
   }

   private BlockPos findContainerAlongLook(double maxDistance) {
      Vec3d start = this.mc.player.getCameraPosVec(1.0F);
      Vec3d look = this.mc.player.getRotationVec(1.0F);

      for (double d = 0.0; d <= maxDistance; d += 0.1) {
         Vec3d point = start.add(look.multiply(d));
         BlockPos pos = BlockPos.ofFloored(point);
         if (this.mc.world.isChunkLoaded(pos)) {
            BlockEntity be = this.mc.world.getBlockEntity(pos);
            if (this.isInteractableContainer(be)) {
               return pos;
            }
         }
      }

      return null;
   }

   private boolean isInteractableContainer(BlockEntity be) {
      return be instanceof ChestBlockEntity
         || be instanceof TrappedChestBlockEntity
         || be instanceof BarrelBlockEntity
         || be instanceof ShulkerBoxBlockEntity
         || be instanceof EnderChestBlockEntity;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.targetPos != null && this.mc.world != null) {
         event.renderer.box(this.targetPos, Color.CYAN, Color.CYAN, ShapeMode.Lines, 0);
      }
   }
}

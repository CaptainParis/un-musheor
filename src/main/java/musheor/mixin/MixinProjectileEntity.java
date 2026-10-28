package musheor.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import musheor.modules.features.MoreTags;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ProjectileEntity.class})
public class MixinProjectileEntity {
   @Inject(
      method = {"method_31471"},
      at = {@At("TAIL")}
   )
   private void cacheOwnerEntityId(EntitySpawnS2CPacket packet, CallbackInfo ci) {
      if (((ProjectileEntity)(Object)this).getType() == EntityType.ENDER_PEARL) {
         int ownerEntityId = packet.getEntityData();
         if (ownerEntityId != 0) {
            MoreTags module = (MoreTags)Modules.get().get(MoreTags.class);
            if (module != null) {
               module.cachePearlOwnerEntityId(((ProjectileEntity)(Object)this).getId(), ownerEntityId);
            }
         }
      }
   }
}

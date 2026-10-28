package musheor.modules.automation;

import baritone.api.pathing.goals.GoalBlock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.utils.manager.InventoryManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import musheor.utils.BaritoneHelper;

public class AutoBreeder extends Module {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("entities")).description("Specific entities to breed."))
            .defaultValue(new EntityType[0])
            .onlyAttackable()
            .build()
      );
   private final Setting<Double> distance2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("distance"))
               .description("Maximum distance to pathfind to new mobs."))
            .defaultValue(32.0)
            .sliderRange(4.0, 128.0)
            .decimalPlaces(1)
            .build()
      );
   private final Map<UUID, Long> recentlyBred = new HashMap<>();
   private UUID pathingTarget = null;

   public AutoBreeder() {
      super(Musheor.AUTOMATION, "auto-breeder", "Automatically breeds specific animals.");
   }

   public void onActivate() {
      this.recentlyBred.clear();
      this.pathingTarget = null;
   }

   public void onDeactivate() {
      BaritoneHelper.cancel();
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         long now = System.currentTimeMillis();
         this.recentlyBred.entrySet().removeIf(e -> now - e.getValue() >= 300000L);
         List<AnimalEntity> candidates = new ArrayList<>();

         for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof AnimalEntity animal
               && ((Set)this.entities.get()).contains(animal.getType())
               && !this.recentlyBred.containsKey(animal.getUuid())
               && !animal.isBaby()
               && animal.isBreedingItem(this.mc.player.getMainHandStack())
               && PlayerUtils.isWithin(animal, (Double)this.distance2.get())) {
               candidates.add(animal);
            }
         }

         candidates.sort(Comparator.comparingDouble(a -> a.squaredDistanceTo(this.mc.player)));
         if (!candidates.isEmpty()) {
            AnimalEntity first = candidates.getFirst();
            if (!first.isBreedingItem(this.mc.player.getMainHandStack())) {
               for (int slot = 0; slot < this.mc.player.getInventory().main.size(); slot++) {
                  ItemStack stack = this.mc.player.getInventory().getStack(slot);
                  if (first.isBreedingItem(stack) && slot < 8) {
                     this.mc.player.getInventory().selectedSlot = slot;
                  } else {
                     if (!first.isBreedingItem(stack)) {
                        this.info("No breeding items found in inventory, disabling...", new Object[0]);
                        this.toggle();
                        return;
                     }

                     InventoryManager.moveItemToHotbar(stack.getItem());
                  }
               }

               return;
            }
         }

         Iterator var10 = candidates.iterator();
         if (var10.hasNext()) {
            AnimalEntity animal = (AnimalEntity)var10.next();
            if (animal.isInRange(this.mc.player, 2.5)) {
               BaritoneHelper.cancel();
               this.pathingTarget = null;
               float yaw = (float)Rotations.getYaw(animal);
               float pitch = (float)Rotations.getPitch(animal);
               Rotations.rotate(yaw, pitch);
               this.mc
                  .interactionManager
                  .sendSequencedPacket(this.mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch));
               VersionHelper.get().interactEntityAt(animal, Hand.MAIN_HAND);
               this.mc
                  .interactionManager
                  .sendSequencedPacket(this.mc.world, sequence -> PlayerInteractEntityC2SPacket.interact(animal, false, Hand.MAIN_HAND));
               this.recentlyBred.put(animal.getUuid(), System.currentTimeMillis());
            } else {
               UUID uuid = animal.getUuid();
               if (!uuid.equals(this.pathingTarget) || !BaritoneHelper.isPathing()) {
                  this.pathingTarget = uuid;
                  BaritoneHelper.setGoalAndPath(new GoalBlock(animal.getBlockPos()));
               }
            }
         }
      }
   }
}

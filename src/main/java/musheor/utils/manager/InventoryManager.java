package musheor.utils.manager;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.mixin.ContainerComponentAccessor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.modules.automation.ElytraTweakz;
import musheor.modules.automation.HighwayBuilder;
import musheor.modules.automation.KekNuker;
import musheor.modules.features.KekMine;
import musheor.utils.RestockType;
import musheor.utils.ModuleControl;
import musheor.utils.HighwayUtils;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class InventoryManager extends Module {
   private final SettingGroup sgItems = this.settings.createGroup("Items");
   private final SettingGroup sgShulkers = this.settings.createGroup("Shulkers");
   private final SettingGroup sgDelays = this.settings.createGroup("Delays");
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static int actionDelay;
   private static int openSettleTicks;
   private static int openDelay;
   private static int restockCompleteDelay;
   private static int swapDelay;
   public static BlockPos restockPos;
   private static BlockPos containerPos;
   private static int placeDelay;
   private static boolean restockInitialized;
   private static int containerScanIndex = 0;
   public static boolean isRestocking;
   public static boolean isBusy;
   private static boolean breakingContainer;
   private static boolean containerPlaced;
   private static boolean itemPrepared;
   public static boolean isPostRestocking;
   private static int stolenCount = 0;
   private static int stealWaitTicks = 0;
   private static boolean waitingStealDelay;
   public static int restockAmount;
   static int cachedToolRestockAmount;
   static int cachedFoodRestockAmount;
   static int cachedMaterialShulkerAmount;
   static int cachedToolShulkerAmount;
   public static InventoryManager INSTANCE;
   public final Setting<Integer> toolRestockAmount = this.sgItems
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("tool-restock-amount"))
                  .description("How many pickaxes should be taken on restock when the player runs out."))
               .defaultValue(1))
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<Integer> foodRestockAmount = this.sgItems
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("food-restock-amount"))
                  .description("How many stacks of food should be taken on restock when the player runs out."))
               .defaultValue(1))
            .sliderRange(1, 10)
            .build()
      );
   public final Setting<Integer> materialShulkerRestockAmount = this.sgShulkers
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("material-shulker-restock-amount"))
                  .description("How many shulkers of pavement materials should be taken when performing an enderchest restock."))
               .defaultValue(1))
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<Integer> toolShulkerRestockAmount = this.sgShulkers
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("tool-shulker-restock-amount"))
                  .description("How many shulkers of pickaxes should be taken when performing an enderchest restock."))
               .defaultValue(1))
            .sliderRange(1, 10)
            .build()
      );
   public final Setting<Integer> stealingDelay = this.sgDelays
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("stealing-delay"))
                  .description("Ticks to wait between stealing slots from a container. 0 steals every matching slot in a single tick."))
               .defaultValue(1))
            .min(0)
            .sliderRange(0, 20)
            .build()
      );
   public final Setting<Integer> delayAfterPlacingContainer = this.sgDelays
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("delay-after-placing-container"))
                  .description("Time the player should wait before placing a container in ticks"))
               .defaultValue(5))
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<Integer> delayBeforeOpeningContainer = this.sgDelays
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("delay-before-opening-container"))
                  .description("Time the player should wait before opening a container in ticks"))
               .defaultValue(5))
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<Integer> delayAfterOpeningContainer = this.sgDelays
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("delay-after-opening-container"))
                  .description("Time the player should wait after opening a container in ticks"))
               .defaultValue(5))
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<Integer> delayAfterSwappingItem = this.sgDelays
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("delay-after-swapping-item"))
                  .description("Time the player should wait before it attempts to swap items"))
               .defaultValue(5))
            .sliderRange(1, 20)
            .build()
      );
   public final Setting<Integer> delayAfterRestocking = this.sgDelays
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("delay-after-restocking"))
                  .description("Time the player should wait completing the restocking process"))
               .defaultValue(10))
            .sliderRange(1, 20)
            .build()
      );

   public InventoryManager() {
      super(Musheor.AUTOMATION, "inventory-manager", "Manager module for inventory related stuff");
      INSTANCE = this;
   }

   public static void loadSettings() {
      placeDelay = (Integer)INSTANCE.delayAfterPlacingContainer.get();
      openDelay = (Integer)INSTANCE.delayBeforeOpeningContainer.get();
      openSettleTicks = (Integer)INSTANCE.delayAfterOpeningContainer.get();
      swapDelay = (Integer)INSTANCE.delayAfterSwappingItem.get();
      restockCompleteDelay = (Integer)INSTANCE.delayAfterRestocking.get();
      cachedMaterialShulkerAmount = (Integer)INSTANCE.materialShulkerRestockAmount.get();
      cachedToolShulkerAmount = (Integer)INSTANCE.toolShulkerRestockAmount.get();
      cachedToolRestockAmount = (Integer)INSTANCE.toolRestockAmount.get();
      cachedFoodRestockAmount = (Integer)INSTANCE.foodRestockAmount.get();
   }

   public static void resetRestock(boolean handlePostRestocking) {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      loadSettings();
      isPostRestocking = false;
      state.setRestockingFromInventory(false);
      state.setRestockingShulkerFromEchest(false);
      itemPrepared = false;
      containerPlaced = false;
      stolenCount = 0;
      containerScanIndex = 0;
      restockInitialized = false;
      stealWaitTicks = 0;
      restockPos = null;
      isRestocking = false;
      state.setUnusedRestockFlag(false);
      state.setRestockType((musheor.utils.RestockType)null);
      restockAmount = 0;
      state.setPostRestocking(false);
      actionDelay = 0;
      if (handlePostRestocking) {
         isPostRestocking = true;
         state.setPostRestocking(true);
      }
   }

   public static void checkAndStartRestock() {
      musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
      if (HighwayBuilder.INSTANCE.mode2.get() != HighwayBuilder.Mode.MANUAL) {
         if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.PAVE) {
            if (HighwayBuilder.isShulkerRestocking() && HighwayBuilder.isToolShulkerRestocking() && findShulkerWithComponent(DataComponentTypes.TOOL) == null && countPickaxes(false) == 0) {
               isRestocking = true;
               state.setRestockType(RestockType.PICKAXE);
               restockAmount = cachedToolShulkerAmount;
               state.setRestockingShulkerFromEchest(true);
               MusheorSystem.debug("Tool shulker restocking process started");
               return;
            }

            if (HighwayBuilder.isShulkerRestocking()
               && HighwayBuilder.isMaterialShulkerRestocking()
               && findShulkerWithItem(Items.ENDER_CHEST) == null
               && findShulkerWithItem(HighwayBuilder.getPavementBlock().asItem()) == null) {
               isRestocking = true;
               state.setRestockType(RestockType.MATERIAL);
               restockAmount = cachedMaterialShulkerAmount;
               state.setRestockingShulkerFromEchest(true);
               MusheorSystem.debug("Echest shulker restocking process started");
               return;
            }

            if (HighwayBuilder.isItemRestockingEnabled()
               && countItem(HighwayBuilder.getPavementBlock().asItem()) <= 8
               && countItem(Items.ENDER_CHEST) <= 8
               && (findShulkerWithItem(Items.ENDER_CHEST) != null || findShulkerWithItem(HighwayBuilder.getPavementBlock().asItem()) != null)) {
               isRestocking = true;
               state.setRestockType(RestockType.MATERIAL);
               restockAmount = countEmptySlots() / 2;
               state.setRestockingFromInventory(true);
               MusheorSystem.debug("Material restocking process started");
               return;
            }

            if (HighwayBuilder.isItemRestockingEnabled() && findShulkerWithComponent(DataComponentTypes.TOOL) != null && countPickaxes(false) < 1) {
               isRestocking = true;
               state.setRestockType(RestockType.PICKAXE);
               restockAmount = cachedToolRestockAmount;
               state.setRestockingFromInventory(true);
               MusheorSystem.debug("Pickaxe restocking process started");
               return;
            }

            if (HighwayBuilder.isItemRestockingEnabled() && !HighwayBuilder.hasUsableFood() && findShulkerWithComponent(DataComponentTypes.FOOD) != null) {
               isRestocking = true;
               state.setRestockType(RestockType.FOOD);
               restockAmount = cachedFoodRestockAmount;
               state.setRestockingFromInventory(true);
               MusheorSystem.debug("Food restocking process started");
               return;
            }
         }

         if (HighwayBuilder.getBuildMode() == HighwayBuilder.BuildMode.DIG) {
            if (HighwayBuilder.isShulkerRestocking() && HighwayBuilder.isToolShulkerRestocking() && findShulkerWithComponent(DataComponentTypes.TOOL) == null && countPickaxes(true) == 0) {
               isRestocking = true;
               state.setRestockType(RestockType.SILK_PICKAXE);
               restockAmount = cachedToolShulkerAmount;
               state.setRestockingShulkerFromEchest(true);
               MusheorSystem.debug("Tool shulker restocking process started");
               return;
            }

            if (HighwayBuilder.isItemRestockingEnabled() && findShulkerWithComponent(DataComponentTypes.TOOL) != null && countPickaxes(true) == 0) {
               isRestocking = true;
               state.setRestockType(RestockType.SILK_PICKAXE);
               restockAmount = cachedToolShulkerAmount;
               state.setRestockingFromInventory(true);
               MusheorSystem.debug("Tool restocking process started");
               return;
            }

            if (HighwayBuilder.isItemRestockingEnabled() && !HighwayBuilder.hasUsableFood()) {
               isRestocking = true;
               state.setRestockType(RestockType.FOOD);
               restockAmount = cachedFoodRestockAmount;
               state.setRestockingFromInventory(true);
               MusheorSystem.debug("Food restocking process started");
               return;
            }
         }
      }
   }

   private static boolean isLootableContainer(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      return block instanceof ChestBlock
         || block instanceof ShulkerBoxBlock
         || block instanceof BarrelBlock
         || block instanceof HopperBlock
         || block instanceof DispenserBlock
         || block instanceof DropperBlock;
   }

   public static boolean hasOpenScreen() {
      if (mc.player == null) {
         return false;
      } else {
         ScreenHandler handler = mc.player.currentScreenHandler;
         return handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler;
      }
   }

   public static boolean canOpenContainerAt(BlockPos pos) {
      if (mc.player == null || mc.world == null) {
         return false;
      } else if (!isLootableContainer(pos)) {
         return false;
      } else if (hasOpenScreen()) {
         return true;
      } else {
         Vec3d hitVec = Vec3d.ofCenter(pos);
         HighwayUtils.lookAtBlock(pos);
         BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
         interactBlock(hitResult);
         return false;
      }
   }

   public static void interactBlock(BlockHitResult bhr) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         mc.interactionManager.sendSequencedPacket(mc.world, sequence -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, sequence));
      }
   }

   public static boolean isToolDurableEnough(ItemStack stack) {
      return stack.getMaxDamage() - stack.getDamage() > (Integer)KekMine.INSTANCE.minToolDurability.get();
   }

   public static void moveItemToHotbar(Item item) {
      int slot = 0;

      for (int i = 9; i < mc.player.getInventory().main.size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            slot = i;
            break;
         }
      }

      if (slot != 0) {
         InvUtils.move().from(slot).to(findEmptyHotbarSlot());
      }
   }

   public static int moveToHotbar(ItemStack stack) {
      int dest = findEmptyHotbarSlot();
      InvUtils.move().from(mc.player.getInventory().getSlotWithStack(stack)).to(dest);
      return dest;
   }

   public static void selectHotbarSlot(int slot) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         slot = Math.max(0, Math.min(8, slot));
         if (mc.player.getInventory().selectedSlot != slot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   }

   public static int findEmptyHotbarSlot() {
      if (mc.player == null) {
         return 8;
      } else {
         for (int i = 0; i <= 8; i++) {
            ItemStack current = mc.player.getInventory().getStack(i);
            if (current.isEmpty()) {
               return i;
            }
         }

         return Utils.random(1, 7);
      }
   }

   public static int findEmptyInventorySlot() {
      if (mc.player == null) {
         return 35;
      } else {
         for (int i = 9; i < 36; i++) {
            ItemStack current = mc.player.getInventory().getStack(i);
            if (current.isEmpty()) {
               return i;
            }
         }

         return Utils.random(9, 35);
      }
   }

   public static boolean withHotbarSwap(int slot, Runnable action) {
      if (mc.player == null || action == null) {
         return false;
      } else if (slot >= 0 && slot <= 35) {
         boolean swapped = swapWithSelected(slot);

         boolean var3;
         try {
            action.run();
            var3 = true;
         } finally {
            if (swapped) {
               swapWithSelected(slot);
            }
         }

         return var3;
      } else {
         return false;
      }
   }

   public static boolean swapWithSelected(int slot) {
      int currentSlot = mc.player.getInventory().selectedSlot;
      if (slot == currentSlot) {
         return false;
      } else {
         int screenSlot = toScreenSlot(slot);
         mc.interactionManager
            .clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, currentSlot, SlotActionType.SWAP, mc.player);
         MusheorSystem.debug(
            "Swapped slot %d to selected in screen %s with ID: %d",
            slot,
            mc.player.currentScreenHandler.toString(),
            mc.player.currentScreenHandler.syncId
         );
         return true;
      }
   }

   public static int toScreenSlot(int slot) {
      return slot >= 0 && slot <= 8 ? 36 + slot : slot;
   }

   public static int findHotbarItem(Item item) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static int findItemSlot(Item item) {
      for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().main.size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static int findUsableElytraSlot() {
      for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().main.size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.ELYTRA && stack.getMaxDamage() - stack.getDamage() > (Integer)ElytraTweakz.INSTANCE.durabilityThreshold.get()) {
            return i;
         }
      }

      return -1;
   }

   public static int findChestArmorSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         PlayerInventory inventory = mc.player.getInventory();

         for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
            if (equippable != null && equippable.slot() == EquipmentSlot.CHEST && !stack.isOf(Items.ELYTRA)) {
               return i;
            }
         }

         return -1;
      }
   }

   public static void throwSlot(int slot, boolean entireStack) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.interactionManager
            .clickSlot(mc.player.currentScreenHandler.syncId, toScreenSlot(slot), entireStack ? 1 : 0, SlotActionType.THROW, mc.player);
      }
   }

   public static void swapElytraChestplate() {
      ItemStack currentItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      if (currentItem.contains(DataComponentTypes.GLIDER)) {
         int chest = findChestArmorSlot();
         InvUtils.move().from(chest).toArmor(2);
      } else if (currentItem.contains(DataComponentTypes.EQUIPPABLE)
         && ((EquippableComponent)currentItem.get(DataComponentTypes.EQUIPPABLE)).slot().getEntitySlotId() == EquipmentSlot.CHEST.getEntitySlotId()) {
         int elytra = findUsableElytraSlot();
         InvUtils.move().from(elytra).toArmor(2);
      } else {
         int elytra = findUsableElytraSlot();
         InvUtils.move().from(elytra).toArmor(2);
      }
   }

   public static ItemStack findStack(Item item) {
      for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().main.size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            return stack;
         }
      }

      return null;
   }

   public static int countItemInShulker(ItemStack shulker, Item item) {
      ItemStack[] containerItems = new ItemStack[27];
      Utils.getItemsInContainerItem(shulker, containerItems);
      int count = 0;

      for (ItemStack stack : containerItems) {
         if (!stack.isEmpty() && stack.getItem() == item) {
            count += stack.getCount();
         }
      }

      return count;
   }

   public static int countComponentInShulker(ItemStack shulker, ComponentType<?> component) {
      ItemStack[] containerItems = new ItemStack[27];
      Utils.getItemsInContainerItem(shulker, containerItems);
      int count = 0;

      for (ItemStack stack : containerItems) {
         if (!stack.isEmpty() && stack.getComponents().contains(component)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   public static int countItem(Item item) {
      int count = 0;

      for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            count += stack.getCount();
         }
      }

      return count;
   }

   public static int countItemIncludingShulkers(Item item) {
      int count = 0;

      for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (!itemStack.isEmpty()) {
            if (itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
               count += countItemInShulker(itemStack, item);
            }

            if (itemStack.getItem() == item) {
               count += itemStack.getCount();
            }
         }
      }

      return count;
   }

   public static ItemStack findShulkerWithItem(Item item) {
      for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().main.size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock && shulkerContainsItem(stack, item)) {
            return stack;
         }
      }

      return null;
   }

   public static ItemStack findSmallestShulkerWithItem(Item item) {
      int count = 0;
      int least = 1729;
      ItemStack stack = null;

      for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().size(); i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
            count = countItemInShulker(itemStack, item);
            if (least > count && count > 0) {
               least = count;
               stack = itemStack;
            }
         }
      }

      return stack;
   }

   public static ItemStack findSmallestShulkerWithComponent(ComponentType<?> component) {
      int count = 0;
      int least = 99;
      ItemStack stack = null;

      for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().size(); i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
            count = countComponentInShulker(itemStack, component);
            if (least > count && count > 0) {
               least = count;
               stack = itemStack;
            }
         }
      }

      return stack;
   }

   public static ItemStack findShulkerWithComponent(ComponentType<?> component) {
      for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().main.size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock && shulkerContainsComponent(stack, component)) {
            return stack;
         }
      }

      return null;
   }

   public static boolean shulkerContainsComponent(ItemStack shulker, ComponentType<?> component) {
      ItemStack[] containerItems = new ItemStack[27];
      Utils.getItemsInContainerItem(shulker, containerItems);

      for (ItemStack stack : containerItems) {
         if (!stack.isEmpty() && stack.getComponents().contains(component)) {
            if (component != DataComponentTypes.TOOL) {
               return true;
            }

            if (stack.getMaxDamage() - stack.getDamage() > 50) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean shulkerContainsItem(ItemStack shulker, Item targetItem) {
      ItemStack[] containerItems = new ItemStack[27];
      Utils.getItemsInContainerItem(shulker, containerItems);

      for (ItemStack stack : containerItems) {
         if (!stack.isEmpty() && stack.getItem() == targetItem) {
            if (!VersionHelper.get().isPickaxe(stack)) {
               return true;
            }

            if (stack.getMaxDamage() - stack.getDamage() > 50) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean isEmptyShulker(ItemStack shulker) {
      ComponentMap components = shulker.getComponents();
      if (!components.contains(DataComponentTypes.CONTAINER)) {
         return false;
      } else {
         ContainerComponentAccessor container = (ContainerComponentAccessor)(Object)components.get(DataComponentTypes.CONTAINER);
         if (container != null) {
            for (ItemStack stack : VersionHelper.get().getStacks(container)) {
               if (!stack.isEmpty()) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public static void swapToItem(Item item) {
      swapToItemSilent(item, false);
   }

   public static void swapToItemSilent(Item item, boolean silent) {
      assert mc.player != null;

      FindItemResult result = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == item);
      if (result.found()) {
         InvUtils.swap(result.slot(), silent);
      } else {
         moveItemToHotbar(item);
      }

      MusheorSystem.debug("Swapping to %s", Registries.ITEM.getId(item.asItem()).toString());
   }

   public static boolean ensureHolding(Item item) {
      assert mc.player != null;

      if (mc.player.getMainHandStack().getItem() == item) {
         return true;
      } else {
         boolean silent = (Boolean)MusheorSystem.Manager.silentPlace.get();
         swapToItemSilent(item, silent);
         return silent && mc.player.getMainHandStack().getItem() == item;
      }
   }

   public static void selectBestPickaxeForEchest(boolean silkTouch) {
      assert mc.player != null;

      double bestScore = -1.0;
      int bestSlot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (!Utils.hasEnchantments(itemStack, new RegistryKey[]{Enchantments.SILK_TOUCH}) || !silkTouch) {
            double score = itemStack.getMiningSpeedMultiplier(Blocks.ENDER_CHEST.getDefaultState());
            if (score > bestScore) {
               bestScore = score;
               bestSlot = i;
            }
         }
      }

      if (bestSlot != -1) {
         InvUtils.swap(bestSlot, false);
      }
   }

   public static ItemStack findBestTool(BlockState state) {
      if (mc.player == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack bestStack = mc.player.getMainHandStack();
         float bestSpeed = 1.0F;

         for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!(Boolean)KekMine.INSTANCE.preventToolBreaking.get() || isToolDurableEnough(stack)) {
               float speed = stack.getMiningSpeedMultiplier(state);
               if (speed > bestSpeed) {
                  bestSpeed = speed;
                  bestStack = stack;
               }
            }
         }

         return bestStack;
      }
   }

   public static void selectBestTool(BlockPos blockPos) {
      assert mc.world != null && mc.player != null && blockPos != null;

      BlockState state = mc.world.getBlockState(blockPos);
      float bestSpeed = 1.0F;
      int bestSlot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (!itemStack.isEmpty() && (!(Boolean)KekMine.INSTANCE.preventToolBreaking.get() || isToolDurableEnough(itemStack))) {
            float speed = itemStack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed && itemStack.isSuitableFor(state)) {
               bestSpeed = speed;
               bestSlot = i;
            }
         }
      }

      if (bestSlot != -1) {
         mc.player.getInventory().selectedSlot = bestSlot;
      } else if (VersionHelper.get().isTool(mc.player.getMainHandStack()) && !isToolDurableEnough(mc.player.getMainHandStack())) {
         InvUtils.move().from(mc.player.getInventory().selectedSlot).to(findEmptyInventorySlot());
      } else {
         for (int ix = 9; ix < mc.player.getInventory().main.size(); ix++) {
            ItemStack itemStack = mc.player.getInventory().getStack(ix);
            if (!itemStack.isEmpty() && (!(Boolean)KekMine.INSTANCE.preventToolBreaking.get() || isToolDurableEnough(itemStack))) {
               float speed = itemStack.getMiningSpeedMultiplier(state);
               if (speed > bestSpeed && itemStack.isSuitableFor(state)) {
                  bestSpeed = speed;
                  bestSlot = ix;
               }
            }
         }

         if (bestSlot != -1) {
            int hotbarSlot = moveToHotbar(mc.player.getInventory().getStack(bestSlot));
            mc.player.getInventory().selectedSlot = Math.max(0, Math.min(8, hotbarSlot));
         }
      }
   }

   public static int countPickaxes(boolean allowSilkTouch) {
      assert mc.player != null;

      int count = 0;

      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (VersionHelper.get().isPickaxe(itemStack)) {
            if ((Boolean)KekMine.INSTANCE.preventToolBreaking.get()) {
               if (!allowSilkTouch
                  && !Utils.hasEnchantments(itemStack, new RegistryKey[]{Enchantments.SILK_TOUCH})
                  && itemStack.getMaxDamage() - itemStack.getDamage() > (Integer)KekMine.INSTANCE.minToolDurability.get()) {
                  count++;
               }

               if (allowSilkTouch && itemStack.getMaxDamage() - itemStack.getDamage() > (Integer)KekMine.INSTANCE.minToolDurability.get()) {
                  count++;
               }
            } else if (!allowSilkTouch && !Utils.hasEnchantments(itemStack, new RegistryKey[]{Enchantments.SILK_TOUCH})) {
               count++;
            } else if (allowSilkTouch) {
               count++;
            }
         }
      }

      return count;
   }

   public static int countBrokenPickaxes() {
      assert mc.player != null;

      int count = 0;

      for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (VersionHelper.get().isPickaxe(itemStack)
            && itemStack.getMaxDamage() - itemStack.getDamage() <= (Integer)KekMine.INSTANCE.minToolDurability.get()) {
            count++;
         }
      }

      return count;
   }

   public static int findBrokenPickaxeScreenSlot() {
      assert mc.player != null;

      for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
         ItemStack itemStack = mc.player.currentScreenHandler.getSlot(i).getStack();
         if (VersionHelper.get().isPickaxe(itemStack)
            && itemStack.getMaxDamage() - itemStack.getDamage() <= (Integer)KekMine.INSTANCE.minToolDurability.get()
            && i > 26) {
            return i;
         }
      }

      return -1;
   }

   public static int countEmptySlots() {
      int emptyCount = 0;

      assert mc.player != null;

      for (ItemStack itemStack : mc.player.getInventory().main) {
         if (itemStack.isEmpty()) {
            emptyCount++;
         }
      }

      return emptyCount;
   }

   public static int countEmptyContainerSlots(ScreenHandler handler) {
      int emptyCount = 0;
      int playerInvOffset = SlotUtils.indexToId(9);

      for (int i = 0; i < playerInvOffset; i++) {
         if (handler.getSlot(i).getStack().isEmpty()) {
            emptyCount++;
         }
      }

      return emptyCount;
   }

   public static boolean moveShulkerWithItemToHotbar(Item itemInShulker, int hotbarSlot) {
      ItemStack stack = findSmallestShulkerWithItem(itemInShulker);
      if (stack == null) {
         MusheorSystem.debug("No shulker with " + itemInShulker.toString() + " found in the inventory");
         return false;
      } else {
         int slot = mc.player.getInventory().getSlotWithStack(stack);
         if (slot == hotbarSlot) {
            return true;
         } else if (slot == -1) {
            return false;
         } else {
            InvUtils.move().from(slot).to(hotbarSlot);
            MusheorSystem.debug("Moving shulker with " + itemInShulker.toString() + " to slot " + hotbarSlot);
            return isShulkerStack(mc.player.getInventory().getStack(hotbarSlot));
         }
      }
   }

   public static boolean moveShulkerWithComponentToHotbar(ComponentType<?> type, int hotbarSlot) {
      ItemStack stack = findSmallestShulkerWithComponent(type);
      if (stack == null) {
         MusheorSystem.debug("No shulker with " + type.toString() + " found in the inventory");
         return false;
      } else {
         int slot = mc.player.getInventory().getSlotWithStack(stack);
         if (slot == hotbarSlot) {
            return true;
         } else if (slot == -1) {
            return false;
         } else {
            InvUtils.move().from(slot).to(hotbarSlot);
            MusheorSystem.debug("Moving shulker with " + type.toString() + " to slot " + hotbarSlot);
            return isShulkerStack(mc.player.getInventory().getStack(hotbarSlot));
         }
      }
   }

   public static Item findNearbyShulkerItem(double radius) {
      if (mc.world != null && mc.player != null) {
         Box area = mc.player.getBoundingBox().expand(radius);

         for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, area, e -> true)) {
            ItemStack stack = itemEntity.getStack();
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
               return itemEntity.getStack().getItem();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static void restockFromShulker(RestockType type, int stacks) {
      if (mc.world != null && mc.player != null && type != null) {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         if (!ModuleControl.isGatherItemActive() && !state.isEchestFarmBusy()) {
            ModuleControl.setAutoWalk(false);
            Module nuker = Modules.get().get(KekNuker.class);
            if (nuker.isActive()) {
               nuker.toggle();
            }

            if (actionDelay > 0) {
               actionDelay--;
            } else {
               if (!restockInitialized) {
                  selectHotbarSlot(8);
                  VersionHelper.get().syncInventory();
                  restockInitialized = true;
                  MusheorSystem.debug("Restocking has been initialized, proceeding...");
               }

               if (mc.player.getBlockPos().getSquaredDistance(restockPos) > 1.5) {
                  BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(restockPos));
                  MusheorSystem.debug("Player is not at restocking position, calling baritone...");
               } else if (!itemPrepared) {
                  boolean moved = false;
                  switch (type) {
                     case SILK_PICKAXE:
                     case PICKAXE:
                        moved = moveShulkerWithComponentToHotbar(DataComponentTypes.TOOL, 8);
                        break;
                     case FOOD:
                        moved = moveShulkerWithComponentToHotbar(DataComponentTypes.FOOD, 8);
                        break;
                     case MATERIAL:
                        Item item = null;
                        if (findShulkerWithItem(HighwayBuilder.getPavementBlock().asItem()) != null) {
                           item = HighwayBuilder.getPavementBlock().asItem();
                        } else if (HighwayBuilder.getPavementBlock() == Blocks.OBSIDIAN && findShulkerWithItem(Items.ENDER_CHEST) != null) {
                           item = Items.ENDER_CHEST;
                        }

                        if (item != null) {
                           moved = moveShulkerWithItemToHotbar(item, 8);
                        }
                  }

                  if (moved) {
                     itemPrepared = true;
                  }

                  actionDelay = swapDelay;
               } else {
                  if (!containerPlaced) {
                     containerPos = HighwayUtils.getBehindPos(HighwayBuilder.getHighwayDirection());
                     if (placeDelay > 0) {
                        ModuleControl.setKeyPressed(mc.options.sneakKey, false);
                        placeDelay--;
                        HighwayUtils.lookAtBlockFace(containerPos, Direction.DOWN);
                        return;
                     }

                     if (mc.world.getBlockState(containerPos).getBlock() instanceof ShulkerBoxBlock) {
                        MusheorSystem.debug("Shulker box placed successfully!");
                        containerPlaced = true;
                        actionDelay = placeDelay;
                     } else {
                        if (BlockUtils.canPlace(containerPos, true) || mc.world.getBlockState(containerPos).isReplaceable()) {
                           if (!isShulkerStack(mc.player.getInventory().getStack(8))) {
                              MusheorSystem.debug("Slot 8 is not holding a shulker, redoing the swap");
                              itemPrepared = false;
                              actionDelay = swapDelay;
                              return;
                           } else {
                              MusheorSystem.debug("Placing shulkerbox at x: %s y: %s z: %s", containerPos.getX(), containerPos.getY(), containerPos.getZ());
                              selectHotbarSlot(8);
                              musheor.utils.BlockPlacer.place(containerPos, Direction.UP, state.getPlacementType(), (Boolean)MusheorSystem.Manager.silentPlace.get());
                              return;
                           }
                        }

                        if (KekMine.INSTANCE.isActive()) {
                           KekMine.INSTANCE.startBreak(containerPos, mc.world.getBlockState(containerPos), false);
                           MusheorSystem.debug("Breaking obstructing block at restocking position");
                           return;
                        }

                        BlockUtils.breakBlock(containerPos, true);
                        MusheorSystem.debug("Breaking obstructing block at restocking position");
                     }
                  }

                  if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
                     HighwayUtils.lookAtBlock(containerPos);
                     mc.interactionManager
                        .sendSequencedPacket(
                           mc.world,
                           sequence -> new PlayerInteractBlockC2SPacket(
                              Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(containerPos), Direction.UP, containerPos, false), sequence
                           )
                        );
                     MusheorSystem.debug("Opening shulkerbox at x: %s y: %s z: %s", containerPos.getX(), containerPos.getY(), containerPos.getZ());
                     actionDelay = openDelay;
                  } else if (openSettleTicks > 0) {
                     openSettleTicks--;
                  } else {
                     ScreenHandler handler = mc.player.currentScreenHandler;
                     int containerSlots = getContainerSlotCount(handler);
                     if (stolenCount < stacks && containerScanIndex < containerSlots) {
                        int stealDelay = (Integer)INSTANCE.stealingDelay.get();
                        if (stealDelay <= 0) {
                           for (; containerScanIndex < containerSlots && stolenCount < stacks; containerScanIndex++) {
                              if (stealMatchingSlot(type, handler, containerScanIndex)) {
                                 stolenCount++;
                              }
                           }
                        } else {
                           if (waitingStealDelay) {
                              if (stealWaitTicks < stealDelay) {
                                 stealWaitTicks++;
                                 return;
                              }

                              waitingStealDelay = false;
                           }

                           if (stealMatchingSlot(type, handler, containerScanIndex)) {
                              stolenCount++;
                              waitingStealDelay = true;
                              stealWaitTicks = 0;
                           }

                           containerScanIndex++;
                        }
                     } else {
                        MusheorSystem.debug("Completed item restocking process");
                        resetRestock(true);
                     }
                  }
               }
            }
         } else {
            isRestocking = false;
         }
      }
   }

   private static int getContainerSlotCount(ScreenHandler handler) {
      return Math.max(0, handler.slots.size() - mc.player.getInventory().main.size());
   }

   private static boolean stealMatchingSlot(RestockType type, ScreenHandler handler, int slot) {
      ItemStack stack = handler.getSlot(slot).getStack();
      if (!matchesRestockType(type, stack)) {
         return false;
      } else {
         String stolen = stack.getItem().getName().getString();
         InvUtils.shiftClick().slotId(slot);
         if ((type == RestockType.PICKAXE || type == RestockType.SILK_PICKAXE) && HighwayBuilder.isSwapBrokenPickaxes()) {
            int brokenPickaxeSlot = findBrokenPickaxeScreenSlot();
            if (brokenPickaxeSlot != -1) {
               InvUtils.shiftClick().slotId(brokenPickaxeSlot);
            }
         }

         MusheorSystem.debug("Stealing {%s} from slot [%s]", stolen, slot);
         return true;
      }
   }

   private static boolean matchesRestockType(RestockType type, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return switch (type) {
            case SILK_PICKAXE -> VersionHelper.get().isPickaxe(stack)
               && Utils.hasEnchantments(stack, new RegistryKey[]{Enchantments.SILK_TOUCH})
               && isToolDurableEnough(stack);
            case PICKAXE -> VersionHelper.get().isPickaxe(stack)
               && !Utils.hasEnchantments(stack, new RegistryKey[]{Enchantments.SILK_TOUCH})
               && isToolDurableEnough(stack);
            case FOOD -> stack.getComponents().contains(DataComponentTypes.FOOD)
               && !((List)((AutoEat)Modules.get().get(AutoEat.class)).blacklist.get()).contains(stack.getItem());
            case MATERIAL -> stack.getItem() == HighwayBuilder.getPavementBlock().asItem()
               || HighwayBuilder.getPavementBlock() == Blocks.OBSIDIAN && stack.getItem() == Items.ENDER_CHEST;
         };
      }
   }

   public static void restockShulkersFromEchest(RestockType type, int shulkers) {
      if (mc.world != null && mc.player != null && type != null) {
         musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
         if (!ModuleControl.isGatherItemActive() && !state.isEchestFarmBusy()) {
            ModuleControl.setAutoWalk(false);
            Module nuker = Modules.get().get(KekNuker.class);
            if (nuker.isActive()) {
               nuker.toggle();
            }

            if (actionDelay > 0) {
               actionDelay--;
            } else {
               if (!restockInitialized) {
                  selectHotbarSlot(findItemSlot(Items.ENDER_CHEST));
                  VersionHelper.get().syncInventory();
                  restockInitialized = true;
                  MusheorSystem.debug("Echest shulker restocking has been initialized, proceeding...");
               }

               if (mc.player.getBlockPos().getSquaredDistance(restockPos) > 1.5) {
                  BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(restockPos));
                  MusheorSystem.debug("Player is not at restocking position, calling baritone...");
               } else if (!itemPrepared) {
                  int echestSlot = findItemSlot(Items.ENDER_CHEST);
                  if (echestSlot == -1) {
                     MusheorSystem.debug("No enderchest in inventory to place for echest restock — aborting.");
                     resetRestock(false);
                  } else {
                     selectHotbarSlot(echestSlot);
                     itemPrepared = true;
                     actionDelay = swapDelay;
                  }
               } else {
                  if (!containerPlaced) {
                     containerPos = HighwayUtils.getBehindPos(HighwayBuilder.getHighwayDirection());
                     if (placeDelay > 0) {
                        ModuleControl.setKeyPressed(mc.options.sneakKey, false);
                        placeDelay--;
                        HighwayUtils.lookAtBlockFace(containerPos, Direction.DOWN);
                        return;
                     }

                     if (mc.world.getBlockState(containerPos).getBlock() instanceof EnderChestBlock) {
                        MusheorSystem.debug("Enderchest placed successfully!");
                        containerPlaced = true;
                        actionDelay = placeDelay;
                     } else {
                        if (state.getRecentPlacementAttempts().containsKey(containerPos)) {
                           return;
                        }

                        if (BlockUtils.canPlace(containerPos, true) || mc.world.getBlockState(containerPos).isReplaceable()) {
                           MusheorSystem.debug("Placing enderchest at x: %s y: %s z: %s", containerPos.getX(), containerPos.getY(), containerPos.getZ());
                           swapToItemSilent(Items.ENDER_CHEST, (Boolean)MusheorSystem.Manager.silentPlace.get());
                           musheor.utils.BlockPlacer.place(containerPos, Direction.DOWN, state.getPlacementType(), (Boolean)MusheorSystem.Manager.silentPlace.get());
                           state.getRecentPlacementAttempts().put(containerPos, state.getTickCount());
                           return;
                        }

                        if (KekMine.INSTANCE.isActive()) {
                           KekMine.INSTANCE.startBreak(containerPos, mc.world.getBlockState(containerPos), false);
                           MusheorSystem.debug("Breaking obstructing block at restocking position");
                           return;
                        }

                        BlockUtils.breakBlock(containerPos, true);
                        MusheorSystem.debug("Breaking obstructing block at restocking position");
                     }
                  }

                  if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
                     HighwayUtils.lookAtBlock(containerPos);
                     mc.interactionManager
                        .sendSequencedPacket(
                           mc.world,
                           sequence -> new PlayerInteractBlockC2SPacket(
                              Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(containerPos), Direction.UP, containerPos, false), sequence
                           )
                        );
                     MusheorSystem.debug("Opening enderchest at x: %s y: %s z: %s", containerPos.getX(), containerPos.getY(), containerPos.getZ());
                     actionDelay = openDelay;
                  } else if (openSettleTicks > 0) {
                     openSettleTicks--;
                  } else {
                     ScreenHandler handler = mc.player.currentScreenHandler;
                     int containerSlots = getContainerSlotCount(handler);
                     if (stolenCount < shulkers && containerScanIndex < containerSlots) {
                        int stealDelay = (Integer)INSTANCE.stealingDelay.get();
                        if (stealDelay <= 0) {
                           for (; containerScanIndex < containerSlots && stolenCount < shulkers; containerScanIndex++) {
                              if (stealShulkerFromEchest(type, handler, containerScanIndex)) {
                                 stolenCount++;
                              }
                           }
                        } else {
                           if (waitingStealDelay) {
                              if (stealWaitTicks < stealDelay) {
                                 stealWaitTicks++;
                                 return;
                              }

                              waitingStealDelay = false;
                           }

                           if (stealShulkerFromEchest(type, handler, containerScanIndex)) {
                              stolenCount++;
                              waitingStealDelay = true;
                              stealWaitTicks = 0;
                           }

                           containerScanIndex++;
                        }
                     } else {
                        MusheorSystem.debug("Completed echest shulker restocking process");
                        resetRestock(true);
                     }
                  }
               }
            }
         } else {
            isRestocking = false;
         }
      }
   }

   private static boolean isShulkerStack(ItemStack stack) {
      return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }

   private static boolean shulkerMatchesRestockType(RestockType type, ItemStack shulker) {
      if (!isShulkerStack(shulker)) {
         return false;
      } else {
         ItemStack[] contents = new ItemStack[27];
         Utils.getItemsInContainerItem(shulker, contents);

         for (ItemStack stack : contents) {
            if (stack != null && matchesRestockType(type, stack)) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean stealShulkerFromEchest(RestockType type, ScreenHandler handler, int slot) {
      if (!musheor.utils.EchestSlotLocks.isUnlocked(slot)) {
         return false;
      } else {
         ItemStack stack = handler.getSlot(slot).getStack();
         if (!shulkerMatchesRestockType(type, stack)) {
            return false;
         } else {
            String stolen = stack.getItem().getName().getString();
            InvUtils.shiftClick().slotId(slot);
            MusheorSystem.debug("Grabbing shulker {%s} from echest slot [%s]", stolen, slot);
            return true;
         }
      }
   }

   public static boolean selectBestPickaxeForEchestMining() {
      if (mc.player == null) {
         return false;
      } else {
         BlockState echest = Blocks.ENDER_CHEST.getDefaultState();
         int bestSlot = -1;
         float bestSpeed = -1.0F;

         for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (VersionHelper.get().isPickaxe(stack)) {
               float speed = stack.getMiningSpeedMultiplier(echest);
               if (speed > bestSpeed) {
                  bestSpeed = speed;
                  bestSlot = i;
               }
            }
         }

         if (bestSlot == -1) {
            return false;
         } else {
            if (bestSlot < 9) {
               InvUtils.swap(bestSlot, false);
            } else {
               int hotbarSlot = moveToHotbar(mc.player.getInventory().getStack(bestSlot));
               selectHotbarSlot(hotbarSlot);
            }

            return true;
         }
      }
   }

   public static void tickPostRestock() {
      if (mc.player != null && mc.world != null && containerPos != null) {
         if (!isBusy && !HighwayBuilder.isEchestFarmerActive() && !isRestocking) {
            musheor.utils.HighwayBuilderState state = musheor.utils.HighwayBuilderState.getInstance();
            if (mc.currentScreen instanceof HandledScreen && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
               mc.player.closeHandledScreen();
               mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
            } else {
               Module nuker = Modules.get().get(KekNuker.class);
               if (!nuker.isActive()) {
                  nuker.toggle();
               }

               if (isPostRestocking) {
                  MusheorSystem.debug("Post-restocking task running...");
                  ModuleControl.setAutoWalk(false);
                  Block restockBlock = mc.world.getBlockState(containerPos).getBlock();
                  if (restockBlock instanceof ShulkerBoxBlock || restockBlock instanceof EnderChestBlock) {
                     breakingContainer = true;
                     if (restockBlock instanceof EnderChestBlock) {
                        if (!selectBestPickaxeForEchestMining()) {
                           selectBestTool(containerPos);
                        }
                     } else {
                        selectBestTool(containerPos);
                     }

                     if (PlayerUtils.isWithinReach(containerPos)) {
                        ModuleControl.cancelBaritone();
                        if (KekMine.INSTANCE.isActive()) {
                           KekMine.INSTANCE.startBreak(containerPos, mc.world.getBlockState(containerPos), false);
                        }

                        if (BlockUtils.breakBlock(containerPos, true)) {
                           return;
                        }
                     } else if (!BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(containerPos));
                     }
                  }

                  if (!mc.world.getBlockState(containerPos).isAir()) {
                     return;
                  }

                  breakingContainer = false;
                  if (restockCompleteDelay > 0) {
                     restockCompleteDelay--;
                     return;
                  }

                  Item item = findNearbyShulkerItem(12.0);
                  if (item != null) {
                     ModuleControl.startGatherItem(item, false);
                  }

                  if (mc.world.getBlockState(containerPos).getBlock().equals(Blocks.AIR)
                     && !ModuleControl.isGatherItemActive()
                     && !state.isRestockingFromInventory()
                     && !state.isRestockingShulkerFromEchest()
                     && !breakingContainer
                     && !state.isEchestFarmBusy()) {
                     resetRestock(false);
                  }
               }
            }
         }
      }
   }
}

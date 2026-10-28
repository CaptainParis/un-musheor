package musheor.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.compat.VersionHelper;
import musheor.utils.HighwayUtils;
import musheor.utils.system.MusheorSystem;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RestockConfig extends Command {
   private static final Map<Item, List<BlockPos>> containersByItem = new HashMap<>();
   private static boolean manualConfigActive = false;
   private static Item configItem = null;
   private static boolean autoConfigActive = false;
   private static BlockPos pendingContainerPos = null;
   private static int openScreenDelayTicks = -1;

   public RestockConfig() {
      super("rc", "Configure restocking container locations for items.", new String[]{"rc"});
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(this::cmdRoot);
      builder.then(literal("auto").executes(this::cmdAuto));
      builder.then(literal("add").then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS)).executes(this::cmdAdd)));
      builder.then(literal("clear").executes(this::cmdClear));
      builder.then(literal("list").executes(this::cmdList));
      builder.then(literal("remove").then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS)).executes(this::cmdRemove)));
   }

   private int cmdRoot(CommandContext<CommandSource> context) {
      if (!autoConfigActive) {
         this.info("Usage: .rc auto | add <item> | remove <item> | list | clear", new Object[0]);
         return 1;
      } else {
         autoConfigActive = false;
         pendingContainerPos = null;
         openScreenDelayTicks = -1;
         if (MeteorClient.EVENT_BUS.isListening(RestockConfig.AutoConfigListener.class)) {
            MeteorClient.EVENT_BUS.unsubscribe(RestockConfig.AutoConfigListener.INSTANCE);
         }

         this.info("Exited auto config mode", new Object[0]);
         return 1;
      }
   }

   private int cmdAuto(CommandContext<CommandSource> context) {
      autoConfigActive = true;
      pendingContainerPos = null;
      openScreenDelayTicks = -1;
      this.info("Auto config mode enabled", new Object[0]);
      this.info("§7Open containers to automatically register their contents", new Object[0]);
      this.info("§7Type .rc to exit", new Object[0]);
      if (!MeteorClient.EVENT_BUS.isListening(RestockConfig.AutoConfigListener.class)) {
         MeteorClient.EVENT_BUS.subscribe(RestockConfig.AutoConfigListener.INSTANCE);
      }

      return 1;
   }

   private int cmdAdd(CommandContext<CommandSource> context) {
      Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
      if (item != null && item != Items.AIR) {
         manualConfigActive = true;
         configItem = item;
         this.info("Restocking config mode enabled for §b%s", new Object[]{formatItemName(item)});
         this.info("§7Right-click containers to add them", new Object[0]);
         this.info("§7Esc to exit config mode", new Object[0]);
         if (!MeteorClient.EVENT_BUS.isListening(RestockConfig.RestockingConfigListener.class)) {
            MeteorClient.EVENT_BUS.subscribe(RestockConfig.RestockingConfigListener.INSTANCE);
         }

         return 1;
      } else {
         this.error("Invalid item: %s", new Object[]{item});
         return 1;
      }
   }

   private int cmdClear(CommandContext<CommandSource> context) {
      int totalContainers = containersByItem.values().stream().mapToInt(List::size).sum();
      containersByItem.clear();
      saveConfig();
      this.info("Cleared all restocking containers (%d total)", new Object[]{totalContainers});
      return 1;
   }

   private int cmdList(CommandContext<CommandSource> context) {
      if (containersByItem.isEmpty()) {
         this.info("No restocking containers configured", new Object[0]);
         return 1;
      } else {
         this.info("Restocking containers:", new Object[0]);

         for (Entry<Item, List<BlockPos>> entry : containersByItem.entrySet()) {
            Item item = entry.getKey();
            List<BlockPos> positions = entry.getValue();
            this.info("§b%s§r (%d containers):", new Object[]{formatItemName(item), positions.size()});

            for (BlockPos pos : positions) {
               this.info("  - %s, blocks away", new Object[]{Math.ceil(HighwayUtils.distanceXZ(mc.player.getBlockPos(), pos))});
            }
         }

         return 1;
      }
   }

   private int cmdRemove(CommandContext<CommandSource> context) {
      Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
      if (item != null && item != Items.AIR) {
         List<BlockPos> removed = containersByItem.remove(item);
         if (removed != null && !removed.isEmpty()) {
            this.info("Removed %d containers for %s", new Object[]{removed.size(), formatItemName(item)});
            saveConfig();
         } else {
            this.warning("No containers configured for %s", new Object[]{formatItemName(item)});
         }

         return 1;
      } else {
         this.error("Invalid item: %s", new Object[]{item});
         return 1;
      }
   }

   public static String formatItemName(Item item) {
      return Registries.ITEM.getId(item).toString().replace("minecraft:", "");
   }

   public static List<BlockPos> getContainers(Item item) {
      return containersByItem.getOrDefault(item, new ArrayList<>());
   }

   public static boolean hasContainers(Item item) {
      return containersByItem.containsKey(item) && !containersByItem.get(item).isEmpty();
   }

   private static void saveConfig() {
      ((MusheorSystem)Systems.get(MusheorSystem.class)).save();
   }

   public static NbtCompound toNbt() {
      NbtCompound tag = new NbtCompound();

      for (Entry<Item, List<BlockPos>> entry : containersByItem.entrySet()) {
         String key = Registries.ITEM.getId(entry.getKey()).toString();
         NbtList posList = new NbtList();

         for (BlockPos pos : entry.getValue()) {
            posList.add(NbtLong.of(pos.asLong()));
         }

         tag.put(key, posList);
      }

      return tag;
   }

   public static void fromNbt(NbtCompound tag) {
      containersByItem.clear();

      for (String key : tag.getKeys()) {
         Identifier id = Identifier.tryParse(key);
         if (id != null && Registries.ITEM.containsId(id)) {
            Item item = (Item)Registries.ITEM.get(id);
            NbtList posList = VersionHelper.get().getList(tag, key, 4);
            List<BlockPos> positions = new ArrayList<>();

            for (int i = 0; i < posList.size(); i++) {
               positions.add(BlockPos.fromLong(((NbtLong)posList.get(i)).longValue()));
            }

            containersByItem.put(item, positions);
         }
      }
   }

   private static class AutoConfigListener {
      private static final RestockConfig.AutoConfigListener INSTANCE = new RestockConfig.AutoConfigListener();

      @EventHandler(
         priority = 200
      )
      private void onInteractBlock(InteractBlockEvent event) {
         if (RestockConfig.autoConfigActive) {
            RestockConfig.pendingContainerPos = event.result.getBlockPos();
            RestockConfig.openScreenDelayTicks = -1;
         }
      }

      @EventHandler(
         priority = 200
      )
      private void onOpenScreen(OpenScreenEvent event) {
         if (RestockConfig.autoConfigActive && RestockConfig.pendingContainerPos != null) {
            if (event.screen != null) {
               RestockConfig.openScreenDelayTicks = 4;
            }
         }
      }

      @EventHandler
      private void onTick(Post event) {
         if (RestockConfig.autoConfigActive && RestockConfig.openScreenDelayTicks >= 0) {
            if (RestockConfig.openScreenDelayTicks > 0) {
               RestockConfig.openScreenDelayTicks--;
            } else {
               RestockConfig.openScreenDelayTicks = -1;
               if (RestockConfig.mc.player != null && RestockConfig.mc.player.currentScreenHandler != null && RestockConfig.pendingContainerPos != null) {
                  ScreenHandler handler = RestockConfig.mc.player.currentScreenHandler;
                  Set<Item> foundItems = new HashSet<>();
                  int containerSlotCount = handler.slots.size() - 36;
                  if (containerSlotCount > 0) {
                     for (int i = 0; i < containerSlotCount; i++) {
                        Slot slot = (Slot)handler.slots.get(i);
                        ItemStack stack = slot.getStack();
                        if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
                           foundItems.add(stack.getItem());
                        }
                     }

                     if (foundItems.isEmpty()) {
                        ChatUtils.info(
                           "Container at §b%d %d %d§r is empty, skipping",
                           new Object[]{RestockConfig.pendingContainerPos.getX(), RestockConfig.pendingContainerPos.getY(), RestockConfig.pendingContainerPos.getZ()}
                        );
                     } else {
                        BlockPos pos = RestockConfig.pendingContainerPos;

                        for (Item item : foundItems) {
                           List<BlockPos> positions = RestockConfig.containersByItem.computeIfAbsent(item, k -> new ArrayList<>());
                           if (!positions.contains(pos)) {
                              positions.add(pos);
                           }
                        }

                        RestockConfig.saveConfig();
                        ChatUtils.info("Registered §b%d§r item(s) from container", new Object[]{foundItems.size(), pos.getX(), pos.getY(), pos.getZ()});

                        for (Item itemx : foundItems) {
                           ChatUtils.info("  - §b%s", new Object[]{RestockConfig.formatItemName(itemx)});
                        }
                     }

                     RestockConfig.pendingContainerPos = null;
                  }
               }
            }
         }
      }
   }

   private static class RestockingConfigListener {
      private static final RestockConfig.RestockingConfigListener INSTANCE = new RestockConfig.RestockingConfigListener();

      @EventHandler(
         priority = 200
      )
      private void onInteractBlock(InteractBlockEvent event) {
         if (RestockConfig.manualConfigActive && RestockConfig.configItem != null) {
            BlockPos pos = event.result.getBlockPos();
            RestockConfig.containersByItem.computeIfAbsent(RestockConfig.configItem, k -> new ArrayList<>()).add(pos);
            RestockConfig.saveConfig();
            ChatUtils.info("Added container for §b%s", new Object[]{Registries.ITEM.getId(RestockConfig.configItem).toString()});
            event.cancel();
         }
      }

      @EventHandler(
         priority = 200
      )
      private void onOpenScreen(OpenScreenEvent event) {
         if (RestockConfig.manualConfigActive) {
            if (event.screen != null) {
               this.exitConfigMode();
               event.cancel();
            }
         }
      }

      private void exitConfigMode() {
         RestockConfig.manualConfigActive = false;
         Item item = RestockConfig.configItem;
         RestockConfig.configItem = null;
         int count = RestockConfig.containersByItem.getOrDefault(item, new ArrayList<>()).size();
         ChatUtils.info("Exited restocking config mode. Added §b%d§r containers for §b%s", new Object[]{count, RestockConfig.formatItemName(item)});
      }
   }
}

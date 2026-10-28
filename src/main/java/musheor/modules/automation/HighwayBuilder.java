package musheor.modules.automation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWalk;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWalk.Direction;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.compat.VersionHelper;
import musheor.utils.HighwayLogic;
import musheor.utils.ModuleControl;
import musheor.utils.BlockRenderUtils;
import musheor.utils.HighwayUtils;
import musheor.utils.hud.screen.EchestConfigScreen;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import musheor.utils.BaritoneHelper;
import musheor.utils.HighwayBuilderState;
import musheor.utils.EchestSlotLocks;

public class HighwayBuilder extends Module {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPlacement = this.settings.createGroup("Placement");
   private final SettingGroup sgRestocking = this.settings.createGroup("Restocking");
   private final SettingGroup sgAutoEat = this.settings.createGroup("Auto Eat");
   private final SettingGroup sgNuker = this.settings.createGroup("Nuker");
   private final SettingGroup sgKillAura = this.settings.createGroup("Kill Aura");
   private final SettingGroup sgSafety = this.settings.createGroup("Safety");
   private final SettingGroup sgInventory = this.settings.createGroup("Inventory");
   private final SettingGroup sgMiscellaneous = this.settings.createGroup("Miscellaneous");
   private final Setting<HighwayBuilder.BuildMode> buildMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("build-mode")).description("Pave mode places blocks - Dig mode digs tunnels."))
               .defaultValue(HighwayBuilder.BuildMode.PAVE))
            .build()
      );
   public final Setting<HighwayBuilder.Mode> mode2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode"))
                  .description(
                     "Which mode to run the highwaybuilder in; Auto = fully automatic alignment, width, etc... - Semi = Chosen highway type and other parameters - Manual = full manual control"
                  ))
               .defaultValue(HighwayBuilder.Mode.AUTO))
            .build()
      );
   private final Setting<HighwayBuilder.HighwayType> highwayType = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("highway-type")).description("Cardinal or diagonal highway type."))
                  .defaultValue(HighwayBuilder.HighwayType.CARDINAL))
               .visible(() -> this.mode2.get() != HighwayBuilder.Mode.AUTO))
            .build()
      );
   private final Setting<Integer> pavementWidth = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("pavement-width"))
                     .description("Width of the pavement below the player's feet."))
                  .defaultValue(4))
               .sliderRange(3, 9)
               .visible(() -> this.mode2.get() != HighwayBuilder.Mode.AUTO))
            .build()
      );
   private final Setting<Boolean> autoBounce = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("auto-bounce"))
                     .description("Automatically start bouncing when no blockages or missing blocks can be found ahead"))
                  .defaultValue(true))
               .visible(() -> this.mode2.get() != HighwayBuilder.Mode.MANUAL))
            .build()
      );
   private final Setting<Integer> bounceDistanceCheck = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("bounce-distance-check"))
                     .description("How many blocks to scan ahead and check before allowing the paver to start bouncing"))
                  .defaultValue(24))
               .sliderRange(1, 64)
               .visible(() -> this.mode2.get() != HighwayBuilder.Mode.MANUAL && (Boolean)this.autoBounce.get()))
            .build()
      );
   private final Setting<Block> pavementBlock = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BlockSetting.Builder)((meteordevelopment.meteorclient.settings.BlockSetting.Builder)((meteordevelopment.meteorclient.settings.BlockSetting.Builder)((meteordevelopment.meteorclient.settings.BlockSetting.Builder)new meteordevelopment.meteorclient.settings.BlockSetting.Builder()
                        .name("pavement-block"))
                     .description("Block that is used to build highway pavement with."))
                  .defaultValue(Blocks.OBSIDIAN))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.PAVE))
            .build()
      );
   private final Setting<HighwayBuilder.ScaffoldMode> scaffoldMode = this.sgPlacement
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("scaffold-mode"))
                     .description("What type of scaffolding to use when going over caves and open area's."))
                  .defaultValue(HighwayBuilder.ScaffoldMode.FAST))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.DIG))
            .build()
      );
   private final Setting<Block> scaffoldBlock = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BlockSetting.Builder)((meteordevelopment.meteorclient.settings.BlockSetting.Builder)((meteordevelopment.meteorclient.settings.BlockSetting.Builder)((meteordevelopment.meteorclient.settings.BlockSetting.Builder)new meteordevelopment.meteorclient.settings.BlockSetting.Builder()
                        .name("scaffold-block"))
                     .description("Block that is used to fix the flooring with."))
                  .defaultValue(Blocks.NETHERRACK))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.DIG && this.scaffoldMode.get() != HighwayBuilder.ScaffoldMode.NORMAL))
            .build()
      );
   private final Setting<Boolean> scaffoldLeftRail = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("scaffold-left-rail"))
                     .description("Places the railing on the left side of the player."))
                  .defaultValue(true))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.DIG))
            .build()
      );
   private final Setting<Boolean> scaffoldRightRail = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("scaffold-right-rail"))
                     .description("Places the railing on the right side of the player."))
                  .defaultValue(true))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.DIG))
            .build()
      );
   private final Setting<Boolean> placeRails = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("place-rails"))
                     .description("Places railings on both sides of the highway 1 block above the pavement."))
                  .defaultValue(true))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.PAVE && this.mode2.get() != HighwayBuilder.Mode.AUTO))
            .build()
      );
   private final Setting<Boolean> placeLeftRail = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("place-left-rail"))
                     .description("Places railings on the left side sides of the highway 1 block above the pavement."))
                  .defaultValue(true))
               .visible(
                  () -> this.buildMode.get() == HighwayBuilder.BuildMode.PAVE
                     && (Boolean)this.placeRails.get()
                     && this.mode2.get() != HighwayBuilder.Mode.AUTO
               ))
            .build()
      );
   private final Setting<Boolean> placeRightRail = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("place-right-rail"))
                     .description("Places railings on the right sides of the highway 1 block above the pavement."))
                  .defaultValue(true))
               .visible(
                  () -> this.buildMode.get() == HighwayBuilder.BuildMode.PAVE
                     && (Boolean)this.placeRails.get()
                     && this.mode2.get() != HighwayBuilder.Mode.AUTO
               ))
            .build()
      );
   private final Setting<Boolean> replaceCryingObsidian = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("replace-crying-obsidian"))
                     .description("Mines and replaces crying obsidian if part of the pavement positions."))
                  .defaultValue(false))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.PAVE))
            .build()
      );
   private final Setting<Boolean> fillCeiling = this.sgPlacement
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("fill-ceiling"))
                     .description("Fills the ceiling at Y level 123 with blocks."))
                  .defaultValue(false))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.PAVE))
            .build()
      );
   private final Setting<Boolean> echestFarming = this.sgRestocking
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("echest-farming"))
                     .description("Allow the player to farm obsidian by mining enderchests."))
                  .defaultValue(true))
               .visible(() -> this.buildMode.get() == HighwayBuilder.BuildMode.PAVE && this.pavementBlock.get() == Blocks.OBSIDIAN))
            .build()
      );
   private final Setting<Boolean> enableItemRestocking = this.sgRestocking
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("enable-item-restocking"))
                  .description("Allow the restocking process to grab items from shulkers."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> swapBrokenPickaxes = this.sgRestocking
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("swap-broken-pickaxes"))
                     .description("Swaps broken pickaxes with new ones when restocking these, allowing you to repair them later."))
                  .defaultValue(true))
               .visible(this.enableItemRestocking::get))
            .build()
      );
   private final Setting<Boolean> shulkerRestocking = this.sgRestocking
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("shulker-restocking"))
                  .description("Allow the restocking process to grab shulkers from the player's enderchest."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> materialShulkerRestocking = this.sgRestocking
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("material-shulker-restocking"))
                     .description("Allow the restocking process to grab shulkers with pavement material from the player's enderchest."))
                  .defaultValue(false))
               .visible(this.shulkerRestocking::get))
            .build()
      );
   private final Setting<Boolean> toolShulkerRestocking = this.sgRestocking
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("tool-shulker-restocking"))
                     .description("Allow the restocking process to grab tool shulkers from the player's enderchest."))
                  .defaultValue(false))
               .visible(this.shulkerRestocking::get))
            .build()
      );
   private final Setting<Boolean> toggleAutoEat = this.sgAutoEat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("toggle-auto-eat"))
                  .description("Pauses the current task and automatically eats."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> toggleAutoGap = this.sgAutoEat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("toggle-auto-gap"))
                  .description("Allow the player to eat golden apples when required"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> toggleKekNuker = this.sgNuker
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("toggle-kek-nuker"))
                  .description("Allows the paver to use the nuker module."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> mineAboveRails = this.sgNuker
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("mine-above-rails"))
                  .description("Cleans and removes blocks above rails"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> toggleKillAura = this.sgKillAura
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("toggle-kill-aura"))
                  .description("Automatically attack nearby hostile entities."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> disconnectIfNoMaterials = this.sgSafety
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disconnect-if-no-materials"))
                  .description("Automatically disconnect when materials are low (less than 8 Obsidian/E-Chests)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> toggleSourceFiller = this.sgSafety
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("toggle-source-filler"))
                  .description("Automatically remove lava sources when you can reach them."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> advancedSourceFiller = this.sgSafety
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("advanced-source-filler"))
                     .description("Uses baritone to path to lava sources ahead of the player. Not recommended."))
                  .defaultValue(false))
               .visible(null))
            .build()
      );
   private final Setting<Boolean> toggleAutoReplenish = this.sgInventory
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("toggle-auto-replenish"))
                  .description("Automatically move items from inventory to hotbar when slots are empty."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> toggleInventoryCleaner = this.sgInventory
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("toggle-inventory-cleaner"))
                  .description("Allow the paver to dispose of unwanted items."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> discordRpc = this.sgMiscellaneous
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("discord-rpc"))
                  .description("Send paver stats to discord activity."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> enableFreelook = this.sgMiscellaneous
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("enable-freelook"))
                  .description("Freelook when using the paver."))
               .defaultValue(false))
            .build()
      );
   public static HighwayBuilder INSTANCE;
   private boolean initializing = false;
   private int restockAttemptCount = 0;

   public HighwayBuilder() {
      super(Musheor.AUTOMATION, "HighwayBuilder", "Automated highway builder for 2b2t.");
      INSTANCE = this;
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      ((WButton)list.add(theme.button("Configure E-Chest slots")).widget()).action = () -> this.mc
         .setScreen(new EchestConfigScreen(this.mc.currentScreen));
      return list;
   }

   public NbtCompound toTag() {
      NbtCompound tag = super.toTag();
      tag.putInt("echestSlotBlacklist", EchestSlotLocks.toMask());
      return tag;
   }

   public Module fromTag(NbtCompound tag) {
      Module module = super.fromTag(tag);
      EchestSlotLocks.fromMask(VersionHelper.get().getInt(tag, "echestSlotBlacklist", 0));
      return module;
   }

   public void onActivate() {
      this.initializing = true;
      this.restockAttemptCount = 0;
      HighwayBuilderState.getInstance().setTickCount(0);
   }

   private void initializeBuilder() {
      HighwayBuilderState state = HighwayBuilderState.getInstance();
      state.reset();
      boolean noPavementBlockPresent = this.pavementBlock.get() == Blocks.OBSIDIAN && InventoryManager.countItem(getPavementBlock().asItem()) < 1;
      boolean noPickaxePresent = InventoryManager.countPickaxes(false) < 1;
      if (this.buildMode.get() != HighwayBuilder.BuildMode.PAVE || !noPavementBlockPresent && !noPickaxePresent) {
         boolean noNonSilkPickaxePresent = InventoryManager.countPickaxes(true) < 1;
         if (this.buildMode.get() == HighwayBuilder.BuildMode.DIG) {
            if (noNonSilkPickaxePresent) {
               this.error("No pickaxe found in inventory...!", new Object[0]);
            }

            ModuleControl.toggleHighwayBuilder();
         } else {
            this.initializing = false;
            state.setOriginPos(this.mc.player.getBlockPos());
            state.setTickCount(0);
            InventoryManager.resetRestock(false);
            if (!this.captureAlignment()) {
               ModuleControl.toggleHighwayBuilder();
            } else {
               ModuleControl.setModuleSetting(KekNuker.class, "nuker-mode", KekNuker.NukerMode.BOUNCE);
               ModuleControl.setModuleSetting(KekNuker.class, "range", 5.5);
               this.enableCompanionModules();
               state.getPathHistory().clear();
               state.getPathHistory().add(this.mc.player.getBlockPos());
               state.getRecentPlacementAttempts().clear();
               ModuleControl.setModuleSetting(AutoWalk.class, "mode", meteordevelopment.meteorclient.systems.modules.movement.AutoWalk.Mode.Simple);
               ModuleControl.setModuleSetting(AutoWalk.class, "simple-direction", Direction.Forwards);
            }
         }
      } else {
         if (noPavementBlockPresent) {
            this.error("No pavement material found in inventory...", new Object[0]);
         }

         if (noPickaxePresent) {
            this.error("No non-silk touch pickaxe found in inventory...!", new Object[0]);
         }

         ModuleControl.toggleHighwayBuilder();
      }
   }

   public void onDeactivate() {
      this.initializing = false;
      HighwayLogic.resetBounceState();
      ModuleControl.setAutoWalk(false);
      BaritoneHelper.cancel();
      disableCompanionModules();
      musheor.utils.DiscordRPC.stop();
      HighwayBuilderState.getInstance().reset();
      this.info("Deactivated.", new Object[0]);
   }

   @EventHandler
   public void onTick(Pre event) {
      HighwayBuilderState state = HighwayBuilderState.getInstance();
      if (this.mc.player == null || this.mc.world == null || this.mc.interactionManager == null) {
         state.setTickCount(0);
      } else if (this.mc.player.isSpectator()) {
         this.info("Player is likely in queue, waiting to proceed with highwaybuilder state checks...", new Object[0]);
         state.setTickCount(0);
      } else if (this.initializing) {
         this.initializeBuilder();
      } else {
         state.incrementTickCount();
         HighwayUtils.pruneRecentPlacements(state.getTickCount(), getPavementBlock());
         if (state.getLavaTarget() != null) {
            ModuleControl.setAutoWalk(false);
         }

         state.incrementDistanceTickCounter();
         if (state.getDistanceTickCounter() >= 400) {
            state.setDistanceTickCounter(0);
            if ((Boolean)this.discordRpc.get()) {
               musheor.utils.DiscordRPC.update();
            } else {
               musheor.utils.DiscordRPC.stop();
            }
         }

         Module BetterEChestFarmer = Modules.get().get(EchestFarmer.class);
         boolean isGapEating = ((AutoGap)Modules.get().get(AutoGap.class)).isEating();
         if (!isEating() && !isGapEating && !isEchestFarmerActive() && !isKillAuraAttacking() && !ModuleControl.isGatherItemActive() && !state.isEchestFarmBusy()) {
            int enderChestCount = this.pavementBlock.get() == Blocks.OBSIDIAN ? InventoryManager.countItem(Items.ENDER_CHEST) : 64;
            int obsidianCount = InventoryManager.countItem(((Block)this.pavementBlock.get()).asItem());
            state.setPickaxeCount(InventoryManager.countPickaxes(false));
            state.setSilkPickaxeCount(InventoryManager.countPickaxes(true));
            if (this.buildMode.get() == HighwayBuilder.BuildMode.PAVE) {
               state.setNeedsRestock(enderChestCount <= 8 && obsidianCount <= 8 || state.getPickaxeCount() < 1 || !hasUsableFood());
            }

            if (this.buildMode.get() == HighwayBuilder.BuildMode.DIG) {
               state.setNeedsRestock(state.getSilkPickaxeCount() < 1 || !hasUsableFood());
            }

            if (state.isPostRestocking()) {
               InventoryManager.tickPostRestock();
            } else if (state.isRestockingShulkerFromEchest()) {
               InventoryManager.restockShulkersFromEchest(state.getRestockType(), InventoryManager.restockAmount);
               MusheorSystem.debug("Restocking shulker from echest %s; Amount: %s", state.getRestockType(), InventoryManager.restockAmount);
            } else if (state.isRestockingFromInventory()) {
               InventoryManager.restockFromShulker(state.getRestockType(), InventoryManager.restockAmount);
               MusheorSystem.debug("Restocking %s; Amount: %s", state.getRestockType(), InventoryManager.restockAmount);
            } else if (state.isEchestFarmActive() && !BetterEChestFarmer.isActive() && (Boolean)this.echestFarming.get()) {
               HighwayLogic.finishEchestFarm();
               MusheorSystem.debug("Running post-echest farmer...");
            } else if (this.shouldStartEchestFarm(obsidianCount, enderChestCount)
               && (Boolean)this.echestFarming.get()
               && this.buildMode.get() == HighwayBuilder.BuildMode.PAVE) {
               HighwayLogic.runEchestFarm();
               MusheorSystem.debug("Running echest farmer...");
            } else if (!state.needsRestock()) {
               this.restockAttemptCount = 0;
               if (this.buildMode.get() == HighwayBuilder.BuildMode.PAVE) {
                  if (this.mode2.get() == HighwayBuilder.Mode.AUTO) {
                     HighwayLogic.tickPaveAuto();
                  } else {
                     switch ((HighwayBuilder.HighwayType)this.highwayType.get()) {
                        case CARDINAL:
                           HighwayLogic.tickPaveCardinal();
                           break;
                        case DIAGONAL:
                           HighwayLogic.tickPaveDiagonal();
                     }
                  }
               }

               if (this.buildMode.get() == HighwayBuilder.BuildMode.DIG) {
                  switch ((HighwayBuilder.HighwayType)this.highwayType.get()) {
                     case CARDINAL:
                        HighwayLogic.tickDigCardinal();
                        break;
                     case DIAGONAL:
                        HighwayLogic.tickDigDiagonal();
                  }
               }
            } else if (((Boolean)this.enableItemRestocking.get() || (Boolean)this.shulkerRestocking.get()) && this.mode2.get() != HighwayBuilder.Mode.MANUAL) {
               if (!InventoryManager.isRestocking && !InventoryManager.isPostRestocking) {
                  if (this.restockAttemptCount >= 6) {
                     this.handleRestockFailure(enderChestCount, obsidianCount);
                  } else {
                     InventoryManager.restockPos = this.mc.player.getBlockPos();
                     ModuleControl.setAutoWalk(false);
                     InventoryManager.checkAndStartRestock();
                     if (!InventoryManager.isRestocking) {
                        this.handleRestockFailure(enderChestCount, obsidianCount);
                     } else {
                        this.restockAttemptCount++;
                     }
                  }
               }
            } else {
               this.handleRestockFailure(enderChestCount, obsidianCount);
            }
         } else {
            ModuleControl.setAutoWalk(false);
         }
      }
   }

   private void handleRestockFailure(int enderChestCount, int obsidianCount) {
      HighwayBuilderState state = HighwayBuilderState.getInstance();
      String reason = "Low on materials and restocking failed or is unavailable. Missing: ";
      if (enderChestCount < 8) {
         reason = reason + "Enderchest count: " + enderChestCount + "/8 ";
      }

      if (obsidianCount < 1) {
         reason = reason + "Obsidian count: " + obsidianCount + "/8 ";
      }

      if (!hasUsableFood()) {
         reason = reason + "Food - golden apples blacklisted in auto-eat module? ";
      }

      if (getBuildMode() == HighwayBuilder.BuildMode.PAVE && state.getSilkPickaxeCount() < 1) {
         reason = reason + "Non SilkTouch pickaxe.";
      }

      if (getBuildMode() == HighwayBuilder.BuildMode.DIG && state.getPickaxeCount() < 1) {
         reason = reason + "Pickaxe.";
      }

      this.restockAttemptCount = 0;
      ModuleControl.toggleHighwayBuilder();
      if ((Boolean)this.disconnectIfNoMaterials.get()) {
         ModuleControl.disconnect(reason);
      } else {
         this.error(reason, new Object[0]);
      }
   }

   private boolean shouldStartEchestFarm(int obsidianCount, int echestCount) {
      HighwayBuilderState state = HighwayBuilderState.getInstance();
      if (!state.isRestockingFromInventory() && !state.isRestockingShulkerFromEchest() && !state.isPostRestocking() && !ModuleControl.isGatherItemActive()) {
         return this.pavementBlock.get() != Blocks.OBSIDIAN ? false : echestCount > 8 && InventoryManager.countEmptySlots() > 0 && obsidianCount <= 8;
      } else {
         return false;
      }
   }

   private boolean captureAlignment() {
      assert this.mc.player != null;

      HighwayBuilderState state = HighwayBuilderState.getInstance();
      state.setDirection(HighwayUtils.directionFromYaw());
      state.setPlacementType((MusheorSystem.PlacementType)MusheorSystem.Manager.placementType.get());
      state.setStartX(this.mc.player.getBlockX());
      state.setStartZ(this.mc.player.getBlockZ());
      state.setAlignX(VersionHelper.get().getPlayerPos().getX());
      state.setAlignZ(VersionHelper.get().getPlayerPos().getZ());
      state.setRailX(Integer.valueOf(this.mc.player.getBlockX()));
      state.setRailY(Integer.valueOf(this.mc.player.getBlockY()));
      state.setRailZ(Integer.valueOf(this.mc.player.getBlockZ()));
      if (this.mode2.get() == HighwayBuilder.Mode.AUTO) {
         musheor.utils.HighwayDetector.DetectedHighway detected = musheor.utils.HighwayDetector.detect(
            this.mc.player.getBlockX(), this.mc.player.getBlockZ(), this.mc.player.getBlockY(), state.getDirection()
         );
         if (detected == null) {
            this.error("Auto mode could not detect a highway. Stand on a highway or use Semi mode.", new Object[0]);
            return false;
         }

         state.setDetectedHighway(detected);
         state.setStartX(detected.startX);
         state.setStartZ(detected.startZ);
         state.setAlignX(detected.alignX);
         state.setAlignZ(detected.alignZ);
         state.setRailX(Integer.valueOf(detected.startX));
         state.setRailZ(Integer.valueOf(detected.startZ));
         this.info("Detected: §b" + detected.label + "§r | Width: §e" + detected.width + "§r | Dir: §a" + detected.direction, new Object[0]);
      }

      return true;
   }

   private void enableCompanionModules() {
      Module KekNuker = Modules.get().get(KekNuker.class);
      Module AutoEat = Modules.get().get(AutoEat.class);
      Module AutoGap = Modules.get().get(AutoGap.class);
      Module KillAura = Modules.get().get(KillAura.class);
      Module SourceRemover = Modules.get().get(SourceRemover.class);
      Module InventoryCleaner = Modules.get().get(InventoryCleaner.class);
      Module HotbarReplenish = Modules.get().get(HotbarReplenish.class);
      Module FreeLook = Modules.get().get(FreeLook.class);
      if ((Boolean)this.toggleKekNuker.get() && !KekNuker.isActive()) {
         KekNuker.toggle();
      }

      if ((Boolean)this.toggleAutoEat.get() && !AutoEat.isActive()) {
         AutoEat.toggle();
      }

      if ((Boolean)this.toggleAutoGap.get() && !AutoGap.isActive()) {
         AutoGap.toggle();
      }

      if ((Boolean)this.toggleKillAura.get() && !KillAura.isActive()) {
         KillAura.toggle();
      }

      if ((Boolean)this.toggleInventoryCleaner.get() && !InventoryCleaner.isActive()) {
         InventoryCleaner.toggle();
      }

      if ((Boolean)this.enableFreelook.get() && !FreeLook.isActive()) {
         ModuleControl.setupFreeLookCamera();
      }

      if ((Boolean)this.toggleAutoReplenish.get() && !HotbarReplenish.isActive()) {
         HotbarReplenish.toggle();
      }

      if ((Boolean)this.discordRpc.get()) {
         musheor.utils.DiscordRPC.start();
      }

      if ((Boolean)this.toggleSourceFiller.get() && !SourceRemover.isActive()) {
         SourceRemover.toggle();
      }
   }

   public static void disableCompanionModules() {
      Module[] alwaysDisableModules = new Module[]{
         Modules.get().get(EchestFarmer.class),
         Modules.get().get(KekNuker.class),
         Modules.get().get(AutoWalk.class),
         Modules.get().get(HotbarReplenish.class),
         Modules.get().get(FreeLook.class),
         Modules.get().get(SourceRemover.class),
         Modules.get().get(InventoryCleaner.class),
         Modules.get().get(KekBounce.class)
      };

      for (Module module : alwaysDisableModules) {
         if (module.isActive()) {
            module.toggle();
         }
      }

      Module KillAura = Modules.get().get(KillAura.class);
      Module AutoEat = Modules.get().get(AutoEat.class);
      Module AutoGap = Modules.get().get(AutoGap.class);
      if ((Boolean)INSTANCE.toggleKillAura.get() && KillAura.isActive()) {
         KillAura.toggle();
      }

      if ((Boolean)INSTANCE.toggleAutoEat.get() && AutoEat.isActive()) {
         AutoEat.toggle();
      }

      if ((Boolean)INSTANCE.toggleAutoGap.get() && AutoGap.isActive()) {
         AutoGap.toggle();
      }
   }

   public static boolean hasUsableFood() {
      List<Item> blacklistedFood = (List<Item>)((AutoEat)Modules.get().get(AutoEat.class)).blacklist.get();

      for (int i = 0; i < Objects.requireNonNull(MinecraftClient.getInstance().player).getInventory().main.size(); i++) {
         ItemStack stack = MinecraftClient.getInstance().player.getInventory().getStack(i);
         if (stack.getComponents().contains(DataComponentTypes.FOOD)
            && !(stack.getComponents() instanceof ShulkerBoxBlock)
            && !blacklistedFood.contains(stack.getItem())) {
            return true;
         }

         if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE && !blacklistedFood.contains(stack.getItem())) {
            return true;
         }
      }

      return false;
   }

   public static void onBlockBroken(BlockState s) {
      HighwayBuilderState state = HighwayBuilderState.getInstance();
      if (s.getBlock() == Blocks.OBSIDIAN) {
         state.incrementObsidianBroken();
      }

      if (s.getBlock() == Blocks.NETHERRACK) {
         state.incrementNetherrackBroken();
      }

      if (s.getBlock() == Blocks.ENDER_CHEST) {
         state.incrementEnderChestBroken();
      }
   }

   public static boolean isEating() {
      return ((AutoEat)Modules.get().get(AutoEat.class)).eating || ((AutoGap)Modules.get().get(AutoGap.class)).isEating();
   }

   public static boolean isKillAuraAttacking() {
      return ((KillAura)Modules.get().get(KillAura.class)).attacking;
   }

   public static HighwayUtils.HighwayDirection getHighwayDirection() {
      return HighwayBuilderState.getInstance().getDirection();
   }

   public static boolean isEchestFarmerActive() {
      return EchestFarmer.INSTANCE.isActive();
   }

   public static HighwayBuilder.BuildMode getBuildMode() {
      return (HighwayBuilder.BuildMode)INSTANCE.buildMode.get();
   }

   public static HighwayBuilder.Mode getMode() {
      return (HighwayBuilder.Mode)INSTANCE.mode2.get();
   }

   public static HighwayBuilder.HighwayType getHighwayType() {
      if (INSTANCE.mode2.get() == HighwayBuilder.Mode.AUTO) {
         musheor.utils.HighwayDetector.DetectedHighway d = HighwayBuilderState.getInstance().getDetectedHighway();
         if (d != null) {
            return d.type;
         }
      }

      return (HighwayBuilder.HighwayType)INSTANCE.highwayType.get();
   }

   public static int getPavementWidth() {
      if (INSTANCE.mode2.get() == HighwayBuilder.Mode.AUTO) {
         musheor.utils.HighwayDetector.DetectedHighway d = HighwayBuilderState.getInstance().getDetectedHighway();
         if (d != null) {
            return d.width;
         }
      }

      return (Integer)INSTANCE.pavementWidth.get();
   }

   public static boolean isMineAboveRails() {
      return (Boolean)INSTANCE.mineAboveRails.get();
   }

   public static boolean isReplaceCryingObsidian() {
      return (Boolean)INSTANCE.replaceCryingObsidian.get();
   }

   public static Block getPavementBlock() {
      return (Block)INSTANCE.pavementBlock.get();
   }

   public static boolean isPlaceRails() {
      return (Boolean)INSTANCE.placeRails.get();
   }

   public static boolean isPlaceLeftRail() {
      return (Boolean)INSTANCE.placeLeftRail.get();
   }

   public static boolean isPlaceRightRail() {
      return (Boolean)INSTANCE.placeRightRail.get();
   }

   public static Block getScaffoldBlock() {
      return (Block)INSTANCE.scaffoldBlock.get();
   }

   public static HighwayBuilder.ScaffoldMode getScaffoldMode() {
      return (HighwayBuilder.ScaffoldMode)INSTANCE.scaffoldMode.get();
   }

   public static boolean isScaffoldLeftRail() {
      return (Boolean)INSTANCE.scaffoldLeftRail.get();
   }

   public static boolean isScaffoldRightRail() {
      return (Boolean)INSTANCE.scaffoldRightRail.get();
   }

   public static boolean isAdvancedSourceFiller() {
      return (Boolean)INSTANCE.advancedSourceFiller.get();
   }

   public static boolean isFillCeiling() {
      return (Boolean)INSTANCE.fillCeiling.get();
   }

   public static boolean isItemRestockingEnabled() {
      return (Boolean)INSTANCE.enableItemRestocking.get();
   }

   public static boolean isSwapBrokenPickaxes() {
      return (Boolean)INSTANCE.swapBrokenPickaxes.get();
   }

   public static boolean isShulkerRestocking() {
      return (Boolean)INSTANCE.shulkerRestocking.get();
   }

   public static boolean isMaterialShulkerRestocking() {
      return (Boolean)INSTANCE.materialShulkerRestocking.get();
   }

   public static boolean isToolShulkerRestocking() {
      return (Boolean)INSTANCE.toolShulkerRestocking.get();
   }

   public static boolean shouldAutoBounce() {
      return (Boolean)INSTANCE.autoBounce.get() && INSTANCE.buildMode.get() == HighwayBuilder.BuildMode.PAVE;
   }

   public static int getBounceDistanceCheck() {
      return (Integer)INSTANCE.bounceDistanceCheck.get();
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      HighwayBuilderState state = HighwayBuilderState.getInstance();

      assert this.mc.player != null;

      if (state.getDirection() != null) {
         if (state.getRailX() != null && state.getRailY() != null && state.getRailZ() != null) {
            if (state.getAlignX() != null && state.getAlignZ() != null) {
               if ((Boolean)MusheorSystem.Manager.placeRender.get()) {
                  if (this.buildMode.get() == HighwayBuilder.BuildMode.PAVE) {
                     musheor.utils.HighwayDetector.DetectedHighway detected = musheor.utils.HighwayDetector.detect(
                        this.mc.player.getBlockX(), this.mc.player.getBlockZ(), this.mc.player.getBlockY(), getHighwayDirection()
                     );
                     int railY = state.getRailY();
                     List<BlockPos> cardinalPositions = new ArrayList<>();

                     for (BlockPos pos : musheor.utils.HighwayPositions.getCardinalFloor(3, 2, isPlaceLeftRail(), isPlaceRightRail())) {
                        if ((detected == null || pos.getY() != railY || !musheor.utils.HighwayDetector.isCrossingHighway(pos, detected)) && BlockUtils.canPlace(pos, true)) {
                           cardinalPositions.add(pos);
                        }
                     }

                     List<BlockPos> diagonalPositions = new ArrayList<>();

                     for (BlockPos posx : musheor.utils.HighwayPositions.getDiagonalFloor(2, 2, isPlaceLeftRail(), isPlaceRightRail())) {
                        if ((detected == null || posx.getY() != railY || !musheor.utils.HighwayDetector.isCrossingHighway(posx, detected)) && BlockUtils.canPlace(posx, true)) {
                           diagonalPositions.add(posx);
                        }
                     }

                     if (getHighwayType() == HighwayBuilder.HighwayType.CARDINAL) {
                        BlockRenderUtils.renderPositions(event, cardinalPositions, getPavementBlock());
                     }

                     if (getHighwayType() == HighwayBuilder.HighwayType.DIAGONAL) {
                        BlockRenderUtils.renderPositions(event, diagonalPositions, getPavementBlock());
                     }
                  }
               }
            }
         }
      }
   }

   public static enum BuildMode {
      PAVE,
      DIG;}

   public static enum HighwayType {
      CARDINAL,
      DIAGONAL;}

   public static enum Mode {
      AUTO,
      SEMI,
      MANUAL;}

   public static enum ScaffoldMode {
      NORMAL,
      FAST,
      NONE;}
}

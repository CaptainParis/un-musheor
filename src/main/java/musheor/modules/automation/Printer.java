/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  baritone.api.BaritoneAPI
 *  baritone.api.IBaritone
 *  baritone.api.pathing.goals.Goal
 *  baritone.api.pathing.goals.GoalBlock
 *  baritone.api.pathing.goals.GoalNear
 *  baritone.api.selection.ISelection
 *  baritone.api.selection.ISelectionManager
 *  baritone.api.utils.BetterBlockPos
 *  it.unimi.dsi.fastutil.Pair
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.events.world.TickEvent$Pre
 *  meteordevelopment.meteorclient.gui.GuiTheme
 *  meteordevelopment.meteorclient.gui.widgets.WWidget
 *  meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList
 *  meteordevelopment.meteorclient.gui.widgets.pressable.WButton
 *  meteordevelopment.meteorclient.settings.BlockListSetting$Builder
 *  meteordevelopment.meteorclient.settings.BlockPosSetting$Builder
 *  meteordevelopment.meteorclient.settings.BlockSetting$Builder
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.StringSetting$Builder
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.systems.modules.combat.AutoTotem
 *  meteordevelopment.meteorclient.systems.modules.combat.KillAura
 *  meteordevelopment.meteorclient.systems.modules.player.AutoEat
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  meteordevelopment.orbit.EventHandler
 *  musheor.commands.RestockConfig
 *  musheor.compat.LitematicaHelper
 *  musheor.compat.VersionHelper
 *  musheor.modules.automation.Refill
 *  musheor.Musheor
 *  musheor.utils.ModuleControl
 *  musheor.utils.BlockRenderUtils
 *  musheor.utils.manager.RotationManager
 *  musheor.utils.manager.InventoryManager
 *  musheor.utils.HighwayUtils
 *  musheor.utils.system.MusheorSystem
 *  musheor.utils.system.MusheorSystem$PlacementType
 *  net.minecraft.block.AirBlock
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockState
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.FallingBlock
 *  net.minecraft.block.LanternBlock
 *  net.minecraft.block.RedstoneWireBlock
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemConvertible
 *  net.minecraft.item.ItemPlacementContext
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.registry.Registries
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.slot.SlotActionType
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.state.property.EnumProperty
 *  net.minecraft.state.property.Properties
 *  net.minecraft.state.property.Property
 *  net.minecraft.util.Hand
 *  net.minecraft.util.hit.BlockHitResult
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.ChunkPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Direction$Axis
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.shape.VoxelShape
 *  net.minecraft.world.BlockView
 *  net.minecraft.world.World
 *  musheor.utils.BaritoneHelper
 *  musheor.utils.HighwayBuilderState
 *  musheor.utils.BlockStateCycler
 *  musheor.utils.BlockPlacer
 *  musheor.utils.BlockPlacer$PlacementBatch
 *  musheor.utils.PacketBudget
 */
package musheor.modules.automation;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BlockSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.commands.RestockConfig;
import musheor.compat.LitematicaHelper;
import musheor.compat.VersionHelper;
import musheor.modules.automation.KekNuker;
import musheor.modules.automation.Refill;
import musheor.Musheor;
import musheor.utils.BlockRenderUtils;
import musheor.utils.manager.RotationManager;
import musheor.utils.manager.InventoryManager;
import musheor.utils.HighwayUtils;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import musheor.utils.BaritoneHelper;
import musheor.utils.HighwayBuilderState;
import musheor.utils.BlockStateCycler;
import musheor.utils.BlockPlacer;

public class Printer
extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgPlacement;
    private final SettingGroup sgRotation;
    private final SettingGroup sgPathfinding;
    private final SettingGroup sgRender;
    private final SettingGroup sgLitematica;
    private final MinecraftClient client;
    private final Long2ObjectMap<Map<BlockPos, BlockState>> chunkTargets;
    private final List<Pair<BlockPos, BlockState>> placeQueue;
    private final List<Pair<BlockPos, BlockState>> renderQueue;
    private int tickCounter;
    private int containerActionTicks;
    private int interactCooldown;
    private int swapDelayLeft;
    private int restockedCount;
    private boolean needsInventorySync;
    private int desyncTicks;
    private final Set<BlockPos> blacklistedContainers;
    private Item heldPlaceItem;
    private final List<Item> restockPriority;
    private boolean showSchematicPos;
    private int rotationSettleLeft;
    private PlacementStrategy pendingStrategy;
    private Map<Block, Integer> remainingMaterials;
    private int materialRefreshTicks;
    private int spiralRadius;
    private int spiralIndex;
    private BlockPos pathTarget;
    private BlockPos pendingPathTarget;
    private boolean pathingToRestock;
    private boolean waitingAfterContainerPlace;
    private final Map<BlockPos, Integer> stateClickCooldown;
    private final Map<BlockPos, Integer> lastClicksNeeded;
    private final Map<BlockPos, Integer> stateStuckRounds;
    private final Set<BlockPos> abandonedStatePositions;
    private int stateClickDelayLeft;
    private static final Set<Property<?>> ORIENTATION_PROPERTIES = new HashSet<Property<?>>(Arrays.asList(Properties.FACING, Properties.HORIZONTAL_FACING, Properties.BLOCK_HALF, Properties.SLAB_TYPE, Properties.AXIS, Properties.HOPPER_FACING));
    private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final float[] TEST_PITCHES = new float[]{-90.0f, 0.0f, 90.0f};
    private static final double[] HIT_Y_FRACTIONS = new double[]{0.25, 0.75};
    private final Map<BlockState, PlacementStrategy> strategyCache;
    private static Method getPlacementStateMethod = null;
    private final Setting<SelectionType> selectionType;
    private final Setting<Boolean> pauseWhenEating;
    private final Setting<Boolean> pauseOnAura;
    private final Setting<Boolean> fixDesync;
    private final Setting<LayerType> layerType;
    private final Setting<List<Block>> ignoredBlocks;
    private final Setting<Block> baritoneBlock;
    private final Setting<Boolean> onlyAir;
    private final Setting<Boolean> gravityCheck;
    private final Setting<Boolean> setCorrectStates;
    private final Setting<Integer> setStateDelay;
    private final Setting<List<Block>> ignoredBlockStates;
    private final Setting<RotationType> rotationType;
    private final Setting<Integer> rotationDelay;
    private final Setting<Boolean> autoPathfinding;
    private final Setting<Integer> pathfindChunkRange;
    private final Setting<RestockType> autoRestock;
    private final Setting<Boolean> playSound;
    private final Setting<Boolean> debugMode;
    private int debugTickCounter;
    private final Setting<Boolean> render;
    private final Setting<Boolean> renderPlaceableOnly;
    private final Setting<Integer> renderRadius;
    private final Setting<Integer> renderMaxResults;
    private final Setting<String> schematicName;
    private final Setting<BlockPos> schematicPos;

    private static BlockState getPlacementState(Block block, ItemPlacementContext ctx) {
        try {
            if (getPlacementStateMethod == null) {
                for (Method m : Block.class.getMethods()) {
                    if (m.getParameterCount() != 1 || !BlockState.class.isAssignableFrom(m.getReturnType()) || !m.getParameterTypes()[0].isAssignableFrom(ItemPlacementContext.class)) continue;
                    getPlacementStateMethod = m;
                    break;
                }
                if (getPlacementStateMethod == null) {
                    block3: for (Class clazz = Block.class; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.getParameterCount() != 1 || !BlockState.class.isAssignableFrom(m.getReturnType()) || !m.getParameterTypes()[0].isAssignableFrom(ItemPlacementContext.class)) continue;
                            m.setAccessible(true);
                            getPlacementStateMethod = m;
                            break block3;
                        }
                    }
                }
            }
            return getPlacementStateMethod != null ? (BlockState)getPlacementStateMethod.invoke((Object)block, ctx) : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    public Printer() {
        super(Musheor.AUTOMATION, "kek-printer", "Places blocks from schematics or fills baritone selections pretty damn fast");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgPlacement = this.settings.createGroup("Placement");
        this.sgRotation = this.settings.createGroup("Rotation");
        this.sgPathfinding = this.settings.createGroup("Pathfinding/Restocking");
        this.sgRender = this.settings.createGroup("Render");
        this.sgLitematica = this.settings.createGroup("Litematica");
        this.client = MinecraftClient.getInstance();
        this.chunkTargets = new Long2ObjectOpenHashMap();
        this.placeQueue = new ArrayList<Pair<BlockPos, BlockState>>();
        this.renderQueue = new ArrayList<Pair<BlockPos, BlockState>>();
        this.containerActionTicks = 0;
        this.interactCooldown = 0;
        this.swapDelayLeft = 0;
        this.restockedCount = 0;
        this.blacklistedContainers = new HashSet<BlockPos>();
        this.heldPlaceItem = null;
        this.restockPriority = new ArrayList<Item>();
        this.showSchematicPos = false;
        this.rotationSettleLeft = 0;
        this.pendingStrategy = null;
        this.remainingMaterials = new HashMap<Block, Integer>();
        this.materialRefreshTicks = 0;
        this.spiralRadius = -1;
        this.spiralIndex = 0;
        this.pathTarget = null;
        this.pendingPathTarget = null;
        this.pathingToRestock = false;
        this.waitingAfterContainerPlace = false;
        this.stateClickCooldown = new HashMap<BlockPos, Integer>();
        this.lastClicksNeeded = new HashMap<BlockPos, Integer>();
        this.stateStuckRounds = new HashMap<BlockPos, Integer>();
        this.abandonedStatePositions = new HashSet<BlockPos>();
        this.stateClickDelayLeft = 0;
        this.strategyCache = new HashMap<BlockState, PlacementStrategy>();
        this.selectionType = this.sgGeneral.add(((EnumSetting.Builder<SelectionType>) ((EnumSetting.Builder<SelectionType>) ((EnumSetting.Builder<SelectionType>) new EnumSetting.Builder<SelectionType>()
            .name("selection-type"))
            .description("Get selection from litematica or baritone"))
            .defaultValue(SelectionType.LITEMATIC))
            .build());
        this.pauseWhenEating = this.sgGeneral.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("pause-when-eating"))
            .description("Pauses the printing process when the player eats (only when using meteor's auto-eat)"))
            .defaultValue(true))
            .build());
        this.pauseOnAura = this.sgGeneral.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("pause-on-aura"))
            .description("Pauses the printing process when the player is killing mobs using killaura"))
            .defaultValue(true))
            .build());
        this.fixDesync = this.sgGeneral.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("fix-desync"))
            .description("Attempts to fix your offhand / inventory desyncs from getting you stuck"))
            .defaultValue(false))
            .build());
        this.layerType = this.sgPlacement.add(((EnumSetting.Builder<LayerType>) ((EnumSetting.Builder<LayerType>) ((EnumSetting.Builder<LayerType>) new EnumSetting.Builder<LayerType>()
            .name("layer-type"))
            .description("Choose how layering is handled when printing"))
            .defaultValue(LayerType.ALL))
            .build());
        this.ignoredBlocks = this.sgPlacement.add(((BlockListSetting.Builder) ((BlockListSetting.Builder) new BlockListSetting.Builder()
            .name("ignored-block-list"))
            .description("Avoids placing blocks configured in this list"))
            .defaultValue(new Block[0])
            .build());
        this.baritoneBlock = this.sgPlacement.add(((BlockSetting.Builder) ((BlockSetting.Builder) ((BlockSetting.Builder) ((BlockSetting.Builder) new BlockSetting.Builder()
            .name("block"))
            .description("What block to place (used for Baritone mode)."))
            .defaultValue(Blocks.OBSIDIAN))
            .visible(() -> this.selectionType.get() == SelectionType.CUBOID))
            .build());
        this.onlyAir = this.sgPlacement.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("only-air"))
            .description("Places blocks only when the desired location is an air block (meaning it doesn't replace non-solid blocks)"))
            .defaultValue(true))
            .build());
        this.gravityCheck = this.sgPlacement.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("gravity-check"))
            .description("Skip gravity-affected blocks (sand, gravel, etc.) if there is no solid block below them to prevent unwanted falling."))
            .defaultValue(true))
            .build());
        this.setCorrectStates = this.sgPlacement.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("set-correct-states"))
            .description("Sets the correct states of a block by interacting with it (e.g: opening trapdoors, setting repeater ticks...)"))
            .defaultValue(true))
            .build());
        this.setStateDelay = this.sgPlacement.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder()
            .name("set-state-delay"))
            .description("Ticks to wait between interacting with a block to achieve the desired state."))
            .defaultValue(0))
            .sliderRange(0, 10)
            .visible(() -> this.setCorrectStates.get()))
            .build());
        this.ignoredBlockStates = this.sgPlacement.add(((BlockListSetting.Builder) ((BlockListSetting.Builder) ((BlockListSetting.Builder) new BlockListSetting.Builder()
            .name("ignored-blockstates-list"))
            .description("Which blocks to ignore or avoid setting the correct state for (e.g: note blocks, doors...)"))
            .defaultValue(new Block[0])
            .visible(() -> this.setCorrectStates.get()))
            .build());
        this.rotationType = this.sgRotation.add(((EnumSetting.Builder<RotationType>) ((EnumSetting.Builder<RotationType>) ((EnumSetting.Builder<RotationType>) new EnumSetting.Builder<RotationType>()
            .name("rotation-type"))
            .description("Automatically rotate the camera to the correct direction when placing directional blocks."))
            .defaultValue(RotationType.PACKET))
            .build());
        this.rotationDelay = this.sgRotation.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder()
            .name("rotation-delay"))
            .description("Ticks to wait after rotating before attempting placement (allows rotation to settle server-side)."))
            .defaultValue(0))
            .min(0)
            .sliderMax(10)
            .visible(() -> this.rotationType.get() == RotationType.PACKET))
            .build());
        this.autoPathfinding = this.sgPathfinding.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("auto-pathfinding"))
            .description("Basically baritone building but better..."))
            .defaultValue(true))
            .build());
        this.pathfindChunkRange = this.sgPathfinding.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder()
            .name("pathfind-chunk-range"))
            .description("The maximum distance in chunks to search for a block to pathfind to and place"))
            .defaultValue(8))
            .visible(() -> this.autoPathfinding.get()))
            .build());
        this.autoRestock = this.sgPathfinding.add(((EnumSetting.Builder<RestockType>) ((EnumSetting.Builder<RestockType>) ((EnumSetting.Builder<RestockType>) ((EnumSetting.Builder<RestockType>) new EnumSetting.Builder<RestockType>()
            .name("auto-restock"))
            .description("Automatically restock more materials from either configured containers or from shulkers you have on you"))
            .defaultValue(RestockType.NONE))
            .visible(() -> this.autoPathfinding.get()))
            .build());
        this.playSound = this.sgPathfinding.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("play-sound"))
            .description("Plays a sound when its finished building or failed"))
            .defaultValue(true))
            .visible(() -> this.autoPathfinding.get()))
            .build());
        this.debugMode = this.sgPathfinding.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("debug-mode"))
            .description("Logs why each nearby candidate is rejected from placement, once per second"))
            .defaultValue(false))
            .build());
        this.debugTickCounter = 0;
        this.render = this.sgRender.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("render"))
            .description("Renders placement positions"))
            .defaultValue(true))
            .build());
        this.renderPlaceableOnly = this.sgRender.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder()
            .name("render-placeable-only"))
            .description("Only renders blocks that are placeable or stuff that can be replaced"))
            .defaultValue(true))
            .build());
        this.renderRadius = this.sgRender.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder()
            .name("render-radius"))
            .description("Only renders placeable blocks in a specified radius (lower if you experience FPS drops)"))
            .defaultValue(16))
            .sliderMin(2)
            .sliderMax(128)
            .visible(() -> MusheorSystem.Manager.placeRender.get()))
            .build());
        this.renderMaxResults = this.sgRender.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder()
            .name("render-max-results"))
            .description("How many blocks are allowed to be rendered at once (increasing can cause lag)"))
            .defaultValue(2000))
            .sliderRange(50, 10000)
            .build());
        this.schematicName = this.sgLitematica.add(((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) new StringSetting.Builder()
            .name("schematic-name"))
            .description("The name of the schematic you want to place"))
            .visible(() -> LitematicaHelper.isLoaded() && this.selectionType.get() == SelectionType.LITEMATIC))
            .defaultValue(""))
            .build());
        this.schematicPos = this.sgLitematica.add(((BlockPosSetting.Builder) ((BlockPosSetting.Builder) ((BlockPosSetting.Builder) ((BlockPosSetting.Builder) new BlockPosSetting.Builder()
            .name("schematic-pos"))
            .description("The position of the schematic you want to place"))
            .visible(() -> LitematicaHelper.isLoaded() && this.selectionType.get() == SelectionType.LITEMATIC && this.showSchematicPos))
            .defaultValue(new BlockPos(0, 0, 0)))
            .build());
    }

    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WButton loadAndPlaceSchematic = (WButton)list.add((WWidget)theme.button("Load and place schematic")).widget();
        loadAndPlaceSchematic.action = () -> {
            if (this.client.player == null || this.client.world == null) {
                return;
            }
            if (!LitematicaHelper.isLoaded()) {
                this.error("Litematica is not installed", new Object[0]);
                return;
            }
            String name = (String)this.schematicName.get();
            BlockPos pos = (BlockPos)this.schematicPos.get();
            if (name.isEmpty()) {
                this.error("No schematic name specified", new Object[0]);
                return;
            }
            if (LitematicaHelper.get().ensureSchematicAt(name, pos)) {
                this.info("Loaded schematic: \u00a7b%s", new Object[]{name});
            }
        };
        WButton clearPlacement = (WButton)list.add((WWidget)theme.button("Remove placement")).widget();
        clearPlacement.action = () -> {
            if (LitematicaHelper.isLoaded()) {
                LitematicaHelper.get().clearAllPlacements();
            }
        };
        WButton toggleSettingVisibility = (WButton)list.add((WWidget)theme.button("Toggle BlockPos visibility")).widget();
        toggleSettingVisibility.action = () -> {
            this.showSchematicPos = !this.showSchematicPos;
            toggleSettingVisibility.set(this.showSchematicPos ? "Hide Coordinates" : "Show Coordinates");
            this.schematicPos.onChanged();
        };
        return list;
    }

    private void putSchematicBlock(BlockPos pos, BlockState targetState) {
        long chunkKey = ChunkPos.toLong((int)(pos.getX() >> 4), (int)(pos.getZ() >> 4));
        ((Map)this.chunkTargets.computeIfAbsent(chunkKey, k -> new HashMap())).put(pos, targetState);
    }

    private void fillSelection(ISelection selection) {
        if (selection == null || this.client.world == null) {
            return;
        }
        BetterBlockPos min = selection.min();
        BetterBlockPos max = selection.max();
        for (int x = min.getX(); x <= max.getX(); ++x) {
            for (int y = min.getY(); y <= max.getY(); ++y) {
                for (int z = min.getZ(); z <= max.getZ(); ++z) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if ((Boolean)this.onlyAir.get() != false ? !(this.client.world.getBlockState(pos).getBlock() instanceof AirBlock) : !BlockUtils.canPlace((BlockPos)pos, (boolean)false)) continue;
                    this.putSchematicBlock(pos, ((Block)this.baritoneBlock.get()).getDefaultState());
                }
            }
        }
    }

    private boolean usesPacketRotation() {
        return this.rotationType.get() == RotationType.PACKET;
    }

    public void onActivate() {
        this.chunkTargets.clear();
        this.restockPriority.clear();
        HighwayBuilderState.getInstance().getRecentPlacementAttempts().clear();
        this.tickCounter = 0;
        this.swapDelayLeft = 0;
        this.rotationSettleLeft = 0;
        this.pendingStrategy = null;
        this.strategyCache.clear();
        this.restockedCount = 0;
        this.desyncTicks = 0;
        this.needsInventorySync = false;
        this.remainingMaterials = new HashMap<Block, Integer>();
        this.materialRefreshTicks = 0;
        this.spiralRadius = -1;
        this.spiralIndex = 0;
        this.pathTarget = null;
        this.pendingPathTarget = null;
        this.pathingToRestock = false;
        this.waitingAfterContainerPlace = false;
        this.clearStateCaches();
        if (this.selectionType.get() == SelectionType.CUBOID) {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            ISelectionManager selectionManager = baritone.getSelectionManager();
            if (selectionManager.getSelections() == null) {
                this.error("No Baritone selection found.", new Object[0]);
                this.toggle();
                return;
            }
            for (ISelection selection : selectionManager.getSelections()) {
                this.fillSelection(selection);
            }
        }
        if (this.selectionType.get() == SelectionType.LITEMATIC) {
            if (!LitematicaHelper.isLoaded()) {
                this.error("Litematica is not installed. Install Litematica or switch to Baritone mode.", new Object[0]);
                this.toggle();
                return;
            }
            if (!LitematicaHelper.get().verifySchematic()) {
                this.error("Invalid or missing schematic", new Object[0]);
                this.toggle();
                return;
            }
            this.remainingMaterials = LitematicaHelper.get().getRemainingMaterialCounts((List)this.ignoredBlocks.get(), ((Boolean)this.onlyAir.get()).booleanValue());
            int total = this.remainingMaterials.values().stream().mapToInt(Integer::intValue).sum();
            this.info("Schematic ready \u2014 \u00a7b" + total + "\u00a7r blocks remaining to place", new Object[0]);
        }
    }

    public void onDeactivate() {
        musheor.utils.ModuleControl.cancelBaritone();
        RotationManager.release((int)10);
        this.renderQueue.clear();
        this.remainingMaterials.clear();
        this.materialRefreshTicks = 0;
        this.spiralRadius = -1;
        this.spiralIndex = 0;
        this.pathTarget = null;
        this.pendingPathTarget = null;
        this.pathingToRestock = false;
        this.waitingAfterContainerPlace = false;
        this.clearStateCaches();
        this.containerActionTicks = 0;
        this.swapDelayLeft = 0;
        this.interactCooldown = 0;
    }

    private void clearStateCaches() {
        this.stateClickCooldown.clear();
        this.lastClicksNeeded.clear();
        this.stateStuckRounds.clear();
        this.abandonedStatePositions.clear();
        this.stateClickDelayLeft = 0;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        boolean needsRotation;
        HighwayBuilderState state;
        block119: {
            int chunksThisTick;
            int currentRingSize;
            ChunkPos playerChunk;
            int maxChunkRadius;
            block122: {
                int containerSize;
                ScreenHandler handler;
                BlockPos container;
                int amount;
                Item neededItem;
                block118: {
                    block120: {
                        BlockPos closest;
                        block121: {
                            block116: {
                                Block neededBlock;
                                block117: {
                                    block115: {
                                        block114: {
                                            boolean doDebug;
                                            if (this.client.player == null) return;
                                            if (this.client.world == null) {
                                                return;
                                            }
                                            state = HighwayBuilderState.getInstance();
                                            ++this.tickCounter;
                                            RotationManager.release((int)10);
                                            if (this.selectionType.get() == SelectionType.LITEMATIC && ((Boolean)this.autoPathfinding.get()).booleanValue() && ++this.materialRefreshTicks >= 200) {
                                                this.materialRefreshTicks = 0;
                                                this.refreshMaterialCounts();
                                            }
                                            HighwayBuilderState.getInstance().getRecentPlacementAttempts().entrySet().removeIf(entry -> this.tickCounter - (Integer)entry.getValue() > (Integer)MusheorSystem.Manager.placementTimeout.get());
                                            if (((Boolean)this.pauseWhenEating.get()).booleanValue() && ((AutoEat)Modules.get().get(AutoEat.class)).eating) {
                                                return;
                                            }
                                            if (((Boolean)this.pauseOnAura.get()).booleanValue() && ((KillAura)Modules.get().get(KillAura.class)).attacking) {
                                                return;
                                            }
                                            if (((Boolean)this.fixDesync.get()).booleanValue()) {
                                                if (this.needsInventorySync) {
                                                    this.needsInventorySync = false;
                                                    VersionHelper.get().syncInventory();
                                                } else if (this.client.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && ((AutoTotem)Modules.get().get(AutoTotem.class)).isActive()) {
                                                    ++this.desyncTicks;
                                                    if (this.desyncTicks >= 20) {
                                                        this.needsInventorySync = true;
                                                        this.desyncTicks = 0;
                                                    }
                                                }
                                            }
                                            this.placeQueue.clear();
                                            if (this.selectionType.get() != SelectionType.LITEMATIC) break block114;
                                            BlockPos playerPos = this.client.player.getBlockPos();
                                            int pRange = (int)Math.ceil((Double)MusheorSystem.Manager.placementRange.get()) + 1;
                                            Map placementCandidates = LitematicaHelper.get().getBlocksInBox(playerPos.add(-pRange, -pRange, -pRange), playerPos.add(pRange, pRange, pRange), ((Boolean)this.onlyAir.get()).booleanValue(), (List)this.ignoredBlocks.get(), 1500);
                                            boolean bl = doDebug = (Boolean)this.debugMode.get() != false && ++this.debugTickCounter >= 20;
                                            if (doDebug) {
                                                this.debugTickCounter = 0;
                                            }
                                            if (doDebug && placementCandidates.isEmpty()) {
                                                this.info("[DBG] No schematic candidates in box (pRange=%d)", new Object[]{pRange});
                                            }
                                            BlockPos playerFeet = this.client.player.getBlockPos();
                                            for (Map.Entry<?, ?> entry2 : ((Map<?, ?>)placementCandidates).entrySet()) {
                                                BlockPos pos = (BlockPos)entry2.getKey();
                                                BlockState blockState = (BlockState)entry2.getValue();
                                                String blockName = Registries.BLOCK.getId(blockState.getBlock()).getPath();
                                                if (InventoryManager.countItem((Item)blockState.getBlock().asItem()) <= 0) {
                                                    if (!doDebug) continue;
                                                    this.info("[DBG] %s @ %s \u2014 skip: no items in inventory", new Object[]{blockName, pos.toShortString()});
                                                    continue;
                                                }
                                                if (!BlockUtils.canPlace((BlockPos)pos, (boolean)true)) {
                                                    if (!doDebug) continue;
                                                    this.info("[DBG] %s @ %s \u2014 skip: BlockUtils.canPlace=false", new Object[]{blockName, pos.toShortString()});
                                                    continue;
                                                }
                                                if (this.layerType.get() == LayerType.SINGLE && (double)pos.getY() >= Math.floor(this.client.player.getY())) {
                                                    if (!doDebug) continue;
                                                    this.info("[DBG] %s @ %s \u2014 skip: above feet layer", new Object[]{blockName, pos.toShortString()});
                                                    continue;
                                                }
                                                if (this.layerType.get() == LayerType.RANGE && !LitematicaHelper.get().isPositionInRenderLayer(pos)) {
                                                    if (!doDebug) continue;
                                                    this.info("[DBG] %s @ %s \u2014 skip: not in render layer", new Object[]{blockName, pos.toShortString()});
                                                    continue;
                                                }
                                                if (state.getRecentPlacementAttempts().containsKey(pos)) {
                                                    if (!doDebug) continue;
                                                    this.info("[DBG] %s @ %s \u2014 skip: recent attempt (tick %d, now %d)", new Object[]{blockName, pos.toShortString(), state.getRecentPlacementAttempts().get(pos), this.tickCounter});
                                                    continue;
                                                }
                                                if (pos.equals((Object)playerFeet) || pos.equals((Object)playerFeet.up())) {
                                                    if (!doDebug) continue;
                                                    this.info("[DBG] %s @ %s \u2014 skip: player standing here", new Object[]{blockName, pos.toShortString()});
                                                    continue;
                                                }
                                                PlacementStrategy strat = this.getPlacementStrategy(blockState);
                                                if (!this.inPlacementRange(pos, strat)) {
                                                    if (!doDebug) continue;
                                                    this.info("[DBG] %s @ %s \u2014 skip: not in placement range (hitDist=%.2f, range=%.1f)", new Object[]{blockName, pos.toShortString(), Math.sqrt(this.client.player.getEyePos().squaredDistanceTo(this.placementHitResult(pos, strat, this.useFreeHit(strat)).getPos())), MusheorSystem.Manager.placementRange.get()});
                                                    continue;
                                                }
                                                if (doDebug) {
                                                    this.info("[DBG] %s @ %s \u2014 ADDED to posAndBlocks", new Object[]{blockName, pos.toShortString()});
                                                }
                                                this.placeQueue.add((Pair<BlockPos, BlockState>)Pair.of(pos, blockState));
                                            }
                                            if (((Boolean)this.render.get()).booleanValue()) {
                                                int rRange = (Integer)this.renderRadius.get();
                                                Map renderCandidates = LitematicaHelper.get().getBlocksInBox(playerPos.add(-rRange, -rRange, -rRange), playerPos.add(rRange, rRange, rRange), ((Boolean)this.onlyAir.get()).booleanValue(), (List)this.ignoredBlocks.get(), ((Integer)this.renderMaxResults.get()).intValue());
                                                this.renderQueue.clear();
                                                for (Map.Entry<?, ?> entry3 : ((Map<?, ?>)renderCandidates).entrySet()) {
                                                    if (InventoryManager.countItem((Item)((BlockState)entry3.getValue()).getBlock().asItem()) <= 0 || this.layerType.get() == LayerType.SINGLE && (double)((BlockPos)entry3.getKey()).getY() >= Math.floor(this.client.player.getY()) || this.layerType.get() == LayerType.RANGE && !LitematicaHelper.get().isPositionInRenderLayer((BlockPos)entry3.getKey()) || !HighwayUtils.isWithinRange((BlockPos)((BlockPos)entry3.getKey()), (double)rRange)) continue;
                                                    this.renderQueue.add((Pair<BlockPos, BlockState>)Pair.of(((BlockPos)entry3.getKey()), ((BlockState)entry3.getValue())));
                                                }
                                            }
                                            break block115;
                                        }
                                        this.renderQueue.clear();
                                        ChunkPos playerChunk2 = this.client.player.getChunkPos();
                                        int chunkRange = (Integer)this.pathfindChunkRange.get();
                                        block8: for (int cx = playerChunk2.x - chunkRange; cx <= playerChunk2.x + chunkRange; ++cx) {
                                            int cz = playerChunk2.z - chunkRange;
                                            while (true) {
                                                if (cz > playerChunk2.z + chunkRange) continue block8;
                                                long key = ChunkPos.toLong((int)cx, (int)cz);
                                                Map posMap = (Map)this.chunkTargets.get(key);
                                                if (posMap != null && !posMap.isEmpty()) {
                                                    for (Map.Entry<?, ?> entry4 : ((Map<?, ?>)posMap).entrySet()) {
                                                        BlockPos pos = (BlockPos)entry4.getKey();
                                                        BlockState currentState = this.client.world.getBlockState(pos);
                                                        BlockState targetState = (BlockState)entry4.getValue();
                                                        if (InventoryManager.countItem((Item)targetState.getBlock().asItem()) <= 0 || !BlockUtils.canPlace((BlockPos)pos, (boolean)true) || this.layerType.get() == LayerType.SINGLE && (double)pos.getY() >= Math.floor(this.client.player.getY()) || this.layerType.get() == LayerType.RANGE && (!LitematicaHelper.isLoaded() || !LitematicaHelper.get().isPositionInRenderLayer(pos)) || !(currentState.getBlock() instanceof AirBlock) && ((Boolean)this.onlyAir.get()).booleanValue()) continue;
                                                        if (((Boolean)this.render.get()).booleanValue() && HighwayUtils.isWithinRange((BlockPos)pos, (double)((Integer)this.renderRadius.get()).intValue())) {
                                                            this.renderQueue.add((Pair<BlockPos, BlockState>)Pair.of(pos, targetState));
                                                        }
                                                        if (state.getRecentPlacementAttempts().containsKey(pos) || !this.inPlacementRange(pos, this.getPlacementStrategy(targetState)) || !this.placeQueue.stream().noneMatch(pair -> ((BlockPos)pair.first()).equals((Object)pos))) continue;
                                                        this.placeQueue.add((Pair<BlockPos, BlockState>)Pair.of(pos, targetState));
                                                    }
                                                    if (posMap.isEmpty()) {
                                                        this.chunkTargets.remove(key);
                                                    }
                                                }
                                                ++cz;
                                            }
                                        }
                                    }
                                    this.placeQueue.sort(Comparator.comparingDouble(pair -> this.client.player.squaredDistanceTo((double)((BlockPos)pair.first()).getX(), (double)((BlockPos)pair.first()).getY(), (double)((BlockPos)pair.first()).getZ())));
                                    this.tickFixStates();
                                    if (this.autoRestock.get() == RestockType.NONE || !((Boolean)this.autoPathfinding.get()).booleanValue()) break block116;
                                    neededBlock = this.pickPrimaryBlock();
                                    if (neededBlock == null) {
                                        boolean nothingLeft = this.selectionType.get() == SelectionType.LITEMATIC
                                            ? this.placeQueue.isEmpty()
                                            : (this.placeQueue.isEmpty() && this.chunkTargets.isEmpty());
                                        if (nothingLeft) {
                                            this.info("All blocks placed!", new Object[0]);
                                            if (((Boolean)this.playSound.get()).booleanValue()) {
                                                VersionHelper.get().playSoundPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
                                            }
                                            this.toggle();
                                            return;
                                        }
                                        if (this.selectionType.get() == SelectionType.CUBOID) {
                                            this.info("Cannot reach or find more blocks to place", new Object[0]);
                                            if (((Boolean)this.playSound.get()).booleanValue()) {
                                                VersionHelper.get().playSoundPlayer(SoundEvents.ENTITY_VILLAGER_NO);
                                            }
                                            this.toggle();
                                            return;
                                        }
                                    }
                                    if (InventoryManager.countItem((Item)neededBlock.asItem()) <= 0 && this.restockPriority.isEmpty()) {
                                        if (this.selectionType.get() == SelectionType.LITEMATIC && this.remainingMaterials.getOrDefault(neededBlock, 0) == 0) {
                                            this.remainingMaterials.remove(neededBlock);
                                            return;
                                        }
                                        this.buildRestockPriority(neededBlock);
                                    }
                                    if (this.autoRestock.get() != RestockType.INVENTORY) break block117;
                                    if (!this.restockPriority.isEmpty()) {
                                        Refill refill;
                                        if (BaritoneHelper.isPathing()) {
                                            BaritoneHelper.cancel();
                                        }
                                        if ((refill = (Refill)Modules.get().get(Refill.class)).isActive()) {
                                            return;
                                        }
                                        if (this.waitingAfterContainerPlace) {
                                            this.waitingAfterContainerPlace = false;
                                            this.restockPriority.clear();
                                            this.containerActionTicks = 0;
                                            return;
                                        }
                                        while (!this.restockPriority.isEmpty() && InventoryManager.countItemIncludingShulkers((Item)this.restockPriority.getFirst()) <= 0) {
                                            this.restockPriority.removeFirst();
                                        }
                                        if (this.restockPriority.isEmpty()) {
                                            this.info("No shulkers found for needed blocks, disabling...", new Object[0]);
                                            if (((Boolean)this.playSound.get()).booleanValue()) {
                                                VersionHelper.get().playSoundPlayer(SoundEvents.ENTITY_VILLAGER_NO);
                                            }
                                            this.toggle();
                                            return;
                                        }
                                        if (this.containerActionTicks <= (Integer)InventoryManager.INSTANCE.delayAfterPlacingContainer.get()) {
                                            ++this.containerActionTicks;
                                            return;
                                        }
                                        refill.item2.set(this.restockPriority.getFirst());
                                        refill.toggle();
                                        this.waitingAfterContainerPlace = true;
                                        return;
                                    }
                                    break block116;
                                }
                                if (this.restockPriority.isEmpty() || !this.placeQueue.isEmpty()) break block116;
                                neededItem = this.restockPriority.getFirst();
                                amount = this.countNeeded(Block.getBlockFromItem((Item)neededItem));
                                if (this.heldPlaceItem != neededItem) {
                                    this.blacklistedContainers.clear();
                                    this.heldPlaceItem = neededItem;
                                    this.restockedCount = 0;
                                    this.info("Restocking %s", new Object[]{RestockConfig.formatItemName((Item)neededItem)});
                                }
                                if (!RestockConfig.hasContainers((Item)neededItem)) {
                                    if (neededItem != neededBlock.asItem()) {
                                        this.restockPriority.removeFirst();
                                        return;
                                    }
                                    this.info("Cannot find container for %s, disabling...", new Object[]{RestockConfig.formatItemName((Item)neededItem)});
                                    if (((Boolean)this.playSound.get()).booleanValue()) {
                                        VersionHelper.get().playSoundPlayer(SoundEvents.ENTITY_VILLAGER_NO);
                                    }
                                    this.restockPriority.clear();
                                    this.toggle();
                                    return;
                                }
                                container = this.nearestRestockContainer(neededItem);
                                if (container == null) {
                                    if (neededItem != neededBlock.asItem()) {
                                        this.restockPriority.removeFirst();
                                        return;
                                    }
                                    this.info("All containers for %s are empty, disabling...", new Object[]{RestockConfig.formatItemName((Item)neededItem)});
                                    if (((Boolean)this.playSound.get()).booleanValue()) {
                                        VersionHelper.get().playSoundPlayer(SoundEvents.ENTITY_VILLAGER_NO);
                                    }
                                    this.blacklistedContainers.clear();
                                    this.restockPriority.clear();
                                    this.toggle();
                                    return;
                                }
                                if (!HighwayUtils.isInPlacementRange((BlockPos)container)) {
                                    if (BaritoneHelper.isPathing()) return;
                                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath((Goal)new GoalNear(container, 2));
                                    return;
                                }
                                musheor.utils.ModuleControl.cancelBaritone();
                                if (!InventoryManager.hasOpenScreen() && this.interactCooldown == 0) {
                                    HighwayUtils.lookAtBlock((BlockPos)container);
                                    InventoryManager.canOpenContainerAt((BlockPos)container);
                                    this.interactCooldown = 3;
                                    return;
                                }
                                if (this.interactCooldown > 0) {
                                    --this.interactCooldown;
                                    return;
                                }
                                if (this.containerActionTicks <= (Integer)InventoryManager.INSTANCE.delayAfterOpeningContainer.get()) {
                                    ++this.containerActionTicks;
                                    return;
                                }
                                if (!musheor.utils.PacketBudget.canSendInvPacket()) {
                                    return;
                                }
                                handler = this.client.player.currentScreenHandler;
                                containerSize = handler.slots.size() - 36;
                                break block118;
                            }
                            if (!((Boolean)this.autoPathfinding.get()).booleanValue()) break block119;
                            if (!this.placeQueue.isEmpty()) break block120;
                            if (this.pathingToRestock) {
                                this.spiralRadius = -1;
                                this.spiralIndex = 0;
                                this.pathTarget = null;
                                this.pendingPathTarget = null;
                                this.pathingToRestock = false;
                            }
                            if (this.selectionType.get() != SelectionType.LITEMATIC) break block121;
                            if (this.pathTarget != null) {
                                double distToGoal = this.client.player.squaredDistanceTo((double)this.pathTarget.getX() + 0.5, (double)this.pathTarget.getY() + 0.5, (double)this.pathTarget.getZ() + 0.5);
                                if (((Boolean)this.debugMode.get()).booleanValue()) {
                                    this.info("[DBG-PF] goal=%s dist=%.1f pathing=%b radius=%d ringIdx=%d", new Object[]{this.pathTarget.toShortString(), Math.sqrt(distToGoal), BaritoneHelper.isPathing(), this.spiralRadius, this.spiralIndex});
                                }
                                if (BaritoneHelper.isPathing()) return;
                                if (!(distToGoal <= 16.0)) {
                                    BaritoneHelper.setGoalAndPath((GoalNear)new GoalNear(this.pathTarget, 2));
                                    return;
                                }
                                if (((Boolean)this.debugMode.get()).booleanValue()) {
                                    this.info("[DBG-PF] arrived at goal, clearing and re-scanning", new Object[0]);
                                }
                                this.pathTarget = null;
                                this.pendingPathTarget = null;
                                this.spiralRadius = -1;
                                this.spiralIndex = 0;
                                return;
                            }
                            if (this.spiralRadius == -1) {
                                Block priorityBlock = this.pickPrimaryBlock();
                                if (priorityBlock != null && InventoryManager.countItem((Item)priorityBlock.asItem()) > 0 && this.remainingMaterials.getOrDefault(priorityBlock, 0) == 0) {
                                    this.remainingMaterials.remove(priorityBlock);
                                    return;
                                }
                                this.spiralRadius = 0;
                                this.spiralIndex = 0;
                            }
                            if (this.spiralRadius > (maxChunkRadius = ((Integer)this.pathfindChunkRange.get()).intValue())) {
                                this.refreshMaterialCounts();
                                Block exhaustedBlock = this.pickPrimaryBlock();
                                if (exhaustedBlock != null && this.remainingMaterials.getOrDefault(exhaustedBlock, 0) == 0) {
                                    this.remainingMaterials.remove(exhaustedBlock);
                                    this.spiralRadius = -1;
                                    this.spiralIndex = 0;
                                    this.pathTarget = null;
                                    this.pendingPathTarget = null;
                                    return;
                                }
                                if (exhaustedBlock == null) {
                                    this.info("All blocks placed!", new Object[0]);
                                    if (((Boolean)this.playSound.get()).booleanValue()) {
                                        VersionHelper.get().playSoundPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
                                    }
                                    this.toggle();
                                    return;
                                }
                                this.info("Cannot reach any blocks to place, disabling...", new Object[0]);
                                if (((Boolean)this.playSound.get()).booleanValue()) {
                                    VersionHelper.get().playSoundPlayer(SoundEvents.ENTITY_VILLAGER_NO);
                                }
                                this.toggle();
                                return;
                            }
                            playerChunk = this.client.player.getChunkPos();
                            currentRingSize = this.spiralRadius == 0 ? 1 : 8 * this.spiralRadius;
                            chunksThisTick = Math.max(1, (currentRingSize + 3) / 4);
                            break block122;
                        }
                        if (this.chunkTargets.isEmpty() || (closest = this.findNearestPlaceable(this.chunkTargets)) == null) break block119;
                        if (HighwayUtils.distanceXZ((BlockPos)this.client.player.getBlockPos(), (BlockPos)closest) > 2.5) {
                            BaritoneHelper.setGoalAndPath((GoalNear)new GoalNear(closest, 2));
                            return;
                        }
                        musheor.utils.ModuleControl.cancelBaritone();
                        if (this.client.player.getBlockPos() != closest) break block119;
                        BaritoneHelper.setGoalAndPath((Goal)new GoalBlock(closest.east()));
                        break block119;
                    }
                    this.pathingToRestock = true;
                    break block119;
                }
                for (int i = 0; i < containerSize; ++i) {
                    ItemStack currentStack = handler.getSlot(i).getStack();
                    if (currentStack.isEmpty()) continue;
                    if (currentStack.getItem() == neededItem) {
                        this.restockedCount += currentStack.getCount();
                        this.client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, (PlayerEntity)this.client.player);
                    }
                    if (this.restockedCount >= amount || InventoryManager.countEmptySlots() <= 0 || !musheor.utils.PacketBudget.canSendInvPacket()) break;
                }
                if (this.restockedCount == 0) {
                    this.blacklistedContainers.add(container);
                } else if (this.restockedCount < amount && InventoryManager.countEmptySlots() > 0) {
                    this.blacklistedContainers.add(container);
                } else if (InventoryManager.countEmptySlots() > 0) {
                    this.blacklistedContainers.clear();
                    this.restockPriority.removeFirst();
                }
                this.containerActionTicks = 0;
                this.interactCooldown = 0;
                this.client.player.closeScreen();
                if (!this.restockPriority.isEmpty()) {
                    if (InventoryManager.countEmptySlots() > 0) return;
                }
                this.restockPriority.clear();
                return;
            }
            for (int bi = 0; bi < chunksThisTick && this.pathTarget == null && this.spiralRadius <= maxChunkRadius; ++bi) {
                ChunkPos toScan = this.spiralChunk(playerChunk, this.spiralRadius, this.spiralIndex);
                BlockPos candidate = this.findPlaceableInChunk(toScan);
                if (candidate != null && (this.pendingPathTarget == null || this.client.player.squaredDistanceTo((double)candidate.getX() + 0.5, (double)candidate.getY() + 0.5, (double)candidate.getZ() + 0.5) < this.client.player.squaredDistanceTo((double)this.pendingPathTarget.getX() + 0.5, (double)this.pendingPathTarget.getY() + 0.5, (double)this.pendingPathTarget.getZ() + 0.5))) {
                    this.pendingPathTarget = candidate;
                }
                int ringSize = this.spiralRadius == 0 ? 1 : 8 * this.spiralRadius;
                ++this.spiralIndex;
                if (this.spiralIndex < ringSize) continue;
                this.spiralIndex = 0;
                ++this.spiralRadius;
                if (this.pendingPathTarget != null) {
                    this.pathTarget = this.pendingPathTarget;
                    this.pendingPathTarget = null;
                    if (((Boolean)this.debugMode.get()).booleanValue()) {
                        this.info("[DBG-PF] new goal from ring %d: %s (dist=%.1f)", new Object[]{this.spiralRadius - 1, this.pathTarget.toShortString(), Math.sqrt(this.client.player.squaredDistanceTo((double)this.pathTarget.getX() + 0.5, (double)this.pathTarget.getY() + 0.5, (double)this.pathTarget.getZ() + 0.5))});
                    }
                    BaritoneHelper.setGoalAndPath((GoalNear)new GoalNear(this.pathTarget, 2));
                    return;
                }
                currentRingSize = this.spiralRadius == 0 ? 1 : 8 * this.spiralRadius;
                chunksThisTick = Math.max(1, (currentRingSize + 3) / 4);
            }
        }
        if (this.swapDelayLeft > 0) {
            --this.swapDelayLeft;
            return;
        }
        if (this.placeQueue.isEmpty()) return;
        Pair<BlockPos, BlockState> firstPair = null;
        for (Pair<BlockPos, BlockState> candidate : this.placeQueue) {
            if (!this.canSupportPlacement((BlockPos)candidate.first(), (BlockState)candidate.second())) continue;
            firstPair = candidate;
            break;
        }
        if (((Boolean)this.debugMode.get()).booleanValue()) {
            if (firstPair == null) {
                this.info("[DBG] posAndBlocks has %d entries but all failed canPlaceSafely", new Object[]{this.placeQueue.size()});
            } else {
                this.info("[DBG] firstPair=%s, swap=%d, rotationTimer=%d, hand=%s", new Object[]{((BlockPos)firstPair.first()).toShortString(), this.swapDelayLeft, this.rotationSettleLeft, this.client.player.getMainHandStack().getItem()});
            }
        }
        if (firstPair == null) return;
        BlockState firstState = (BlockState)firstPair.second();
        Block firstBlock = firstState.getBlock();
        PlacementStrategy firstStrat = this.getPlacementStrategy(firstState);
        if (this.rotationSettleLeft > 0) {
            --this.rotationSettleLeft;
            if (!this.usesPacketRotation()) return;
            if (this.pendingStrategy == null) return;
            this.applyStrategyRotation(this.pendingStrategy);
            return;
        }
        PlacementStrategy appliedStrategy = this.pendingStrategy;
        this.pendingStrategy = null;
        boolean rotationApplied = appliedStrategy != null;
        boolean bl = needsRotation = this.usesPacketRotation() && (firstStrat.requiredYaw() != null || firstStrat.requiredPitch() != null);
        if (needsRotation) {
            if (this.usesPacketRotation()) {
                if (!rotationApplied) {
                    this.applyStrategyRotation(firstStrat);
                    this.pendingStrategy = firstStrat;
                    this.rotationSettleLeft = (Integer)this.rotationDelay.get();
                    if ((Boolean)this.debugMode.get() == false) return;
                    this.info("[DBG] Waiting for rotation: yaw=%s pitch=%s", new Object[]{firstStrat.requiredYaw(), firstStrat.requiredPitch()});
                    return;
                }
                this.applyStrategyRotation(firstStrat);
            } else if (!this.rotationReady(firstStrat)) {
                if (((Boolean)this.debugMode.get()).booleanValue()) {
                    this.info("[DBG] Wrong facing for %s \u2014 waiting (yaw=%s pitch=%s)", new Object[]{Registries.BLOCK.getId(firstBlock).getPath(), firstStrat.requiredYaw(), firstStrat.requiredPitch()});
                }
                this.swapDelayLeft = (Integer)MusheorSystem.Manager.swapDelay.get();
                return;
            }
        }
        try (BlockPlacer.PlacementBatch batch = BlockPlacer.beginBatch((Block)firstBlock, (MusheorSystem.PlacementType)state.getPlacementType());){
            block113: {
                if (!batch.isReady()) {
                    if (((Boolean)this.debugMode.get()).booleanValue()) {
                        this.info("[DBG] Wrong item in hand (%s), switching to %s", new Object[]{this.client.player.getMainHandStack().getItem(), firstBlock.asItem()});
                    }
                    this.swapDelayLeft = (Integer)MusheorSystem.Manager.swapDelay.get();
                    return;
                }
                Iterator<Pair<BlockPos, BlockState>> iterator = this.placeQueue.iterator();
                while (iterator.hasNext()) {
                    boolean pairNeedsRotation;
                    Pair<BlockPos, BlockState> pair2 = iterator.next();
                    if (!BlockUtils.canPlace((BlockPos)((BlockPos)pair2.first()), (boolean)true)) {
                        if (!((Boolean)this.debugMode.get()).booleanValue()) continue;
                        this.info("[DBG] %s @ %s \u2014 inner skip: canPlace=false", new Object[]{Registries.BLOCK.getId(((BlockState)pair2.second()).getBlock()).getPath(), ((BlockPos)pair2.first()).toShortString()});
                        continue;
                    }
                    PlacementStrategy pairStrat = this.getPlacementStrategy((BlockState)pair2.second());
                    if (!this.inPlacementRange((BlockPos)pair2.first(), pairStrat)) {
                        if (!((Boolean)this.debugMode.get()).booleanValue()) continue;
                        this.info("[DBG] %s @ %s \u2014 inner skip: out of range (hitDist=%.2f, range=%.1f)", new Object[]{Registries.BLOCK.getId(((BlockState)pair2.second()).getBlock()).getPath(), ((BlockPos)pair2.first()).toShortString(), Math.sqrt(this.client.player.getEyePos().squaredDistanceTo(this.placementHitResult((BlockPos)pair2.first(), pairStrat, this.useFreeHit(pairStrat)).getPos())), MusheorSystem.Manager.placementRange.get()});
                        continue;
                    }
                    if (((BlockState)pair2.second()).getBlock() != firstBlock) {
                        if (!((Boolean)this.debugMode.get()).booleanValue()) continue;
                        this.info("[DBG] %s @ %s \u2014 inner skip: wrong block type (want %s)", new Object[]{Registries.BLOCK.getId(((BlockState)pair2.second()).getBlock()).getPath(), ((BlockPos)pair2.first()).toShortString(), Registries.BLOCK.getId(firstBlock).getPath()});
                        continue;
                    }
                    if (!this.canSupportPlacement((BlockPos)pair2.first(), (BlockState)pair2.second())) {
                        if (!((Boolean)this.debugMode.get()).booleanValue()) continue;
                        this.info("[DBG] %s @ %s \u2014 inner skip: canPlaceSafely=false", new Object[]{Registries.BLOCK.getId(firstBlock).getPath(), ((BlockPos)pair2.first()).toShortString()});
                        continue;
                    }
                    boolean bl2 = pairNeedsRotation = this.usesPacketRotation() && (pairStrat.requiredYaw() != null || pairStrat.requiredPitch() != null);
                    if (pairNeedsRotation) {
                        boolean mismatch;
                        if (this.usesPacketRotation() && rotationApplied) {
                            mismatch = pairStrat.requiredYaw() != appliedStrategy.requiredYaw() || !Objects.equals(pairStrat.requiredPitch(), appliedStrategy.requiredPitch());
                        } else {
                            boolean bl3 = mismatch = !this.rotationReady(pairStrat);
                        }
                        if (mismatch) {
                            if (this.usesPacketRotation()) return;
                            this.swapDelayLeft = (Integer)MusheorSystem.Manager.swapDelay.get();
                            return;
                        }
                    }
                    if (musheor.utils.PacketBudget.canPlace()) {
                        boolean freeHit = this.useFreeHit(pairStrat);
                        BlockHitResult hit = this.placementHitResult((BlockPos)pair2.first(), pairStrat, freeHit);
                        if (!batch.place((BlockPos)pair2.first(), hit, freeHit)) {
                            return;
                        }
                        if (((BlockState)pair2.second()).getBlock() == Blocks.OBSIDIAN) {
                            HighwayBuilderState.getInstance().incrementObsidianPlaced();
                        }
                        state.getRecentPlacementAttempts().put((BlockPos)pair2.first(), this.tickCounter);
                        if (this.selectionType.get() != SelectionType.LITEMATIC) continue;
                        this.remainingMaterials.computeIfPresent(((BlockState)pair2.second()).getBlock(), (k, v) -> v <= 1 ? null : Integer.valueOf(v - 1));
                        continue;
                    }
                    break block113;
                }
                return;
            }
            if ((Boolean)this.debugMode.get() == false) return;
            this.info("[DBG] canInteract()=false, skipping placement this tick", new Object[0]);
            return;
        }
    }

    private void buildRestockPriority(Block primaryBlock) {
        this.restockPriority.clear();
        this.restockPriority.add(primaryBlock.asItem());
        HashSet<Item> seen = new HashSet<Item>();
        seen.add(primaryBlock.asItem());
        ArrayList<Map.Entry<Item, Integer>> others = new ArrayList<Map.Entry<Item, Integer>>();
        if (this.selectionType.get() == SelectionType.LITEMATIC) {
            for (Map.Entry entry : this.remainingMaterials.entrySet()) {
                Item item;
                if ((Integer)entry.getValue() <= 0 || seen.contains(item = ((Block)entry.getKey()).asItem())) continue;
                seen.add(item);
                if (InventoryManager.countItem((Item)item) > 0 || !RestockConfig.hasContainers((Item)item)) continue;
                others.add(Map.entry(item, (Integer)entry.getValue()));
            }
        } else {
            for (Map<?, ?> map : this.chunkTargets.values()) {
                for (BlockState blockState : (java.util.Collection<BlockState>)(java.util.Collection)map.values()) {
                    Item item = blockState.getBlock().asItem();
                    if (seen.contains(item)) continue;
                    seen.add(item);
                    if (InventoryManager.countItem((Item)item) > 0 || !RestockConfig.hasContainers((Item)item)) continue;
                    others.add(Map.entry(item, this.countNeeded(blockState.getBlock())));
                }
            }
        }
        others.sort((a, b) -> Integer.compare((Integer)b.getValue(), (Integer)a.getValue()));
        for (Map.Entry entry : others) {
            this.restockPriority.add((Item)entry.getKey());
        }
    }

    private BlockPos nearestRestockContainer(Item item) {
        List containers = RestockConfig.getContainers((Item)item);
        BlockPos playerPos = this.client.player.getBlockPos();
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (BlockPos pos : (java.util.Collection<BlockPos>)(java.util.Collection)containers) {
            double dist;
            if (this.blacklistedContainers.contains(pos) || !((dist = playerPos.getSquaredDistance((Vec3i)pos)) < closestDist)) continue;
            closestDist = dist;
            closest = pos;
        }
        return closest;
    }

    private int countNeeded(Block block) {
        if (this.selectionType.get() == SelectionType.LITEMATIC) {
            return this.remainingMaterials.getOrDefault(block, 0);
        }
        int count = 0;
        for (Map<?, ?> posMap : this.chunkTargets.values()) {
            for (BlockState blockState : (java.util.Collection<BlockState>)(java.util.Collection)posMap.values()) {
                if (blockState.getBlock() != block) continue;
                ++count;
            }
        }
        return count;
    }

    private Block pickPrimaryBlock() {
        Set<Block> blockSource;
        Block firstInInventory = null;
        String firstInInventoryName = null;
        Block firstOverall = null;
        String firstOverallName = null;
        if (this.selectionType.get() == SelectionType.LITEMATIC) {
            blockSource = this.remainingMaterials.keySet();
        } else {
            HashSet<Block> fromMap = new HashSet<Block>();
            for (Map<?, ?> posMap : this.chunkTargets.values()) {
                for (BlockState s : (java.util.Collection<BlockState>)(java.util.Collection)posMap.values()) {
                    fromMap.add(s.getBlock());
                }
            }
            blockSource = fromMap;
        }
        for (Block b : blockSource) {
            String name = Registries.BLOCK.getId(b).getPath();
            if (firstOverall == null || name.compareTo(firstOverallName) < 0) {
                firstOverall = b;
                firstOverallName = name;
            }
            if (InventoryManager.countItem((Item)b.asItem()) <= 0 || firstInInventory != null && name.compareTo(firstInInventoryName) >= 0) continue;
            firstInInventory = b;
            firstInInventoryName = name;
        }
        if (firstInInventory != null) {
            return firstInInventory;
        }
        if (firstOverall != null) {
            return firstOverall;
        }
        return this.selectionType.get() == SelectionType.CUBOID ? (Block)this.baritoneBlock.get() : null;
    }

    private void refreshMaterialCounts() {
        if (!LitematicaHelper.isLoaded() || this.selectionType.get() != SelectionType.LITEMATIC) {
            return;
        }
        Map fresh = LitematicaHelper.get().getRemainingMaterialCounts((List)this.ignoredBlocks.get(), ((Boolean)this.onlyAir.get()).booleanValue());
        fresh.entrySet().removeIf(e -> ((Integer)((java.util.Map.Entry)e).getValue()) <= 0);
        this.remainingMaterials = fresh;
    }

    private PlacementStrategy getPlacementStrategy(BlockState targetState) {
        return this.strategyCache.computeIfAbsent(targetState, this::computePlacementStrategy);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private PlacementStrategy computePlacementStrategy(BlockState targetState) {
        if (this.client.player == null || this.client.world == null) {
            return new PlacementStrategy(Direction.DOWN, 0.5, null, null, true);
        }
        boolean hasOrientation = ORIENTATION_PROPERTIES.stream().anyMatch(arg_0 -> ((BlockState)targetState).contains(arg_0));
        if (!hasOrientation) {
            return new PlacementStrategy(Direction.DOWN, 0.5, null, null, true);
        }
        Item item = targetState.getBlock().asItem();
        if (item == Items.AIR) {
            return new PlacementStrategy(Direction.DOWN, 0.5, null, null, true);
        }
        ItemStack stack = new ItemStack((ItemConvertible)item);
        BlockPos simPos = this.client.player.getBlockPos();
        float savedYaw = this.client.player.getYaw();
        float savedPitch = this.client.player.getPitch();
        try {
            BlockHitResult hit;
            Vec3d hitVec;
            double[] yFracs;
            for (Direction hitDir : Direction.values()) {
                double[] dArray;
                if (hitDir.getAxis() == Direction.Axis.Y) {
                    double[] dArray2 = new double[1];
                    dArray = dArray2;
                    dArray2[0] = hitDir == Direction.UP ? 1.0 : 0.0;
                } else {
                    dArray = HIT_Y_FRACTIONS;
                }
                for (double hitFracY : yFracs = dArray) {
                    hitVec = new Vec3d((double)simPos.getX() + 0.5 + (double)hitDir.getOffsetX() * 0.5, (double)simPos.getY() + hitFracY, (double)simPos.getZ() + 0.5 + (double)hitDir.getOffsetZ() * 0.5);
                    hit = new BlockHitResult(hitVec, hitDir, simPos, false);
                    boolean anyMatch = false;
                    boolean mismatchFound = false;
                    block7: for (Direction yawDir : HORIZONTAL_DIRECTIONS) {
                        this.client.player.setYaw(this.yawForFacing(yawDir));
                        for (float pitch : TEST_PITCHES) {
                            this.client.player.setPitch(pitch);
                            ItemPlacementContext ctx = new ItemPlacementContext((PlayerEntity)this.client.player, Hand.MAIN_HAND, stack, hit);
                            BlockState placed = Printer.getPlacementState(targetState.getBlock(), ctx);
                            if (placed == null) continue;
                            if (this.orientationMatches(placed, targetState)) {
                                anyMatch = true;
                                continue;
                            }
                            mismatchFound = true;
                            break block7;
                        }
                    }
                    if (!anyMatch || mismatchFound) continue;
                    PlacementStrategy placementStrategy = new PlacementStrategy(hitDir, hitFracY, null, null, false);
                    return placementStrategy;
                }
            }
            for (Direction hitDir : Direction.values()) {
                double[] dArray;
                if (hitDir.getAxis() == Direction.Axis.Y) {
                    double[] dArray3 = new double[1];
                    dArray = dArray3;
                    dArray3[0] = hitDir == Direction.UP ? 1.0 : 0.0;
                } else {
                    dArray = HIT_Y_FRACTIONS;
                }
                for (double hitFracY : yFracs = dArray) {
                    hitVec = new Vec3d((double)simPos.getX() + 0.5 + (double)hitDir.getOffsetX() * 0.5, (double)simPos.getY() + hitFracY, (double)simPos.getZ() + 0.5 + (double)hitDir.getOffsetZ() * 0.5);
                    hit = new BlockHitResult(hitVec, hitDir, simPos, false);
                    for (Direction yawDir : HORIZONTAL_DIRECTIONS) {
                        this.client.player.setYaw(this.yawForFacing(yawDir));
                        for (float pitch : TEST_PITCHES) {
                            this.client.player.setPitch(pitch);
                            ItemPlacementContext ctx = new ItemPlacementContext((PlayerEntity)this.client.player, Hand.MAIN_HAND, stack, hit);
                            BlockState placed = Printer.getPlacementState(targetState.getBlock(), ctx);
                            if (placed == null || !this.orientationMatches(placed, targetState)) continue;
                            boolean needsH = targetState.contains((Property)Properties.HORIZONTAL_FACING) || targetState.contains((Property)Properties.FACING) && ((Direction)targetState.get((Property)Properties.FACING)).getAxis().isHorizontal();
                            boolean pitchMatters = false;
                            for (float otherPitch : TEST_PITCHES) {
                                if (otherPitch == pitch) continue;
                                this.client.player.setPitch(otherPitch);
                                ItemPlacementContext ctx2 = new ItemPlacementContext((PlayerEntity)this.client.player, Hand.MAIN_HAND, stack, hit);
                                BlockState placed2 = Printer.getPlacementState(targetState.getBlock(), ctx2);
                                if (placed2 != null && this.orientationMatches(placed2, targetState)) continue;
                                pitchMatters = true;
                                break;
                            }
                            this.client.player.setPitch(pitch);
                            Direction reqYaw = needsH ? yawDir : null;
                            Float reqPitch = pitchMatters ? Float.valueOf(pitch) : null;
                            PlacementStrategy placementStrategy = new PlacementStrategy(hitDir, hitFracY, reqYaw, reqPitch, false);
                            return placementStrategy;
                        }
                    }
                }
            }
        }
        finally {
            this.client.player.setYaw(savedYaw);
            this.client.player.setPitch(savedPitch);
        }
        return new PlacementStrategy(Direction.DOWN, 0.5, null, null, false);
    }

    private boolean orientationMatches(BlockState placed, BlockState target) {
        if (placed.getBlock() != target.getBlock()) {
            return false;
        }
        for (Property<?> prop : ORIENTATION_PROPERTIES) {
            if (!placed.contains(prop) || !target.contains(prop) || placed.get(prop).equals(target.get(prop))) continue;
            return false;
        }
        return true;
    }

    private boolean rotationReady(PlacementStrategy strat) {
        if (this.client.player == null) {
            return false;
        }
        float effYaw = RotationManager.yawOr((float)this.client.player.getYaw());
        float effPitch = RotationManager.pitchOr((float)this.client.player.getPitch());
        if (strat.requiredYaw() != null && Printer.facingFromYaw(effYaw) != strat.requiredYaw()) {
            return false;
        }
        if (strat.requiredPitch() != null) {
            boolean isDown;
            float actual = effPitch;
            float required = strat.requiredPitch().floatValue();
            boolean reqUp = required < -45.0f;
            boolean reqDown = required > 45.0f;
            boolean isUp = actual < -45.0f;
            boolean bl = isDown = actual > 45.0f;
            if (reqUp && !isUp) {
                return false;
            }
            if (reqDown && !isDown) {
                return false;
            }
            if (!reqUp && !reqDown && (isUp || isDown)) {
                return false;
            }
        }
        return true;
    }

    private float yawForFacing(Direction facing) {
        return switch (facing) {
            case Direction.SOUTH -> 0.0f;
            case Direction.WEST -> 90.0f;
            case Direction.NORTH -> 180.0f;
            case Direction.EAST -> -90.0f;
            default -> this.client.player.getYaw();
        };
    }

    private static Direction facingFromYaw(float yaw) {
        float normalized = (yaw % 360.0f + 360.0f) % 360.0f;
        if (normalized >= 315.0f || normalized < 45.0f) {
            return Direction.SOUTH;
        }
        if (normalized < 135.0f) {
            return Direction.WEST;
        }
        if (normalized < 225.0f) {
            return Direction.NORTH;
        }
        return Direction.EAST;
    }

    private void applyStrategyRotation(PlacementStrategy strat) {
        if (strat.requiredYaw() != null) {
            RotationManager.setYaw((float)this.yawForFacing(strat.requiredYaw()), (int)10);
        }
        if (strat.requiredPitch() != null) {
            RotationManager.setPitch((float)RotationManager.snapPitch((float)strat.requiredPitch().floatValue()), (int)10);
        }
    }

    private void tickFixStates() {
        if (!((Boolean)this.setCorrectStates.get()).booleanValue()) {
            return;
        }
        if (this.selectionType.get() != SelectionType.LITEMATIC || !LitematicaHelper.isLoaded()) {
            return;
        }
        if (this.client.player == null || this.client.world == null) {
            return;
        }
        if (this.client.player.isSneaking() || this.client.currentScreen != null) {
            return;
        }
        if (this.tickCounter % 200 == 0) {
            this.stateClickCooldown.entrySet().removeIf(e -> this.tickCounter >= (Integer)e.getValue());
            this.lastClicksNeeded.keySet().removeIf(this::isTooFar);
            this.stateStuckRounds.keySet().removeIf(this::isTooFar);
        }
        if (this.stateClickDelayLeft > 0) {
            --this.stateClickDelayLeft;
            return;
        }
        BlockPos playerPos = this.client.player.getBlockPos();
        int range = (int)Math.ceil(this.interactionRange()) + 1;
        Map mismatched = LitematicaHelper.get().getMismatchedStatesInBox(playerPos.add(-range, -range, -range), playerPos.add(range, range, range), (List)this.ignoredBlocks.get(), 512);
        if (mismatched.isEmpty()) {
            return;
        }
        ArrayList targets = new ArrayList(mismatched.entrySet());
        targets.sort(Comparator.comparingDouble((java.util.Map.Entry e) -> this.client.player.squaredDistanceTo((double)((BlockPos)e.getKey()).getX() + 0.5, (double)((BlockPos)e.getKey()).getY() + 0.5, (double)((BlockPos)e.getKey()).getZ() + 0.5)));
        boolean batched = (Integer)this.setStateDelay.get() == 0;
        for (Map.Entry<?, ?> entry : (java.util.List<Map.Entry<?, ?>>)(java.util.List)targets) {
            int sent;
            int needed;
            BlockPos pos = (BlockPos)entry.getKey();
            BlockState target = (BlockState)entry.getValue();
            if (this.abandonedStatePositions.contains(pos) || ((List)this.ignoredBlockStates.get()).contains(target.getBlock()) || this.layerType.get() == LayerType.SINGLE && (double)pos.getY() >= Math.floor(this.client.player.getY()) || this.layerType.get() == LayerType.RANGE && !LitematicaHelper.get().isPositionInRenderLayer(pos)) continue;
            Integer waitUntil = this.stateClickCooldown.get(pos);
            if (waitUntil != null) {
                if (this.tickCounter < waitUntil) continue;
                this.stateClickCooldown.remove(pos);
            }
            if ((needed = BlockStateCycler.clicksNeeded((BlockState)this.client.world.getBlockState(pos), (BlockState)target)) == -1 || needed == 0) {
                this.lastClicksNeeded.remove(pos);
                this.stateStuckRounds.remove(pos);
                continue;
            }
            if (!this.isInInteractRange(pos)) continue;
            if (!musheor.utils.PacketBudget.canPlace()) break;
            Integer lastNeeded = this.lastClicksNeeded.get(pos);
            if (lastNeeded != null && needed >= lastNeeded) {
                if (this.stateStuckRounds.merge(pos, 1, Integer::sum) >= 3) {
                    this.abandonedStatePositions.add(pos);
                    if (!((Boolean)this.debugMode.get()).booleanValue()) continue;
                    this.info("[DBG-STATE] %s @ %s \u2014 giving up, %d rounds changed nothing", new Object[]{Registries.BLOCK.getId(target.getBlock()).getPath(), pos.toShortString(), 3});
                    continue;
                }
            } else {
                this.stateStuckRounds.remove(pos);
            }
            this.lastClicksNeeded.put(pos, needed);
            BlockHitResult hit = this.centerHitResult(pos);
            if (batched) {
                for (sent = 0; sent < needed && musheor.utils.PacketBudget.canPlace(); ++sent) {
                    BlockUtils.interact((BlockHitResult)hit, (Hand)Hand.MAIN_HAND, (boolean)true);
                }
                this.stateClickCooldown.put(pos, this.tickCounter + 5);
            } else {
                BlockUtils.interact((BlockHitResult)hit, (Hand)Hand.MAIN_HAND, (boolean)true);
                sent = 1;
                this.stateClickDelayLeft = (Integer)this.setStateDelay.get();
                this.stateClickCooldown.put(pos, this.tickCounter + (Integer)this.setStateDelay.get());
            }
            if (((Boolean)this.debugMode.get()).booleanValue()) {
                this.info("[DBG-STATE] %s @ %s \u2014 sent %d/%d interactions", new Object[]{Registries.BLOCK.getId(target.getBlock()).getPath(), pos.toShortString(), sent, needed});
            }
            if (batched && sent >= needed) continue;
            break;
        }
    }

    private boolean isTooFar(BlockPos pos) {
        return this.client.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) > 1024.0;
    }

    private double interactionRange() {
        return Math.min((Double)MusheorSystem.Manager.placementRange.get(), this.client.player.getBlockInteractionRange());
    }

    private boolean isInInteractRange(BlockPos pos) {
        double range = this.interactionRange();
        return this.client.player.getEyePos().squaredDistanceTo(this.centerHitResult(pos).getPos()) <= range * range;
    }

    private BlockHitResult centerHitResult(BlockPos pos) {
        Vec3d eye = this.client.player.getEyePos();
        BlockState state = this.client.world.getBlockState(pos);
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();
        double maxX = (double)pos.getX() + 1.0;
        double maxY = (double)pos.getY() + 1.0;
        double maxZ = (double)pos.getZ() + 1.0;
        VoxelShape shape = state.getOutlineShape((BlockView)this.client.world, pos);
        if (!shape.isEmpty()) {
            Box box = shape.getBoundingBox();
            minX = (double)pos.getX() + box.minX;
            maxX = (double)pos.getX() + box.maxX;
            minY = (double)pos.getY() + box.minY;
            maxY = (double)pos.getY() + box.maxY;
            minZ = (double)pos.getZ() + box.minZ;
            maxZ = (double)pos.getZ() + box.maxZ;
        }
        Vec3d hit = new Vec3d(Math.max(minX, Math.min(eye.x, maxX)), Math.max(minY, Math.min(eye.y, maxY)), Math.max(minZ, Math.min(eye.z, maxZ)));
        return new BlockHitResult(hit, KekNuker.faceToward(eye, pos), pos, false);
    }

    private boolean useFreeHit(PlacementStrategy strat) {
        return !this.usesPacketRotation() && strat.freeHit();
    }

    private BlockHitResult placementHitResult(BlockPos pos, PlacementStrategy strat, boolean freeHit) {
        Vec3d eye = this.client.player.getEyePos();
        if (freeHit) {
            double hx = Math.max((double)pos.getX(), Math.min(eye.x, (double)pos.getX() + 1.0));
            double hy = Math.max((double)pos.getY(), Math.min(eye.y, (double)pos.getY() + 1.0));
            double hz = Math.max((double)pos.getZ(), Math.min(eye.z, (double)pos.getZ() + 1.0));
            return new BlockHitResult(new Vec3d(hx, hy, hz), KekNuker.faceToward(eye, pos), pos, false);
        }
        Direction dir = strat.hitDirection();
        double hx = (double)pos.getX() + 0.5 + (double)dir.getOffsetX() * 0.5;
        double hy = (double)pos.getY() + strat.hitYFraction();
        double hz = (double)pos.getZ() + 0.5 + (double)dir.getOffsetZ() * 0.5;
        if (dir.getAxis() != Direction.Axis.X) {
            hx = Math.max((double)pos.getX() + 0.05, Math.min(eye.x, (double)pos.getX() + 0.95));
        }
        if (dir.getAxis() != Direction.Axis.Z) {
            hz = Math.max((double)pos.getZ() + 0.05, Math.min(eye.z, (double)pos.getZ() + 0.95));
        }
        return new BlockHitResult(new Vec3d(hx, hy, hz), dir, pos, false);
    }

    private boolean inPlacementRange(BlockPos pos, PlacementStrategy strat) {
        Vec3d eye = this.client.player.getEyePos();
        Vec3d hit = this.placementHitResult(pos, strat, this.useFreeHit(strat)).getPos();
        double range = (Double)MusheorSystem.Manager.placementRange.get();
        return eye.squaredDistanceTo(hit) <= range * range;
    }

    private boolean canSupportPlacement(BlockPos pos, BlockState targetState) {
        if (targetState.getBlock() instanceof LanternBlock) {
            boolean hanging = (Boolean)targetState.get((Property)Properties.HANGING);
            BlockPos attach = hanging ? pos.up() : pos.down();
            return !this.client.world.getBlockState(attach).isAir();
        }
        if (targetState.getBlock() instanceof RedstoneWireBlock) {
            return !this.client.world.getBlockState(pos.down()).isAir();
        }
        if (((Boolean)this.gravityCheck.get()).booleanValue() && targetState.getBlock() instanceof FallingBlock) {
            return !this.client.world.getBlockState(pos.down()).isAir();
        }
        return true;
    }

    private ChunkPos spiralChunk(ChunkPos center, int radius, int index) {
        if (radius == 0) {
            return center;
        }
        int cx = center.x;
        int cz = center.z;
        if (index < 2 * radius + 1) {
            return new ChunkPos(cx - radius + index, cz - radius);
        }
        if ((index -= 2 * radius + 1) < 2 * radius) {
            return new ChunkPos(cx + radius, cz - radius + 1 + index);
        }
        if ((index -= 2 * radius) < 2 * radius) {
            return new ChunkPos(cx + radius - 1 - index, cz + radius);
        }
        return new ChunkPos(cx - radius, cz + radius - 1 - (index -= 2 * radius));
    }

    private BlockPos findPlaceableInChunk(ChunkPos chunk) {
        if (this.client.world == null || this.client.player == null) {
            return null;
        }
        if (!this.client.world.isChunkLoaded(chunk.x, chunk.z)) {
            return null;
        }
        int minX = chunk.getStartX();
        int minZ = chunk.getStartZ();
        int maxX = chunk.getEndX();
        int maxZ = chunk.getEndZ();
        int minY = this.client.world.getBottomY();
        int maxY = this.client.world.getBottomY() + this.client.world.getHeight() - 1;
        Map candidates = LitematicaHelper.get().getBlocksInBox(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), ((Boolean)this.onlyAir.get()).booleanValue(), (List)this.ignoredBlocks.get(), 512);
        if (candidates.isEmpty()) {
            return null;
        }
        BlockPos playerFeet = this.client.player.getBlockPos();
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Object __e : candidates.entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) __e;
            double dist;
            BlockPos pos = (BlockPos)entry.getKey();
            BlockState targetState = (BlockState)entry.getValue();
            if (InventoryManager.countItem((Item)targetState.getBlock().asItem()) <= 0 || this.layerType.get() == LayerType.SINGLE && (double)pos.getY() >= Math.floor(this.client.player.getY()) || this.layerType.get() == LayerType.RANGE && !LitematicaHelper.get().isPositionInRenderLayer(pos) || pos.equals((Object)playerFeet) || pos.equals((Object)playerFeet.up()) || !((dist = this.client.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5)) < closestDist)) continue;
            closestDist = dist;
            closest = pos;
        }
        return closest;
    }

    private BlockPos findNearestPlaceable(Long2ObjectMap<Map<BlockPos, BlockState>> chunkPositions) {
        if (this.client.player == null || this.client.world == null) {
            return null;
        }
        double px = this.client.player.getX();
        double py = this.client.player.getY();
        double pz = this.client.player.getZ();
        ArrayList<Map.Entry> chunks = new ArrayList<Map.Entry>();
        for (Long2ObjectMap.Entry entry : chunkPositions.long2ObjectEntrySet()) {
            chunks.add(Map.entry(entry.getLongKey(), (Map)entry.getValue()));
        }
        chunks.sort(Comparator.comparingDouble(e -> {
            int cx = ChunkPos.getPackedX((long)((Long)e.getKey()));
            int cz = ChunkPos.getPackedZ((long)((Long)e.getKey()));
            double centerX = (double)(cx << 4) + 8.0;
            double centerZ = (double)(cz << 4) + 8.0;
            return (centerX - px) * (centerX - px) + (centerZ - pz) * (centerZ - pz);
        }));
        for (Map.Entry chunk : chunks) {
            Map posMap = (Map)chunk.getValue();
            if (posMap == null || posMap.isEmpty()) continue;
            BlockPos bestPos = null;
            double bestDist = Double.MAX_VALUE;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>)posMap).entrySet()) {
                double dz;
                double dy;
                double dx;
                double distSq;
                BlockPos pos = (BlockPos)entry.getKey();
                BlockState targetState = (BlockState)entry.getValue();
                if (this.client.world.getBlockState(pos).getBlock() == targetState.getBlock() || !(this.client.world.getBlockState(pos).getBlock() instanceof AirBlock) || !BlockUtils.canPlaceBlock((BlockPos)pos, (boolean)true, (Block)targetState.getBlock()) || InventoryManager.countItem((Item)targetState.getBlock().asItem()) <= 0 || !((distSq = (dx = (double)pos.getX() + 0.5 - px) * dx + (dy = (double)pos.getY() + 0.5 - py) * dy + (dz = (double)pos.getZ() + 0.5 - pz) * dz) < bestDist)) continue;
                bestDist = distSq;
                bestPos = pos;
            }
            if (bestPos == null) continue;
            return bestPos;
        }
        return null;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (this.client.player == null || this.client.world == null) {
            return;
        }
        if (!((Boolean)MusheorSystem.Manager.placeRender.get()).booleanValue() || !((Boolean)this.render.get()).booleanValue()) {
            return;
        }
        if (this.renderQueue.isEmpty()) {
            return;
        }
        ArrayList<Pair<BlockPos, Block>> renderBlocks = new ArrayList<>();
        for (Pair<BlockPos, BlockState> pair : this.renderQueue) {
            if (((Boolean)this.renderPlaceableOnly.get()).booleanValue() && !BlockUtils.canPlace((BlockPos)((BlockPos)pair.first()))) continue;
            renderBlocks.add(Pair.of(((BlockPos)pair.first()), ((BlockState)pair.second()).getBlock()));
        }
        BlockRenderUtils.renderBlocks((Render3DEvent)event, renderBlocks);
    }

    private record PlacementStrategy(Direction hitDirection, double hitYFraction, Direction requiredYaw, Float requiredPitch, boolean freeHit) {


    }

    private enum SelectionType {
        CUBOID,
        LITEMATIC}

    public enum LayerType {
        ALL,
        SINGLE,
        RANGE}

    private enum RotationType {
        NONE,
        PACKET}

    private enum RestockType {
        NONE,
        HOTBAR,
        INVENTORY}
}

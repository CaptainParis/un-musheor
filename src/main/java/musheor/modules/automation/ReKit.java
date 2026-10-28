/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.reflect.TypeToken
 *  com.google.gson.Gson
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.events.game.OpenScreenEvent
 *  meteordevelopment.meteorclient.events.world.TickEvent$Pre
 *  meteordevelopment.meteorclient.gui.GuiTheme
 *  meteordevelopment.meteorclient.gui.widgets.WWidget
 *  meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList
 *  meteordevelopment.meteorclient.gui.widgets.pressable.WButton
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.KeybindSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.StringSetting$Builder
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.utils.misc.Keybind
 *  meteordevelopment.orbit.EventHandler
 *  musheor.Musheor
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.screen.ingame.HandledScreen
 *  net.minecraft.client.gui.screen.ingame.InventoryScreen
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.registry.Registries
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.slot.SlotActionType
 *  net.minecraft.util.Identifier
 *  musheor.utils.PacketBudget
 */
package musheor.modules.automation;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import musheor.utils.PacketBudget;

public class ReKit
extends Module {
    private final SettingGroup sgGeneral;
    private final MinecraftClient mc;
    public final Setting<RekitMode> mode;
    public final Setting<Keybind> key;
    public final Setting<String> kitName;
    private final HashMap<String, HashMap<Integer, Item>> kitCache;
    private boolean keyWasPressed;
    private int pendingTicks;
    private ScreenHandler pendingHandler;

    public ReKit() {
        super(Musheor.MAIN, "rekit", "Sorts inventory to match a saved kit loadout when opening a container or pressing a keybind.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.mc = MinecraftClient.getInstance();
        this.mode = this.sgGeneral.add(((EnumSetting.Builder<RekitMode>) ((EnumSetting.Builder<RekitMode>) ((EnumSetting.Builder<RekitMode>) new EnumSetting.Builder<RekitMode>()
            .name("mode"))
            .description("Auto = triggers on container open. OnKey = triggers on keybind."))
            .defaultValue(RekitMode.MANUAL))
            .build());
        this.key = this.sgGeneral.add(((KeybindSetting.Builder) ((KeybindSetting.Builder) ((KeybindSetting.Builder) ((KeybindSetting.Builder) new KeybindSetting.Builder()
            .name("key"))
            .description("Keybind to trigger rekit."))
            .defaultValue(Keybind.none()))
            .visible(() -> this.mode.get() == RekitMode.AUTO))
            .build());
        this.kitName = this.sgGeneral.add(((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) new StringSetting.Builder()
            .name("kit-name"))
            .description("Name of the active kit. Leave blank to disable."))
            .defaultValue(""))
            .build());
        this.kitCache = new HashMap();
        this.keyWasPressed = false;
        this.pendingTicks = -1;
        this.pendingHandler = null;
    }

    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WButton saveBtn = (WButton)list.add((WWidget)theme.button("Save current loadout")).widget();
        WButton clearBtn = (WButton)list.add((WWidget)theme.button("Clear kit cache")).widget();
        saveBtn.action = () -> this.saveKit((String)this.kitName.get());
        clearBtn.action = this::clearCache;
        return list;
    }

    public void onActivate() {
        String name = (String)this.kitName.get();
        if (name.isEmpty()) {
            this.info("No kit selected.", new Object[0]);
            return;
        }
        HashMap<Integer, Item> kit = this.loadKit(name);
        if (kit == null) {
            this.info("Kit '\u00a7b%s\u00a7r' not found. Use \u00a7b.kit create\u00a7r to save one.", new Object[]{name});
        } else {
            this.kitCache.put(name, kit);
            this.info("Loaded kit '\u00a7b%s\u00a7r' (\u00a7b%d\u00a7r slots).", new Object[]{name, kit.size()});
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (this.mode.get() != RekitMode.MANUAL) {
            return;
        }
        if (this.mc.player == null) {
            return;
        }
        if (!(event.screen instanceof HandledScreen)) {
            return;
        }
        if (event.screen instanceof InventoryScreen) {
            return;
        }
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        if (handler == null || handler == this.mc.player.playerScreenHandler) {
            return;
        }
        this.pendingHandler = handler;
        this.pendingTicks = 1;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (this.mc.player == null) {
            return;
        }
        if (this.pendingTicks > 0 && --this.pendingTicks == 0) {
            ScreenHandler current = this.mc.player.currentScreenHandler;
            if (current != null && current == this.pendingHandler) {
                this.applyKit(this.pendingHandler);
            }
            this.pendingHandler = null;
        }
        if (this.mode.get() == RekitMode.AUTO) {
            boolean pressed = ((Keybind)this.key.get()).isPressed();
            if (pressed && !this.keyWasPressed) {
                ScreenHandler current;
                this.keyWasPressed = true;
                ScreenHandler handler = null;
                if (this.mc.currentScreen instanceof HandledScreen && (current = this.mc.player.currentScreenHandler) != null && current != this.mc.player.playerScreenHandler) {
                    handler = current;
                }
                this.applyKit(handler);
            } else if (!pressed) {
                this.keyWasPressed = false;
            }
        }
    }

    public void applyKit(ScreenHandler containerHandler) {
        if (this.mc.player == null) {
            return;
        }
        String name = (String)this.kitName.get();
        if (name.isEmpty()) {
            return;
        }
        HashMap kit = this.kitCache.computeIfAbsent(name, this::loadKit);
        if (kit == null) {
            this.info("Kit '\u00a7b%s\u00a7r' not found.", new Object[]{name});
            return;
        }
        Set<Integer> kitSlots = kit.keySet();
        int syncId = this.syncIdFor(containerHandler);
        int containerSize = containerHandler != null && containerHandler != this.mc.player.playerScreenHandler ? containerHandler.slots.size() - 36 : 0;
        Item[] invItem = new Item[36];
        int[] invCount = new int[36];
        for (int i = 0; i < 36; ++i) {
            ItemStack s = this.mc.player.getInventory().getStack(i);
            invItem[i] = s.getItem();
            invCount[i] = s.getCount();
        }
        Item[] conItem = new Item[containerSize];
        int[] conCount = new int[containerSize];
        for (int i = 0; i < containerSize; ++i) {
            ItemStack s = containerHandler.getSlot(i).getStack();
            conItem[i] = s.getItem();
            conCount[i] = s.getCount();
        }
        record MoveOp(int from, int to, int leftover) {


        }
        ArrayList<MoveOp> plan = new ArrayList<MoveOp>();
        for (Object __e : kit.entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) __e;
            int i;
            int targetInv = (Integer)entry.getKey();
            Item targetItem = (Item)entry.getValue();
            int maxCount = targetItem.getMaxCount();
            int targetScreen = this.toScreenSlot(targetInv, containerHandler);
            int current = invItem[targetInv] == targetItem ? invCount[targetInv] : 0;
            if (current >= maxCount) continue;
            if (invCount[targetInv] > 0 && invItem[targetInv] != targetItem) {
                int freeSlot = this.findFreeInvSlot(invCount, kitSlots, targetInv);
                if (freeSlot != -1) {
                    plan.add(new MoveOp(targetScreen, this.toScreenSlot(freeSlot, containerHandler), -1));
                    invItem[freeSlot] = invItem[targetInv];
                    invCount[freeSlot] = invCount[targetInv];
                } else {
                    if (containerSize <= 0) continue;
                    plan.add(new MoveOp(targetScreen, -1, -1));
                }
                invItem[targetInv] = Items.AIR;
                invCount[targetInv] = 0;
                current = 0;
            }
            int remaining = maxCount - current;
            for (i = 0; i < 36 && remaining > 0; ++i) {
                Item kitAtI;
                if (invItem[i] != targetItem || i == targetInv || invCount[i] == 0 || (kitAtI = (Item)kit.get(i)) != null && kitAtI == targetItem) continue;
                int fromScreen = this.toScreenSlot(i, containerHandler);
                int moved = Math.min(invCount[i], remaining);
                plan.add(new MoveOp(fromScreen, targetScreen, invCount[i] > remaining ? fromScreen : -1));
                int n = i;
                invCount[n] = invCount[n] - moved;
                if (invCount[i] == 0) {
                    invItem[i] = Items.AIR;
                }
                int n2 = targetInv;
                invCount[n2] = invCount[n2] + moved;
                invItem[targetInv] = targetItem;
                remaining -= moved;
            }
            for (i = 0; i < containerSize && remaining > 0; ++i) {
                if (conItem[i] != targetItem || conCount[i] == 0) continue;
                int moved = Math.min(conCount[i], remaining);
                plan.add(new MoveOp(i, targetScreen, conCount[i] > remaining ? i : -1));
                int n = i;
                conCount[n] = conCount[n] - moved;
                if (conCount[i] == 0) {
                    conItem[i] = Items.AIR;
                }
                int n3 = targetInv;
                invCount[n3] = invCount[n3] + moved;
                invItem[targetInv] = targetItem;
                remaining -= moved;
            }
        }
        for (MoveOp op : plan) {
            if (!PacketBudget.canSendInvPacket()) {
                return;
            }
            if (op.to() == -1) {
                this.mc.interactionManager.clickSlot(syncId, op.from(), 0, SlotActionType.QUICK_MOVE, (PlayerEntity)this.mc.player);
                continue;
            }
            this.mc.interactionManager.clickSlot(syncId, op.from(), 0, SlotActionType.PICKUP, (PlayerEntity)this.mc.player);
            this.mc.interactionManager.clickSlot(syncId, op.to(), 0, SlotActionType.PICKUP, (PlayerEntity)this.mc.player);
            if (op.leftover() == -1) continue;
            this.mc.interactionManager.clickSlot(syncId, op.leftover(), 0, SlotActionType.PICKUP, (PlayerEntity)this.mc.player);
        }
    }

    private int toScreenSlot(int invSlot, ScreenHandler containerHandler) {
        if (containerHandler != null && containerHandler != this.mc.player.playerScreenHandler) {
            int containerSize = containerHandler.slots.size() - 36;
            return invSlot <= 8 ? containerSize + 27 + invSlot : containerSize + (invSlot - 9);
        }
        return invSlot <= 8 ? 36 + invSlot : invSlot;
    }

    private int syncIdFor(ScreenHandler containerHandler) {
        return containerHandler != null && containerHandler != this.mc.player.playerScreenHandler ? containerHandler.syncId : this.mc.player.playerScreenHandler.syncId;
    }

    private int findFreeInvSlot(int[] invCount, Set<Integer> kitSlots, int exclude) {
        int i;
        for (i = 35; i >= 9; --i) {
            if (i == exclude || kitSlots.contains(i) || invCount[i] != 0) continue;
            return i;
        }
        for (i = 8; i >= 0; --i) {
            if (i == exclude || kitSlots.contains(i) || invCount[i] != 0) continue;
            return i;
        }
        return -1;
    }

    public void saveKit(String kitName) {
        if (kitName == null || kitName.isEmpty()) {
            this.info("Set a kit name first.", new Object[0]);
            return;
        }
        if (this.mc.player == null) {
            return;
        }
        LinkedHashMap<String, String> kitData = new LinkedHashMap<String, String>();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            kitData.put(String.valueOf(i), Registries.ITEM.getId(stack.getItem()).toString());
        }
        File file = this.kitFile(kitName);
        if (file == null) {
            return;
        }
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file);){
            new Gson().toJson(kitData, (Appendable)writer);
        }
        catch (IOException e) {
            this.error("Failed to save kit: %s", new Object[]{e.getMessage()});
            return;
        }
        HashMap<Integer, Item> itemMap = this.parseKitData(kitData);
        this.kitCache.put(kitName, itemMap);
        this.info("Saved kit '\u00a7b%s\u00a7r' (\u00a7b%d\u00a7r slots).", new Object[]{kitName, itemMap.size()});
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public HashMap<Integer, Item> loadKit(String kitName) {
        File file = this.kitFile(kitName);
        if (file == null) return null;
        if (!file.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(file);){
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            HashMap raw = (HashMap)new Gson().fromJson((Reader)reader, type);
            if (raw == null) {
                HashMap<Integer, Item> hashMap = null;
                return hashMap;
            }
            HashMap<Integer, Item> hashMap = this.parseKitData(raw);
            return hashMap;
        }
        catch (Exception e) {
            this.error("Failed to load kit '\u00a7b%s\u00a7r': %s", new Object[]{kitName, e.getMessage()});
            return null;
        }
    }

    private HashMap<Integer, Item> parseKitData(Map<String, String> raw) {
        HashMap<Integer, Item> map = new HashMap<Integer, Item>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            try {
                Item item;
                int slot = Integer.parseInt(e.getKey());
                if (slot < 0 || slot > 35 || (item = (Item)Registries.ITEM.get(Identifier.of((String)e.getValue()))) == Items.AIR) continue;
                map.put(slot, item);
            }
            catch (NumberFormatException numberFormatException) {}
        }
        return map;
    }

    public File kitFile(String kitName) {
        if (kitName == null || kitName.isEmpty()) {
            return null;
        }
        try {
            return new File(new File(new File(MeteorClient.FOLDER, "musheor"), "kits"), kitName + ".json");
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    public void clearCache() {
        this.kitCache.clear();
        this.info("Cleared kit cache.", new Object[0]);
    }

    public void deleteKit(String name) {
        this.kitCache.remove(name);
        File file = this.kitFile(name);
        if (file != null && file.exists() && file.delete()) {
            this.info("Deleted kit '\u00a7b%s\u00a7r'.", new Object[]{name});
        }
    }

    public boolean kitExists(String name) {
        if (this.kitCache.containsKey(name)) {
            return true;
        }
        File file = this.kitFile(name);
        return file != null && file.exists();
    }

    public List<String> listKits() {
        File kitsDir = new File(new File(MeteorClient.FOLDER, "musheor"), "kits");
        if (!kitsDir.exists()) {
            return Collections.emptyList();
        }
        File[] files = kitsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) {
            return Collections.emptyList();
        }
        ArrayList<String> names = new ArrayList<String>();
        for (File f : files) {
            names.add(f.getName().replace(".json", ""));
        }
        return names;
    }

    public enum RekitMode {
        MANUAL,
        AUTO}
}

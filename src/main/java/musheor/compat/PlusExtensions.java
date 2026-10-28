/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.Settings
 *  meteordevelopment.meteorclient.systems.modules.Module
 */
package musheor.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;

public final class PlusExtensions {
    private static final Ext NONE = new Ext(){};
    private static final Map<Class<? extends Module>, List<Ext>> REGISTRY = new HashMap<Class<? extends Module>, List<Ext>>();

    private PlusExtensions() {
    }

    public static void register(Class<? extends Module> target, Ext ext) {
        REGISTRY.computeIfAbsent(target, k -> new ArrayList()).add(ext);
    }

    public static Ext attach(Module module, Settings settings) {
        List<Ext> exts = REGISTRY.get(module.getClass());
        if (exts == null || exts.isEmpty()) {
            return NONE;
        }
        Ext ext = exts.size() == 1 ? exts.get(0) : new Composite(List.copyOf(exts));
        ext.onInit(module, settings.createGroup("Musheor+"));
        return ext;
    }

    public static interface Ext {
        default public void onInit(Module module, SettingGroup group) {
        }

        default public void onActivate(Module module) {
        }

        default public void onDeactivate(Module module) {
        }

        default public void onTick(Module module) {
        }

        default public boolean suppressSwap(Module module) {
            return false;
        }
    }

    private record Composite(List<Ext> delegates) implements Ext
    {
        @Override
        public void onInit(Module module, SettingGroup group) {
            for (Ext ext : this.delegates) {
                ext.onInit(module, group);
            }
        }

        @Override
        public void onActivate(Module module) {
            for (Ext ext : this.delegates) {
                ext.onActivate(module);
            }
        }

        @Override
        public void onDeactivate(Module module) {
            for (Ext ext : this.delegates) {
                ext.onDeactivate(module);
            }
        }

        @Override
        public void onTick(Module module) {
            for (Ext ext : this.delegates) {
                ext.onTick(module);
            }
        }

        @Override
        public boolean suppressSwap(Module module) {
            for (Ext ext : this.delegates) {
                if (!ext.suppressSwap(module)) continue;
                return true;
            }
            return false;
        }
    }
}

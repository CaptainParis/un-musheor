package musheor.accessor;

import java.util.Comparator;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
	@Accessor("ENTRY_ORDERING")
	static Comparator<PlayerListEntry> musheor$getEntryOrdering() {
		throw new AssertionError();
	}
}

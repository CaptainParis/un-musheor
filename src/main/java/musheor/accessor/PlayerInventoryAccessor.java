package musheor.accessor;

import java.util.List;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {
	@Accessor("selectedSlot")
	int musheor$getSelectedSlot();

	@Accessor("selectedSlot")
	void musheor$setSelectedSlot(int slot);

	@Accessor("main")
	DefaultedList<ItemStack> musheor$getMain();
}

package musheor.mixin;

import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.AxeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AxeItem.class})
public interface AxeItemAccessor {
   @Accessor("field_7898")
   static Map<Block, Block> getStrippedBlocks() {
      throw new AssertionError();
   }
}

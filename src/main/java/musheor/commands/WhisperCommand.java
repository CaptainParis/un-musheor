package musheor.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import musheor.compat.VersionHelper;
import musheor.modules.tech.Whisper;
import musheor.utils.ModuleControl;
import net.minecraft.command.CommandSource;

public class WhisperCommand extends Command {
   public static String lastTarget;
   static int antiSpamLength = (Integer)((Whisper)Modules.get().get(Whisper.class)).antiSpamLength.get();

   public WhisperCommand() {
      super("tp", "Sends a random message to another player.", new String[0]);
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
         if (VersionHelper.get().getName(context) != null) {
            lastTarget = VersionHelper.get().getName(context);
         }

         antiSpamLength = (Integer)((Whisper)Modules.get().get(Whisper.class)).antiSpamLength.get();
         ModuleControl.sendTpCommand(lastTarget, antiSpamLength);
         return 1;
      }));
      builder.executes(context -> {
         antiSpamLength = (Integer)((Whisper)Modules.get().get(Whisper.class)).antiSpamLength.get();
         ModuleControl.sendTpCommand(lastTarget, antiSpamLength);
         return 1;
      });
   }
}

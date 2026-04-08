package com.lx862.takeaseat.commands;

import com.lx862.takeaseat.TakeASeat;
import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;

public class TakeASeatCommand {
    public static void register(String command, CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(command)
            .requires(Permissions.require("takeaseat.reload", PermissionLevel.GAMEMASTERS))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            TakeASeat.getConfig().load();
                            context.getSource().sendSuccess(() -> Component.literal("Config reloaded.").withStyle(ChatFormatting.GREEN), false);
                            return 1;
                        })
                )
        );
    }
}
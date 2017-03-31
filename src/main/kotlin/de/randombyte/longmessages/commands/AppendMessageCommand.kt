package de.randombyte.longmessages.commands

import de.randombyte.kosp.PlayerExecutedCommand
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.entity.living.player.Player
import java.util.*

internal class AppendMessageCommand(
        val appendMessage: (UUID, String) -> Unit,
        val sendSuccessMessage: (Player, String) -> Unit
) : PlayerExecutedCommand() {

    internal companion object {
        const val MESSAGE_ARG = "message"
    }

    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        val message = args.getOne<String>(MESSAGE_ARG).get()

        appendMessage(player.uniqueId, message)
        sendSuccessMessage(player, message)

        return CommandResult.success()
    }
}
package de.randombyte.longmessages.commands

import de.randombyte.kosp.PlayerExecutedCommand
import de.randombyte.kosp.extensions.toText
import de.randombyte.kosp.extensions.yellow
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.entity.living.player.Player
import java.util.*

internal class DeleteMessageCommand(
        val isMessageStored: (UUID) -> Boolean,
        val isCommand: (UUID) -> Boolean,
        val deleteMessage: (UUID) -> Unit
) : PlayerExecutedCommand() {
    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        if (!isMessageStored(player.uniqueId))
            throw CommandException("No message stored to delete!".toText())

        val isCommand = isCommand(player.uniqueId)

        deleteMessage(player.uniqueId)

        val objectName = if (isCommand) "Command" else "Message"
        player.sendMessage("$objectName deleted!".yellow())

        return CommandResult.success()
    }
}
package de.randombyte.longmessages.commands

import de.randombyte.kosp.PlayerExecutedCommand
import de.randombyte.kosp.extensions.*
import de.randombyte.kosp.getServiceOrFail
import de.randombyte.longmessages.LongMessages.Companion.COMMAND_PREFIX
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.cause.NamedCause
import org.spongepowered.api.service.pagination.PaginationService
import org.spongepowered.api.text.action.TextActions.executeCallback
import org.spongepowered.api.text.action.TextActions.suggestCommand
import java.util.*

internal class ShowMessageCommand(
        val getMessage: (UUID) -> String?,
        val isCommand: (UUID) -> Boolean,
        val deleteMessage: (UUID) -> Unit
) : PlayerExecutedCommand() {

    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        val storedMessage = getMessage(player.uniqueId)
        if (storedMessage == null) {
            player.sendMessage("No message stored yet!".yellow())
            return CommandResult.success()
        }

        val isCommand = isCommand(player.uniqueId)
        val storedObjectNameString = if (isCommand) "command" else "message"
        val sendTextButtonString = getSendTextButtonString(isCommand)
        val getDeleteTextButtonString = getDeleteTextButtonString(isCommand) + " "

        getServiceOrFail(PaginationService::class).builder()
                .title("LongMessages - $storedObjectNameString".aqua())
                .header(getDeleteTextButtonString.red().action(suggestCommand("/longmessages delete")) +
                        sendTextButtonString.yellow().action(executeCallback { currentPlayer ->
                            if (currentPlayer is Player) {
                                val currentStoredMessage = getMessage(currentPlayer.uniqueId)
                                if (currentStoredMessage != null) {
                                    if (isCommand(currentPlayer.uniqueId)) {
                                        val command = currentStoredMessage.removePrefix(COMMAND_PREFIX)
                                        currentPlayer.executeCommand(command)
                                    } else {
                                        currentPlayer.simulateChat(currentStoredMessage.toText(),
                                                Cause.of(NamedCause.simulated(player)))
                                    }
                                    deleteMessage(currentPlayer.uniqueId)
                                } else {
                                    player.sendMessage("No message stored yet!".yellow())
                                }
                            }
                        }))
                .contents("\"".gray() + storedMessage + "\"".gray())
                .build().sendTo(player)

        return CommandResult.success()
    }

    private fun getSendTextButtonString(isCommand: Boolean) =
            if (isCommand) "[Execute command]" else "[Send message]"

    private fun getDeleteTextButtonString(isCommand: Boolean) =
            if (isCommand) "[Delete command]" else "[Delete message]"

}
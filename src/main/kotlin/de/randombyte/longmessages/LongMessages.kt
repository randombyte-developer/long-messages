package de.randombyte.longmessages

import com.google.inject.Inject
import de.randombyte.kosp.bstats.BStats
import de.randombyte.kosp.config.ConfigManager
import de.randombyte.kosp.extensions.*
import de.randombyte.longmessages.commands.AppendMessageCommand
import de.randombyte.longmessages.commands.DeleteMessageCommand
import de.randombyte.longmessages.commands.ShowMessageCommand
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.args.GenericArguments.optional
import org.spongepowered.api.command.args.GenericArguments.remainingRawJoinedStrings
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.message.MessageChannelEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.TextActions.runCommand
import java.util.*

@Plugin(id = LongMessages.ID, name = LongMessages.NAME, version = LongMessages.VERSION, authors = arrayOf(LongMessages.AUTHOR))
class LongMessages @Inject constructor(
        val logger: Logger,
        @DefaultConfig(sharedRoot = true) configurationLoader: ConfigurationLoader<CommentedConfigurationNode>,
        val bStats: BStats
) {

    internal companion object {
        const val NAME = "LongMessages"
        const val ID = "long-messages"
        const val VERSION = "2.0.1"
        const val AUTHOR = "RandomByte"

        const val ROOT_PERMISSION = "longmessages"

        const val COMMAND_PREFIX = "/"
    }

    private val configManager = ConfigManager(configurationLoader, clazz = Config::class.java)

    private val storedMessages: MutableMap<UUID, String> = mutableMapOf()

    private val appendMessageCommand = AppendMessageCommand(
            appendMessage = this::appendMessage,
            sendSuccessMessage = { player, message -> player.sendMessage(getSuccessMessage(message)) })

    private val showMessageCommand = ShowMessageCommand(
            getMessage = storedMessages::get,
            isCommand = { playerUuid -> isCommand(storedMessages.getValue(playerUuid)) },
            deleteMessage = { storedMessages.remove(it) })

    @Listener
    fun onInit(event: GameInitializationEvent) {
        configManager.save(configManager.get()) // adds missing nodes

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .permission(ROOT_PERMISSION)
                .arguments(optional(remainingRawJoinedStrings(AppendMessageCommand.MESSAGE_ARG.toText())))
                .executor { src, args ->
                    // redirect based upon if arg is provided
                    if (args.hasAny(AppendMessageCommand.MESSAGE_ARG))
                        appendMessageCommand.execute(src, args)
                    else showMessageCommand.execute(src, args)
                }
                .child(CommandSpec.builder()
                        .executor(DeleteMessageCommand(
                                isMessageStored = storedMessages::containsKey,
                                isCommand = { playerUuid -> isCommand(storedMessages.getValue(playerUuid)) },
                                deleteMessage = { storedMessages.remove(it) }))
                        .build(), "delete")
                .build(), "longmessages", "lmsgs")
        logger.info("$NAME loaded: $VERSION")
    }

    @Listener(order = Order.FIRST) // to cancel the event before other plugins receive it
    fun onMessage(event: MessageChannelEvent.Chat, @First player: Player) {
        if (!player.hasPermission(ROOT_PERMISSION)) return

        val config = configManager.get()

        val plainMessage = event.rawMessage.toPlain()
        val triggerCharacters = config.triggerCharacters
        val triggerCharsAsPrefix = config.triggerCharactersAsPrefix
        val rawMessage = getRawMessage(plainMessage, triggerCharacters, triggerCharsAsPrefix)

        if (rawMessage != null) {
            appendMessage(player.uniqueId, rawMessage)
            player.sendMessage(getSuccessMessage(rawMessage))
            event.isMessageCancelled = true
        }
    }

    @Listener
    fun onDisconnect(event: ClientConnectionEvent.Disconnect) {
        storedMessages.remove(event.targetEntity.uniqueId)
    }

    private fun getRawMessage(message: String, triggerCharacters: List<String>, triggerCharsAsPrefix: Boolean): String? {
        fun checkFront(trigger: String): String? =
                if (triggerCharsAsPrefix && message.startsWith(trigger)) message.removePrefix(trigger) else null

        fun checkBack(trigger: String): String? =
                if (message.endsWith(trigger)) message.removeSuffix(trigger) else null

        // first check only the front for trigger characters
        triggerCharacters.forEach { trigger ->
            val rawMessage = checkFront(trigger)
            if (rawMessage != null) return rawMessage
        }

        // also check the end of the message for trigger characters
        triggerCharacters.forEach { trigger ->
            val rawMessage = checkBack(trigger)
            if (rawMessage != null) return rawMessage
        }

        return null
    }

    private fun appendMessage(playerUuid: UUID, message: String) {
        val storedMessage = storedMessages[playerUuid]?.plus(" ") ?: ""
        val newMessage = storedMessage + message
        storedMessages.put(playerUuid, newMessage)
    }

    private fun getSuccessMessage(appendedMessage: String): Text {
        val isCommand = isCommand(appendedMessage)
        val name1 = if (isCommand) "Command" else "Message"
        val name2 = if (isCommand) "command" else "message"
        return "$name1 stored: ".green() + "\"".gray() + appendedMessage + "\"\n".gray() +
                "See your whole stored $name2: [/longmessages]".aqua().action(runCommand("/longmessages"))
    }

    private fun isCommand(storedMessage: String) = storedMessage.startsWith(COMMAND_PREFIX)
}
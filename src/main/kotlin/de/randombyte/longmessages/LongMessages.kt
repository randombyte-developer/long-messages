package de.randombyte.longmessages

import com.google.inject.Inject
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.message.MessageChannelEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.channel.MessageChannel
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.util.Identifiable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Plugin(id = LongMessages.ID, name = LongMessages.NAME, version = LongMessages.VERSION, authors = arrayOf(LongMessages.AUTHOR))
class LongMessages @Inject constructor(val logger: Logger) {

    companion object {
        const val NAME = "LongMessages"
        const val ID = "de.randombyte.longmessages"
        const val VERSION = "v1.0.1"
        const val AUTHOR = "RandomByte"

        val CONTINUE_CHAR = "+"
        val SPACE_CHAR = "_"
    }

    private val storedMessages = HashMap<String, MutableList<Text>>()

    private val Identifiable.uuid: String
        get() = uniqueId.toString()

    private fun String.lastChar() = this[lastIndex].toString()

    @Listener
    fun onInit(event: GameInitializationEvent) {
        val deleteStoredMessages = CommandSpec.builder()
            .description(Text.of("Deletes all stored messages"))
            .executor { src, ctx ->
                if (src is Player) {
                    storedMessages.remove(src.uuid)
                    src.sendMessage(Text.of(TextColors.YELLOW, "Deleted stored messages!"))
                    CommandResult.success()
                } else {
                    src.sendMessage(Text.of(TextColors.YELLOW, "This command must be executed by a player!"))
                    CommandResult.empty()
                }
            }
            .build()
        Sponge.getCommandManager().register(this, deleteStoredMessages, "deleteStoredMessage", "dsm", "d")
        logger.info("$NAME loaded: $VERSION")
    }

    @Listener
    fun onMessage(event: MessageChannelEvent.Chat, @First player: Player) {
        if (!player.hasPermission("longmessages.use")) return

        if (handleMessage(event.rawMessage, player)) {
            event.isCancelled = true
        } else if (storedMessages.containsKey(player.uuid)) {
            handleSendStoredMessages(event.rawMessage, player, event.channel.orElseGet { null })
            event.isCancelled = true
        } else return
    }

    @Listener
    fun onLogin(event: ClientConnectionEvent.Login) {
        val lastPlayedOpt = event.targetUser.getValue(Keys.LAST_DATE_PLAYED)
        if (!lastPlayedOpt.isPresent) return
        if (lastPlayedOpt.get().get().plus(5, ChronoUnit.MINUTES).toEpochMilli() < Instant.now().toEpochMilli()) {
            //Five minutes passed after last seen on this server -> delete storedMessages for this Player
            storedMessages.remove(event.targetUser.uuid)
        }
    }

    /**
     * @return If message was stored
     */
    private fun handleMessage(message: Text, player: Player): Boolean {
        fun String.removeLastChar() = substring(0, lastIndex)
        val plainMessage = when (message.toPlain().lastChar()) {
            SPACE_CHAR -> message.toPlain().removeLastChar() + " "
            CONTINUE_CHAR -> message.toPlain().removeLastChar()
            else -> return false
        }
        if (plainMessage.isBlank()) return false //Don't store blank messages
        val preparedMessage = Text.builder(message, plainMessage).build()
        val messages = storedMessages[player.uuid] ?: ArrayList<Text>()
        messages.add(preparedMessage)
        storedMessages[player.uuid] = messages
        player.sendMessage(Text.of(TextColors.YELLOW, "Stored: ", TextColors.GRAY, preparedMessage))
        return true
    }

    private fun handleSendStoredMessages(lastMessage: Text, player: Player, channel: MessageChannel?) {
        val builder = Text.builder()
        builder
            .append(Text.of("<", player.name, "> "))
            .append(storedMessages[player.uuid]!!)
            .append(lastMessage)
        storedMessages.remove(player.uuid)
        (channel ?: Sponge.getServer().broadcastChannel).send(player, builder.build())
    }
}
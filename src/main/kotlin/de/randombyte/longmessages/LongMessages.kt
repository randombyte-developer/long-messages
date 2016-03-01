package de.randombyte.longmessages

import com.google.inject.Inject
import me.flibio.updatifier.Updatifier
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.message.MessageChannelEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import java.util.*

@Plugin(id = PluginInfo.ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, dependencies = "after: Updatifier")
@Updatifier(repoName = PluginInfo.NAME, repoOwner = "randombyte-developer", version = PluginInfo.VERSION)
class LongMessages {

    private val CONTINUE_CHAR = "+"
    private val SPACE_CHAR = "_"

    @Inject private lateinit var logger: Logger;
    private val storedMessages = HashMap<String, MutableList<Text>>()

    private val Player.uuid: String
        get() = uniqueId.toString()

    private fun String.lastChar() = this[lastIndex].toString()

    @Listener
    fun onInit(event: GameInitializationEvent) {
        logger.info("${PluginInfo.NAME} loaded: ${PluginInfo.VERSION}")
    }

    @Listener
    fun onMessage(event: MessageChannelEvent.Chat, @First player: Player) {
        if (handleMessage(event.rawMessage, player)) {
            event.setMessage(null)
        } else if (storedMessages.containsKey(player.uuid)) {
            handleSendStoredMessages(event.rawMessage, player)
            event.setMessage(null)
        } else return
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
        val preparedMessage = Text.builder(message, plainMessage).build()
        val messages = storedMessages[player.uuid] ?: ArrayList<Text>()
        messages.add(preparedMessage)
        storedMessages[player.uuid] = messages
        player.sendMessage(Text.of(TextColors.YELLOW, "Stored: ", TextColors.GRAY, preparedMessage))
        return true
    }

    private fun handleSendStoredMessages(lastMessage: Text, player: Player) {
        val builder = Text.builder()
        builder.append(storedMessages[player.uuid]!!).append(lastMessage)
        storedMessages.remove(player.uuid)
        Sponge.getServer().broadcastChannel.send(player, builder.build())
    }
}
package com.dongtronic.diabot.platforms.discord.listeners

import com.dongtronic.diabot.commands.CommandUpdateHandler
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.EventListener


/**
 * Handler for when an author deletes their command execution message, which causes a deletion of any replies to the
 * author's message.
 *
 * @param maxReplies The maximum number of replies to track in history. Once the number of replies exceeds this value,
 * the oldest replies will no longer be tracked for deletion.
 */
class JDACommandUpdateHandler(maxReplies: Int) : CommandUpdateHandler<MessageDeleteEvent>(), EventListener {
    override val responses = limitedMap<String, Array<String>>(maxReplies)

    override fun onEvent(event: GenericEvent) {
        if (event is MessageDeleteEvent) {
            onMessageDelete(event)
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        val responseIds = responses[event.messageId]

        if (responseIds != null) {
            responseIds.forEach { responseId ->
                event.channel.deleteMessageById(responseId).queue()
            }
            responses.remove(event.messageId)
        }
    }
}
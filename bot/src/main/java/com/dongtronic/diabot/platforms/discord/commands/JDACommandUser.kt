package com.dongtronic.diabot.platforms.discord.commands

import cloud.commandframework.jda.JDACommandSender
import com.dongtronic.diabot.commands.CommandUser
import com.dongtronic.diabot.commands.ReplyType
import com.dongtronic.diabot.nameOf
import com.dongtronic.diabot.platforms.discord.listeners.JDACommandUpdateHandler
import com.dongtronic.diabot.submitMono
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import reactor.core.publisher.Mono

class JDACommandUser(
        event: MessageReceivedEvent,
        mapper: ResponseEmojiMapper = ResponseEmojiMapper(),
        updateHandler: JDACommandUpdateHandler? = null
) : CommandUser<MessageReceivedEvent, Message>(event, mapper, updateHandler) {
    override fun getAuthorName(): String {
        return event.author.name
    }

    override fun getAuthorDisplayName(): String {
        return event.nameOf(event.author)
    }

    override fun getAuthorUniqueId(): String {
        return event.author.id
    }

    override fun getAuthorMention(): String {
        return event.author.asMention
    }

    override fun deleteAuthorMessage(reason: String?): Mono<Boolean> {
        return event.message.delete()
                .reason(reason)
                .submitMono()
                .subscribeOn(defaultScheduler)
                .map { true }
    }

    override fun reply(message: Message, type: ReplyType, markReply: Boolean): Mono<Message> {
        val action = when (type) {
            ReplyType.NATIVE_REPLY -> event.message.reply(message)
            ReplyType.NONE -> event.channel.sendMessage(message)
            ReplyType.MENTION -> {
                // insert mention at beginning
                val messageBuilder = MessageBuilder(message)
                messageBuilder.stringBuilder.insert(0, getAuthorMention() + ' ')
                event.channel.sendMessage(messageBuilder.build())
            }
        }

        var mono = action.submitMono().subscribeOn(defaultScheduler)

        if (markReply) {
            mono = mono.doOnNext { updateHandler?.markReply(event.message.id, it.id) }
        }

        return mono
    }

    override fun reply(message: CharSequence, type: ReplyType, markReply: Boolean): Mono<Message> {
        return reply(MessageBuilder(message).build(), type, markReply)
    }

    /**
     * Replies to the author's message with an embed and optionally marks the reply IDs for later deletion.
     *
     * @param embed Message embed to send
     * @param type Method of replying
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Mono] of the sent message
     */
    fun reply(embed: MessageEmbed, type: ReplyType = defaultReplyType, markReply: Boolean = true): Mono<Message> {
        return reply(MessageBuilder(embed).build(), type, markReply)
    }

    override fun react(unicode: String): Mono<Boolean> {
        return event.message
                .addReaction(unicode)
                .submitMono()
                .subscribeOn(defaultScheduler)
                .map { true }
    }

    /**
     * Converts this [JDACommandUser] object (Diabot) into a [JDACommandSender] object (cloud)
     *
     * @return Converted [JDACommandSender]
     */
    fun toJdaCommandSender(): JDACommandSender {
        return JDACommandSender.of(event)
    }

    companion object {
        private val emojiMapper = ResponseEmojiMapper()

        /**
         * Converts a [JDACommandSender] object (cloud) into a [JDACommandUser] object (Diabot)
         *
         * @param jdaCommandSender [JDACommandSender] to convert
         * @param listener An optional [JDACommandUpdateHandler] to delete replies when an author deletes their message
         * @return Converted [JDACommandUser]
         */
        fun of(jdaCommandSender: JDACommandSender, listener: JDACommandUpdateHandler? = null): JDACommandUser {
            return JDACommandUser(jdaCommandSender.event.get(), emojiMapper, listener)
        }

        /**
         * Converts a [MessageReceivedEvent] into a [JDACommandUser]
         *
         * @param event [MessageReceivedEvent] to convert
         * @param listener An optional [JDACommandUpdateHandler] to delete replies when an author deletes their message
         * @return Converted [JDACommandUser]
         */
        fun of(event: MessageReceivedEvent, listener: JDACommandUpdateHandler? = null): JDACommandUser {
            return JDACommandUser(event, emojiMapper, listener)
        }
    }
}
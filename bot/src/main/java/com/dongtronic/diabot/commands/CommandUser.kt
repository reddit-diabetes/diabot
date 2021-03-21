package com.dongtronic.diabot.commands

import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

/**
 * Generic command user object for Diabot.
 *
 * @param E Message event type
 * @param M Message type
 * @property event Message event linked to this user
 * @property responseLevelMapper A mapper from [ResponseLevel]s to [String]s
 * @property updateHandler An optional [CommandUpdateHandler] to track message updates that should modify or delete
 * a response from the bot
 * @property defaultReplyType The default [ReplyType] for this platform
 * @property defaultScheduler The default [Scheduler] to subscribe on for auto-subscribe functions and for functions
 * which return a publisher.
 */
abstract class CommandUser<E, M>(
        val event: E,
        val responseLevelMapper: ResponseLevelMapper,
        val updateHandler: CommandUpdateHandler<*>? = null
) {
    open val defaultReplyType = ReplyType.NATIVE_REPLY
    open val defaultScheduler: Scheduler = Schedulers.boundedElastic()

    /**
     * Gets the name of the message author that executed the command.
     *
     * @return Author's name
     */
    abstract fun getAuthorName(): String

    /**
     * Gets the display name of the message author that executed the command.
     *
     * @return Author's display name
     */
    abstract fun getAuthorDisplayName(): String

    /**
     * Gets the unique user ID for the executor of the command.
     *
     * @return Author's unique user ID
     */
    abstract fun getAuthorUniqueId(): String

    /**
     * Gets the command executioner's mention tag.
     *
     * If the platform does not provide such a function (mentioning users) this should throw [NotImplementedError].
     *
     * @return A mention for the author
     * @throws NotImplementedError if the platform does not support mentioning users.
     */
    @Throws(NotImplementedError::class)
    abstract fun getAuthorMention(): String

    /**
     * Deletes the message which executed the command.
     *
     * @param reason An audit-log reason for deleting, if applicable
     * @return A [Mono] returning whether the deletion was successful
     */
    abstract fun deleteAuthorMessage(reason: String? = null): Mono<Boolean>

    //
    // Message Responses
    //

    /**
     * Replies to the author's message with a raw message and optionally marks the reply IDs for later deletion.
     * 
     * @param message Message to send
     * @param type Method of replying
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Mono] of the sent message
     */
    abstract fun reply(message: M, type: ReplyType = defaultReplyType, markReply: Boolean = true): Mono<M>

    /**
     * Replies to the author's message with a raw message and optionally marks the reply IDs for later deletion.
     *
     * @param message Message to send
     * @param type Method of replying
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Mono] of the sent message
     */
    abstract fun reply(message: CharSequence, type: ReplyType = defaultReplyType, markReply: Boolean = true): Mono<M>

    /**
     * Replies to the author's message with a response level and optionally marks the reply IDs for later deletion.
     *
     * @param responseLevel Response level
     * @param message Message to send
     * @param type Method of replying
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Mono] of the sent message
     */
    fun reply(
            responseLevel: ResponseLevel,
            message: CharSequence,
            type: ReplyType = defaultReplyType,
            markReply: Boolean = true
    ): Mono<M> =
            reply(responseLevelMapper.getResponseIndicator(responseLevel) + message, type, markReply)

    /**
     * Replies to the author's message with a [ResponseLevel.SUCCESS] response level and optionally marks the reply IDs
     * for later deletion.
     *
     * @param message Message to send
     * @param type Method of replying
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Mono] of the sent message
     */
    fun replySuccess(message: CharSequence, type: ReplyType = defaultReplyType, markReply: Boolean = true): Mono<M> =
            reply(ResponseLevel.SUCCESS, message, type, markReply)

    /**
     * Replies to the author's message with a [ResponseLevel.WARNING] response level and optionally marks the reply IDs
     * for later deletion.
     *
     * @param message Message to send
     * @param type Method of replying
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Mono] of the sent message
     */
    fun replyWarning(message: CharSequence, type: ReplyType = defaultReplyType, markReply: Boolean = true): Mono<M> =
            reply(ResponseLevel.WARNING, message, type, markReply)

    /**
     * Replies to the author's message with a [ResponseLevel.ERROR] response level and optionally marks the reply IDs
     * for later deletion.
     *
     * @param message Message to send
     * @param type Method of replying
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Mono] of the sent message
     */
    fun replyError(message: CharSequence, type: ReplyType = defaultReplyType, markReply: Boolean = true): Mono<M> =
            reply(ResponseLevel.ERROR, message, type, markReply)

    //
    // Message Responses (auto-subscribe)
    //

    /**
     * Replies to the author's message with a raw message and optionally marks the reply IDs for later deletion.
     *
     * Auto-subscribing version of [CommandUser.reply].
     *
     * @param message Message to send
     * @param type Method of replying
     * @param scheduler The scheduler to subscribe on
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Disposable] of the subscription
     */
    fun replyS(
            message: M,
            type: ReplyType = defaultReplyType,
            scheduler: Scheduler = defaultScheduler,
            markReply: Boolean = true
    ): Disposable =
            reply(message, type, markReply)
                    .subscribeOn(scheduler).subscribe()

    /**
     * Replies to the author's message with a raw message and optionally marks the reply IDs for later deletion.
     *
     * Auto-subscribing version of [CommandUser.reply].
     *
     * @param message Message to send
     * @param type Method of replying
     * @param scheduler The scheduler to subscribe on
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Disposable] of the subscription
     */
    fun replyS(
            message: CharSequence,
            type: ReplyType = defaultReplyType,
            scheduler: Scheduler = defaultScheduler,
            markReply: Boolean = true
    ): Disposable =
            reply(message, type, markReply)
                    .subscribeOn(scheduler).subscribe()

    /**
     * Replies to the author's message with a response level and optionally marks the reply IDs for later deletion.
     *
     * Auto-subscribing version of [CommandUser.reply].
     *
     * @param responseLevel Response level
     * @param message Message to send
     * @param type Method of replying
     * @param scheduler The scheduler to subscribe on
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Disposable] of the subscription
     */
    fun replyS(
            responseLevel: ResponseLevel,
            message: CharSequence,
            type: ReplyType = defaultReplyType,
            scheduler: Scheduler = defaultScheduler,
            markReply: Boolean = true
    ): Disposable =
            reply(responseLevelMapper.getResponseIndicator(responseLevel) + message, type, markReply)
                    .subscribeOn(scheduler).subscribe()

    /**
     * Replies to the author's message with a [ResponseLevel.SUCCESS] response level and optionally marks the reply IDs
     * for later deletion.
     *
     * Auto-subscribing version of [CommandUser.replySuccess].
     *
     * @param message Message to send
     * @param type Method of replying
     * @param scheduler The scheduler to subscribe on
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Disposable] of the subscription
     */
    fun replySuccessS(
            message: CharSequence,
            type: ReplyType = defaultReplyType,
            scheduler: Scheduler = defaultScheduler,
            markReply: Boolean = true
    ): Disposable =
            reply(ResponseLevel.SUCCESS, message, type, markReply)
                    .subscribeOn(scheduler).subscribe()

    /**
     * Replies to the author's message with a [ResponseLevel.WARNING] response level and optionally marks the reply IDs
     * for later deletion.
     *
     * Auto-subscribing version of [CommandUser.replyWarning].
     *
     * @param message Message to send
     * @param type Method of replying
     * @param scheduler The scheduler to subscribe on
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Disposable] of the subscription
     */
    fun replyWarningS(
            message: CharSequence,
            type: ReplyType = defaultReplyType,
            scheduler: Scheduler = defaultScheduler,
            markReply: Boolean = true
    ): Disposable =
            reply(ResponseLevel.WARNING, message, type, markReply)
                    .subscribeOn(scheduler).subscribe()

    /**
     * Replies to the author's message with a [ResponseLevel.ERROR] response level and optionally marks the reply IDs
     * for later deletion.
     *
     * Auto-subscribing version of [CommandUser.replyError].
     *
     * @param message Message to send
     * @param type Method of replying
     * @param scheduler The scheduler to subscribe on
     * @param markReply If the reply's IDs should be marked for later deletion
     * @return A [Disposable] of the subscription
     */
    fun replyErrorS(
            message: CharSequence,
            type: ReplyType = defaultReplyType,
            scheduler: Scheduler = defaultScheduler,
            markReply: Boolean = true
    ): Disposable =
            reply(ResponseLevel.ERROR, message, type, markReply)
                    .subscribeOn(scheduler).subscribe()

    //
    // Reactions
    //

    /**
     * Add a reaction to the author's message.
     *
     * @param unicode The unicode/custom emote to react to the message with
     * @return A [Mono] denoting whether the reaction was successfully applied
     */
    abstract fun react(unicode: String): Mono<Boolean>

    /**
     * Add a reaction to the author's message by the [ResponseLevel].
     *
     * @param responseLevel The response level to react with
     * @return A [Mono] denoting whether the reaction was successfully applied
     */
    fun react(responseLevel: ResponseLevel): Mono<Boolean> =
            react(responseLevelMapper.getResponseIndicator(responseLevel, false))

    /**
     * Add a reaction to the author's message from the [ResponseLevel.SUCCESS] value.
     *
     * @return A [Mono] denoting whether the reaction was successfully applied
     */
    fun reactSuccess(): Mono<Boolean> =
            react(ResponseLevel.SUCCESS)

    /**
     * Add a reaction to the author's message from the [ResponseLevel.WARNING] value.
     *
     * @return A [Mono] denoting whether the reaction was successfully applied
     */
    fun reactWarning(): Mono<Boolean> =
            react(ResponseLevel.WARNING)

    /**
     * Add a reaction to the author's message from the [ResponseLevel.ERROR] value.
     *
     * @return A [Mono] denoting whether the reaction was successfully applied
     */
    fun reactError(): Mono<Boolean> =
            react(ResponseLevel.ERROR)

    // Reactions (auto-subscribe)

    /**
     * Add a reaction to the author's message.
     *
     * Auto-subscribing version of [CommandUser.react].
     *
     * @param unicode The unicode/custom emote to react to the message with
     * @param scheduler The scheduler to subscribe on
     * @return A [Disposable] of the subscription
     */
    fun reactS(
            unicode: String,
            scheduler: Scheduler = defaultScheduler
    ): Disposable =
            react(unicode).subscribeOn(scheduler).subscribe()

    /**
     * Add a reaction to the author's message by the [ResponseLevel].
     *
     * Auto-subscribing version of [CommandUser.react].
     *
     * @param responseLevel The response level to react with
     * @param scheduler The scheduler to subscribe on
     * @return A [Disposable] of the subscription
     */
    fun reactS(
            responseLevel: ResponseLevel,
            scheduler: Scheduler = defaultScheduler
    ): Disposable =
            react(responseLevel).subscribeOn(scheduler).subscribe()

    /**
     * Add a reaction to the author's message from the [ResponseLevel.SUCCESS] value.
     *
     * Auto-subscribing version of [CommandUser.reactSuccess].
     *
     * @param scheduler The scheduler to subscribe on
     * @return A [Disposable] of the subscription
     */
    fun reactSuccessS(scheduler: Scheduler = defaultScheduler): Disposable =
            react(ResponseLevel.SUCCESS).subscribeOn(scheduler).subscribe()

    /**
     * Add a reaction to the author's message from the [ResponseLevel.WARNING] value.
     *
     * Auto-subscribing version of [CommandUser.reactWarning].
     *
     * @param scheduler The scheduler to subscribe on
     * @return A [Disposable] of the subscription
     */
    fun reactWarningS(scheduler: Scheduler = defaultScheduler): Disposable =
            react(ResponseLevel.WARNING).subscribeOn(scheduler).subscribe()

    /**
     * Add a reaction to the author's message from the [ResponseLevel.ERROR] value.
     *
     * Auto-subscribing version of [CommandUser.reactError].
     *
     * @param scheduler The scheduler to subscribe on
     * @return A [Disposable] of the subscription
     */
    fun reactErrorS(scheduler: Scheduler = defaultScheduler): Disposable =
            react(ResponseLevel.ERROR).subscribeOn(scheduler).subscribe()
}

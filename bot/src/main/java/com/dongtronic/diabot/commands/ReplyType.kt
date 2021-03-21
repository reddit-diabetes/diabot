package com.dongtronic.diabot.commands

/**
 * The kind of reply method to use when responding to a command sender.
 */
enum class ReplyType {
    /**
     * The native replying method built into the platform.
     * For Discord, this method is equal to right clicking on the original message and choosing "Reply".
     */
    NATIVE_REPLY,

    /**
     * Mention the command sender in the response.
     */
    MENTION,

    /**
     * Don't mark the message as a response to the authoring message.
     */
    NONE
}
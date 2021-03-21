package com.dongtronic.diabot.commands

import java.util.*

/**
 * Handler for when an author deletes their command execution message, which will cause a deletion of any replies to the
 * author's message.
 *
 * @param D Message delete event type
 */
abstract class CommandUpdateHandler<D> {
    /**
     * Map of command execution message IDs and the bot's response message ID(s)
     */
    abstract val responses: HashMap<String, Array<String>>

    /**
     * Handle when a message is deleted.
     *
     * @param event Message delete event
     */
    abstract fun onMessageDelete(event: D)

    /**
     * Log a command response message.
     *
     * @param executingId The message ID of the author's message (that executed the command to respond)
     * @param responseId The message ID of the command's response to the author
     */
    fun markReply(executingId: String, responseId: String) {
        responses.merge(executingId, arrayOf(responseId)) { old: Array<String>, new: Array<String> ->
            old.plus(new)
        }
    }

    /**
     * Makes a limited-capacity [LinkedHashMap]. Removal follows FIFO, meaning the oldest entries will be removed first.
     *
     * @param maxEntries The maximum number of entries to hold in the map before removing old ones
     * @return A [LinkedHashMap] whose size will never exceed [maxEntries]
     */
    fun <K, V> limitedMap(maxEntries: Int): LinkedHashMap<K, V> {
        return object : LinkedHashMap<K, V>(maxEntries, 1f, false) {
            override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
                return size > maxEntries
            }
        }
    }
}
package com.dongtronic.diabot.data.mongodb

import com.dongtronic.diabot.graph.GraphSettings
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands.DisplayOptions.Companion.optionsForDisplay
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands.DisplayOptions.Companion.sortOptions
import com.dongtronic.diabot.util.*
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.conversions.Bson
import org.litote.kmongo.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import kotlin.reflect.KProperty

class NightscoutDAO private constructor() {
    private val mongo = MongoDB.getInstance().database
    val collection: MongoCollection<NightscoutUserDTO>
            = mongo.getCollection(DiabotCollection.NIGHTSCOUT.getEnv(), NightscoutUserDTO::class.java)
    private val scheduler = Schedulers.boundedElastic()
    private val logger = logger()

    init {
        // Create a unique index
        val options = IndexOptions().unique(true)
        collection.createIndex(descending(NightscoutUserDTO::userId), options).toMono()
                .subscribeOn(scheduler)
                .subscribe()
    }

    /**
     * Gets a [NightscoutUserDTO] from a user ID
     *
     * @param userId The user ID to look up
     * @return [NightscoutUserDTO] for this user
     */
    fun getUser(userId: String): Mono<NightscoutUserDTO> {
        return collection.findOne(filter(userId)).subscribeOn(scheduler)
    }

    /**
     * Searches for users with the given Nightscout URL
     *
     * @param url The URL to look up
     * @return [NightscoutUserDTO]s with the given URL.
     */
    fun getUsersForURL(url: String): Flux<NightscoutUserDTO> {
        return collection.findMany(NightscoutUserDTO::url eq url)
                .subscribeOn(scheduler)
    }

    /**
     * Inserts a new user's NS data into the database.
     * This should only be used if it is known that the user does not exist already as it will error otherwise.
     *
     * @param dto The [NightscoutUserDTO] to be inserted
     * @return The result of the insertion
     */
    fun addUser(dto: NightscoutUserDTO): Mono<InsertOneResult> {
        return collection.insertOne(dto).toMono()
                .subscribeOn(scheduler)
    }

    /**
     * Deletes stored NS data belonging to the given user.
     * If no properties are given, all of their NS data will be deleted from the database.
     *
     * @param userId The user's ID.
     * @param fields Which fields to delete. If this is not provided then all of the user's data will be deleted.
     * @return Either a [UpdateResult] or [DeleteResult] representing the result of data deletion.
     * If `fields` is blank this will return [DeleteResult]. If not blank, [UpdateResult].
     */
    fun deleteUser(userId: String, vararg fields: KProperty<*>): Mono<*> {
        if (fields.isNullOrEmpty()) {
            // delete all of the user's data
            return collection.deleteOne(filter(userId)).toMono()
                    .subscribeOn(scheduler)
        }

        val unsets = fields.map { unset(it) }
        val combined = combine(unsets)
        return collection.updateOne(filter(userId), combined).toMono()
                .subscribeOn(scheduler)
    }

    /**
     * Updates or inserts a user's document in the database.
     *
     * @param dto The [NightscoutUserDTO] instance to update with
     * @return [UpdateResult]
     */
    fun replaceUser(dto: NightscoutUserDTO): Mono<UpdateResult> {
        return collection.replaceOne(filter(dto.userId), dto, replaceUpsert()).toMono()
                .subscribeOn(scheduler)
    }

    /**
     * Sets a user's NS URL.
     *
     * @param userId The user's ID
     * @param url The user's Nightscout URL
     * @return The result of setting their URL
     */
    fun setUrl(userId: String, url: String): Mono<UpdateResult> {
        val updates = combine(
                setValue(NightscoutUserDTO::url, url),
                unset(NightscoutUserDTO::token)
        )

        return collection.updateOne(NightscoutUserDTO::userId eq userId, updates, upsert())
                .toMono()
                .subscribeOn(scheduler)
    }

    /**
     * Changes a user's NS privacy settings for a guild.
     *
     * @param userId The user ID to change privacy under.
     * @param guildId The guild ID to change privacy under.
     * @param public Whether or not this guild should be set as public or private.
     * If this is null, the privacy will be toggled instead.
     * @return This user's new privacy setting for the given guild.
     */
    fun changePrivacy(userId: String, guildId: String, public: Boolean? = null): Mono<Boolean> {
        val upsertAfter = findOneAndUpdateUpsert().returnDocument(ReturnDocument.AFTER)
        val userFilter = filter(userId)
        val newState = if (public != null) {
            Mono.just(public)
        } else {
            val filter = and(userFilter, NightscoutUserDTO::publicGuilds `in` listOf(guildId))
            collection.countDocuments(filter).toMono().map { it == 0L }
        }

        val update = newState.map {
            if (it) {
                addToSet(NightscoutUserDTO::publicGuilds, guildId)
            } else {
                pull(NightscoutUserDTO::publicGuilds, guildId)
            }
        }

        return update.flatMap { collection.findOneAndUpdate(userFilter, it, upsertAfter).toMono() }
                .map { it.publicGuilds.contains(guildId) }
                .subscribeOn(scheduler)
    }

    /**
     * Sets a token to be used when fetching data from a user's Nightscout.
     *
     * @param userId The user's ID
     * @param token The Nightscout token. If null, the token key will be deleted.
     * @return The result of setting the user's Nightscout token
     */
    fun setToken(userId: String, token: String?): Mono<UpdateResult> {
        if (token == null) {
            // delete the key instead of setting it to null
            return deleteUser(userId, NightscoutUserDTO::token).ofType(UpdateResult::class.java)
        }

        return collection.updateOne(NightscoutUserDTO::userId eq userId,
                setValue(NightscoutUserDTO::token, token), upsert())
                .toMono()
                .subscribeOn(scheduler)
    }

    /**
     * Changes a user's NS display settings.
     *
     * @param userId The user ID to change display settings for.
     * @param append The modification type for the display settings.
     * `TRUE` will append to the currently existing settings (if any).
     * `FALSE` will delete from the currently existing settings.
     * `NULL` will set the user's display settings to the ones provided.
     * @param displaySettings The display settings to update with.
     * @return The user's new display settings.
     */
    @Deprecated("Use updateDisplay(String, UpdateMode, Set) instead", ReplaceWith("updateDisplay(String, UpdateMode, Set)"))
    fun updateDisplay(userId: String, append: Boolean? = null, vararg displaySettings: String): Mono<List<String>> {
        val updateMode = when (append) {
            true -> UpdateMode.ADD
            false -> UpdateMode.DELETE
            null -> UpdateMode.SET
        }
        val settings = displaySettings.map {
            NightscoutDisplayCommands.DisplayOptions.valueOf(it.toUpperCase())
        }.toSet()

        return updateDisplay(userId, updateMode, settings).map { it.optionsForDisplay() }
    }

    /**
     * Changes a user's NS display settings.
     *
     * @param userId The user ID to change display settings for.
     * @param updateMode The modification type for the display settings.
     * @param displaySettings The display settings to update with. Using `null` will reset the settings to default.
     * @return The user's new display settings.
     */
    fun updateDisplay(
            userId: String,
            updateMode: UpdateMode,
            displaySettings: Set<NightscoutDisplayCommands.DisplayOptions>? = null
    ): Mono<Set<NightscoutDisplayCommands.DisplayOptions>> {
        if (displaySettings == null) {
            return deleteUser(userId, NightscoutUserDTO::displayOptions)
                    .map { emptySet<NightscoutDisplayCommands.DisplayOptions>() }
        }

        val settings = displaySettings.sortOptions()

        if (updateMode == UpdateMode.SET) {
            val upsertAfter = findOneAndUpdateUpsert().returnDocument(ReturnDocument.AFTER)
            val update = setValue(NightscoutUserDTO::displayOptions, settings)

            return collection.findOneAndUpdate(filter(userId), update, upsertAfter).toMono()
                    .map { it.displayOptions }
                    .subscribeOn(scheduler)
        }

        val user = getUser(userId).map {
            val currentOptions = it.displayOptions.toMutableSet()

            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (updateMode) {
                UpdateMode.ADD -> currentOptions.addAll(settings)
                UpdateMode.DELETE -> currentOptions.removeAll(settings)
            }

            it.copy(displayOptions = currentOptions)
        }.zipWhen { replaceUser(it) }

        return user
                .map { it.t1.displayOptions }
                .subscribeOn(scheduler)
    }

    /**
     * Changes a user's NS graph settings.
     *
     * @param userId The user ID to change graph settings for.
     * @param graphSettings The graph settings to set for the user. If this is null, then the graph settings will be deleted.
     * @return The user's new graph settings.
     */
    fun updateGraphSettings(userId: String, graphSettings: GraphSettings? = null): Mono<GraphSettings> {
        val upsertAfter = findOneAndUpdateUpsert().returnDocument(ReturnDocument.AFTER)

        if (graphSettings == null) {
            // delete the key
            return deleteUser(userId, NightscoutUserDTO::graphSettings)
                    .map { GraphSettings() }
        }

        val update = setValue(NightscoutUserDTO::graphSettings, graphSettings)

        return collection.findOneAndUpdate(filter(userId), update, upsertAfter).toMono()
                .map { it.graphSettings }
                .subscribeOn(scheduler)
    }

    enum class UpdateMode { SET, ADD, DELETE }

    companion object {
        val instance: NightscoutDAO by lazy { NightscoutDAO() }

        fun filter(userId: String): Bson {
            return NightscoutUserDTO::userId eq userId
        }
    }
}
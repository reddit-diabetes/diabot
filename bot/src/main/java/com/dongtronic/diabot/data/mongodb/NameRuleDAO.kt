package com.dongtronic.diabot.data.mongodb

import com.dongtronic.diabot.util.DiabotCollection
import com.dongtronic.diabot.util.MongoDB
import com.dongtronic.diabot.util.findOne
import com.dongtronic.diabot.util.logger
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.conversions.Bson
import org.litote.kmongo.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty

class NameRuleDAO private constructor() {
    private val mongo = MongoDB.getInstance().database
    val collection: MongoCollection<NameRuleDTO>
            = mongo.getCollection(DiabotCollection.NAME_RULES.getEnv(), NameRuleDTO::class.java)

    private val ruleCache: Cache<String, NameRuleDTO> = Caffeine.newBuilder()
            .expireAfterAccess(240, TimeUnit.MINUTES)
            .maximumSize(200)
            .build()

    private val scheduler = Schedulers.boundedElastic()
    private val logger = logger()

    init {
        // Create a unique index
        val options = IndexOptions().unique(true)
        collection.createIndex(descending(NameRuleDTO::guildId), options).toMono()
                .subscribeOn(scheduler)
                .subscribe()
    }

    /**
     * Gets a [NameRuleDTO] from a guild ID
     *
     * @param guildId The guild ID to look up
     * @return [NameRuleDTO] for this guild
     */
    fun getGuild(guildId: String): Mono<NameRuleDTO> {
        val cached = ruleCache.getIfPresent(guildId)
        if (cached != null) {
            // check if the cached value is the non-enforcing dto
            return if (cached == NameRuleDTO(guildId)) {
                NoSuchElementException().toMono()
            } else {
                cached.toMono()
            }
        }

        return collection.findOne(filter(guildId))
                .subscribeOn(scheduler)
                .doOnNext { ruleCache.put(it.guildId, it) }
                // cache a non-enforcing NameRuleDTO if there are no rules for this guild
                .doOnError(NoSuchElementException::class.java) { ruleCache.put(guildId, NameRuleDTO(guildId)) }
    }

    /**
     * Inserts a new guild's name enforcement data into the database.
     * This should only be used if it is known that the guild does not exist already as it will error otherwise.
     *
     * @param dto The [NameRuleDTO] to be inserted
     * @return The result of the insertion
     */
    fun addGuild(dto: NameRuleDTO): Mono<InsertOneResult> {
        return collection.insertOne(dto).toMono()
                .subscribeOn(scheduler)
                .doOnNext { ruleCache.invalidate(dto.guildId) }
    }

    /**
     * Deletes stored name enforcement data belonging to the given guild.
     * If no properties are given, all of the guild's rules will be deleted from the database.
     *
     * @param guildId The guild's ID.
     * @param fields Which fields to delete. If this is not provided then all of the guild's data will be deleted.
     * @return Either a [UpdateResult] or [DeleteResult] representing the result of data deletion.
     * If `fields` is blank this will return [DeleteResult]. If not blank, [UpdateResult].
     */
    fun deleteGuild(guildId: String, vararg fields: KProperty<*>): Mono<*> {
        if (fields.isNullOrEmpty()) {
            // delete all of the guild's data
            return collection.deleteOne(filter(guildId)).toMono()
                    .subscribeOn(scheduler)
                    .doOnNext { ruleCache.invalidate(guildId) }
        }

        val unsets = fields.map { unset(it) }
        val combined = combine(unsets)
        return collection.updateOne(filter(guildId), combined).toMono()
                .subscribeOn(scheduler)
                .doOnNext { ruleCache.invalidate(guildId) }
    }

    /**
     * Sets a guild's name enforcement pattern.
     *
     * @param guildId The guild's ID
     * @param pattern The name enforcement regex pattern. If null, the key will be deleted.
     * @return The result of setting the pattern for the guild
     */
    fun setPattern(guildId: String, pattern: String?): Mono<UpdateResult> {
        if (pattern == null) {
            // delete the key instead of setting it to null
            return deleteGuild(guildId, NameRuleDTO::pattern).ofType(UpdateResult::class.java)
        }

        return collection.updateOne(filter(guildId),
                setValue(NameRuleDTO::pattern, pattern), upsert())
                .toMono()
                .subscribeOn(scheduler)
                .doOnNext { ruleCache.invalidate(guildId) }
    }

    /**
     * Sets a hint message to be used when users violate a guild's name rules.
     *
     * @param guildId The guild's ID
     * @param hintMessage The hint message. If null, the key will be deleted.
     * @return The result of setting the guild's hint message
     */
    fun setHint(guildId: String, hintMessage: String?): Mono<UpdateResult> {
        if (hintMessage == null) {
            // delete the key instead of setting it to null
            return deleteGuild(guildId, NameRuleDTO::hintMessage).ofType(UpdateResult::class.java)
        }

        return collection.updateOne(filter(guildId),
                setValue(NameRuleDTO::hintMessage, hintMessage), upsert())
                .toMono()
                .subscribeOn(scheduler)
                .doOnNext { ruleCache.invalidate(guildId) }
    }

    /**
     * Sets the username enforcing status for a guild.
     *
     * @param guildId The guild's ID
     * @param enforce Whether to enable the guild's enforcing state or not. If null, the key will be deleted.
     * @return The result of setting the guild's enforcing state
     */
    fun setEnforcing(guildId: String, enforce: Boolean?): Mono<UpdateResult> {
        if (enforce == null) {
            // delete the key instead of setting it to null
            return deleteGuild(guildId, NameRuleDTO::enforce).ofType(UpdateResult::class.java)
        }

        return collection.updateOne(filter(guildId),
                setValue(NameRuleDTO::enforce, enforce), upsert())
                .toMono()
                .subscribeOn(scheduler)
                .doOnNext { ruleCache.invalidate(guildId) }
    }

    companion object {
        val instance: NameRuleDAO by lazy { NameRuleDAO() }

        fun filter(guildId: String): Bson {
            return NameRuleDTO::guildId eq guildId
        }
    }
}
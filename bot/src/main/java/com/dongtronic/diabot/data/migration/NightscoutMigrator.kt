package com.dongtronic.diabot.data.migration

import com.dongtronic.diabot.data.mongodb.NightscoutDAO
import com.dongtronic.diabot.data.mongodb.NightscoutUserDTO
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands
import com.dongtronic.diabot.util.logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import redis.clients.jedis.Jedis

class NightscoutMigrator : Migrator {
    private val redis = com.dongtronic.diabot.data.redis.NightscoutDAO.getInstance()
    private val mongo = NightscoutDAO.instance
    private val jedis: Jedis = Jedis(System.getenv("REDIS_URL"))
    private val logger = logger()

    override fun needsMigration(): Mono<Boolean> {
        return mongo.collection.countDocuments().toMono().map {
            return@map it == 0L || getAllUids().size.toLong() > it
        }
    }

    override fun migrate(): Flux<Long> {
        val dtos = getAllUids().mapNotNull { userId ->
            val url = redis.getNightscoutUrl(userId)
            val token = redis.getNightscoutToken(userId)
            val display = redis.getNightscoutDisplay(userId).split(" ")
            val public = getPublicGuilds(userId).toList()

            // if all of their data is empty then skip them
            if (url == null
                    && token == null
                    && display.isNullOrEmpty()
                    && public.isNullOrEmpty()) {
                return@mapNotNull null
            }

            NightscoutUserDTO(
                    userId = userId,
                    url = url,
                    token = token,
                    displayOptions = display.mapNotNull { option ->
                        NightscoutDisplayCommands.DisplayOptions.values().firstOrNull { it.name == option }
                    }.toSet(),
                    publicGuilds = public
            )
        }

        return dtos.toFlux()
                .flatMap { mongo.addUser(it) }
                .map { it.wasAcknowledged() }
                .onErrorContinue { t, u ->
                    logger.warn("Could not import nightscout: $u", t)
                }
                .filter { it }
                .count()
                .toFlux()
    }

    /**
     * Gets a list of the guilds which a user has set their NS to public in, from redis
     */
    private fun getPublicGuilds(userId: String): Set<String> {
        return jedis.keys("$userId:*:nightscoutpublic")
                .map { it.split(":")[1] }
                .toSet()
    }

    /**
     * Gets a list of all the user IDs which have any form of Nightscout data saved in redis
     */
    private fun getAllUids(): Set<String> {
        return jedis.keys("*:nightscout*")
                .map { it.substringBefore(":") }
                .toSet()
    }
}
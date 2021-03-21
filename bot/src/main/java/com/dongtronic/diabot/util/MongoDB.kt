package com.dongtronic.diabot.util

import com.dongtronic.diabot.platforms.discord.utils.ColorDeserializer
import com.dongtronic.diabot.platforms.discord.utils.ColorSerializer
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Collation
import com.mongodb.client.model.CollationStrength
import com.mongodb.reactivestreams.client.MongoCollection
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.match
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.reactivestreams.aggregate
import org.litote.kmongo.reactivestreams.find
import org.litote.kmongo.sample
import org.litote.kmongo.util.KMongoConfiguration
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.awt.Color
import java.util.concurrent.TimeUnit

class MongoDB {
    private val clientSettings: MongoClientSettings = MongoClientSettings.builder()
            .applyToConnectionPoolSettings {
                // close connections after 1 hour of inactivity
                it.maxConnectionIdleTime(60, TimeUnit.MINUTES)
                it.maxSize(mongoEnv("CONNECTIONS", "30").toInt())
            }
            .applyConnectionString(ConnectionString(mongoEnv("URI")))
            .build()
    val client = KMongo.createClient(clientSettings)
    val database: MongoDatabase = client.getDatabase(mongoEnv("DATABASE", "diabot"))

    init {
        val colorModule = SimpleModule()
        // serializer/deserializer for java.awt.Color
        colorModule.addSerializer(Color::class.java, ColorSerializer())
        colorModule.addDeserializer(Color::class.java, ColorDeserializer())
        KMongoConfiguration.registerBsonModule(colorModule)
        KMongoConfiguration.bsonMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    }

    companion object {
        private var instance: MongoDB? = null

        fun getInstance(): MongoDB {
            if (instance == null) {
                instance = MongoDB()
            }
            return instance as MongoDB
        }
    }
}

/**
 * Gets the value of an environment variable, with a `MONGO_` prefix.
 *
 * @param key The key to grab
 * @param default The default value to fallback to, if any.
 * @return The value of the key or the default value.
 */
fun mongoEnv(key: String, default: String? = null): String {
    val mongoKey = "MONGO_" + key.toUpperCase()

    if (default == null)
        return System.getenv(mongoKey)

    return System.getenv().getOrDefault(mongoKey, default)
}

/**
 * Collation for case-insensitive queries
 */
val caseCollation: Collation = Collation.builder()
        .collationStrength(CollationStrength.SECONDARY)
        .locale("en")
        .build()

fun <T> MongoCollection<T>.findOne(vararg filter: Bson): Mono<T> {
    return Mono.from(find(*filter).collation(caseCollation).limit(1)).errorOnEmpty()
}

inline fun <reified T : Any> MongoCollection<T>.findOneRandom(vararg filter: Bson): Mono<T> {
    val filters = match(and(*filter))
    val count = sample(1)
    return Mono.from(aggregate<T>(filters, count).collation(caseCollation)).errorOnEmpty()
}

fun <T> MongoCollection<T>.findMany(vararg filter: Bson): Flux<T> {
    return Flux.from(find(*filter).collation(caseCollation)).errorOnEmpty()
}

fun <T> Mono<T>.errorOnEmpty(throwable: Throwable = NoSuchElementException()): Mono<T> {
    return switchIfEmpty(Mono.error(throwable))
}

fun <T> Flux<T>.errorOnEmpty(throwable: Throwable = NoSuchElementException()): Flux<T> {
    return switchIfEmpty(Flux.error(throwable))
}
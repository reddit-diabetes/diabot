package com.dongtronic.nightscout

import com.dongtronic.diabot.logic.diabetes.BloodGlucoseConverter
import com.dongtronic.diabot.util.logger
import com.dongtronic.nightscout.data.BgEntry
import com.dongtronic.nightscout.data.NightscoutDTO
import com.dongtronic.nightscout.exceptions.NoNightscoutDataException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.litote.kmongo.MongoOperator
import reactor.core.publisher.Mono
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.Closeable
import java.time.Instant

class Nightscout(baseUrl: String, token: String? = null) : Closeable {
    private val logger = logger()
    private val responseCache = hashMapOf<String, Response.Builder>()
    private val httpClient: OkHttpClient
    private val service: NightscoutService

    init {
        val client = OkHttpClient.Builder()
        if (token != null) {
            client.interceptors().add(Interceptor {
                // adds the auth token to the parameters
                val newUrl = it.request()
                        .url
                        .newBuilder()
                        .addQueryParameter("token", token)
                        .build()

                val newRequest = it.request()
                        .newBuilder()
                        .url(newUrl)
                        .build()

                return@Interceptor it.proceed(newRequest)
            })
        }

        client.interceptors().add(Interceptor { chain ->
            // provide cached responses / cache network responses
            val request = chain.request()

            return@Interceptor responseCache[request.toString()].let { cached ->
                if (cached == null) {
                    logger.debug("Performing network request for endpoint ${request.url.encodedPath}")
                    val networkResponse = chain.proceed(request)
                    val body = networkResponse.peekBody(1024 * 1024)
                    responseCache[request.toString()] = networkResponse.newBuilder().body(body)
                    networkResponse
                } else {
                    logger.debug("Providing cached response for endpoint ${request.url.encodedPath}")
                    cached.build()
                }
            }
        })

        httpClient = client.build()
        service = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addCallAdapterFactory(ReactorCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(jacksonObjectMapper()))
                .build()
                .create(NightscoutService::class.java)
    }

    /**
     * Tests if the URL is a valid Nightscout instance
     *
     * @return true if the instance exists, false if not
     */
    fun isNightscoutInstance(): Mono<Boolean> {
        return service.getStatusResponse().map { it.code() != 404 }
    }

    /**
     * Tests whether a Nightscout requires a token to be read.
     * This does not actually test whether or not the instance is *secured*, it tests whether a token is currently needed.
     * If this class was instantiated with the `token` parameter filled, and it grants read access to this NS, then this will be false.
     *
     * @return true if a token is required, false if not
     */
    fun needsNightscoutToken(): Mono<Boolean> {
        return service.getStatusResponse().map { it.code() == 401 }
    }

    /**
     * Fetches a Nightscout's settings and puts the data in a [NightscoutDTO] instance
     *
     * @param dto NS data
     * @return The [NightscoutDTO] instance with the Nightscout's settings (target ranges, title, bg units)
     */
    fun getSettings(dto: NightscoutDTO = NightscoutDTO()): Mono<NightscoutDTO> {
        return service.getStatusJson().map { json ->
            val settings = json.path("settings")
            val ranges = settings.path("thresholds")

            val title = settings.path("customTitle").asText()
            val units = settings.path("units").asText()
            val low = ranges.path("bgLow").asInt()
            val bottom = ranges.path("bgTargetBottom").asInt()
            val top = ranges.path("bgTargetTop").asInt()
            val high = ranges.path("bgHigh").asInt()

            val builder = dto.newBuilder()

            builder.title(title)
            builder.low(low)
            builder.bottom(bottom)
            builder.top(top)
            builder.high(high)
            builder.units(units)

            return@map builder.build()
        }
    }

    /**
     * Fetches the `pebble` endpoint for a Nightscout instance and puts the data in a [NightscoutDTO] instance.
     *
     * @param dto NS DTO
     * @return The [NightscoutDTO] instance with COB and IOB data
     */
    fun getPebble(dto: NightscoutDTO = NightscoutDTO()): Mono<NightscoutDTO> {
        return service.getPebbleJson().map { json ->
            val bgsJson = json.get("bgs")?.get(0)
            val builder = dto.newBuilder()

            if (bgsJson == null) {
                logger.warn("Failed to get bgs object from pebble Endpoint JSON:\n${json.toPrettyString()}")
                return@map dto
            }
            var bgDelta = "0"
            if (bgsJson.hasNonNull("cob")) {
                builder.cob(bgsJson.get("cob").asInt())
            }
            if (bgsJson.hasNonNull("iob")) {
                builder.iob(bgsJson.get("iob").asText().toFloat())
            }
            if (bgsJson.hasNonNull("bgdelta")) {
                bgDelta = bgsJson.get("bgdelta").asText()
            }
            val newestBg = dto.getNewestEntryOrNull()
            if (newestBg != null && newestBg.delta == null) {
                val bgBuilder = newestBg.newBuilder()
                BloodGlucoseConverter.convert(bgDelta, dto.units)?.let {
                    bgBuilder.delta(it)
                }
                builder.replaceEntry(bgBuilder.build())
            }
            return@map builder.build()
        }
    }

    /**
     * Fetches a Nightscout's most recent SGV(s) and puts the data in a [NightscoutDTO] instance
     *
     * @param dto NS data
     * @param count The amount of SGV entries to retrieve
     * @return The [NightscoutDTO] instance with recent glucose data (sgv, timestamp, trend, delta)
     */
    fun getRecentSgv(dto: NightscoutDTO = NightscoutDTO(), count: Int = 1): Mono<NightscoutDTO> {
        val findParam = EntriesParameters()
                .find("sgv", operator = MongoOperator.exists)
                .count(count)
                .toMap()
        return service.getEntriesJson(findParam).map { json ->
            if (json.isEmpty) {
                throw NoNightscoutDataException()
            }

            val dtoBuilder = dto.newBuilder()

            // Parse JSON and construct response
            json.forEach { entryJson ->
                val sgv = entryJson.path("sgv").asText()
                val timestamp = entryJson.path("date").asLong()
                var trend = TrendArrow.NONE
                val direction: String
                if (entryJson.hasNonNull("trend")) {
                    trend = TrendArrow.getTrend(entryJson.path("trend").asInt())
                } else if (entryJson.hasNonNull("direction")) {
                    direction = entryJson.path("direction").asText()
                    trend = TrendArrow.getTrend(direction)
                }

                var delta = ""
                if (entryJson.hasNonNull("delta")) {
                    delta = entryJson.path("delta").asText()
                }

                val bgBuilder = BgEntry.Builder()
                bgBuilder.glucose(BloodGlucoseConverter.convert(sgv, "mg")!!)

                if (delta.isNotEmpty()) {
                    try {
                        bgBuilder.delta(BloodGlucoseConverter.convert(delta, "mg")!!)
                    } catch (e: IllegalArgumentException) {
                        // invalid delta
                    }
                }

                bgBuilder.dateTime(Instant.ofEpochMilli(timestamp))
                bgBuilder.trend(trend)
                dtoBuilder.replaceEntry(bgBuilder.build())
            }

            return@map dtoBuilder.build()
        }
    }

    override fun close() {
        logger.debug("Closing Nightscout API: ${responseCache.size} responses")
        responseCache.forEach { (_, response) ->
            // clear the cache bodies
            response.body(null)
        }
        responseCache.clear()
    }
}
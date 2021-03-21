package com.dongtronic.diabot.platforms.discord.commands.info

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.Main
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.ReplyType
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.Permission
import java.awt.Color
import java.lang.management.ManagementFactory
import java.time.Instant

class AboutCommand(private val prefix: String) {
    private val homeGuildInvite = "https://discord.gg/diabetes"
    private val description = "a diabetes bot"
    private val defaultColour = Color(0, 0, 255)
    private val features = arrayOf(
            "Converting between mmol/L and mg/dL",
            "Performing A1c estimations",
            "Showing Nightscout information"
    )
    private val owners = buildString {
        val mainOwners = Main.ownerIds

        mainOwners.forEachIndexed { index, ownerId ->
            if (mainOwners.size > 2
                    && index != 0) append(", ")

            if (mainOwners.size != 1
                    && index + 1 == mainOwners.size) append("and ")

            append("<@$ownerId>")
        }
    }
    private val permissions = listOf(
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MANAGE_ROLES,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_READ,
            Permission.MESSAGE_WRITE,
            Permission.NICKNAME_MANAGE
    )

    @CommandMethod("about")
    @CommandDescription("Provides general information about the bot")
    @CommandCategory(Category.INFO)
    fun execute(sender: JDACommandUser) {
        val jda = sender.event.jda
        val guild = if (sender.event.isFromGuild) sender.event.guild else null

        val builder = EmbedBuilder()

        builder.setColor(guild?.selfMember?.color ?: defaultColour)


        val name = jda.selfUser.name
        builder.setDescription(buildString {
            append("Hello! I am **$name**, $description.\n")

            append("I was written in Kotlin by $owners ")
            append("using Incendo's [Cloud command framework](https://github.com/Incendo/cloud) ")
            append("and the [JDA library](https://github.com/DV8FromTheWorld/JDA) (${JDAInfo.VERSION})\n")

            append("Type `${prefix}help` to see my commands!\n")

            append("Join my server [`here`]($homeGuildInvite)")
            append(", or [`invite`](${generateInvite(jda)}) me to your server!\n\n")

            if (features.isNotEmpty()) {
                append("Some of my features include: ```css\n")

                features.forEachIndexed { index, feature ->
                    if (index != 0) append("\n")

                    append("\uD83D\uDC4C $feature")
                }

                append("```")
            }
        })

        builder.addField("Stats", buildString {
            append("Shard ${jda.shardInfo.shardId+1}/${jda.shardInfo.shardTotal}")
        }, true)

        builder.addField("", buildString {
            append(plural(jda.userCache.size(), "User"))
            append("\n")
            append(plural(jda.guildCache.size(), "Server"))
        }, true)

        builder.addField("", buildString {
            append(plural(jda.textChannelCache.size(), "Text Channel"))
            append("\n")
            append(plural(jda.voiceChannelCache.size(), "Voice Channel"))
        }, true)

        builder.setFooter("Last restart")

        val actualTime = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().uptime
        val epoch = Instant.ofEpochMilli(actualTime)
        builder.setTimestamp(epoch)

        sender.reply(builder.build(), ReplyType.NONE).subscribe()
    }

    private fun plural(count: Number, text: CharSequence): String {
        val builder = StringBuilder("$count $text")
        if (count.toDouble() != 1.00) builder.append("s")
        return builder.toString()
    }

    private fun generateInvite(jda: JDA): String {
        return jda.getInviteUrl(permissions)
    }
}
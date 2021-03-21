package com.dongtronic.diabot.platforms.discord.commands.quote

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.data.mongodb.QuoteDAO
import com.dongtronic.diabot.data.mongodb.QuoteDTO
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import com.mongodb.client.result.DeleteResult
import net.dv8tion.jda.api.Permission
import reactor.core.publisher.Mono

class QuoteSubcommands {
    private val mentionsRegex = QuoteCommand.mentionsRegex
    private val quoteRegex = Regex("\"(?<message>[\\s\\S]*)\" ?- ?(?<author>.*[^\\s])")
    private val logger = logger()

    @GuildOnly
    @CommandMethod("quote add|a <quote>")
    @CommandDescription("Creates new quotes")
    @CommandCategory(Category.FUN)
    @Example(["[add] \"this is a quote added manually\" - gar"])
    fun addQuote(
            sender: JDACommandUser,
            @Greedy
            @Argument("quote", description = "The quote to add")
            quote: String
    ) {
        val event = sender.event

        if (!QuoteDAO.checkRestrictions(event.textChannel, warnDisabledGuild = true)) return

        val match = quoteRegex.find(quote)
        if (match == null) {
            sender.replyErrorS("Could not parse quote. Please make sure you are using the correct format for this command")
            return
        }

        var authorId = 0L
        var author = match.groups["author"]!!.value.trim()
        val message = match.groups["message"]!!.value.trim()

        val mention = mentionsRegex.matchEntire(author)
        if (mention != null) {
            val mentioned = event.message.mentionedMembers.lastOrNull()
            val uid = mention.groups["uid"]!!.value.trim().toLongOrNull()
            if (mentioned != null && uid != null) {
                author = mentioned.user.name
                authorId = uid
            }
        }

        val quoteDto = QuoteDTO(
                guildId = event.guild.id,
                channelId = event.channel.id,
                author = author,
                authorId = authorId.toString(),
                message = message,
                messageId = event.message.id
        )

        QuoteDAO.getInstance().addQuote(quoteDto).subscribe({
            sender.replySuccessS("New quote added by ${event.member!!.effectiveName} as #${it.quoteId}")
        }, {
            sender.replyErrorS("Could not add quote: ${it.message}")
            logger.warn("Unexpected error when adding quote", it)
        })
    }

    @GuildOnly
    @CommandMethod("quote delete|del|d|remove|rm|r <quoteId>")
    @CommandDescription("Deletes a quote by its ID")
    @CommandCategory(Category.FUN)
    @Example(["[delete] 1337"])
    fun deleteQuote(
            sender: JDACommandUser,
            @Argument("quoteId", description = "The quote ID to delete")
            quoteId: Long
    ) {
        val event = sender.event

        if (!QuoteDAO.checkRestrictions(event.textChannel, warnDisabledGuild = true, checkQuoteLimit = false)) return

        val deleteCommand = QuoteDAO.getInstance().deleteQuote(event.guild.id, quoteId.toString())
        var execution: Mono<DeleteResult> = deleteCommand

        if (!event.member!!.hasPermission(Permission.MESSAGE_MANAGE)) {
            execution = QuoteDAO.getInstance()
                    .getQuote(event.guild.id, quoteId.toString())
                    .flatMap {
                        if (it.authorId == event.author.id) {
                            deleteCommand
                        } else {
                            Mono.error(IllegalCallerException("You may not delete this quote as you are not the author of it."))
                        }
                    }
        }

        execution.subscribe({
            if (it.wasAcknowledged() && it.deletedCount != 0L) {
                sender.replySuccessS("Quote #$quoteId deleted")
            } else {
                sender.replyErrorS("No quote found for #$quoteId")
            }
        }, {
            if (it is IllegalCallerException && it.message != null) {
                sender.replyErrorS(it.message!!)
            } else if (it is NoSuchElementException) {
                sender.replyErrorS("No quote found for #$quoteId")
            } else {
                sender.replyErrorS("Could not delete quote #$quoteId")
                logger.warn("Unexpected error when deleting quote", it)
            }
        })
    }

    @GuildOnly
    @DiscordPermission(Permission.MESSAGE_MANAGE)
    @CommandMethod("quote edit|e <quoteId> <quote>")
    @CommandDescription("Edits an existing quote by its ID")
    @CommandCategory(Category.FUN)
    @Example(["[edit] 1337 \"edited quote\" - edited author"])
    fun editQuote(
            sender: JDACommandUser,
            @Argument("quoteId", description = "The quote ID to edit")
            id: Long,
            @Greedy
            @Argument("quote", description = "The quote that should replace the old one")
            quote: String
    ) {
        val event = sender.event

        if (!QuoteDAO.checkRestrictions(event.textChannel, warnDisabledGuild = true, checkQuoteLimit = false)) return

        val match = quoteRegex.matchEntire(quote)
        if (match == null) {
            sender.replyErrorS("Could not parse command. Please make sure you are using the correct format for this command")
            return
        }

        val author = match.groups["author"]!!.value.trim()
        val message = match.groups["message"]!!.value.trim()
        val oldQuote = QuoteDAO.getInstance().getQuote(event.guild.id, id.toString())

        oldQuote.flatMap {
            val dto = it.copy(
                    author = author,
                    message = message,
                    messageId = event.message.id,
                    channelId = event.channel.id
            )

            QuoteDAO.getInstance().updateQuote(dto)
        }.subscribe({
            if (it.wasAcknowledged()) {
                sender.replySuccessS("Quote #$id edited")
            } else {
                sender.replyErrorS("Could not edit quote #$id")
            }
        }, {
            if (it is NoSuchElementException) {
                sender.replyErrorS("No quote found for #$id")
            } else {
                sender.replyErrorS("Could not edit quote #$id")
                logger.warn("Unexpected error when editing quote", it)
            }
        })
    }
}
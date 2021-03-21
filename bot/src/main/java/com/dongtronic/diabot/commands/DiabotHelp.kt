package com.dongtronic.diabot.commands

import cloud.commandframework.CommandHelpHandler
import cloud.commandframework.CommandManager
import cloud.commandframework.arguments.StaticArgument
import com.dongtronic.diabot.util.WrappedObjectBuilder
import com.dongtronic.diabot.util.logger
import com.github.ygimenez.method.Pages
import com.github.ygimenez.model.Page
import com.github.ygimenez.type.PageType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

/**
 * Extension of [CommandHelpHandler] for Diabot
 *
 * @param C Command sender type
 * @property commandManager Command manager instance
 * @property prefix Command prefix to add to each command
 * @property messenger Function which sends a message to the specified sender and returns the sent message object
 * @property reactionAllowed Function which checks if a user is allowed to interact with a paginated message
 */
class DiabotHelp<C>(
        private val commandManager: CommandManager<C>,
        private val prefix: String,
        private val messenger: (sender: C, message: Message) -> Mono<Message>,
        private val reactionAllowed: (sender: C, reactingUser: User) -> Boolean
) {
    private val logger = logger()

    /**
     * Converts the `reactionAllowed` parameter into a predicate for a sender.
     *
     * @param sender Command sender type
     * @return Predicate of whether a [User] can react to a message.
     */
    private fun reactionPredicate(sender: C) = Predicate<User> { reactionAllowed(sender, it) }

    /**
     * Submits a help query requested by a sender.
     *
     * @param rawQuery The help query
     * @param recipient The command sender that submitted the help query
     */
    fun queryCommands(
            rawQuery: String,
            recipient: C
    ) {
        val helpTopic = commandManager.getCommandHelpHandler { !it.isHidden }.queryHelp(recipient, rawQuery)

        printTopic(recipient, rawQuery, helpTopic)
    }

    /**
     * Notifies the recipient that their query did not return any results.
     *
     * @param recipient The command sender
     * @param query The sender's help query
     */
    fun printNoResults(recipient: C, query: String) {
        val msg = MessageBuilder("No results for query: `${prefix}${query}`").build()
        messenger(recipient, msg).subscribe()
    }

    /**
     * Prints more detailed information that is specific to the type of [CommandHelpHandler.HelpTopic] returned.
     *
     * @param recipient The command sender
     * @param query The sender's help query
     * @param helpTopic The help topic returned from querying the help handler
     */
    fun printTopic(
            recipient: C,
            query: String,
            helpTopic: CommandHelpHandler.HelpTopic<C>
    ) {
        when (helpTopic) {
            is CommandHelpHandler.IndexHelpTopic -> {
                printIndexHelpTopic(recipient, query, helpTopic)
            }
            is CommandHelpHandler.MultiHelpTopic -> {
                printMultiHelpTopic(recipient, query, helpTopic)
            }
            is CommandHelpHandler.VerboseHelpTopic -> {
                printVerboseHelpTopic(recipient, query, helpTopic)
            }
            else -> throw IllegalArgumentException("Unknown help topic: $helpTopic")
        }
    }

    /**
     * Prints detailed information about the [CommandHelpHandler.IndexHelpTopic] topic type. This help topic essentially
     * lists all of the commands available.
     *
     * @param recipient The command sender
     * @param query The sender's help query
     * @param helpTopic The help topic returned from querying the help handler
     */
    fun printIndexHelpTopic(
            recipient: C,
            query: String,
            helpTopic: CommandHelpHandler.IndexHelpTopic<C>
    ) {
        if (helpTopic.isEmpty) {
            printNoResults(recipient, query)
            return
        }

        val categoryGrouped = helpTopic.entries.groupBy {
            it.command.commandMeta.getOrDefault(
                    ParserUtils.META_CATEGORY,
                    Category.UNSPECIFIED
            )
        }

        val embeds = categoryGrouped.flatMap { entry ->
            var count = 0
            val embed = createWrappedEmbedBuilder {
                count = 0
                it.setTitle(entry.key.displayName + " (continued)")
                it.setDescription(null)
            }

            embed.modify {
                it.setTitle(entry.key.displayName)
            }

            entry.value.forEach { helpEntry ->
                embed.modifyInPlace {
                    val desc = it.descriptionBuilder
                    if (++count > 1) desc.newLine()
                    desc.append("**•** `$prefix").append(helpEntry.syntaxString).append("`")
                            .append(" **=>** ").append(helpEntry.description)
                }
            }

            embed.build()
        }

        val pages = paginateEmbeds(embeds)

        messenger(recipient, MessageBuilder(pages.getEmbed(0)).build()).subscribe { message ->
            Pages.paginate(message, pages, 3, TimeUnit.MINUTES, reactionPredicate(recipient))
        }
    }

    /**
     * Prints detailed information about the [CommandHelpHandler.MultiHelpTopic] topic type. This help topic essentially
     * lists several commands available that match the help query.
     *
     * @param recipient The command sender
     * @param query The sender's help query
     * @param helpTopic The help topic returned from querying the help handler
     */
    fun printMultiHelpTopic(
            recipient: C,
            query: String,
            helpTopic: CommandHelpHandler.MultiHelpTopic<C>
    ) {
        if (helpTopic.childSuggestions.isEmpty()) {
            printNoResults(recipient, query)
            return
        }

        var count = 0
        val embed = createWrappedEmbedBuilder {
            count = 0
            it.setDescription(null)
        }

        helpTopic.childSuggestions.forEach { suggestion ->
            embed.modifyInPlace {
                val desc = it.descriptionBuilder
                if (++count > 1) desc.newLine()
                desc.append("**―** `$prefix").append(suggestion).append("`")
            }
        }

        val embeds = embed.build().toMutableList()
        val pages = paginateEmbeds(embeds)
        val message = MessageBuilder()
                .setContent("Showing results for `$prefix$query`")
                .setEmbed(pages.getEmbed())
                .build()

        messenger(recipient, message).subscribe { sentMessage ->
            if (embeds.size > 1) {
                Pages.paginate(sentMessage, pages, 3, TimeUnit.MINUTES, reactionPredicate(recipient))
            }
        }
    }

    /**
     * Prints detailed information about the [CommandHelpHandler.VerboseHelpTopic] topic type. This help topic prints
     * information that is specific to one command.
     *
     * @param recipient The command sender
     * @param query The sender's help query
     * @param helpTopic The help topic returned from querying the help handler
     */
    fun printVerboseHelpTopic(
            recipient: C,
            query: String,
            helpTopic: CommandHelpHandler.VerboseHelpTopic<C>
    ) {
        val embed = EmbedBuilder()
        embed.setColor(0x24aab6)
        embed.setTitle("Command Help")

        val desc = embed.descriptionBuilder
        val syntax = commandManager.commandSyntaxFormatter.apply(helpTopic.command.arguments, null)
        val literalArg = helpTopic.command.components.mapNotNull { it.argument as? StaticArgument }.last()
        val aliases = literalArg.aliases

        desc.append("```")

        desc.append("> ").append(syntax).newSection()

        desc.append("Description:").newLine()
        desc.tab().append(helpTopic.description).newSection()

        val category = helpTopic.command.commandMeta.getOrDefault(ParserUtils.META_CATEGORY, Category.UNSPECIFIED)
        desc.append("Category:").newLine()
        desc.tab().append(category.displayName).newSection()

        desc.append("Aliases:").newLine()
        desc.tab().append(aliases.joinToString()).newSection()

        val examples = helpTopic.command.commandMeta.getOrDefault(ParserUtils.META_EXAMPLE, emptyArray())
        if (examples.isNotEmpty()) {
            val aliasesShuffled = aliases.shuffled()
            // pattern for `[alias]`
            val pattern = aliases.joinToString(prefix = "\\[(", separator = "|", postfix = ")\\]").toRegex()

            desc.append("Examples:").newLine()

            examples.forEachIndexed { index, ex ->
                val aliasIndex = index % aliasesShuffled.size
                // replace the alias with a random one
                val example = ex.replace(pattern, aliasesShuffled[aliasIndex])
                desc.tab().append("> ").append(example).newLine()
            }

            desc.newLine()
        }

        val components = helpTopic.command.components.filterNot { it.argument is StaticArgument }
        if (components.isNotEmpty()) {
            desc.append("Arguments:").newLine()

            for (component in components) {
                val description = component.argumentDescription.description.ifEmpty { "No description" }

                val name = commandManager.commandSyntaxFormatter.apply(listOf(component.argument), null)

                desc.tab().append("• ").append(name).newLine()
                desc.dualTab().append("— ").append(description).newLine()
            }

            desc.newLine()
        }
        desc.append("```")

        embed.setFooter("Query: $query")

        messenger(recipient, MessageBuilder(embed.build()).build()).subscribe()
    }

    /**
     * Gets a page's content as a [Message].
     *
     * @receiver List<Page>
     * @param page The page to grab the message from. This is 1-indexed, so `1` is the first page.
     * @return Message on the given page
     */
    private fun List<Page>.getMessage(page: Int = 1): Message {
        return getPage(page).content as Message
    }

    /**
     * Gets a page's content as a [MessageEmbed] instance.
     *
     * @receiver List<Page>
     * @param page The page to grab the embed from. This is 1-indexed, so `1` is the first page.
     * @return Message embed on the given page
     */
    private fun List<Page>.getEmbed(page: Int = 1): MessageEmbed {
        return getPage(page).content as MessageEmbed
    }

    /**
     * Gets a specific page by its number, or the first page if there is no page at the position given.
     *
     * @receiver List<Page>
     * @param page The page number to grab. This is 1-indexed, so `1` is the first page.
     * @return The [Page] at the given number, or the first page if there is no page at the given position
     */
    private fun List<Page>.getPage(page: Int): Page {
        return getOrNull(page - 1) ?: first()
    }

    /**
     * Appends a tab character
     *
     * @receiver StringBuilder
     * @return This [StringBuilder] instance
     */
    private fun StringBuilder.tab() = append("\t")

    /**
     * Appends two tab characters
     *
     * @receiver StringBuilder
     * @return This [StringBuilder] instance
     */
    private fun StringBuilder.dualTab() = tab().tab()

    /**
     * Appends a newline character
     *
     * @receiver StringBuilder
     * @return This [StringBuilder] instance
     */
    private fun StringBuilder.newLine() = append("\n")

    /**
     * Appends two newline characters
     *
     * @receiver StringBuilder
     * @return This [StringBuilder] instance
     */
    private fun StringBuilder.newSection() = newLine().newLine()

    /**
     * Creates a [WrappedObjectBuilder] for the [EmbedBuilder].
     *
     * @param prepForWrap The function used to prepare the builder for a wrap
     * @return A [WrappedObjectBuilder] instance for [EmbedBuilder] objects
     */
    private fun createWrappedEmbedBuilder(
            prepForWrap: (builder: EmbedBuilder) -> EmbedBuilder
    ): WrappedObjectBuilder<EmbedBuilder, MessageEmbed> {
        return WrappedObjectBuilder(
                builder = EmbedBuilder(),
                duplicateBuilder = { EmbedBuilder(it) },
                buildObject = { it.build() },
                isUnderLimit = { _: EmbedBuilder, newBuilder: EmbedBuilder ->
                    val length = newBuilder.descriptionBuilder.length
                    val isDescBelowLimit = length <= MessageEmbed.TEXT_MAX_LENGTH

                    newBuilder.isValidLength && isDescBelowLimit
                },
                prepForWrap = prepForWrap
        )
    }

    /**
     * Converts [MessageEmbed] objects to [Page]s, and optionally adds page numbers to the embeds' footers.
     *
     * @param embeds List of embeds to paginate.
     * @param addPageNumbers If page numbers should be added to the embeds' footers.
     * @param skipPagesIfOnlyOne If page numbers should be skipped if there is only one embed.
     * @param mergeFooter Function to merge an embed's footer if there is already text in it.
     * @return List of [Page] object(s) representing the embeds.
     */
    private fun paginateEmbeds(
            embeds: Collection<MessageEmbed>,
            addPageNumbers: Boolean = true,
            skipPagesIfOnlyOne: Boolean = true,
            mergeFooter: (existing: String, toAdd: String) -> String =
                    { existing: String, toAdd: String ->
                        "$existing; $toAdd"
                    }
    ): List<Page> {
        return embeds.mapIndexed { index: Int, embed: MessageEmbed ->
            val paged = if (addPageNumbers && (!skipPagesIfOnlyOne || embeds.size > 1)) {
                var pageString = "Page ${index + 1}/${embeds.size}"

                if (embed.footer?.text != null) {
                    pageString = mergeFooter(embed.footer!!.text!!, pageString)
                }

                EmbedBuilder(embed)
                        .setFooter(pageString, embed.footer?.iconUrl)
                        .build()
            } else {
                embed
            }

            Page(PageType.EMBED, paged)
        }
    }
}
package com.dongtronic.diabot.platforms.discord.commands.info

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.commands.annotations.HomeGuildOnly
import com.dongtronic.diabot.data.mongodb.ProjectDAO
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed

class InfoCommands {
    private val logger = logger()

    @CommandMethod("info|i <project>")
    @CommandDescription("Project Information. Administrators can add new projects")
    @CommandCategory(Category.INFO)
    fun mainCommand(
            sender: JDACommandUser,
            @Argument("project")
            project: String
    ) {
        ProjectDAO.instance.getProject(project).subscribe({
            val builder = EmbedBuilder()

            builder.setTitle(it.name)
            builder.setDescription(it.text)

            sender.reply(builder.build()).subscribe()
        }, {
            if (it is NoSuchElementException) {
                sender.replyErrorS("Could not find project info for $project")
            } else {
                logger.warn("Could not retrieve project info", it)
                sender.replyErrorS("Could not retrieve project info: ${it.message}")
            }
        })
    }

    @CommandMethod("info list|ls|l")
    @CommandDescription("Lists all projects")
    @CommandCategory(Category.INFO)
    fun listProjects(sender: JDACommandUser) {
        ProjectDAO.instance.listProjects().collectSortedList().subscribe({ dtos ->
            val projects = dtos.map { it.name }
            val builder = EmbedBuilder()

            builder.setTitle("Available Projects")
            builder.addField(MessageEmbed.Field("Help", "Use `diabot info [project]` to get information about a project", false))
            builder.setDescription(projects.joinToString("\n"))

            sender.reply(builder.build()).subscribe()
        }, {
            if (it is NoSuchElementException) {
                sender.replyS("No projects are available.")
            } else {
                logger.warn("Could not get list of projects", it)
                sender.replyErrorS("Could not get list of projects: ${it.message}")
            }
        })
    }

    @HomeGuildOnly
    @DiscordPermission(Permission.MANAGE_CHANNEL)
    @CommandMethod("info set|s <project> <description>")
    @CommandDescription("Set project information")
    @CommandCategory(Category.INFO)
    @Example(["[set] openaps OpenAPS is a project for ..."])
    fun setProject(
            sender: JDACommandUser,
            @Argument("project")
            project: String,
            @Argument("description")
            @Greedy
            description: String
    ) {
        ProjectDAO.instance.setInfo(project, description).subscribe({
            if (it.upsertedId == null) {
                sender.replySuccessS("Description for `$project` updated")
            } else {
                sender.replySuccessS("Added project `$project`")
            }
        }, {
            logger.warn("Could not set project description", it)
            sender.replyErrorS("Could not set project description: ${it.message}")
        })
    }

    @HomeGuildOnly
    @DiscordPermission(Permission.MANAGE_CHANNEL)
    @CommandMethod("info delete|del|d|remove|rem|r <project>")
    @CommandDescription("Delete project information")
    @CommandCategory(Category.INFO)
    @Example(["[delete] openaps"])
    fun deleteProject(
            sender: JDACommandUser,
            @Argument("project")
            project: String
    ) {
        ProjectDAO.instance.deleteProject(project).subscribe({
            if (it.deletedCount == 1L) {
                sender.replySuccessS("Deleted project info for $project")
            } else {
                sender.replyErrorS("Could not find project for $project")
            }
        }, {
            logger.warn("Could not delete project info for $project", it)
            sender.replyErrorS("Could not delete project info for $project: ${it.message}")
        })
    }
}
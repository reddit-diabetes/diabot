package com.dongtronic.diabot.platforms.discord.commands.admin

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.commands.annotations.RequireAdminChannel
import com.dongtronic.diabot.data.mongodb.RewardsDAO
import com.dongtronic.diabot.nameOf
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.platforms.discord.utils.RoleUtils
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import java.util.*

class AdminRewardCommands {
    private val logger = logger()

    @GuildOnly
    @DiscordPermission(Permission.MANAGE_ROLES)
    @CommandMethod("admin rewards|reward|roles|r add|a <requiredRole> <rewardRole>")
    @CommandDescription("Add role reward")
    @CommandCategory(Category.ADMIN)
    fun addReward(
            sender: JDACommandUser,
            @Argument("requiredRole", description = "The role which is required for the reward role to be added")
            required: Role,
            @Argument("rewardRole", description = "The role which is granted as a reward")
            reward: Role
    ) {
        RewardsDAO.instance.changeRewardRole(sender.event.guild.id, required.id, reward.id, true).subscribe({
            sender.replySuccessS("Added reward **${reward.name}** for **${required.name}**")
        }, {
            logger.warn("Could not add reward role ${reward.id} for role ${required.id} under guild ${sender.event.guild.id}", it)
            sender.replyErrorS("Could not add reward **${reward.name}** for **${reward.name}**")
        })
    }

    @GuildOnly
    @DiscordPermission(Permission.MANAGE_ROLES)
    @CommandMethod("admin rewards delete|del|d|remove|rm|r <requiredRole> <rewardRole>")
    @CommandDescription("Delete role reward")
    @CommandCategory(Category.ADMIN)
    fun deleteReward(
            sender: JDACommandUser,
            @Argument("requiredRole", description = "The role which is required for the reward role")
            required: Role,
            @Argument("rewardRole", description = "The reward role to delete")
            reward: Role
    ) {
        RewardsDAO.instance.changeRewardRole(sender.event.guild.id, required.id, reward.id, false).subscribe({
            sender.replySuccessS("Removed reward **${reward.name}** for **${required.name}**")
        }, {
            logger.warn("Could not remove reward role ${reward.id} for role ${required.id} under guild ${sender.event.guild.id}", it)
            sender.replyErrorS("Could not remove reward **${reward.name}** for **${reward.name}**")
        })
    }

    @GuildOnly
    @CommandMethod("admin rewards list|ls|l")
    @CommandDescription("List configured rewards")
    @CommandCategory(Category.ADMIN)
    fun listRewards(sender: JDACommandUser) {
        val guild = sender.event.guild

        RewardsDAO.instance.getRewards(guild.id)
                .defaultIfEmpty(emptyList())
                .subscribe({ dtos ->
                    val rewards = RoleUtils.buildRewardsMap(dtos, guild)
                    val builder = EmbedBuilder()
                    buildRewardResponse(builder, rewards, guild)
                    sender.reply(builder.build()).subscribe()
                }, {
                    logger.warn("Could not retrieve rewards for guild ${guild.id}", it)
                    sender.replyErrorS("Could not retrieve list of rewards for this guild")
                })
    }

    private fun buildRewardResponse(builder: EmbedBuilder, rewards: TreeMap<Role, List<Role>>, guild: Guild) {
        builder.setTitle("${rewards.size} Rewards for ${guild.name}")
        builder.setColor(java.awt.Color.ORANGE)

        for ((required, rewardList) in rewards) {
            val rewardString = StringBuilder()
            for (reward in rewardList) {
                rewardString.append(reward.name)
                rewardString.append(", ")
            }
            rewardString.trimEnd(',', ' ')

            builder.appendDescription("**")
            builder.appendDescription(required.name)
            builder.appendDescription("** => ")
            builder.appendDescription(rewardString.toString())
            builder.appendDescription("\n")
        }
    }

    @GuildOnly
    @DiscordPermission(Permission.MANAGE_ROLES)
    @CommandMethod("admin rewards optin|oi|i <user>")
    @CommandDescription("Opt user in to rewards")
    @CommandCategory(Category.ADMIN)
    fun optInUser(
            sender: JDACommandUser,
            @Argument("user", description = "The user to opt-in to rewards")
            user: User
    ) {
        val effectiveName = sender.event.nameOf(user)

        RewardsDAO.instance.changeOpt(sender.event.guild.id, user.id, false).subscribe({
            logger.info("User $effectiveName (${user.id}) opted in to rewards, $it")
            sender.replySuccessS("User $effectiveName opted in to rewards")
        }, {
            logger.warn("Error while opting user $effectiveName (${user.id}) in to rewards", it)
            sender.replyErrorS("Could not opt user $effectiveName in to rewards")
        })
    }

    @GuildOnly
    @DiscordPermission(Permission.MANAGE_ROLES)
    @CommandMethod("admin rewards optout|oo|o <user>")
    @CommandDescription("Opt user out of rewards")
    @CommandCategory(Category.ADMIN)
    fun optOutUser(
            sender: JDACommandUser,
            @Argument("user", description = "The user to opt-out of rewards")
            user: User
    ) {
        val effectiveName = sender.event.nameOf(user)

        RewardsDAO.instance.changeOpt(sender.event.guild.id, user.id, true).subscribe({
            logger.info("User $effectiveName (${user.id}) opted out of rewards, $it")
            sender.replySuccessS("User $effectiveName opted out of rewards")
        }, {
            logger.warn("Error while opting user $effectiveName (${user.id}) out of rewards", it)
            sender.replyErrorS("Could not opt user $effectiveName out of rewards")
        })
    }

    @GuildOnly
    @RequireAdminChannel
    @DiscordPermission(Permission.MANAGE_ROLES)
    @CommandMethod("admin rewards listoptouts|lo")
    @CommandDescription("List opted-out users")
    @CommandCategory(Category.ADMIN)
    fun listOptOuts(sender: JDACommandUser) {
        RewardsDAO.instance.getOptOuts(sender.event.guild.id)
                .map { it.optOut }
                .defaultIfEmpty(emptyList())
                .subscribe({ optouts ->
                    val builder = EmbedBuilder()

                    builder.setTitle("Reward opt-outs")

                    val members = optouts.mapNotNull { sender.event.guild.getMemberById(it) }

                    if (members.isEmpty()) {
                        builder.setDescription("No users are opted-out.")
                    } else {
                        members.forEach { member ->
                            builder.appendDescription("**${member.effectiveName}** (`${member.user.id}`)")
                        }
                    }

                    sender.reply(builder.build()).subscribe()
                }, {
                    logger.warn("Could not retrieve opt outs for guild ${sender.event.guild.id}", it)
                    sender.replyErrorS("Could not retrieve list of opt outs for this guild")
                })
    }
}
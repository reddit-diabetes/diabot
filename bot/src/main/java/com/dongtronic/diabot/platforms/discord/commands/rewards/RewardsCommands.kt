package com.dongtronic.diabot.platforms.discord.commands.rewards

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.data.mongodb.RewardsDAO
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger

class RewardsCommands {
    private val logger = logger()

    @GuildOnly
    @CommandMethod("rewards|reward|r optin|oi|i")
    @CommandDescription("Opt in to receiving automatic role rewards")
    @CommandCategory(Category.UTILITIES)
    @Example(["[optin]"])
    fun optIn(sender: JDACommandUser) {
        changeOpt(sender, false)
    }

    @GuildOnly
    @CommandMethod("rewards optout|ou|o")
    @CommandDescription("Opt out of receiving automatic role rewards")
    @CommandCategory(Category.UTILITIES)
    @Example(["[optout]"])
    fun optOut(sender: JDACommandUser) {
        changeOpt(sender, true)
    }

    private fun changeOpt(sender: JDACommandUser, optOut: Boolean) {
        val guildId = sender.event.guild.id

        val user = sender.event.author
        RewardsDAO.instance.changeOpt(guildId, user.id, optOut).subscribe({
            logger.info("User ${user.name}#${user.discriminator} (${user.id}) opted out of rewards, $it")
            sender.replySuccessS("Opted out of rewards")
        }, {
            logger.warn("Error while opting user ${user.name}#${user.discriminator} (${user.id}) out of rewards", it)
            sender.replyErrorS("Could not opt out of rewards")
        })
    }
}
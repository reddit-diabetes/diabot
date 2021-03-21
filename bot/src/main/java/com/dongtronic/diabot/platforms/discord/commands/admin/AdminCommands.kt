package com.dongtronic.diabot.platforms.discord.commands.admin

import com.dongtronic.diabot.commands.annotations.ChildCommands

@ChildCommands(
        AdminAnnounceCommand::class,
        AdminChannelCommands::class,
        AdminRewardCommands::class,
        AdminUsernameCommands::class
)
class AdminCommands
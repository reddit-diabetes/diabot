## Display Options

### `title`
Your Nightscout display title. [This can be changed on your Nightscout instance through the `CUSTOM_TITLE` environment variable](https://github.com/nightscout/cgm-remote-monitor#predefined-values-for-your-browser-settings-optional). You may also add custom emotes to your Nightscout title by embedding the emote's raw value in the `CUSTOM_TITLE` variable. The emote's raw value can be found by typing the emote in Discord and prefixing it with a backslash `\`, like `\:youremote:`.

### `trend`
The current BG trend arrow in Nightscout.

### `cob`
The amount of carbs on board. This value is reported by either `devicestatus` (OpenAPS / AndroidAPS) or Nightscout's careportal. The `cob` plugin must be enabled on the Nightscout instance for this to be displayed.

### `iob`
The amount of insulin on board. This value is reported by either `devicestatus` (OpenAPS / AndroidAPS) or Nightscout's careportal. The `iob` plugin must be enabled on the Nightscout instance for this to be displayed.

### `avatar`
Adds your Discord profile picture to the embed.

### `simple`
Removes your Discord profile picture from the embed.
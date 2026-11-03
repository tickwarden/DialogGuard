# DialogGuard

DialogGuard is a **client-side Fabric mod** intended to reduce phishing through
Minecraft dialog screens. It inspects server-sent `show_dialog` packets and
also checks dialog screens opened through routes that do not use that packet.
The policy can block dialogs containing input controls or text matching
configured sensitive terms, then notify the player.

This is a heuristic defence, **not proof that a server, link, message or dialog
is safe**. False positives and missed scams are possible. Do not enter account
passwords, device-login codes, recovery codes or other secrets into an
untrusted server's dialog, chat, website or form.

## Requirements and installation

The checked-in [`fabric.mod.json`](src/main/resources/fabric.mod.json) declares:

- A client environment, Java 21 or newer, Fabric Loader `>=0.16.14`, Fabric API,
  and Minecraft `>=1.21.6 <=1.21.11`.
- Mod version supplied during the Gradle build; the current
  [`gradle.properties`](gradle.properties) sets `1.1.1`, targets Minecraft
  `1.21.8`, Yarn `1.21.8+build.1`, Loader `0.17.2`, and Fabric API
  `0.136.0+1.21.8`.

Those are **declared** compatibility bounds, not an independently verified
runtime matrix. A jar is produced under `build/libs/` by the Gradle build; put
a trusted compatible jar and Fabric API in the client's `mods` directory and
restart that client. The mod is not a server-side filter for other players.

## Detection and response

1. `ClientCommonNetworkHandlerMixin` inspects `ShowDialogS2CPacket` before
   the normal screen is opened. It is intended to cover both play and
   configuration phases; a blocking verdict cancels the packet. If inspection
   throws, that handler cancels rather than allowing the dialog.
2. `ScreenMixin` checks a `DialogScreen` after initialization, including a
   locally opened dialog such as a chat `show_dialog` click action. On a
   blocking verdict it neutralizes supported text widgets and schedules a
   return to the parent screen. Its inspection exception path also blocks.
3. `DialogInspector` passes the dialog title, optional external title, body
   and input descriptions to the policy engine. By default, an input control
   alone is enough to block. For text matching, one strong keyword or two
   **distinct** weak keywords are enough; matching is case-insensitive and
   starts at a word boundary, but is prefix-based rather than a full semantic
   analysis. Duplicate configured weak terms count once.
4. A warning can appear as a banner, toast, chat message and log entry.
   Repeated reports with the same reason within 750 ms are deduplicated;
   toast/chat alerts have a separate configurable cooldown. Disconnecting
   clears session state.

The fallback explicitly clears single-line `TextFieldWidget` text before
hiding/disabling it. For multiline `EditBoxWidget`, the current code hides and
disables the widget but does not explicitly clear its text. Other widget types
are not claimed to be cleared. Text representation, translations, other UI
routes and future Minecraft changes may all affect detection.

## Configuration

The Fabric config directory contains `dialogguard.json` (normally
`config/dialogguard.json`). The loader starts with defaults, reads the file
when present, sanitizes several values and **writes the resulting JSON back**,
including on reload. Preserve a copy before editing or reloading if its exact
format or extra fields matter. On a read/write or JSON parse error it reports
an error and activates defaults in memory.

| Setting | Default | Meaning |
| --- | --- | --- |
| `blockInputDialogs` | `true` | Block dialogs with any input control. |
| `blockKeywordDialogs` | `true` | Apply strong/weak keyword rules. |
| `showBanner`, `showToast`, `echoToChat` | `true` | Enable each notification channel. |
| `bannerSeconds` | `15` | Banner duration, clamped to 3–120 seconds. |
| `notificationCooldownSeconds` | `4` | Toast/chat cooldown, clamped to 0–120 seconds; `0` disables throttling. |
| `strongKeywords`, `weakKeywords` | Built-in lists | Customizable terms; null lists are replaced with defaults. |

Default keyword lists include terms in several languages. Broad terms can
block legitimate dialogs, while different wording or non-text presentation can
avoid a match. Disabling either policy reduces protection; disabling both
allows dialogs through the policy engine.

## Client commands

These are registered as Fabric **client** commands:

- `/dialogguard` — show the available subcommands and config path.
- `/dialogguard status` — show active rules, notification cooldown, block
  count and, if available, the last reason and elapsed time.
- `/dialogguard reload` — reload and rewrite the config; on failure, report
  that defaults are active.
- `/dialogguard reset` — reset session counters without hiding an active
  banner.

## Build and tests

The checked-in wrapper points to Gradle `9.7.1`; the project uses the
`fabric-loom` `1.11-SNAPSHOT` plugin and Java 21. A normal build is requested
with:

```sh
./gradlew build
```

Gradle/Loom may download the wrapper, plugins and game dependencies. The
repository's Fabric GameTest configuration sets **`eula = true`** and enables
server GameTests; `runGameTest` is configured to launch a forked Minecraft
test server. Review the [Minecraft EULA](https://www.minecraft.net/eula), the
build configuration and your network/environment before running any task that
starts those tests. Do not run them against a real server or world. The source
contains GameTests for the pure policy engine's input, strong/weak term,
duplicate and boundary rules; it does not establish an end-to-end client or
version-compatibility test matrix.

A manual example from the project, **only in a disposable creative/op world**,
is:

```mcfunction
/dialog show @s {type:"minecraft:notice",title:"Microsoft login",inputs:[{type:"minecraft:text",key:"c",label:"Verification code"}]}
```

Do not substitute a real credential or verification code. This README update
was based on source inspection; Gradle, GameTests, a Minecraft server and a
client were not run for this documentation change.

## Source map

| Path | Role |
| --- | --- |
| [`DialogGuardClient.java`](src/main/java/dev/dialogguard/DialogGuardClient.java) | Client entry point, event registration and banner hooks. |
| [`DialogRiskAnalyzer.java`](src/main/java/dev/dialogguard/DialogRiskAnalyzer.java) | Input and keyword policy. |
| [`DialogInspector.java`](src/main/java/dev/dialogguard/DialogInspector.java) | Adapts dialog model data to the policy. |
| [`InputNeutralizer.java`](src/main/java/dev/dialogguard/InputNeutralizer.java) | Hides/disables supported text widgets. |
| [`GuardConfig.java`](src/main/java/dev/dialogguard/GuardConfig.java), [`GuardState.java`](src/main/java/dev/dialogguard/GuardState.java) | Configuration, notification and session state. |
| [`mixin/`](src/main/java/dev/dialogguard/mixin/) | Packet interception, screen fallback and dialog accessor. |
| [`DialogRiskAnalyzerGameTest.java`](src/gametest/java/dev/dialogguard/gametest/DialogRiskAnalyzerGameTest.java) | Server GameTests for the policy engine. |

## License

[MIT](LICENSE). See the license file for its copyright and conditions.

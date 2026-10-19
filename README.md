# DialogGuard

Client-side Fabric mod (MC 1.21.6 / 1.21.8 / 1.21.11) that blocks phishing through server-sent dialogs.

- **Packet guard:** drops the `show_dialog` S2C packet (play + configuration phase) if its dialog has input fields or reads like a credential / verification-code request.
- **Screen fallback:** inspects `DialogScreen` itself as well, covering locally opened dialogs such as chat `show_dialog` click events that bypass the packet hook. Blocked screens have their inputs neutralized and are returned to their parent screen.
- **Warnings:** red banner, toast and chat warning. Toast/chat alerts are rate-limited to avoid notification floods; the banner and session count still update for each non-duplicate block.
- **In-game commands:** `/dialogguard status`, `/dialogguard reload`, and `/dialogguard reset` show current policy/session stats, reload `config/dialogguard.json`, and reset the session counters.
- **GameTests:** the server GameTest suite covers input blocking, strong/weak keyword rules, duplicate keyword handling and word boundaries.

Configuration: `config/dialogguard.json`. `notificationCooldownSeconds` defaults to `4`; set it to `0` to disable toast/chat throttling. Keyword lists and existing warning options remain configurable.

Build and run the server GameTests with `./gradlew build` (JDK 21, Gradle 8.14+). Jar: `build/libs/`.

Manual test (creative/op, single player):

`/dialog show @s {type:"minecraft:notice",title:"Microsoft login",inputs:[{type:"minecraft:text",key:"c",label:"Verification code"}]}`

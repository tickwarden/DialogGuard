# DialogGuard

Client-side Fabric mod (MC 1.21.6 / 1.21.8 / 1.21.11) that blocks phishing through server-sent dialogs.

- Layer 1: drops the `show_dialog` S2C packet (play + configuration phase) if it has input fields or reads like a credential / verification-code request.
- Layer 2: any `DialogScreen` that still opens (e.g. chat `show_dialog` click event) gets its text fields disabled and is closed.
- Red banner + toast + chat line: "SECURITY WARNING: Servers can never safely request your Microsoft credentials or verification codes."
- Config: `config/dialogguard.json`.

Build: `./gradlew build` (JDK 21, Gradle 8.14+; run `gradle wrapper` once if no wrapper is present). Jar: `build/libs/`.

Manual test (creative/op, single player): 
`/dialog show @s {type:"minecraft:notice",title:"Microsoft login",inputs:[{type:"minecraft:text",key:"c",label:"Verification code"}]}`

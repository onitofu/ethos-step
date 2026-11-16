# Ethos Step

A minimal Paper plugin that permanently unlocks a configurable base step height.
No commands, timers, databases or other plugins are required.

## Compatibility

- Target: Paper 1.21.x and 26.x, using the same JAR.
- Paper 1.21.x requires Java 21; Paper 26.x requires Java 25.
- Compiled against Paper 1.21 with Java 21 bytecode and `api-version: '1.21'`.
- Resolves both `minecraft:step_height` and the older `minecraft:generic.step_height`.
- Folia is not supported. Future Paper releases require compatibility verification.

See [Paper's Java requirements](https://docs.papermc.io/paper/getting-started/).

### Verified versions

The same Java 21 JAR was tested on these actual Paper servers:

| Paper | Build | Java | Verification |
| --- | --- | --- | --- |
| 1.21 | 130 | 21 | Full player lifecycle |
| 1.21.3 | 83 | 21 | Full player lifecycle |
| 1.21.11 | 132 | 21 | Full player lifecycle |
| 26.1.2 | 74 | 25 | Full player lifecycle |
| 26.2 | 135 | 25 | Full player lifecycle |

Full lifecycle verification used a connected test player and checked denial without
permission, first grant, permission removal, death/respawn, reconnect, server restart
and a config update from `1.0` to `1.5`.

## Installation

1. Put `ethos-step-1.0.0.jar` in the server's `plugins/` directory.
2. Restart the server to generate `plugins/ethos-step/config.yml`.
3. Grant `ethos.step.use` to the desired players or groups in your permissions plugin.
   The permission defaults to `false`, including for operators.

For example, with LuckPerms:

```text
/lp user <player> permission set ethos.step.use true
/lp group <group> permission set ethos.step.use true
```

## Configuration and persistence

```yaml
step-height: 1.0
```

This is the **base height in blocks**, not an additive bonus. A value of `1.0`
allows stepping onto a full block at normal player scale. Other plugins' attribute
modifiers remain in place and can affect the resulting height. Minecraft still
applies its attribute limits. Invalid, negative or non-finite settings fall back
to `1.0` with a warning in the server log.

The plugin applies the height on login, after respawn, and to online players when
the plugin is enabled. A permission granted during play takes effect at the next
one of these events; there is no permission polling.

The first successful grant stores `ethos-step:granted` in the player's persistent
data. Once unlocked, the height is restored even if the permission is removed.
It survives reconnects, deaths and normal server restarts while the plugin remains
installed. The unlock is part of player data: deleting player data also deletes it.

After editing the config, restart the server. All previously unlocked players
receive the new configured value when next processed, even without the permission.
Disabling or removing the plugin does not roll back the attribute, but automatic
restoration after respawn requires the plugin. There is no revoke/reset command.

## Building and CI

Build with JDK 21 or 25:

```sh
./gradlew clean build
```

The JAR is written to `build/libs/ethos-step-1.0.0.jar`. Paper and the test
dependencies are not bundled. Checkstyle runs as part of `build`; JUnit results
are in `build/reports/tests/test/` and JaCoCo reports in
`build/reports/jacoco/test/html/`.

GitHub Actions follows the sibling plugins' Build & Check workflow on pushes and
pull requests to `main`/`master`. It runs Checkstyle, builds and tests on Java 21
and 25, then uploads the Java 21 JAR as the `ethos-step` artifact.

The automated tests cover permission gating, permanent unlocks, respawn-style
attribute replacement, changed configuration, repeated application, both registry
names and invalid settings. Java-version CI checks do not substitute for testing
the JAR on actual Paper servers.

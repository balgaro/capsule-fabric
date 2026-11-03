# Capsule for Fabric (unofficial port)

An unofficial **Fabric 1.20.1 port** of [Capsule by Lythom](https://github.com/Lythom/capsule): capsules capture a region
containing any blocks or machines, then deploy and undeploy it at will. Inspired by Dragon Ball capsules.

All the credit for the mod itself goes to **Lythom (Samuel Bouchet)**. This repository only adapts the 1.20 (Forge) branch to
Fabric; the original is MIT licensed and so is this port (see [LICENSE.md](LICENSE.md)). The original mod pages and wiki still
describe how capsules work: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/capsule),
[wiki](https://github.com/Lythom/capsule/wiki). The original README is kept as [UPSTREAM_README.md](UPSTREAM_README.md).

> **Status: experimental.** The server side is exercised by an automated smoke test (below). Anything that only runs on a
> game client (the deploy preview, item colors and model states, the label screen, JEI) is ported but **has not been tried
> in a real client**. Please report what you find.

## Requirements

- Minecraft **1.20.1**, Fabric Loader 0.15+, Java 17
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) (the config uses Forge's config format)
- Optional: JEI (recipes), Open Parties and Claims or Flan (claim protection)

The mod adds items, blocks and a screen, so **it must be installed on the server and on every client.**

Config: `config/capsule-common.toml` and the folders under `config/capsule/`, as in the original.

## Build

```
./gradlew build
```

The jar is `build/libs/Capsule-Fabric-1.20.1-<version>.jar`. Needs a JDK 17.

## What was changed from the Forge version

See [PORTING.md](PORTING.md) for the details. In short: registries, events, networking, config and data files were moved to
Fabric APIs; Forge-only hooks were replaced by mixins; claim mods are asked through their Fabric APIs (Open Parties and
Claims by reflection, so it works with old and new versions). SecurityCraft support is gone (SecurityCraft has no Fabric
version).

## Smoke test

[tools/smoke-test](tools/smoke-test) starts nothing by itself: it expects a Fabric 1.20.1 test server in Docker and drives it
with a [mineflayer](https://github.com/PrismarineJS/mineflayer) bot. It checks that an empty capsule captures a chest with its
items, that deploying brings the chest back with the items, and that the normal loop (right click, throw, capture, pick up,
throw, deploy) works for a size 3 capsule.

## Credits

- Capsule: [Lythom](https://github.com/Lythom), MIT license
- Fabric port: [balgaro](https://github.com/balgaro)

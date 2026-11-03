# Smoke test

Needs Docker and Node 18+. It talks to a throwaway Fabric 1.20.1 server in a container named `capsule-test`, with the
capsule jar, Fabric API and Forge Config API Port installed, offline mode, RCON enabled, the bot `capbot` as operator, on a
flat world, published on port 25570:

```
docker run -d --name capsule-test -p 25570:25565 \
  -e EULA=TRUE -e TYPE=FABRIC -e VERSION=1.20.1 -e MEMORY=2G -e ONLINE_MODE=FALSE \
  -e MODRINTH_PROJECTS="fabric-api,forge-config-api-port" -e MODE=creative -e LEVEL_TYPE=minecraft:flat \
  -e SPAWN_PROTECTION=0 -e ENABLE_RCON=true -e OPS=capbot \
  -v "$PWD/mods:/mods:ro" itzg/minecraft-server:java17      # put the built jar in ./mods
npm install
npm test
```

`test.js`: an instant (size 1) capsule captures a chest with items and deploys it again with the items.
`test-throw.js`: a size 3 capsule is activated, thrown onto a capture base, captures a 3x3x3 cube, is picked up again,
activated, thrown, and deploys the cube.

The bot sends the throw message by hand (what the client would send), so this checks the server side only, not the client UI.
mineflayer logs a lot of noise about modded recipes it cannot parse; that is expected.

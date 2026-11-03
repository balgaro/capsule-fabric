// Smoke test for the Fabric port of Capsule: a bot joins, gets an instant (size 1) empty capsule,
// captures a chest with items, then deploys it again, all through the real server code paths.
const mineflayer = require('mineflayer')
const { execSync } = require('child_process')

const rcon = (cmd) => {
  try { return execSync(`docker exec capsule-test rcon-cli "${cmd.replace(/"/g, '\\"')}"`, { encoding: 'utf8' }).trim() }
  catch (e) { return 'ERR ' + (e.stdout || e.message) }
}
const sleep = (ms) => new Promise(r => setTimeout(r, ms))
const blockAt = (x, y, z, id) => rcon(`execute if block ${x} ${y} ${z} ${id}`).startsWith('Test passed')

function packLong(x, y, z) {
  const X = BigInt(x) & 0x3FFFFFFn, Y = BigInt(y) & 0xFFFn, Z = BigInt(z) & 0x3FFFFFFn
  return (X << 38n) | (Z << 12n) | Y
}
function throwPacket(x, y, z, instant) {
  const b = Buffer.alloc(11)
  b.writeUInt8(3, 0)            // message id: CapsuleThrowQueryToServer
  b.writeUInt8(instant ? 1 : 0, 1)
  b.writeUInt8(1, 2)            // has position
  b.writeBigUInt64BE(BigInt.asUintN(64, packLong(x, y, z)), 3)
  return b
}
const send = (x, y, z) => bot._client.write('custom_payload', { channel: 'capsule:capsule_channel', data: throwPacket(x, y, z, true) })
const stateOf = (item) => item && item.nbt && item.nbt.value && item.nbt.value.state ? item.nbt.value.state.value : null

const bot = mineflayer.createBot({ host: 'localhost', port: 25570, username: 'capbot', version: '1.20.1' })
bot.on('error', e => { console.log('bot error', e.message); process.exit(2) })
bot.on('kicked', r => { console.log('kicked', JSON.stringify(r)); process.exit(2) })

bot.once('spawn', async () => {
  const results = []
  const check = (name, ok, extra = '') => { results.push(ok); console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')) }
  try {
    await sleep(3000)
    const g = Math.floor(bot.entity.position.y) - 1   // top solid block under the bot
    const X = 11, Z = 11
    console.log('ground y =', g)
    rcon(`tp capbot ${X} ${g + 1} 5`)
    await sleep(1500)
    rcon(`setblock ${X} ${g + 1} ${Z} chest{Items:[{Slot:0b,id:"minecraft:diamond",Count:5b},{Slot:3b,id:"minecraft:apple",Count:7b}]}`)
    check('chest placed', blockAt(X, g + 1, Z, 'minecraft:chest'))

    rcon('clear capbot')
    rcon('execute as capbot run capsule giveEmpty 1 false')
    await sleep(1000)
    bot.setQuickBarSlot(0)
    await sleep(500)
    const held = bot.heldItem
    check('got an empty instant capsule', !!held && stateOf(held) === 0, 'state=' + stateOf(held))

    // capture: aim at the chest
    send(X, g + 1, Z)
    await sleep(3000)
    check('chest captured (block removed)', !blockAt(X, g + 1, Z, 'minecraft:chest'))
    const afterCapture = bot.heldItem
    check('capsule is linked now', stateOf(afterCapture) === 2, 'state=' + stateOf(afterCapture))
    console.log('stray items nearby after capture:', rcon(`execute if entity @e[type=item,x=${X},y=${g + 1},z=${Z},distance=..8]`))

    // deploy: aim at the block above the ground (what the client sends for a deploy)
    send(X, g + 1, Z)
    await sleep(3000)
    check('chest deployed again', blockAt(X, g + 1, Z, 'minecraft:chest'))
    const contents = rcon(`data get block ${X} ${g + 1} ${Z} Items`)
    console.log('chest contents:', contents)
    check('contents preserved', contents.includes('minecraft:diamond') && contents.includes('minecraft:apple'))
  } catch (e) {
    console.log('test crashed:', e.stack)
    results.push(false)
  }
  console.log(results.every(Boolean) ? 'ALL PASSED' : 'SOME FAILED')
  bot.quit()
  setTimeout(() => process.exit(results.every(Boolean) ? 0 : 1), 500)
})

// Second smoke test: the normal gameplay loop of a size 3 capsule.
// right click = activate, throw (what the client sends) = the capsule flies, lands, and captures or deploys by itself.
const mineflayer = require('mineflayer')
const { execSync } = require('child_process')

const rcon = (cmd) => {
  try { return execSync(`docker exec capsule-test rcon-cli "${cmd.replace(/"/g, '\\"')}"`, { encoding: 'utf8' }).trim() }
  catch (e) { return 'ERR ' + (e.stdout || e.message) }
}
const sleep = (ms) => new Promise(r => setTimeout(r, ms))
const blockAt = (x, y, z, id) => rcon(`execute if block ${x} ${y} ${z} ${id}`).startsWith('Test passed')
const countStone = (x0, y0, z0, x1, y1, z1) => {
  let n = 0
  for (let x = x0; x <= x1; x++) for (let y = y0; y <= y1; y++) for (let z = z0; z <= z1; z++) if (blockAt(x, y, z, 'minecraft:cobblestone')) n++
  return n
}
function packLong(x, y, z) {
  const X = BigInt(x) & 0x3FFFFFFn, Y = BigInt(y) & 0xFFFn, Z = BigInt(z) & 0x3FFFFFFn
  return (X << 38n) | (Z << 12n) | Y
}
function throwPacket(x, y, z, instant) {
  const b = Buffer.alloc(11)
  b.writeUInt8(3, 0); b.writeUInt8(instant ? 1 : 0, 1); b.writeUInt8(1, 2)
  b.writeBigUInt64BE(BigInt.asUintN(64, packLong(x, y, z)), 3)
  return b
}
const stateOf = (item) => item && item.nbt && item.nbt.value && item.nbt.value.state ? item.nbt.value.state.value : null

const bot = mineflayer.createBot({ host: 'localhost', port: 25570, username: 'capbot', version: '1.20.1' })
bot.on('error', e => { console.log('bot error', e.message); process.exit(2) })
bot.on('kicked', r => { console.log('kicked', JSON.stringify(r)); process.exit(2) })

bot.once('spawn', async () => {
  const results = []
  const check = (name, ok, extra = '') => { results.push(ok); console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')) }
  try {
    await sleep(3000)
    const g = Math.floor(bot.entity.position.y) - 1
    // a 3x3x3 cobblestone cube with the capture base block under its centre; the bot stands a few blocks away
    rcon(`fill 8 ${g + 1} 8 16 ${g + 6} 16 air`)
    rcon(`fill 8 ${g} 8 16 ${g} 16 grass_block`)
    rcon(`tp capbot 12 ${g + 1} 6`)
    await sleep(1500)
    rcon(`fill 11 ${g + 1} 11 13 ${g + 3} 13 cobblestone`)
    check('cube built', countStone(11, g + 1, 11, 13, g + 3, 13) === 27)
    // the capture base (a marker block facing up) under the centre of the cube: the cube is the area above it
    rcon(`setblock 12 ${g} 12 capsule:capsulemarker[facing=up]`)
    await sleep(500)
    check('capture base placed', blockAt(12, g, 12, 'capsule:capsulemarker'))

    rcon('clear capbot')
    rcon('execute as capbot run capsule giveEmpty 3 false')
    await sleep(1000)
    bot.setQuickBarSlot(0)
    await sleep(500)
    check('got a size 3 capsule', stateOf(bot.heldItem) === 0, 'state=' + stateOf(bot.heldItem))

    // right click: activate
    bot.activateItem()
    await sleep(1000)
    check('capsule activated by right click', stateOf(bot.heldItem) === 4, 'state=' + stateOf(bot.heldItem))

    // throw at the ground block next to the cube (the client sends the aimed block)
    bot._client.write('custom_payload', { channel: 'capsule:capsule_channel', data: throwPacket(12, g + 1, 12, false) })
    await sleep(8000)
    const left = countStone(11, g + 1, 11, 13, g + 3, 13)
    check('thrown capsule captured the cube', left === 0, 'cobblestone left: ' + left)
    console.log('item entities near:', rcon(`execute if entity @e[type=item,x=12,y=${g + 2},z=12,distance=..12]`))
    // the capsule was dropped as an item entity: pick it up again
    rcon('tp capbot 12 ' + (g + 1) + ' 12')
    rcon('tp @e[type=item] capbot')
    await sleep(4000)
    const inv = bot.inventory.items().map(i => i.name + ':' + stateOf(i)).join(', ')
    console.log('inventory:', inv)
    const linked = bot.inventory.items().find(i => stateOf(i) === 2)
    check('capsule picked up again, linked to the capture', !!linked, inv)
    if (linked) {
      await bot.equip(linked, 'hand')
      await sleep(500)
      // deploy elsewhere: activate, then throw to a spot on the ground
      rcon(`fill 8 ${g + 1} 20 16 ${g + 6} 28 air`)
      rcon(`fill 8 ${g} 20 16 ${g} 28 grass_block`)
      rcon(`tp capbot 12 ${g + 1} 18`)
      await sleep(1500)
      bot.activateItem()
      await sleep(1000)
      check('linked capsule activated', stateOf(bot.heldItem) === 1, 'state=' + stateOf(bot.heldItem))
      bot._client.write('custom_payload', { channel: 'capsule:capsule_channel', data: throwPacket(12, g + 1, 24, false) })
      await sleep(8000)
      const placed = countStone(11, g + 1, 23, 13, g + 3, 25) + countStone(11, g + 2, 23, 13, g + 4, 25) * 0
      console.log('cobblestone in the deploy area (x11-13, y' + (g + 1) + '-' + (g + 3) + ', z23-25):', placed)
      check('deployed cube is back (27 blocks)', placed === 27, 'blocks=' + placed)
    }
  } catch (e) {
    console.log('test crashed:', e.stack)
    results.push(false)
  }
  console.log(results.every(Boolean) ? 'ALL PASSED' : 'SOME FAILED')
  bot.quit()
  setTimeout(() => process.exit(results.every(Boolean) ? 0 : 1), 500)
})

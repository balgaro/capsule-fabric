# Porting notes (Forge 1.20.1 to Fabric 1.20.1)

Base: the `1.20` branch of [Lythom/capsule](https://github.com/Lythom/capsule), mod version 8.0. The game logic (capture, deploy,
templates, blueprints, recipes, commands) is unchanged and uses the same Mojang mappings, so most of the code is the original.

## Approach

Rather than rewrite every call site, small stand-ins in `capsule.compat` keep the original shapes where that was safe:

| Forge | Fabric port |
|---|---|
| `DeferredRegister` / `RegistryObject` / `ForgeRegistries` | `capsule.compat.*`: register immediately with vanilla registries |
| `SimpleChannel`, `NetworkEvent`, `PacketDistributor` | `capsule.compat.network.*` on Fabric networking, one channel, message id prefix |
| `IItemHandler` capability | `capsule.compat.IItemHandler` backed by the Fabric Transfer API or a vanilla `Container` |
| `PlayerInteractEvent`, `TickEvent`, ... | small `CompatEvents` objects built by Fabric callbacks, so handler bodies are unchanged |

## Things that needed more than a rename

- **Network threading.** Forge handlers run through `enqueueWork`; Fabric delivers packets on the network thread. The stand-in's
  `enqueueWork` hops to the server/client main thread. Without that, block entities cannot be read (a captured chest came back empty).
- **Forge hooks with no Fabric equivalent**, done with mixins (`capsule.mixin`):
  - `EntityJoinLevelEvent` cancellation during a capture (so popped items do not appear): `ServerLevelMixin`
  - `Item.onEntityItemUpdate` (a thrown capsule flies to its target and deploys on landing): `ItemEntityMixin`
  - `PlayerInteractEvent.LeftClickEmpty` (rotate a blueprint by left click in the air): `MinecraftMixin`
- **Claim protection probe.** Forge asked mods without an adapter with a `BlockEvent.EntityPlaceEvent`. On Fabric the probe fires the
  use/attack/break callbacks (guarded so Capsule's own handlers ignore them) and treats a `FAIL` as denied.
  Open Parties and Claims is called by reflection (its API method names changed between versions).
- **Custom ingredient.** `capsule:ingredient` is a Fabric `CustomIngredient`. In recipes the key is `"fabric:type"`, and it cannot be
  inside a plain array: those arrays became `fabric:any`.
- **Data files.** `forge:` tags became `c:` tags, `forge:not`/`forge:tag_empty` conditions became `fabric:load_conditions` with
  `fabric:tags_populated`.
- **Persistent player data.** Forge's `getPersistentData()` is replaced by a scoreboard tag for the starter capsules gift, and
  an in-memory tag for the capture base preview (it was client only).
- **Rendering.** Forge's fluid/render hooks became the Fabric `FluidRenderHandler`, `WorldRenderEvents.LAST` and
  `ColorProviderRegistry`.
- **Block rotation** uses vanilla `BlockState.rotate(Rotation)` (Forge's version could call block specific overrides).
- **Removed:** SecurityCraft owner check (no Fabric version of SecurityCraft). The Forge `setNoRepair()` item property.

## Known gaps

- No client has been used with this build. The server side loop is covered by `tools/smoke-test`.
- The deploy preview draws in `WorldRenderEvents.LAST`; Forge drew after particles, so ordering with other overlays may differ.
- Fluid previews use the registered Fabric fluid render handler; fluids without one are skipped.

# Adding 3D model effects (ModelFx)

ModelFx lets any class show a 3D model in the world for a while: a summoned weapon, ice on a frozen mob, a floating rune, a shield... The Hand of Order hammer and the freeze crystals are both built with it.

Nothing needs registering. You make a model, drop two files into the right folders, describe the effect in code and play it.

---

## 1. Make the model in Blockbench

1. **File → New → Java Block/Item.**
2. Build it out of cubes and paint it.
3. Keep these rules in mind (Minecraft's limits for this model type):
   - Coordinates must stay between **-16 and 32 pixels** on every axis (3 blocks). Make big things small and enlarge them with `.scale(...)` in code.
   - A cube can only be rotated by **-45, -22.5, 0, 22.5 or 45 degrees**, on one axis.
   - The model's **front is North (-Z)**. When played, the front points the way you tell it to face (usually the player's yaw). Up is +Y.
4. Decide the **pivot**: the point the model turns and scales around and that sits exactly on the spawn position.
   - Default: bottom center `(8, 0, 8)`, which is right for things standing on the ground (crystals, pillars).
   - Weapons: put it at the grip (the hammer uses `(8, -10, 8)`).
   - Note it down; you'll give it to `.pivot(x, y, z)`.

## 2. Put the files in place

| What | Where |
|---|---|
| Model (File → Export → Export Block/Item Model) | `src/main/resources/assets/forgerealmmod/models/model_fx/<name>.json` |
| Texture PNG | `src/main/resources/assets/forgerealmmod/textures/model_fx/<name>.png` |

Open the JSON and check the `textures` part points at the PNG like this:

```json
"textures": {
  "0": "forgerealmmod:model_fx/<name>",
  "particle": "forgerealmmod:model_fx/<name>"
}
```

Blockbench usually writes this for you if the PNG is saved in that folder. Subfolders work too (`model_fx/swords/blade.json` → `"forgerealmmod:model_fx/swords/blade"`).

A model can use several textures (`"1"`, `"2"`, `"3"`...), as the Hand of Order sword does; each one just needs its PNG in `textures/model_fx/`. Animated textures work like on blocks: stack the frames vertically in the PNG and put a `<name>.png.mcmeta` next to it, e.g. `{"animation":{"frametime":3}}`.

Every model in `models/model_fx/` is loaded automatically when the game starts.

**Quick check in game** (shows it at your feet for 5 seconds):

```
/particle forgerealmmod:model{model:"forgerealmmod:model_fx/<name>",glow:1b,lifetime:100} ~ ~ ~ 0 0 0 0 1
```

A purple and black cube means the model or texture path is wrong.

You can try an animation the same way, without restarting the game. `keys` takes the same things as the code in section 4 (`from` + `tick` is a `.during`):

```
/particle forgerealmmod:model{model:"forgerealmmod:model_fx/<name>",glow:1b,lifetime:100,keys:[{tick:20,pitch:90f,ease:"in"},{tick:30,yaw:90f},{tick:100,forward:12f},{from:30,tick:100,spin:1440f}]} ~ ~2 ~ 0 0 0 0 1
```

## 3. Describe the effect in code

Make it a constant at the top of your class, like `ParticleFx` and `SlashFx`. Every method returns a new copy, so you can make variants freely.

```java
private static final ModelFx RUNE = ModelFx.of("rune")      // models/model_fx/rune.json
        .scale(1.5f)                  // 1 = Blockbench size (16 px = 1 block)
        .pivot(8, 8, 8)               // pixels, Blockbench coordinates
        .glow()                       // full bright, visible at night
        .aura(0xFF80C0FF, 0.1f, 3)    // glowing outline: ARGB color, thickness in blocks, softness
        .lifetime(60)                 // ticks (20 = 1 second)
        .fade(5, 10)                  // fade in 5 ticks, fade out the last 10
        .spin(6f);                    // turns around its own up axis the whole time, degrees per tick
```

### All options

| Option | What it does |
|---|---|
| `ModelFx.of("name")` | Model from `models/model_fx/name.json` |
| `.scale(s)` / `.scale(x, y, z)` | Size. Per-axis version for stretching (e.g. fit a mob's box) |
| `.pivot(x, y, z)` | Turn/scale point in Blockbench pixels (default `8, 0, 8`) |
| `.rotation(yaw, pitch, roll)` | Fixed starting orientation (added to what you pass when playing it) |
| `.color(argb)` | Tint and opacity. `0x80FFFFFF` = half see-through |
| `.glow()` | Full bright |
| `.seeThrough()` | For textures with transparent pixels (ice, glass, energy). Drawn last so things inside stay visible |
| `.aura(argb, blocks, layers)` | Glow around it. Alpha of the color = strength. 1 layer = hard edge, 3-4 = soft |
| `.lifetime(ticks)` | How long it stays |
| `.delay(ticks)` | Wait before appearing |
| `.fade(in, out)` | Fade in / fade out ticks |
| `.spin(degPerTick)` | Keeps turning around its own up axis the whole time |
| `.key(tick, pose)` / `.key(tick, pose, ease)` | Animation keyframe (see below) |
| `.during(from, to, pose)` / `.during(from, to, pose, ease)` | Extra animation that runs at the same time as the keyframes (see below) |
| `.noKeys()` | Remove all keyframes and `.during` animations |

## 4. Animate it with keyframes

A keyframe says: **by this tick, these things have these values.** Ticks count from when the model appears.

```java
ModelFx sword = SWORD
        .key(20, ModelFx.pose().pitch(90), ModelFx.Ease.IN)     // ticks 0-20:  tips forward
        .key(30, ModelFx.pose().yaw(90))                        // ticks 20-30: turns right, STILL tipped forward
        .key(60, ModelFx.pose().forward(10), ModelFx.Ease.IN);  // ticks 30-60: flies forward, still tipped + turned
```

The rules:

- **A key only changes what it names.** `pose().yaw(90)` touches the yaw and nothing else, so the pitch from the key before stays. Keys "stick".
- **A key starts moving when the key before it is reached** (the first one starts at tick 0), and uses its own ease.
- **Everything starts at rest**: no offset, no rotation, scale 1, alpha 1. Use `.key(0, ...)` to start from something else, e.g. `.key(0, ModelFx.pose().scale(0.3f).alpha(0f))` to start small and invisible.
- After the last key it holds what it has.
- `ModelFx.rest()` is a pose that puts **everything** back to the start in one go: `.key(80, ModelFx.rest())`.

What a pose can change:

```java
ModelFx.pose()            // changes nothing until you name something
    .forward(2f)          // blocks in front (the way the effect faces, however the model is turned)
    .up(1f)               // blocks up
    .right(0.5f)          // blocks to the right
    .pitch(90)            // + tips the top forward (a downward swing)
    .yaw(45)              // + turns right, around the world's up
    .roll(30)             // + tips the top to the right
    .spin(360)            // + turns around its OWN up axis (up in Blockbench), however it is tilted
    .scale(0.5f)          // multiplies .scale(...)
    .alpha(0f)            // 0 = invisible
```

All rotations happen around the pivot. Go past 360 to keep turning: `yaw(720)` is two full turns.

**How rotations combine.** They are always applied in the same order, whatever order you write them in: `spin` first, then `roll`, then `pitch`, then `yaw`. For a sword modelled standing up:

- `pitch(90)` tips it over so the blade points forward.
- adding `yaw(90)` swings that forward-pointing blade round to the right, so it ends up sideways-on.
- adding `spin(90)` instead gives it a quarter turn around its own length, so it still points forward.

### Several things at the same time

**Same timing:** put them in one key. They move together.

```java
.key(60, ModelFx.pose().forward(10).up(2).scale(2f))    // flies forward, rises and grows, all during the same ticks
```

**Their own timing:** `.during(fromTick, toTick, pose, ease)` runs next to the keyframes. Add as many as you want; they can overlap the keys and each other.

```java
ModelFx sword = SWORD
        .key(20, ModelFx.pose().pitch(90), ModelFx.Ease.IN)
        .key(30, ModelFx.pose().yaw(90))
        .key(100, ModelFx.pose().forward(12), ModelFx.Ease.IN)      // flies forward during ticks 30-100...
        .during(30, 100, ModelFx.pose().spin(1440))                 // ...and spins 4 full turns on the way
        .during(0, 10, ModelFx.pose().scale(1.5f), ModelFx.Ease.OUT_BACK);  // ...and pops bigger in the first 10 ticks
```

Which spin to animate:

| You want | Animate |
|---|---|
| Turn around its own length/up axis (a drill, a spinning top), whatever way it's tilted | `spin` |
| Flat spin around the world's up (a thrown blade, a rotor) | `yaw` |
| Tumble end over end | `pitch` |

If a `.during` and a key change the *same* thing during the same ticks, the one that started later takes over.

Example: the Hand of Order sword (ability 3). It rotates in two steps, keeps both rotations, then flies forward while it spins flat like a thrown blade.

```java
ModelFx sword = HAND_OF_ORDER_SWORD
        .key(20, ModelFx.pose().pitch(90), ModelFx.Ease.IN)       // ticks 0-20: tips forward
        .key(30, ModelFx.pose().yaw(90), ModelFx.Ease.IN)         // 20-30: turns sideways (still tipped)
        .key(40, ModelFx.pose().forward(5), ModelFx.Ease.IN)      // 30-40: lunges forward (still tipped + turned)
        .key(100, ModelFx.pose().forward(12), ModelFx.Ease.IN)    // 40-100: flies on
        .during(30, 100, ModelFx.pose().yaw(90 + 1440));          // ticks 30-100: 4 full turns on top of the 90

ParticleShapes.model(sl, sword, player.position().add(0, 3, 0), player.getYRot(), 0f, 0f);
```

| Tick | pitch | yaw | forward |
|---|---|---|---|
| 0 | 0 | 0 | 0 |
| 20 | 90 | 0 | 0 |
| 30 | 90 | 90 | 0 |
| 40 | 90 | about 296 (spinning) | 5 |
| 100 | 90 | 1530 | 12 |

The spin starts from 90 because that is where the tick-30 key left the yaw, so the target is `90 + 1440`. Swap the last line for `pose().spin(1440)` to roll it around its own blade, or `pose().pitch(90 + 1440)` to tumble it end over end.

**Easing** (how it gets to a keyframe from the previous one):

| Ease | Feels like |
|---|---|
| `LINEAR` | constant speed |
| `IN` | slow then fast: falling, slamming |
| `OUT` | fast then slow: landing softly |
| `IN_OUT` | slow, fast, slow: winding up |
| `OUT_BACK` | overshoots then settles: popping into existence |
| `OUT_BOUNCE` | bounces at the end |

> **Effects written before keys stuck:** a key used to be a complete pose, so anything it didn't name snapped back to rest. If an old effect now stays small, invisible or rotated, add the value it should return to in the next key.

Example: the Hand of Order hammer swing. It starts small and invisible, so the next key says what size and alpha to go to.

```java
ORDER_HAMMER
    .key(0,  ModelFx.pose().pitch(-20).scale(0.3f).alpha(0f))
    .key(6,  ModelFx.pose().pitch(-35).scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)   // appears behind you
    .key(13, ModelFx.pose().pitch(-55), ModelFx.Ease.IN_OUT)     // winds up
    .key(18, ModelFx.pose().pitch(90),  ModelFx.Ease.IN)         // slams down
    .key(20, ModelFx.pose().pitch(85),  ModelFx.Ease.OUT)        // small bounce
    .key(23, ModelFx.pose().pitch(90),  ModelFx.Ease.IN);
```

## 5. Play it

Works from server code (everyone nearby sees it). Call it inside your ability method:

```java
// at a spot, facing the player's direction
ParticleShapes.model(sl, RUNE, position, player.getYRot(), 0f, 0f);

// facing the way an entity faces
ParticleShapes.model(sl, RUNE, position, player);

// stuck to an entity: moves with it, disappears when it dies or unloads
// offset is from its feet, in world directions
ParticleShapes.modelOn(sl, RUNE, mob, new Vec3(0, mob.getBbHeight(), 0), 0f, 0f, 0f);

// remove everything stuck to an entity
ParticleShapes.clearModels(sl, mob);
```

> **Watch out:** methods never change the effect you call them on, they return a new one.
> `RUNE.key(...)` on its own line does nothing; keep the result:
> `ModelFx moving = RUNE.key(...).key(...);` and play `moving`.

Changing something per cast is just another copy:

```java
ParticleShapes.model(sl, RUNE.scale(3f).lifetime(100), position, yaw, 0f, 0f);
```

### Doing something when the animation reaches a point

The animation runs on the players' screens, so damage and sounds go on the server with `Later.run`, using the same tick as the keyframe:

```java
ParticleShapes.model(sl, hammer, grip, yaw, 0f, 0f);
Later.run(sl, 18, () -> hammerImpact(player, sl, impact));   // 18 = the slam keyframe
```

---

## Recipe: a new effect from start to finish

Say you want a spear that falls from the sky onto a target.

1. Blockbench: model a spear standing upright, tip at the bottom. Pivot at the tip, e.g. `(8, 0, 8)`.
2. Save `models/model_fx/sky_spear.json` and `textures/model_fx/sky_spear.png`.
3. Test with `/particle forgerealmmod:model{model:"forgerealmmod:model_fx/sky_spear",lifetime:100} ~ ~ ~ 0 0 0 0 1`.
4. In your class:

```java
private static final ModelFx SKY_SPEAR = ModelFx.of("sky_spear")
        .scale(2f).glow().aura(0xFFFF4040, 0.1f, 3)
        .lifetime(40).fade(0, 10)
        .key(0,  ModelFx.pose().up(12f).alpha(0f))             // starts 12 blocks up, invisible
        .key(4,  ModelFx.pose().alpha(1f))                     // fades in (still 12 up)
        .key(12, ModelFx.pose().up(0f), ModelFx.Ease.IN);      // hits the ground at tick 12

// in the ability:
ParticleShapes.model(sl, SKY_SPEAR, target.position(), player.getYRot(), 0f, 0f);
Later.run(sl, 12, () -> target.hurt(player.damageSources().playerAttack(player), 20f));
```

---

## Freezing (bonus)

The ice look from Hand of Order is its own reusable status:

```java
Freeze.apply(target, 80);     // frozen 4 seconds: no AI for mobs, rooted for players
Freeze.isFrozen(target);
Freeze.ticksLeft(target);
Freeze.thaw(target);          // break the ice early (with the shatter effect)
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Purple/black cube | Model file name or its `textures` path is wrong, or the PNG isn't in `textures/model_fx/` |
| Model doesn't load at all | A cube is outside -16..32 px, or a rotation isn't -45/-22.5/0/22.5/45. Check the game log |
| Keyframes do nothing / it doesn't move | You called `.key(...)` but played the original. Save the result in a variable and play that |
| It never comes back from a key (stays small / invisible / rotated) | Keys stick. Name the value to return to in a later key (`.scale(1f).alpha(1f)`), or use `ModelFx.rest()` |
| Spin goes around the wrong axis | `spin` = its own up axis, `yaw` = the world's up, `pitch` = end over end. See the table in section 4 |
| Aura invisible | The color needs an alpha: `0xFFFF0000`, not `0xFF0000` |
| Points the wrong way | The front must be North (-Z) in Blockbench, or add `.rotation(180, 0, 0)` |
| Swings around the wrong spot | Set `.pivot(...)` to the right pixel coordinates |
| Things inside it disappear | Add `.seeThrough()` |
| Aura too bright / dim | Lower / raise the alpha of the aura color (`0x80...` is half strength) |
| Not visible from far away | Effects are sent to players within 32 blocks, same as other particles |

The code lives in `src/main/java/net/lukario/frogerealm/particles/fx/` (`ModelFx`, `ModelFxRenderer`, `ModelFxParticle`). You only need to touch `ModelFx` to add new options.

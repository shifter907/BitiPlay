# BitiPlay World

An open-ended 2D side-scrolling play world for Android, in the spirit of Sago Mini World.
Eight environments, six playable characters, and no scores, timers, or fail states.

Everything is drawn procedurally with the Android `Canvas` API — there are no image or
audio assets, so the whole app is ~1 MB and looks sharp at any density.

## Running it

```bash
gradlew.bat :app:installDebug
```

The APK also lands at `app/build/outputs/apk/debug/app-debug.apk`.

Requires an Android 7.0 (API 24) device or newer. The app is landscape-only.

## Controls

| Gesture | What happens |
| --- | --- |
| **Drag left/right** | Scrolls the environment. Flick for momentum. |
| **Tap the ground** | The active character walks there (or drives, if riding). |
| **Tap an object** | The character walks over and uses it — picks it up, feeds it, mounts it, waters it. |
| **Tap another character** | Switches control to them. Tap the one you control to make them cheer. |
| **Character tray** (bottom-left) | Pick any of the six friends. |
| **Action button** (bottom-right) | Context-sensitive: throw what you're holding, hop off a vehicle, let go of a cart, or jump. |
| **Map button** (top-left) | Opens the world picker. |
| **Speaker** (top-right) | Mutes the synthesised sound effects. |
| **Deck buttons** (right edge) | Cruise ship only — jump between decks. |

## The worlds

Seven of the eight loop seamlessly: keep scrolling in either direction and the right edge
becomes the left edge, so there is no wall to hit. The cruise ship is the exception — it has
a bow and a stern, and three stacked decks instead.

| World | Highlights |
| --- | --- |
| **Neighborhood** | Houses, a garden to water, dig spots (one hides buried treasure), wheelbarrow, wagon, tent, grill, apple tree, car, bike, trike, dog, cat, bunny |
| **Beach** | Surf, palms, a sandcastle you build in three stages, buried shells and treasure, rock pools, boat, ice-cream stand, crabs, seagulls, turtle |
| **Farm** | Barn, silo, turning windmill, eight crop beds, well, chicken coop, tractor, hay bales, and cows, pigs, sheep, horse, goat, chickens, duck, goose |
| **Amusement Park** | Ferris wheel and carousel you can spin, a running roller coaster, swing ride, go-karts, a park train, balloon/popcorn/ticket stalls |
| **Cruise Ship** | Three decks — sun deck with pool and grill, cabin corridor with doors that open, and the hold with a running engine. Stairs or the deck buttons move you between them. |
| **Downtown** | Animated skyline, taxis, traffic lights on a cycle, subway entrance, hot-dog cart, bus stop, a fire hydrant that sprays, pigeons |
| **Zoo** | Six enclosures — elephant, giraffe, monkeys, penguins with a pond, lions, zebras — plus species-specific feed dispensers and a feed cart |
| **Grocery Store** | Ten shelf aisles that hand out real items, produce island, freezer, working checkout, and shopping carts to push |

## Things to try

- Carry the watering can to a seedling and water it four times; when it's ripe, pick the fruit.
- Take a shovel to a dig spot — some of them have something buried.
- Put a raw patty or a corn cob on a grill, wait for the sizzle, then take it off cooked.
- Load a wheelbarrow or shopping cart with several items and push it across the world.
- Pitch the tent in three taps, then tap it again to duck inside.
- Feed anything edible to any animal, or just tap one to pet it.

## Code layout

```
app/src/main/java/com/bitiplay/world/
  MainActivity.kt      fullscreen, immersive, landscape host
  GameView.kt          SurfaceView + render thread; input is taken under the frame lock
  Game.kt              camera, input routing, scene switching, save state
  core/                math helpers, camera (wrapping + multi-deck projection)
  art/                 palette and the allocation-free Canvas drawing primitives
  engine/              Entity, Scene, Render (culling, sorting, parallax, picking)
  ent/                 Character, Item, Vehicle, Pushable, Animal, Plant, DigSpot,
                       Tent, Grill, Dispenser, Stairs, Prop
  scenes/              the eight environments plus shared scenery art
  fx/                  particle pool
  audio/               procedural sound synthesis (no audio files)
  ui/                  HUD, character tray, world picker
```

### How the wrap works

World x loops modulo the scene width. Nothing ever compares absolute positions —
`Scene.delta` returns the shortest signed distance around the loop, and the renderer places
entities from that delta. The camera x is deliberately *not* wrapped to one scene width but
to four, which keeps parallax layers continuous across the seam (a layer at parallax `f`
would otherwise jump by `width * f` every time the camera crossed the origin).

### How the decks work

Decks are authored in identical local coordinates and separated only at render time, by
`Cam.levelF * levelHeight`. Because `levelF` is fractional, changing decks animates as a
vertical slide for free, and one deck is always exactly one screen away from the next.

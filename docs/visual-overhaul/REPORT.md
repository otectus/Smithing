# Visual overhaul validation

Work started from clean `main`, commit `3840ff0`, origin `https://github.com/otectus/Smithing.git`. Minecraft 1.20.1 / Forge 47.4.23 / Java 17. The baseline client ran before source edits; its 13 screenshots and result are in [before](before/). See the [before/after gallery](GALLERY.md).

## Delivered implementation

- Four custom baked stations with matching cardinal rotations and picking/collision surfaces. Worked iron, soot masonry, worn timber and bronze fasteners share deliberate 16px material ramps. All 242 authored model faces use explicit 1:1 UVs.
- All four hammer/tongs tiers, loaded workpiece overlays, hot stock, guide item and block inventory displays have finished artwork. Heated item layers work in ordinary Forge rendering.
- Volumetric billet/coal piles, heated stock and shaped equipment, molten slag animation, and water levels use synchronized station data. Anvil stock has category silhouettes; shaped equipment uses its original model, with a generic fallback for custom/missing item renderers.
- Directional strike/grinding sparks, ignition embers and quench splashes/steam are client-configurable and action driven. First-person and third-person tongs/hammer poses follow vanilla swing synchronization. Decorative effects are bounded (96 action particles per tick, at most six metal/four fuel meshes per forge, distance detail reduction and normal frustum culling).
- Responsive iron-framed recipe/minigame screens, parchment guide, matching JEI presentation and clearer quality tooltips. Long names, costs, recipe components, keyboard selection, scrollbars, hover/focus and next steps have explicit presentation. Anvil targets remain in the same coordinate space as input while only the decorative underlay shakes.
- Server timing/scoring, recipes, balance, registry IDs, packet formats and persistent data formats are unchanged. New block shapes and client render bounds are intentional presentation/interaction changes.
- [Editable artwork and export instructions](../../art/README.md). The former placeholder generator is retired: its default operation verifies exports without writing; `--write` explicitly exports finished source art. Models and textures remain resource-pack replaceable and resolve again after reload.

Production artifact: [`otes-smithing-1.1.0.jar`](../../build/libs/otes-smithing-1.1.0.jar). SHA-256: `e4fef44f461e95acfcb808b7506ba9cd87ea8374e2eb20f61eb07bafb0b9ed91`.

## Checks completed

Commands are run from the repository root (Gradle uses `/home/otectus/Projects/.mcmod-tools/gradlew-quiet.sh` to retain logs).

| Check | Result |
| --- | --- |
| Baseline `./gradlew runClient -PsmokeTest` | PASS, 43s; 13 before screenshots |
| `python3 tools/generate_textures.py --check` | PASS; 30 textures, 242 UV faces and five emissive item-layer models |
| `python3 /home/otectus/Projects/.mcmod-tools/check_mod.py .` | 0 errors, 0 warnings; 13 informational notes from dynamic tier registrations/parent models |
| `./gradlew compileJava compileGameTestJava` | PASS |
| `./gradlew runGameTestServer` | All 23 tests passed; [log](validation/gametests-base.txt) |
| `./gradlew runGameTestServer -PcompatMods` | All 23 tests passed with Spartan Weaponry, Spartan Shields and Immersive Armors; [log](validation/gametests-compat.txt), [recipe report](validation/compat-report.txt) |
| Initial expanded screenshot smoke | PASS, 78s; resource reload, JEI, station galleries, GUI scales and frame sampling; [result](validation/initial-smoke.json) |
| `./gradlew build packTestJar` | PASS, 11s after emission correction; [build log](validation/build.txt); packaged production jar excludes the test harness |
| Final desktop `./gradlew runClient -PsmokeTest` | PASS, 84s; [result](validation/final-desktop-smoke.json), [log](validation/final-desktop-smoke.txt), [39 screenshots](after/desktop/) |
| Exact-minimum screenshot smoke, Gamescope SDL at 960×720 | Game/agent/Gradle PASS; required 320×240 logical viewport asserted; [result](validation/exact-minimum.json), [screenshots](after/). Compositor wrapper exited 139 during teardown after the child completed successfully. |
| `python3 tools/packtest/ultima_pack_test.py --expect spartan,shields,upgrades --label visual-overhaul-emission-final` | PASS, 233s, corrected final jar; 422 loaded mods, 27 material families, 174 explicit + 181 automatic recipes. No mod-owned error/stack trace or crash report. [Summary](validation/ultima-summary.json), [result](validation/ultima-result.json), [client log](validation/ultima-client.txt), [screenshots](after/ultima/) |
| Two-client dedicated-server animation test (command below) | PASS; synchronized loaded-tongs and hammer swings, six first-/third-person screenshots, both clients and server exited cleanly; [result](validation/multiplayer-summary.json) |

An independent source review covered models, shapes, client/server boundaries, reload behavior, effect budgets and animation hooks; no consequential defect was found. Runtime inspection identified and corrected cold stock inheriting a molten tint and missing `forge_data.layers` nesting on five emissive item models, and prompted further small-layout/tooltip refinements. The Forge 47.4.23 loader source confirmed the required emission nesting; the asset checker now enforces it. The gallery harness also refreshes the heating station immediately before each capture so it has not already melted.

The desktop window manager clamped the requested GUI scale/window size: its final logical viewport was 472×573, not 320×240. Its north gallery also caught the resource-reload fade. Those desktop captures are retained as intermediate evidence; final gallery images come from the isolated display pass. The harness now waits for the overlay to disappear, positions the camera before effects, opens the survival inventory for all tool tiers, and samples frame timing for at least three seconds. Earlier short performance samples are not used as final measurements.

The exact-size command was `gamescope --backend sdl -W 960 -H 720 -w 960 -h 720 --force-windows-fullscreen -- env JAVA_TOOL_OPTIONS=-Dotes_smithing.packtest.requireExactMinimum=true ./gradlew runClient -PsmokeTest --no-daemon`. Its raw `inventory_quality_tooltip_hovered` value records a cursor-warp attempt; the compositor kept the cursor over the tongs. The final packaged Ultima run uses a deterministic inventory render cursor and separately captures all tool tiers and the quality tooltip. Its custom fonts, textures and interface decorations belong to the pack.

## Performance

The exact-size run compared 32 empty forges with 32 fueled, loaded, ignited forges at the same fixed spectator camera and render distance 6. Each phase discarded 60 warmup frames and sampled at least 180 frames and three seconds. VSync was off; vanilla's maximum setting (260) means unlimited rendering. Hardware: NVIDIA RTX 4050 Laptop GPU, OpenGL 4.6, driver 615.71.09, 960×720, no shader pack.

| Phase | Frames / sample time | Mean frame interval | 95th percentile interval | Mean client render tick |
| --- | --- | --- | --- | --- |
| 32 empty | 3,016 / 3.014s | 1.00ms | 5.87ms | 0.21ms |
| 32 active | 1,758 / 3.033s | 1.73ms | 9.16ms | 0.47ms |

These are CPU render-event intervals and durations in a small test world, not GPU timestamps or guaranteed displayed FPS. The nested compositor was slow while VSync was enabled earlier in the sequence. The matched uncapped samples show a bounded additional rendering cost on this machine; they do not establish performance on low-end hardware or in a large survival world.

The isolated Ultima run also completed the 32-forge comparison at 1900×1025. Mean client render ticks were 2.05ms empty / 1.48ms active, with 1,070 / 1,592 sampled frames. The reversal is within run/order/environment variability; it is not evidence that active stations improve performance. No timing guarantee is inferred from these short smoke measurements.

## Multiplayer and coverage limits

The dedicated-server test uses two actual Forge clients, synchronized loaded tongs and hammer swings, and separate first-person and observer screenshots. Its matching server libraries are read from an existing Forge 47.4.23 installation; each run creates its own world, configuration and localhost port. Final evidence is in [the multiplayer result](validation/multiplayer-summary.json), [server log](validation/multiplayer-server.txt), [leader log](validation/multiplayer-leader.txt), [observer log](validation/multiplayer-peer.txt), and [screenshots](after/multiplayer/).

Final command: `python3 tools/visual-test/multiplayer_visual_test.py --work-dir /tmp/otes-smithing-multiplayer-final-11 --server-libraries /home/otectus/Projects/runic-skills/build/instance-validation-server/libraries --fullscreen-clients --timeout 180`. The harness waits for the held-item equip transition and consumes the observer's acknowledgement before disconnect cleanup. Its final `./gradlew packTestJar --offline --no-daemon` build [passed in 11s](validation/multiplayer-harness-build.txt); offline mode avoided a stalled Forge network preflight. These last changes affect only the test harness; the production artifact matches the final Ultima run.

The pose fixture uses vanilla night vision because its newly staged client location reported zero block/sky light. Ordinary station lighting was checked separately in day/night galleries. A [zero-light frame](after/multiplayer/packtest_hot_tongs_zero_light.png) also confirms the corrected emissive tongs layer.

The final Ultima desktop observer reported `UNOBSERVED` because the game window was not visible on the active workspace for sufficient external captures. The game completed its full sequence and saved 40 internal framebuffer screenshots, all data/runtime assertions passed, and no mod-owned error or crash was reported. Its earlier pass had `freeze=OK`.

Not covered: off-hand/left-handed animation runs, non-English translations, a screenshot of the Shift-expanded quality tooltip, long-latency human minigame sessions, or every third-party custom item renderer. English, synthetic long names and Ultima's custom font were inspected. Spoken narrator output could not be checked because native `flite` is unavailable; this also affected the baseline. No save migration was required, and no release or remote push was performed.

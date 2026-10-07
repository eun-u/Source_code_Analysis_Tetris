# Campus RPG bitmap assets

Generated with the built-in image_gen tool for the Korean university Tetris RPG. These are original project-bound concept/game assets. No text, HUD, characters, or embedded game boards appear in the backgrounds.

| File | Size | Use |
| --- | --- | --- |
| concrete-tile.png | 1254 × 1254, transparent outside tile | Ordinary aged reinforced-concrete block; warm grey with a faint dusty-coral edge tint. Add restrained per-piece color in code. |
| ore-tile.png | transparent outside tile | Original voxel-style gold ore embedded in gray concrete, with multiple high-contrast gold clusters readable at 18–30 pixels. |
| university-bg.png | wide landscape | Korean concrete lecture hall courtyard in muted late-afternoon light; broad clear foreground for combat UI. |
| university-battle-bg.png | wide landscape | Warm late-afternoon Korean campus and visibly tiled courtyard used behind and within the PvE university combat frame. |
| graduation-bg.png | 1672 × 941 | Korean university concrete library/research annex at rainy night. |
| employment-bg.png | 1672 × 941 | Korean office district and pedestrian plaza before dawn. |
| poses/player-idle.png | transparent | Full-body Korean student fighter facing right, ready stance with backpack and notes. |
| poses/player-windup.png | transparent | Matching student crouching to prepare a melee strike. |
| poses/player-strike.png | transparent | Matching student lunging forward with a direct attack. |
| poses/player-hurt.png | transparent | Matching student recoiling from a hit. |
| poses/professor-strike.png | transparent | Full-body professor striking in the same combat style. |
| poses/professor-hurt.png | transparent | Matching professor recoiling from a hit. |

Prompt set: isolated top-down aged Korean-campus concrete square tile with sparse aggregate and no windows or text; edit the same tile to add readable jagged golden cracks for the item; wide low-detail-center backgrounds showing a late-20th-century concrete Korean campus, a night library annex, and a dawn Korean office district respectively. All prompts excluded Western Gothic architecture, HUD, grids, characters, logos, labels, and text.

An earlier `ore-tile.png` edit enlarged the ivory-gold mineral core and simplified its four radial cracks for use at actual Tetris cell sizes; the ordinary concrete tile was retained.

The latest `ore-tile.png` edit uses the prior project tile as its source and replaces the magic-gem crack with original chunky, pixel-stepped gold deposits in dark gray stone. It evokes a classic voxel mining block without copying another game's texture. `ConcreteBlockSkin` leaves those deposits unobscured at playable cell sizes and draws a simplified gold cluster only for smaller previews or missing images.

The latest `university-bg.png` edit uses the previous campus background as its image_gen source. It preserves the Korean lecture building, distant hillside and open paved combat space while moving the lighting from very dark evening to muted overcast late afternoon. The campus backdrop remains visible in the character arena and is subdued behind general menus.

`university-battle-bg.png` was edited with the built-in image_gen tool from `university-bg.png`, using the approved battle mockup only as a mood/composition reference. The prompt asked for warm peach-gold late-afternoon light, illuminated Korean concrete campus windows and an unobstructed perspective-tiled courtyard. It explicitly excluded the mockup's HUD, characters, borders, text and blocks. This separate battle asset leaves the lobby and story backgrounds unchanged.

Combat pose prompt set: transparent full-body pixel-art Korean university student in a dark-purple hoodie, dark trousers and backpack, facing right with consistent proportions across idle, windup, lunge, and hurt frames; transparent full-body Korean professor for strike and hurt frames. Generated with image_gen for this game; no third-party character art was copied. Existing non-professor enemies retain their earlier static sprites and use code-driven movement until dedicated pose sheets are made.

At runtime these full-resolution images should be sampled to the target cell and panel dimensions. The block images are source art, not pre-baked tetromino shapes.

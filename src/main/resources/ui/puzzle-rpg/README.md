# 제작 이미지

2026-10-07 · built-in image_gen · 원본 아틀라스를 수정하지 않고 GameArt에서 전투별 영역을 선택합니다.

파일: university.png, graduation.png, employment.png과 독립 컷아웃 alcohol.png, course.png, professor.png, retake.png, gpt.png, capstone.png. 전투 9개에 사용하며 게임 JAR에 포함됩니다.

## 최종 프롬프트

### university

Use case: stylized-concept. Asset type: ONE production sprite atlas for a Korean university themed puzzle RPG, transparent PNG, three full-body actors arranged in three strictly equal vertical columns on a wide landscape 1536x1024 canvas. Each actor must stay entirely inside its own column with plenty of transparent margin, aligned at same baseline. Left column: cute animated beer mug monster, golden liquid, white foam hair, stubby arms and feet, mischievous eyes, bottlecap shield. Center column: living liberal-arts textbook monster, lavender cover, pages forming arms, round glasses, mint bookmarks, grumpy face. Right column: elderly university professor boss, original fictional human, silver hair, glasses, tweed purple jacket, stern but humorous, holding pointer and stack of exam papers. Style: cohesive crisp chunky pixel art, dark violet stepped outlines, mint lavender gold palette, SNES RPG enemies, readable at 120px tall. All three face slightly left toward the player. No overlaps between columns, no frames, labels, text, UI, ground, checkerboard, blur, watermarks, copyrighted figures. Background must be genuinely transparent alpha. Only these three actors.

### graduation

Use case: stylized-concept. Asset type: ONE 2D pixel RPG sprite atlas, 3 full-body enemy actors in three strictly equal nonoverlapping columns on wide1536x1024 canvas, aligned baseline, 10percent padding inside each column. Left: angry red letter F exam paper monster, paperlimb hands, crumpled angry face, tiny graduationcap, represents retaking a failed class. Center: original friendly but adversarial mint computer chatbot robot, CRT monitor head showing simple geometric eyes and smile, purple hoodie, keyboard arms, represents GPT without logos. Right: final capstone thesis boss, enormous purple bound dissertation book creature with academic mortarboard and gold tassel, layeredpage wings and rolled diploma scepter, solemn eyes. Crisp chunky pixel art, dark violet outlines,mintlavendergoldcoralpalette,SNES university RPG, readable120pxhigh, allfaceleft. Background must be actual transparent alpha acrossallblankspace, no glow backdrop, ground, checkerboard or scenery. No typography otherthan simple F shape onleft paper, no UI, watermarks, copyrighted figures, smoothing. Eachsprite fully separatedinsidecolumn.

### employment

Use case: stylized-concept. Asset type: ONE 2D pixel RPG sprite atlas, 3 full-body enemy actors in three strictly equal nonoverlapping columns on wide1536x1024 canvas, alignedbaseline,10percentpaddinginsideeachcolumn. Left: adversarial codingtest terminal monster, darkpurple CRT computermonitor with mint pixel bracket eyes, keyboardfeet,cablearms, smallgeometriccodeicons withoutreadabletext. Center: stern fictional jobinterviewer, officebusinesssuit lavenderjacket,golden tie, spectacles, clipboardinhand,mintnametag, chibifullbodyhuman, confrontingplayer. Right: companyheadquartersboss, anthropomorphic toweringpurpleofficebuilding withgoldroof, mintwindowsformingeyes, blockarmsandfeet, gold staff IDbadgehangingfromneck,finalemploymentsymbol. Crispchunkypixelart,darkvioletoutlines,mintlavendergoldpalette,SNESuniversityRPG,readable120pxhigh,allfaceslightlyleft. Background mustbeactualtransparentalphaacrossallblankspace,noglowbackdrop,ground,checkerboardorscenery. No readabletext,UI,watermarks,copyrightedfigures,smoothing. Eachspritefullyseparatedinsidecolumn.


## 캐릭터 경계 수정

독립 컷아웃 6개도 built-in image_gen의 편집 기능으로 제작했습니다. 원본 아틀라스의 이웃 캐릭터가 표시되지 않도록 개별 PNG를 우선 사용합니다.


### course

Keep only the CENTER living textbook character from this supplied university RPG atlas, preserving its exact lavender book design, face, glasses, bookmark, paper arms, feet and chunky pixel style. Completely remove the beer mug, professor, pointer, background glow and all fragments from those other characters. Reconstruct any edge of the textbook hidden by another character if necessary. Center the full-body book monster on a square transparent canvas, generous transparent margin, no crop. Actual transparent alpha in ALL blank space. No added objects, shadows, ground, text, labels, frames or watermark.

### professor

Keep only the RIGHT university professor character from this supplied university RPG atlas, preserving his exact silver hair, glasses, moustache, purple tweed jacket, pointer, exam papers, full body and chunky pixel style. Completely remove the book monster, beer mug, background glow and every fragment of the other characters. Center the full-body professor on a square transparent canvas, generous clear transparent margin including the entire pointer. Actual transparent alpha in ALL blank space. No added objects, shadows, ground, labels, frames or watermark.

### capstone

Keep only the RIGHT capstone thesis book boss from this supplied university RPG atlas, preserving its exact purple dissertation cover, gold tassel, graduation cap, rolled diploma staff, page wings, eyes, feet, and chunky pixel style. Completely remove the red F paper monster, chatbot robot, all keyboard or arm fragments from those other characters, and background glow. Center the full-body capstone book boss on a square transparent canvas, generous clear margin including entire diploma staff. Actual transparent alpha in ALL blank space. No new text, shadows, ground, labels, frames or watermark.

### alcohol

Extract ONLY the LEFT beer mug monster from this supplied university puzzle RPG atlas. Preserve the exact golden beer, foam hair, purple cap shield, stubby hands and feet, original face and crisp pixel-art identity. Completely remove ALL other characters, their fragments, background glow, shadows, ground and labels. Center the complete full-body actor on a square canvas with generous margins. Actual transparent alpha in every blank area, no background gradients or ground. Only one actor, no cropping, no watermark.

### retake

Extract ONLY the LEFT red F exam paper monster from this supplied university puzzle RPG atlas. Preserve the exact red F paper, angry face, graduation cap, arms and feet, original face and crisp pixel-art identity. Completely remove ALL other characters, their fragments, background glow, shadows, ground and labels. Center the complete full-body actor on a square canvas with generous margins. Actual transparent alpha in every blank area, no background gradients or ground. Only one actor, no cropping, no watermark.

### gpt

Extract ONLY the CENTER mint chatbot robot from this supplied university puzzle RPG atlas. Preserve the exact mint CRT head, geometric eyes, purple hoodie, keyboard hands and feet, original face and crisp pixel-art identity. Completely remove ALL other characters, their fragments, background glow, shadows, ground and labels. Center the complete full-body actor on a square canvas with generous margins. Actual transparent alpha in every blank area, no background gradients or ground. Only one actor, no cropping, no watermark.

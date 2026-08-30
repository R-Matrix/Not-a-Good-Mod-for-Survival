# Third-party notices

## TweakerMore

Some configuration UI code in this project contains adaptations of the
optional-rule presentation and Malilib config-label hook from Fallen_Breath's
TweakerMore project:

- https://github.com/Fallen-Breath/TweakerMore
- Copyright (C) 2023 Fallen_Breath and contributors
- License: GNU Lesser General Public License v3.0 (LGPL-3.0-only)
- License text: `LICENSES/LGPL-3.0.txt` (also available at https://www.gnu.org/licenses/lgpl-3.0.html)

The adapted portions retain the original attribution and LGPL-3.0-only
licensing. Other project code remains covered by the license in `LICENSE`.

## Litematica

The survival-mode inventory-to-main-hand swap for projected item frames delegates to
Litematica's public pick-block supply API (`InventoryUtils.setPickedItemToHand` and
`findSlotWithBoxWithItem`), so the target hotbar slot obeys Litematica's
`PICK_BLOCK_AVOID_DAMAGEABLE` and `PICK_BLOCK_AVOID_TOOLS` rules:

- https://github.com/maruohon/litematica
- License: LGPL-3.0-only

Only the approach is referenced; `ProjectionSurvivalSupply` is written against the
1.21.4 API and does not copy Litematica's code.

## QuickCraft

The projected item frame preview slot colours, alpha steps and the translucent
ghost item idea follow the design of yiyihehe's QuickCraft container verifier:

- https://github.com/yiyihehe/quickcraft
- License: MIT

Only the idea and the colour values are referenced; the implementation in
`FramePreviewPalette` is written against the 1.21.4 API and does not copy
QuickCraft's code. The translucent ghost item is drawn through vanilla's own
GUI pipeline by flushing the buffered geometry while the global shader colour
carries the ghost alpha.

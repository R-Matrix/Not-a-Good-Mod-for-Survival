# TODO

## 高优先级

- [x] ? 饥饿时也可强制疾跑
- [x] 如同基岩版在前方放置方块
- [x] 投影模式十部分渲染
- [x] 与 xaero world map 联动, 显示对应区块的 map id(如有) (可选旗帜配置)
- [x] 告示牌启用长文本编辑
- [x] 告示牌只有一个字符时, 放大字号
- [x] 物品栏中的玩家模型调试时碰撞箱不渲染
- [x] 全局 Malilib 配置搜索
- [x] mod menu 支持
- [x] 投影轻松放置讲台书和显示(放置)展示框(及其内容物)

## 投影辅助第二轮收敛

- [x] 预览界面：单格原版箱子风格 + 保留下方文字建议
- [x] 预览槽位改为 QuickCraft 幽灵显示（缺=蓝幽灵、错=红、满足=原样）
- [x] 轻松放置门控改为“照搬方块语义 + 实体渲染开关”，不再静态复刻渲染判据，不改渲染逻辑
- [x] 一次点击连发“放入 + 全部旋转”到位；去掉空手腾挪；自动打印未开时放空框给一次性提示
- [x] 第三轮修正：预览只保留单个格子（不再用 9 格行贴图）、拾取/朝向基于实体位置（修复旋转/偏移投影的异常偏移）、红蓝按“实际放入 vs 投影预期”判定、渲染完全按 QuickCraft 分层、精简提示文字
- [x] 第四/五轮修正：单格面板改为 malilib SINGLE_ITEM 同款 32×32 迷你面板；幽灵渲染走原版 GUI 管道（ColorModulator）不再用 FBO；恢复坐标/旋转/读书三行文字
- [x] 第六轮修正：GUI 深度分层（物品 z=150 前用 z=0、后用 z=300）修复幽灵物品不可见；handler 槽位库存改回预期物品恢复悬停 tooltip
- [x] 第七轮修正：悬停 tooltip 动态跟随槽位显示内容（空框=预期物品，错填=实际物品），行为与箱子槽位一致
- [x] 第八轮修正：补齐 `render`→`drawMouseoverTooltip`（1.21.4 HandledScreen 不自绘槽位 tooltip），悬停真正显示箱子式信息
- [x] 创造模式展示框供给与中键拾取：`enableCreativeFrameSupply`（默认关、仅创造）自动供给框+带组件内容物，中键按框内状态拾取
- [x] 创造中键不吞 + GUI 门控：`tryPick` 在 GUI 打开时不拦截中键（背包中键快速移动正常）；创造拾取对齐原版 `onPickItem`（背包已有→**优先放进空快捷栏槽并选中**（原快捷栏物品不动），主手为空才进主手，快捷栏满才换选中槽；没有→先腾挪旧物再放入，背包满才覆盖），放置/填充路径同样不吞物品
- [x] 生存模式背包换手：`enableSurvivalFrameSupply`（默认关、仅生存）按投影放置/填充展示框或中键拾取时，从背包任意位置把物品换到主手（参照 Litematica `swapItemToMainHand` 的 `clickSlot` SWAP 思路）
- [x] 生存换手交给 Litematica：`setPickedItemToHand` 处理目标槽选择（含 `PICK_BLOCK_AVOID_DAMAGEABLE`/`PICK_BLOCK_AVOID_TOOLS` 避开），`PICK_BLOCK_SHULKERS` 开时从潜影盒取内容物
- [x] 投影展示框内容物参与容器高亮：`enableProjectionContentHighlight`（默认关）打开容器时用 Litematica `MaterialListHudRenderer.highlightSlotsWithItem` 高亮内容物槽位

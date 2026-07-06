# Java HTML/CSS 界面库核心开发思路

## 目标定位

建设一个完全脱离浏览器内核的 Java UI 渲染库：业务侧使用 HTML + CSS 描述界面，库内部完成 DOM/CSSOM/Style/Layout/Paint 管线，最终通过 JavaFX Canvas 或未来 Swing/AWT Graphics2D 后端绘制。

初期建议以 JavaFX 为默认技术底座，原因是：

- Canvas 绘制 API 稳定，适合自研渲染管线。
- JavaFX 对字体、图片、事件、窗口、多 DPI 的基础支持比 Swing 更现代。
- 后续可保留 `RendererBackend` 抽象，再扩展 Swing/AWT 或离屏渲染。

## 总体渲染管线

```text
HTML String
  -> HtmlParser
  -> DOM Tree
  -> CssParser
  -> CSSOM / Stylesheet
  -> StyleResolver
  -> Styled DOM
  -> LayoutEngine
  -> Layout Tree
  -> JavaFxRenderer
  -> Canvas Pixels
```

## 分层模块拆解

### 1. DOM 层

职责：

- 表示文档节点、元素节点、文本节点。
- 保存父子关系、标签名、属性、文本内容。
- 提供 DOM 查询和遍历能力。

核心类：

- `DomNode`
- `DocumentNode`
- `ElementNode`
- `TextNode`

演进方向：

- 补充 namespace、HTML5 语义容错、实体解码。
- 支持节点增删改和增量渲染脏标记。

### 2. HTML Parser 层

职责：

- 将 HTML 字符串解析为 DOM Tree。
- 支持标签、文本、属性、void tag。
- 对非法嵌套做容错恢复。

初版采用自研轻量状态机；生产级阶段可以保留接口并替换为更严格的 tokenizer + tree builder。

### 3. CSSOM 层

职责：

- 解析样式表为规则列表。
- 表示选择器、声明、优先级和规则顺序。
- 支持内联 style 解析。

初版选择器：

- `div`
- `.card`
- `#app`
- `div.card`
- `#app .card title`

演进方向：

- 子代、兄弟、属性选择器。
- 伪类和伪元素。
- media query、变量、自定义属性。

### 4. Style Resolver 层

职责：

- 按 UA 默认样式、外部 CSS、内联 style 计算最终样式。
- 完成继承属性传播。
- 处理选择器 specificity 和 source order。

关键规则：

- 内联样式优先级最高。
- `id > class > tag`。
- 同优先级后声明覆盖前声明。
- `color/font-size/font-family/font-weight` 默认继承。

### 5. Layout Tree 层

职责：

- 将 Styled DOM 转换为布局树。
- 根据 display、盒模型和可用宽度计算坐标与尺寸。
- 输出只服务绘制的结构，避免绘制层回看 DOM。

当前布局能力：

- 块级流式布局。
- `margin/padding/border`。
- 固定 `px` 和基础百分比宽度。
- 文本换行和多行文本盒。
- 图片盒尺寸计算。
- 简易 Flex row/column、gap、基础 `justify-content`。
- 简易 Grid columns、gap。

演进方向：

- inline formatting context。
- 更精确的字体测量和 baseline。
- min/max、position、overflow。
- Grid track sizing、alignment。

### 6. Paint 层

职责：

- 将 Layout Tree 绘制到 JavaFX Canvas。
- 绘制背景、边框、文本、图片、渐变、阴影。
- 层叠顺序按布局树 DFS。

演进方向：

- 资源加载策略、缓存失效和失败占位。
- 透明度、clip。
- 文本 shaping、字体 fallback、高 DPI。
- repaint dirty region。

### 7. Event/Input 层

职责：

- 将 JavaFX 鼠标/键盘事件命中测试到 LayoutBox。
- 触发 hover、active、focus 状态变化。
- 支持表单控件、滚动、选择。

当前已提供 `HitTester`，可以根据坐标命中 Layout Tree 中最深的盒子。`MikoRenderView` 已绑定 JavaFX 鼠标事件，支持 `:hover`、`:active`、`:focus` 参与样式重算和重绘，并提供 click 事件冒泡分发 API。

### 8. Widget/Runtime 层

职责：

- 对外提供 `MikoRenderView` 组件。
- 支持加载 HTML/CSS、重新布局、重绘。
- 管理资源加载、缓存、调试面板。

## 8 个 Agent 工作流分工

当前运行环境未暴露可调用的子代理 API，因此本仓库按 8 条职责流组织首版交付：

1. 架构 Agent：定义分层边界、接口方向、长期路线。
2. HTML Agent：实现 DOM 模型、HTML 解析、raw text。
3. CSS Agent：实现 CSSOM、声明解析、选择器解析、`!important`。
4. Cascade Agent：实现 specificity、source order、继承、内联样式、重要声明优先级。
5. Layout Agent：实现盒模型、块布局、文本换行、简易 Flex/Grid、图片盒。
6. Paint Agent：实现 JavaFX Canvas 绘制、图片、渐变、阴影、多行文本。
7. Interaction Agent：实现 Layout Tree hit test、hover/active/focus 状态和事件触发重绘。
8. QA Agent：实现单元测试并运行 Maven 测试。

## 建议里程碑

### M1：可运行原型

- DOM/CSSOM/Style/Layout/Paint 闭环。
- Canvas Demo。
- 单元测试覆盖解析、层叠、布局。

### M2：可用布局

- inline 文本排版。
- 百分比、min/max、box-sizing。
- Flex wrap、alignment。
- 基础图片和滚动。

### M3：组件化 UI

- 输入框、按钮、列表、表格。
- 事件模型、hover/focus。
- 样式状态刷新和局部重排。

### M4：性能和工程化

- 渲染树缓存。
- 脏矩形重绘。
- 字体/图片资源缓存。
- 调试可视化工具。

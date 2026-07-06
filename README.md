# Miko Pure Render

Miko Pure Render 是一个纯 Java HTML/CSS 界面渲染库原型，不依赖 Chromium、WebView2 或浏览器内核。当前技术底座选用 JavaFX，渲染目标是 `Canvas`，核心链路覆盖：

- HTML 解析：DOM 树、标签、文本、属性。
- CSS 解析：基础选择器、声明块、内联样式。
- 样式计算：选择器匹配、优先级、层叠覆盖、基础继承。
- 布局引擎：盒模型、块级流式布局、文本换行、简易 Flex row/column、简易 Grid。
- 绘制层：JavaFX `GraphicsContext` 绘制背景、边框、圆角、多行文字、图片、渐变、阴影。
- 交互基础：Layout Tree 坐标命中测试、`:hover`、`:active`、`:focus` 状态重绘、click 冒泡事件、CSS cursor。

## 运行

```powershell
mvn test
mvn javafx:run
```

## 绑定后端方法

`MikoRenderView` 支持按 CSS 选择器绑定事件，点击命中元素或其祖先匹配选择器时会触发对应方法：

```java
MikoRenderView view = new MikoRenderView();

view.bindClick("#save", backendService::save);

view.bindClick(".card", event -> {
    String id = event.currentTarget().attribute("id").orElse("");
    backendService.selectCard(id);
    event.stopPropagation();
});
```

后端方法也可以直接更新前端元素，更新后会自动重新计算样式、布局并重绘：

```java
view.bindClick("#save", () -> {
    backendService.save();
    view.setText("#message", "Saved");
    view.addClass("#message", "success");
});

view.setAttribute("#avatar", "src", "avatar.png");
view.setStyle(".panel", "background-color: #fff; border: 1px solid #ddd;");
```

文本选择由 CSS 控制，默认可选；需要禁用时设置：

```css
button {
  user-select: none;
}

.article {
  user-select: text;
}
```

滚动条也可以通过 CSS 配置：

```css
.panel {
  height: 200px;
  overflow: auto;
  scrollbar-width: thin;              /* auto | thin | none | 12px */
  scrollbar-color: #147d64 #d9e2ec;   /* thumb track */
}
```

基础表单控件已支持输入：

```html
<input id="title" value="Editable title" placeholder="Title">
<textarea id="notes">Type notes here</textarea>
```

支持聚焦、键盘输入、退格/删除、左右移动 caret，`textarea` 支持回车换行。

## 初版边界

这是可运行的架构原型，不是完整浏览器。它先把端到端管线跑通，并预留扩展点：

- HTML：支持常见开始/结束标签、void 标签、属性、注释/doctype 跳过、`style/script` raw text。
- CSS：支持元素、`.class`、`#id`、简单组合、后代选择器、子代选择器、`:hover/:active/:focus`、`!important`、基础 `border/background` shorthand 和声明解析。
- Layout：支持 `display:block`、`display:flex`、`display:grid`、`width/height`、百分比长度、独立 `margin/padding` 边、文本换行、图片盒、基础滚动偏移。
- Paint：支持纯色背景、`linear-gradient(...)`、多行文字、边框、圆角、图片、简化 `box-shadow`、`overflow:hidden/auto/scroll` 裁剪。

详细架构见 [docs/core-development-plan.md](docs/core-development-plan.md)。

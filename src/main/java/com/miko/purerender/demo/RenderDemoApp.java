package com.miko.purerender.demo;

import com.miko.purerender.MikoRenderView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public final class RenderDemoApp extends Application {
    private static final String SAMPLE_HTML = """
            <body>
              <section id="app" class="shell">
                <h1>Miko Pure Render</h1>
                <p class="lead">HTML and CSS rendered by a pure Java layout and paint pipeline.</p>
                <div class="toolbar">
                  <button class="primary">Render</button>
                  <button>Inspect</button>
                  <button>Export</button>
                </div>
                <div class="form">
                  <input id="titleInput" value="Editable title" placeholder="Title">
                  <textarea id="notes">Type notes here</textarea>
                </div>
                <div class="cards">
                  <article class="card">
                    <h2>DOM</h2>
                    <p>Tags, text, attributes and child nodes.</p>
                  </article>
                  <article class="card accent">
                    <h2>Layout</h2>
                    <p>Box model, flow layout and simple flex.</p>
                  </article>
                  <article class="card">
                    <h2>Paint</h2>
                    <p>JavaFX Canvas renders the layout tree.</p>
                  </article>
                </div>
              </section>
            </body>
            """;

    private static final String SAMPLE_CSS = """
            body {
              background-color: #f4f7f8;
              color: #1f2933;
              font-size: 16px;
              font-family: System;
            }
            .shell {
              padding: 28px;
              background: linear-gradient(to right, #f4f7f8, #e0fcff);
            }
            h1 {
              color: #102a43;
              font-size: 34px;
              margin: 0 0 10px 0;
            }
            h2 {
              font-size: 20px;
              margin: 0 0 8px 0;
            }
            p {
              margin: 0 0 12px 0;
            }
            .lead {
              color: #52606d;
              font-size: 18px;
            }
            .lead.updated {
              color: #147d64;
            }
            .toolbar {
              display: flex;
              gap: 10px;
              margin: 12px 0 22px 0;
            }
            .form {
              display: flex;
              gap: 12px;
              margin: 0 0 22px 0;
            }
            input:focus, textarea:focus {
              border-color: #147d64;
            }
            button {
              background-color: #ffffff;
              border-width: 1px;
              border-color: #bcccdc;
              border-radius: 6px;
              padding: 8px 14px;
              width: 92px;
              cursor: pointer;
              user-select: none;
            }
            button.primary {
              background-color: #147d64;
              color: #ffffff;
              border-color: #147d64;
            }
            button:hover {
              background-color: #e0fcff;
              border-color: #38bec9;
            }
            button.primary:hover {
              background-color: #0c6b58;
            }
            button:active {
              background-color: #102a43;
              color: #ffffff;
            }
            button:focus {
              border-color: #f0b429;
              border-width: 2px;
            }
            .cards {
              display: grid;
              grid-template-columns: repeat(3, 1fr);
              gap: 14px;
            }
            .card {
              padding: 18px;
              background-color: #ffffff;
              border-width: 1px;
              border-color: #d9e2ec;
              border-radius: 8px;
              box-shadow: 0 4px 18px #00000022;
              height: 92px;
              overflow: auto;
              cursor: pointer;
              user-select: text;
              scrollbar-width: thin;
              scrollbar-color: #147d64 #d9e2ec;
            }
            .card.accent {
              background-color: #e0fcff;
              border-color: #38bec9;
            }
            .card:hover {
              background-color: #f0fdfa;
              border-color: #147d64;
            }
            """;

    @Override
    public void start(Stage stage) {
        TextArea htmlEditor = new TextArea(SAMPLE_HTML);
        TextArea cssEditor = new TextArea(SAMPLE_CSS);
        htmlEditor.setWrapText(false);
        cssEditor.setWrapText(false);

        TabPane editors = new TabPane(
                new Tab("HTML", htmlEditor),
                new Tab("CSS", cssEditor)
        );
        editors.getTabs().forEach(tab -> tab.setClosable(false));

        MikoRenderView renderView = new MikoRenderView();
        Button renderButton = new Button("Render");
        Label status = new Label("Ready");
        renderButton.setOnAction(event -> renderView.load(htmlEditor.getText(), cssEditor.getText()));
        renderView.addEventListener("click", event -> {
            if (event.currentTarget() == event.target()) {
                String id = event.target().attribute("id").map(value -> "#" + value).orElse("");
                String classes = event.target().attribute("class")
                        .map(value -> "." + value.replace(" ", "."))
                        .orElse("");
                status.setText("Clicked " + event.target().tagName() + id + classes);
            }
        });
        renderView.bindClick("button.primary", () -> {
            renderView.setText(".lead", "Updated by a backend Java method and repainted without a browser engine.");
            renderView.addClass(".lead", "updated");
            status.setText("Backend method: updated .lead");
        });
        renderView.bindClick(".card", event -> {
            event.currentTarget().attribute("class")
                    .ifPresent(value -> status.setText("Backend method: card selected ." + value.replace(" ", ".")));
            event.stopPropagation();
        });

        HBox header = new HBox(12, new Label("Miko Pure Render Demo"), renderButton, status);
        header.setPadding(new Insets(10));
        HBox.setHgrow(renderButton, Priority.NEVER);

        BorderPane previewPane = new BorderPane(renderView);
        previewPane.setTop(header);

        SplitPane splitPane = new SplitPane(editors, previewPane);
        splitPane.setDividerPositions(0.42);

        Scene scene = new Scene(splitPane, 1180, 760);
        stage.setTitle("Miko Pure Render");
        stage.setScene(scene);
        stage.show();
        renderView.load(htmlEditor.getText(), cssEditor.getText());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

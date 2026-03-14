package com.quietpages.quietpages;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;

public class HelloController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox sidebar;
    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnHome;
    @FXML
    private Button btnLibrary;
    @FXML
    private Button btnCollections;
    @FXML
    private Button btnOnlineBooks;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnThemes;
    @FXML
    private Button btnConnect;

    @FXML
    public void initialize() {
        showLibrary();
        setActiveNav(btnLibrary);
    }

    @FXML
    private void onHome() {
        loadTabSafe("home-view.fxml");
        setActiveNav(btnHome);
    }

    @FXML
    private void onLibrary() {
        showLibrary();
        setActiveNav(btnLibrary);
    }

    @FXML
    private void onCollections() {
        loadTabSafe("collections-view.fxml");
        setActiveNav(btnCollections);
    }

    @FXML
    private void onOnlineBooks() {
        loadTabSafe("online-books-view.fxml");
        setActiveNav(btnOnlineBooks);
    }

    @FXML
    private void onSettings() {
        loadTabSafe("settings-view.fxml");
        setActiveNav(btnSettings);
    }

    private void showLibrary() {
        loadTabSafe("library-view.fxml");
    }

    /**
     * Loads a tab FXML safely.
     * If the file does not exist yet (teammate hasn't built it),
     * shows a placeholder instead of crashing.
     */
    private void loadTabSafe(String fxmlName) {
        URL resource = HelloApplication.class.getResource(fxmlName);
        if (resource == null) {
            showPlaceholder(fxmlName);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("[Shell] Failed to load " + fxmlName + ": " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("  Caused by: " + cause);
                cause = cause.getCause();
            }
            showPlaceholder(fxmlName);
        }
    }

    /** Shows a simple "coming soon" label when a tab's FXML doesn't exist yet. */
    private void showPlaceholder(String fxmlName) {
        String tabName = fxmlName.replace("-view.fxml", "").replace("-", " ");
        javafx.scene.control.Label label = new javafx.scene.control.Label(
                tabName.substring(0, 1).toUpperCase() + tabName.substring(1)
                        + " tab — coming soon");
        label.setStyle("-fx-text-fill: #888888; -fx-font-size: 16px;");
        StackPane placeholder = new StackPane(label);
        placeholder.setStyle("-fx-background-color: #2B2B2B;");
        contentArea.getChildren().setAll(placeholder);
    }

    private void setActiveNav(Button active) {
        for (Node n : sidebar.getChildren()) {
            if (n instanceof Button b) {
                b.getStyleClass().remove("nav-active");
            }
        }
        if (active != null)
            active.getStyleClass().add("nav-active");
    }
}
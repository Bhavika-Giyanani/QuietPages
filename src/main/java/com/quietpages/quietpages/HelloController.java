package com.quietpages.quietpages;

import com.quietpages.quietpages.controller.LibraryController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

import java.net.URL;

public class HelloController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private BorderPane rootPane;
    @FXML private VBox       sidebar;
    @FXML private StackPane  contentArea;

    @FXML private Button btnMenuToggle;
    @FXML private Button btnHome;
    @FXML private Button btnLibrary;
    @FXML private Button btnCollections;
    @FXML private Button btnOnlineBooks;
    @FXML private Button btnSettings;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean sidebarCollapsed = true;

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        collapseSidebar();
        showLibrary();
        setActiveNav(btnLibrary);
    }

    // ── Hamburger toggle ──────────────────────────────────────────────────────
    @FXML
    private void onMenuToggle() {
        sidebarCollapsed = !sidebarCollapsed;
        if (sidebarCollapsed) collapseSidebar();
        else                  expandSidebar();
    }

    private void collapseSidebar() {
        sidebar.setPrefWidth(48);
        sidebar.setMinWidth(48);
        sidebar.setMaxWidth(48);

        setNavButtonText(btnHome,        null);
        setNavButtonText(btnLibrary,     null);
        setNavButtonText(btnCollections, null);
        setNavButtonText(btnOnlineBooks, null);
        setNavButtonText(btnSettings,    null);
    }

    private void expandSidebar() {
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);

        setNavButtonText(btnHome,        "  Home");
        setNavButtonText(btnLibrary,     "  Library");
        setNavButtonText(btnCollections, "  Collections");
        setNavButtonText(btnOnlineBooks, "  Online Books");
        setNavButtonText(btnSettings,    "  Settings");
    }

    private void setNavButtonText(Button btn, String text) {
        if (btn == null) return;
        if (text == null) {
            btn.setText("");
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(javafx.geometry.Pos.CENTER);
        } else {
            btn.setText(text);
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }
    }

    // ── Tab navigation ────────────────────────────────────────────────────────
    @FXML private void onHome()        { loadTabSafe("home-view.fxml");         setActiveNav(btnHome); }
    @FXML private void onLibrary()     { showLibrary();                          setActiveNav(btnLibrary); }
    @FXML private void onCollections() { loadTabSafe("collections-view.fxml");  setActiveNav(btnCollections); }
    @FXML private void onOnlineBooks() { loadTabSafe("online-books-view.fxml"); setActiveNav(btnOnlineBooks); }
    @FXML private void onSettings()    { loadTabSafe("settings-view.fxml");     setActiveNav(btnSettings); }

    private void showLibrary() {
        URL resource = HelloApplication.class.getResource("library-view.fxml");
        if (resource == null) { showPlaceholder("library-view.fxml"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            // Reader not yet implemented — openBook callback left unwired
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("[Shell] Failed to load library-view.fxml: " + e.getMessage());
            showPlaceholder("library-view.fxml");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void loadTabSafe(String fxmlName) {
        URL resource = HelloApplication.class.getResource(fxmlName);
        if (resource == null) { showPlaceholder(fxmlName); return; }
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

    private void showPlaceholder(String fxmlName) {
        String tabName = fxmlName.replace("-view.fxml", "").replace("-", " ");
        javafx.scene.control.Label label = new javafx.scene.control.Label(
                tabName.substring(0, 1).toUpperCase() + tabName.substring(1)
                        + " — coming soon");
        label.setStyle("-fx-text-fill: #888888; -fx-font-size: 16px;");
        StackPane placeholder = new StackPane(label);
        placeholder.setStyle("-fx-background-color: #2B2B2B;");
        contentArea.getChildren().setAll(placeholder);
    }

    private void setActiveNav(Button active) {
        for (Node n : sidebar.getChildren()) {
            if (n instanceof Button b) b.getStyleClass().remove("nav-active");
        }
        if (active != null) active.getStyleClass().add("nav-active");
    }
}
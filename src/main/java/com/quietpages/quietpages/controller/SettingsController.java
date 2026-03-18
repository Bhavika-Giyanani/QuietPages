package com.quietpages.quietpages.controller;

import com.quietpages.quietpages.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;

/**
 * Controller for settings-view.fxml.
 *
 * Adapted from teammate's SettingsPage.java.
 * Original called ThemeManager.applyTheme("dark.css") — we strip
 * the ".css" extension and forward to our ThemeManager which injects
 * CSS variable overrides across the whole scene instead of swapping files.
 */
public class SettingsController {

    @FXML
    private FlowPane themesPane;

    @FXML
    private Button btnLight;
    @FXML
    private Button btnDark;
    @FXML
    private Button btnOcean;
    @FXML
    private Button btnSunset;
    @FXML
    private Button btnForest;
    @FXML
    private Button btnLavender;
    @FXML
    private Button btnRose;
    @FXML
    private Button btnMidnight;

    @FXML
    public void initialize() {
        markActive(ThemeManager.getCurrentTheme());
    }

    @FXML
    private void onLight() {
        apply("light");
    }

    @FXML
    private void onDark() {
        apply("dark");
    }

    @FXML
    private void onOcean() {
        apply("ocean");
    }

    @FXML
    private void onSunset() {
        apply("sunset");
    }

    @FXML
    private void onForest() {
        apply("forest");
    }

    @FXML
    private void onLavender() {
        apply("lavender");
    }

    @FXML
    private void onRose() {
        apply("rose");
    }

    @FXML
    private void onMidnight() {
        apply("midnight");
    }

    private void apply(String theme) {
        ThemeManager.applyTheme(theme);
        markActive(theme);
    }

    private void markActive(String theme) {
        if (themesPane == null || theme == null)
            return;
        themesPane.getChildren().forEach(node -> {
            if (node instanceof Button btn) {
                boolean active = btn.getId() != null
                        && btn.getId().toLowerCase().contains(theme.toLowerCase());
                if (active) {
                    if (!btn.getStyleClass().contains("settings-theme-btn-active"))
                        btn.getStyleClass().add("settings-theme-btn-active");
                } else {
                    btn.getStyleClass().remove("settings-theme-btn-active");
                }
            }
        });
    }
}
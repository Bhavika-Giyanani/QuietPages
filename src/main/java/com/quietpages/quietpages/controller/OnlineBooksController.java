package com.quietpages.quietpages.controller;

import com.quietpages.quietpages.model.DownloadEntry;
import com.quietpages.quietpages.model.OnlineSite;
import com.quietpages.quietpages.service.LibraryService;
import com.quietpages.quietpages.service.OnlineBooksService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Controller for online-books-view.fxml.
 *
 * WebView is created lazily via reflection-free code in initialize().
 * No javafx.web types appear as @FXML fields — this avoids FXML loader
 * reflection issues with the javafx.web module.
 */
public class OnlineBooksController {

    // ── FXML — Toolbar ────────────────────────────────────────────────────────
    @FXML private Button btnAddSite;
    @FXML private Button btnDownloads;
    @FXML private Button btnInfo;
    @FXML private Button btnHome;
    @FXML private Button btnRefresh;
    @FXML private Button btnBack;
    @FXML private Button btnForward;

    // ── FXML — Left sidebar ───────────────────────────────────────────────────
    @FXML private VBox siteListVBox;

    // ── FXML — Main content area ──────────────────────────────────────────────
    @FXML private StackPane webContainer;
    @FXML private Label     lblSelectSite;

    // ── FXML — Downloads panel ────────────────────────────────────────────────
    @FXML private VBox downloadsPanel;
    @FXML private VBox downloadsListVBox;
    @FXML private Label lblNoDownloads;

    // ── State — NO javafx.web types as fields ────────────────────────────────
    private final OnlineBooksService service       = OnlineBooksService.getInstance();
    private final LibraryService     libraryService = LibraryService.getInstance();

    private ObservableList<OnlineSite>    sites     = FXCollections.observableArrayList();
    private ObservableList<DownloadEntry> downloads = FXCollections.observableArrayList();

    // WebView held as Object to avoid any class-loading at field-declaration time
    private Object  webViewObj;    // actually javafx.scene.web.WebView
    private Object  engineObj;     // actually javafx.scene.web.WebEngine

    private OnlineSite activeSite = null;

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        downloadsPanel.setVisible(false);
        downloadsPanel.setManaged(false);

        setupWebView();
        loadSites();
    }

    private void setupWebView() {
        // Create WebView via reflection-safe direct instantiation
        // (javafx.web is already required in module-info)
        javafx.scene.web.WebView wv = new javafx.scene.web.WebView();
        javafx.scene.web.WebEngine we = wv.getEngine();

        webViewObj = wv;
        engineObj  = we;

        wv.setVisible(false);

        // Detect EPUB/PDF navigation and intercept as a download
        we.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl == null || newUrl.isBlank()) return;
            String lower = newUrl.toLowerCase();
            if (lower.endsWith(".epub") || lower.endsWith(".pdf")) {
                // Go back, then download the file ourselves
                we.executeScript("history.back()");
                startDownload(newUrl);
                return;
            }
            // Show webview once a real page loads
            lblSelectSite.setVisible(false);
            wv.setVisible(true);
        });

        // Insert at index 0 — behind the placeholder label and downloads panel
        webContainer.getChildren().add(0, wv);
        StackPane.setAlignment(wv, Pos.CENTER);
    }

    // ── Site list ─────────────────────────────────────────────────────────────
    private void loadSites() {
        sites = service.getAllSites();
        renderSiteList();
    }

    private void renderSiteList() {
        siteListVBox.getChildren().clear();
        for (OnlineSite site : sites) {
            siteListVBox.getChildren().add(createSiteRow(site));
        }
    }

    private HBox createSiteRow(OnlineSite site) {
        HBox row = new HBox(12);
        row.getStyleClass().add("site-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(60);

        // Icon
        ImageView icon = new ImageView();
        icon.setFitWidth(36);
        icon.setFitHeight(36);
        icon.setPreserveRatio(true);

        if (site.getIconData() != null && site.getIconData().length > 0) {
            try {
                icon.setImage(new Image(new ByteArrayInputStream(site.getIconData())));
            } catch (Exception ignored) {}
        } else {
            loadFaviconAsync(site.getUrl(), icon);
        }

        Label lbl = new Label(site.getTitle());
        lbl.getStyleClass().add("site-title");

        row.getChildren().addAll(icon, lbl);
        row.setUserData(site);

        // Left-click: load site
        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                activateSite(site, row);
            } else if (e.getButton() == MouseButton.SECONDARY && !site.isDefault()) {
                showSiteContextMenu(site, row);
            }
        });

        return row;
    }

    private void activateSite(OnlineSite site, HBox row) {
        activeSite = site;
        // Highlight active
        siteListVBox.getChildren().forEach(n -> n.getStyleClass().remove("site-row-active"));
        row.getStyleClass().add("site-row-active");
        // Load in WebView
        getEngine().load(site.getUrl());
    }

    private void showSiteContextMenu(OnlineSite site, Node anchor) {
        ContextMenu menu = new ContextMenu();

        MenuItem edit   = new MenuItem("  Edit");
        MenuItem remove = new MenuItem("  Remove");

        edit.setOnAction(e -> showAddSiteDialog(site));
        remove.setOnAction(e -> {
            service.removeSite(site.getId());
            if (activeSite != null && activeSite.getId() == site.getId()) {
                activeSite = null;
                lblSelectSite.setVisible(true);
                getWebView().setVisible(false);
            }
            loadSites();
        });

        menu.getItems().addAll(edit, remove);
        menu.show(anchor, Side.BOTTOM, 0, 0);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    @FXML private void onAddSite()   { showAddSiteDialog(null); }

    @FXML private void onDownloads() {
        boolean show = !downloadsPanel.isVisible();
        downloadsPanel.setVisible(show);
        downloadsPanel.setManaged(show);
        if (show) updateDownloadsPanel();
    }

    @FXML private void onHome() {
        if (activeSite != null) getEngine().load(activeSite.getUrl());
    }

    @FXML private void onRefresh() {
        getEngine().reload();
    }

    @FXML private void onBack() {
        getEngine().executeScript("history.back()");
    }

    @FXML private void onForward() {
        getEngine().executeScript("history.forward()");
    }

    @FXML private void onInfo() { /* reserved for future use */ }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────
    private void showAddSiteDialog(OnlineSite existing) {
        boolean isEdit = existing != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit site" : "Add site");

        DialogPane pane = new DialogPane();
        pane.getStylesheets().add(
                getClass().getResource("/com/quietpages/quietpages/library.css").toExternalForm());
        pane.getStyleClass().add("edit-dialog-pane");
        pane.setPrefWidth(460);

        // ── Form ──────────────────────────────────────────────────────────────
        VBox form = new VBox(14);
        form.setPadding(new Insets(16));

        Label heading = new Label(isEdit ? "Edit site" : "Add site");
        heading.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#DDDDDD;");

        // URL row
        Label urlLbl = new Label("Site url :");
        urlLbl.getStyleClass().add("edit-field-label");
        TextField urlField = new TextField(isEdit ? existing.getUrl() : "");
        urlField.getStyleClass().add("edit-field");
        urlField.setMaxWidth(Double.MAX_VALUE);
        HBox urlRow = new HBox(12, urlLbl, urlField);
        urlRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(urlField, Priority.ALWAYS);

        // Title row
        Label titleLbl = new Label("Title :");
        titleLbl.getStyleClass().add("edit-field-label");
        TextField titleField = new TextField(isEdit ? existing.getTitle() : "");
        titleField.getStyleClass().add("edit-field");
        titleField.setMaxWidth(Double.MAX_VALUE);
        HBox titleRow = new HBox(12, titleLbl, titleField);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleField, Priority.ALWAYS);

        // Icon row
        final byte[][] iconBytes = {isEdit ? existing.getIconData() : null};
        ImageView iconPreview = new ImageView();
        iconPreview.setFitWidth(48);
        iconPreview.setFitHeight(48);
        iconPreview.setPreserveRatio(true);
        if (iconBytes[0] != null && iconBytes[0].length > 0) {
            try { iconPreview.setImage(
                    new Image(new ByteArrayInputStream(iconBytes[0]))); }
            catch (Exception ignored) {}
        }
        StackPane iconBox = new StackPane(iconPreview);
        iconBox.setStyle("-fx-background-color:#3A3A3A;-fx-background-radius:4;");
        iconBox.setMinSize(58, 58);
        iconBox.setMaxSize(58, 58);

        Button selectIconBtn = new Button("Select icc");
        selectIconBtn.getStyleClass().add("edit-cover-btn");
        selectIconBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Icon");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files",
                            "*.png","*.jpg","*.jpeg","*.ico","*.webp"));
            File f = fc.showOpenDialog(selectIconBtn.getScene().getWindow());
            if (f != null) {
                try {
                    iconBytes[0] = Files.readAllBytes(f.toPath());
                    iconPreview.setImage(new Image(f.toURI().toString()));
                } catch (Exception ex) {
                    System.err.println("[Online] Icon load: " + ex.getMessage());
                }
            }
        });

        // Auto-load favicon on URL focus-out
        urlField.focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused && !urlField.getText().isBlank()) {
                loadFaviconAsync(urlField.getText(), iconPreview);
            }
        });

        Label iccLbl = new Label("Icon :");
        iccLbl.getStyleClass().add("edit-field-label");
        HBox iconRow = new HBox(12, iconBox, selectIconBtn);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        HBox iconFormRow = new HBox(12, iccLbl, iconRow);
        iconFormRow.setAlignment(Pos.CENTER_LEFT);

        form.getChildren().addAll(heading, urlRow, titleRow, iconFormRow);
        pane.setContent(form);

        ButtonType saveType = new ButtonType(
                isEdit ? "Update" : "Add", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveType,
                new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));

        dialog.setDialogPane(pane);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String url   = urlField.getText().trim();
            String title = titleField.getText().trim();
            if (url.isBlank() || title.isBlank()) return;

            if (isEdit) {
                existing.setUrl(url);
                existing.setTitle(title);
                if (iconBytes[0] != null) existing.setIconData(iconBytes[0]);
                service.updateSite(existing);
            } else {
                OnlineSite newSite = new OnlineSite();
                newSite.setUrl(url);
                newSite.setTitle(title);
                newSite.setDefault(false);
                newSite.setIconData(iconBytes[0]);
                service.addSite(newSite);
            }
            loadSites();
        }
    }

    // ── Downloads panel ───────────────────────────────────────────────────────
    private void updateDownloadsPanel() {
        downloadsListVBox.getChildren().clear();

        if (downloads.isEmpty()) {
            lblNoDownloads.setVisible(true);
            lblNoDownloads.setManaged(true);
            return;
        }
        lblNoDownloads.setVisible(false);
        lblNoDownloads.setManaged(false);

        for (DownloadEntry entry : downloads) {
            VBox item = new VBox(3);
            item.getStyleClass().add("download-item");

            Label nameLbl = new Label(entry.getFileName());
            nameLbl.getStyleClass().add("download-name");
            nameLbl.setWrapText(true);
            nameLbl.setMaxWidth(290);

            Label statusLbl = new Label(entry.getStatusLabel());
            statusLbl.getStyleClass().add(
                    entry.getStatus() == DownloadEntry.Status.FAILED
                            ? "download-status-failed" : "download-status-ok");

            item.getChildren().addAll(nameLbl, statusLbl);
            downloadsListVBox.getChildren().add(item);
        }
    }

    // ── Download a file ───────────────────────────────────────────────────────
    private void startDownload(String fileUrl) {
        String fileName = fileUrl.contains("/")
                ? fileUrl.substring(fileUrl.lastIndexOf('/') + 1) : fileUrl;
        // Strip query params from filename
        if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));

        DownloadEntry entry = new DownloadEntry(fileName);
        downloads.add(0, entry);
        downloadsPanel.setVisible(true);
        downloadsPanel.setManaged(true);
        updateDownloadsPanel();

        final String finalFileName = fileName;
        Task<File> task = new Task<>() {
            @Override protected File call() throws Exception {
                URL url = URI.create(fileUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();

                Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"), "QuietPages");
                Files.createDirectories(tmpDir);
                Path dest = tmpDir.resolve(finalFileName);

                try (InputStream in  = conn.getInputStream();
                     OutputStream out = Files.newOutputStream(dest)) {
                    in.transferTo(out);
                }
                return dest.toFile();
            }
        };

        task.setOnSucceeded(e -> {
            File downloaded = task.getValue();
            com.quietpages.quietpages.model.Book book =
                    libraryService.importFile(downloaded);
            Platform.runLater(() -> {
                entry.setStatus(book != null
                        ? DownloadEntry.Status.COMPLETED
                        : DownloadEntry.Status.FAILED);
                updateDownloadsPanel();
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            entry.setStatus(DownloadEntry.Status.FAILED);
            updateDownloadsPanel();
            System.err.println("[Download] Failed: " +
                    task.getException().getMessage());
        }));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Favicon helpers ───────────────────────────────────────────────────────
    private void loadFaviconAsync(String siteUrl, ImageView target) {
        Task<byte[]> task = new Task<>() {
            @Override protected byte[] call() {
                return fetchFaviconBytes(siteUrl);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            byte[] bytes = task.getValue();
            if (bytes != null && bytes.length > 0) {
                try { target.setImage(new Image(new ByteArrayInputStream(bytes))); }
                catch (Exception ignored) {}
            }
        }));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private byte[] fetchFaviconBytes(String siteUrl) {
        try {
            if (!siteUrl.startsWith("http")) siteUrl = "https://" + siteUrl;
            URL url = URI.create(siteUrl).toURL();
            String faviconUrl = url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
            HttpURLConnection conn = (HttpURLConnection)
                    URI.create(faviconUrl).toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();
            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return is.readAllBytes();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── WebView accessors (avoid casting at every call site) ──────────────────
    private javafx.scene.web.WebView getWebView() {
        return (javafx.scene.web.WebView) webViewObj;
    }

    private javafx.scene.web.WebEngine getEngine() {
        return (javafx.scene.web.WebEngine) engineObj;
    }
}
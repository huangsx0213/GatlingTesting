package com.qa.app.ui.vm;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

public class GatlingInternalReportViewModel implements Initializable {

    private static final String LAST_DIR_PREF_KEY = "lastGatlingInternalReportDir";

    @FXML
    private ComboBox<File> reportDirectoriesCombo;
    @FXML
    private WebView webView;
    @FXML
    private StackPane webViewContainer;

    private Preferences prefs;
    private double zoomLevel = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 3.0;

    public void refresh() {
        loadInitialDirectory();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        prefs = Preferences.userNodeForPackage(GatlingInternalReportViewModel.class);

        // Bind WebView size to its container to fix scrollbar issues
        webView.prefWidthProperty().bind(webViewContainer.widthProperty());
        webView.prefHeightProperty().bind(webViewContainer.heightProperty());
        webView.setZoom(zoomLevel); // Initialize zoom level

        // Configure ComboBox to display directory names
        reportDirectoriesCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        reportDirectoriesCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        loadInitialDirectory();
    }

    private void loadInitialDirectory() {
        String lastDirPath = prefs.get(LAST_DIR_PREF_KEY, null);
        File initialDir = null;

        if (lastDirPath != null) {
            File lastDir = new File(lastDirPath);
            if (lastDir.exists() && lastDir.isDirectory()) {
                initialDir = lastDir.getParentFile();
            }
        }

        if (initialDir == null) {
            initialDir = new File("target/gatling");
        }

        if (initialDir.exists()) {
            populateReportDirectories(initialDir);
            if (!reportDirectoriesCombo.getItems().isEmpty()) {
                // Default to the latest report (last in the sorted list)
                reportDirectoriesCombo.getSelectionModel().selectFirst();
                File selectedDir = reportDirectoriesCombo.getSelectionModel().getSelectedItem();
                loadReport(selectedDir);
                savePreferences(selectedDir);
            }
        }
    }

    private void populateReportDirectories(File parentDir) {
        if (parentDir == null || !parentDir.isDirectory()) {
            return;
        }
        reportDirectoriesCombo.getItems().clear();
        try (Stream<Path> stream = Files.walk(parentDir.toPath(), 1)) {
            stream.map(Path::toFile)
                  .filter(file -> file.isDirectory() && file.getName().contains("simulation"))
                  .sorted(Comparator.comparingLong(File::lastModified).reversed())
                  .forEach(reportDirectoriesCombo.getItems()::add);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDirectorySelection() {
        File selectedDir = reportDirectoriesCombo.getSelectionModel().getSelectedItem();
        if (selectedDir != null) {
            loadReport(selectedDir);
            savePreferences(selectedDir);
        }
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Gatling Report Folder");
        String lastDirPath = prefs.get(LAST_DIR_PREF_KEY, null);
        if (lastDirPath != null) {
            File initialDir = new File(lastDirPath);
            if (initialDir.exists() && initialDir.isDirectory()) {
                directoryChooser.setInitialDirectory(initialDir.getParentFile());
            }
        }

        File selectedDirectory = directoryChooser.showDialog(getStage());
        if (selectedDirectory != null) {
            populateReportDirectories(selectedDirectory.getParentFile());
            reportDirectoriesCombo.getSelectionModel().select(selectedDirectory);
            loadReport(selectedDirectory);
            savePreferences(selectedDirectory);
        }
    }

    private void loadReport(File directory) {
        if (directory == null || !directory.isDirectory()) {
            webView.getEngine().loadContent("<html><body><h1>Error: Invalid directory selected.</h1></body></html>");
            return;
        }

        File indexFile = new File(directory, "index.html");
        if (indexFile.exists()) {
            try {
                webView.getEngine().load(indexFile.toURI().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                webView.getEngine().loadContent("<html><body><h1>Error loading report.</h1></body></html>");
            }
        } else {
            webView.getEngine().loadContent("<html><body><h1>Error: index.html not found in selected folder.</h1></body></html>");
        }
    }

    private void savePreferences(File directory) {
        if (directory != null) {
            prefs.put(LAST_DIR_PREF_KEY, directory.getAbsolutePath());
        }
    }

    @FXML
    private void handleZoomIn() {
        if (zoomLevel < ZOOM_MAX) {
            zoomLevel += ZOOM_STEP;
            webView.setZoom(zoomLevel);
        }
    }

    @FXML
    private void handleZoomOut() {
        if (zoomLevel > ZOOM_MIN) {
            zoomLevel -= ZOOM_STEP;
            webView.setZoom(zoomLevel);
        }
    }

    private Stage getStage() {
        return (Stage) webView.getScene().getWindow();
    }
} 
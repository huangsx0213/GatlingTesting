package com.qa.app.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public class DialogHelper {

    /**
     * Centers a dialog on its owner window.
     *
     * @param owner  The owner window.
     * @param dialog The dialog stage to be centered.
     */
    public static void centerDialogOnOwner(Window owner, Stage dialog) {
        if (owner != null && dialog != null) {
            dialog.setOnShown(event -> {
                double ownerX = owner.getX();
                double ownerY = owner.getY();
                double ownerWidth = owner.getWidth();
                double ownerHeight = owner.getHeight();

                double dialogWidth = dialog.getWidth();
                double dialogHeight = dialog.getHeight();

                dialog.setX(ownerX + (ownerWidth - dialogWidth) / 2);
                dialog.setY(ownerY + (ownerHeight - dialogHeight) / 3);
            });
        }
    }

    /**
     * Shows a reusable large text editor dialog with optional JSON formatting support.
     *
     * @param title            dialog title.
     * @param initialValue     initial text content (nullable).
     * @param ownerNode        node used to resolve dialog owner (nullable).
     * @param enableJsonFormat whether to display a "Format JSON" helper button.
     * @return edited text when confirmed, otherwise null.
     */
    public static String showLargeTextEditor(String title,
                                             String initialValue,
                                             Node ownerNode,
                                             boolean enableJsonFormat) {
        return showLargeTextEditor(title, initialValue, ownerNode, enableJsonFormat, 400, 300);
    }

    /**
     * Shows a reusable large text editor dialog with configurable size.
     *
     * @param title            dialog title.
     * @param initialValue     initial text content (nullable).
     * @param ownerNode        node used to resolve dialog owner (nullable).
     * @param enableJsonFormat whether to display a "Format JSON" helper button.
     * @param prefWidth        preferred text area width.
     * @param prefHeight       preferred text area height.
     * @return edited text when confirmed, otherwise null.
     */
    public static String showLargeTextEditor(String title,
                                             String initialValue,
                                             Node ownerNode,
                                             boolean enableJsonFormat,
                                             double prefWidth,
                                             double prefHeight) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);

        Window ownerWindow = null;
        if (ownerNode != null && ownerNode.getScene() != null) {
            ownerWindow = ownerNode.getScene().getWindow();
            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }
        }

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        ButtonType formatJsonButtonType = null;
        if (enableJsonFormat) {
            formatJsonButtonType = new ButtonType("Format JSON", ButtonBar.ButtonData.RIGHT);
            dialog.getDialogPane().getButtonTypes().add(formatJsonButtonType);
        }

        TextArea textArea = new TextArea(initialValue == null ? "" : initialValue);
        textArea.setWrapText(true);
        textArea.setPrefSize(prefWidth, prefHeight);
        dialog.getDialogPane().setContent(textArea);
        dialog.setResizable(true);

        if (enableJsonFormat && formatJsonButtonType != null) {
            Button formatButton = (Button) dialog.getDialogPane().lookupButton(formatJsonButtonType);
            ObjectMapper mapper = new ObjectMapper();
            formatButton.addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    Object json = mapper.readValue(textArea.getText(), Object.class);
                    String formatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                    textArea.setText(formatted);
                } catch (JsonProcessingException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("JSON Format Error");
                    alert.setHeaderText("Invalid JSON");
                    alert.setContentText("The text could not be formatted as JSON. Please check the syntax.");
                    if (dialog.getDialogPane().getScene() != null) {
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                    }
                    alert.showAndWait();
                }
                event.consume();
            });
        }

        dialog.setResultConverter(btn -> btn == okButtonType ? textArea.getText() : null);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        try {
            stage.getIcons().add(new Image(DialogHelper.class.getResourceAsStream("/static/icon/favicon.png")));
        } catch (Exception ignored) {
        }

        centerDialogOnOwner(ownerWindow, stage);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
}

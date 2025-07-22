package com.qa.app.ui.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

/**
 * A TableCell that shows a styled tooltip on hover for long text.
 * The tooltip remains visible as long as the mouse is over the cell.
 * @param <S> The type of the TableView row to which this cell belongs.
 */
public class ClickableTooltipTableCell<S> extends TableCell<S, String> {

    private final Tooltip tooltip;

    public ClickableTooltipTableCell() {
        // Initialize the tooltip and set its style.
        // This single instance will be reused for the cell.
        tooltip = new Tooltip();
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(400);
        tooltip.setStyle(
            "-fx-background-color: #3c3c3c;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 12px;" +
            "-fx-border-width: 0;" +
            "-fx-background-radius: 8;" +
            "-fx-font-size: 13px;" // Slightly larger font for readability
        );
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null || item.isEmpty()) {
            setText(null);
            setTooltip(null);
        } else {
            setText(item);
            tooltip.setText(item);
            setTooltip(tooltip);
        }
    }
} 
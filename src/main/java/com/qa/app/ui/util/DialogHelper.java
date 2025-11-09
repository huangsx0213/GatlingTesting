package com.qa.app.ui.util;

import javafx.stage.Stage;
import javafx.stage.Window;

public class DialogHelper {

    /**
     * Centers a dialog on its owner window.
     *
     * @param owner The owner window.
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
}

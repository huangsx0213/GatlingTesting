package com.qa.app.ui.vm;

import com.qa.app.model.GatlingRunParameters;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class GatlingRunDialogViewModel {

    @FXML
    private Spinner<Integer> usersSpinner;
    @FXML
    private Spinner<Integer> rampUpSpinner;
    @FXML
    private Spinner<Integer> repetitionsSpinner;

    @FXML
    public void initialize() {
        usersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1));
        rampUpSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3600, 0));
        repetitionsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1));
    }

    public GatlingRunParameters getParameters() {
        return new GatlingRunParameters(
                usersSpinner.getValue(),
                rampUpSpinner.getValue(),
                repetitionsSpinner.getValue()
        );
    }
} 
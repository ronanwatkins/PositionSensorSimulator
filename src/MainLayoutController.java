import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import model.*;
import utils.Vector;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    @FXML
    private Slider yawSlider;
    @FXML
    private Slider pitchSlider;
    @FXML
    private Slider rollSlider;

    @FXML
    private Label yawLabel;
    @FXML
    private Label pitchLabel;
    @FXML
    private Label rollLabel;

    @FXML
    private Label magneticFieldLabel;
    @FXML
    private Label accelerometerLabel;
    @FXML
    private Label gyroscopeLabel;

    private int yawValue;
    private int pitchValue;
    private int rollValue;

    private final double GRAVITY_CONSTANT = 9.80665;
    private final double MAGNETIC_NORTH = 22874.1;
    private final double MAGNETIC_EAST = 5939.5;
    private final double MAGNETIC_VERTICAL = 43180.5;

    private static final DecimalFormat TWO_DECIMAL_FORMAT = new DecimalFormat("#0.00");

    private AccelerometerModel accelerometerModel;
    private GyroscopeModel gyroscopeModel;
    private MagneticFieldModel magneticFieldModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        accelerometerModel = new AccelerometerModel();
        gyroscopeModel = new GyroscopeModel();
        magneticFieldModel = new MagneticFieldModel();

        accelerometerModel.setUpdateDuration(200);
        gyroscopeModel.setUpdateDuration(200);
        magneticFieldModel.setUpdateDuration(200);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateSensorValues();
                return null;
            }
        };
        new Thread(task).start();

        yawSlider.valueProperty().addListener((observable, oldvalue, newvalue) ->
                {
                    yawValue = newvalue.intValue();
                    updateSliderValues();
                } );

        pitchSlider.valueProperty().addListener((observable, oldvalue, newvalue) ->
                {
                    pitchValue = newvalue.intValue();
                    updateSliderValues();
                } );

        rollSlider.valueProperty().addListener((observable, oldvalue, newvalue) ->
                {
                    rollValue = newvalue.intValue();
                    updateSliderValues();
                } );

    }

    private void updateSliderValues() {
        // Restrict pitch value to -90 to +90
        if (pitchValue < -90) {
            pitchValue = -180 - pitchValue;
            yawValue += 180;
            rollValue += 180;
        } else if (pitchValue > 90) {
            pitchValue = 180 - pitchValue;
            yawValue += 180;
            rollValue += 180;
        }

        // yaw from 0 to 360
        if (yawValue < 0) {
            yawValue = yawValue + 360;
        }
        if (yawValue >= 360) {
            yawValue -= 360;
        }

        // roll from -180 to + 180
        if (rollValue >= 180) {
            rollValue -= 360;
        }

        updateMagneticFieldData();
        updateAccelerometerData();

        yawLabel.setText(yawValue + "");
        pitchLabel.setText(pitchValue + "");
        rollLabel.setText(rollValue + "");
    }

    private void updateSensorValues() {
        while(true) {
            gyroscopeModel.refreshAngularSpeed(10, pitchValue, yawValue, rollValue);

            accelerometerModel.updateSensorReadoutValues();
            gyroscopeModel.updateSensorReadoutValues();
            magneticFieldModel.updateSensorReadoutValues();

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    gyroscopeLabel.setText(TWO_DECIMAL_FORMAT.format(gyroscopeModel.getReadGyroscopePitch())
                            + ", "
                            + TWO_DECIMAL_FORMAT.format(gyroscopeModel.getReadGyroscopeYaw())
                            + ", "
                            + TWO_DECIMAL_FORMAT.format(gyroscopeModel.getReadGyroscopeRoll()));

                    magneticFieldLabel.setText(TWO_DECIMAL_FORMAT.format(magneticFieldModel.getReadCompassX())
                            + ", "
                            + TWO_DECIMAL_FORMAT.format(magneticFieldModel.getReadCompassY())
                            + ", "
                            + TWO_DECIMAL_FORMAT.format(magneticFieldModel.getReadCompassZ()));
                    
                    accelerometerLabel.setText(TWO_DECIMAL_FORMAT.format(accelerometerModel.getReadAccelerometerX())
                            + ", "
                            + TWO_DECIMAL_FORMAT.format(accelerometerModel.getReadAccelerometerY())
                            + ", "
                            + TWO_DECIMAL_FORMAT.format(accelerometerModel.getReadAccelerometerZ()));
                }
            });
            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    private void updateMagneticFieldData() {

        Vector magneticFieldVector = new Vector(MAGNETIC_EAST, MAGNETIC_NORTH, -MAGNETIC_VERTICAL);
        magneticFieldVector.scale(0.001); // convert from nT (nano-Tesla) to uT
        // (micro-Tesla)

        magneticFieldVector.reverserollpitchyaw(rollValue, pitchValue, yawValue);
        magneticFieldModel.setCompass(magneticFieldVector);
    }

    private void updateAccelerometerData() {

        // get component vectors (gravity + linear_acceleration)
        Vector gravityVec = getGravityVector();
        Vector linearVec = getLinearAccVector(accelerometerModel);

        Vector resultVec = Vector.addVectors(gravityVec, linearVec);

        accelerometerModel.setXYZ(resultVec);

        double limit = GRAVITY_CONSTANT * 10;

        accelerometerModel.limitate(limit);
    }

    private Vector getLinearAccVector(AccelerometerModel accModel) {
        double meterPerPixel = 1. / 3000;
        double dt = 0.001 * 1; // from ms to s
        double k = 500;
        double gamma = 50;

        accModel.refreshAcceleration(k, gamma, dt);

        // Now calculate this into mobile phone acceleration:
        // ! Mobile phone's acceleration is just opposite to
        // lab frame acceleration !
        Vector vec = new Vector(-accModel.getAx() * meterPerPixel, 0,
                -accModel.getAz() * meterPerPixel);
        vec.reverserollpitchyaw(rollValue, pitchValue, yawValue);

        return vec;
    }

    private Vector getGravityVector() {
        // apply orientation
        // we reverse roll, pitch, and yawDegree,
        // as this is how the mobile phone sees the coordinate system.
        Vector gravityVec = new Vector(0, 0, GRAVITY_CONSTANT);
        gravityVec.reverserollpitchyaw(rollValue, pitchValue, yawValue);

        return gravityVec;
    }
}

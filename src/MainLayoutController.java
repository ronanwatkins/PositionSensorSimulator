import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
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

    @FXML
    private Box box;

    @FXML
    private AnchorPane anchorPane;

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

    private double mousePosX = 0;
    private double mousePosY = 0;

    private Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        pitchSlider.setValue(-90);

        //region init
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
        //endregion

        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setSpecularColor(Color.ORANGE);
        redMaterial.setDiffuseColor(Color.RED);

        box.setMaterial(redMaterial);
        box.getTransforms().addAll(rotateZ, rotateY, rotateX);

        anchorPane.setOnMousePressed((MouseEvent mouseEvent) -> {
            mousePosX = mouseEvent.getSceneX();
            mousePosY = mouseEvent.getSceneY();
        });

        anchorPane.setOnMouseDragged((MouseEvent mouseEvent) -> {
            double dx = (mousePosX - mouseEvent.getSceneX()) ;
            double dy = (mousePosY - mouseEvent.getSceneY());
            if (mouseEvent.isPrimaryButtonDown()) {
                double rotateXAngle = (rotateX.getAngle() - (dy*10 / box.getHeight() * 360) * (Math.PI / 180)) % 360;
                if(rotateXAngle < 0)
                    rotateXAngle += 360;
                rotateX.setAngle(rotateXAngle);

                double temp = 0;
                if(rotateXAngle > 270 || (rotateXAngle < 90 && rotateXAngle > 0)) {
                    if(rotateXAngle > 270) {
                        temp = 360 - rotateXAngle - 90;
                        System.out.println("Temp 1: " + temp);
                        pitchValue = (int) (temp);
                        System.out.println("pitchValue: " + pitchValue);
                        pitchSlider.setValue(pitchValue);
                        updateSliderValues();
                    }
                    else {
                        temp = rotateXAngle - 90;
                        System.out.println("Temp 2: " + temp);
                        pitchValue = (int) ((90-temp)*2)-360;
                        System.out.println("pitchValue: " + pitchValue);
                        pitchSlider.setValue(pitchValue);
                        updateSliderValues();
                    }
                }
                else if(rotateXAngle <= 270 || rotateXAngle >= 90) {
                    if(rotateXAngle <= 270 && rotateXAngle <= 180) {
                        temp = rotateXAngle - 90;
                        System.out.println("Temp 3: " + temp);
                    } else {
                        temp = 360 - rotateXAngle - 90;
                        System.out.println("Temp 4: " + temp);
                    }
                }
                //pitchValue = (int)temp;
                //updateSliderValues();
                //pitchSlider.setValue(temp);


                double rotateYAngle = (rotateY.getAngle() - (dx*10 / box.getWidth() * -360) * (Math.PI / 180)) % 360;
                // roll from 0 to 360
                if (rotateYAngle < 0) {
                    rotateYAngle = rotateYAngle + 360;
                }
                /*if (rotateYAngle >= 360) {
                    rotateYAngle -= 360;
                }*/
                //System.out.println("rotateYAngle: " + rotateYAngle);
                rotateY.setAngle(rotateYAngle);
                rollSlider.setValue(rotateYAngle);

                //rotateZ.setAngle(rotateZ.getAngle() -
                //        (dx / box.getDepth() * 360) * (Math.PI / 180));


                updateSliderValues();
            }
            mousePosX = mouseEvent.getSceneX();
            mousePosY = mouseEvent.getSceneY();
        });
    }

    private void updateSliderValues() {
        // Restrict pitch value to -90 to +90
        if (pitchValue < -90) {
            pitchValue = -180 - pitchValue;
            //yawValue += 180;
            //rollValue += 180;
        } else if (pitchValue > 90) {
            pitchValue = 180 - pitchValue;
            //yawValue += 180;
            //rollValue += 180;
        }

        // yaw from 0 to 360
        if (yawValue < 0) {
            yawValue = yawValue + 360;
            //yawValue = yawValue + 180;
        }
        if (yawValue >= 360) {
            yawValue = yawValue - 360;
            //yawValue = yawValue + 180;
        }

        if (rollValue > 360) {
            rollValue -= 360;
        }
        if(rollValue < 0) {
            rollValue += 360;
        }

        // roll from -180 to + 180
//        if (rollValue >= 180) {
//            rollValue -= 360;
//        }

        updateMagneticFieldData();
        updateAccelerometerData();

        //rotateX.setAngle(pitchValue);
        rotateZ.setAngle(yawValue);
        rotateY.setAngle(rollValue);

        double temp = 0;
//        if(rotateXAngle > 270 || (rotateXAngle < 90 && rotateXAngle > 0)) {
//            if(rotateXAngle > 270) {
//                temp = 360 - rotateXAngle - 90;
//                System.out.println("Temp: " + temp);
//            }
//            else {
//                temp = rotateXAngle - 90;
//                System.out.println("Temp: " + temp);
//            }
//        }
//        else if(rotateXAngle < 270 || rotateXAngle > 90) {
//            if(rotateXAngle < 270 && rotateXAngle < 180) {
//                temp = rotateXAngle - 90;
//                System.out.println("Temp: " + temp);
//            } else {
//                temp = 360 - rotateXAngle - 90;
//                System.out.println("Temp: " + temp);
//            }
//        }


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

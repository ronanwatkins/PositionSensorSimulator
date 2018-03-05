package model;

import utils.Vector;

import java.io.PrintWriter;
import java.util.Random;

public class AccelerometerModel extends SensorModel {

    /**
     * Current read-out value of accelerometer x-component.
     *
     * This value is updated only at the desired updateSensorRate().
     */
    private double mReadAccelx;
    /** Current read-out value of accelerometer y-component. */
    private double mReadAccely;
    /** Current read-out value of accelerometer z-component. */
    private double mReadAccelz;

    /**
     * Internal state value of accelerometer x-component.
     *
     * This value is updated regularly by updateSensorPhysics().
     */
    private double mAccelX;
    /** Internal state value of accelerometer x-component. */
    private double mAccelY;
    /** Internal state value of accelerometer x-component. */
    private double mAccelZ;

    private double aX; // acceleration
    private double aZ;

    private double mAccX; // accelerometer position x on screen
    private double mAccZ; // (DONT confuse with acceleration a!)

    /**
     * Partial read-out value of accelerometer x-component.
     *
     * This partial value is used to calculate the sensor average.
     */
    private double mPartialAccelX;
    /** Partial read-out value of accelerometer y-component. */
    private double mPartialAccelY;
    /** Partial read-out value of accelerometer z-component. */
    private double mPartialAccelZ;

    /** Number of summands in partial sum for accelerometer. */
    private int mPartialAccelN;

    /** Current position on screen. */
    private int mMoveX;
    /** Current position on screen. */
    private int mMoveZ;

    private double mVX; // velocity
    private double mVZ;

    /** Spring constant. */
    private double mSpringK;

    /**
     * Mass of accelerometer test particle.
     *
     * This is set to 1, as only the ratio k/m enters the simulation.
     */
    private double mMass;

    private double mGamma; // damping of spring

    /** Inverse of screen pixel per meter */
    private double mMeterPerPixel;

    /**
     * Gravity constant.
     *
     * This takes the value 9.8 m/s^2.
     * */
    private double mGConstant;

    // Accelerometer
    private double mAccelerometerLimit;
    private boolean mShowAcceleration;

    public AccelerometerModel() {
        mAccX = 0;
        mAccZ = 0;

        mShowAcceleration = true;

        mMoveX = 0;
        mMoveZ = 0;

        mSpringK = 500; // spring constant
        mMass = 1; // mass
        mGamma = 50; // damping
        mMeterPerPixel = 1 / 3000.; // meter per pixel

        mGConstant = 9.80665; // meter per second^2
        mAccelerometerLimit = 10;
    }

    public void setXYZ(Vector vec) {
        mAccelX = vec.x;
        mAccelY = vec.y;
        mAccelZ = vec.z;
    }

    public void limitate(double limit) {
        if (mAccelX > limit) {
            mAccelX = limit;
        }
        if (mAccelX < -limit) {
            mAccelX = -limit;
        }
        if (mAccelY > limit) {
            mAccelY = limit;
        }
        if (mAccelY < -limit) {
            mAccelY = -limit;
        }
        if (mAccelZ > limit) {
            mAccelZ = limit;
        }
        if (mAccelZ < -limit) {
            mAccelZ = -limit;
        }
    }

    @Override
    public void updateSensorReadoutValues() {
        long currentTime = System.currentTimeMillis();
        // Form the average
        if (mAverage) {
            mPartialAccelX += mAccelX;
            mPartialAccelY += mAccelY;
            mPartialAccelZ += mAccelZ;
            mPartialAccelN++;
        }

        // Update
        if (currentTime >= mNextUpdate) {
            mNextUpdate += mUpdateDuration;
            if (mNextUpdate < currentTime) {
                // Don't lag too much behind.
                // If we are too slow, then we are too slow.
                mNextUpdate = currentTime;
            }

            if (mAverage) {
                // form average
                computeAvg();

                // reset average
                resetAvg();
            } else {
                // Only take current value
                mReadAccelx = mAccelX;
                mReadAccely = mAccelY;
                mReadAccelz = mAccelZ;
            }
        }
    }

    public void resetAvg() {
        mPartialAccelX = 0;
        mPartialAccelY = 0;
        mPartialAccelZ = 0;
        mPartialAccelN = 0;
    }

    public void computeAvg() {
        mReadAccelx = mPartialAccelX / mPartialAccelN;
        mReadAccely = mPartialAccelY / mPartialAccelN;
        mReadAccelz = mPartialAccelZ / mPartialAccelN;
    }

    public double getReadAccelerometerX() {
        return mReadAccelx;
    }

    public double getReadAccelerometerY() {
        return mReadAccely;
    }

    public double getReadAccelerometerZ() {
        return mReadAccelz;
    }

    public void refreshAcceleration(double kView, double gammaView, double dt) {
        mSpringK = kView;
        mGamma = gammaView;

        // First calculate the force acting on the
        // sensor test particle, assuming that
        // the accelerometer is mounted by a string:
        // F = - k * x
        double Fx = kView * (mMoveX - mAccX);
        double Fz = gammaView * (mMoveZ - mAccZ);

        // a = F / m
        aX = Fx / mMass;
        aZ = Fz / mMass;

        mVX += aX * dt;
        mVZ += aZ * dt;

        // Now this is the force that tries to adjust
        // the accelerometer back
        // integrate dx/dt = v;
        mAccX += mVX * dt;
        mAccZ += mVZ * dt;

        // We put damping here: We don't want to damp for
        // zero motion with respect to the background,
        // but with respect to the mobile phone:
        mAccX += gammaView * (mMoveX - mAccX) * dt;
        mAccZ += gammaView * (mMoveZ - mAccZ) * dt;
    }

    public double getAx() {
        return aX;
    }

    public double getAz() {
        return aZ;
    }
}

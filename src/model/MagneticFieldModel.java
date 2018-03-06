package model;

import utils.Vector;

public class MagneticFieldModel extends SensorModel {

    /** Current read-out value of compass x-component. */
    private double mReadCompassX;
    /** Current read-out value of compass y-component. */
    private double mReadCompassY;
    /** Current read-out value of compass z-component. */
    private double mReadCompassZ;

    /** Partial read-out value of compass x-component. */
    private double mPartialCompassX;
    /** Partial read-out value of compass y-component. */
    private double mPartialCompassY;
    /** Partial read-out value of compass z-component. */
    private double mPartialCompassZ;
    /** Number of summands in partial sum for compass. */
    private int mPartialCompassN;

    /** Internal state value of compass x-component. */
    private double mCompassX;
    /** Internal state value of compass y-component. */
    private double mCompassY;
    /** Internal state value of compass z-component. */
    private double mCompassZ;

    // Magnetic field
    private double mNorth;
    private double mEast;
    private double mVertical;

    public MagneticFieldModel() {
        mNorth = 22874.1;
        mEast = 5939.5;
        mVertical = 43180.5;
    }

    @Override
    public void updateSensorReadoutValues() {
        long currentTime = System.currentTimeMillis();
        // Form the average
        if (mAverage) {
            mPartialCompassX += mCompassX;
            mPartialCompassY += mCompassY;
            mPartialCompassZ += mCompassZ;
            mPartialCompassN++;
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
                mReadCompassX = mPartialCompassX / mPartialCompassN;
                mReadCompassY = mPartialCompassY / mPartialCompassN;
                mReadCompassZ = mPartialCompassZ / mPartialCompassN;

                // reset average
                mPartialCompassX = 0;
                mPartialCompassY = 0;
                mPartialCompassZ = 0;
                mPartialCompassN = 0;

            } else {
                // Only take current value
                mReadCompassX = mCompassX;
                mReadCompassY = mCompassY;
                mReadCompassZ = mCompassZ;
            }
        }
    }

    public void setCompass(Vector vec) {
        mCompassX = vec.x;
        mCompassY = vec.y;
        mCompassZ = vec.z;
    }

    public double getReadCompassX() {
        return mReadCompassX;
    }

    public double getReadCompassY() {
        return mReadCompassY;
    }

    public double getReadCompassZ() {
        return mReadCompassZ;
    }
}

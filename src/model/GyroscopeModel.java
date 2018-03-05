package model;

public class GyroscopeModel extends SensorModel {
    private static final double EPSILON = 0.10;

    private double mInstantSpeedYaw;
    private double mInstantSpeedRoll;
    private double mInstantSpeedPitch;

    /** Current read-out value of gyroscope. */
    private double mReadAngleSpeedYaw;
    private double mReadAngleSpeedRoll;
    private double mReadAngleSpeedPitch;

    /** Partial read-out value of gyroscope. */
    private float mPartialAngleSpeedYaw;
    private float mPartialAngleSpeedRoll;
    private float mPartialAngleSpeedPitch;

    /** Number of summands in partial sum for gyroscope. */
    private int mPartialAngleSpeedN;

    private double mOldYaw;
    private double mOldRoll;
    private double mOldPitch;

    // rotation radius in meters
    private double mRadiusYaw;
    private double mRadiusRoll;
    private double mRadiusPitch;

    public GyroscopeModel() {
        mRadiusPitch = 0.1;
        mRadiusYaw = 0.15;
        mRadiusRoll = 0.1;
    }

    @Override
    public void updateSensorReadoutValues() {
        long currentTime = System.currentTimeMillis();
        // Form the average
        if (mAverage) {
            mPartialAngleSpeedYaw += mInstantSpeedYaw;
            mPartialAngleSpeedRoll += mInstantSpeedRoll;
            mPartialAngleSpeedPitch += mInstantSpeedPitch;
            mPartialAngleSpeedN++;
        }

        // Update
        if (currentTime >= mNextUpdate) {
            mNextUpdate += mUpdateDuration;
            System.out.println("mUpdateDuration Gyroscope: " + mUpdateDuration);
            if (mNextUpdate < currentTime) {
                // Don't lag too much behind.
                // If we are too slow, then we are too slow.
                mNextUpdate = currentTime;
            }

            if (mAverage) {
                // form average
                mReadAngleSpeedYaw = mPartialAngleSpeedYaw
                        / mPartialAngleSpeedN;
                mReadAngleSpeedRoll = mPartialAngleSpeedRoll
                        / mPartialAngleSpeedN;
                mReadAngleSpeedPitch = mPartialAngleSpeedPitch
                        / mPartialAngleSpeedN;
                // reset average
                mPartialAngleSpeedYaw = 0;
                mPartialAngleSpeedRoll = 0;
                mPartialAngleSpeedPitch = 0;
                mPartialAngleSpeedN = 0;

            } else {
                // Only take current value
                mReadAngleSpeedYaw = mInstantSpeedYaw;
                mReadAngleSpeedRoll = mInstantSpeedRoll;
                mReadAngleSpeedPitch = mInstantSpeedPitch;
            }
        }
    }

    public double getReadGyroscopeYaw() {
        return mReadAngleSpeedYaw;
    }

    public double getReadGyroscopeRoll() {
        return mReadAngleSpeedRoll;
    }

    public double getReadGyroscopePitch() {
        return mReadAngleSpeedPitch;
    }

    public void refreshAngularSpeed(double dt, double crtPitch, double crtYaw,
                                    double crtRoll) {
        // for yaw:
        // dt
        // movedAngleYaw = mCrtYaw - mOldYaw
        // rYaw = rotation radius
        // distanceYaw = movedAngleYaw.toRadians() * rYaw
        // tangentialSpeedYaw = distanceYaw / dt
        // angularSpeedYaw = tangentialSpeedYaw/ rYaw
        if (Math.abs(crtYaw - mOldYaw) > EPSILON) {
            double distanceYawDegrees = crtYaw - mOldYaw;
            double distanceYawRadians = Math.toRadians(distanceYawDegrees)
                    * mRadiusYaw;
            double tangentialSpeed = distanceYawRadians / dt;
            mInstantSpeedYaw = tangentialSpeed / mRadiusYaw;
            mOldYaw += distanceYawDegrees / 20;
        } else {
            mInstantSpeedYaw = 0;
        }

        if (Math.abs(crtPitch - mOldPitch) > EPSILON) {
            double distancePitchDegrees = crtPitch - mOldPitch;
            double distancePitchRadians = Math.toRadians(distancePitchDegrees)
                    * mRadiusPitch;
            double tangentialSpeed = distancePitchRadians / dt;
            mInstantSpeedPitch = tangentialSpeed / mRadiusPitch;
            mOldPitch += distancePitchDegrees / 20;
        } else {
            mInstantSpeedPitch = 0;
        }

        if (Math.abs(crtRoll - mOldRoll) > EPSILON) {
            double distanceRollDegrees = crtRoll - mOldRoll;
            double distanceRollRadians = Math.toRadians(distanceRollDegrees)
                    * mRadiusRoll;
            double tangentialSpeed = distanceRollRadians / dt;
            mInstantSpeedRoll = tangentialSpeed / mRadiusRoll;
            mOldRoll += distanceRollDegrees / 20;
        } else {
            mInstantSpeedRoll = 0;
        }
    }

}

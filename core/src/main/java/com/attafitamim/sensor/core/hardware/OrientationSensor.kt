package com.attafitamim.sensor.core.hardware

import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Handler
import com.attafitamim.sensor.core.base.Sensor
import com.attafitamim.sensor.core.base.Speed
import com.attafitamim.sensor.core.math.matrixMultiplication3x3
import com.attafitamim.sensor.core.providers.OrientationResponseProvider
import java.util.Observable
import java.util.Observer
import java.util.Timer
import java.util.TimerTask
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OrientationSensor(context: Context, orientationDelegate: Delegate) : Sensor, Observer {

    private var accSensor: AccelerometerSensor?
    private var gySensor: GyroscopeSensor?
    private var mgSensor: MagneticSensor?
    private var orientationHandler: Handler? = Handler()
    private var responseProvider: OrientationResponseProvider?
    private var timestamp = 0f
    private var initState = true
    private var fuseTimer: Timer? = null
    private var rotationMatrix = FloatArray(9)
    private var accMagOrientation = FloatArray(3)
    private var fusedOrientation = FloatArray(3)
    private var acceleration = FloatArray(3)
    private var gyro = FloatArray(3)
    private var gyroMatrix = FloatArray(9)
    private var gyroOrientation = FloatArray(3)
    private var magnet = FloatArray(3)

    fun dispose() {
        orientationHandler = null
        rotationMatrix = FloatArray(0)
        accMagOrientation = FloatArray(0)
        fusedOrientation = FloatArray(0)
        acceleration = FloatArray(0)
        gyro = FloatArray(0)
        gyroMatrix = FloatArray(0)
        gyroOrientation = FloatArray(0)
        magnet = FloatArray(0)
        accSensor = null
        gySensor = null
        mgSensor = null
        responseProvider = null
    }

    fun forceDispose() {
        dispose()
        System.gc()
    }

    fun init(azimuthTol: Double, pitchTol: Double, rollTol: Double) {
        responseProvider?.init(azimuthTol, pitchTol, rollTol)
    }

    override val isSupport: Boolean
        get() = accSensor!!.isSupport && mgSensor!!.isSupport

    override fun on(speed: Speed) {
        if (accSensor!!.isSupport) {
            accSensor!!.addObserver(this)
            accSensor!!.on(speed)
        }
        if (gySensor!!.isSupport) {
            gySensor!!.addObserver(this)
            gySensor!!.on(speed)
        }
        if (mgSensor!!.isSupport) {
            mgSensor!!.addObserver(this)
            mgSensor!!.on(speed)
        }

        // time reference: http://webraidmobile.wordpress.com/2010/10/21/how-long-is-sensor_delay_game/
        fuseTimer = Timer()
        when (speed) {
            Speed.NORMAL -> fuseTimer!!.scheduleAtFixedRate(
                CalculateFusedOrientationTask(),
                1, 224
            )

            Speed.UI -> fuseTimer!!.scheduleAtFixedRate(
                CalculateFusedOrientationTask(),
                1, 77
            )

            Speed.GAME -> fuseTimer!!.scheduleAtFixedRate(
                CalculateFusedOrientationTask(),
                1, 37
            )

            Speed.FASTEST -> fuseTimer!!.scheduleAtFixedRate(
                CalculateFusedOrientationTask(),
                1, 16
            )
        }
    }

    override fun off() {
        fuseTimer!!.cancel()
        if (accSensor!!.isSupport) {
            accSensor!!.deleteObserver(this)
            accSensor!!.off()
        }
        if (gySensor!!.isSupport) {
            gySensor!!.deleteObserver(this)
            gySensor!!.off()
        }
        if (mgSensor!!.isSupport) {
            mgSensor!!.deleteObserver(this)
            mgSensor!!.off()
        }
    }

    override val maximumRange: Float
        get() = 0f

    @Deprecated("Deprecated in Java")
    override fun update(observable: Observable, o: Any?) {
        when (observable) {
            is AccelerometerSensor -> {
                val values = accSensor?.event?.values ?: return

                System.arraycopy(
                    values,
                    0,
                    acceleration,
                    0,
                    3
                )

                calculateAccMagOrientation()
            }

            is GyroscopeSensor -> {
                val event = gySensor?.event ?: return
                gyroFunction(event)
            }

            is MagneticSensor -> {
                val values = mgSensor?.event?.values ?: return

                System.arraycopy(
                    values,
                    0,
                    magnet,
                    0,
                    3
                )
            }
        }
    }

    private fun updateValues() {
        responseProvider?.dispatcher(fusedOrientation)
    }

    var updateOrientationValueTask = Runnable { updateValues() }

    init {
        accSensor = AccelerometerSensor(context)
        gySensor = GyroscopeSensor(context)
        mgSensor = MagneticSensor(context)
        gyroOrientation[0] = 0.0f
        gyroOrientation[1] = 0.0f
        gyroOrientation[2] = 0.0f

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f
        gyroMatrix[1] = 0.0f
        gyroMatrix[2] = 0.0f
        gyroMatrix[3] = 0.0f
        gyroMatrix[4] = 1.0f
        gyroMatrix[5] = 0.0f
        gyroMatrix[6] = 0.0f
        gyroMatrix[7] = 0.0f
        gyroMatrix[8] = 1.0f
        responseProvider = OrientationResponseProvider(orientationDelegate)
    }

    internal inner class CalculateFusedOrientationTask : TimerTask() {
        override fun run() {
            val oneMinusCoefficient = 1.0f - FILTER_COEFFICIENT

            /*
             * Fix for 179� <--> -179� transition problem:
             * Check whether one of the two OrientationSensor angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360� (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360� from the result
             * if it is greater than 180�. This stabilizes the output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] =
                    (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoefficient * accMagOrientation[0]).toFloat()
                fusedOrientation[0] -= if (fusedOrientation[0] > Math.PI) (2.0 * Math.PI).toFloat() else 0.toFloat()
            } else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] =
                    (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoefficient * (accMagOrientation[0] + 2.0 * Math.PI)).toFloat()
                fusedOrientation[0] -= if (fusedOrientation[0] > Math.PI) (2.0 * Math.PI).toFloat() else 0.toFloat()
            } else {
                fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoefficient * accMagOrientation[0]
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] =
                    (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoefficient * accMagOrientation[1]).toFloat()
                fusedOrientation[1] -= if (fusedOrientation[1] > Math.PI) (2.0 * Math.PI).toFloat() else 0.toFloat()
            } else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] =
                    (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoefficient * (accMagOrientation[1] + 2.0 * Math.PI)).toFloat()
                fusedOrientation[1] -= if (fusedOrientation[1] > Math.PI) (2.0 * Math.PI).toFloat() else 0.toFloat()
            } else {
                fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoefficient * accMagOrientation[1]
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] =
                    (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoefficient * accMagOrientation[2]).toFloat()
                fusedOrientation[2] -= if (fusedOrientation[2] > Math.PI) (2.0 * Math.PI).toFloat() else 0f
            } else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] =
                    (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoefficient * (accMagOrientation[2] + 2.0 * Math.PI)).toFloat()
                fusedOrientation[2] -= if (fusedOrientation[2] > Math.PI) (2.0 * Math.PI).toFloat() else 0f
            } else {
                fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoefficient * accMagOrientation[2]
            }

            // overwrite gyro matrix and OrientationSensor with fused OrientationSensor
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation)
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3)

            // update sensor output
            orientationHandler!!.post(updateOrientationValueTask)
        }
    }

    private fun calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation)
        }
    }

    // This function performs the integration of the GyroscopeSensor data.
    // It writes the GyroscopeSensor based OrientationSensor into gyroOrientation.
    private fun gyroFunction(event: SensorEvent) {
        if (accMagOrientation.isEmpty()) return

        if (initState) {
            val initMatrix = getRotationMatrixFromOrientation(accMagOrientation)
            val test = FloatArray(3)
            SensorManager.getOrientation(initMatrix, test)
            gyroMatrix = matrixMultiplication3x3(gyroMatrix, initMatrix)
            initState = false
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        val deltaVector = FloatArray(4)
        if (timestamp != 0f) {
            val dT: Float = (event.timestamp - timestamp) * NS2S
            System.arraycopy(event.values, 0, gyro, 0, 3)
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f)
        }
        timestamp = event.timestamp.toFloat()
        val deltaMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector)
        gyroMatrix = matrixMultiplication3x3(gyroMatrix, deltaMatrix)
        SensorManager.getOrientation(gyroMatrix, gyroOrientation)
    }

    private fun getRotationMatrixFromOrientation(o: FloatArray?): FloatArray {
        val xM = FloatArray(9)
        val yM = FloatArray(9)
        val zM = FloatArray(9)
        val sinX = sin(o!![1].toDouble()).toFloat()
        val cosX = cos(o[1].toDouble()).toFloat()
        val sinY = sin(o[2].toDouble()).toFloat()
        val cosY = cos(o[2].toDouble()).toFloat()
        val sinZ = sin(o[0].toDouble()).toFloat()
        val cosZ = cos(o[0].toDouble()).toFloat()

        // rotation about x-axis (pitch)
        xM[0] = 1.0f
        xM[1] = 0.0f
        xM[2] = 0.0f
        xM[3] = 0.0f
        xM[4] = cosX
        xM[5] = sinX
        xM[6] = 0.0f
        xM[7] = -sinX
        xM[8] = cosX

        // rotation about y-axis (roll)
        yM[0] = cosY
        yM[1] = 0.0f
        yM[2] = sinY
        yM[3] = 0.0f
        yM[4] = 1.0f
        yM[5] = 0.0f
        yM[6] = -sinY
        yM[7] = 0.0f
        yM[8] = cosY

        // rotation about z-axis (azimuth)
        zM[0] = cosZ
        zM[1] = sinZ
        zM[2] = 0.0f
        zM[3] = -sinZ
        zM[4] = cosZ
        zM[5] = 0.0f
        zM[6] = 0.0f
        zM[7] = 0.0f
        zM[8] = 1.0f

        // rotation order is y, x, z (roll, pitch, azimuth)
        var resultMatrix: FloatArray = matrixMultiplication3x3(xM, yM)
        resultMatrix = matrixMultiplication3x3(zM, resultMatrix)
        return resultMatrix
    }

    private fun getRotationVectorFromGyro(
        gyroValues: FloatArray?,
        deltaRotationVector: FloatArray,
        timeFactor: Float
    ) {
        val normValues = FloatArray(3)

        // Calculate the sample angular speed
        val angularSpeed = (
                gyroValues!![0] *
                gyroValues[0] +
                gyroValues[1] *
                gyroValues[1] +
                gyroValues[2] *
                gyroValues[2]
        ).toDouble()

        val omegaMagnitude = sqrt(angularSpeed).toFloat()

        // Normalize the rotation vector
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude
            normValues[1] = gyroValues[1] / omegaMagnitude
            normValues[2] = gyroValues[2] / omegaMagnitude
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        val thetaOverTwo = omegaMagnitude * timeFactor
        val sinThetaOverTwo = sin(thetaOverTwo.toDouble()).toFloat()
        val cosThetaOverTwo = cos(thetaOverTwo.toDouble()).toFloat()
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0]
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1]
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2]
        deltaRotationVector[3] = cosThetaOverTwo
    }

    fun interface Delegate {
        fun onOrientation(azimuth: Double, pitch: Double, roll: Double)
    }

    companion object {
        const val EPSILON = 0.000000001f
        private const val NS2S = 1.0f / 1000000000.0f
        const val TIME_CONSTANT = 30
        const val FILTER_COEFFICIENT = 0.98f
    }
}

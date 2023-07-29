package de.tub.affinity3.android.classes

import com.google.android.gms.location.DetectedActivity
import de.tub.affinity3.android.clients.NearbyConnectionsClientV2
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService
import de.tub.affinity3.android.clients.ContextRecognitionClient
import java.util.concurrent.TimeUnit
import timber.log.Timber

class DiscoveryDecider(
    val serviceConfig: AdvertiseAndDiscoveryService.ServiceType.ContextService,
    val contextRecognitionClient: ContextRecognitionClient,
    val nearbyClient: NearbyConnectionsClientV2,
    val timeProvider: TimeProvider = defaultTimeProvider
) {
    private var discoveryStartedTime: Long = -1L
    private var discoveryStoppedTime: Long = -1L

    /**
     * FORCE_START: Discovery will be started no matter what and time resetted
     * RESUME_START: Discovery will be started when not running or resumed when running but can be stopped
     * STOP: Discovery should be stopped (overruled by FORCE_START)
     * SOFT_STOP: Discovery can be stopped (overruled by both starts)
     * NONE: Nothing should happen
     */
    private enum class DiscoveryStatus {
        FORCE_START, RESUME_START, SOFT_STOP, STOP, NONE
    }

    fun reset() {
        this.discoveryStartedTime = -1L
        this.discoveryStoppedTime = -1L
        this.nearbyClient.stop()
    }

    fun toggleAdvertiseAndDiscovery(): Boolean {
        var currentStatus = DiscoveryStatus.NONE
        Timber.d("\n*******************************************")
        Timber.d("Config: $serviceConfig")
        Timber.d("discoveryStartedTime: $discoveryStartedTime")
        Timber.d("discoveryStoppedTime: $discoveryStoppedTime")
        val runningTime = if (isNotRunning()) {
            0
        } else {
            TimeUnit.MILLISECONDS.toSeconds(timeProvider.currentTimeMillis() - this.discoveryStartedTime)
        }
        val pausedTime = if (isRunning()) {
            0
        } else {
            TimeUnit.MILLISECONDS.toSeconds(timeProvider.currentTimeMillis() - this.discoveryStoppedTime)
        }
        Timber.d("runningFor: ${runningTime}s, pausedFor: ${pausedTime}s")
        currentStatus = combinedStatus(currentStatus, resumeDiscoveryWhenHasConnectedClients())
        Timber.d("Status after ConnectedClients: $currentStatus")
        currentStatus = combinedStatus(currentStatus, stopWhenRunningLongEnough())
        Timber.d("Status after stop when running long time: $currentStatus")
        currentStatus = combinedStatus(currentStatus, startOrResumeByTime())
        Timber.d("Status after start when timeout: $currentStatus")
        currentStatus = combinedStatus(currentStatus, startDiscoveryWhenContextIsRelevant())
        Timber.d("Status after context: $currentStatus")
        currentStatus = combinedStatus(currentStatus, stopOrResumeWithSleepingTime(currentStatus))
        Timber.d("Status after put to sleep: $currentStatus")
        Timber.d(" *******************************************\n")

        return when (currentStatus) {
            DiscoveryStatus.FORCE_START -> {
                forceStart()
                true
            }
            DiscoveryStatus.RESUME_START -> {
                resumeStart()
                true
            }
            DiscoveryStatus.STOP -> {
                stop()
                false
            }
            DiscoveryStatus.SOFT_STOP -> {
                stop()
                false
            }
            DiscoveryStatus.NONE -> {
                isRunning()
            }
        }
    }

    private fun combinedStatus(
        statusOne: DiscoveryStatus,
        statusTwo: DiscoveryStatus
    ): DiscoveryStatus {
        return when {
            statusOne == statusTwo -> statusOne
            statusOne == DiscoveryStatus.FORCE_START || statusTwo == DiscoveryStatus.FORCE_START -> DiscoveryStatus.FORCE_START
            statusOne == DiscoveryStatus.STOP || statusTwo == DiscoveryStatus.STOP -> DiscoveryStatus.STOP
            statusOne == DiscoveryStatus.RESUME_START || statusTwo == DiscoveryStatus.RESUME_START -> DiscoveryStatus.RESUME_START
            statusOne == DiscoveryStatus.SOFT_STOP || statusTwo == DiscoveryStatus.SOFT_STOP -> DiscoveryStatus.SOFT_STOP
            else -> DiscoveryStatus.NONE
        }
    }

    private fun stop() {
        this.nearbyClient.pause()
        if (isRunning()) {
            this.discoveryStoppedTime = timeProvider.currentTimeMillis()
        }
    }

    /**
     * Will start the Discovery process, but not reset the start time if it was already running
     *
     * This allows for the process to be stopped when it has been running for a longer time without
     * any condition changes that would force a start of the process
     */
    private fun resumeStart() {
        this.nearbyClient.startOrResume()
        if (isNotRunning() || hasNeverBeenRunning()) {
            this.discoveryStartedTime = timeProvider.currentTimeMillis()
        }
    }

    /**
     * Will start the Discovery process and reset the start time
     */
    private fun forceStart() {
        this.nearbyClient.startOrResume()
        this.discoveryStartedTime = timeProvider.currentTimeMillis()
    }

    /**
     * Resumes the Discovery when there are still clients connected
     *
     * returns whether or not the Discovery should continue to run
     */
    private fun resumeDiscoveryWhenHasConnectedClients(): DiscoveryStatus {
        val shouldResume = this.nearbyClient.getNumberOfConnectedClients() > 0
        return if (shouldResume) {
            DiscoveryStatus.FORCE_START
        } else {
            DiscoveryStatus.NONE
        }
    }

    /**
     * Starts Discovery when the context is relevant
     *
     * returns whether or not the Discovery was started
     */
    private fun startDiscoveryWhenContextIsRelevant(): DiscoveryStatus {
        return when (this.contextRecognitionClient.currentActivity?.type) {
            null -> DiscoveryStatus.NONE
            DetectedActivity.IN_VEHICLE -> DiscoveryStatus.FORCE_START
            DetectedActivity.ON_BICYCLE -> DiscoveryStatus.NONE
            DetectedActivity.ON_FOOT -> DiscoveryStatus.NONE
            DetectedActivity.STILL -> DiscoveryStatus.RESUME_START
            DetectedActivity.TILTING -> DiscoveryStatus.RESUME_START
            DetectedActivity.WALKING -> DiscoveryStatus.NONE
            DetectedActivity.RUNNING -> DiscoveryStatus.NONE
            else -> DiscoveryStatus.NONE
        }
    }

    private fun startOrResumeByTime(): DiscoveryStatus {
        val finishedMinimumRunTime = hasBeenRunningForMinTime()
        val shouldRunAgain = shouldRunAgain()
        return if ((finishedMinimumRunTime.not() && isRunning()) || shouldRunAgain) {
            DiscoveryStatus.RESUME_START
        } else {
            DiscoveryStatus.NONE
        }
    }

    private fun stopOrResumeWithSleepingTime(statusSoFar: DiscoveryStatus): DiscoveryStatus {
        if (statusSoFar != DiscoveryStatus.RESUME_START) {
            return statusSoFar
        }
        val finishedMaxRuntime = hasBeenRunningForMaxTime()
        val shouldRunAgain = passedSleepingTime()
        return if (isRunning()) {
            if (finishedMaxRuntime) {
                DiscoveryStatus.STOP
            } else {
                DiscoveryStatus.RESUME_START
            }
        } else {
            if (shouldRunAgain) {
                DiscoveryStatus.RESUME_START
            } else {
                DiscoveryStatus.STOP
            }
        }
    }

    private fun stopWhenRunningLongEnough(): DiscoveryStatus {
        return if (hasBeenRunningForMinTime()) {
            DiscoveryStatus.SOFT_STOP
        } else {
            DiscoveryStatus.NONE
        }
    }

    private fun isRunning(): Boolean =
        this.discoveryStartedTime >= 0L && this.discoveryStoppedTime < this.discoveryStartedTime

    private fun isNotRunning(): Boolean = isRunning().not()

    private fun hasNeverBeenRunning() = this.discoveryStartedTime < 0L

    private fun hasBeenRunningForMinTime(): Boolean {
        return (timeProvider.currentTimeMillis() - this.discoveryStartedTime) >= this.serviceConfig.minRuntimeInSeconds * 1000 && isRunning()
    }

    private fun hasBeenRunningForMaxTime(): Boolean {
        val runningFor = (timeProvider.currentTimeMillis() - this.discoveryStartedTime)
        return runningFor >= this.serviceConfig.maxRuntimeInSeconds * 1000 && isRunning()
    }

    private fun passedSleepingTime(): Boolean {
        return (this.discoveryStoppedTime >= 0L && (timeProvider.currentTimeMillis() - this.discoveryStoppedTime) >= this.serviceConfig.sleepTimeInSeconds * 1000) ||
                discoveryStartedTime < 0L
    }

    private fun shouldRunAgain(): Boolean {
        return (this.discoveryStoppedTime >= 0L && (timeProvider.currentTimeMillis() - this.discoveryStoppedTime) >= this.serviceConfig.maxTimeoutInSeconds * 1000) ||
                discoveryStartedTime < 0L
    }

    companion object {
        val defaultTimeProvider = object : TimeProvider {
            override fun currentTimeMillis(): Long {
                return System.currentTimeMillis()
            }
        }
    }

    interface TimeProvider {
        fun currentTimeMillis(): Long
    }
}

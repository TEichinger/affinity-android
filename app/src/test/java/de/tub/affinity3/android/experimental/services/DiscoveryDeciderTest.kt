package de.tub.affinity3.android.experimental.services

import com.google.android.gms.location.DetectedActivity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import de.tub.affinity3.android.classes.DiscoveryDecider
import de.tub.affinity3.android.clients.ContextRecognitionClient
import de.tub.affinity3.android.clients.NearbyConnectionsClientV2
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService
import org.junit.Before
import org.junit.Test

class DiscoveryDeciderTest {

    lateinit var decider: DiscoveryDecider
    val contextRecognitionClient: ContextRecognitionClient = mock()
    val nearbyClient: NearbyConnectionsClientV2 = mock()

    val activityAlwaysOn = DetectedActivity(
        DetectedActivity.IN_VEHICLE,
        100
    )
    val activityCanContinue = DetectedActivity(
        DetectedActivity.STILL,
        100
    )
    val activityUnrelevant = DetectedActivity(
        DetectedActivity.RUNNING,
        100
    )

    class TestTimeProvider : DiscoveryDecider.TimeProvider {
        var currentTime: Long = 0L

        fun advanceTimeBy(timeInMillis: Long) {
            currentTime += timeInMillis
        }

        fun reset() {
            currentTime = 0L
        }

        override fun currentTimeMillis(): Long {
            return currentTime
        }
    }

    val timeProvider = TestTimeProvider()

    @Before
    fun setUp() {
        timeProvider.reset()
    }

    @Test
    fun `starts discovery`() {
        val config = AdvertiseAndDiscoveryService.ServiceType.ContextService(
            minRuntimeInSeconds = 10,
            maxRuntimeInSeconds = 30,
            maxTimeoutInSeconds = 60,
            sleepTimeInSeconds = 20
        )
        decider = DiscoveryDecider(
            serviceConfig = config,
            nearbyClient = nearbyClient,
            contextRecognitionClient = contextRecognitionClient,
            timeProvider = timeProvider
        )
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityUnrelevant)
        whenever(nearbyClient.hasConnectedClients()).thenReturn(false)

        decider.toggleAdvertiseAndDiscovery()

        verify(nearbyClient, times(1)).startOrResume()
    }

    @Test
    fun `discovery stopped when passed minimum required time for single run`() {
        val config = AdvertiseAndDiscoveryService.ServiceType.ContextService(
            minRuntimeInSeconds = 10,
            maxRuntimeInSeconds = 30,
            maxTimeoutInSeconds = 60,
            sleepTimeInSeconds = 20
        )
        decider = DiscoveryDecider(
            serviceConfig = config,
            nearbyClient = nearbyClient,
            contextRecognitionClient = contextRecognitionClient,
            timeProvider = timeProvider
        )
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityUnrelevant)
        whenever(nearbyClient.hasConnectedClients()).thenReturn(false)

        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(11 * 1000)
        decider.toggleAdvertiseAndDiscovery()

        verify(nearbyClient, times(1)).startOrResume()
        verify(nearbyClient, times(1)).pause()
    }

    @Test
    fun `discovery restarted after max timeout`() {
        val config = AdvertiseAndDiscoveryService.ServiceType.ContextService(
            minRuntimeInSeconds = 10,
            maxRuntimeInSeconds = 30,
            maxTimeoutInSeconds = 60,
            sleepTimeInSeconds = 20
        )
        decider = DiscoveryDecider(
            serviceConfig = config,
            nearbyClient = nearbyClient,
            contextRecognitionClient = contextRecognitionClient,
            timeProvider = timeProvider
        )
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityUnrelevant)
        whenever(nearbyClient.hasConnectedClients()).thenReturn(false)

        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(11 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(60 * 1000)
        decider.toggleAdvertiseAndDiscovery()

        verify(nearbyClient, times(2)).startOrResume()
        verify(nearbyClient, times(1)).pause()
    }

    /*
    @Test
    fun `discovery continues when has connected clients`() {
        val config = AdvertiseAndDiscoveryService.ServiceType.ContextService(
            minRuntimeInSeconds = 10,
            maxRuntimeInSeconds = 20,
            maxTimeoutInSeconds = 60
        )
        decider = DiscoveryDecider(
            serviceConfig = config,
            nearbyClient = nearbyClient,
            contextRecognitionClient = contextRecognitionClient,
            timeProvider = timeProvider
        )
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityUnrelevant)
        whenever(nearbyClient.hasConnectedClients()).thenReturn(true)

        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(11 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(60 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(60 * 1000)

        verify(nearbyClient, times(3)).startOrResume()
        verify(nearbyClient, never()).pause()
    }
    */
    /* 
    @Test
    fun `discovery stops when no more connected clients`() {
        val config = AdvertiseAndDiscoveryService.ServiceType.ContextService(
            minRuntimeInSeconds = 10,
            maxRuntimeInSeconds = 30,
            maxTimeoutInSeconds = 60,
            sleepTimeInSeconds = 20
        )
        decider = DiscoveryDecider(
            serviceConfig = config,
            nearbyClient = nearbyClient,
            contextRecognitionClient = contextRecognitionClient,
            timeProvider = timeProvider
        )
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityUnrelevant)
        whenever(nearbyClient.hasConnectedClients()).thenReturn(true)

        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(11 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(60 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        whenever(nearbyClient.hasConnectedClients()).thenReturn(false)
        timeProvider.advanceTimeBy(60 * 1000)
        decider.toggleAdvertiseAndDiscovery()

        verify(nearbyClient, times(3)).startOrResume()
        verify(nearbyClient, times(1)).pause()
    }
    */

    @Test
    fun `discovery continues when has relevant context`() {
        val config = AdvertiseAndDiscoveryService.ServiceType.ContextService(
            minRuntimeInSeconds = 10,
            maxRuntimeInSeconds = 30,
            maxTimeoutInSeconds = 60,
            sleepTimeInSeconds = 20
        )
        decider = DiscoveryDecider(
            serviceConfig = config,
            nearbyClient = nearbyClient,
            contextRecognitionClient = contextRecognitionClient,
            timeProvider = timeProvider
        )
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityAlwaysOn)
        whenever(nearbyClient.hasConnectedClients()).thenReturn(false)

        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(11 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(60 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(60 * 1000)
        decider.toggleAdvertiseAndDiscovery()
        timeProvider.advanceTimeBy(60 * 1000)

        verify(nearbyClient, times(4)).startOrResume()
        verify(nearbyClient, never()).pause()
    }

    @Test
    fun `discovery has sleeping phases when in specific context that does not require full discovery all the time`() {
        val config = AdvertiseAndDiscoveryService.ServiceType.ContextService(
            minRuntimeInSeconds = 10,
            maxRuntimeInSeconds = 30,
            maxTimeoutInSeconds = 60,
            sleepTimeInSeconds = 20
        )
        decider = DiscoveryDecider(
            serviceConfig = config,
            nearbyClient = nearbyClient,
            contextRecognitionClient = contextRecognitionClient,
            timeProvider = timeProvider
        )
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityCanContinue)
        whenever(nearbyClient.hasConnectedClients()).thenReturn(false)

        decider.toggleAdvertiseAndDiscovery() // start
        timeProvider.advanceTimeBy(25 * 1000) // - 25s
        decider.toggleAdvertiseAndDiscovery() // resume
        timeProvider.advanceTimeBy(10 * 1000) // - 35s
        decider.toggleAdvertiseAndDiscovery() // stop
        timeProvider.advanceTimeBy(10 * 1000) // - 45s
        decider.toggleAdvertiseAndDiscovery() // stop
        timeProvider.advanceTimeBy(10 * 1000) // - 55s
        decider.toggleAdvertiseAndDiscovery() // start
        timeProvider.advanceTimeBy(15 * 1000) // - 70s
        decider.toggleAdvertiseAndDiscovery() // resume
        whenever(contextRecognitionClient.currentActivity).thenReturn(activityUnrelevant)
        timeProvider.advanceTimeBy(5 * 1000) // - 75s
        decider.toggleAdvertiseAndDiscovery() // stop
        timeProvider.advanceTimeBy(30 * 1000) // - 105s
        decider.toggleAdvertiseAndDiscovery() // nothing
        timeProvider.advanceTimeBy(25 * 1000) // - 130s
        decider.toggleAdvertiseAndDiscovery() // nothing
        timeProvider.advanceTimeBy(10 * 1000) // - 140s
        decider.toggleAdvertiseAndDiscovery() // resume

        verify(nearbyClient, times(5)).startOrResume()
        verify(nearbyClient, times(3)).pause()
    }
}

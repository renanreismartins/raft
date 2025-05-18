package statemachine

import org.example.Network
import org.example.Source
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.TimeMachine2
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeaderTest {

    @Test
    fun `Signals Heartbeat timeout`() {
        val network = Network()
        network.clock = 4

        val leader = StateMachine(
            address = Source("127.0.0.1", 9001),
            name = "NodeA",
            network = network,
            peers = listOf(),
            term = 1,
            role = Role.LEADER,
            termStartedAt = 4,
            sentHeartbeatAt = 2
        )

        assertTrue(leader.hasHeartbeatTimedOut())
    }

    @Test
    fun `On a Heartbeat Timeout (2 ticks), replicate the Log and reset the Heartbeat clock`() {
        // Given
        val network = Network()

        val leader = StateMachine(
            address = Source("127.0.0.1", 9001),
            name = "NodeA",
            network = network,
            peers = listOf(),
            term = 1,
            role = Role.LEADER,
            termStartedAt = 4,
            sentHeartbeatAt = 0
        )

        // When time passes and the Heartbeat timeout has not been reached
        val timeMachine = TimeMachine2(network, leader).tick()
        val (_, leaderWithoutTimeout) = timeMachine

        // Then
        assertFalse(leaderWithoutTimeout.hasHeartbeatTimedOut())

        // Leader reaches Heartbeat timeout then it resets Heartbeat clock
        val (_, leaderWithTimeout) = timeMachine.tick()
        assertEquals(2, leaderWithTimeout.sentHeartbeatAt)

        //TODO check log replication
    }
}
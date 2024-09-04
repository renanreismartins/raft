package usecases

import org.example.AppendEntryResponse
import org.example.ClientCommand
import org.example.Destination
import org.example.Follower
import org.example.Leader
import org.example.Network
import org.example.Source
import org.example.TimeMachine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StateReplication {
    @Test
    fun `A leader replicates its state for all the followers`() {
        val network = Network()

        val follower1Address = Source("127.0.0.1", 9002)
        val follower1 =
            Follower(
                follower1Address,
                "FollowerWithoutCommunication",
                network = network,
                peers = listOf(),
            )

        val follower2Address = Source("127.0.0.1", 9003)
        val follower2 =
            Follower(
                follower2Address,
                "FollowerWithCommunication",
                network = network,
                peers = listOf(),
            )

        val leaderAddress = Source("127.0.0.1", 9001)
        val leader =
            Leader(
                leaderAddress,
                "Leader",
                peers = listOf(Destination.from(follower2Address), Destination.from(follower1Address)),
                network = network,
            )

        network.add(ClientCommand(Source("127.0.0.1", 9999), Destination.from(leaderAddress), 1, "client command"))

        val timeMachine = TimeMachine(network, leader, follower1, follower2).tick(2)
        val (_, newLeader, newFollower1, newFollower2) = timeMachine

        assertEquals(1, newLeader.commitIndex)
        assertEquals(1, newFollower1.messages.received.size)
        assertEquals(1, newFollower1.log.size)

        // TODO: TimeMachine is immutable, which means we can't keep ticking the initial instance, which is inconvenient for big tests. Refactor.
        val (_, newLeader2, _, _) = timeMachine.tick()

        /**
         * Leader should have received 3 messages
         *  1. ClientCommand
         *  2 & 3. AppendStateResponse from both Followers
         */
        assertEquals(3, newLeader2.messages.received.size)
        assertEquals(2, newLeader2.messages.received.filter { it.message is AppendEntryResponse }.size)
    }
}

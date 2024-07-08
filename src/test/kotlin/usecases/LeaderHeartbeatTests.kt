package usecases

import org.example.Destination
import org.example.Follower
import org.example.Heartbeat
import org.example.Leader
import org.example.Network
import org.example.ReceivedMessage
import org.example.SentMessage
import org.example.Source
import org.example.TimeMachine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LeaderHeartbeatTests {

    @Test
    fun `A leader will send a heartbeat to all nodes it hasn't communicated with in the heartbeatTimeout period`() {
        // Given

        // Network starts on tick 1 to simulate previous communication
        val network = Network()

        val leaderAddress = Source("127.0.0.1", 9001)
        val followerWithoutCommunicationAddress = Source("127.0.0.1", 9002)
        val followerWithCommunicationAddress = Source("127.0.0.1", 9003)

        val initialLeader = Leader(
            leaderAddress,
            "Leader",
            peers = listOf(Destination.from(followerWithCommunicationAddress), Destination.from(followerWithoutCommunicationAddress)),
            network = network,
            sent = listOf(SentMessage(Heartbeat(leaderAddress, Destination.from(followerWithCommunicationAddress), "0"), 0))
        )

        val followerWithCommunication = Follower(
            followerWithCommunicationAddress,
            "FollowerWithCommunication",
            network = network,
            peers = listOf(),
            received = listOf(ReceivedMessage(Heartbeat(leaderAddress, Destination.from(followerWithCommunicationAddress), "0"), 1))
        )

        val followerWithoutCommunication = Follower(
            followerWithoutCommunicationAddress,
             "FollowerWithoutCommunication",
            network = network,
            peers = listOf(),
        )

        val timeMachine = TimeMachine(network, initialLeader, followerWithCommunication, followerWithoutCommunication)

        val (_, leader, followerOldHeartbeat_, followerNewHeartbeat) = timeMachine.tick()

        assertEquals(2, leader.sent.size)

        // TODO check received messages on followers
    }

}
package statemachine

import org.example.Config
import org.example.Destination
import org.example.Heartbeat
import org.example.Network
import org.example.ReceivedMessage
import org.example.RequestForVotes
import org.example.SentMessage
import org.example.Source
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.TimeMachine2
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FollowerTests {
    @Test
    fun `Follower becomes a Candidate if it does not receive a message before the election timeout (5 ticks)`() {
        val network = Network()

        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val peerAddress = Destination("127.0.0.1", 9002)
        val follower =
            StateMachine(followerAddress, "NodeA", network = network, peers = listOf(peerAddress), config = Config(electionTimeout = 5))

        // First election times out
        val timeMachine = TimeMachine2(network, follower).tick(5)
        val (_, candidate) = timeMachine

        // Then
        assertEquals(Role.CANDIDATE, candidate.role)
        assertEquals(1, candidate.term)
        assertEquals(setOf(followerAddress), candidate.votesReceived)
        assertEquals(
            listOf(SentMessage(
                RequestForVotes(
                    followerAddress,
                    peerAddress,
                    1,
                    "REQUEST FOR VOTES",
                    0),
                5)
            ),
            candidate.messages.sent)

    }

    @Test
    fun `Follower becomes a Candidate if it does not receive a message before the heartbeat timeout (3 ticks)`() {
        // In this scenario the Follower already received a message, but it too long
        // (more time than the HeartBeatTimeout) to receive a second message, triggering
        // an election

        val network = Network()

        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val follower =
            StateMachine(followerAddress, "NodeA", network = network, peers = emptyList(), config = Config(heartbeatTimeout = 3))
                .add(ReceivedMessage(Heartbeat(Source("127.0.0.1", 9002), Destination.from(followerAddress), 0, ""), 1))

        // First HeartBeatTimeOut
        val timeMachine = TimeMachine2(network, follower).tick(3)
        val (_, remainedFollower) = timeMachine

        // Then Follower continues to be a Follower given the previous Message
        assertEquals(Role.FOLLOWER, remainedFollower.role)

        // When HeartBeatTimeOut happens and no new messages have arrived in the period
        val (_, candidate) = timeMachine.tick(3)

        // Then Follower became a candidate
        assertEquals(Role.CANDIDATE, candidate.role)
    }

    @Test
    fun `Follower should not become a Candidate it receives a message before the heartbeat timeout (3 ticks)`() {
        val network = Network()

        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val follower =
            StateMachine(followerAddress, "NodeA", network = network, peers = emptyList(), config = Config(heartbeatTimeout = 3))
                .add(ReceivedMessage(Heartbeat(Source("127.0.0.1", 9002), Destination.from(followerAddress), 0, ""), 1))

        // First HeartBeatTimeOut
        val timeMachine = TimeMachine2(network, follower).tick(3)
        val (_, remainedFollower) = timeMachine

        // Then
        assertEquals(Role.FOLLOWER, remainedFollower.role)
    }
}

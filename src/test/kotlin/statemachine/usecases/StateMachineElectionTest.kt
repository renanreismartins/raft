package statemachine.usecases

import org.example.AppendEntries
import org.example.AppendEntriesResponse
import org.example.Config
import org.example.Destination
import org.example.Network
import org.example.Source
import org.example.VoteFromFollower
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.TimeMachine2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StateMachineElectionTest {
    @Test
    fun `A Request for Votes is sent when a Follower becomes a Candidate (election timeout in 3 ticks), the Follower receives the request and sends its vote to the Candidate that becomes a Leader`() {
        // Given
        val network = Network()

        val willPromoteAddress = Source("127.0.0.1", 9001)
        val remainsFollowerAddress = Source("127.0.0.1", 9002)

        val willPromote =
            StateMachine(
                willPromoteAddress,
                "NodeA",
                network = network,
                peers = listOf(Destination.from(remainsFollowerAddress)),
                config = Config(20),
            )

        val remainsFollower =
            StateMachine(
                remainsFollowerAddress,
                "NodeB",
                network = network,
                peers = Destination.from(listOf(willPromoteAddress)),
                config = Config(30),
            )

        // When election times out
        val timeMachine = TimeMachine2(network, willPromote, remainsFollower).tick(21)
        val (_, becameCandidate, remainedFollower) = timeMachine

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertEquals(Role.CANDIDATE, becameCandidate.role)

        //Receives a vote from himself
        assertEquals(setOf(willPromoteAddress), becameCandidate.votesReceived)

        // Follower receives a request for vote
        assertEquals(Role.FOLLOWER, remainedFollower.role)
        assertEquals(
            "REQUEST FOR VOTES",
            remainedFollower.received().first().message.content
        )

        // When the Nodes Vote for the Candidate
        val (_, leader, followerAfterVote) = timeMachine.tick()

        // Candidate receives the vote and as it has the quorum, it is promoted to leader
        assertEquals(
            setOf(willPromoteAddress, remainsFollowerAddress),
            leader.votesReceived
        )
        assertEquals(Role.LEADER, leader.role)
        assertEquals(
            leader.received().first().message,
            VoteFromFollower(
                followerAfterVote.address,
                Destination.from(willPromoteAddress),
                1,
                "VOTE FROM FOLLOWER",
                true
            )
        )

        // Leader sends an AppendEntries on its promotion
        assertIs<AppendEntries>(leader.messages.sent.last().message)


        // Follower accepts the new Leader and send an AppendEntries Response
        val (_, leaderWaitingResponse, followerWithAppendRequest) = TimeMachine2(network, leader, followerAfterVote).tick()
        assertEquals(leaderWaitingResponse.address, followerWithAppendRequest.currentLeader)
        assertIs<AppendEntries>(followerWithAppendRequest.messages.received.last().message)

        // Leader receives the AppendEntries Response
        val (_, leaderWithResponse, _) = TimeMachine2(network, leaderWaitingResponse, followerWithAppendRequest).tick()
        val appendEntriesResponse = leaderWithResponse.messages.received.last().message
        assertIs<AppendEntriesResponse>(appendEntriesResponse)
        assertTrue(appendEntriesResponse.success)
        assertEquals(0, appendEntriesResponse.ack)

    }
}
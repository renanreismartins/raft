package statemachine.messagehandlers

import org.example.Destination
import org.example.Heartbeat
import org.example.Log
import org.example.Network
import org.example.Source
import org.example.VoteFromFollower
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.messagehandlers.voteHandler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VoteHandlerTest {

    @Test
    fun `Candidate is demoted to Follower if it receives a vote with higher term`() {
        // Given
        val network = Network()

        val candidateAddress = Source("127.0.0.1", 9001)
        val candidate = StateMachine(
            address = candidateAddress,
            name = "NodeA",
            network = network,
            peers = listOf(),
            votedFor = candidateAddress,
            term = 1,
            role = Role.CANDIDATE,
            termStartedAt = 3
        )

        val vote = VoteFromFollower(
            Source("127.0.0.1", 9002),
            Destination.from(candidateAddress),
            2,
            "",
            true
        )

        // When a Node (Candidate) receives a Vote with higher Term
        val demotedCandidate = voteHandler(candidate, vote)

        // Then it is Demoted to Follower
        assertEquals(Role.FOLLOWER, demotedCandidate.role)
        assertEquals(vote.term, demotedCandidate.term)
        assertEquals(emptySet(), demotedCandidate.votesReceived)

        // Cancels the election
        assertEquals(0, demotedCandidate.termStartedAt)

        assertNull(demotedCandidate.votedFor)
    }

    @Test
    fun `Candidate should receive a vote if the Vote Term is the same as its Term and the Vote is positive`() {
        // Given
        val network = Network()

        val candidateAddress = Source("127.0.0.1", 9001)

        val followerAddress1 = Destination("127.0.0.1", 9002)
        val followerAddress2 = Destination("127.0.0.1", 9003)

        // Meaningless Log to test setLength
        val log = Log(listOf(Heartbeat(candidateAddress, followerAddress1, 0, "")))

        val candidate = StateMachine(
            address = candidateAddress,
            name = "NodeA",
            network = network,
            peers = listOf(followerAddress1, followerAddress2),
            term = 1,
            role = Role.CANDIDATE,
            termStartedAt = 3,
            log = log
        ).voteForItself()

        val vote = VoteFromFollower(
            Source("127.0.0.1", 9002),
            Destination.from(candidateAddress),
            1,
            "",
            true
        )

        // When a Candidate Receives a valid and positive Vote
        val leader = voteHandler(candidate, vote)


        // Then it should have the vote from itself and the follower vote
        assertEquals(2, leader.votesReceived.size)
        assertTrue(leader.votesReceived.first().isTheSame(candidateAddress))
        assertTrue(leader.votesReceived.last().isTheSame(followerAddress1))

        // Then it should have been promoted to Leader if it has quorum
        assertEquals(Role.LEADER, leader.role)
        val expectedSetLength = mapOf(
            followerAddress1 to 1,
            followerAddress2 to 1
        )

        assertEquals(expectedSetLength, leader.setLength)

        val expectedAckedLength = mapOf(
            followerAddress1 to 0,
            followerAddress2 to 0
        )
        assertEquals(expectedAckedLength, leader.ackedLength)

        //TODO TEST HERE THE LOG REPLICATION THAT WILL BE ADDED IN THE FUTURE
    }
}
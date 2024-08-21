import org.example.Candidate
import org.example.Network
import org.example.ReceivedMessage
import org.example.VoteFromFollower
import org.example.Destination
import org.example.Source
import org.example.TimeMachine
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CandidateTest {
    @Test
    fun `Does not become a leader if it has not received Votes from the majority of the cluster`() {
        val candidate = candidate()

        assertFalse(candidate.shouldBecomeLeader())
    }

    //TODO Write a 'use' case where the Follower 'ticks' until it becomes a candidate
    // that would cover the shouldPromote from the Follower class
    @Test
    fun `Becomes a leader if it has received Votes from the majority of the cluster`() {
        val messageLog = listOf(
            // Follower votes for itself when promoted to Candidate, hence VoteFromFollower from host0 to host0
            ReceivedMessage(VoteFromFollower(Source("host0", 1), Destination("host0", 1), 0, "Vote"), 0),
            ReceivedMessage(VoteFromFollower(Source("host1", 1), Destination("host0", 1), 0, "Vote"), 0),
            ReceivedMessage(VoteFromFollower(Source("host2", 1), Destination("host0", 1), 0, "Vote"), 0),
        )

        val candidate = candidate().add(*messageLog.toTypedArray())

        assertTrue(candidate.shouldBecomeLeader())
    }

    @Test
    fun `When a Candidate reaches the electionTimeout, it will start a new term`() {
        // Given a candidate that is starting its term on the current tick
        val candidate = candidate().copy(termStartedAt = 0)
        val network = candidate.network

        val timeMachine = TimeMachine(network, candidate)

        // When the network is ticked 5 times (the length of the electionTimeout)
        val (_, candidateWithNewTerm) = timeMachine.tick(5)

        // Then the candidate has started a new term
        assertEquals(1, candidateWithNewTerm.term)
    }


    @Test
    fun `When a Candidate reaches the electionTimeout twice, it will be on term 2`() {
        // Given a candidate that is starting its term on the current tick
        val candidate = candidate().copy(termStartedAt = 0)
        val network = candidate.network

        val timeMachine = TimeMachine(network, candidate)

        // When the network is ticked 5 times (the length of the electionTimeout)
        val (_, candidateWithNewTerm) = timeMachine.tick(10)

        // Then the candidate has started a new term
        assertEquals(2, candidateWithNewTerm.term)
    }

    // TODO: Write a test for when a Candidate receives a message from a Node on an older tick
    //       It should ignore the message

    fun candidate(): Candidate {
        return Candidate(Source("host", 1), "name", 0, Network(), listOf(Destination("host1", 1), Destination("host2", 1), Destination("host2", 1)))
    }
}
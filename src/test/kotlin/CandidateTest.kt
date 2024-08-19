import org.example.Candidate
import org.example.Network
import org.example.ReceivedMessage
import org.example.VoteFromFollower
import org.example.Destination
import org.example.Source
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
            ReceivedMessage(VoteFromFollower(Source("host0", 1), Destination("host0", 1), "Vote"), 0),
            ReceivedMessage(VoteFromFollower(Source("host1", 1), Destination("host0", 1), "Vote"), 0),
            ReceivedMessage(VoteFromFollower(Source("host2", 1), Destination("host0", 1), "Vote"), 0),
        )

        val candidate = candidate().add(*messageLog.toTypedArray())

        assertTrue(candidate.shouldBecomeLeader())
    }

    fun candidate(): Candidate {
        return Candidate(Source("host", 1), "name", 0, Network(), listOf(Destination("host1", 1), Destination("host2", 1), Destination("host2", 1)))
    }
}
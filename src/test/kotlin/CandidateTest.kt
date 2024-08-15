import org.example.Address
import org.example.Candidate
import org.example.Destination
import org.example.Network
import org.example.ReceivedMessage
import org.example.Source
import org.example.VoteFromFollower
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CandidateTest {
    @Test
    fun `Does not become a leader if it has not received Votes from the majority of the cluster`() {
        val candidate = candidate()

        assertFalse(candidate.shouldBecomeLeader())
    }

    @Test
    fun `Becomes a leader if it has received Votes from the majority of the cluster`() {
        val messageLog = listOf(
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
import org.example.Address
import org.example.Candidate
import org.example.Network
import org.example.VoteFromFollower
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CandidateTest {
    @Test
    fun `Does not become a leader if it has not received Votes from the majority of the cluster`() {
        val candidate = candidate()

        assertFalse(candidate.shouldBecomeLeader(emptyList()))
    }

    @Test
    fun `Becomes a leader if it has received Votes from the majority of the cluster`() {
        val candidate = candidate()

        val messageLog = listOf(
            0 to VoteFromFollower(Address("host1", 1), Address("host0", 1), "Vote"),
            0 to VoteFromFollower(Address("host2", 1), Address("host0", 1), "Vote")
        )
        assertTrue(candidate.shouldBecomeLeader(messageLog))
    }

    fun candidate(): Candidate {
        return Candidate(Address("host", 1), "name", 0, Network(), listOf(Address("host1", 1), Address("host2", 1), Address("host2", 1)))
    }
}
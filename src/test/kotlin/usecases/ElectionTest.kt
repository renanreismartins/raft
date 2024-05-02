package usecases

import org.example.Address
import org.example.Candidate
import org.example.Config
import org.example.Follower
import org.example.Network
import org.example.TimeMachine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectionTest {

    // TODO increment the scope of this test so the Candidate receives a Vote from the Follower
    @Test
    fun `A Request for Votes is sent when a Follower Becomes a Candidate (timeout 3 ticks), the Follower receives the request and sends its vote to the Candidate`() {
        // Given
        val network = Network()

        val willPromoteAddress = Address("127.0.0.1", 9001)
        val remainsFollowerAddress = Address("127.0.0.1", 9002)

        val willPromote = Follower(willPromoteAddress, "NodeA", network = network, peers = listOf(remainsFollowerAddress), config = Config(3))
        val remainsFollower = Follower(remainsFollowerAddress, "NodeA", network = network, peers = listOf(remainsFollowerAddress), config = Config(10))

        // When
        val timeMachine = TimeMachine(network, willPromote, remainsFollower).tick(4)
        val (_, becameCandidate, remainedFollower) = timeMachine

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertTrue(becameCandidate is Candidate)
        assertTrue(remainedFollower is Follower)
        assertEquals("REQUEST FOR VOTES", remainedFollower.messages.first().second.content)

        val (_, candidateWithVote, _)= timeMachine.tick()

        assertEquals("VOTE FROM FOLLOWER", candidateWithVote.messages.first().second.content)
    }
}
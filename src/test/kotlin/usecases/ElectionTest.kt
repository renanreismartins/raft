package usecases

import org.example.Address
import org.example.Candidate
import org.example.Config
import org.example.Follower
import org.example.Network
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
        network.tick()
        val wp1 = willPromote.tick()
        val rf1 = remainsFollower.tick()

        network.tick()
        val wp2 = wp1.tick()
        val rf2 = rf1.tick()

        network.tick()
        val wp3 = wp2.tick()
        val rf3 = rf2.tick()

        network.tick()
        val candidate = wp3.tick()
        val follower = rf3.tick()
        network.tick()

        val followerWithRequest =  follower.tick()

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertTrue(follower is Follower)
        assertTrue(candidate is Candidate)
        assertEquals("REQUEST FOR VOTES", followerWithRequest.messages.first().second.content)

        // Then Follower sends the Vote to the Candidate
        network.tick()
        val candidateWithVote = candidate.tick()

        assertEquals("VOTE FROM FOLLOWER", candidateWithVote.messages.first().second.content)
    }
}
package usecases

import org.example.Address
import org.example.Follower
import org.example.Network
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ElectionTest {

    // TODO increment the scope of this test so the Candidate receives a Vote from the Follower
    @Test
    fun `A Request for Votes is sent when a Follower Becomes a Candidate (timeout 3 ticks) and the Follower receives the request`() {
        // Given
        val network = Network()

        val willPromoteAddress = Address("127.0.0.1", 9001)
        val remainsFollowerAddress = Address("127.0.0.1", 9002)

        val willPromote = Follower(willPromoteAddress, "NodeA", network = network, peers = listOf(remainsFollowerAddress))
        val remainsFollower = Follower(willPromoteAddress, "NodeA", network = network, peers = listOf(remainsFollowerAddress))

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

        // Then
        //TODO ASSERT ONE NODE IS CANDIDATE AND THE OTHER IS FOLLOWER
        //TODO this will fail because all the followers are becoming candidates at the same time
        assertEquals("REQUEST FOR VOTES", followerWithRequest.messages.first().second.content)
    }
}
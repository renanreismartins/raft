import org.example.Address
import org.example.Follower
import org.example.Network
import org.example.NetworkMessage
import org.example.RequestForVotes
import org.example.VoteFromFollower
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FollowerTest {
    @Test
    fun `Do not vote for more than one Candidate`() {
        val network = Network(mapOf(Address("host", 1) to listOf(
            NetworkMessage(RequestForVotes(Address("host", 2), Address("host", 1), "REQUEST FOR VOTES"), 0),
            NetworkMessage(RequestForVotes(Address("host", 3), Address("host", 1), "REQUEST FOR VOTES"), 0)
        )))
        val follower = Follower(Address("host", 1), "follower", 0, network, emptyList(), emptyList())

        follower.tick()

        //TODO COULD BE THAT THE ADDRESSES ARE WRONG
        assertEquals(listOf(VoteFromFollower(Address("host", 1), Address("host", 2), "VOTE FROM FOLLOWER")), follower.sent)
    }
}
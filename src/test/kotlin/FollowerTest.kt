import org.example.Address
import org.example.Follower
import org.example.Network
import org.example.NetworkMessage
import org.example.RequestForVotes
import org.example.TimeMachine
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

        //TODO Write an ADR on why use time machine instead of tick on the Node and Network and why the Network is mutable
        val timeMachine = TimeMachine(network, Follower(Address("host", 1), "follower", 0, network, emptyList(), emptyList()))
        val (_, follower) = timeMachine.tick()

        //TODO COULD BE THAT THE ADDRESSES ARE WRONG
        assertEquals(listOf((1 to VoteFromFollower(Address("host", 1), Address("host", 2), "VOTE FROM FOLLOWER"))), follower.sent)
    }
}
import org.example.Address
import org.example.Destination
import org.example.Follower
import org.example.Network
import org.example.NetworkMessage
import org.example.RequestForVotes
import org.example.Source
import org.example.TimeMachine
import org.example.VoteFromFollower
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FollowerTest {
    @Test
    fun `Do not vote for more than one Candidate`() {
        val network = Network(mapOf(Source("host", 1) to listOf(
            NetworkMessage(RequestForVotes(Source("host", 2), Destination("host", 1), "REQUEST FOR VOTES"), 0),
            NetworkMessage(RequestForVotes(Source("host", 3), Destination("host", 1), "REQUEST FOR VOTES"), 0)
        )))

        //TODO Write an ADR on why use time machine instead of tick on the Node and Network and why the Network is mutable
        val timeMachine = TimeMachine(network, Follower(Source("host", 1), "follower", 0, network, emptyList(), emptyList()))
        val (_, follower) = timeMachine.tick()

        assertEquals(listOf((1 to VoteFromFollower(Source("host", 1), Destination("host", 2), "VOTE FROM FOLLOWER"))), follower.sent)
    }
}
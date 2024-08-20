import org.example.Follower
import org.example.Network
import org.example.NetworkMessage
import org.example.RequestForVotes
import org.example.SentMessage
import org.example.TimeMachine
import org.example.VoteFromFollower
import org.example.Destination
import org.example.Source
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FollowerTest {
    @Test
    fun `After receiving Request For Votes from more than one candidate, respond with a Vote to only one of them`() {
        val network = Network(mapOf(
            Source("host", 1) to listOf(
            NetworkMessage(RequestForVotes(Source("host", 2), Destination("host", 1), 0, "REQUEST FOR VOTES"), 0),
            NetworkMessage(RequestForVotes(Source("host", 3), Destination("host", 1), 0, "REQUEST FOR VOTES"), 0)
        )))

        //TODO Write an ADR on why use time machine instead of tick on the Node and Network and why the Network is mutable
        val timeMachine = TimeMachine(network, Follower(Source("host", 1), "follower", 0, network, emptyList()))
        val (_, follower) = timeMachine.tick()

        assertEquals(listOf(SentMessage(VoteFromFollower(Source("host", 1), Destination("host", 2), 0, "VOTE FROM FOLLOWER"), 1)), follower.sent())
    }
}
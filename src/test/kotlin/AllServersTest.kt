import org.example.Destination
import org.example.Follower
import org.example.Heartbeat
import org.example.Leader
import org.example.Network
import org.example.Source
import org.example.TimeMachine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AllServersTest {
    @Test
    fun `When a Node receives a message with a higher term property, it should update it's term and revert to a Follower`() {
        val leaderAddress = Source("127.0.0.1", 9001)
        val network = Network()
        val initialLeader =
            Leader(
                leaderAddress,
                "Leader",
                peers = listOf(),
                network = network,
            )

        network.add(Heartbeat(Source("127.0.0.1", 9002), Destination.from(leaderAddress), content = "1", term = 10))
        val timeMachine = TimeMachine(network, initialLeader)

        val (_, demotedLeader) = timeMachine.tick()

        assertIs<Follower>(demotedLeader)
        assertEquals(10, demotedLeader.term)
    }
}

import org.example.AppendEntry
import org.example.AppendEntryResponse
import org.example.Destination
import org.example.Follower
import org.example.Network
import org.example.Source
import org.example.TimeMachine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AppendEntryTest {

    @Test
    fun `Do not accept (reply false) when the message term is smaller than the current term`() {
        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val network = Network()
        val follower =
            Follower(
                followerAddress,
                "Follower",
                peers = listOf(),
                network = network,
                term = 2
            )

        val destinationAddress = Source("127.0.0.1", 9002)
        network.add(
            AppendEntry(
                src = destinationAddress,
                dest = Destination.from(followerAddress),
                content = "1",
                term = 1,
                prevLogIndex = 1,
                prevLogTerm = 1,
                leaderCommit = 1)
        )

        // When
        val (_, followerWithResponse) = TimeMachine(network, follower).tick()

        // Then
        val appendEntryResponse = AppendEntryResponse(
            src = followerAddress,
            dest = Destination.from(destinationAddress),
            content = "",
            term = follower.term,
            success = false,
        )
        assertEquals(appendEntryResponse, followerWithResponse.messages.sent.first().message)

    }
}
import org.example.AppendEntry
import org.example.AppendEntryResponse
import org.example.Destination
import org.example.Follower
import org.example.Log
import org.example.Network
import org.example.Source
import org.example.TimeMachine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail

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
                term = 2,
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
                leaderCommit = 1,
            ),
        )

        // When
        val (_, followerWithResponse) = TimeMachine(network, follower).tick()

        // Then
        val appendEntryResponse =
            AppendEntryResponse(
                src = followerAddress,
                dest = Destination.from(destinationAddress),
                content = "",
                term = follower.term,
                success = false,
            )
        assertEquals(
            appendEntryResponse,
            followerWithResponse.messages.sent
                .first()
                .message,
        )
    }

    @Test
    fun `Do not accept (reply false) when the receiver log doesn't contain an entry at prevLogIndex`() {
        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val follower =
            Follower(
                followerAddress,
                "Follower",
                peers = listOf(),
                network = Network(),
                term = 1,
            )

        // When
        val node =
            follower.appendEntryResponse(
                AppendEntry(
                    src = Source("127.0.0.1", 9002),
                    dest = Destination.from(followerAddress),
                    content = "1",
                    term = 1,
                    prevLogIndex = 2,
                    prevLogTerm = 1,
                    leaderCommit = 1,
                ),
            )

        // Then
        val response = node.messages.toSend.first()
        assertIs<AppendEntryResponse>(response)
        assertEquals(false, response.success)
        assertEquals(1, response.term)
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Do not accept (reply false) when the receiver log entry at prevLogIndex conflicts with the term`() {
        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val sourceAddress = Source("127.0.0.1", 9002)
        val follower =
            Follower(
                followerAddress,
                "Follower",
                peers = listOf(),
                network = Network(),
                term = 1,
                log =
                    Log(
                        listOf(
                            AppendEntry(
                                src = sourceAddress,
                                dest = Destination.from(followerAddress),
                                content = "",
                                term = 1,
                                prevLogIndex = 0,
                                prevLogTerm = 0,
                                leaderCommit = 0,
                            ),
                        ),
                    ),
            )

        // When
        val node =
            follower.appendEntryResponse(
                AppendEntry(
                    src = Source("127.0.0.1", 9002),
                    dest = Destination.from(followerAddress),
                    content = "1",
                    term = 2,
                    prevLogIndex = 1,
                    prevLogTerm = 2,
                    leaderCommit = 0,
                ),
            )

        // Then
        val response = node.messages.toSend.first()
        assertIs<AppendEntryResponse>(response)
        assertEquals(false, response.success)
        assertEquals(1, response.term)
    }

    @Test
    fun `Accepts (replies true) when logs are aligned, appends new log entry and updates commitIndex`() {
        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val sourceAddress = Source("127.0.0.1", 9002)
        val follower =
            Follower(
                followerAddress,
                "Follower",
                peers = listOf(),
                network = Network(),
                term = 1,
                log =
                Log(
                    listOf(
                        AppendEntry(
                            src = sourceAddress,
                            dest = Destination.from(followerAddress),
                            content = "",
                            term = 1,
                            prevLogIndex = 0,
                            prevLogTerm = 1,
                            leaderCommit = 1,
                        ),
                        AppendEntry(
                            src = sourceAddress,
                            dest = Destination.from(followerAddress),
                            content = "",
                            term = 2,
                            prevLogIndex = 1,
                            prevLogTerm = 1,
                            leaderCommit = 1,
                        ),
                    ),
                ),
            )

        // When
        val node =
            follower.appendEntryResponse(
                AppendEntry(
                    src = Source("127.0.0.1", 9002),
                    dest = Destination.from(followerAddress),
                    content = "1",
                    term = 2,
                    prevLogIndex = 2,
                    prevLogTerm = 2,
                    leaderCommit = 4,
                ),
            )

        // Then
        val response = node.messages.toSend.first()
        assertIs<AppendEntryResponse>(response)
        assertEquals(true, response.success)

        //TODO move this assertion to a higher level as the term calculation is done in handleOutdatedTerm
        //assertEquals(1, response.term)

        assertEquals(3, node.commitIndex)
        assertEquals(3, node.log.size())
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Accepts (replies true) when Follower log has a conflict on the NEW entry, clears conflicting entries, appends new entry and updates commitIndex`() {
        fail()
    }
}

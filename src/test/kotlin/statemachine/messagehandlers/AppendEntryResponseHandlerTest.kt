package statemachine.messagehandlers

import org.example.AppendEntries
import org.example.AppendEntriesResponse
import org.example.Destination
import org.example.Entry
import org.example.Log2
import org.example.Network
import org.example.Source
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.messagehandlers.appendEntryResponseHandler
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppendEntryResponseHandlerTest {

    @Test
    fun `Node should be demoted to Follower when it receives a response with greater term`() {
        // Given
        val followerAddress = Destination("127.0.0.1", 9001)
        val leaderAddress = Source("127.0.0.1", 9000)
        val leader = StateMachine(
            address = leaderAddress,
            name = "leader",
            network = Network(),
            peers = listOf(),
            term = 1,
            role = Role.LEADER,
            termStartedAt = 4,
            votedFor = leaderAddress,
            sentLength = mapOf(followerAddress to 0),
            ackedLength = mapOf(followerAddress to 0)
        )

        val responseWithGreaterTerm = AppendEntriesResponse(
            Source("127.0.0.1", 9001),
            Destination.from(leaderAddress),
            3,
            "",
            0,
            true
        )

        val demoted = appendEntryResponseHandler(leader, responseWithGreaterTerm)

        assertEquals(Role.FOLLOWER, demoted.role)
        assertEquals(3, demoted.term)
        assertNull(demoted.votedFor)
        assertNull(demoted.termStartedAt)
    }

    @Test
    fun `Leader should record the Follower's acknowledgement and Commit Entries when it successfully accept the Entries`() {
        // Given
        val followerAddress = Destination("127.0.0.1", 9001)
        val followerAddress2 = Destination("127.0.0.1", 9002)
        val leaderAddress = Source("127.0.0.1", 9000)

        // A Leader with Logs synced with 2 Followers
        val leader = StateMachine(
            address = leaderAddress,
            name = "leader",
            network = Network(),
            peers = listOf(),
            term = 1,
            role = Role.LEADER,
            termStartedAt = 4,
            votedFor = leaderAddress,
            sentLength = mapOf(followerAddress to 0, followerAddress2 to 2),
            ackedLength = mapOf(followerAddress to 0, followerAddress2 to 2)
        )

        // When a Follower successfully ack Entries
        val response = AppendEntriesResponse(
            Source.from(followerAddress),
            Destination.from(leaderAddress),
            1,
            "",
            1,
            true
        )

        val leaderWithAck = appendEntryResponseHandler(leader, response)

        // Then sync sentLength
        assertEquals(
            mapOf(followerAddress to 1, followerAddress2 to 2),
            leaderWithAck.sentLength
        )

        // Then sync ackLength
        assertEquals(
            mapOf(followerAddress to 1, followerAddress2 to 2),
            leaderWithAck.ackedLength
        )

        //TODO check commited entries
    }


    @Test
    fun `Leader sync the Logs (Replicate) when the Acknowledgment of the Entries was not successful`() {
        /**
         * The Acknowledgment can fail for two reasons:
         * The Log in the Follower is not in the expected state to receive the entries
         * or when there are two AppendEntriesResponse in transit, and they arrive out of order.
         * This way the Ack number in the message will be smaller than the ack number recorded
         * in the Leader.
         */

        // Given
        val followerAddress = Destination("127.0.0.1", 9001)
        val followerAddress2 = Destination("127.0.0.1", 9002)
        val leaderAddress = Source("127.0.0.1", 9000)

        // A Leader with Logs synced with 2 Followers
        val leader = StateMachine(
            address = leaderAddress,
            name = "leader",
            network = Network(),
            peers = listOf(followerAddress, followerAddress2),
            log = Log2(listOf(
                Entry("ADD 1", 1),
                Entry("ADD 2", 1),
                Entry("ADD 3", 1))
            ),
            term = 1,
            role = Role.LEADER,
            termStartedAt = 4,
            votedFor = leaderAddress,
            sentLength = mapOf(followerAddress to 3, followerAddress2 to 3),
            ackedLength = mapOf(followerAddress to 2, followerAddress2 to 2)
        )

        // When a Follower does not ack Entries
        val response = AppendEntriesResponse(
            Source.from(followerAddress),
            Destination.from(leaderAddress),
            1,
            "",
            1,
            true
        )
        val leaderWithAck = appendEntryResponseHandler(leader, response)

        // Then decrease the sentLength of the first Follower and replicate Logs,
        // and sentLength of the second Follower is maintained
        assertEquals(
            mapOf(followerAddress to 2, followerAddress2 to 3),
            leaderWithAck.sentLength
        )
        assertIs<AppendEntries>(leaderWithAck.messages.toSend.last())

        // Then ackLength is maintained for both Followers
        assertEquals(
            mapOf(followerAddress to 2, followerAddress2 to 2),
            leaderWithAck.ackedLength
        )

        //TODO check commited entries
    }

}
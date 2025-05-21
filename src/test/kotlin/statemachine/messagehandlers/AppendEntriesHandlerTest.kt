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
import org.example.statemachine.messagehandlers.appendEntriesHandler
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppendEntriesHandlerTest {

    @Test
    fun `Node should be demoted to Follower, update its Term, cancel election, and set the Leader Record if it has an outdated Term`() {
        // Given
        val newLeaderAddress = Source("127.0.0.1", 9002)
        val outdatedLeaderAddress = Source("127.0.0.1", 9001)
        val outdatedLeader = StateMachine(
            address = outdatedLeaderAddress,
            name = "OutdatedLeader",
            network = Network(),
            peers = listOf(),
            term = 1,
            role = Role.LEADER,
            termStartedAt = 4,
            votedFor = outdatedLeaderAddress
        )

        val appendEntries = AppendEntries(newLeaderAddress,
            Destination.from(outdatedLeaderAddress),
            2,
            0,
            0,
            0,
            emptyList()
        )

        // When an outdated Leader receives an AppendEntries with higher term
        val demoted = appendEntriesHandler(outdatedLeader, appendEntries)


        // Then
        assertEquals(Role.FOLLOWER, demoted.role)
        assertEquals(2, demoted.term)
        assertNull(demoted.votedFor)
        assertNull(demoted.termStartedAt)
        assertEquals(newLeaderAddress, demoted.currentLeader)
    }

    /**
     *
     * This is typically the case when there are two Candidates in the same Term
     * and one gets promoted to Leader and send the AppendEntries
     *
     */
    @Test
    fun `Node should be demoted to Follower and set the Leader Record if it has the Term as the same as the Message Term`() {
        // Given
        val newLeaderAddress = Source("127.0.0.1", 9002)
        val outdatedCandidateAddress = Source("127.0.0.1", 9001)
        val outdatedCandidate = StateMachine(
            address = outdatedCandidateAddress,
            name = "OutdatedLeader",
            network = Network(),
            peers = listOf(),
            term = 1,
            role = Role.CANDIDATE,
            termStartedAt = 4,
            votedFor = outdatedCandidateAddress
        )

        val appendEntries = AppendEntries(newLeaderAddress,
            Destination.from(outdatedCandidateAddress),
            1,
            0,
            0,
            0,
            emptyList()
        )

        // When an outdated Candidate receives an AppendEntries an equal Term
        val demoted = appendEntriesHandler(outdatedCandidate, appendEntries)

        // Then
        assertEquals(Role.FOLLOWER, demoted.role)
        assertEquals(1, demoted.term)
        assertEquals(outdatedCandidateAddress, demoted.votedFor)
        assertEquals(4, demoted.termStartedAt)
        assertEquals(newLeaderAddress, demoted.currentLeader)
    }


    /**
     *
     * Entries should be accepted when the Terms of the Node and Message are the same
     * and the Log in the Follower is at least of the size of the prefix in the Message
     * and the prefix is 0, meaning the Leader had nothing to append to the Follower.
     * In this case ack = 0
     *
     * This is the case when the Leader has an empty Log.
     */
    @Test
    fun `Entries should be accepted when Follower has no entries to be appended to the Follower`() {
        // Given
        val leaderAddress = Source("127.0.0.1", 9002)
        val followerAddress = Source("127.0.0.1", 9001)
        val follower = StateMachine(
            address = followerAddress,
            name = "NodeA",
            network = Network(),
            peers = listOf(),
            term = 2,
            role = Role.FOLLOWER,
            termStartedAt = 4,
            votedFor = leaderAddress
        )

        val appendEntries = AppendEntries(leaderAddress,
            Destination.from(followerAddress),
            2,
            0,
            0,
            0,
            emptyList()
        )

        // When a Follower receives an AppendEntries that should be accepted
        val followerWithResponse = appendEntriesHandler(follower, appendEntries)

        // Then Entries should be accepted
        val response = followerWithResponse.messages.toSend[0]
        assertIs<AppendEntriesResponse>(response)
        assertTrue(response.success)
        assertEquals(0, response.ack)
    }

    /**
     *
     * Entries should be accepted when the Terms of the Node and Message are the same
     * and the Log in the Follower is at least of the size of the prefix in the Message
     * and the Logs are identical up to the prefixLen
     *
     */
    @Test
    fun `Entries should be accepted when Leader and Follower have identical Log up to the prefix`() {
        // Given
        val leaderAddress = Source("127.0.0.1", 9002)
        val followerAddress = Source("127.0.0.1", 9001)

        val log = Log2(listOf(Entry("ADD 1", 1)))

        val follower = StateMachine(
            address = followerAddress,
            name = "NodeA",
            network = Network(),
            peers = listOf(),
            log = log,
            term = 1,
            role = Role.FOLLOWER,
            termStartedAt = 4,
            votedFor = leaderAddress
        )

        val appendEntries = AppendEntries(
            leaderAddress,
            Destination.from(followerAddress),
            1,
            1,
            1,
            0,
            listOf(Entry("ADD 2", 1))
        )

        // When a Follower receives an AppendEntries that should be accepted
        val followerWithResponse = appendEntriesHandler(follower, appendEntries)

        // Then Entries should be accepted
        val response = followerWithResponse.messages.toSend[0]
        assertIs<AppendEntriesResponse>(response)
        assertTrue(response.success)

        //TODO is Ack the total of entries acked or the entries acked only on this message?
        // Apparently the former.
        assertEquals(2, response.ack)
    }

    @Test
    fun `Entries should not be accepted when the Follower has a outdated Term`() {
        // Given
        val leaderAddress = Source("127.0.0.1", 9002)
        val followerAddress = Source("127.0.0.1", 9001)

        val follower = StateMachine(
            address = followerAddress,
            name = "NodeA",
            network = Network(),
            peers = listOf(),
            term = 1,
            role = Role.FOLLOWER,
            termStartedAt = 4,
            votedFor = leaderAddress
        )

        val appendEntriesWithOutdatedTerm = AppendEntries(
            leaderAddress,
            Destination.from(followerAddress),
            0,
            0,
            0,
            0,
            listOf(Entry("ADD 2", 1))
        )

        // When a Follower receives an AppendEntries with an outdated term, should reject it
        val followerWithResponse = appendEntriesHandler(follower, appendEntriesWithOutdatedTerm)

        // Then Entries should be rejected
        val response = followerWithResponse.messages.toSend[0]
        assertIs<AppendEntriesResponse>(response)
        assertFalse(response.success)
        assertEquals(0, response.ack)
    }

    @Test
    fun `Entries should not be accepted when there is a gap in the logs`() {
        // Given
        val leaderAddress = Source("127.0.0.1", 9002)
        val followerAddress = Source("127.0.0.1", 9001)

        val log = Log2(listOf(Entry("ADD 1", 1)))

        val follower = StateMachine(
            address = followerAddress,
            name = "NodeA",
            network = Network(),
            peers = listOf(),
            log = log,
            term = 1,
            role = Role.FOLLOWER,
            termStartedAt = 4,
            votedFor = leaderAddress
        )

        val appendEntries = AppendEntries(
            leaderAddress,
            Destination.from(followerAddress),
            1,
            2,
            1,
            0,
            listOf(Entry("ADD 2", 1))
        )

        // When there is a gap in the Logs
        // The Follower has fewer logs than the Leader believe it has sent
        val followerWithResponse = appendEntriesHandler(follower, appendEntries)

        // Then Entries should be accepted
        val response = followerWithResponse.messages.toSend[0]
        assertIs<AppendEntriesResponse>(response)
        assertFalse(response.success)
        assertEquals(0, response.ack)
    }

    @Test
    fun `Entries should not be accepted when logs are not in sync`() {
        // Given
        val leaderAddress = Source("127.0.0.1", 9002)
        val followerAddress = Source("127.0.0.1", 9001)

        val log = Log2(listOf(Entry("ADD 1", 1)))

        val follower = StateMachine(
            address = followerAddress,
            name = "NodeA",
            network = Network(),
            peers = listOf(),
            log = log,
            term = 1,
            role = Role.FOLLOWER,
            termStartedAt = 4,
            votedFor = leaderAddress
        )

        val appendEntries = AppendEntries(
            leaderAddress,
            Destination.from(followerAddress),
            2,
            1,
            2,
            0,
            listOf(Entry("ADD 2", 2))
        )

        // When a Follower receives an AppendEntries that should be accepted
        val followerWithResponse = appendEntriesHandler(follower, appendEntries)

        // Then Entries should be accepted
        val response = followerWithResponse.messages.toSend[0]
        assertIs<AppendEntriesResponse>(response)
        assertFalse(response.success)
        assertEquals(0, response.ack)
    }
}
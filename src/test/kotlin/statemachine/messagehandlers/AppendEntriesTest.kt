package statemachine.messagehandlers

import org.example.AppendEntries
import org.example.Destination
import org.example.Entry
import org.example.Log2
import org.example.Messages
import org.example.Network
import org.example.Source
import org.example.statemachine.StateMachine
import org.example.statemachine.messagehandlers.appendEntries
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AppendEntriesTest {

    /**
     * When the Follower has entries (size 2) that goes beyond the prefixLen (size 1)
     * it means the Follower's entries overlap with the suffix.
     */
    @Test
    fun `Out of sync Entries in the Follower should be truncated and new entry added`() {
        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val follower = StateMachine(
            followerAddress,
            "Follower",
            0,
            Network(),
            listOf(),
            null,
            Messages(),
            Log2(listOf(Entry("ADD 1", 1), Entry("ADD 2", 1))),
            1
        )

        // When processing entries with a higher term signaling out of sync logs
        val followerWithTruncatedLog = appendEntries(
            follower,
            AppendEntries(
                Source("127.0.0.1", 9000),
                Destination.from(followerAddress),
                1,
                1,
                1,
                0,
                listOf(Entry("ADD 3", 2))
            )
        )

        // Then Follower has the out of sync Log truncated and the entry in the message added to
        // the log.
        assertEquals(
            listOf(Entry("ADD 1", 1), Entry("ADD 3", 2)),
            followerWithTruncatedLog.log.entries
        )
    }

    @Test
    fun `Overlapping entries between Leader and Follower are preserved in the Follower and new Entries are added`() {
        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val entries = listOf(
            Entry("ADD 1", 1),
            Entry("ADD 2", 1),
            Entry("ADD 3", 1),
            Entry("ADD 4", 1),
        )
        val follower = StateMachine(
            followerAddress,
            "Follower",
            0,
            Network(),
            listOf(),
            null,
            Messages(),
            Log2(entries),
            1
        )

        // When processing in sync overlapping entries
        val followerWithTruncatedLog = appendEntries(
            follower,
            AppendEntries(
                Source("127.0.0.1", 9000),
                Destination.from(followerAddress),
                1,
                2,
                1,
                0,
                listOf(
                    Entry("ADD 3", 1),
                    Entry("ADD 4", 1),
                    Entry("ADD 5", 1)
                )
            )
        )

        // Then Follower has all its logs, added the new entry
        assertEquals(
            entries + Entry("ADD 5", 1),
            followerWithTruncatedLog.log.entries
        )
    }

    @Test
    fun `New Entries are added and delivered to the application`() {
        // Given
        val followerAddress = Source("127.0.0.1", 9001)
        val follower = StateMachine(
            followerAddress,
            "Follower",
            0,
            Network(),
            listOf(),
            null,
            Messages(),
            Log2(listOf(Entry("ADD 1", 1))),
            1
        )

        // When processing adding one entry and the Leader signaled an entry to be commited
        val NUMBER_OF_ENTRIES_TO_COMMIT = 1
        val logWithCommitedEntry = appendEntries(
            follower,
            AppendEntries(
                Source("127.0.0.1", 9000),
                Destination.from(followerAddress),
                1,
                1,
                1,
                 NUMBER_OF_ENTRIES_TO_COMMIT,
                listOf(Entry("ADD 2", 1))
            )
        )

        // Then Follower has all its logs, added the new entry
        assertEquals(
            listOf(Entry("ADD 1", 1), Entry("ADD 2", 1)),
            logWithCommitedEntry.log.entries
        )

        // And keeps track of the commited index signaled by the leader
        assertEquals(1, logWithCommitedEntry.commitLength)

        //TODO could test entry delivered to the application
    }
}
package statemachine

import org.example.AppendEntries
import org.example.Destination
import org.example.Entry
import org.example.Log2
import org.example.Network
import org.example.Source
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LogReplicationTest {

    //TODO reset heartbeat at the REPLICATE LOG

    @Test
    fun `Logs should not be replicated when the they are in sync`() {
        // Given
        val network = Network()

        val followerAddress = Destination("127.0.0.1", 9002)

        val leaderAddress = Source("127.0.0.1", 9001)

        /**
         * When a Leader is instantiated, it assumes that all Logs were already sent to the
         * Follower.
         *
         * The state that controls the assumptions of what entries were sent to the Follower
         * (sentLength[ follower ]) is set with the size of the Log.
         *
         */
        val leader = StateMachine(
            address = leaderAddress,
            name = "NodeA",
            network = network,
            peers = listOf(followerAddress),
            log = Log2(listOf(Entry("ADD 2", 1), Entry("ADD 2", 2))),
            term = 2,
            //TODO the following 4 should be in a method "promoteToLeader"
            // duplication from VoteHandler
            role = Role.LEADER,
            termStartedAt = null,
            sentLength = mapOf(followerAddress to 2), // peerAddresses to Log size
            ackedLength = mapOf(followerAddress to 0) // peerAddresses to 0 on election
        )

        // When leader replicates its log to a Follower
        val appendEntries = leader.replicateLog(followerAddress)

        // Then it has an AppendEntry with no Entries
        assertEquals(
            AppendEntries(
                leaderAddress,
                followerAddress,
                2,
                2,
                2,
                0,
                listOf()
            ),
            appendEntries
        )
    }

    @Test
    fun `All Log entries should be replicated if nothing was sent to the Follower yet`() {
        // Given
        val network = Network()
        network.clock = 12

        val followerAddress = Destination("127.0.0.1", 9002)

        val leaderAddress = Source("127.0.0.1", 9001)
        val entries = listOf(Entry("ADD 2", 1), Entry("ADD 2", 2))
        val leader = StateMachine(
            address = leaderAddress,
            name = "NodeA",
            network = network,
            peers = listOf(followerAddress),
            log = Log2(entries),
            term = 2,
            role = Role.LEADER,
            sentLength = mapOf(followerAddress to 0),
            ackedLength = mapOf(followerAddress to 0), // peerAddresses to 0 on election
            termStartedAt = null
        )

        // When leader replicates its log to a Follower
        // it has not sent any logs yet
        val appendEntries = leader.replicateLog(followerAddress)

        // Then Leader has sends an AppendEntry with all Log Entries

        /**
         * TODO Verify if it this scenario is possible:
         * A Leader with a Log > 0 but the sentLength[ follower ] of 0
         * Here even though the Log has an entry with a Term of 2,
         * the prefixTerm is sent as 0, because prefixLen is 0 as the sentLength.
         * Remember sentLength is calculated on the Leader promotion and here,
         * We are assuming the value is different than the initialised one.
         */

        assertEquals(
            AppendEntries(
                leaderAddress,
                followerAddress,
                2,
                0,
                0,
                0,
                entries
            ),
            appendEntries
        )
    }

    @Test
    fun `AppendEntries should be sent to all Followers and the Heartbeat timeout reset`() {
        // Given
        val network = Network()
        network.clock = 12

        val followerAddress1 = Destination("127.0.0.1", 9002)
        val followerAddress2 = Destination("127.0.0.1", 9003)
        val leaderAddress = Source("127.0.0.1", 9001)

        val leader = StateMachine(
            address = leaderAddress,
            name = "NodeA",
            network = network,
            peers = listOf(followerAddress1, followerAddress2),
            log = Log2(listOf(Entry("ADD 2", 1), Entry("ADD 2", 2))),
            term = 2,
            role = Role.LEADER,
            termStartedAt = null,
            sentLength = mapOf(followerAddress1 to 2), // peerAddresses to Log size
            ackedLength = mapOf(followerAddress1 to 0) // peerAddresses to 0 on election
        )

        // When leader replicates its log to a Follower
        val leaderWithEntries = leader.replicateLog()

        // Then Leader should have 2 AppendEntries, one for each Follower
        assertEquals(2, leaderWithEntries.messages.toSend.size)
        assertEquals(12, leaderWithEntries.sentHeartbeatAt)
    }
}
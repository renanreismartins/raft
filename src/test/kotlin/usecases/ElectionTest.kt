package usecases

import org.example.Address
import org.example.Candidate
import org.example.Config
import org.example.Follower
import org.example.Leader
import org.example.Network
import org.example.RequestForVotes
import org.example.TimeMachine
import org.example.VoteFromFollower
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectionTest {

    @Test
    fun `A Request for Votes is sent when a Follower becomes a Candidate (timeout 3 ticks), the Follower receives the request and sends its vote to the Candidate that becomes a Leader`() {
        // Given
        val network = Network()

        val willPromoteAddress = Address("127.0.0.1", 9001)
        val remainsFollowerAddress = Address("127.0.0.1", 9002)

        val willPromote = Follower(willPromoteAddress, "NodeA", network = network, peers = listOf(remainsFollowerAddress), config = Config(3))
        val remainsFollower = Follower(remainsFollowerAddress, "NodeA", network = network, peers = listOf(remainsFollowerAddress), config = Config(10))

        // When
        val timeMachine = TimeMachine(network, willPromote, remainsFollower).tick(4)
        val (_, becameCandidate, remainedFollower) = timeMachine

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertTrue(becameCandidate is Candidate)
        assertTrue(remainedFollower is Follower)
        assertEquals("REQUEST FOR VOTES", remainedFollower.received.first().second.content)

        val (_, leaderWithVote, _)= timeMachine.tick()

        assertTrue(leaderWithVote is Leader)
        assertEquals("VOTE FROM FOLLOWER", leaderWithVote.received.first().second.content)
    }

    @Test
    fun `With 3 nodes, where 2 will promote to Candidate at the same tick, only will receive a vote from the remaining Follower (and promote) the other will revert to Follower`() {
        // Given
        val network = Network()

        val willBecomeLeaderAddress = Address("127.0.0.1", 9001)
        val willLoseElectionAddress = Address("127.0.0.1", 9002)
        val remainsFollowerAddress = Address("127.0.0.1", 9003)

        val willBecomeLeader = Follower(willBecomeLeaderAddress, "NodeA", network = network, peers = listOf(willLoseElectionAddress, remainsFollowerAddress), config = Config(3))
        val willLoseElection = Follower(willLoseElectionAddress, "NodeB", network = network, peers = listOf(willBecomeLeaderAddress, remainsFollowerAddress), config = Config(3))
        val remainsFollower = Follower(remainsFollowerAddress, "NodeC", network = network, peers = listOf(willBecomeLeaderAddress, willLoseElectionAddress), config = Config(10))

        // When
        val timeMachine = TimeMachine(network, willBecomeLeader, willLoseElection, remainsFollower).tick(4)
        val (_, candidateWillWin, candidateWillLose, follower) = timeMachine

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertTrue(candidateWillWin is Candidate)
        assertTrue(candidateWillLose is Candidate)
        assertTrue(follower is Follower)

        val futureWinnerRequestForVotes = follower.received.first().second
        assertTrue(futureWinnerRequestForVotes is RequestForVotes)
        assertEquals(willBecomeLeaderAddress, futureWinnerRequestForVotes.src)

        val futureLoserRequestForVotes = follower.received[1].second
        assertTrue(futureLoserRequestForVotes is RequestForVotes)
        assertEquals(willLoseElectionAddress, futureLoserRequestForVotes.src)

        val (_, leader, demotedToFollower, _) = timeMachine.tick()

        assertTrue(leader is Leader)
        assertTrue(leader.received.last().second is VoteFromFollower)

        // TODO: demotedToFollower is being promoted to Leader because the follower is sending multiple votes.
        assertTrue(demotedToFollower is Follower)


        // TODO check that non-Leaders receive the Heartbeat from new Leader
    }

}
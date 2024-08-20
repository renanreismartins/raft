package usecases

import org.example.Candidate
import org.example.Config
import org.example.Follower
import org.example.Heartbeat
import org.example.Leader
import org.example.Network
import org.example.RequestForVotes
import org.example.TimeMachine
import org.example.VoteFromFollower
import org.example.Destination
import org.example.Source
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ElectionTest {

    @Test
    fun `A Request for Votes is sent when a Follower becomes a Candidate (timeout 3 ticks), the Follower receives the request and sends its vote to the Candidate that becomes a Leader`() {
        // Given
        val network = Network()

        val willPromoteAddress = Source("127.0.0.1", 9001)
        val remainsFollowerAddress = Source("127.0.0.1", 9002)

        val willPromote = Follower(willPromoteAddress, "NodeA", network = network, peers = listOf(Destination.from(remainsFollowerAddress)), config = Config(3))
        val remainsFollower = Follower(remainsFollowerAddress, "NodeA", network = network, peers = Destination.from(listOf(remainsFollowerAddress)), config = Config(10))

        // When
        val timeMachine = TimeMachine(network, willPromote, remainsFollower).tick(4)
        val (_, becameCandidate, remainedFollower) = timeMachine

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertTrue(becameCandidate is Candidate)
        assertTrue(remainedFollower is Follower)
        assertEquals("REQUEST FOR VOTES", remainedFollower.received().first().message.content)

        val (_, leaderWithVote, _)= timeMachine.tick()

        assertTrue(leaderWithVote is Leader)
        assertEquals("Vote from self", leaderWithVote.received().first().message.content)
        assertEquals("VOTE FROM FOLLOWER", leaderWithVote.received().last().message.content)
    }

    //TODO use the assertIs idiomatic matchers
    @Test
    fun `With 3 nodes, where 2 will promote to Candidate at the same tick, only one will receive a vote from the remaining Follower (and promote), the other will revert to Follower`() {
        // Given
        val network = Network()

        val willBecomeLeaderAddress = Source("127.0.0.1", 9001)
        val willLoseElectionAddress = Source("127.0.0.1", 9002)
        val remainsFollowerAddress = Source("127.0.0.1", 9003)


        val willBecomeLeader = Follower(willBecomeLeaderAddress, "NodeA", network = network, peers = Destination.from(listOf(willLoseElectionAddress, remainsFollowerAddress)), config = Config(3))
        val willLoseElection = Follower(willLoseElectionAddress, "NodeB", network = network, peers = Destination.from(listOf(willBecomeLeaderAddress, remainsFollowerAddress)), config = Config(3))
        val remainsFollower = Follower(remainsFollowerAddress, "NodeC", network = network, peers = Destination.from(listOf(willBecomeLeaderAddress, willLoseElectionAddress)), config = Config(10))

        // When
        val timeMachine = TimeMachine(network, willBecomeLeader, willLoseElection, remainsFollower).tick(4)
        val (_, candidateWillWin, candidateWillLose, follower) = timeMachine

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertTrue(candidateWillWin is Candidate)
        assertTrue(candidateWillLose is Candidate)
        assertTrue(follower is Follower)

        // Make sure the Request For Votes arrived at the Follower

        //TODO Make a assertion matcher to guarantee a node sent a message and it arrived on the other Node and write an ADR
        val futureWinnerRequestForVotes = follower.received().first().message
        assertTrue(futureWinnerRequestForVotes is RequestForVotes)
        assertEquals(willBecomeLeaderAddress, futureWinnerRequestForVotes.src)

        val futureLoserRequestForVotes = follower.received()[1].message
        assertTrue(futureLoserRequestForVotes is RequestForVotes)
        assertEquals(willLoseElectionAddress, futureLoserRequestForVotes.src)

        val (_, leader, candidateToBeDemoted, _) = timeMachine.tick()

        // New leader is decided, other candidate will be demoted when it receives Heartbeat on the next tick
        assertIs<Leader>(leader)
        assertTrue(leader.received().last().message is VoteFromFollower)
        assertIs<Candidate>(candidateToBeDemoted)

        // TODO check that non-Leaders receive the Heartbeat from new Leader
        val (_, _, follower1, follower2) = timeMachine.tick()
        // Candidate has been demoted to Follower
        assertIs<Follower>(follower1)
        assertEquals(Heartbeat(willBecomeLeaderAddress, Destination.from(follower1.address), 0, "0"), follower1.received().last().message)
        assertEquals(Heartbeat(willBecomeLeaderAddress, Destination.from(follower2.address), 0, "0"), follower2.received().last().message)
    }

}
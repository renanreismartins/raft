package statemachine.usecases

import org.example.AppendEntries
import org.example.AppendEntriesResponse
import org.example.ClientCommand
import org.example.Config
import org.example.Destination
import org.example.Network
import org.example.Source
import org.example.VoteFromFollower
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.TimeMachine2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StateMachineElectionTest {
    @Test
    fun `A Request for Votes is sent when a Follower becomes a Candidate (election timeout in 3 ticks), the Follower receives the request and sends its vote to the Candidate that becomes a Leader`() {
        // Given
        val network = Network()

        val willPromoteAddress = Source("127.0.0.1", 9001)
        val remainsFollowerAddress = Source("127.0.0.1", 9002)

        val willPromote =
            StateMachine(
                willPromoteAddress,
                "NodeA",
                network = network,
                peers = listOf(Destination.from(remainsFollowerAddress)),
                config = Config(20),
            )

        val remainsFollower =
            StateMachine(
                remainsFollowerAddress,
                "NodeB",
                network = network,
                peers = Destination.from(listOf(willPromoteAddress)),
                config = Config(30),
            )

        // When election times out
        val timeMachine = TimeMachine2(network, willPromote, remainsFollower).tick(21)
        val (_, becameCandidate, remainedFollower) = timeMachine

        // Then Candidate got promoted and sent its Request for Votes to the Follower
        assertEquals(Role.CANDIDATE, becameCandidate.role)

        //Receives a vote from himself
        assertEquals(setOf(willPromoteAddress), becameCandidate.votesReceived)

        // Follower receives a request for vote
        assertEquals(Role.FOLLOWER, remainedFollower.role)
        assertEquals(
            "REQUEST FOR VOTES",
            remainedFollower.received().first().message.content
        )

        // When the Nodes Vote for the Candidate
        val (_, leader, followerAfterVote) = timeMachine.tick()

        // Candidate receives the vote and as it has the quorum, it is promoted to leader
        assertEquals(
            setOf(willPromoteAddress, remainsFollowerAddress),
            leader.votesReceived
        )
        assertEquals(Role.LEADER, leader.role)
        assertEquals(
            leader.received().first().message,
            VoteFromFollower(
                followerAfterVote.address,
                Destination.from(willPromoteAddress),
                1,
                "VOTE FROM FOLLOWER",
                true
            )
        )

        // Leader sends an AppendEntries on its promotion
        assertIs<AppendEntries>(leader.messages.sent.last().message)

        // Follower accepts the new Leader and send an AppendEntries Response
        val (_, leaderWaitingResponse, followerWithAppendRequest) = TimeMachine2(network, leader, followerAfterVote).tick()
        assertEquals(leaderWaitingResponse.address, followerWithAppendRequest.currentLeader)
        assertIs<AppendEntries>(followerWithAppendRequest.messages.received.last().message)


        // Leader receives the AppendEntries Response
        val (_, leaderWithResponse, followerAfterResponse) = TimeMachine2(network, leaderWaitingResponse, followerWithAppendRequest).tick()
        val appendEntriesResponse = leaderWithResponse.messages.received.last().message
        assertIs<AppendEntriesResponse>(appendEntriesResponse)
        assertTrue(appendEntriesResponse.success)
        assertEquals(0, appendEntriesResponse.ack)
        assertEquals(0, leaderWithResponse.commitLength)


        // Follower has not received any message as the logs are empty and in sync
        assertEquals(followerWithAppendRequest.messages.received, followerAfterResponse.messages.received)

        // A client sends a command
        network.add(ClientCommand(Source("127.0.0.1", 8000), Destination.from(leaderWithResponse.address), -1, "ADD 1"))
        val (_, leaderWithCommitedEntry, followerNoCommitedEntry) = TimeMachine2(network, leaderWithResponse, followerAfterResponse).tick(4)

        /**
         * On Follower response, Leader has the entry commited because
         * the follower acknowledged it, and also himself (2 of 3 nodes)
         *
         * The follower has not yet commited, because it needs the confirmation
         * from the Leader, that can come in another message or heartbeat
         */
        assertEquals(1, leaderWithCommitedEntry.commitLength)
        assertEquals(0, followerNoCommitedEntry.commitLength)

        /**
         * Heartbeat timeout happens, the leader sends an appendEntries
         * and the Follower gets the previous entry commited
         */
        val (_, l1AfterHeartBeatWithResponse, f1WithCommitedEntry) = TimeMachine2(network, leaderWithCommitedEntry, followerNoCommitedEntry).tick(5)
        assertEquals(1, f1WithCommitedEntry.commitLength)
        assertEquals(1, l1AfterHeartBeatWithResponse.commitLength)
    }
}
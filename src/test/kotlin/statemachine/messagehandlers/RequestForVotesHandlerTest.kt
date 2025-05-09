package statemachine.messagehandlers

import org.example.Destination
import org.example.Heartbeat
import org.example.Log
import org.example.Network
import org.example.RequestForVotes
import org.example.Source
import org.example.VoteFromFollower
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.messagehandlers.requestForVotesHandler
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RequestForVotesHandlerTest {

    /**
     *
     * In this scenario the Voting Node (the Node that handles the Request For Votes)
     * could also be a candidate. This can happen as two Nodes can start an election at
     * the same time, even though it is not the normal scenario.
     *
     */
    @Test
    fun `A Node should demote to Follower when the Request For Votes has a higher Term than its own, but still votes yes`() {
        // Given
        val network = Network()

        // Given
        val votingNodeAddress = Source("127.0.0.1", 9001)
        val votingNode = StateMachine(
            address = votingNodeAddress,
            name = "NodeA",
            network = network,
            peers = listOf(),
            votedFor = votingNodeAddress,
            term = 1,
            role = Role.CANDIDATE,
            termStartedAt = 10
        )

        // When
        /**
         * A message from a higher term arrives, the Voting Node will have its Term set to the
         * message term, this way the Message Term and the Voting Node term are the
         * same and the logs are in synch, the positive vote happens.
         */
        val candidateAddress = Source("127.0.0.1", 9002)
        val requestFromCandidate = RequestForVotes(
            candidateAddress,
            Destination.from(votingNodeAddress),
            4,
            "",
            0,
            0
        )

        val votingNodeWithVote = requestForVotesHandler(votingNode, requestFromCandidate)

        // Then
        assertEquals(Role.FOLLOWER, votingNodeWithVote.role)
        assertEquals(4, votingNodeWithVote.term)
        assertEquals(candidateAddress, votingNodeWithVote.votedFor)
        assertEquals(setOf(), votingNodeWithVote.votesReceived)
        assertEquals(0, votingNodeWithVote.termStartedAt)
        assertIs<VoteFromFollower>(votingNodeWithVote.messages.toSend.first())
    }

    @Test
    fun `A Node should vote 'no' if the Candidate Log has a lower term than the Voting Node Log`() {
        // Given
        val network = Network()

        // Given
        val votingNodeAddress = Source("127.0.0.1", 9001)
        val log = Log(listOf(Heartbeat(votingNodeAddress, Destination("127.0.0.1", 9003), 3, "")))

        val votingNode = StateMachine(
            address = votingNodeAddress,
            name = "NodeA",
            network = network,
            peers = listOf(),
            votedFor = null,
            term = 4,
            role = Role.FOLLOWER,
            log = log,
            termStartedAt = 0
        )

        // When
        /**
         * Both Nodes are in the same Term (4), but the Candidate Log Term (2)
         * is lower than the Voting Node Log Term (3)
         */
        val candidateAddress = Source("127.0.0.1", 9002)
        val requestFromCandidate = RequestForVotes(
            candidateAddress,
            Destination.from(votingNodeAddress),
            4,
            "",
            2,
            1
        )

        val votingNodeWithVote = requestForVotesHandler(votingNode, requestFromCandidate)

        // Then
        // A negative vote is sent
        val vote = votingNodeWithVote.messages.toSend.first()
        assertIs<VoteFromFollower>(vote)
        assertEquals(4, vote.term)
        assertTrue(vote.dest.isTheSame(candidateAddress))
        assertFalse(vote.agrees)
    }

    //TODO add a case for when the Candidate has the same Log Size
    @Test
    fun `A Node should vote 'no' if the Candidate Log has fewer entries than the Voting Node Log`() {
        // Given
        val network = Network()

        // Given
        val votingNodeAddress = Source("127.0.0.1", 9001)
        val log = Log(listOf(
            Heartbeat(votingNodeAddress, Destination("127.0.0.1", 9003), 3, ""),
            Heartbeat(votingNodeAddress, Destination("127.0.0.1", 9003), 3, "")
        ))

        val votingNode = StateMachine(
            address = votingNodeAddress,
            name = "NodeA",
            network = network,
            peers = listOf(),
            votedFor = null,
            term = 4,
            role = Role.FOLLOWER,
            log = log,
            termStartedAt = 0
        )

        // When
        /**
         * Both Nodes are in the same Term (4), the Candidate Last Log Term (3)
         * is the same as the Voting Node Last Log Term (3),
         * but the Candidate has fewer Log entries (RequestForVotes with logLength = 1)
         */
        val candidateAddress = Source("127.0.0.1", 9002)
        val requestFromCandidate = RequestForVotes(
            candidateAddress,
            Destination.from(votingNodeAddress),
            4,
            "",
            3,
            1
        )

        val votingNodeWithVote = requestForVotesHandler(votingNode, requestFromCandidate)

        // Then
        // A negative vote is sent
        val vote = votingNodeWithVote.messages.toSend.first()
        assertIs<VoteFromFollower>(vote)
        assertEquals(4, vote.term)
        assertTrue(vote.dest.isTheSame(candidateAddress))
        assertFalse(vote.agrees)
    }

    @Test
    fun `A Node should vote 'no' it has already voted to another Candidate than the Candidate requesting the vote`() {
        // Given
        val network = Network()

        // Given
        val votingNodeAddress = Source("127.0.0.1", 9001)
        val log = Log(listOf(Heartbeat(votingNodeAddress, Destination("127.0.0.1", 9003), 3, "")))

        val votingNode = StateMachine(
            address = votingNodeAddress,
            name = "NodeA",
            network = network,
            peers = listOf(),
            votedFor = Destination("127.0.0.1", 9003),
            term = 4,
            role = Role.FOLLOWER,
            log = log,
            termStartedAt = 0
        )

        // When
        /**
         * Both Nodes are in the same Term (4), the Candidate Last Log Term (3)
         * is the same as the Voting Node Last Log Term (3),
         * the Candidate and Voting Node has the same log size (1),
         * but the Voting Node has Voted
         */
        val candidateAddress = Source("127.0.0.1", 9002)
        val requestFromCandidate = RequestForVotes(
            candidateAddress,
            Destination.from(votingNodeAddress),
            4,
            "",
            3,
            1
        )

        val votingNodeWithVote = requestForVotesHandler(votingNode, requestFromCandidate)

        // Then
        // A negative vote is sent
        val vote = votingNodeWithVote.messages.toSend.first()
        assertIs<VoteFromFollower>(vote)
        assertEquals(4, vote.term)
        assertTrue(vote.dest.isTheSame(candidateAddress))
        assertFalse(vote.agrees)
    }

    @Test
    fun `A Node should vote 'no' if the Request For Vote Term is not as itself's term`() {

    }

    /**
     *
     * A Node should vote 'yes' only if:
     * The Candidate Log has a higher Term than the Log int Voting node;
     *
     * Or the Candidate Log is on the same Term as the Voting Node Log,
     * but it has more entries than the Voting node;
     *
     * The Request For Votes is on the same term as the Voting node;
     *
     */
    @Test
    fun `A Node should vote 'yes'`() {
        // Given
        val network = Network()

        // Given
        val votingNodeAddress = Source("127.0.0.1", 9001)
        val votingNode = StateMachine(
            address = votingNodeAddress,
            name = "NodeA",
            network = network,
            peers = listOf(),
            votedFor = votingNodeAddress,
            term = 4,
            role = Role.FOLLOWER,
            termStartedAt = 0
        )

        // When
        /**
         * A message from a higher term arrives, the Voting Node will have its Term set to the
         * message term, this way the Message Term and the Voting Node term are the
         * same and the logs are in synch, the positive vote happens.
         */
        val candidateAddress = Source("127.0.0.1", 9002)
        val requestFromCandidate = RequestForVotes(
            candidateAddress,
            Destination.from(votingNodeAddress),
            4,
            "",
            3,
            0
        )

        val votingNodeWithVote = requestForVotesHandler(votingNode, requestFromCandidate)

        // Then
        assertEquals(candidateAddress,votingNodeWithVote.votedFor)

        val vote = votingNodeWithVote.messages.toSend.first()
        assertIs<VoteFromFollower>(vote)
        assertEquals(4, vote.term)
        assertTrue(vote.dest.isTheSame(candidateAddress))
        assertTrue(vote.agrees)
    }
}

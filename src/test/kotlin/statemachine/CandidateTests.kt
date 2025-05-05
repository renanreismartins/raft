package statemachine

import org.example.Config
import org.example.Network
import org.example.Source
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.TimeMachine2
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CandidateTests {
    @Test
    fun `Candidate starts a new election if the current election has timed out`() {
        val network = Network()

        // Given
        val candidateAddress = Source("127.0.0.1", 9001)
        val candidate =
            StateMachine(candidateAddress, "NodeA", network = network, peers = listOf(), config = Config(electionTimeout = 5))
                .startElection()

        // When ElectionTimeout is reached
        val timeMachine = TimeMachine2(network, candidate).tick(5)
        val (_, stillCandidate) = timeMachine

        // Then
        assertEquals(Role.CANDIDATE, stillCandidate.role)
        assertEquals(2, stillCandidate.term)
        assertEquals(setOf(candidateAddress), candidate.votesReceived)
    }
}

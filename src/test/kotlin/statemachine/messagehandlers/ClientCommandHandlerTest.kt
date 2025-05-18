package statemachine.messagehandlers

import org.example.ClientCommand
import org.example.Destination
import org.example.Entry
import org.example.Log2
import org.example.Network
import org.example.Source
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import org.example.statemachine.messagehandlers.clientCommandHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClientCommandHandlerTest {

    @Test
    fun `Leader should add Client Command to its Log, Ack its own Log and Replicate the Log`() {
        // Given
        val network = Network()

        val followerAddress = Destination("127.0.0.1", 9002)

        val leaderAddress = Source("127.0.0.1", 9001)
        val leader = StateMachine(
            address = leaderAddress,
            name = "NodeA",
            network = network,
            peers = listOf(followerAddress),
            ackedLength = mapOf(followerAddress to 2),
            term = 1,
            role = Role.LEADER,
            termStartedAt = 4
        )

        val command = ClientCommand(
            Source("127.0.0.1", 9000),
            Destination.from(leaderAddress),
            -1,
            ""
        )

        // When a Leader receives a Client Command
        val leaderWithCommand = clientCommandHandler(leader, command)

        // Then it adds the command to its Log
        assertEquals(
            Log2(listOf(Entry(command.content, 1))),
            leaderWithCommand.log
        )

        // Then it ack itself
        assertEquals(
            mapOf(
                followerAddress to 2,
                Destination.from(leaderAddress) to 1
            ),
            leaderWithCommand.ackedLength
        )

        //TODO check log replication
    }

    @Test
    fun `Non Leader should add Client Command should forward the command to the Leader`() {
        //TODO How followers are notified about the new leader?
    }
}
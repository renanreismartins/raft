package org.example.statemachine.messagehandlers

import org.example.ClientCommand
import org.example.Destination
import org.example.Entry
import org.example.statemachine.RaftLogger
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

fun clientCommandHandler(node: StateMachine, command: ClientCommand): StateMachine {
    //TODO: Ugly Adding the Leader Term to the Client Command.
    val internalCommand = ClientCommand(command.src, command.dest, node.term, command.content)

    if (node.role == Role.LEADER) {
        val newNode = node
            .copy(log = node.log.add(Entry(command.content, node.term)))

        RaftLogger.logInfo(newNode, "Entry appended to the log. Logs: ${newNode.log}")

        //TODO is this doubling the ackedLength? node.ackedLength !!+ (node.ackedLength +....
        val result = newNode.copy(ackedLength = node.ackedLength !!+ (node.ackedLength + (Destination.from(node.address) to newNode.log.size())))
        // val result = newNode.copy(ackedLength = node.ackedLength !!+ (Destination.from(node.address) to newNode.log.size()))

        RaftLogger.logInfo(newNode, "ackedLength updated. ackedLength: ${result.ackedLength}")

        return result.replicateLog()
    } else {
        return node.toSend(internalCommand)
    }

}
package org.example.statemachine.messagehandlers

import org.example.ClientCommand
import org.example.Destination
import org.example.Entry
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

fun clientCommandHandler(node: StateMachine, command: ClientCommand): StateMachine {
    //TODO: Ugly Adding the Leader Term to the Client Command.
    val internalCommand = ClientCommand(command.src, command.dest, node.term, command.content)

    if (node.role == Role.LEADER) {
        val newNode = node
            .copy(log = node.log.add(Entry(command.content, node.term)))

        val result = newNode.copy(ackedLength = node.ackedLength + (node.ackedLength + (Destination.from(node.address) to newNode.log.size())))

        // TODO REPLICATE LOG(leader address, followers)
        return result
    } else {
        return node.toSend(internalCommand)
    }

}
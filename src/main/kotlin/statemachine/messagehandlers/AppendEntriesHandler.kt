package org.example.statemachine.messagehandlers

import org.example.AppendEntries
import org.example.AppendEntriesResponse
import org.example.Destination
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

fun appendEntriesHandler(node: StateMachine, message: AppendEntries): StateMachine {
    val follower = if (message.term > node.term) {
        node.copy(
            term = message.term,
            votedFor = null,
            termStartedAt = null, // Cancel election timeout
            role = Role.FOLLOWER,
            currentLeader = message.src
        )
    } else if (message.term == node.term) {
        node.copy(
            role = Role.FOLLOWER,
            currentLeader = message.src
        )
    } else {
        node
    }

    // TODO Possible refactor: Add this check to the AppendEntries Message type.
    // It is an appropriated type because the application of AppendEntries should no be a
    // reason for the type Log to change, also Demeter Law and testability.
    val logOk = (follower.log.size() >= message.prefixLen)
            && (message.prefixLen == 0 || (follower.log.entries[message.prefixLen - 1].term == message.prefixTerm))

    return if (follower.term == message.term && logOk)
        follower.toSend(
            AppendEntriesResponse(
                follower.address,
                Destination.from(message.src),
                "",
                message.prefixLen + message.entries.size,
                true
            )
        )
    else
        follower.toSend(
            AppendEntriesResponse(
                follower.address,
                Destination.from(message.src),
                "",
                0,
                false
            )
        )
}
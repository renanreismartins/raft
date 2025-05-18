package org.example.statemachine.messagehandlers

import org.example.Destination
import org.example.RequestForVotes
import org.example.VoteFromFollower
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

fun requestForVotesHandler(node: StateMachine, message: RequestForVotes): StateMachine {
    val newNode = if (message.term > node.term) {
        node.copy(
            term = message.term,
            role = Role.FOLLOWER,
            votedFor = null,
            votesReceived = emptySet(), // Not in Kleppmann explanation
            termStartedAt = 0  // Not in Kleppmann explanation
        )
    } else {
        node
    }

    //TODO check if this could be log.prevLogTerm
    //TODO test lastTerm logic
    val lastTerm = if (newNode.log.size() > 0) newNode.log.entries.last().term else 0

    val candidatesLogHasHigherTerm = message.lastLogTerm > lastTerm
    val candidateHasMoreLogEntries = message.lastLogTerm == lastTerm && message.logLength >= newNode.log.size()
    val candidateHasMoreRecentLog = candidatesLogHasHigherTerm || candidateHasMoreLogEntries

    val hasNotVotedForOtherCandidate = newNode.votedFor == null || newNode.votedFor.isTheSame(newNode.address)

    if (message.term == newNode.term && candidateHasMoreRecentLog && hasNotVotedForOtherCandidate) {
        return newNode
            .copy(votedFor = message.src)
            .toSend(VoteFromFollower(newNode.address, Destination.from(message.src), newNode.term, "VOTE FROM FOLLOWER"))
    }

    return newNode.toSend(VoteFromFollower(newNode.address, Destination.from(message.src), newNode.term, "VOTE FROM FOLLOWER", false))
}
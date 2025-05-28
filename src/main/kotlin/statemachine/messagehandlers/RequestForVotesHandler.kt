package org.example.statemachine.messagehandlers

import org.example.Destination
import org.example.RequestForVotes
import org.example.VoteFromFollower
import org.example.statemachine.RaftLogger
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

fun requestForVotesHandler(node: StateMachine, message: RequestForVotes): StateMachine {
    val newNode = if (message.term > node.term) {
        RaftLogger.logInfo(node, "⬇️ Will be demoted to Follower and have the new term: ${message.term}")
        node.copy(
            term = message.term,
            role = Role.FOLLOWER,
            votedFor = null,
            votesReceived = emptySet(), //TODO should not be present in the follower. Not in Kleppmann explanation
            termStartedAt = null, //TODO should not be present in the follower. Not in Kleppmann explanation
        )
    } else {
        node
    }

    //TODO check if this could be log.prevLogTerm
    //TODO test lastTerm logic
    val lastTerm = if (newNode.log.size() > 0) newNode.log.entries.last().term else 0

    val candidatesLogHasHigherTerm = message.lastLogTerm > lastTerm
    val candidateHasMoreLogEntries = message.lastLogTerm == lastTerm && message.logLength >= newNode.log.size()
    //TODO candidateHasMoreRecentLog -> LogsAreInSync
    val candidateHasMoreRecentLog = candidatesLogHasHigherTerm || candidateHasMoreLogEntries

    //TODO hasNotVotedForOtherCandidate -> hasNotVotedForOtherNode
    // TODO encapsulate vote check
    val hasNotVotedForOtherCandidate = newNode.votedFor == null || newNode.votedFor.isTheSame(newNode.address)
    val candidateAndFollowerAreSameTerm = message.term == newNode.term

    RaftLogger.debug(newNode, "Candidate Log has a greater term than in the message (${message.logLength} > $lastTerm)", candidatesLogHasHigherTerm)
    RaftLogger.debug(newNode, "Candidate Log is in the same term as the Follower and have at least the same number of Log Entries", candidateHasMoreLogEntries)
    RaftLogger.debug(newNode, "Candidate has a more Recent Log than the Follower", candidateHasMoreRecentLog)
    RaftLogger.debug(newNode, "Follower has not yet voted to another Node", hasNotVotedForOtherCandidate)
    RaftLogger.debug(newNode, "Candidate and Follower are on the same Term ", candidateAndFollowerAreSameTerm)

    if (candidateAndFollowerAreSameTerm && candidateHasMoreRecentLog && hasNotVotedForOtherCandidate) {
        RaftLogger.logInfo(newNode, "voted positive to ${message.src}")

        return newNode
            .copy(votedFor = message.src)
            .toSend(VoteFromFollower(newNode.address, Destination.from(message.src), newNode.term, "VOTE FROM FOLLOWER"))
    }

    RaftLogger.logInfo(newNode, "voted negative to ${message.src}")

    return newNode.toSend(VoteFromFollower(newNode.address, Destination.from(message.src), newNode.term, "VOTE FROM FOLLOWER", false))
}
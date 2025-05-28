package org.example.statemachine.messagehandlers

import org.example.AppendEntries
import org.example.AppendEntriesResponse
import org.example.Destination
import org.example.statemachine.RaftLogger
import org.example.statemachine.Role
import org.example.statemachine.StateMachine
import kotlin.math.min

fun appendEntriesHandler(node: StateMachine, message: AppendEntries): StateMachine {
    val follower = if (message.term > node.term) {
        RaftLogger.logInfo(node, "⬇️ Will be demoted to Follower and have the new term: ${message.term}")
        RaftLogger.logInfo(node, "Will have its vote reset and a new currentLeader ${message.src}")

        node.copy(
            term = message.term,
            votedFor = null,
            termStartedAt = null, // Cancel election timeout
            role = Role.FOLLOWER,
            currentLeader = message.src
        )
    } else if (message.term == node.term) {
        RaftLogger.logInfo(node, "Received an AppendEntries with the same Term as its own Term")
        RaftLogger.logInfo(node, "⬇️ Will be demoted to Follower and have a new currentLeader ${message.src}\"")

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

    RaftLogger.logInfo(follower, "🪵 Follower and Leader are on the same Term", follower.term == message.term)
    RaftLogger.logInfo(follower, "🪵 Follower Log ok", logOk)

    return if (follower.term == message.term && logOk) {
        val nodeWithEntries = appendEntries(follower, message)
        val ack = message.prefixLen + message.entries.size

        RaftLogger.logInfo(follower, "📝 Entries successfully appended. Ack: $ack")
        nodeWithEntries.toSend(
            AppendEntriesResponse(
                follower.address,
                Destination.from(message.src),
                follower.term, //TODO this works because term was not changed in appendEntries
                "",
                ack,
                true
            )
        )

    } else {
        RaftLogger.logInfo(follower, "📝 Entries rejected")
        follower.toSend(
            AppendEntriesResponse(
                follower.address,
                Destination.from(message.src),
                follower.term,
                "",
                0,
                false
            )
        )
    }
}

fun appendEntries(follower: StateMachine, message: AppendEntries): StateMachine {
    RaftLogger.logInfo(follower, "📝 Appending Entries")
    RaftLogger.logInfo(follower, "No Entries in the message", message.entries.isEmpty())
    val followerWithTruncatedLog = if (message.entries.size > 0 && follower.log.size() > message.prefixLen) {

        RaftLogger.logInfo(follower, "Follower has more entries than prefixLen")

        val index = min(follower.log.size(), message.prefixLen + message.entries.size) - 1

        if (follower.log.entries[index].term != message.entries[index - message.prefixLen].term) {
            val truncatedEntries = follower.log.entries.slice(0 .. message.prefixLen - 1)

            RaftLogger.logInfo(follower, "Follower will have the Log Entries truncated to: $truncatedEntries")

            //OR follower.log.entries.slice(0 until message.prefixLen)
            follower.copy(log = follower.log.copy(entries = truncatedEntries))
        } else {
            follower
        }
    } else {
        follower
    }

    val followerWithAppendedSuffix = if (message.prefixLen + message.entries.size > followerWithTruncatedLog.log.size()) {
        val startSuffix = followerWithTruncatedLog.log.size() - message.prefixLen
        val endSuffix = message.entries.size - 1
        val suffixEntries = message.entries.slice(startSuffix .. endSuffix)
        val logWithAppendedSuffix = followerWithTruncatedLog.log.copy(entries = followerWithTruncatedLog.log.entries + suffixEntries)

        RaftLogger.logInfo(followerWithTruncatedLog, "Suffix appended to the Log $logWithAppendedSuffix")

        followerWithTruncatedLog.copy(log = logWithAppendedSuffix)
    } else {
        RaftLogger.logInfo(followerWithTruncatedLog, "Suffix not appended to the Log")
        followerWithTruncatedLog
    }

    val followerWithCommitedEntries = if (message.commitLength > follower.commitLength) {
        val range = follower.commitLength..message.commitLength - 1
        val commitedEntries = followerWithAppendedSuffix.log.entries.slice(range)

        //TODO: Deliver commitedEntries to the APP.. put this in a collection and then call Callback?

        RaftLogger.logInfo(followerWithAppendedSuffix, "Entries Commited. commitLength: ${message.commitLength}")

        followerWithAppendedSuffix.copy(commitLength = message.commitLength)
    } else {
        RaftLogger.logInfo(followerWithAppendedSuffix, "No entries Commited")
        followerWithAppendedSuffix
    }

    return followerWithCommitedEntries
}
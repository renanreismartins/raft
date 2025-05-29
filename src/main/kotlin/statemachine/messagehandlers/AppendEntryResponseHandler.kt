package org.example.statemachine.messagehandlers

import org.example.AppendEntriesResponse
import org.example.Destination
import org.example.statemachine.RaftLogger
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

//TODO appendEntryResponseHandler appendEntriesResponseHandler
fun appendEntryResponseHandler(node: StateMachine, message: AppendEntriesResponse): StateMachine {
    if (message.term > node.term) {
        return node.copy(
            term = message.term,
            role = Role.FOLLOWER,
            votedFor = null,
            termStartedAt = null
        )
    }

    val followerAddress = Destination.from(message.src)
    if (message.term == node.term) {
        // Ack was successful
        if (message.success && message.ack >= node.ackedLength!!.getValue(followerAddress)) {

            RaftLogger.logInfo(node, "Entries acked by follower")

            val nodeWithResponse = commitLogEntries(
                node.copy(
                    sentLength = node.sentLength!! + (followerAddress to message.ack),
                    ackedLength = node.ackedLength + (followerAddress to message.ack)
                )
            )

            RaftLogger.logInfo(nodeWithResponse, "Leader with the AppendEntriesResponse: $nodeWithResponse")

            return nodeWithResponse

        } else if (node.sentLength!!.getValue(followerAddress) > 0) {
            /**
             * TODO this will generate a replicate log request (AppendEntries) for each follower,
             * making the communication extremely chatty and delaying the processing
             * of the initial AppendEntries request, because it will trigger nested
             * calls and have to wait for the sync of all nodes.
             *
             * Another option could be to replicate the log only to the Follower that is
             * sending this response. What is not viable, because the other Nodes will not
             * receive a message, triggering an election.
             *
             * The correct fix is to have a heartbeat timeout for each follower,
             * it could be derived from the Sent Messages or a Map<Follower, Timestamp>
             * and then send the Log replication to the responding Follower.
             *
             */
            return node.copy(
                sentLength =
                    node.sentLength + (followerAddress to node.sentLength.getValue(followerAddress) - 1)
            ).replicateLog()
        }
    }

    return node
}

fun commitLogEntries(leader: StateMachine): StateMachine {
    val logIndicesToBeCommited = (leader.commitLength until leader.log.size())
        .filter { commitLength ->
            commitLengthHasQuorum(
                commitLength,
                leader
            )
        }

    return if (logIndicesToBeCommited.isEmpty())
        leader
    else {
        //TODO deliver logs to the application
        val commitLength = logIndicesToBeCommited.last() + 1

        RaftLogger.logInfo(leader, "Entries commited: $commitLength")

        leader.copy(commitLength = commitLength)
    }

}

fun commitLengthHasQuorum(commitLength: Int, leader: StateMachine): Boolean {

    RaftLogger.logInfo(leader, "commitLength: $commitLength / ackedLength: ${leader.ackedLength}")

    val numberOfNodesAckedTheCommitLength = leader.ackedLength!!
        .filter { it.value > commitLength }
        .count()

    RaftLogger.logInfo(leader, "Nr Nodes acked the commitLength: $numberOfNodesAckedTheCommitLength")

    val hasQuorum = numberOfNodesAckedTheCommitLength >= ((leader.config.clusterSize + 1) / 2)

    RaftLogger.logInfo(leader, "Has Quorum: $hasQuorum")

    return hasQuorum
}
package org.example.statemachine.messagehandlers

import org.example.Destination
import org.example.VoteFromFollower
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

fun voteHandler(node: StateMachine, vote: VoteFromFollower): StateMachine {
    //TODO CHECK ALL PLACES THE ROLE IS CHANGED AND RESET VARIABLES (TIMERS, VOTED FOR, ETC)
    if (vote.term > node.term) {
        return node.copy(
            term = vote.term,
            role = Role.FOLLOWER,
            votedFor = null,
            votesReceived = emptySet(), // Not in Kleppmann explanation, Follower will not have this attribute when demoted.
            termStartedAt = null  // Not in Kleppmann explanation
        )
    }

    if (node.role == Role.CANDIDATE && vote.term == node.term && vote.agrees) {
        val nodeWithVote = node
            .copy(votesReceived = node.votesReceived.plus(vote.src))

        if (nodeWithVote.votesReceived.size >= (nodeWithVote.config.clusterSize + 1) / 2) {
            val leader = nodeWithVote
                .copy(role = Role.LEADER)
                .copy(termStartedAt = null) // Cancel election timeout
                .copy(sentLength = nodeWithVote.peers.associateWith { nodeWithVote.log.size() })

                /**
                 * The ackedLength is where the Leader records the number of Log Entries the Followers
                 * have confirmed they have received. The Leader also self-acknowledge its entries
                 * when it receives a command from the client.
                 * This Index can be as big as the size of the Log.
                 *
                 */
                .copy(ackedLength = nodeWithVote.peers.associateWith { 0 })

            return leader.replicateLog()
        }

        return nodeWithVote
    }

    return node
}
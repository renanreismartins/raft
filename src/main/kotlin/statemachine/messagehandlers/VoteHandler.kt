package org.example.statemachine.messagehandlers

import org.example.VoteFromFollower
import org.example.statemachine.CLUSTER_SIZE
import org.example.statemachine.Role
import org.example.statemachine.StateMachine

fun voteHandler(node: StateMachine, vote: VoteFromFollower): StateMachine {
    if (vote.term > node.term) {
        return node.copy(
            term = vote.term,
            role = Role.FOLLOWER,
            votedFor = null,
            votesReceived = emptySet(), // Not in Kleppmann explanation, Follower will not have this attribute when demoted.
            termStartedAt = 0  // Not in Kleppmann explanation
        )
    }

    if (node.role == Role.CANDIDATE && vote.term == node.term && vote.agrees) {
        val nodeWithVote = node
            .copy(votesReceived = node.votesReceived.plus(vote.src))
            .copy(termStartedAt = 0)
        // TODO if needed: current leader = node.address

        if (nodeWithVote.votesReceived.size >= (CLUSTER_SIZE + 1) / 2) {
            val leader = nodeWithVote
                .copy(role = Role.LEADER)
                .copy(setLength = nodeWithVote.peers.associateWith { nodeWithVote.log.size() })
                .copy(ackedLength = nodeWithVote.peers.associateWith { 0 })
                .copy(termStartedAt = 0)

            // TODO REPLICATE LOG(leader address, followers)
            return leader
        }

        return nodeWithVote
    }

    return node
}
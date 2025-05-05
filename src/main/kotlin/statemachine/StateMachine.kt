package org.example.statemachine

import org.example.Address
import org.example.Config
import org.example.Destination
import org.example.Log
import org.example.Message
import org.example.Messages
import org.example.Network
import org.example.ReceivedMessage
import org.example.RequestForVotes
import org.example.Source

enum class Role {
    FOLLOWER,
    CANDIDATE,
    LEADER
}

data class StateMachine(
    val address: Source,
    val name: String,
    val state: Int = 0,
    val network: Network,
    val peers: List<Destination>,
    val votedFor: Address? = null,
    val messages: Messages = Messages(),
    val log: Log = Log(),
    val term: Int = 0,
    val config: Config = Config(),
    val commitIndex: Int = 0,
    val lastApplied: Int = 0,
    val role: Role = Role.FOLLOWER,
    // <Candidate>
    // this was derived from the received messages. See: shouldBecomeLeader() in the old impl
    val votesReceived: Set<Address> = emptySet(),
    val termStartedAt: Int? = null,
    // </Candidate>
) {

    fun tickWithoutSideEffects(): StateMachine {
        val tickMessages = network.get(this.address)
        val machine = tickMessages
            .fold(this)
            { machine, message -> machine.add(message.toReceived()) }

        if (role == Role.FOLLOWER && communicationTimedOut()) return machine.startElection()
        if (role == Role.CANDIDATE && hasReachedElectionTimeout()) return machine.startElection()

        return machine
    }

    fun tick(): StateMachine {
        val machine = tickWithoutSideEffects()

        machine.messages.toSend.forEach { send(it) } // TODO encapsulate access to messages?
        return machine.flushMessages()
    }

    // <Follower>
    private fun communicationTimedOut(): Boolean {
        // On system startup, Follower hasn't received any messages and should promote itself after electionTimeout
        val hasReachedFirstTimeoutAfterStartup = received().isEmpty() && network.clock >= config.electionTimeout
        // During normal operation of the system, Follower hasn't received messages in heartbeatTimeout period and should promote itself
        val hasReachedTimeoutWithLastMessage =
            received().isNotEmpty() && network.clock - received().last().receivedAt >= config.heartbeatTimeout
        return hasReachedFirstTimeoutAfterStartup || hasReachedTimeoutWithLastMessage
    }

    // <Candidate>
    fun hasReachedElectionTimeout(): Boolean = network.clock - termStartedAt!! >= config.electionTimeout
    // </Candidate>

    // <Candidate and Follower>
    fun startElection(): StateMachine {
        //TODO check if this could be log.prevLogTerm
        //TODO test lastTerm logic
        val lastTerm = if (log.size() > 0) log.messages.last().term else 0
        val requestForVotes = peers.map { peer -> RequestForVotes(this.address, peer, term, "REQUEST FOR VOTES", lastTerm) }

        return this.copy(
            role = Role.CANDIDATE,
            term = term + 1,
            votedFor = address,
            votesReceived = setOf(address),
            messages = messages.toSend(requestForVotes),
            termStartedAt = network.clock
        )
    }
    // </Candidate and Follower>

    fun received() = messages.received

    fun add(vararg message: ReceivedMessage): StateMachine =
        this.copy(messages = messages.copy(received = received() + message))

    fun Message.toReceived(): ReceivedMessage =
        ReceivedMessage(this, network.clock)

    fun send(message: Message) {
        network.add(message)
    }

    private fun flushMessages(): StateMachine =
        this.copy(messages = messages.flush(network.clock))
}
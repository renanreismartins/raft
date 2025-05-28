package org.example.statemachine

import org.example.Address
import org.example.AppendEntry
import org.example.AppendEntries
import org.example.AppendEntriesResponse
import org.example.AppendEntryResponse
import org.example.ClientCommand
import org.example.Config
import org.example.Destination
import org.example.Heartbeat
import org.example.Log2
import org.example.Message
import org.example.Messages
import org.example.Network
import org.example.ReceivedMessage
import org.example.RequestForVotes
import org.example.Source
import org.example.VoteFromFollower
import org.example.statemachine.messagehandlers.appendEntriesHandler
import org.example.statemachine.messagehandlers.requestForVotesHandler
import org.example.statemachine.messagehandlers.voteHandler

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
    val log: Log2 = Log2(),
    val term: Int = 0,
    val config: Config = Config(),
    val commitLength: Int = 0,
    val lastApplied: Int = 0,
    val role: Role = Role.FOLLOWER,
    val currentLeader: Address? = null,
    // <Candidate>
    // this was derived from the received messages. See: shouldBecomeLeader() in the old impl
    val votesReceived: Set<Address> = emptySet(),
    val termStartedAt: Int? = null, // TODO call it Election Clock?
    // </Candidate>

    // <Leader>
    //TODO cancel values in case of demotion
    // These could be encapsulated collections that would accept
    // an Address in a method to return its value.
    // This sentLength is called nextIndex in the paper
    val sentLength: Map<Destination, Int>? = null,

    val sentHeartbeatAt: Int? = null,

    // This ackedLength is called matchIndex in the paper
    val ackedLength: Map<Destination, Int>? = null,
    // </Leader>
) {

    /*
    TODO ADR this method allows unit testing, after a unit of time (tick)
    passes the state of the Node can be verified without the interaction
    with the other Nodes and network.
    */
    fun tickWithoutSideEffects(): StateMachine {
        val tickMessages = network.get(this.address)
        val nodeAfterProcessing = tickMessages
            .fold(this)
            { machine, message ->
                when (message) {
                    is RequestForVotes -> requestForVotesHandler(machine, message)
                    is AppendEntry -> TODO()
                    is AppendEntryResponse -> TODO()
                    is ClientCommand -> TODO()
                    is Heartbeat -> this //TODO
                    is VoteFromFollower -> voteHandler(machine, message)
                    is AppendEntries -> appendEntriesHandler(machine, message)
                    is AppendEntriesResponse -> this //TODO
                }.add(message.toReceived())
            }

        if (nodeAfterProcessing.role == Role.FOLLOWER && nodeAfterProcessing.communicationTimedOut()) return nodeAfterProcessing.startElection()
        if (nodeAfterProcessing.role == Role.CANDIDATE && nodeAfterProcessing.hasReachedElectionTimeout()) return nodeAfterProcessing.startElection()
        if (nodeAfterProcessing.role == Role.LEADER && nodeAfterProcessing.hasHeartbeatTimedOut()) return nodeAfterProcessing.replicateLog()

        return nodeAfterProcessing
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
            received().isNotEmpty() && network.clock - received().last().receivedAt >= config.electionTimeout

        RaftLogger.logInfo(this, "Reached first timeout after startup", hasReachedFirstTimeoutAfterStartup)
        RaftLogger.logInfo(this, "Reached timeout since the last message", hasReachedTimeoutWithLastMessage)

        return hasReachedFirstTimeoutAfterStartup || hasReachedTimeoutWithLastMessage
    }
    // <//Follower>

    // <Candidate>
    fun hasReachedElectionTimeout(): Boolean {
        val hasTimedOut = network.clock - termStartedAt!! >= config.electionTimeout

        RaftLogger.logInfo(this, "Reached election timeout", hasTimedOut)

        return hasTimedOut
    }

    fun voteForItself(): StateMachine {
        val nodeWithVote = this.copy(
            votedFor = this.address,
            votesReceived = this.votesReceived.plus(this.address)
        )

        RaftLogger.logInfo(this, "Voted for itself. Votes Received ${nodeWithVote.votesReceived}")

        return nodeWithVote
    }
    // </Candidate>

    // <Candidate and Follower>
    fun startElection(): StateMachine {
        val newTerm = term + 1

        RaftLogger.logInfo(this, "Election started. New Term: $newTerm")

        return this.copy(
            role = Role.CANDIDATE,
            term = newTerm,
            termStartedAt = network.clock
        )
        .voteForItself()
        .requestVotes()
    }

    fun requestVotes(): StateMachine {
        //TODO check if this could be log.prevLogTerm
        //TODO test lastTerm logic
        val lastTerm = if (log.size() > 0) log.entries.last().term else 0
        val requestForVotes =
            peers.map { peer -> RequestForVotes(this.address, peer, term, "REQUEST FOR VOTES", lastTerm, log.size()) }

        RaftLogger.logInfo(this, "Requested Votes: $requestForVotes")

        return this.copy(messages = messages.toSend(requestForVotes))
    }
    // </Candidate and Follower>

    // <Leader>
    fun hasHeartbeatTimedOut(): Boolean {
        val hasTimeout = network.clock - sentHeartbeatAt!! >= config.heartbeatTimeout

        RaftLogger.logInfo(this, "Reached heartbeat timeout", hasTimeout)

        return hasTimeout
    }
    // </Leader>

    fun toSend(message: Message): StateMachine {
        RaftLogger.logInfo(this, "New Message to send: $message")

        return this.copy(messages = messages.toSend(message))
    }

    fun received() = messages.received

    //TODO rename message -> messages
    fun add(vararg message: ReceivedMessage): StateMachine {
        message.forEach {
            RaftLogger.logInfo(this, "New Message received: $it")
        }

        return this.copy(messages = messages.copy(received = received() + message))
    }

    fun Message.toReceived(): ReceivedMessage =
        ReceivedMessage(this, network.clock)

    fun send(message: Message) {
        network.add(message)
    }

    private fun flushMessages(): StateMachine =
        this.copy(messages = messages.flush(network.clock))

    fun replicateLog(): StateMachine {
        RaftLogger.logInfo(this, "[Log replication initiated]")

        val entries: List<AppendEntries> = peers.map { replicateLog(it) }
        // TODO add toSend(List<Message>) instead fo the fold
        val nodeWithEntriesToSend =  entries
            .fold(this) { acc, t -> acc.toSend(t) }
            .copy(sentHeartbeatAt = network.clock) // TODO resetHeartbeatClock()

        RaftLogger.logInfo(this, "[Log replication ended]")

        return nodeWithEntriesToSend
    }

    fun replicateLog(follower: Destination): AppendEntries {
        //TODO sentLength was already set in the election, verify if defensive check (default value) is needed here
        val prefixLen = sentLength!!.getOrDefault(follower, 0)

        val suffix = log.entries.slice(prefixLen until log.size())

        // Term of the last prefix entry or 0
        // - 1 because of 0 index nature of the List
        //TODO encapsulate this behaviour eg getLastTermForFollower()
        val prefixTerm =
            if (prefixLen > 0) log.entries[prefixLen - 1].term
            else 0

        return AppendEntries(
            this.address,
            follower,
            this.term,
            prefixLen,
            prefixTerm,
            commitLength,
            suffix
        )

    }
}
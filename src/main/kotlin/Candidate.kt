package org.example

data class Candidate(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val messages: Messages = Messages(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers) {

    // TODO Make process return Node, so we have finer control over how we handle each message
    //      e.g. If a Candidate receives a Heartbeat, it should demote to follower, this is difficult
    //      with the current architecture. This would allow us to remove methods to demote/promote

    override fun handleMessage(message: Message): Node {
        return when(message) {
            is Heartbeat -> copy(state = (state + message.content.toInt())).demote()
            is RequestForVotes -> this
            is VoteFromFollower -> {
                if (shouldBecomeLeader()) {
                    val heartbeats = peers.map { peer -> Heartbeat(address, peer, "0") }
                    return promote().toSend(heartbeats)
                }
                return this
            }
        }
    }

    override fun tickWithoutSideEffects(): Node {
        //TODO We have this common logic in the Follower, move it to Node
        val tickMessages = network.get(this.address)

        return tickMessages.fold(this as Node) { node, msg ->
            node.process(msg)
        }
    }

    fun shouldBecomeLeader(): Boolean {
        println("should become leader")
        //TODO + 1 represents the Vote for Self, do we want to add it to the MessageLogEntry and remove it from here
        return received().count { m -> m.message is VoteFromFollower } + 1 > clusterSize() / 2
    }

    private fun clusterSize(): Int  {
        return peers.size + 1
    }

    //TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    //TODO It seems the 'received =' can be removed as Kotlin can infer the type
    override fun add(vararg message: ReceivedMessage): Candidate = this.copy(messages = messages.copy(received = received() + message))
    override fun add(vararg message: SentMessage): Candidate = this.copy(messages = messages.copy(sent = sent() + message))

    private fun promote(): Leader {
        return Leader(this.address, this.name, this.state, this.network, this.peers, this.messages, this.config)
    }

    private fun demote(): Follower {
        return Follower(this.address, this.name, this.state, this.network, this.peers, this.messages, this.config)
    }
}

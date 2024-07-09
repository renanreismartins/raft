package org.example

data class Candidate(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val received: List<ReceivedMessage> = emptyList(),
    override val sent: List<SentMessage> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, received) {

    // TODO Make process return Node, so we have finer control over how we handle each message
    //      e.g. If a Candidate receives a Heartbeat, it should demote to follower, this is difficult
    //      with the current architecture. This would allow us to remove methods to demote/promote
    override fun process(accumulatedToSend: List<Message>, received: Message): Pair<Node, List<Message>> {
        val (n, newToSend) = when(received) {
            is Heartbeat -> (this.copy(state = (state + received.content.toInt())) to emptyList<Message>())
            is RequestForVotes -> (this to emptyList())
            is VoteFromFollower -> (this to emptyList())
        }

        val n2 = n.copy(received = n.received + ReceivedMessage(received, network.clock), sent = n.sent + newToSend.map { SentMessage(it, network.clock) } )
        return n2 to accumulatedToSend + newToSend
    }

    override fun tick(): Node {
        val (node, messages) = tickWithoutSideEffects()
        messages.forEach { send(it) }
        return node
    }

    override fun tickWithoutSideEffects(): Pair<Node, List<Message>> {
        //TODO We have this common logic in the Follower, move it to Node
        val tickMessages = network.get(this.address)

        val (node, messagesToSend) = tickMessages.fold(this as Node to emptyList<Message>()) { (n, messages), msg ->
            n.process(messages, msg)
        }

        val newNode = node as Candidate
        if (node.shouldDemoteToFollower()) {
            return node.demote() to messagesToSend
        }

        if (node.shouldBecomeLeader()) {
            val leader = newNode.promote()
            val heartbeats = peers.map { peer -> Heartbeat( leader.address, peer, "0") }
            //TODO most of the time when we have a call to the 'copy' method passing only one param, it is most likely we
            //want to have a 'business' method as add().
            //This would avoid bugs of passing the wrong values as in:
            //leader.copy(sent = sent + heartbeats...) instead of leader.copy(sent = leader.sent + heartbeats....)
            return leader.copy(sent = leader.sent + heartbeats.map { SentMessage(it, network.clock) }) to messagesToSend + heartbeats
        }

        return node to messagesToSend
    }

    fun shouldBecomeLeader(): Boolean {
        //TODO + 1 represents the Vote for Self, do we want to add it to the MessageLogEntry and remove it from here
        return received.count { m -> m.message is VoteFromFollower } + 1 > clusterSize() / 2
    }

    // TODO We need to make sure that this Heartbeat comes from the real Leader (check term index?)
    //      and we aren't receiving Heartbeats from any other source
    private fun shouldDemoteToFollower(): Boolean {
        return received.any { it.message is Heartbeat }
    }

    private fun clusterSize(): Int  {
        return peers.size + 1
    }

    //TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    private fun promote(): Leader {
        return Leader(this.address, this.name, this.state, this.network, this.peers, this.received, this.sent, this.config)
    }

    private fun demote(): Follower {
        return Follower(this.address, this.name, this.state, this.network, this.peers, this.received, this.sent, this.config)
    }
}
package org.example


//TODO maybe we should make all the constructors except the Followe as private
// this way all nodes can only be initialized as Follower and only transition to a new
// state thought the state machine
data class Leader(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val received: List<ReceivedMessage> = emptyList(), // TODO: Make a map <Destination, List<ReceivedMessage>>
    override val sent: List<SentMessage> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, received) {

    override fun process(accumulatedToSend: List<Message>, received: Message): Pair<Node, List<Message>> {
        val (n, newToSend) = when(received) {
            is Heartbeat -> (this.copy(state = state + received.content.toInt()) to accumulatedToSend)
            is RequestForVotes -> (this to accumulatedToSend)
            is VoteFromFollower -> (this to accumulatedToSend)
        }

        val n2 = n.copy(received = n.received + ReceivedMessage(received, network.clock), sent = n.sent + newToSend.map { SentMessage(it, network.clock) } )
        return n2 to accumulatedToSend + newToSend
    }

    // 1. Process messages, create messages to send
    // 2. THEN, check sent messages to see which peers we need to send a heartbeat to
    // 3.

    override fun tick(): Node {
        val (node, messages) = tickWithoutSideEffects()
        messages.forEach { send(it) }
        return node
    }

    override fun tickWithoutSideEffects(): Pair<Node, List<Message>> {
        val tickMessages = network.get(this.address)

        val (node, responses) = tickMessages.fold(this as Node to emptyList<Message>()) { (n, messages), msg ->
            n.process(messages, msg)
        }

        val nodesWeHaveSentMessagesTo =
            sent.filter { it.sentAt > network.clock - config.heartbeatTimeout }
                .map { it.message.dest } + responses.map { it.dest }

        val nodesWeNeedToSendHeartbeatTo = peers.toSet() - nodesWeHaveSentMessagesTo.toSet()

        val heartbeats = nodesWeNeedToSendHeartbeatTo.map { Heartbeat(address, it, "0") }

        return (node as Leader).copy(sent = node.sent + heartbeats.map { SentMessage(it, network.clock) }) to responses + heartbeats
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }
}

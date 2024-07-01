package org.example

data class Leader(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val received: List<MessageLogEntry> = emptyList(),
    override val sent: List<MessageLogEntry> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, received) {
    override fun tick(): Node {
        TODO("Not yet implemented")
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

}

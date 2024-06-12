package org.example

data class Leader(
    override val address: Address,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Address>,
    override val messages: List<MessageLogEntry> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, messages) {
    override fun tick(): Node {
        TODO("Not yet implemented")
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

}

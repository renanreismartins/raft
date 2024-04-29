package org.example

typealias NetworkDelay = Int
typealias DeliveryTime = Int

data class NetworkMessage(val message: Message, val deliveryTime: DeliveryTime)

class Network(initialMessages: Map<Address, List<NetworkMessage>> = emptyMap()) {
    var clock = 0
    private val messages = initialMessages.toMutableMap()

    fun get(address: Address): List<Message> {
        // get all messages for address with 0 delay and store
        val (messagesToDeliver, remaining) = messages[address]
            ?.partition { (_, deliveryTime) ->
                deliveryTime <= clock
            } ?: (listOf<NetworkMessage>() to listOf())

        messages[address] = remaining

        // return store messages
        return messagesToDeliver.map { it.message }
    }

    fun add(message: Message, delay: NetworkDelay = 0) {
        val networkMessage = NetworkMessage(message, clock + 1 + delay)
        messages[message.dest] = messages[message.dest]?.plus(networkMessage) ?: listOf(networkMessage)
    }

    fun tick(ticks: Int = 1): Network {
        clock += ticks
        return this
    }
}
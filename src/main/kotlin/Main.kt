package org.example

fun main() {
    println("Hello World!")
}


val network = mutableMapOf<Address, List<Message>>()
data class Message(val src: Address, val dest: Address, val content: String)

data class Address(val host: String, val port: Int)

abstract class Node(open val address: Address, open val name: String, open val clock: Int = 0, open val state: Int) {
    abstract fun tick(): Node
    abstract fun receive(message: Message): Node
    abstract fun send(address: Address, content: String)
}

data class Follower(override val address: Address,
                    override val name: String,
                    override val clock: Int = 0,
                    override val state: Int = 0) :Node(address, name, clock, state) {

    //TODO: This has to be moved to the Node class
    override fun tick(): Follower {
        val messages = receiveMessages()

        val newState = messages.fold(state) { acc, msg ->
            msg.content.toInt() + acc
        }

        return this.copy(clock =  clock + 1, state = newState)
    }

    private fun receiveMessages(): List<Message> {
        val messages = network.getOrDefault(this.address, emptyList())
        network[this.address] = emptyList()
        return messages
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    override fun send(destination: Address, content: String) {
        val message = Message(this.address, destination, content) // TODO add tiny type Source and Destination
        val messages = network.getOrDefault(destination, emptyList()) + message
        network[destination] = messages
    }
}

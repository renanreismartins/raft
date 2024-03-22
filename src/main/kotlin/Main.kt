package org.example

fun main() {
    println("Hello World!")
}

data class Message(val src: Address, val dest: Address, val content: String)

data class Address(val host: String, val port: Int)

abstract class Node(open val address: Address, open val name: String, open val clock: Int = 0) {
    abstract fun tick(): Node
    abstract fun receive(message: Message): Node
    abstract fun send(dest: Node, content: String)
}

data class Follower(override val address: Address, override val name: String, override val clock: Int) : Node(address, name, clock) {

    //TODO: This has to be moved to the Node class
    override fun tick(): Follower {
        return this.copy(clock =  clock + 1)
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    override fun send(dest: Node, content: String) {
        val message = Message(this.address, dest.address, content)
        //TODO We need to decide how to pass the message. Queue and receive behaviour
    }
}

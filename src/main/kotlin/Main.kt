package org.example

import java.net.InetAddress

fun main() {
    println("Hello World!")
}

data class Message(val dest: InetAddress, val src: InetAddress)

data class Address(val host: String, val port: Int)

interface Node {
   fun tick() : Pair<Node,List<Message>>;
   fun receive(message: Message) : Node;
}

data class Follower(val address: Address, val name: String) : Node {
    val queue = Queue()


    fun tick() {
       this.send(RforVotes(n1, n2, n3))
    }


}


nodes.forEach { n ->
  val (new, message) = n.tick
  messages.
}
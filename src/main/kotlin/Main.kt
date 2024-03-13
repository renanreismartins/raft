package org.example

import java.net.InetAddress

fun main() {
    println("Hello World!")
}

data class Message(val dest: InetAddress, val src: InetAddress)

data class Address(val host: String, val port: Int)

data class Node(val address: Address, val name: String) {

}
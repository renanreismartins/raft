package org.example

sealed class Address(open val host: String, open val port: Int)

data class Source(override val host: String, override val port: Int) : Address(host, port)

data class Destination(override val host: String, override val port: Int) : Address(host, port) {
    companion object {
        fun from(src: Address): Destination {
            return Destination(src.host, src.port)
        }

        fun from(src: List<Source>): List<Destination> {
            return src.map { it -> Destination(it.host, it.port) }
        }
    }
}

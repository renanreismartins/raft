import org.example.Address
import org.example.Node
import java.net.InetAddress

class NodeTests {

// one node cluster
    // message arriving in one-node system
    fun `On initialisation, node will start as follower`() {
    }

    fun `Node will start an election`() {
    }

    fun `Creates a RequestVote message`() {
    }


    fun `Node A can send a message to Node B`() {
        val a = Node(Address("127.0.0.1", 9000), "A")
        val electionStarted = a.startElection();

        val b = Node(Address("127.0.0.1", 9001), "A")
    }
}
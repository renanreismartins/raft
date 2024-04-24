import org.example.Address
import org.example.Candidate
import org.example.Follower
import org.example.Network
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeTests {
    @Test
    fun `A Node can send a message to another Node`() {
        // Given
        val network = Network()
        val nodeA = Follower(Address("127.0.0.1", 9001), "NodeA", network = network)
        val nodeB = Follower(Address("127.0.0.1", 9002), "NodeB", network = network)

        // When
        nodeA.send(nodeB.address, "1")
        network.tick()
        val newNodeB = nodeB.tick()

        // Then
        assertEquals(1, newNodeB.state)
    }

    @Test
    fun `Follower becomes a Candidate if it does not receive a heartbeat before the election timeout (3 ticks)`() {
        // Given
        val network = Network()
        val follower = Follower(Address("127.0.0.1", 9001), "NodeA", network = network)

        // When
        network.tick()
        val f1 = follower.tick()

        network.tick()
        val f2 = f1.tick()

        network.tick()
        val f3 = f2.tick()

        network.tick()
        val candidate = f3.tick()

        assertTrue(candidate is Candidate)
    }

//    fun `Follower stays as a follower if it receives a heartbeat before the election timeout (3 ticks)`() {
//        // Given
//        val follower = Follower(Address("127.0.0.1", 9001), "NodeA")
//
//        // When
//        val stillFollower = follower.tick().tick().tick() // It is still a 'follower' instead of a candidate
//
//        assertTrue(stillFollower is Follower)
//    }

    /*
       fun `Follower moves to Candidate after election timeout (3 ticks)`() {
           val follower = Follower(Address("127.0.0.1", 9001), "NodeA")
           follower.tick()
           follower.tick()
           follower.tick()
           follower == Candidate
       }

       fun `Creates a RequestVote message`() {
       }


       fun `Node A can send a message to Node B`() {
           val a = Node(Address("127.0.0.1", 9000), "A")
           val electionStarted = a.startElection();

           val b = Node(Address("127.0.0.1", 9001), "A")
       }*/
}
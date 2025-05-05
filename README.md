# Raft

This is an implementation of the Raft protocol made from scratch.

This branch tries to follow the implementation approach explained by Martin Kleppmann in its
distributed systems lectures and the original paper.

This is a second attempt of implementation. The first on the main branch, used from the beginning
a flat hierarchy of classes to distinguish the different roles a Node can have in the
Raft state machine. However, when try the implementation had too much boilerplate code due to the
hierarch.

This implementation will be more "spaghetti" as there will be conditionals to check each role.
Although from the very beginning it is clear it is not the ideal solution, it will be easier to
contemplate the algorithm as a whole without abstractions and with explicit role separation
(several conditionals in a place instead of responsibilities in different classes and files).
Also, it will give an easy understanding of what is shared between all the roles.

After a better completion of the algorithm a refactor to a more organised sophisticated approach
can be done.

The ergonomics of the previous architecture to simulate time and the network are useful and
will be adapted to the new approach. Everything new will be inside the statemachine package until
the refactor.

<h2>Raft(1/9) - Initialisation and Election</h2>

<h3>Follower</h3>
Nodes start as a Follower.

When the system is just initialised, a Follower may be promoted
to Candidate in one of the two following scenarios:

When a Follower have not yet received any messages and the ElectionTimeOut is reached.
Or when the period that takes two different messages from the
Leader to arrive at the Follower is bigger than the HeartBeatTimeOut.
This is a sign that indicates the Leader might have crashed.

Each Follower is instantiated with a random ElectionTimeOut to avoid two nodes
starting an election at the same time.

<h3>Candidate</h3>
After a promotion from Follower to Candidate, the Candidate must start an election.
Another scenario where the Candidate must start an election is when after having started
a previous election, it is not promoted to Leader before the ElectionTimeOut has been reached.

<h3>Election<h3>
To start an election, the Candidate increments its term, votes to itself, and request votes to
all its peers.

<h3>Request for Votes</h3>
The Request for vote is composed of the candidate id, the current term of the candidate, the log
length and the last term from the log, or 0 if the log is empty.

<h3>Start election timer</h3>
After the start of an election the Candidate must start an election time, so if the election does
not conclude before reaching the ElectionTimeOut, the Candidate starts a new election.
This time out is necessary to avoid the system being stale without a Leader in case
of split votes.

The Candidate will keep the "timestamp" of when the term starts <termStartedAt>.
That way if we subtract <termStartedAt> from the current time and the result is bigger or equals
to the ElectionTimeOut, it timed out and a new election is needed.

<h3>Recovery from crash</h3>
Left for later to focus on happy path scenarios and as it can be simulated removing the Node from the network.

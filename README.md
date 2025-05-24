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

<h2>Raft(2/9) - Voting</h2>
When receiving a Request for Vote (and probably any other message), if the Term in the message is higher than the Term in the receiving Node, it must update its term to the new value. This signals that successful elections have happened and the receiving Node is outdated. This also means the Node have to be demoted to Follower and have the vote decision cleared.
Note that even in this case a Node is still going to take decision on how to vote.

<h3>Deciding the Vote</h3>
A Node votes for a Candidate if three conditions are met.

1 - The Candidate Term (that comes in the Vote Request) is the same as its own Term. If they are not, it means that the one of the Nodes have participated in other elections.

2 - The Node have yet not voted for another Candidate. Remember that when a new Election happens a new Candidate will send a Request For Vote with a higher term and the Node will have its state adjusted to reflect the new election (see the description of the beginning of this session). If a Node has voted for itself before on the same Election, it can repeat that vote.

3 - The Log of the candidate is more up to date than the current Node. That is verified checking if the Candidate Log Term is higher than the Last Log Term of the current node. Or if the Logs are on the same term, then the Candidate's log must be bigger than the current Node's log.

If one of the previous conditions are not met then the Node should respond with a negative vote.

<h2>Raft(3/9) - Candidate Receiving Votes</h2>
When a Vote is received, if the Term of the vote is higher than the Term in the receiving Node, the current Node is outdated. The Node's Term has to be updated as the Term from the Vote, the Node must be Demoted to Follower, its Vote Record (votedFor) deleted, as it voted for itself previously. Finally, the election timeout must be canceled.

<h3>Computing a Vote</h3>
For a Node to accept a Vote it needs to be a Candidate, the Term for the Vote be the same as its Term and the Vote to have been granted as positive (agrees = true).

If all the conditions are met, then the Vote should be stored in Votes Received. Note that Votes Received is a set, so this operation is idempotent.

And if the candidate has the quorum (more than 50% of the participants), the Node can be Promoted to Leader.

During the Promotion step Kleppmann is using a flag (currentLeader) to sign that. This implementation accounts for that in different ways, checking the role or even other mechanisms when refactoring for a interfaces hierarch.

The Election timeout has to be canceled and for each of the other Nodes the log must be replicated.

<h3>Log Replication Controls Initialization</h3>
The Log Replication will be fully implemented in the next steps. At this point, the Leader will only have the state that controls what to send to each of the other Nodes initialized.

The Number of Logs Records that the Leader already sent to the other Nodes must be initialized as the Log Length, this assumes that the other Nodes have already received all entries the Leader has. If that is not true, the Log Replication process will adjust the information: (sentLength[follower] = log.length)

The Number of Log Records that the other Node has already acknowledged. It should assume nothing was acknowledged yet and as the Leader receives the acknowledgements, this number will increase: (ackedLength[followe] = 0)

<h2>raft(4/9) - Broadcasting Messages</h2>
When receiving a Command from a Client, if the Node is not a Leader, it should redirect the receiving messages to the Leader via FIFO (First In, First Out).

**I'm not sure if the Candidate also should do the same. I will assume it will.**

When the Leader receives the Client Command, does not matter if via another Node or straight from the Client, it must append the Command (Message) to its Log, the Log must contain the Current Term of the Leader. The Command coming from the Clients should not be aware of the Leader Term, thus a new type might be needed.

TODO: When adding the new type, it would still be considered a Message, to be handled sequentially. A type outside the Message interface would have to be handled before or after the other messages arrivals, because it is not part of the same collection.

The Leader must set the ackLength of itself to the size of its Log: ackLength[log.size], this signals that
the Node (itself) has acked the Log until that point.
Then the Leader must replicate its Log to all its Peers.

TODO: When a non Leader receives a Client Command, it must forward to the Leader. The implementation still do not have a mechanism that changes the state of the Followers to know the last elected Leader.

<h3>Heartbeats</h3>
To prevent a new Election to start when the Leader is still alive, the Leader sends Heartbeats.
The Heartbeat timeout must be calculated to be of a shorter time than the Election Timeout added of the time the Heartbeat takes to arrive at the other Nodes.

The Heartbeats implementation is the same as the Log Replication and do not have its own Message type as in the previous implementation.

<h2>Raft (5/9) - Replicating from Leader to Followers Log Replication</h2>
The Leader replicates the Log Entries it believes it have not yet sent to the Followers.
This is done via the 'prefixLen', that is the index of the last Entry sent to a particular Follower: sentLength[ follower ].

It 'slices' its log from that the prefixLen until the end of the Log. It sends to the Follower the missing Entries and with that the Term of the last not sent Entry. This is similar to the Last Term of the Log, but considering only the sent messages to the particular Follower. If there are no Entries, the Term is 0.

<h2>Raft (6/9) - Other Nodes Receiving the Log Replication</h2>
The Node receiving the Log replication must do the usual Term check: if the Term in the Message is higher than its current Term it should update its Term, reset the Vote record and cancel the Election timer.

If the Term in the message is the SAME as the Node Term, then the Node must set the Current Leader record to the Message Sender and Demote itself to Follower. It seems quite common that a Node will receive an AppendEntries (Log Replication) message with the same Term as its own, and it seems odd that the Node should perform the two previously mentioned steps at each Message. The explanation is that this might happen when the Node is a Candidate and receives a message from the Leader in the same Election Term. However, Klepmann does not add a contidional based on the Node Role. This implementation will perform the Demotion and Current Leader record set, independently of the Node Role.

<h3>Log Replication Checks</h3>
In order to accept the Log Entries the receiving Node must verify if its Log is in accordance with the sent Entries. This is similar to the checks performed in the Log during the Leader election but slightly different.

On the Leader Election we split the Log in a Prefix, that is what the Leader believe it has already sent to the Follower. For that, the receiving Node Log must be at least as long as the prefix sent in the Message, meaning the receiving Node must have a Log Size bigger or equal to the prefixLen sent in the Message.

If it is not, it means there is a gap where the receiving Node does not have the Entries the Leader belive it has.

The second requirement is that the prefixLen is 0, meaning there was nothing on the Leader to be sent.
Or the Last Log Entry Term of the prefix is the same as the prefixTerm (prefixTerm is the corresponding Log Entry Term in the Leader). Raft has an invariant where if there are two Nodes in the system and if the Log Entries at the same Index have the same Term, then the protocol guarantees that the Log up to that index is identical.

To efficiently check if two Nodes have identical Logs up to a certain index, it is just matter of checking if the Term of the Entries of the same index are the same. That is what the previous conditions are guaranteeing.

<h3>Log Response</h3>
The ACK sent in the Response, in Kleppmann's implementation, is calculated in the Follower receiving the Log Entries. However, the Leader knows the prefixLen and the suffix length, so a positive or negative Response would be sufficient and the Leader could calculate that value on its side. Klepmman's implementation will be followed here as the consequences of that change are unknown at the moment.


<h2>Raft (7/9) - Updating Follower's Log</h2>
<h3>Truncating the Log</h3>
The first check to be done when Appending Entries to the Followers Log is to check if the Follower already have some of the Entries sent in the Message. The check is done verifying if the size of the Log is greater than the prefixLen and also if the Message contains Entries to be appended (the Number of Entries to append is greater than 0).

If the Follower has some of the Entries in the Message then the Log should be truncated, removing the Entries that are out of sync and keeping the synched ones.

To remove the out of sync entries we need to find the index of Last comparable Log Entry between the Follower and the Leader. For that we compare which is smaller, the Log in the Follower or the prefixLen + the suffixLen. The last expression is a reflection of the part of the Log in the Leader that it believe the Follower must have. Then we subtract one from this index.

If the Terms on the two corresponding Entries are different in the Follower and the Leader on the calculated index (followerLog[index].ter != suffix[index - prefixLen]) it means the Logs are not in sync and the then the Log must be truncated until prefixLen - 1, this position is where all the new entries should be appended.

<h3>Appending the new Entries</h3>
With the out of sync entries removed, it is time to append the new Entries.
For that the prefixLen and the suffix Len must be greater than the Follower's Log size. Could be that the entries to be appended are already contained in the Followers Log and they should not appended again. To accomplish that the first suffix log index to be appended should be the Followers Log size - prefixLen, until the end of the suffix (suffix - 1). To visualise this case, see images/07_appending_the_new_entries.jpg.

<h3>Commiting Entries in the Follower</h3>
The appended new entries are not yet commited in the Follower. The entries to be commited is a decision only the Leader can take. And it signals it to the Followers sending the 'leaderCommit' containted in the AppendEntries Message, if leaderCommit is greater than the commitLength in the Follower, then the Entries in the Follower Log from the index pointed by commitLength until leaderCommit - 1 should be delivered to the application and the commitLength should be updated to the leaderCommit value.


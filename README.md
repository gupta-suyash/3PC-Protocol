Three Phase Commit Protocol.

This implementation designs a simplified three phase centralized commit protocol. To handle different failures we have also designed a termination protocol. The protocol simulates six different type of failures (three at the coordinator site, and three at the cohorts). To implement a practical system the failures are randomly generated. To check the code implementation for a specific type of failure, the user can remove the random option with a specific value.

The code allows any number of sites. Each site is designed as a client-server architecture. To observe the execution of three phase commit protocol on a three site architecture following steps are required.


Compile

-- javac *


Execute each server individually

Syntax: java <server_class> total_number_of_sites

-- java Site1Server 3

-- java Site2Server 3

-- java Site3Server 3


Execute each client using the common Client class

Syntax: java <client_class> number_of_sites_excluding_self coordinator_id my_id

-- java Site1Client 2 1 1

-- java Site1Client 2 1 2

-- java Site1Client 2 1 3


Start the transaction using a Transaction client.

Syntax: TransactionClient number_of_cohorts.

-- TransactionClient 2


---------------------------- x ---------------------------------

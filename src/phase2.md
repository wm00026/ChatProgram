# Phase II
*Improvements to the Program*

## Goal
Phase II focused on improving parts of the already established codebase before adding new features. This is to bolster the features of the program while also ensuring future additions and use cases are not limited.

## Additions in this Phase:
- Thread Pool: An ExecutorService inside ChatServer. It is a fixed collection of pre-created worker threads. It uses Runnable to assign a task to a thread, rather than create and destroy one. The modification made placed a ceiling on the OS threads being spawned using *threadPool.submit*. Further, a shutdown method was added, which ends programs more gracefully than before.
- Hardening the ClientHandler: Changes to the ClientHandler were made to harden it against issues. The biggest was limiting the message length to prevent the consumption to uncessary heap memory. A few  changes were also made to fix minor issues.
- Integration Tests: The integration tests for the program tested the major classes (ChatServer, ClientHandler, and Protocol) together, rather than individually through unit tests. The Handshake, public messaging broadcast, and graceful disconnected with /quit were tested with *ChatServerIntegrationTest.java*. Furthermore, I added a change to ChatServer that allowed for testing not on the main port, 12345, for better testing results. 
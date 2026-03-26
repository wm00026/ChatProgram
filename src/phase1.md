# Phase I - Building the Basics

## Goal
The goal of Phase I was to have the program *work* and to test functionality. The build was a basic chat program that could handle users chatting in a general channel, with commands and personal messaging.

## Implementation
The implementation started with the development of the ***common*** package with three classes. Protocol.java, User.java, and Message.java. Each of these classes would serve as the structure for which things were implemented onto. 
- Protocol.java: This class focuses on the most basic parts of the messaging. Formatting of message types, commands, username parameters, validation of usernames and commands, and creating the /help message. 
- User.java: Builds the User class. Constructor for a user; includes their username, socket, out stream, and active status. Getters and Setters are established as well.
- Message.java: Builds the Message class. Contains message types, constructors for messages from a sender with and without a recipient, format of message types, and getters.

The next step was to develop the Server package, which contained the program on the server side and a client handler.
- ChatServer.java: Binds a connection to a socket, connects users to the program in a loop, and tracks connected users. Connected users are currently tracked through a ConcurrentHashMap. 
- ClientHandler.java: Focuses on a few key responsiblities: First, the program checks if a Username is valid or not, then places them in the main chatroom with a welcome message. The program also handles commands, system messages, and closing the socket.

The next step after the server side was the client side. There was only one program for the client, a ChatClient.java file. The program listens for messages from the server and then prints them to console; and facilitates input from the user. 

The final step was to develop some JUnit tests for the Protocol and Message classes. This was to ensure that the base systems worked as entented. 
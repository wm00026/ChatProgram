# Phase III
*Implementation of New Features*

## Goal
The goal of Phase III was to implement more features to flesh out the basic program. This focused on a few things: The implementation of chat rooms, of a logger, and admin commands like mute, kick, and ban. There was also the implementation of a Swing GUI.

## Additions in this Phase
- ChatRoom: Before, everyone was only able to be placed into a general chatroom. The purpsoe of implementing this feature was so people could create and join another chatroom. The chatroom manages the "general" chatroom and other chatrooms, broadcasting messages, managing users. A chatroom is first created by one user and can be joined by other users.
- Logger: The logger program creates a log of messages and activity within a room. It records time, user, and message, and also records who enters a chatroom.
- Admin controls: Administrative commands (/kick, /mute, /ban) were created for an admin user. An admin user is the 1st user who joins the program. The admin can mute a user, preventing messages, kicking a user from the program, and banning a user from the program by banning the username.
- GUI: Before, the program just worked within the terminal. This is not a bad thing, but for a "cleaner" look, a Swing GUI was implemented that creates a JFrame window on startup. It's a basic Swing GUI with a window for the chat and window for sending messages.


## Where the Program Stands Now
The program is at a functional state. The program can handle multiple users; users can whisper, create Chat rooms, and have admin commands. There is always new features and programs that can be added. Consider this the version 0.1; it works, but it isn't close to "final"...

or at least where the features will stop. There's plenty of improvements to make.
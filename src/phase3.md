# Phase III
*Implementation of New Features*

## Goal
The goal of Phase III was to implement more features to flesh out the basic program. This focused on a few things: The implementation of chat rooms, of a logger, and admin commands like mute, kick, and ban.
- ChatRoom: Before, everyone was only able to be placed into a general chatroom. The purpsoe of implementing this feature was so people could create and join another chatroom. The chatroom manages the "general" chatroom and other chatrooms, broadcasting messages, managing users. A chatroom is first created by one user and can be joined by other users.
- Logger: The logger program creates a log of messages and activity within a room. It records time, user, and message, and also records who enters a chatroom.
- Admin controls: Administrative commands (/kick, /mute, /ban) were created for an admin user. An admin user is the 1st user who joins the program. The admin can mute a user, preventing messages, kicking a user from the program, and banning a user from the program by banning the username.
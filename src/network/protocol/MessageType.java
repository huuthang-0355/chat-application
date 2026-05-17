package network.protocol;

public enum MessageType {
    LOGIN, // client sends username to server on connect
    LOGOUT, // client disconnect
    MSG, // broadcast to everyone
    PRIVATE, // private msg to one specific user
    FILE, // file transfer
    ERROR, // server sends an error to clients
    USER_LIST, // server sneds the list of online users

    REGISTER, // user register
    LOGIN_OK,
    LOGIN_FAIL,

    // Group PROTOCOL
    GROUP_MSG, // msg to a named group
    CREATE_GROUP, // client create a new group
    JOIN_GROUP, // client joins group
    LEAVE_GROUP, // client leaves group
    GROUP_LIST // server sends the list of groups user belongs to
}

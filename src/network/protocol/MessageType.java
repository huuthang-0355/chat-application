package network.protocol;

public enum MessageType {
    LOGIN, // client sends username to server on connect
    LOGOUT, // client disconnect
    MSG, // broadcast to everyone
    PRIVATE, // private msg to one specific user
    GROUP_MSG, // msg to a named group
    FILE, // file transfer
    ERROR, // server sends an error to clients
    USER_LIST, // server sneds the list of online users
}

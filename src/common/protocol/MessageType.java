package common.protocol;

public enum MessageType {
    LOGIN, // client sends username to server on connect
    LOGOUT, // client disconnect
    MSG, // broadcast to everyone
    PRIVATE, // private msg to one specific user
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
    GROUP_LIST, // server sends the list of groups user belongs to
    GROUP_MEMBERS, // client requests members / server sends members

    // FILE HANDLING
    FILE_UPLOAD, // client -> server (contains bytes)
    FILE_NOTIFY, // server -> client (contains HTML notification, no bytes)
    FILE_REQ, // client -> server (contains requested fileId)
    FILE_DOWNLOAD, // server -> client (contains requested bytes)

    FETCH_HISTORY, // client -> server (request chat history)

    HISTORY_RESPONSE, // server -> client

    DELETE_MSG, // client -> server

    DELETE_CONVERSATION // client -> server
}

# Java Desktop Chat Application — Learning Roadmap

> A staged development plan for building a chat application with **Java, Swing, TCP Sockets, Multithreading, PostgreSQL, and JDBC**.

---

## Stage 1 — TCP Socket Fundamentals

### Goal
Understand how two programs communicate over a network using TCP sockets.

### Features to Implement
- A server that listens on a port and accepts **one** client connection
- A client that connects and sends a single message
- Server echoes the message back

### Required Concepts
| Concept | Why It Matters |
|---|---|
| `java.net.Socket` / `ServerSocket` | Core TCP API |
| `InputStream` / `OutputStream` | Raw byte-level I/O |
| `BufferedReader` / `PrintWriter` | Text-based wrapper for streams |
| Port numbers & localhost | Network addressing basics |
| Try-with-resources | Proper resource cleanup |

### Architecture Ideas
- Keep everything in **one package** (`network.basic`). No abstractions yet.
- Server and Client are two separate `main()` entry points.

### Recommended Classes
```
src/
  network/basic/
    SimpleServer.java
    SimpleClient.java
```

### Mini-Tasks
1. Create `SimpleServer` — listen on port `12345`, accept a connection, read one line, print it.
2. Create `SimpleClient` — connect to `localhost:12345`, send `"Hello Server"`, read the echo.
3. Run both in separate terminals and observe the output.
4. Modify to exchange **3 messages** back and forth (request → response pattern).

### Common Mistakes
- Forgetting to `flush()` the output stream → messages never arrive.
- Not closing sockets → port stays occupied.
- Mixing `DataInputStream` with `BufferedReader` → encoding chaos.

### Debugging Mindset
- **Watch the console on both sides.** If one side hangs, the other probably isn't sending/flushing.
- Use `netstat -an | findstr 12345` to check if the port is in use.

### Code Yourself vs. Ask AI
| Do Yourself | OK to Ask AI |
|---|---|
| Write the socket open/read/write/close flow | Explain exception types (`BindException`, `ConnectException`) |
| Debug "connection refused" errors | Clarify `flush()` behavior |

### Review Checklist
- [ ] Server starts without errors
- [ ] Client connects and sends a message
- [ ] Server receives and echoes it back
- [ ] Both programs close cleanly (no `Address already in use` on restart)

---

## Stage 2 — Console-Based Single-Client Chat

### Goal
Turn the echo server into a **real-time two-way chat** between one server and one client in the console.

### Features to Implement
- Continuous message exchange (not just one message)
- Either side can type and send at any time
- Graceful shutdown with a `/quit` command

### Required Concepts
| Concept | Why |
|---|---|
| `Thread` basics | Read and write must happen simultaneously |
| Blocking I/O | `readLine()` blocks → you need a second thread |
| `Runnable` interface | Cleanest way to define thread tasks |
| `volatile` keyword | Safely share a "running" flag between threads |

### Architecture Ideas
- **Two threads per endpoint**: one for reading from the socket, one for reading from `System.in`.
- Introduce a `ReadThread` runnable that loops on `reader.readLine()`.

### Recommended Classes
```
src/
  network/chat/
    ChatServer.java      # accepts 1 client, starts read/write threads
    ChatClient.java      # connects, starts read/write threads
    ReadThread.java       # shared runnable: reads from socket, prints to console
```

### Mini-Tasks
1. Refactor `SimpleServer` → infinite `while` loop reading messages.
2. Add a second thread for keyboard input so you can send while receiving.
3. Implement `/quit` to close the connection gracefully from either side.
4. Test: open two terminals, have a conversation.

### Common Mistakes
- Reading from `System.in` and socket on the **same thread** → one blocks the other.
- Not handling `SocketException` when the remote side disconnects.
- Infinite loop spinning when the stream is closed → check for `null` from `readLine()`.

### Debugging Mindset
- **Print thread names** (`Thread.currentThread().getName()`) to understand which thread does what.
- If the app hangs, one thread is blocked on I/O — identify which one.

### Code Yourself vs. Ask AI
| Do Yourself | OK to Ask AI |
|---|---|
| Write the two-thread architecture | Explain `Thread.interrupt()` vs. closing the socket |
| Handle `/quit` logic | Best practice for graceful shutdown |

### Review Checklist
- [ ] Both sides can send messages at any time
- [ ] Messages appear instantly on the other side
- [ ] `/quit` closes both programs cleanly
- [ ] No zombie threads after exit

---

## Stage 3 — Multi-Client Server with Threads

### Goal
Evolve the server to handle **multiple clients simultaneously**, each in its own thread.

### Features to Implement
- Server accepts N clients concurrently
- Messages from one client are broadcast to all others
- Server logs connections/disconnections

### Required Concepts
| Concept | Why |
|---|---|
| Thread-per-client pattern | Each client gets a dedicated handler thread |
| `synchronized` / `CopyOnWriteArrayList` | Safely share the client list across threads |
| `ExecutorService` | Managed thread pool (better than raw `new Thread()`) |
| Broadcasting pattern | Iterate all clients, send to each |

### Architecture Ideas
- **`ClientHandler`** class: one instance per connected client, runs on its own thread.
- Server maintains a `List<ClientHandler>` — this is the **shared state**.
- Broadcasting = iterate the list, call `sendMessage()` on each handler.

### Recommended Classes
```
src/
  server/
    ChatServer.java          # accept loop + client list management
    ClientHandler.java       # per-client thread: read messages, call broadcast
  client/
    ChatClient.java          # connects, read/write threads
```

### Mini-Tasks
1. Create `ClientHandler implements Runnable` — read loop, on message → call `server.broadcast(msg, this)`.
2. `ChatServer` keeps a `CopyOnWriteArrayList<ClientHandler>`, has `broadcast()` and `removeClient()`.
3. Test with **3 terminals**: 1 server + 2 clients. Verify messages appear on both clients.
4. Handle client disconnect gracefully (remove from list, log it).

### Common Mistakes
- `ConcurrentModificationException` when iterating the client list while another thread modifies it.
- Sending a message back to the **sender** (should broadcast to others only, or to all — design choice).
- Not removing disconnected clients → `NullPointerException` on next broadcast.

### Debugging Mindset
- Add `[Server]`, `[Client-1]` prefixes to all log messages.
- Test edge cases: what happens if a client disconnects mid-message?
- Watch thread count in your IDE debugger — it should equal `N clients + 1 accept thread + main`.

### Code Yourself vs. Ask AI
| Do Yourself | OK to Ask AI |
|---|---|
| Implement `ClientHandler` and broadcast logic | Explain `CopyOnWriteArrayList` vs `synchronizedList` |
| Handle concurrent disconnect scenarios | Thread pool sizing strategies |

### Review Checklist
- [ ] Server handles 3+ clients simultaneously
- [ ] Messages from one client appear on all others
- [ ] Disconnecting a client doesn't crash the server
- [ ] No `ConcurrentModificationException`

---

## Stage 4 — Chat Protocol Design

### Goal
Replace raw text messages with a **structured protocol** so the system can distinguish message types (login, chat, file, etc.).

### Features to Implement
- Define a text-based protocol (e.g., `TYPE|SENDER|TARGET|PAYLOAD`)
- Parse incoming messages into structured objects
- Route messages based on type

### Required Concepts
| Concept | Why |
|---|---|
| Application-layer protocol design | Structured communication between client and server |
| String parsing (`split`, regex) | Decoding protocol messages |
| Enum for message types | Type safety for `LOGIN`, `MSG`, `FILE`, etc. |
| Builder or Factory pattern | Clean message construction |

### Architecture Ideas
- Define a `Message` class with fields: `type`, `sender`, `target`, `content`, `timestamp`.
- Define `MessageType` enum: `LOGIN`, `LOGOUT`, `PRIVATE_MSG`, `GROUP_MSG`, `FILE`, `HISTORY`, `ACK`, `ERROR`.
- Protocol format: `TYPE|sender|target|content` (pipe-delimited, simple to parse).
- `MessageParser` utility class to convert `String ↔ Message`.

### Recommended Classes
```
src/
  protocol/
    MessageType.java       # enum
    Message.java           # data class
    MessageParser.java     # encode/decode
```

### Mini-Tasks
1. Define `MessageType` enum with all planned types.
2. Create `Message` class with fields + `toString()` for serialization.
3. Create `MessageParser.encode(Message) → String` and `MessageParser.decode(String) → Message`.
4. Write unit tests (or a `main()` test) to verify round-trip: `decode(encode(msg)).equals(msg)`.
5. Integrate into `ClientHandler`: parse incoming raw text → `Message` → route by type.

### Common Mistakes
- Not escaping the delimiter in message content (what if the user types `|`?).
- Forgetting to handle malformed messages → `ArrayIndexOutOfBoundsException` on `split()`.
- Making the protocol too complex too early.

### Debugging Mindset
- **Log every raw message** received before parsing. This helps when parsing fails.
- Ask yourself: "If I add a new feature, do I need a new message type or can I reuse one?"

### Code Yourself vs. Ask AI
| Do Yourself | OK to Ask AI |
|---|---|
| Design the protocol format | Review your protocol for edge cases |
| Implement `MessageParser` | Suggest delimiter-escaping strategies |

### Review Checklist
- [ ] `Message` class has all needed fields
- [ ] `MessageParser` round-trips correctly
- [ ] `ClientHandler` routes messages by type
- [ ] Malformed messages are handled (logged, not crashed)

---

## Stage 5 — Swing GUI Integration

### Goal
Replace the console interface with a **Swing-based GUI** for the chat client.

### Features to Implement
- Login window (username input)
- Main chat window with message display area, input field, send button
- Online user list panel
- Separate tabs or windows for private chats

### Required Concepts
| Concept | Why |
|---|---|
| `JFrame`, `JPanel`, `JTextField`, `JTextArea`, `JButton` | Core Swing components |
| `BorderLayout`, `GridBagLayout`, `BoxLayout` | Arranging components |
| `ActionListener` / `KeyListener` | Handling user events |
| `SwingUtilities.invokeLater()` | Thread-safe GUI updates |
| EDT (Event Dispatch Thread) | All GUI updates must happen on this thread |
| MVC pattern | Separate UI from networking logic |

### Architecture Ideas
- **Client-side only** — the server remains console-based.
- Use **MVC**: `ChatView` (Swing), `ChatController` (business logic), `NetworkService` (socket communication).
- Network thread receives messages → posts to EDT via `SwingUtilities.invokeLater()`.
- **Never do blocking I/O on the EDT.**

### Recommended Classes
```
src/
  client/
    view/
      LoginView.java           # login window
      ChatView.java            # main chat window
      UserListPanel.java       # shows online users
    controller/
      ChatController.java      # mediates between view and network
    network/
      NetworkService.java      # socket read/write, runs on background thread
    ClientApp.java             # entry point
```

### Mini-Tasks
1. Build `LoginView` — username field + connect button. On click → connect to server.
2. Build `ChatView` — message area (non-editable `JTextArea`), input field, send button.
3. Wire `NetworkService` to run on a background thread; on message received → update `ChatView` via EDT.
4. Add `UserListPanel` — server broadcasts online user list on connect/disconnect.
5. Support private chat: double-click a user → open a new chat tab/window.

### Common Mistakes
- Updating Swing components from the network thread → random crashes / visual glitches.
- Putting `socket.read()` on the EDT → entire GUI freezes.
- Forgetting `setDefaultCloseOperation(EXIT_ON_CLOSE)` → app doesn't quit.

### Debugging Mindset
- If GUI freezes, you're blocking the EDT. Check your thread.
- Use `System.out.println(SwingUtilities.isEventDispatchThread())` to verify thread context.

### Code Yourself vs. Ask AI
| Do Yourself | OK to Ask AI |
|---|---|
| Layout the Swing components | Complex `GridBagLayout` constraints |
| Wire controller to view | EDT threading patterns |
| Handle send button click | Styling tips (fonts, colors) |

### Review Checklist
- [ ] Login window connects to server
- [ ] Messages appear in the chat area in real-time
- [ ] Sending a message works via button and Enter key
- [ ] Online user list updates on connect/disconnect
- [ ] No GUI freezes

---

## Stage 6 — Database Integration (PostgreSQL + JDBC)

### Goal
Add persistent storage for users, messages, and groups using **PostgreSQL**.

### Features to Implement
- Database schema creation (users, messages, groups, group_members)
- JDBC connection pool setup
- DAO (Data Access Object) layer for all database operations

### Required Concepts
| Concept | Why |
|---|---|
| SQL (DDL + DML) | Creating tables, INSERT, SELECT, UPDATE, DELETE |
| JDBC (`Connection`, `PreparedStatement`, `ResultSet`) | Java ↔ PostgreSQL communication |
| PostgreSQL JDBC Driver (`postgresql`) | Official JDBC driver for PostgreSQL |
| Connection pooling (HikariCP) | Efficient connection reuse |
| DAO pattern | Clean separation of DB logic from business logic |
| SQL injection & `PreparedStatement` | Security — **never** concatenate user input into SQL |
| Database transactions | Atomic multi-step operations |

### Architecture Ideas
```
┌─────────┐     ┌────────────┐     ┌──────┐
│ Service  │ ──▶ │    DAO     │ ──▶ │  DB  │
│  Layer   │     │ (JDBC)     │     │(PgSQL)│
└─────────┘     └────────────┘     └──────┘
```
- **One DAO per table**: `UserDAO`, `MessageDAO`, `GroupDAO`.
- `DatabaseConfig` class manages connection pool.
- DAOs are used by server-side services only.

### Recommended Classes
```
src/
  server/
    db/
      DatabaseConfig.java      # HikariCP setup, getConnection()
      UserDAO.java             # CRUD for users table
      MessageDAO.java          # save/fetch/delete messages
      GroupDAO.java            # group CRUD + membership
    schema.sql                 # DDL script
```

### PostgreSQL vs MSSQL — Key Syntax Differences

| PostgreSQL | MSSQL Equivalent | Notes |
|---|---|---|
| `SERIAL PRIMARY KEY` | `INT IDENTITY(1,1) PRIMARY KEY` | Auto-increment |
| `BOOLEAN DEFAULT FALSE` | `BIT DEFAULT 0` | true/false vs 1/0 |
| `TEXT` | `NVARCHAR(MAX)` | Unbounded text |
| `VARCHAR(n)` | `NVARCHAR(n)` | - |
| `NOW()` | `GETDATE()` | Current timestamp |
| `LIMIT n` | `FETCH NEXT n ROWS ONLY` | Pagination |

### Database Schema (Initial)
```sql
-- Run in psql or pgAdmin Query Tool

CREATE TABLE users (
    id            SERIAL       PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE messages (
    id          SERIAL    PRIMARY KEY,
    sender_id   INT       REFERENCES users(id),
    receiver_id INT       REFERENCES users(id),   -- NULL = broadcast
    content     TEXT      NOT NULL,
    sent_at     TIMESTAMP DEFAULT NOW(),
    is_deleted  BOOLEAN   DEFAULT FALSE
);

CREATE TABLE groups (
    id         SERIAL      PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_by INT         REFERENCES users(id),
    created_at TIMESTAMP   DEFAULT NOW()
);

CREATE TABLE group_members (
    group_id INT REFERENCES groups(id),
    user_id  INT REFERENCES users(id),
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE group_messages (
    id        SERIAL    PRIMARY KEY,
    group_id  INT       REFERENCES groups(id),
    sender_id INT       REFERENCES users(id),
    content   TEXT      NOT NULL,
    sent_at   TIMESTAMP DEFAULT NOW()
);
```

### JDBC Connection Setup for PostgreSQL

**Step 1 — Download the JAR:**
1. Go to: https://jdbc.postgresql.org/download/
2. Download `postgresql-42.7.3.jar` (latest stable)
3. Place it in your `lib/` folder:
   ```
   23120355/
     lib/
       postgresql-42.7.3.jar   ← put it here
     src/
   ```

**Step 2 — Add to IntelliJ classpath:**
1. `File` → `Project Structure` (Ctrl+Alt+Shift+S)
2. Go to `Modules` → `Dependencies` tab
3. Click `+` → `JARs or Directories`
4. Select `lib/postgresql-42.7.3.jar` → OK
5. Make sure scope is `Compile` → Apply

**JDBC URL format:**
```
jdbc:postgresql://localhost:5432/chatapp
```

**`DatabaseConfig` key values:**
```
URL      : jdbc:postgresql://localhost:5432/chatapp
Username : postgres   (or your PostgreSQL role)
Password : your password
```

### Mini-Tasks
1. Install PostgreSQL, open `psql` or **pgAdmin**, create a database: `CREATE DATABASE chatapp;`
2. Run `schema.sql` in pgAdmin's Query Tool or via `psql -f schema.sql chatapp`.
3. Download `postgresql-42.7.3.jar` and add it to IntelliJ via Project Structure (steps above).
4. Implement `DatabaseConfig` with `getConnection()` using the PostgreSQL JDBC URL.
5. Implement `UserDAO.createUser()` and `UserDAO.findByUsername()`.
6. Test with a simple `main()` that inserts and retrieves a user.

### Common Mistakes
- Not closing `Connection` / `ResultSet` → connection leak → server crashes after N connections.
- Using `Statement` instead of `PreparedStatement` → SQL injection vulnerability.
- Hardcoding DB credentials in source code → use a config file or environment variables.

### Debugging Mindset
- Use **pgAdmin** or **DBeaver** to manually inspect data and run queries.
- Log every SQL query during development.
- If you see `Connection refused` on port 5432 → PostgreSQL service isn't running. Start it via `services.msc` or `pg_ctl start`.
- If you see `FATAL: password authentication failed` → wrong username/password in `DatabaseConfig`.
- If `Connection` errors appear, check for connection leaks (forgot `try-with-resources`).

### Code Yourself vs. Ask AI
| Do Yourself | OK to Ask AI |
|---|---|
| Write the SQL schema | Complex JOIN queries |
| Implement basic DAO methods | HikariCP configuration |
| Handle `PreparedStatement` binding | Password hashing libraries |

### Review Checklist
- [ ] Database created with all tables
- [ ] `UserDAO` can create and find users
- [ ] `MessageDAO` can save and retrieve messages
- [ ] Connections are properly closed (no leaks)
- [ ] `PreparedStatement` used everywhere (no string concatenation)

---

## Stage 7 — Authentication System

### Goal
Implement **user registration and login** with password hashing and session management.

### Features to Implement
- User registration with validation
- Login with password verification
- Server-side session tracking (who is online)
- Duplicate login prevention

### Required Concepts
| Concept | Why |
|---|---|
| Password hashing (BCrypt) | Never store plaintext passwords |
| Session management | Track logged-in users on the server |
| Input validation | Prevent empty/invalid usernames |
| Protocol messages: `REGISTER`, `LOGIN`, `LOGIN_OK`, `LOGIN_FAIL` | Auth flow via chat protocol |

### Architecture Ideas
- `AuthService` handles registration and login logic.
- `ClientHandler` checks authentication before allowing chat commands.
- Server maintains a `Map<String, ClientHandler>` for online sessions.
- **Flow**: Client sends `LOGIN|username|password` → Server validates → responds `LOGIN_OK` or `LOGIN_FAIL|reason`.

### Recommended Classes
```
src/
  server/
    service/
      AuthService.java        # register(), login(), validates credentials
    session/
      SessionManager.java     # tracks online users, prevents duplicate login
```

### Mini-Tasks
1. Download the **jBCrypt** JAR:
   - Go to: https://mvnrepository.com/artifact/org.mindrot/jbcrypt/0.4
   - Click `jar` under Files to download `jbcrypt-0.4.jar`
   - Place it in your `lib/` folder (same folder as the MSSQL JAR)
   - Add it to IntelliJ via `Project Structure` → `Modules` → `Dependencies` → `+` → select the JAR
2. Implement `AuthService.register(username, password)` — hash password, save via `UserDAO`.
3. Implement `AuthService.login(username, password)` — fetch hash, verify with BCrypt.
4. Add `REGISTER` and `LOGIN` handling in `ClientHandler`.
5. Update `LoginView` to show error messages on failed login.
6. Implement `SessionManager` — add/remove online users, check duplicates.

### Common Mistakes
- Storing plaintext passwords.
- Not validating username uniqueness before inserting → DB constraint violation crash.
- Allowing unauthenticated users to send chat messages.

### Debugging Mindset
- Test: register → login → logout → login again. Does it work?
- Test: login with wrong password. Does the error message make sense?
- Test: login while already logged in from another client.

### Review Checklist
- [ ] Registration creates a user with hashed password
- [ ] Login succeeds with correct credentials
- [ ] Login fails gracefully with wrong password
- [ ] Duplicate username registration is rejected
- [ ] Duplicate login is prevented
- [ ] Unauthenticated clients cannot send messages

---

## Stage 8 — Group Chat

### Goal
Allow users to create groups, join/leave groups, and send messages visible to all group members.

### Features to Implement
- Create group (with creator as first member)
- Invite / add members to a group
- Send group message (delivered to all online members, stored in DB)
- Leave group
- List user's groups

### Architecture Ideas
- New message types: `CREATE_GROUP`, `JOIN_GROUP`, `LEAVE_GROUP`, `GROUP_MSG`, `GROUP_LIST`.
- `GroupService` on the server handles business logic.
- Broadcasting to group = iterate `group_members`, find their `ClientHandler` if online, send.
- Store group messages in `group_messages` table.

### Recommended Classes
```
src/
  server/
    service/
      GroupService.java       # create, join, leave, list groups
  client/
    view/
      GroupChatView.java      # group chat tab/window
```

### Mini-Tasks
1. Implement `GroupService.createGroup(name, creatorId)`.
2. Implement `GroupService.addMember(groupId, userId)`.
3. Handle `GROUP_MSG` in `ClientHandler` — save to DB + broadcast to online members.
4. Add group chat tab in `ChatView`.
5. Display group member list in the UI.

### Common Mistakes
- Sending group messages to **all** clients instead of only group members.
- Not persisting group messages → history is lost.
- Allowing non-members to send messages to a group.

### Review Checklist
- [ ] Group creation works, creator is auto-added as member
- [ ] Group messages are delivered only to group members
- [ ] Group messages are persisted in the database
- [ ] Offline members can see messages when they view history later
- [ ] Leave group works correctly

---

## Stage 9 — File Transfer

### Goal
Enable users to send files (images, documents) to other users during a private or group chat.

### Features to Implement
- Select a file from the local filesystem
- Send file metadata (name, size) first, then binary data
- Receiver gets a download prompt and saves the file
- Progress indication for large files

### Required Concepts
| Concept | Why |
|---|---|
| `FileInputStream` / `FileOutputStream` | Reading/writing files as bytes |
| Binary protocol (switching from text) | Files are binary, not text |
| Chunked transfer | Large files must be sent in pieces |
| `JFileChooser` | Swing file picker dialog |

### Architecture Ideas
- **Approach 1 (Simple)**: Encode file as Base64 string, send via existing text protocol. Works for small files (< 1MB).
- **Approach 2 (Better)**: Open a **separate socket connection** for file transfer. Send metadata on chat socket, transfer bytes on file socket.
- Start with Approach 1, upgrade to Approach 2 if needed.

### Recommended Classes
```
src/
  server/
    service/
      FileTransferService.java   # handles file routing
  client/
    service/
      FileSender.java            # reads file, encodes, sends
      FileReceiver.java          # decodes, writes to disk
```

### Mini-Tasks
1. Add a "Send File" button in `ChatView` → opens `JFileChooser`.
2. Read the selected file, Base64-encode it, send as `FILE|sender|target|filename|base64data`.
3. Server routes the file message to the target client.
4. Receiver decodes Base64, prompts "Save as?" with `JFileChooser`, writes to disk.
5. Add file size limit (e.g., 5MB) and show error for oversized files.

### Common Mistakes
- Sending large files as a single message → `OutOfMemoryError`.
- Not handling binary data correctly → corrupted files.
- Blocking the GUI while transferring.

### Review Checklist
- [ ] Can send a small file (image, text file) to another user
- [ ] Receiver can save the file to disk
- [ ] File content is not corrupted after transfer
- [ ] Large file handling doesn't crash the app
- [ ] File transfer works in group chat too

---

## Stage 10 — Chat History and Deletion

### Goal
Allow users to view past conversations and delete individual messages or entire conversations.

### Features to Implement
- View private chat history with a specific user
- View group chat history
- Delete a specific message (soft delete — mark as deleted)
- Delete entire conversation history
- Paginated loading (don't load 10,000 messages at once)

### Architecture Ideas
- New message types: `FETCH_HISTORY`, `HISTORY_RESPONSE`, `DELETE_MSG`, `DELETE_CONVERSATION`.
- `MessageService` on the server queries `MessageDAO` with pagination.
- Soft delete: set `is_deleted = TRUE` in DB, don't show in UI.
- Load history on demand: when user opens a chat, request last 50 messages.

### Recommended Classes
```
src/
  server/
    service/
      MessageService.java    # fetch history, delete, paginate
  client/
    view/
      HistoryView.java       # scrollable history panel with delete buttons
```

### Mini-Tasks
1. Implement `MessageDAO.getHistory(userId1, userId2, limit, offset)`.
2. Handle `FETCH_HISTORY` in `ClientHandler` — query DB, send back as `HISTORY_RESPONSE`.
3. Parse `HISTORY_RESPONSE` on client → display in `ChatView`.
4. Add "Delete" button per message → sends `DELETE_MSG|messageId`.
5. Implement soft delete in `MessageDAO`.
6. Add "Clear History" button → `DELETE_CONVERSATION`.

### Common Mistakes
- Loading all messages at once → slow UI, high memory usage.
- Hard-deleting messages → no audit trail, can't undo.
- Letting User A delete User B's messages.

### Review Checklist
- [ ] Chat history loads when opening a conversation
- [ ] Pagination works (scroll to load more)
- [ ] Deleting a message removes it from the UI
- [ ] Deleted messages don't reappear on reload
- [ ] Only the sender can delete their own messages

---

## Stage 11 — Refactoring and Architecture Improvements

### Goal
Clean up the codebase, apply design patterns, improve error handling, and prepare for potential future features.

### Areas to Improve

#### 1. Code Organization
```
src/
  common/                    # shared between client and server
    protocol/
      MessageType.java
      Message.java
      MessageParser.java
    model/
      User.java
      Group.java
  server/
    ChatServer.java
    ClientHandler.java
    db/
      DatabaseConfig.java
      UserDAO.java
      MessageDAO.java
      GroupDAO.java
    service/
      AuthService.java
      GroupService.java
      MessageService.java
      FileTransferService.java
    session/
      SessionManager.java
  client/
    ClientApp.java
    controller/
      ChatController.java
    network/
      NetworkService.java
    view/
      LoginView.java
      ChatView.java
      GroupChatView.java
      UserListPanel.java
    service/
      FileSender.java
      FileReceiver.java
```

#### 2. Design Patterns to Apply
| Pattern | Where |
|---|---|
| **Observer** | NetworkService notifies ChatController of new messages |
| **Singleton** | DatabaseConfig, SessionManager |
| **Factory** | MessageParser creates Message objects |
| **DAO** | Already applied — clean up interfaces |
| **MVC** | Strengthen separation in client code |

#### 3. Error Handling
- Centralized exception handling in `ClientHandler`.
- Reconnection logic on client-side network failure.
- User-friendly error dialogs in Swing.

#### 4. Security Hardening
- Input sanitization on all user inputs.
- Rate limiting on message sends.
- Validate file types and sizes on the server.

### Mini-Tasks
1. Extract shared `protocol/` and `model/` packages.
2. Add interfaces for all DAOs (`IUserDAO`, `IMessageDAO`).
3. Implement Observer pattern for network events.
4. Add proper logging (use `java.util.logging` or SLF4J).
5. Write a `README.md` with setup instructions.
6. Code review: remove dead code, add Javadoc to public methods.

### Review Checklist
- [ ] Clear package structure separating client, server, and common code
- [ ] No circular dependencies between packages
- [ ] All public methods have Javadoc
- [ ] Consistent error handling — no unhandled exceptions
- [ ] README with build and run instructions

---

## Overall Progress Tracker

| # | Stage | Status |
|---|---|---|
| 1 | TCP Socket Fundamentals | ⬜ Not Started |
| 2 | Console-Based Single-Client Chat | ⬜ Not Started |
| 3 | Multi-Client Server with Threads | ⬜ Not Started |
| 4 | Chat Protocol Design | ⬜ Not Started |
| 5 | Swing GUI Integration | ⬜ Not Started |
| 6 | Database Integration (PostgreSQL + JDBC) | ⬜ Not Started |
| 7 | Authentication System | ⬜ Not Started |
| 8 | Group Chat | ⬜ Not Started |
| 9 | File Transfer | ⬜ Not Started |
| 10 | Chat History and Deletion | ⬜ Not Started |
| 11 | Refactoring & Architecture Improvements | ⬜ Not Started |

---

> [!TIP]
> **How to use this roadmap**: Complete each stage fully before moving to the next. Use the review checklist at the end of each stage as your "gate" — don't proceed until every box is checked. When you're stuck, re-read the "Common Mistakes" and "Debugging Mindset" sections before asking for help.

> [!IMPORTANT]
> **Key Questions to Ask Yourself Throughout**:
> - "What thread is this code running on?"
> - "What happens if the other side disconnects right now?"
> - "Is this data shared between threads? If so, is access synchronized?"
> - "Am I closing all resources in a `finally` block or try-with-resources?"

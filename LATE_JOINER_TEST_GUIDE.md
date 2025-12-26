# Late Joiner Test Guide

## 🎯 Testing Scenario: Client Joins After Groups Are Created

This guide helps you verify that clients who connect **after** groups have been created can:
1. ✅ See all existing groups
2. ✅ Join any group
3. ✅ See all online users  
4. ✅ Start private chats with any user

---

## ✅ Current Implementation Status

The late joiner functionality is **ALREADY IMPLEMENTED**. Here's how it works:

### Server-Side (Automatic)

| Event | Action | Code Location |
|-------|--------|---------------|
| Client logs in | Send `USER_LIST` to all clients | [ClientHandler.java:130](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ClientHandler.java#L130) |
| Client logs in | Send `GROUP_LIST_UPDATE` to new client | [ClientHandler.java:133](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ClientHandler.java#L133) |
| Client requests user list | Send current `USER_LIST` | [ClientHandler.java:246-249](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ClientHandler.java#L246-L249) |
| Client requests group list | Send current `GROUP_LIST_UPDATE` | [ClientHandler.java:251-253](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ClientHandler.java#L251-L253) |

### Client-Side (Automatic)

| Event | Action | Code Location |
|-------|--------|---------------|
| Chat view loads | Request `USER_LIST_QUERY` | [ClientApp.java:54](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/client/src/main/java/com/securechat/client/ClientApp.java#L54) |
| Chat view loads | Request `GROUP_LIST_QUERY` | [ClientApp.java:55](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/client/src/main/java/com/securechat/client/ClientApp.java#L55) |
| Receives `GROUP_LIST_UPDATE` | Update UI group list | [NetworkClient.java:282-288](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/client/src/main/java/com/securechat/client/NetworkClient.java#L282-L288) |
| Receives `USER_LIST_UPDATE` | Update UI user list | [NetworkClient.java:367-379](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/client/src/main/java/com/securechat/client/NetworkClient.java#L367-L379) |

---

## 🧪 Step-by-Step Test

### Prerequisites
- 3 terminal windows
- Server and client code compiled

### Test Steps

#### Step 1: Start Server
```powershell
# Terminal 1
cd "c:\Users\USER\OneDrive\Desktop\Networking Project2\server"
mvn javafx:run
```

Wait for server to display "Server started on port 5000"

#### Step 2: Start Client 1 (Alice - Early Joiner)
```powershell
# Terminal 2
cd "c:\Users\USER\OneDrive\Desktop\Networking Project2\client"
mvn javafx:run
```

**Login as:**
- Username: `alice`
- Password: `test123`
- Server IP: `localhost`

#### Step 3: Start Client 2 (Bob - Early Joiner)
```powershell
# Terminal 3
cd "c:\Users\USER\OneDrive\Desktop\Networking Project2\client"
mvn javafx:run
```

**Login as:**
- Username: `bob`
- Password: `test123`
- Server IP: `localhost`

#### Step 4: Create Groups (Alice)

In Alice's client window:
1. Click **"Create Group"** button
2. Enter group name: `developers`
3. Click OK
4. Repeat for group: `testers`

**Expected Result:**
- Alice sees: `developers`, `testers` in group list
- Bob sees: `developers`, `testers` in group list (auto-updated)

#### Step 5: Join a Group (Bob)

In Bob's client window:
1. Click on `developers` in the group list
2. Chat window opens
3. Bob is automatically joined to the group

#### Step 6: Send Group Message (Bob)

In Bob's `developers` chat window:
1. Type: `Hello from Bob!`
2. Press Send

**Expected Result:**
- Bob sees his message in the chat
- Alice does NOT see it (she hasn't joined yet)

#### Step 7: Join Group (Alice)

In Alice's client window:
1. Click on `developers` in the group list
2. Chat window opens
3. Alice is automatically joined

#### Step 8: Send Group Message (Alice)

In Alice's `developers` chat window:
1. Type: `Hi Bob!`
2. Press Send

**Expected Result:**
- Alice sees her message
- Bob sees Alice's message in his chat window

#### Step 9: Start Client 3 (Charlie - LATE JOINER) ⭐

```powershell
# Terminal 4 (new terminal)
cd "c:\Users\USER\OneDrive\Desktop\Networking Project2\client"
mvn javafx:run
```

**Login as:**
- Username: `charlie`
- Password: `test123`
- Server IP: `localhost`

#### Step 10: Verify Late Joiner Sees Everything ✅

**In Charlie's client window, verify:**

1. **Group List** (left panel):
   - [ ] `developers` is visible
   - [ ] `testers` is visible

2. **User List** (right panel):
   - [ ] `alice [● Online]` is visible
   - [ ] `bob [● Online]` is visible
   - [ ] `charlie [● Online]` is visible (himself)

3. **Can Join Group:**
   - [ ] Click on `developers` group
   - [ ] Chat window opens
   - [ ] Charlie can send messages

4. **Can Private Chat:**
   - [ ] Click on `alice` in user list
   - [ ] Private chat window opens
   - [ ] Charlie can send DM to Alice

---

## 🔍 Troubleshooting

### Issue 1: Late joiner doesn't see groups

**Symptoms:**
- Charlie's group list is empty
- Groups were created before Charlie joined

**Possible Causes:**
1. Server not sending `GROUP_LIST_UPDATE` on login
2. Client not requesting group list on chat view load
3. Empty group list being sent

**Debug Steps:**

1. **Check Server Console** when Charlie logs in:
   ```
   User logged in: charlie
   ```

2. **Check Client Console** when Charlie's chat view loads:
   ```
   (Should see network activity)
   ```

3. **Add Debug Logging** to verify:

   Edit [ClientHandler.java:367-376](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ClientHandler.java#L367-L376):
   ```java
   private void sendAllGroupsUpdate() {
       if (username == null)
           return;
       Set<String> allGroups = serverState.getGroups().keySet();
       String payload = String.join(",", allGroups);
       
       // ADD THIS DEBUG LINE:
       System.out.println("[DEBUG] Sending groups to " + username + ": " + payload);
       
       Packet updatePacket = new Packet(PacketType.GROUP_LIST_UPDATE, 1);
       updatePacket.setReceiver(username);
       updatePacket.setPayload(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
       sendPacket(updatePacket);
   }
   ```

4. **Restart server** and test again

**Expected Debug Output:**
```
[DEBUG] Sending groups to charlie: developers,testers
```

### Issue 2: Late joiner doesn't see users

**Symptoms:**
- Charlie's user list is empty
- Other users are online

**Debug Steps:**

1. **Add Debug Logging** to [ClientHandler.java:301-316](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ClientHandler.java#L301-L316):
   ```java
   private void broadcastUserList() {
       Map<String, String> statusMap = serverState.getAllUserStatuses();
       StringBuilder sb = new StringBuilder();
       for (Map.Entry<String, String> entry : statusMap.entrySet()) {
           if (sb.length() > 0)
               sb.append("|");
           sb.append(entry.getKey()).append(":").append(entry.getValue());
       }

       // ADD THIS DEBUG LINE:
       System.out.println("[DEBUG] Broadcasting user list: " + sb.toString());

       Packet listPacket = new Packet(PacketType.USER_LIST, 2);
       listPacket.setPayload(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

       for (ClientHandler handler : serverState.getConnectedUsers().values()) {
           handler.sendPacket(listPacket);
       }
   }
   ```

**Expected Debug Output:**
```
[DEBUG] Broadcasting user list: alice:Online|bob:Online|charlie:Online
```

### Issue 3: Groups show but can't join

**Symptoms:**
- Charlie sees groups in list
- Clicking group doesn't open chat window or doesn't join

**Possible Causes:**
1. Group join logic not working
2. Server not adding Charlie to group members

**Debug Steps:**

1. **Check Server Console** when Charlie clicks on a group:
   ```
   charlie joined developers
   ```

2. **Verify group membership** by sending a message from Alice or Bob
   - Charlie should receive it if he's in the group

### Issue 4: Users show but can't private chat

**Symptoms:**
- Charlie sees users in list
- Clicking user doesn't open chat window

**Possible Causes:**
1. E2EE handshake failing
2. Chat window controller issue

**Debug Steps:**

1. **Check Client Console** when Charlie clicks on a user:
   ```
   System: Initiated E2EE with alice...
   System: E2EE established with alice
   ```

2. **Try sending a message** - if E2EE fails, you'll see:
   ```
   System: Securing connection with alice... (Message will be sent automatically)
   ```

---

## 🎯 Success Criteria Checklist

After completing the test, verify:

- [ ] **Late joiner sees all groups** created before they joined
- [ ] **Late joiner sees all online users** who connected before them
- [ ] **Late joiner can join any group** by clicking on it
- [ ] **Late joiner can send group messages** after joining
- [ ] **Late joiner can start private chats** with any online user
- [ ] **Late joiner receives group messages** sent after they join
- [ ] **Late joiner receives private messages** from other users
- [ ] **Early joiners see late joiner** in their user list automatically

---

## 📊 Expected Behavior Summary

```
Timeline:
─────────────────────────────────────────────────────────────
T=0:  Server starts
T=1:  Alice connects  → Sees: [] groups, [alice] users
T=2:  Bob connects    → Sees: [] groups, [alice, bob] users
                       Alice sees: [alice, bob] users
T=3:  Alice creates "developers" group
                     → Alice sees: [developers] groups
                     → Bob sees: [developers] groups
T=4:  Alice creates "testers" group
                     → Alice sees: [developers, testers] groups
                     → Bob sees: [developers, testers] groups
T=5:  Bob joins "developers"
                     → Bob is member of developers
T=6:  Charlie connects (LATE JOINER)
                     → Charlie sees: [developers, testers] groups ✅
                     → Charlie sees: [alice, bob, charlie] users ✅
                     → Alice sees: [alice, bob, charlie] users
                     → Bob sees: [alice, bob, charlie] users
T=7:  Charlie joins "developers"
                     → Charlie is member of developers ✅
                     → Can chat with Bob ✅
T=8:  Charlie clicks on "alice" in user list
                     → Private chat window opens ✅
                     → E2EE handshake initiated ✅
                     → Can send DM to Alice ✅
```

---

## 🔬 Advanced Testing

### Test Scenario 2: Multiple Groups

1. Create 5 groups before late joiner connects
2. Verify late joiner sees all 5 groups
3. Late joiner joins 3 of them
4. Verify late joiner receives messages only in joined groups

### Test Scenario 3: User Status Changes

1. Alice changes status to "Busy"
2. Charlie (late joiner) connects
3. Verify Charlie sees `alice [⊘ Busy]` in user list

### Test Scenario 4: Rapid Connections

1. Start 5 clients in quick succession
2. First client creates groups
3. Verify all clients see the groups

---

## 📝 Code Flow Diagram

```
Late Joiner (Charlie) Connects:
────────────────────────────────

1. Charlie → Server: LOGIN packet
2. Server: Authenticate Charlie
3. Server → Charlie: AUTH_RESPONSE (SUCCESS)
4. Server → Charlie: GROUP_LIST_UPDATE [developers, testers]
5. Server → ALL: USER_LIST [alice:Online, bob:Online, charlie:Online]
6. Charlie's UI loads chat view
7. Charlie → Server: USER_LIST_QUERY
8. Charlie → Server: GROUP_LIST_QUERY
9. Server → Charlie: USER_LIST [alice:Online, bob:Online, charlie:Online]
10. Server → Charlie: GROUP_LIST_UPDATE [developers, testers]
11. Charlie's UI displays groups and users ✅

Charlie Joins Group:
────────────────────

1. Charlie clicks "developers" in group list
2. Chat window opens
3. Charlie → Server: GROUP_JOIN (developers)
4. Server: Add Charlie to developers members
5. Server → developers members: USER_LIST_UPDATE (group context)
6. Charlie can now send/receive in developers ✅

Charlie Starts Private Chat:
────────────────────────────

1. Charlie clicks "alice" in user list
2. Private chat window opens
3. Charlie → Server: KEY_EXCHANGE (init) → Alice
4. Alice → Server: KEY_EXCHANGE (response) → Charlie
5. E2EE established ✅
6. Charlie can send encrypted DMs to Alice ✅
```

---

## ✅ Conclusion

The late joiner functionality is **fully implemented** and should work automatically. If you encounter issues:

1. Check server console for login messages
2. Check client console for network activity
3. Add debug logging as shown in troubleshooting section
4. Verify packet types are being handled correctly

**The system is designed to handle late joiners seamlessly!** 🚀

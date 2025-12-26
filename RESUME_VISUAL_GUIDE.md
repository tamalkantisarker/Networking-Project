# Resume Support - Visual Guide

## 📊 How Resume Works - Visual Flow

![Resume Flow Diagram](C:/Users/USER/.gemini/antigravity/brain/e9290e6c-84fc-42b1-88a0-a79c253b61fa/resume_flow_diagram_1766614041740.png)

---

## 🔍 Key Concepts Explained

### 1. **File ID Generation**
Every file gets a unique, stable identifier:
```java
String fileId = SHA256(fileName + fileSize)
```
- Same file = Same ID (enables resume)
- Different file = Different ID (starts fresh)

### 2. **LSTCI Table (Server-Side)**
**L**ast **S**uccessfully **T**ransmitted **C**hunk **I**ndex

```
┌─────────┬──────────┬────────────┐
│ File ID │ Receiver │ Last Chunk │
├─────────┼──────────┼────────────┤
│ ABC123  │ bob      │ 49         │
│ ABC123  │ alice    │ 102        │
│ XYZ789  │ group1   │ 25         │
└─────────┴──────────┴────────────┘
```

The server tracks progress for **each receiver** separately.

### 3. **Resume Query Protocol**

```
Client                    Server                    Receiver
  │                         │                          │
  │  RESUME_QUERY(fileId)  │                          │
  ├────────────────────────>│                          │
  │                         │ Check LSTCI[fileId][bob] │
  │                         │ = 49                     │
  │  RESUME_INFO(49)        │                          │
  │<────────────────────────┤                          │
  │                         │                          │
  │ Skip chunks 0-49        │                          │
  │ Start from chunk 50     │                          │
  │                         │                          │
  │  FILE_CHUNK(50)         │                          │
  ├────────────────────────>├─────────────────────────>│
  │                         │                          │
```

### 4. **Chunk Acknowledgment**

Every chunk is acknowledged:
```
Sender → Server → Receiver: FILE_CHUNK(index=50)
Receiver → Server: CHUNK_ACK(index=50)
Server: Update LSTCI[fileId][receiver] = 50
Server → Sender: CHUNK_ACK(index=50) [forwarded]
```

This ensures the server always knows the latest progress.

---

## 🎯 Resume Scenarios

### Scenario 1: Direct Message File Transfer

```
alice sends bigfile.bin to bob
├─ Chunks 0-99 sent successfully
├─ alice disconnects (network issue)
├─ alice reconnects
├─ alice sends bigfile.bin to bob again
└─ Resume from chunk 100 ✅
```

### Scenario 2: Group File Transfer

```
alice sends bigfile.bin to group "team"
├─ Members: bob, charlie, diana
├─ Progress:
│   ├─ bob: received chunks 0-150
│   ├─ charlie: received chunks 0-100
│   └─ diana: received chunks 0-120
├─ alice disconnects at chunk 150
├─ alice reconnects and resends
└─ Resume from chunk 100 (minimum across all members) ✅
```

**Why minimum?** To ensure ALL group members get ALL chunks.

### Scenario 3: Server Restart

```
alice sends bigfile.bin to bob
├─ Chunks 0-50 sent successfully
├─ Server restarts (LSTCI table cleared)
├─ alice sends bigfile.bin to bob again
└─ Resume from chunk 0 (no saved progress) ❌
```

**Note:** LSTCI is in-memory only. Server restart = lost progress.

---

## 🔧 Code Locations Reference

### Client-Side Resume

**File:** [NetworkClient.java](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/client/src/main/java/com/securechat/client/NetworkClient.java)

```java
// Lines 538-556: Resume Query
System.out.println("[RESUME] Querying server for existing progress");
CompletableFuture<Integer> resumeFuture = new CompletableFuture<>();
pendingResumeRequests.put(fileId, resumeFuture);

Packet query = new Packet(PacketType.RESUME_QUERY, 1);
query.setFileId(fileId);
query.setReceiver(target);
sendPacket(query);

int lastChunkIndex = resumeFuture.get(2, TimeUnit.SECONDS);

// Lines 578-582: Skip Already-Sent Chunks
int startFrom = lastChunkIndex + 1;
if (startFrom > 0) {
    bis.skip((long) startFrom * FileTransferUtil.CHUNK_SIZE);
    Platform.runLater(() -> controller.appendChat(
        "System: Resuming " + file.getName() + " from chunk " + (startFrom + 1)
    ));
}
```

### Server-Side LSTCI Tracking

**File:** [ServerState.java](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ServerState.java)

```java
// Line 39: LSTCI Table Declaration
private final Map<String, Map<String, Integer>> lstciTable = new ConcurrentHashMap<>();

// Lines 130-134: Update LSTCI
public void updateLSTCI(String fileId, String receiver, int chunkIndex) {
    lstciTable.computeIfAbsent(fileId, k -> new ConcurrentHashMap<>())
            .put(receiver, chunkIndex);
}

// Lines 136-139: Get LSTCI
public int getLSTCI(String fileId, String receiver) {
    return lstciTable.getOrDefault(fileId, Collections.emptyMap())
            .getOrDefault(receiver, -1);
}
```

### Server Resume Query Handler

**File:** [ClientHandler.java](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/server/src/main/java/com/securechat/server/ClientHandler.java)

```java
// Lines 188-230: Handle RESUME_QUERY
case RESUME_QUERY:
    String target = packet.getReceiver();
    String fileId = packet.getFileId();
    int lastChunk = -1;

    // Check if target is a group
    if (serverState.getGroups().containsKey(target)) {
        // Find minimum progress across all group members
        Set<ClientHandler> members = serverState.getGroups().get(target);
        int minChunk = Integer.MAX_VALUE;
        for (ClientHandler member : members) {
            int memberProgress = serverState.getLSTCI(fileId, member.getUsername());
            if (memberProgress != -1) {
                minChunk = Math.min(minChunk, memberProgress);
            } else {
                minChunk = -1; // At least one member hasn't started
                break;
            }
        }
        lastChunk = minChunk;
    } else {
        // Single user
        lastChunk = serverState.getLSTCI(fileId, target);
    }

    // Send RESUME_INFO back
    Packet infoPacket = new Packet(PacketType.RESUME_INFO, 1);
    infoPacket.setFileId(fileId);
    infoPacket.setChunkIndex(lastChunk);
    this.sendPacket(infoPacket);
```

---

## 📈 Performance Benefits

### Bandwidth Savings Example

**Scenario:** 1GB file transfer interrupted at 60%

| Without Resume | With Resume |
|----------------|-------------|
| Transfer 1GB | Transfer 1GB |
| **Interrupt at 600MB** | **Interrupt at 600MB** |
| Restart: Transfer 1GB again | Restart: Transfer 400MB only |
| **Total: 1.6GB transferred** | **Total: 1GB transferred** |
| ❌ 600MB wasted | ✅ 600MB saved |

### Time Savings

Assuming 10 MB/s transfer speed:

| File Size | Interrupted At | Resume Saves |
|-----------|----------------|--------------|
| 100 MB | 50% | ~5 seconds |
| 500 MB | 75% | ~37.5 seconds |
| 1 GB | 90% | ~90 seconds |
| 10 GB | 95% | ~475 seconds (~8 min) |

---

## ✅ Testing Checklist

Use this checklist when testing resume support:

- [ ] **Packet types exist**: `RESUME_QUERY` and `RESUME_INFO` in [PacketType.java](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/common/src/main/java/com/securechat/common/protocol/PacketType.java#L24-L25)
- [ ] **Client sends query**: Check console for `[RESUME] Querying server...`
- [ ] **Server responds**: Check console for `Responded to RESUME_QUERY...`
- [ ] **Client skips chunks**: Check console for `System: Resuming... from chunk X`
- [ ] **Transfer continues**: Chunks sent starting from resume point
- [ ] **File integrity**: SHA-256 hash matches original file
- [ ] **Group resume works**: Minimum progress across all members
- [ ] **UI shows progress**: Status messages visible in chat window

---

## 🚀 Quick Test Commands

```powershell
# Create 50MB test file
fsutil file createnew "$env:USERPROFILE\Desktop\testfile.bin" 52428800

# Verify file was created
Get-Item "$env:USERPROFILE\Desktop\testfile.bin"

# After transfer completes, verify integrity
Get-FileHash "$env:USERPROFILE\Desktop\testfile.bin" -Algorithm SHA256
Get-FileHash "c:\Users\USER\OneDrive\Desktop\Networking Project2\client\downloads\testfile.bin" -Algorithm SHA256
```

---

## 📚 Additional Resources

- **Full Testing Guide**: [RESUME_TESTING_GUIDE.md](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/RESUME_TESTING_GUIDE.md)
- **Quick 5-Min Test**: [QUICK_RESUME_TEST.md](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/QUICK_RESUME_TEST.md)
- **Project Report**: [REPORT.md](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/REPORT.md#L62)

---

**Resume support is fully implemented and ready to use!** 🎉

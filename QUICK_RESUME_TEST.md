# Quick Resume Test - 5 Minute Guide

## 🚀 Fastest Way to Test Resume Support

### Step 1: Create Test File (10 seconds)
```powershell
fsutil file createnew "$env:USERPROFILE\Desktop\bigfile.bin" 52428800
```
This creates a 50MB test file on your Desktop.

### Step 2: Start Server (Terminal 1)
```powershell
cd "c:\Users\USER\OneDrive\Desktop\Networking Project2\server"
mvn javafx:run
```

### Step 3: Start Client 1 - Sender (Terminal 2)
```powershell
cd "c:\Users\USER\OneDrive\Desktop\Networking Project2\client"
mvn javafx:run
```
- Login as: `alice` / `password123`

### Step 4: Start Client 2 - Receiver (Terminal 3)
```powershell
cd "c:\Users\USER\OneDrive\Desktop\Networking Project2\client"
mvn javafx:run
```
- Login as: `bob` / `password123`

### Step 5: Send File
1. In alice's window, click on **bob** in the user list
2. Click **"Send File"** button
3. Select `bigfile.bin` from Desktop
4. **Watch the console** - you'll see chunks being sent

### Step 6: Interrupt Transfer
- When you see something like `chunk 100/800` in the console
- **Close alice's window** (or press Ctrl+C in Terminal 2)

### Step 7: Restart & Resume
1. Restart Terminal 2:
   ```powershell
   mvn javafx:run
   ```
2. Login as `alice` again
3. Send the **same file** (`bigfile.bin`) to `bob` again

### Step 8: Verify Resume ✅

**Look for these messages in alice's console:**
```
[RESUME] Querying server for existing progress of bigfile.bin
[RESUME] Server reports last chunk received: 99
System: Resuming bigfile.bin from chunk 100
```

**Look for this in server console:**
```
Responded to RESUME_QUERY for <fileId> (Target: bob): 99
```

---

## 🎯 Success Indicators

| ✅ What to Look For | Where |
|---------------------|-------|
| `[RESUME] Querying server...` | Client 1 console |
| `[RESUME] Server reports last chunk: X` | Client 1 console |
| `System: Resuming <file> from chunk Y` | Client 1 UI |
| `Responded to RESUME_QUERY...` | Server console |
| Transfer continues from where it stopped | Both consoles |

---

## ❌ If Resume Doesn't Work

1. **Make sure you're sending the EXACT same file** (same name, same size)
2. **Don't restart the server** between tests (LSTCI is in-memory)
3. **Send to the same target** (same username or group)
4. **Check server console** for "Responded to RESUME_QUERY" message

---

## 📊 What's Happening Behind the Scenes

```
1. alice: "Hey server, do you have any chunks of file XYZ for bob?"
2. Server: "Yes! Bob received chunks 0-99"
3. alice: "Great! I'll skip those and start from chunk 100"
4. alice → Server → bob: Chunks 100, 101, 102... (resume!)
```

---

## 🔬 Advanced: Verify File Integrity

After transfer completes, verify the files are identical:

```powershell
# Original file hash
Get-FileHash "$env:USERPROFILE\Desktop\bigfile.bin" -Algorithm SHA256

# Received file hash (in client/downloads folder)
Get-FileHash "c:\Users\USER\OneDrive\Desktop\Networking Project2\client\downloads\bigfile.bin" -Algorithm SHA256

# Hashes should match!
```

---

**Total Test Time: ~5 minutes** ⏱️

For detailed information, see [RESUME_TESTING_GUIDE.md](file:///c:/Users/USER/OneDrive/Desktop/Networking%20Project2/RESUME_TESTING_GUIDE.md)

# Encrypted Chat & File Transfer System - Project Report

## 1. Introduction
This project implements a secure, real-time communication system capable of direct messaging, group chats, and resumable file transfers. It addresses the need for secure communications over untrusted networks by enforcing strong encryption mechanisms (RSA and AES-GCM) at the application layer.

## 2. Motivation
Standard TCP sockets provide reliable delivery but lack built-in security and message prioritization. This project aims to:
- Secure data against eavesdropping and tampering.
- efficient file transfers that do not block chat messages (using priority queues).
- Provide a seamless user experience with resume support for large files.

## 3. Architecture
The system follows a modular **Client-Server** architecture:

- **Common Module**: Contains the `Packet` structure, `PacketType` definitions, `CryptoUtil` (RSA/AES logic), and `FileTransferUtil`.
- **Server Module**:
    - `ServerState`: Singleton managing active users, groups, and encryption keys.
    - `PacketDispatcher`: A dedicated thread ensuring ordered processing of messages.
    - `ClientHandler`: Per-client thread managing TCP connections and decryption.
- **Client Module**:
    - JavaFX-based GUI for user interaction.
    - `NetworkClient`: Handles socket communication and background listening.

## 4. Encryption Design
Security is paramount in this application.
- **Handshake (RSA-2048)**:
    1. Server starts and generates an RSA KeyPair.
    2. Client connects; Server sends RSA Public Key.
    3. Client generates a random AES-256 key, encrypts it with the Server's Public Key, and sends it.
    4. Server decrypts the AES key with its Private Key.
- **Transport (AES-256-GCM)**:
    - All subsequent packets are encrypted using AES in Galois/Counter Mode (GCM).
    - GCM provides both confidentiality and integrity (Auth Tag).
    - A unique IV (Initialization Vector) is generated for every packet to prevent replay attacks.

## 5. Priority Queue System
To prevent large file transfers from stalling chat messages, the server utilizes a `PriorityBlockingQueue`.
- **Priority 1 (High)**: Login, Direct Messages, Group Messages.
- **Priority 3 (Low)**: File Chunks.
The `PacketDispatcher` consumes this queue, ensuring that if a user sends a chat message while a file is uploading, the chat message is processed first.

## 6. Group Chat
Group management is handled in-memory:
- `ServerState` maintains a `Map<String, Set<ClientHandler>>` for groups.
- When a `GROUP_MESSAGE` is received, the server iterates through the set of members and multicasts the packet to all members except the sender.

## 7. Universal Chunking & Protocol Visibility
- **Universal Chunking**: Both files and text messages are treated as chunks. Large messages are automatically split (1KB chunks) and reassembled at the destination.
- **Protocol Logs**: The server provides full visibility for every data packet, logging progress numbers `[X/Y]` for every DM, Group Message, and File routing action.
- **TCP Congestion Feedback**: The server logs real-time events when high-priority chat packets bypass low-priority file chunks in the priority queue.

## 8. Social Features & UI
- **Premium UI**: A sleek, dark-mode JavaFX interface with glassmorphic elements and message bubbles.
- **Global Presence**: All users and groups are visible globally. A user can join any group by clicking the "Join Group" button after selection.
- **Emoji Support**: Expressive communication with a vibrant, built-in emoji palette.
- **End-to-End Encryption (E2EE)**: DMs are secured via Diffie-Hellman key exchange, ensuring the server only sees encrypted hex payloads (Privacy Audit).

## 9. Testing & Verification
The system was tested using the following scenarios:
1. **Handshake**: Verified that AES keys are correctly exchanged and subsequent traffic is unreadable without the key.
2. **Concurrency**: Multiple clients connected simultaneously; Group chats broadcasted correctly.
3. **File Resume**: A file transfer was interrupted by killing the client. Upon restart, the transfer resumed exactly where it left off, verified by SHA-256 checksum comparison of the final file.

## 9. Conclusion
The Secure Chat & File Transfer System successfully demonstrates the integration of advanced networking concepts—cryptography, priority queuing, and state management—into a cohesive Java application. The modular design allows for easy extensibility, while the robust security model ensures data privacy.

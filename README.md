PostOffice
========

Introduction
---------------
*PostOffice* is a socket based library designed for asynchronous interprocess communication via a *PostOffice* server daemon. Clients register a password protected mailbox with the server daemon, which can be used to send and receive mail to and from other client mailboxes.

There are many existing message passing solutions, such as *[DBus](http://dbus.freedesktop.org)* and *[CORBA2](http://www.corba.org)*, but most are highly generalized and intended for large scale deployment, often resulting in hefty documentation and buggy language bindings.

*PostOffice* was created in response to those developers who want a simple, platform independent and intuitive way for passing messages between processes. The system uses the standard terminology of a physical mail system, making it immediately familiar.

This document functions as a description of the underlying protocol for those interested in implementing a client binding for a different programming language.

Architecture
--------------

*PostOffice* (hence the name) works like an ordinary post office, as seen in the figure below.

![enter image description here](https://raw.githubusercontent.com/ownaginatious/postoffice-java/master/Documentation/Diagrams/postoffice_spec.png)

When clients connect to a *PostOffice* server, they are connected to a *clerk*, which operates on behalf of the client on the server. These operations typically involve creating and checking out mail boxes and sending and receiving messages. When a client connects to a clerk, they must bind to a particular mailbox. If one does not exist, the clerk can create one for them. Clerks can also destroy mail boxes when they are no longer needed.

A client can create and destroy as many mailboxes as it likes. Mailboxes are the identifiers for clients in the server. All mailboxes have unique addresses (human readable dot-separated identifiers). Like a physical mailbox, a mailbox can receive and store messages for a particular client while they are disconnected. A mailbox may be checked out by only a single clerk at a time.

Once a mailbox is checked out, a client can send and receive messages via the clerk. Any outgoing mail will be recorded as having originated at that particular mailbox. Mailboxes are password protected to allow only their creators to use them. The protocol by which actions are performed through the client is documented in later sections.

Control Flags
----------------
The following control flag enumerations are used for communication between the client connector and the *PostOffice* server. Each code is represented by a single byte when sent over the socket.

**Requests**

Requests are sent from clients to the server. The table below lists their code names, code numbers and descriptions.

| Code Name             | Code                       | Description             |
| ----------------- | ----------------------------: | ------------------
| `REQBOX`            | 0                                | Checkout an existing mailbox. |
| `RETBOX`            | 1                                | Return an existing mailbox. |
| `CREATEBOX`      | 2                                | Create a new mailbox. |
| `REMOVEBOX`      | 3                                | Delete/destroy an existing mailbox. |
| `SENDLETTER`    | 4                                | Send a letter to a mailbox. |
| `GETMAIL`          | 5                                | Retrieve mail from the connected mailbox. |
| `NEXTLETTER*`  | 6                               | Request the next letter in the connected mailbox. |
| `SATIATED*`      | 7                               | Request for no further message transfers from the connected mailbox. |
| `EMPTYBOX`        | 8                               | Delete all messages in the connected mailbox. |
| `DISCONNECT`    | 9                               | Indicate that the client is going to disconnect from the *PostOffice* server (impeding socket closure). |

Request flags marked by `*` are *non-initiating* flags, meaning they cannot be used as immediate requests to a server, and only as part of another request.

**Responses**

Responses are returned from the server to the client. The table below lists their code names, code numbers and descriptions.

| Code Name             | Code                       | Description              |
 ----------------- | ----------------------------: | ------------------
| `REQGRANTED`    | 0                                | The clients request has been completed successfully. |
| `REQDATA`            | 1                               | The client should now send some data. |
| `NOAUTH`      | 2                                | Client has made a request it is not authorized to perform. |
| `BADCOMMAND`      | 3                                | Client sent an unknown request code. |
| `ALREADYCONN`    | 4                                | Client has requested to connect to a mailbox while already connected to one. |
| `BOXEXISTS`  | 6                               | A box was requested to be created, but it already exists. |
| `NONEXISTBOX`      | 7                               | A request has been made for a box that does not exist.|
| `NOBOXCONN`        | 8                               | A request has been made on a mailbox, but no mailbox has been connected to yet. |
| `BOXINUSE`    | 9                               | The requested mailbox has already been checked out. |
| `DELFILE`    | 10                              | Failed to deliver a letter (the recipient does not exist). |
| `SHUTDOWN`    | 11                               | Indicator that the server is shutting down and will be closing all sockets. |
| `COMMTIMEOUT`    | 10                              | The clients response time has expired; server is going to disconnect. |
| `NOMAIL`    | 10                              | There is no mail of the requested type in the mailbox. |
| `INMAIL`    | 14                              | There is mail of the requested type in the mailbox. |

Data Restrictions
--------------------

**1. Identifiers**

Identifiers specified by clients must only be those recognizable by the regular expression `([a-zA-Z0-9]+\.)*[a-zA-Z0-9]+`

These include expressions of the form: `canada.government.primeminister`, `america.government.president` and so on. It is similar to the *[Java package naming hierarchy](http://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html)*.

All addresses must be communicated in the *UTF-8* encoding.

**2. Passwords**

All passwords must be 16-byte (128-bit) MD5-hashes. This is the only hashing type that the server supports.

**3. Variable length data transmission**

Some of the data transferred between the server and the client is ambiguous in length (i.e. mailbox identifiers, letters. etc). Two different schemes are used for handling this.

>**String data**
>
>For string data (i.e. identifiers), the data is sent in a modified `UTF-8` format that looks as follows `<2-bytes><UTF-8 String bytes>`. The initial 2-bytes are a *short* storing the length of the following UTF-8 string. This limits all identifiers to a maximum length of 65,536 characters. In all sequence diagrams, data of this type will be marked with ***S***.


>**Binary data**
>
>For binary data (i.e. message payloads), the data is sent in a similar way to string data as follows:
`<4-bytes><UTF-8 Data bytes>`. The initial 4-bytes are an *integer* storing the length of the binary stream. This limits all message payloads to a length of 4,294,967,296 bytes, or 4 GB. In all sequence diagrams, data of this type will be marked with ***B***.
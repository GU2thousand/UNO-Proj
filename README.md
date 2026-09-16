[English](#english) | [简体中文](#简体中文)

<a id="english"></a>

# English

# UNO Project


## Authors
- Zhuchang Gu(zg2923@nyu.edu)
- Yangzhou Lin(yl13853@nyu.edu)

## Demo Link: https://vimeo.com/1190970365?share=copy&fl=sv&fe=ci
## Requirements

- Java 17 or newer
- Maven 3.9+

## Build

```bash
mvn compile
```

The project now uses SQLite for persistent player profiles and match history. The database file is created at `data/uno.db`.

## Start the server

Default port:

```bash
mvn exec:java -Dexec.mainClass=server.ServerMain
```

Or use the helper script:

```bash
./run-server.sh
```

Or use Eclipse Run Configurations
![alt text](image.png)

Custom port:

```bash
mvn exec:java -Dexec.mainClass=server.ServerMain -Dexec.args="5051"
```

Or:

```bash
./run-server.sh 5051
```

The project defaults to port `5050` because port `5000` is commonly occupied by macOS system services.

## Start the client

Open a new terminal window for each client:

```bash
mvn exec:java -Dexec.mainClass=client.ClientMain
```

Or:

```bash
./run-client.sh
```
Or use Eclipse run configurations:
![alt text](image-1.png)

In the lobby UI:

1. Enter `127.0.0.1`
2. Enter port `5050` unless you started the server on another port
3. Enter a username
4. Client A creates a room
5. Either let Client B join the same room ID, or have the host click `Add Bot`
6. When the room reaches its selected size, the host clicks `Start Game`
7. During play, `Group Draw` and `Triple Peek` only become usable when you have no ordinary playable card and are otherwise about to draw
8. `Group Draw` and `Triple Peek` now exist in four color variants: red, yellow, green, and blue
9. If the host starts another game after `Game Over`, the same room can begin a new round without reconnecting
10. After a match ends, the server writes match history and refreshes each human player's stats in the UI

## Advanced Concepts Used

### 1. GUI Programming
We developed a graphical Java client using Swing to provide an interactive game interface. 
Players can view cards, select actions through buttons, and receive real-time game updates through the GUI instead of a command-line interface.

### 2. Networking and Client-Server Architecture
The project uses a client-server architecture that allows multiple players to connect to the same UNO game room through network sockets. 
The server is responsible for synchronizing the game state, handling player requests, and broadcasting updates to all connected clients.

### 3. Multithreading
The server is implemented as a multithreaded application so it can handle multiple client connections concurrently. 
Each client connection runs on a separate thread, allowing multiple players to interact with the game simultaneously.

### 4. Database Persistence with SQLite
The project uses SQLite to persist player accounts, match history, and game statistics. 
SQLite provides lightweight local database storage and allows the system to save and retrieve data across different game sessions using SQL queries and JDBC.

## Notes

- If the server says `Port XXXX is already in use`, start it with another port and use the same port in both clients.
- Some macOS Swing runs print `TSM` or `IMKCFRunLoopWakeUpReliable` lines in the terminal. Those are system input-method logs and are not the actual game error.

---

<a id="简体中文"></a>

# 简体中文

# UNO 项目

## 作者

- Zhuchang Gu(zg2923@nyu.edu)
- Yangzhou Lin(yl13853@nyu.edu)

## 演示链接

https://vimeo.com/1190970365?share=copy&fl=sv&fe=ci

## 环境要求

- Java 17 或更新版本
- Maven 3.9+

## 构建

```bash
mvn compile
```

项目使用 SQLite 持久化玩家资料和对局历史，数据库文件创建于 `data/uno.db`。

## 启动服务器

默认端口：

```bash
mvn exec:java -Dexec.mainClass=server.ServerMain
```

也可以使用辅助脚本：

```bash
./run-server.sh
```

也可以使用 Eclipse Run Configurations：

![Eclipse 服务器运行配置](image.png)

自定义端口：

```bash
mvn exec:java -Dexec.mainClass=server.ServerMain -Dexec.args="5051"
```

或者：

```bash
./run-server.sh 5051
```

项目默认使用 `5050` 端口，因为 macOS 系统服务经常占用 `5000` 端口。

## 启动客户端

为每个客户端打开一个新终端窗口：

```bash
mvn exec:java -Dexec.mainClass=client.ClientMain
```

或者：

```bash
./run-client.sh
```

也可以使用 Eclipse Run Configurations：

![Eclipse 客户端运行配置](image-1.png)

在大厅界面中：

1. 输入 `127.0.0.1`
2. 输入端口 `5050`；如果服务器使用其他端口，请填写对应端口
3. 输入用户名
4. 客户端 A 创建房间
5. 让客户端 B 使用相同房间 ID 加入，或由房主点击 `Add Bot` 添加机器人
6. 房间达到选定人数后，由房主点击 `Start Game`
7. 游戏中，只有没有普通可出牌、即将摸牌时，才可使用 `Group Draw` 和 `Triple Peek`
8. `Group Draw` 和 `Triple Peek` 均有红、黄、绿、蓝四种颜色
9. 在 `Game Over` 后，房主可在同一房间开始新一局，无需重新连接
10. 对局结束后，服务器保存对局历史，并刷新界面中每位真人玩家的统计数据

## 使用的高级概念

### 1. GUI 编程

使用 Swing 构建交互式 Java 图形客户端。玩家可查看手牌、通过按钮选择操作，并在 GUI 中接收实时更新，无需命令行界面。

### 2. 网络与客户端—服务器架构

多个玩家通过网络 socket 连接同一 UNO 房间。服务器同步游戏状态、处理玩家请求，并向所有已连接客户端广播更新。

### 3. 多线程

服务器通过多线程并发处理多个客户端连接。每个连接运行在独立线程上，让多名玩家同时参与游戏。

### 4. SQLite 数据持久化

使用 SQLite 保存玩家账号、对局历史和游戏统计。通过 SQL 查询及 JDBC，轻量级本地数据库可跨游戏会话保存和读取数据。

## 注意事项

- 如果服务器提示 `Port XXXX is already in use`，请换用其他端口，并让两个客户端使用相同端口。
- 部分 macOS Swing 运行过程会在终端打印 `TSM` 或 `IMKCFRunLoopWakeUpReliable`，这些是系统输入法日志，并非实际游戏错误。

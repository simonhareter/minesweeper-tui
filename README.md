<img width="636" height="204" alt="image" src="https://github.com/user-attachments/assets/03e5fec5-c5c9-4f4f-b426-314720396101" />
<br><br>

A terminal-based classic Minesweeper game written in Java 25 with rankings stored in a SQLite database (jdbc).

## Features:
-  Raw terminal input
-  Cursor-based rendering
-  3 different difficulty modes (Beginner, Intermediate, Expert)
-  A ranking system for best completion times

## Controls
### Movement
- wasd, arrows, hjkl
### General
- Enter: Select option
- r: Reset game
- f: Place flag
- m: Return to menu
- q: Quit game

## System Requirements
- Have a Java 25+ runtime on your system
- Make

## Dependencies
No manual installation required. It is downloaded on first build if missing!
- SQLite JDBC driver

## How to play
### 2. Clone the repository
```
git clone git@github.com:simonhareter/minesweeper-tui.git
```
### 3. Navigate to the repository
```
cd minesweeper-tui
```
### 4. Build
```
make build
```
### 5. Run
```
make run
```

<br>
<img width="400" height="631" alt="image" src="https://github.com/user-attachments/assets/48dc1256-78e2-4a82-b208-b31eb9550c41" />
<img width="470" height="631" alt="image" src="https://github.com/user-attachments/assets/9ec17a8d-094e-4519-a224-130e3166dfbe" />

## Make commands
```
make build
make run
make clean
```

## License

This project is licensed under the MIT License.

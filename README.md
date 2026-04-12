<p align="center">
  <img width="636" height="204" alt="image" src="https://github.com/user-attachments/assets/03e5fec5-c5c9-4f4f-b426-314720396101" />
</p>
<br><br>

A terminal-based classic Minesweeper game written in Java 25 with rankings stored in a SQLite database (jdbc).

## Features
-  Raw terminal input
-  Cursor-based rendering
-  3 different difficulty modes (Beginner, Intermediate, Expert)
-  A ranking system for best completion times

## Controls
### Movement
- wasd, arrow keys, hjkl
### General
- Enter : Select option
- r : Reset game
- f : Place flag / Remove flag
- m : Return to menu
- q : Quit game

## System Requirements
- Linux / MacOS / Windows WSL
- Have a Java 25+ SDK on your system
```
javac --version
```
- Make

## Dependencies
No manual installation required. It is downloaded on first build if missing!
- SQLite JDBC driver

## How to play
### 1. Clone the repository
```
git clone git@github.com:simonhareter/minesweeper-tui.git
```
### 2. Navigate to the repository
```
cd minesweeper-tui
```
### 3. Build
```
make build
```
### 4. Run
```
make run
```

<br>
<img width="380" height="631" alt="image" src="https://github.com/user-attachments/assets/48dc1256-78e2-4a82-b208-b31eb9550c41" />
<img width="450" height="631" alt="image" src="https://github.com/user-attachments/assets/9ec17a8d-094e-4519-a224-130e3166dfbe" />

## Make commands
```
make build
make run
make clean
```

## License

This project is licensed under the MIT License.

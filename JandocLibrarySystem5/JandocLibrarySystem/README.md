# Jandoc Library Management System

A desktop Library Management System built with **Java Swing** and **MongoDB**, made for Lab 1.

## What it covers (mapped to the requirements)

| Requirement | Where it's implemented |
|---|---|
| Named after you | Title bar, header, and window title all say "Jandoc Library Management System" |
| Modified colors (background + CRUD interface) | `ui/UITheme.java` — blue & white palette applied to every screen |
| Borrower classifications (student/visitor/employee) | `UserService.ROLE_STUDENT/ROLE_VISITOR/ROLE_EMPLOYEE`, chosen via dropdown on registration and in the Members tab |
| Google-style login interface | `ui/LoginFrame.java` — centered white card, circular avatar, "Sign in" heading, clean sans-serif fields, blue action button (styled like Google's account sign-in page) |
| Full CRUD | Books tab (`BookPanel`), Members tab (`UserPanel`), and Borrow/Return tab (`BorrowPanel`) |

## Project structure

```
JandocLibrarySystem/
├── pom.xml
└── src/main/java/com/jandoc/library/
    ├── Main.java                  # entry point
    ├── config/MongoConfig.java    # MongoDB connection manager
    ├── service/
    │   ├── BookService.java       # book CRUD
    │   ├── UserService.java       # member CRUD + login auth
    │   └── LibraryService.java    # borrow / return transaction logic
    └── ui/
        ├── UITheme.java           # colors, fonts, styled components
        ├── LoginFrame.java        # Google-style sign-in screen
        ├── RegisterDialog.java    # create-account dialog
        ├── MainFrame.java         # main window with tabs
        ├── BookPanel.java         # Books CRUD tab
        ├── UserPanel.java         # Members CRUD tab
        └── BorrowPanel.java       # Borrow / Return tab
```

## How the data is modeled (MongoDB)

**`books` collection** — one document per title:
```json
{ "_id": ObjectId, "title": "...", "author": "...", "isbn": "...",
  "category": "...", "totalCopies": 5, "availableCopies": 3 }
```

**`users` collection** — one document per member, with borrow history embedded directly (no separate transactions collection needed):
```json
{ "_id": ObjectId, "name": "...", "email": "...", "password": "<sha-256 hash>",
  "role": "Student | Visitor | Employee",
  "borrowedBooks": [
    { "bookId": ObjectId, "title": "...", "isbn": "...", "borrowDate": ISODate,
      "dueDate": ISODate, "returned": false }
  ]
}
```

Borrowing a book pushes a new entry into `borrowedBooks` and decrements the book's
`availableCopies`. Returning it flips that entry's `returned` flag to `true` and
increments `availableCopies` back up — both collections stay in sync.

## Setup

**Requirements:** JDK 17+, Maven, and MongoDB running locally (you said this is already running on `localhost:27017` — no setup needed there).

### Option A — VS Code (recommended, project is pre-configured)

1. Unzip the project and open the `JandocLibrarySystem` folder in VS Code
   (`File > Open Folder…`).
2. Install the recommended extensions when VS Code prompts you (or manually install
   **Extension Pack for Java** and **Maven for Java** from the Extensions panel).
   The project ships a `.vscode/extensions.json` that will suggest these automatically.
3. Wait a few seconds for VS Code to detect the Maven project and download the
   MongoDB driver dependency (bottom-right progress notification).
4. Run it any of these ways:
   - Press **F5** (or **Run > Start Debugging**) — uses the included `launch.json`
     configuration, "Run Jandoc Library System".
   - Open `Main.java`, click the **▷ Run** button that appears above `public static void main`.
   - Open the **Terminal** in VS Code and run `mvn clean compile exec:java`.
   - Press **Ctrl+Shift+B** (Cmd+Shift+B on Mac) to run the default build task
     (`Maven: clean compile`), then **Ctrl+Shift+P > Tasks: Run Task > Run via Maven exec**.

### Option B — command line only

1. Unzip the project and open a terminal in the `JandocLibrarySystem` folder.
2. Build and run:
   ```bash
   mvn clean compile exec:java
   ```
   or build a runnable jar you can double-click / run anywhere:
   ```bash
   mvn clean package
   java -jar target/JandocLibrarySystem.jar
   ```

### First run

The `users` and `books` collections don't exist yet — MongoDB creates them
automatically the first time you register a member or add a book. Click
**"Create account"** on the login screen to make your first member account, then
sign in.

## Using the app

1. **Sign in** — Google-style card, enter email + password (or "Create account" first).
2. **Books tab** — add a book (title, author, ISBN, category, total copies), search,
   select a row to edit or delete it.
3. **Members tab** — add/edit/delete members and set their borrower type
   (Student / Visitor / Employee).
4. **Borrow / Return tab** — pick a member from the dropdown, type a book's ISBN and
   click **Borrow Book**. The table below shows that member's active borrows; select
   one and click **Return Selected Book** to check it back in.

## Customizing further (if your instructor asks you to explain changes you made)

- Colors: everything lives in `UITheme.java` — change the `Color` constants there
  and the whole app updates.
- Database name: `MongoConfig.DATABASE_NAME` (currently `JandocLibraryDB`).
- Loan period: `LibraryService.borrowBook()` currently sets a 14-day due date —
  change the `14L` to whatever your policy requires.

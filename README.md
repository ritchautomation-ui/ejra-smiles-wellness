# EJRA Smiles & Wellness

> **GitHub** = project repository (source code for everything)
> **Vercel** = frontend deployment (public website + admin dashboard)
> **Railway** = Java backend deployment (API + `Lead[]` array + Binary Search Tree)

## Project Description

A dental clinic **Lead / Appointment Management System**. Patients request appointments on the public website. Clinic staff manage those leads in the **Lead Catcher** admin dashboard. A Java backend stores every lead in a fixed-size `Lead[]` array **and** in a custom **Binary Search Tree** keyed by Lead ID.

## Laboratory Task 4

| Requirement | Where it is |
|---|---|
| GUI with title, dashboard, buttons, inputs, table, messages | `frontend/admin/` (Lead Catcher) |
| Add / View / Search / Delete record | Add Lead, Leads, Search Lead pages, all calling the Java API |
| Custom BST (Insert, Search, Delete, 3 traversals) | `backend/.../store/LeadBST.java` |
| BST connected to the real system | `LeadStore.java` inserts/deletes in **array and BST together** |
| Traversal feature in the GUI | **BST Traversal** page (buttons, result sequence, explanation, tree diagram) |
| Validation and error handling | Client checks in `admin/script.js`, server checks in `LeadStore.java` / `ApiHandler.java` |

There is **no separate BST demo**. The BST is the search engine of the actual system.

## Technologies
- Java (JDK 17+), Java `HttpServer` (no Spring Boot, no database)
- HTML, CSS, JavaScript (frontend only sends requests and displays results)
- Custom Array, Custom Binary Search Tree
- GitHub, Vercel, Railway (Docker)

## Features
- Public website with an appointment request form
- Admin dashboard: statistics, recent requests, all leads, add, search, delete, change status, appointments list
- BST traversal page with Inorder / Preorder / Postorder, explanation of the order, and a tree diagram
- Input validation with clear messages; the app never crashes on bad input
- Configurable API address (`frontend/config.js`) and CORS support for Vercel ↔ Railway

## Data Structures

### Lead Array
`LeadStore.java` keeps `Lead[] leads = new Lead[100]` and a counter `count`. Add appends at `leads[count++]`. Delete finds the lead and shifts later elements left. View returns the array in insertion order.

### Binary Search Tree
`LeadBST.java` is a hand-written BST. Each `Node` holds the **Lead ID (key)**, the `Lead` object, and `left` / `right` links.
Rule: smaller IDs go **left**, larger IDs go **right**. Duplicate IDs are rejected.

The sample data (`Main.java`) is inserted in this order, giving a balanced tree:

```
            1003 (Juan)
           /            \
     1001 (Maria)     1005 (Pedro)
          \             /
        1002 (Angela) 1004 (Mark)
```

**Array and BST stay synchronized:** every add and delete goes through `LeadStore`, which updates both. `GET /api/stats` reports `inSync: true`.

## BST Operations

| Operation | How it works | Time (average) |
|---|---|---|
| **Insert** | Compare with each node, go left/right until an empty spot, attach the new node | O(log n) |
| **Search** | Start at the root, go left if smaller, right if larger, stop when equal or empty | O(log n) |
| **Delete** | Leaf: remove. One child: replace by the child. Two children: copy the **inorder successor** (smallest key in the right subtree), then delete that successor | O(log n) |
| **Inorder** | Left → Root → Right. Gives IDs in **ascending sorted order** | O(n) |
| **Preorder** | Root → Left → Right. Parents before children (rebuilds the same tree shape) | O(n) |
| **Postorder** | Left → Right → Root. Children before parents (safe for deleting a tree) | O(n) |

With the sample tree: Inorder `1001, 1002, 1003, 1004, 1005` · Preorder `1003, 1001, 1002, 1005, 1004` · Postorder `1002, 1001, 1004, 1005, 1003`.
(Worst case is O(n) if IDs are added in strictly ascending order, because the tree becomes a straight line. Balanced trees such as AVL are a possible future improvement.)

## Project Structure

```
ejra-smiles-wellness/
├── backend/src/com/ejra/smiles/
│   ├── Main.java                 entry point (PORT env var, 0.0.0.0, seeds sample leads)
│   ├── model/Lead.java           one lead + JSON output
│   ├── store/LeadStore.java      Lead[] array + BST kept in sync, all validation
│   ├── store/LeadBST.java        custom Binary Search Tree
│   └── server/
│       ├── ApiHandler.java       REST API + CORS
│       └── StaticFileHandler.java  serves /frontend for LOCAL use only
├── frontend/
│   ├── config.js                 API_BASE setting (edit after Railway deploy)
│   ├── public/  index.html, style.css, script.js     public website
│   └── admin/   index.html, style.css, script.js     Lead Catcher dashboard
├── Dockerfile                    Railway build (Java only)
├── vercel.json                   Vercel routing (/ and /admin)
├── .gitignore
└── README.md
```

### API summary
| Method & path | Purpose |
|---|---|
| `GET /api/leads` | View all leads |
| `POST /api/leads` | Add lead (array + BST insert). `leadId` optional; auto-generated if omitted |
| `GET /api/leads/{id}` | Search by Lead ID (BST search, returns the path taken) |
| `DELETE /api/leads/{id}` | Delete (array + BST delete) |
| `PUT /api/leads/{id}/status` | Change status |
| `GET /api/stats` | Dashboard numbers |
| `GET /api/bst/inorder` `preorder` `postorder` | Traversals |
| `GET /api/bst/tree` | Tree structure for the diagram |

Error codes: `400` invalid/empty input · `404` lead not found · `409` duplicate ID · `500` unexpected error.

## Run Locally

Requirements: **JDK 17 or newer** (`java -version` and `javac -version` must both work).

Open a terminal **in the project root** (the folder that contains `backend` and `frontend`):

```bash
# Mac / Linux / Git Bash
mkdir -p out
javac -d out $(find backend/src -name "*.java")
java -cp out com.ejra.smiles.Main
```

```powershell
# Windows PowerShell
mkdir out -Force
javac -d out (Get-ChildItem -Recurse backend\src -Filter *.java | ForEach-Object FullName)
java -cp out com.ejra.smiles.Main
```

Then open:
- Website: <http://localhost:8080/>
- Admin: <http://localhost:8080/admin/>
- API: <http://localhost:8080/api/leads>

Stop the server with `Ctrl + C`. Data is kept in memory, so it resets to the 5 sample leads on every restart (no database, by design).

## GitHub Setup

Beginner-friendly, step by step.

### 1. Install and create an account
1. Install **Git**: <https://git-scm.com/downloads> (accept the defaults).
2. Create a free account at <https://github.com>.

### 2. Create the repository (one person only)
1. On GitHub click **+** (top right) → **New repository**.
2. **Repository name:** `ejra-smiles-wellness`
3. Choose **Public** (or Private and add your teammates later).
4. **Do NOT** tick "Add a README", ".gitignore" or a license (the project already has them).
5. Click **Create repository**.
6. Copy the repository URL shown, e.g. `https://github.com/YOUR-USERNAME/ejra-smiles-wellness.git`

### 3. Push the project
Open a terminal inside the `ejra-smiles-wellness` folder:

```bash
git init
git add .
git commit -m "Initial EJRA Smiles project"
git branch -M main
git remote add origin <REPOSITORY_URL>
git push -u origin main
```

**Where to paste your URL:** replace `<REPOSITORY_URL>` (including the `<` `>`) with the URL you copied, for example
`git remote add origin https://github.com/YOUR-USERNAME/ejra-smiles-wellness.git`.
If Git asks who you are, run once:
`git config --global user.name "Your Name"` and `git config --global user.email "you@email.com"`.
When asked to sign in, use your GitHub login in the browser window (or a Personal Access Token as the password).

### 4. Verify
Refresh the repository page on GitHub. You should see `backend`, `frontend`, `Dockerfile`, `vercel.json`, `.gitignore`, `README.md`.

### 5. Group members: clone the repository
```bash
git clone https://github.com/YOUR-USERNAME/ejra-smiles-wellness.git
cd ejra-smiles-wellness
```
(The owner must add members first: GitHub repo → **Settings → Collaborators → Add people**.)

### 6. Each member works on their own branch
Suggested branches: `main`, `frontend`, `bst`, `api`, `validation`, `testing`.
```bash
git checkout -b bst          # create and switch to branch "bst"
# ...edit files...
git status                   # see what changed
git add .
git commit -m "Describe what you changed"
git push -u origin bst       # first push of this branch
git push                     # later pushes
```
Switch branches with `git checkout frontend`. See all branches with `git branch`.

### 7. Merge into main
**Easiest (recommended): Pull Request on GitHub**
1. After pushing, open the repo on GitHub → **Compare & pull request** (yellow banner).
2. Base: `main` ← Compare: your branch → **Create pull request**.
3. A teammate reviews, then click **Merge pull request** → **Confirm merge**.

**Or in the terminal:**
```bash
git checkout main
git pull origin main
git merge bst
git push origin main
```
Before starting new work, always update your copy: `git checkout main` then `git pull origin main`.
If Git reports a **merge conflict**, open the marked files, keep the correct lines (remove `<<<<<<<`, `=======`, `>>>>>>>`), then `git add .` and `git commit`.

## Railway Backend Deployment

The Java backend is built from the `Dockerfile` in the repository root (root directory: **leave as `/`**).

1. Go to <https://railway.app> and **Sign in with GitHub**.
2. Click **New Project** → **Deploy from GitHub repo**.
3. Select **ejra-smiles-wellness** (allow Railway to access the repo if asked).
4. Railway detects the `Dockerfile` and builds automatically. No root-directory change is needed because the Dockerfile is at the repo root.
5. Open the service → **Settings → Networking → Generate Domain**. Copy the URL, e.g. `https://ejra-smiles-production.up.railway.app`.
   (If Railway asks for a port, enter `8080`. The app reads Railway's `PORT` automatically.)
6. Test in your browser: `https://YOUR-RAILWAY-DOMAIN/api/leads`. You should see JSON with 5 leads.
7. **Logs:** open the service → **Deployments** → click the latest deployment → **Build Logs** (compile errors appear here) and **Deploy Logs** (runtime output). A healthy start prints `EJRA Smiles & Wellness is running on port ...`.

## Vercel Frontend Deployment

1. Go to <https://vercel.com> and **Sign up / Log in with GitHub**.
2. Click **Add New… → Project**.
3. **Import** the `ejra-smiles-wellness` repository.
4. Configure:
   - **Framework Preset:** Other
   - **Root Directory:** leave as `./` (the repo root). `vercel.json` already sets `outputDirectory` to `frontend` and adds the routes for `/` and `/admin`.
   - **Build Command / Install Command:** leave empty (static site).
5. Click **Deploy**.
6. Copy your URL, e.g. `https://ejra-smiles-wellness.vercel.app`.
   - `https://YOUR-PROJECT.vercel.app/` → public website
   - `https://YOUR-PROJECT.vercel.app/admin/` → Lead Catcher dashboard

Vercel never runs the Java code. It only serves the static files.

## Connecting Frontend to Backend

Open **`frontend/config.js`** and replace `YOUR-RAILWAY-DOMAIN`:

```js
// BEFORE
const RAILWAY_API = "https://YOUR-RAILWAY-DOMAIN.up.railway.app/api";
// AFTER (example)
const RAILWAY_API = "https://ejra-smiles-production.up.railway.app/api";
```

Keep `https://` at the start and `/api` at the end, with no trailing slash. Then:

```bash
git add frontend/config.js
git commit -m "Set Railway API URL"
git push
```
Vercel redeploys automatically after every push to `main`. All frontend code reads `CONFIG.API_BASE`, so this is the only place to edit. On `localhost` the config automatically uses `http://localhost:8080/api`.

### CORS (restrict it after deployment)
The API sends `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods` and `Access-Control-Allow-Headers`, and answers browser `OPTIONS` preflight requests with `204`.

By default it allows **any** origin (`*`), which is fine while developing. After you know your Vercel URL, restrict it:

1. Railway → your service → **Variables** → **New Variable**
2. Name: `ALLOWED_ORIGIN`  Value: `https://ejra-smiles-wellness.vercel.app` (your real Vercel URL, no trailing slash, no path)
3. Railway redeploys. Now only your Vercel site may call the API.

(The code lives at the top of `ApiHandler.java`: it reads `ALLOWED_ORIGIN` and falls back to `"*"`.)
To test the restricted version locally you would set `ALLOWED_ORIGIN=http://localhost:8080`; note that pages served by the same Java server on localhost do not need CORS at all.

## Testing

Run from the deployed Vercel site (or locally) and tick each item:

- [ ] 1. Public website loads
- [ ] 2. Admin dashboard loads at `/admin/` and shows "Backend connected"
- [ ] 3. Sample leads display (5 leads)
- [ ] 4. Add Lead works (e.g. ID `1006`)
- [ ] 5. The new lead appears in the Leads table
- [ ] 6. Search by Lead ID (`1006`) works and shows the BST path
- [ ] 7. Delete Lead works (confirm dialog)
- [ ] 8. The deleted record disappears
- [ ] 9. Inorder gives ascending order
- [ ] 10. Preorder works (root first)
- [ ] 11. Postorder works (root last)
- [ ] 12. Dashboard statistics update after add/delete
- [ ] 13. Invalid input shows an error: empty fields, `abc` as ID, duplicate ID, bad email/phone
- [ ] 14. Searching or deleting a nonexistent ID (e.g. `9999`) shows a not-found message
- [ ] 15. Browser console (F12) shows **no CORS error**

Quick API check without the frontend: `curl https://YOUR-RAILWAY-DOMAIN/api/bst/inorder`

## Group Members and Contributions

Replace the names and adjust the split to match your group. **Every member must be able to explain their part.**

| Member | Branch | Contribution | Files to know |
|---|---|---|---|
| _Member 1_ | `frontend` | Admin GUI layout, navigation, tables, messages | `frontend/admin/*` |
| _Member 2_ | `bst` | BST Insert, Search, Delete | `LeadBST.java` |
| _Member 3_ | `bst` | Inorder / Preorder / Postorder + traversal page | `LeadBST.java`, `admin/script.js` (traversal part) |
| _Member 4_ | `api` | Array + BST integration, REST API, CORS, deployment | `LeadStore.java`, `ApiHandler.java`, `Main.java`, `Dockerfile` |
| _Member 5_ | `validation` / `testing` | Validation, error handling, test checklist, README | `LeadStore.java` (validation), `admin/script.js` (form checks) |

### Defense cheat sheet
- **Why does Inorder come out sorted?** It visits smaller IDs (left), then the node, then larger IDs (right). That is exactly ascending order.
- **How is the array kept in sync with the BST?** Only `LeadStore` changes either one, and it always changes both in the same `synchronized` method.
- **How does delete work with two children?** Copy the smallest key from the right subtree (inorder successor) into the node, then delete that successor.
- **Why a BST instead of scanning the array?** Search checks about log₂(n) nodes instead of up to n on a balanced tree.
- **Where is the BST?** Only in Java (`LeadBST.java`). JavaScript never builds a tree; it only displays what the API returns.

## Important Notes
- **GitHub** = project repository · **Vercel** = frontend deployment · **Railway** = Java backend deployment.
- Do **not** run the Java `HttpServer` on Vercel; Vercel hosts only `frontend/`.
- Data is stored **in memory** (no database). Restarting or redeploying the Railway service resets it to the 5 sample leads. This is expected for this laboratory stage.
- Lead storage holds at most **100** leads (`LeadStore.CAPACITY`); the API returns a clear error when full.
- The public website assigns Lead IDs automatically (starting at 1001, continuing after the highest used ID). Staff enter their own Lead ID in the admin form.
- Railway's free/trial tier may put the service to sleep; the first request after idle can be slow.
- Planned (not yet implemented) structures from the DSA Integration Plan: linked list, queue, heap, hash table, graph, BFS/DFS, sorting. Only the Array, sequential search, and BST are implemented now.

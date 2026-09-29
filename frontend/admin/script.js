/*
 * Lead Catcher admin dashboard.
 * This file only SENDS REQUESTS to the Java backend and DISPLAYS results.
 * The Lead[] array and the Binary Search Tree live in Java (never in JavaScript).
 * All URLs come from CONFIG.API_BASE (see /config.js).
 */
const $ = (id) => document.getElementById(id);
let leads = [];          // last list received from GET /api/leads (display only)

/* ------------------------------------------------------------ helpers */
function esc(v) {
  return String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

let toastTimer;
function toast(text, ok = true) {
  const t = $("toast");
  t.textContent = text;
  t.className = "toast show " + (ok ? "ok" : "err");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (t.className = "toast"), 5000);
}

/** Calls the Java API. Throws an Error with a readable message on any failure. */
async function api(path, options = {}) {
  let res;
  try {
    res = await fetch(CONFIG.API_BASE + path, {
      headers: { "Content-Type": "application/json" },
      ...options
    });
  } catch (e) {
    setConn(false);
    throw new Error("Cannot reach the backend. Check that the Java server is running and CONFIG.API_BASE is correct.");
  }
  setConn(true);
  let json;
  try { json = await res.json(); } catch (e) { throw new Error("The server sent an unexpected response."); }
  if (!res.ok || json.success === false) throw new Error(json.error || "Request failed (" + res.status + ").");
  return json;
}

function setConn(ok) {
  const c = $("conn");
  c.textContent = ok ? "Backend connected" : "Backend offline";
  c.className = "conn " + (ok ? "ok" : "bad");
}

const pill = (s) => `<span class="pill s-${esc(s.split(" ")[0])}">${esc(s)}</span>`;

/* ---------------------------------------------------------- navigation */
const TITLES = {
  dashboard: ["Dashboard", "Overview of every lead captured by the website and clinic staff."],
  leads: ["Leads", "View, update and delete leads. Every change is applied to the array and the BST."],
  add: ["Add Lead", "Create a lead. It is stored in Lead[] and inserted into the Binary Search Tree."],
  search: ["Search Lead", "Find a lead by Lead ID using the Binary Search Tree."],
  appointments: ["Appointments", "Preferred appointment dates, earliest first."],
  bst: ["BST Traversal", "Run Inorder, Preorder or Postorder on the Java BST and see the resulting order."]
};

function go(page) {
  document.querySelectorAll(".page").forEach((p) => (p.hidden = p.id !== "page-" + page));
  document.querySelectorAll("#nav button").forEach((b) => b.classList.toggle("active", b.dataset.page === page));
  $("title").textContent = TITLES[page][0];
  $("subtitle").textContent = TITLES[page][1];
  if (page === "bst") loadTree();
  else if (page !== "add" && page !== "search") refresh();
}
$("nav").addEventListener("click", (e) => { if (e.target.dataset.page) go(e.target.dataset.page); });
document.querySelectorAll("[data-go]").forEach((b) => b.addEventListener("click", () => go(b.dataset.go)));

/* ------------------------------------------------- load + render data */
async function refresh() {
  try {
    const [l, s] = await Promise.all([api("/leads"), api("/stats")]);
    leads = l.leads;
    renderStats(s.stats);
    renderRecent();
    renderLeads();
    renderAppointments();
  } catch (e) { toast(e.message, false); }
}

function renderStats(s) {
  const box = (label, n, extra = "") => `<div class="stat"><small>${label}</small><b ${extra}>${n}</b></div>`;
  $("stats").innerHTML =
    box("Total leads", s.total) + box("New leads", s.new, 'style="color:var(--accent)"') +
    box("Contacted", s.contacted) + box("Appointment requests", s.appointmentRequested) +
    box("Converted", s.converted) + box("BST height", s.bstHeight);
}

function renderRecent() {
  const rows = leads.slice(-5).reverse().map((l) =>
    `<tr><td><b>${esc(l.name)}</b></td><td>${esc(l.service)}</td><td>${esc(l.preferredDate)}</td><td>${pill(l.status)}</td></tr>`).join("");
  $("recentBody").innerHTML = rows || `<tr><td colspan="4" class="empty">No leads yet. Add one from the Add Lead page.</td></tr>`;
}

function renderLeads() {
  $("leadCount").textContent = "(" + leads.length + " of 100)";
  const opts = (cur) => ["New", "Contacted", "Appointment Requested", "Converted"]
    .map((s) => `<option ${s === cur ? "selected" : ""}>${s}</option>`).join("");
  $("leadsBody").innerHTML = leads.map((l) => `
    <tr>
      <td><b>${l.leadId}</b></td>
      <td>${esc(l.name)}<small>${esc(l.dateSubmitted)}</small></td>
      <td>${esc(l.email)}<small>${esc(l.phone)}</small></td>
      <td>${esc(l.service)}</td>
      <td>${esc(l.preferredDate)}<small>${esc(l.preferredTime)}</small></td>
      <td>${esc(l.source)}</td>
      <td><select data-status="${l.leadId}" aria-label="Status of lead ${l.leadId}">${opts(l.status)}</select></td>
      <td><button class="btn danger" data-del="${l.leadId}">Delete</button></td>
    </tr>`).join("") || `<tr><td colspan="8" class="empty">No leads to show.</td></tr>`;
}

function renderAppointments() {
  const sorted = [...leads].sort((a, b) => (a.preferredDate + a.preferredTime).localeCompare(b.preferredDate + b.preferredTime));
  $("apptBody").innerHTML = sorted.map((l) =>
    `<tr><td>${esc(l.preferredDate)}</td><td>${esc(l.preferredTime)}</td><td><b>${esc(l.name)}</b></td>
     <td>${esc(l.service)}</td><td>${esc(l.notes) || "-"}</td><td>${pill(l.status)}</td></tr>`).join("")
    || `<tr><td colspan="6" class="empty">No appointments yet.</td></tr>`;
}

/* ------------------------------------------------- delete + status */
async function deleteLead(id) {
  if (!confirm("Delete lead " + id + "? It will be removed from the array and the BST.")) return;
  try {
    const r = await api("/leads/" + id, { method: "DELETE" });
    toast(r.message, true);
    $("searchResult").innerHTML = "";
    refresh();
  } catch (e) { toast(e.message, false); refresh(); }
}

$("leadsBody").addEventListener("click", (e) => { if (e.target.dataset.del) deleteLead(e.target.dataset.del); });
$("leadsBody").addEventListener("change", async (e) => {
  if (!e.target.dataset.status) return;
  try {
    await api("/leads/" + e.target.dataset.status + "/status", { method: "PUT", body: JSON.stringify({ status: e.target.value }) });
    toast("Status updated.", true);
    refresh();
  } catch (err) { toast(err.message, false); refresh(); }
});
$("refreshBtn").addEventListener("click", refresh);

/* ------------------------------------------------------- add a lead */
const FIELDS = { "f-id": "Lead ID", "f-name": "Patient name", "f-email": "Email", "f-phone": "Phone",
                 "f-service": "Service", "f-date": "Preferred date", "f-time": "Preferred time" };

$("addForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  document.querySelectorAll(".bad").forEach((x) => x.classList.remove("bad"));
  const v = (id) => $(id).value.trim();

  // 1) Empty required fields
  const missing = Object.keys(FIELDS).filter((id) => !v(id));
  if (missing.length) {
    missing.forEach((id) => $(id).classList.add("bad"));
    return toast("Please fill in: " + missing.map((id) => FIELDS[id]).join(", ") + ".", false);
  }
  // 2) Invalid numeric input
  if (!/^\d+$/.test(v("f-id")) || Number(v("f-id")) < 1 || Number(v("f-id")) > 2147483647) {
    $("f-id").classList.add("bad");
    return toast("Lead ID must be a positive whole number (digits only), for example 1006.", false);
  }

  const body = {
    leadId: v("f-id"), name: v("f-name"), email: v("f-email"), phone: v("f-phone"),
    service: v("f-service"), preferredDate: v("f-date"), preferredTime: v("f-time"),
    notes: v("f-notes"), source: v("f-source"), status: v("f-status")
  };
  try {
    // 3) Duplicate IDs, bad email/phone/date are checked by the Java server (409 / 400)
    const r = await api("/leads", { method: "POST", body: JSON.stringify(body) });
    toast(r.message, true);
    $("addForm").reset();
    refresh();
  } catch (err) {
    if (/Duplicate/i.test(err.message)) $("f-id").classList.add("bad");
    toast(err.message, false);
  }
});

/* ------------------------------------------------- search (BST) */
$("searchForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const raw = $("s-id").value.trim();
  const out = $("searchResult");
  if (!raw) { out.innerHTML = ""; return toast("Enter a Lead ID to search.", false); }
  if (!/^\d+$/.test(raw)) { out.innerHTML = ""; return toast("Lead ID must be a whole number (digits only).", false); }
  try {
    const r = await api("/leads/" + raw);
    const l = r.lead;
    out.innerHTML = `
      <div class="card detail">
        <div class="card-h"><h2>Lead ${l.leadId}: ${esc(l.name)}</h2>${pill(l.status)}</div>
        <p class="hint" style="margin-top:0">BST path from the root (${r.path.length} comparison${r.path.length > 1 ? "s" : ""}):</p>
        <div class="path">${r.path.map((id) => `<span class="chip ${id == l.leadId ? "" : "arrow"}">${id}</span>`).join("")}</div>
        <dl>
          <dt>Email</dt><dd>${esc(l.email)}</dd><dt>Phone</dt><dd>${esc(l.phone)}</dd>
          <dt>Service</dt><dd>${esc(l.service)}</dd>
          <dt>Preferred</dt><dd>${esc(l.preferredDate)} at ${esc(l.preferredTime)}</dd>
          <dt>Source</dt><dd>${esc(l.source)}</dd><dt>Notes</dt><dd>${esc(l.notes) || "-"}</dd>
          <dt>Submitted</dt><dd>${esc(l.dateSubmitted)}</dd>
        </dl>
        <button class="btn danger" data-del="${l.leadId}">Delete this lead</button>
      </div>`;
    toast("Lead " + l.leadId + " found in the BST.", true);
  } catch (err) {
    out.innerHTML = `<div class="card"><p class="empty">${esc(err.message)}</p></div>`;
    toast(err.message, false);
  }
});
$("searchResult").addEventListener("click", (e) => { if (e.target.dataset.del) deleteLead(e.target.dataset.del); });

/* ------------------------------------------------- BST traversal */
const EXPLAIN = {
  inorder: {
    rule: "Left → Root → Right",
    why: "The left subtree (smaller IDs) is visited first, then the node itself, then the right subtree (larger IDs). Because of the BST rule, this always produces the Lead IDs in ascending sorted order."
  },
  preorder: {
    rule: "Root → Left → Right",
    why: "Each node is visited before its children, so the root lead comes first and every parent appears before its descendants. Preorder is useful for copying the tree, because re-inserting the leads in this order rebuilds the same shape."
  },
  postorder: {
    rule: "Left → Right → Root",
    why: "Both children are visited before their parent, so the root lead comes last. Postorder is useful for deleting a whole tree safely, because children are always removed before their parents."
  }
};

document.querySelectorAll("[data-trav]").forEach((b) => b.addEventListener("click", async () => {
  const type = b.dataset.trav;
  try {
    const r = await api("/bst/" + type);
    const info = EXPLAIN[type];
    $("travResult").hidden = false;
    $("travName").textContent = type.charAt(0).toUpperCase() + type.slice(1) + " traversal (" + r.count + " leads)";
    $("travRule").textContent = info.rule;
    $("travWhy").textContent = info.why;
    $("travChips").innerHTML = r.leads.length
      ? r.leads.map((l, i) => `<div class="chip ${i < r.leads.length - 1 ? "arrow" : ""}">${l.leadId}<small>${esc(l.name)}</small></div>`).join("")
      : `<span class="hint">The tree is empty. Add a lead first.</span>`;
    loadTree();
  } catch (e) { toast(e.message, false); }
}));

function treeHtml(n) {
  const child = (label, c) => c
    ? `<li><small>${label}</small>${treeHtml(c)}</li>`
    : `<li><small>${label}</small><span class="none">empty</span></li>`;
  const kids = (n.left || n.right) ? `<ul>${child("L", n.left)}${child("R", n.right)}</ul>` : "";
  return `<span class="node">${n.id}</span><span class="nm">${esc(n.name)}</span>${kids}`;
}

async function loadTree() {
  try {
    const r = await api("/bst/tree");
    $("bstInfo").textContent = r.size + " node" + (r.size === 1 ? "" : "s") + " · height " + r.height;
    $("tree").innerHTML = r.tree ? `<ul style="margin-left:0;border:0;padding:0"><li>${treeHtml(r.tree)}</li></ul>` : `<p class="hint">The tree is empty.</p>`;
  } catch (e) { toast(e.message, false); }
}
$("treeBtn").addEventListener("click", loadTree);

/* ------------------------------------------------------------ start */
refresh();

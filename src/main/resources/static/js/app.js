/* ============================================================
   BrainRidge Bank — Online Banking Frontend
   Vanilla JS. Talks to the same REST API as before:
     POST /api/v1/accounts
     GET  /api/v1/accounts/{id}
     GET  /api/v1/accounts/{id}/transactions
     POST /api/v1/transfers
   ============================================================ */

const API = "/api/v1";
const STORAGE_KEY = "brainridge_accounts";
const ACTIVITY_KEY = "brainridge_activity";
const STATS_KEY = "brainridge_stats";

const $ = (id) => document.getElementById(id);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

/* ---------------- Formatting helpers ---------------- */
const money = (v) =>
    new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(Number(v) || 0);

const dateTime = (iso) =>
    new Date(iso).toLocaleString("en-US", {
        month: "short", day: "numeric", hour: "numeric", minute: "2-digit"
    });

const relTime = (iso) => {
    const diff = (Date.now() - new Date(iso).getTime()) / 1000;
    if (diff < 60) return "just now";
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return dateTime(iso);
};

const esc = (t) => {
    const d = document.createElement("div");
    d.textContent = t == null ? "" : String(t);
    return d.innerHTML;
};

const initials = (name) =>
    (name || "?").trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join("").toUpperCase();

/* Masked account number in a bank-card style, using the tail of the UUID. */
const maskedNumber = (id) => `•••• •••• ${(id || "").slice(-4).toUpperCase()}`;

/* ---------------- Storage (browser-side convenience only) ---------------- */
const readJSON = (key, fallback) => {
    try { return JSON.parse(localStorage.getItem(key)) ?? fallback; }
    catch { return fallback; }
};

const getAccounts = () => readJSON(STORAGE_KEY, []);
const setAccounts = (a) => localStorage.setItem(STORAGE_KEY, JSON.stringify(a));

const getStats = () => readJSON(STATS_KEY, { transfers: 0, volume: 0 });
const setStats = (s) => localStorage.setItem(STATS_KEY, JSON.stringify(s));
const bumpStats = (amount) => {
    const s = getStats();
    s.transfers += 1;
    s.volume += Number(amount) || 0;
    setStats(s);
};

const getActivity = () => readJSON(ACTIVITY_KEY, []);
const logActivity = (message, type = "info") => {
    const a = getActivity();
    a.unshift({ message, type, time: new Date().toISOString() });
    localStorage.setItem(ACTIVITY_KEY, JSON.stringify(a.slice(0, 25)));
    renderActivity();
};

const upsertAccount = (acc) => {
    const list = getAccounts().filter((a) => a.id !== acc.id);
    list.unshift({ id: acc.id, ownerName: acc.ownerName, balance: acc.balance, createdAt: acc.createdAt });
    setAccounts(list);
    renderAll();
};

const patchBalance = (id, balance) => {
    setAccounts(getAccounts().map((a) => (a.id === id ? { ...a, balance } : a)));
    renderAll();
};

/* ---------------- API layer (endpoints unchanged) ---------------- */
async function api(url, options = {}) {
    const res = await fetch(url, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    const text = await res.text();
    let data = null;
    if (text) { try { data = JSON.parse(text); } catch { data = { message: text }; } }
    if (!res.ok) throw new Error((data && data.message) || `Request failed (${res.status})`);
    return data;
}

const createAccount = (ownerName, initialBalance) =>
    api(`${API}/accounts`, { method: "POST", body: JSON.stringify({ ownerName, initialBalance }) });

const postTransfer = (fromAccountId, toAccountId, amount, description) =>
    api(`${API}/transfers`, { method: "POST", body: JSON.stringify({ fromAccountId, toAccountId, amount, description }) });

const getAccount = (id) => api(`${API}/accounts/${id}`);
const getHistory = (id) => api(`${API}/accounts/${id}/transactions?page=0&size=20`);

async function refreshBalance(id) {
    const acc = await getAccount(id);
    patchBalance(acc.id, acc.balance);
    return acc;
}

async function refreshAll() {
    for (const a of getAccounts()) {
        try { await refreshBalance(a.id); } catch { /* server may have reset */ }
    }
}

/* ---------------- Toasts ---------------- */
const TOAST_ICONS = {
    success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
};

function toast(title, message, type = "info") {
    const el = document.createElement("div");
    el.className = `toast ${type}`;
    el.innerHTML = `
        <span class="toast-ico">${TOAST_ICONS[type] || TOAST_ICONS.info}</span>
        <div class="toast-body">
            <div class="toast-title">${esc(title)}</div>
            ${message ? `<div class="toast-msg">${esc(message)}</div>` : ""}
        </div>
        <button class="toast-close" aria-label="Dismiss"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>`;
    const remove = () => { el.classList.add("hide"); setTimeout(() => el.remove(), 200); };
    el.querySelector(".toast-close").addEventListener("click", remove);
    $("toastStack").appendChild(el);
    setTimeout(remove, 4500);
}

/* ---------------- Inline results & validation ---------------- */
function showResult(id, type, title, lines = []) {
    const el = $(id);
    el.className = `result show ${type}`;
    el.innerHTML = `<div class="result-title">${title}</div>${lines.filter(Boolean).map((l) => `<p>${l}</p>`).join("")}`;
}
const hideResult = (id) => { const el = $(id); el.className = "result"; el.innerHTML = ""; };

const setInvalid = (fieldId, invalid) => $(fieldId).classList.toggle("invalid", invalid);

function setLoading(btn, loading, label) {
    if (loading) {
        btn.dataset.label = btn.textContent;
        btn.disabled = true;
        btn.innerHTML = `<span class="spinner"></span>${label || "Working…"}`;
    } else {
        btn.disabled = false;
        btn.textContent = btn.dataset.label || label || "Submit";
    }
}

/* ---------------- Empty state helper ---------------- */
const EMPTY_ICONS = {
    users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>',
    activity: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>',
    bars: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="20" x2="12" y2="10"/><line x1="18" y1="20" x2="18" y2="4"/><line x1="6" y1="20" x2="6" y2="16"/></svg>',
    history: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v5h5"/><path d="M3.05 13A9 9 0 1 0 6 5.3L3 8"/><path d="M12 7v5l4 2"/></svg>'
};

function emptyBlock(icon, title, text, goto, gotoLabel) {
    const btn = goto ? `<button class="btn btn--secondary btn--sm" data-goto="${goto}" type="button">${esc(gotoLabel)}</button>` : "";
    return `<div class="empty"><div class="empty-ico">${EMPTY_ICONS[icon] || EMPTY_ICONS.activity}</div><h4>${esc(title)}</h4><p>${esc(text)}</p>${btn}</div>`;
}

/* Wrap an empty state so it spans a full account-grid row inside a card. */
const spanEmpty = (html) => `<div class="card" style="grid-column:1 / -1;"><div class="card-body">${html}</div></div>`;

/* ---------------- Rendering: metrics + hero ---------------- */
function renderMetrics() {
    const accounts = getAccounts();
    const stats = getStats();
    const total = accounts.reduce((s, a) => s + Number(a.balance || 0), 0);

    $("metricBalance").textContent = money(total);
    $("metricBalanceSub").textContent = accounts.length === 0
        ? "No accounts yet"
        : `Across ${accounts.length} account${accounts.length === 1 ? "" : "s"}`;

    $("metricAccounts").textContent = accounts.length;
    $("metricTransfers").textContent = stats.transfers;
    $("metricVolume").textContent = money(stats.volume);

    $("navAccountsCount").textContent = accounts.length;
    $("accountsCountBadge").textContent = `${accounts.length} total`;
}

/* ---------------- Rendering: balance distribution ---------------- */
function renderDistribution() {
    const accounts = getAccounts();
    const body = $("distributionBody");
    if (accounts.length === 0) {
        body.innerHTML = emptyBlock("bars", "No data yet", "Balances appear here once you open accounts.");
        return;
    }
    const max = Math.max(...accounts.map((a) => Number(a.balance || 0)), 1);
    const rows = accounts.slice(0, 6).map((a) => {
        const pct = Math.max((Number(a.balance || 0) / max) * 100, 2);
        return `
            <div class="dist-row">
                <div class="dist-head"><span class="name">${esc(a.ownerName)}</span><span class="val numeric">${money(a.balance)}</span></div>
                <div class="dist-track"><div class="dist-fill" style="width:${pct}%"></div></div>
            </div>`;
    }).join("");
    body.innerHTML = `<div class="dist">${rows}</div>`;
}

/* ---------------- Rendering: account tiles ---------------- */
function accountCard(a) {
    return `
        <article class="acct-card">
            <div class="acct-card-top">
                <span class="acct-chip" aria-hidden="true"></span>
                <span class="badge badge--success">Active</span>
            </div>
            <div class="acct-holder">
                <span class="avatar">${esc(initials(a.ownerName))}</span>
                <div>
                    <div class="acct-holder-name">${esc(a.ownerName)}</div>
                    <div class="acct-number">${esc(maskedNumber(a.id))}</div>
                </div>
            </div>
            <div>
                <div class="acct-balance-label">Available balance</div>
                <div class="acct-balance numeric">${money(a.balance)}</div>
            </div>
            <div class="acct-actions">
                <button class="btn btn--secondary btn--sm" data-action="refresh" data-id="${a.id}" type="button">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M3 21v-5h5"/></svg>
                    Refresh
                </button>
                <button class="btn btn--secondary btn--sm" data-action="history" data-id="${a.id}" type="button">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v5h5"/><path d="M3.05 13A9 9 0 1 0 6 5.3L3 8"/><path d="M12 7v5l4 2"/></svg>
                    Activity
                </button>
            </div>
        </article>`;
}

function wireAccountActions(scope) {
    scope.querySelectorAll("[data-action]").forEach((btn) => {
        btn.addEventListener("click", () => onAccountRowAction(btn.dataset.action, btn.dataset.id, btn));
    });
}

function renderAccountsGrid() {
    const grid = $("accountsGrid");
    const accounts = getAccounts();
    if (accounts.length === 0) {
        grid.innerHTML = spanEmpty(emptyBlock("users", "No accounts yet", "Use the form on the left to open your first account."));
        return;
    }
    grid.innerHTML = accounts.map(accountCard).join("");
    wireAccountActions(grid);
}

function renderOverviewAccounts() {
    const grid = $("overviewAccountsGrid");
    const accounts = getAccounts();
    if (accounts.length === 0) {
        grid.innerHTML = spanEmpty(emptyBlock("users", "No accounts yet", "Open your first account to get started.", "accounts", "Open account"));
        wireGotoButtons();
        return;
    }
    grid.innerHTML = accounts.slice(0, 6).map(accountCard).join("");
    wireAccountActions(grid);
}

async function onAccountRowAction(action, id, btn) {
    if (action === "refresh") {
        const original = btn.innerHTML;
        btn.innerHTML = '<span class="spinner"></span>';
        btn.disabled = true;
        try {
            const acc = await refreshBalance(id);
            toast("Balance updated", `${acc.ownerName}: ${money(acc.balance)}`, "success");
        } catch (err) {
            toast("Could not refresh", err.message, "error");
        } finally {
            btn.disabled = false;
            btn.innerHTML = original;
        }
    } else if (action === "history") {
        setView("history");
        $("historyAccount").value = id;
        $("historyForm").requestSubmit();
    }
}

/* ---------------- Rendering: activity feed ---------------- */
const ACT_ICONS = {
    success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>',
    error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
};

function renderActivity() {
    const feed = $("activityFeed");
    const items = getActivity();
    if (items.length === 0) {
        feed.innerHTML = emptyBlock("activity", "No activity yet", "Open an account or run the demo to see events here.");
        return;
    }
    feed.innerHTML = items.map((it) => `
        <div class="feed-item">
            <span class="feed-ico ${esc(it.type)}">${ACT_ICONS[it.type] || ACT_ICONS.info}</span>
            <div class="feed-body">
                <div class="feed-msg">${it.message}</div>
                <div class="feed-time">${relTime(it.time)}</div>
            </div>
        </div>`).join("");
}

/* ---------------- Selects & transfer preview ---------------- */
function populateSelects() {
    const accounts = getAccounts();
    ["fromAccount", "toAccount", "historyAccount"].forEach((selId) => {
        const sel = $(selId);
        const current = sel.value;
        sel.innerHTML = `<option value="">Select account</option>` +
            accounts.map((a) => `<option value="${a.id}">${esc(a.ownerName)} — ${money(a.balance)}</option>`).join("");
        if (accounts.some((a) => a.id === current)) sel.value = current;
    });
    updateTransferReadiness();
    renderTransferPreview();
}

function updateTransferReadiness() {
    const count = getAccounts().length;
    const btn = $("transferBtn");
    const help = $("transferHelpText");
    const callout = $("transferHelp");
    if (count < 2) {
        btn.disabled = true;
        callout.classList.add("warn");
        help.textContent = count === 0
            ? "Create at least two accounts before sending money."
            : "You have 1 account. Open one more to send money between them.";
    } else {
        btn.disabled = false;
        callout.classList.remove("warn");
        help.textContent = "Choose a sender and receiver, enter an amount, then send.";
    }
}

function renderTransferPreview() {
    const box = $("transferPreview");
    const from = getAccounts().find((a) => a.id === $("fromAccount").value);
    const to = getAccounts().find((a) => a.id === $("toAccount").value);

    if (!from && !to) {
        box.innerHTML = `<div class="empty" style="padding:24px 8px;"><div class="empty-ico">${EMPTY_ICONS.history}</div><p>Pick a sender and receiver to preview the transfer.</p></div>`;
        return;
    }

    const party = (role, acc) => acc ? `
        <div class="xfer-party">
            <span class="avatar">${esc(initials(acc.ownerName))}</span>
            <div>
                <div class="xfer-party-role">${role}</div>
                <div class="xfer-party-name">${esc(acc.ownerName)}</div>
            </div>
            <div class="xfer-party-bal">
                <div class="xfer-party-role">Balance</div>
                <div class="amt numeric">${money(acc.balance)}</div>
            </div>
        </div>` : `
        <div class="xfer-party">
            <span class="avatar">?</span>
            <div><div class="xfer-party-role">${role}</div><div class="cell-muted">Not selected yet</div></div>
        </div>`;

    const arrow = `<div class="xfer-arrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/></svg></div>`;

    box.innerHTML = `<div class="xfer-preview">${party("From", from)}${arrow}${party("To", to)}</div>`;
}

/* ---------------- Master render ---------------- */
function renderAll() {
    renderMetrics();
    renderDistribution();
    renderAccountsGrid();
    renderOverviewAccounts();
    populateSelects();
}

/* ---------------- View routing ---------------- */
const VIEW_META = {
    overview: "Dashboard",
    accounts: "Accounts",
    transfer: "Send Money",
    history: "Transactions"
};

function setView(view) {
    $$(".nav-item").forEach((n) => n.classList.toggle("active", n.dataset.view === view));
    $$(".view").forEach((v) => v.classList.toggle("active", v.id === `view-${view}`));
    $("breadcrumbCurrent").textContent = VIEW_META[view] || "Dashboard";
    closeSidebar();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function wireGotoButtons() {
    $$("[data-goto]").forEach((b) => {
        if (b.dataset.wired) return;
        b.dataset.wired = "1";
        b.addEventListener("click", () => setView(b.dataset.goto));
    });
}

/* ---------------- Sidebar (mobile) ---------------- */
const openSidebar = () => { $("sidebar").classList.add("open"); $("mobileOverlay").classList.add("show"); };
const closeSidebar = () => { $("sidebar").classList.remove("open"); $("mobileOverlay").classList.remove("show"); };

/* ---------------- Server status ---------------- */
async function checkServer() {
    const el = $("serverStatus");
    const txt = $("serverStatusText");
    try {
        await fetch("/", { method: "HEAD" });
        el.className = "server-status online";
        txt.textContent = "Server online";
    } catch {
        el.className = "server-status offline";
        txt.textContent = "Server offline";
    }
}

/* ---------------- Create account ---------------- */
$("createForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideResult("createResult");
    const ownerName = $("ownerName").value.trim();
    const balanceRaw = $("initialBalance").value;
    const initialBalance = parseFloat(balanceRaw);

    const nameBad = ownerName.length === 0;
    const balBad = balanceRaw === "" || isNaN(initialBalance) || initialBalance < 0;
    setInvalid("fieldOwner", nameBad);
    setInvalid("fieldBalance", balBad);
    if (nameBad || balBad) return;

    const btn = $("createBtn");
    setLoading(btn, true, "Opening…");
    try {
        const acc = await createAccount(ownerName, initialBalance);
        upsertAccount(acc);
        logActivity(`Opened account for <b>${esc(acc.ownerName)}</b> with ${money(acc.balance)}.`, "success");
        toast("Account opened", `${acc.ownerName} · ${money(acc.balance)}`, "success");
        const count = getAccounts().length;
        showResult("createResult", "success", "Account opened",
            [`<b>${esc(acc.ownerName)}</b> now has <b>${money(acc.balance)}</b>.`,
             count < 2 ? "Open one more account to enable transfers." : "You can now send money from the Send Money tab."]);
        $("createForm").reset();
        $("initialBalance").value = "1000.00";
    } catch (err) {
        toast("Could not open account", err.message, "error");
        showResult("createResult", "error", "Could not open account", [esc(err.message)]);
    } finally {
        setLoading(btn, false, "Open account");
    }
});
["ownerName", "initialBalance"].forEach((id) =>
    $(id).addEventListener("input", () => setInvalid(id === "ownerName" ? "fieldOwner" : "fieldBalance", false)));

/* ---------------- Transfer ---------------- */
$("transferForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideResult("transferResult");
    const fromAccountId = $("fromAccount").value;
    const toAccountId = $("toAccount").value;
    const amountRaw = $("transferAmount").value;
    const amount = parseFloat(amountRaw);
    const description = $("transferNote").value.trim() || null;

    const fromBad = !fromAccountId;
    const toBad = !toAccountId;
    const amtBad = amountRaw === "" || isNaN(amount) || amount <= 0;
    setInvalid("fieldFrom", fromBad);
    setInvalid("fieldTo", toBad);
    setInvalid("fieldAmount", amtBad);
    if (fromBad || toBad || amtBad) return;

    if (fromAccountId === toAccountId) {
        setInvalid("fieldTo", true);
        showResult("transferResult", "error", "Pick two different accounts", ["You cannot transfer to the same account."]);
        return;
    }

    const btn = $("transferBtn");
    setLoading(btn, true, "Sending…");
    try {
        const t = await postTransfer(fromAccountId, toAccountId, amount, description);
        bumpStats(t.amount);
        await refreshBalance(fromAccountId);
        await refreshBalance(toAccountId);
        const fromName = getAccounts().find((a) => a.id === fromAccountId)?.ownerName || "Sender";
        const toName = getAccounts().find((a) => a.id === toAccountId)?.ownerName || "Receiver";
        logActivity(`<b>${esc(fromName)}</b> sent <b>${money(t.amount)}</b> to <b>${esc(toName)}</b>.`, "success");
        toast("Transfer sent", `${money(t.amount)} · ${fromName} → ${toName}`, "success");
        showResult("transferResult", "success", "Transfer complete",
            [`<b>${money(t.amount)}</b> moved from ${esc(fromName)} to ${esc(toName)}.`,
             description ? `Note: ${esc(description)}` : "",
             "View it anytime under Transactions."]);
        $("transferAmount").value = "";
        $("transferNote").value = "";
        renderTransferPreview();
    } catch (err) {
        toast("Transfer failed", err.message, "error");
        showResult("transferResult", "error", "Transfer failed", [esc(err.message)]);
    } finally {
        setLoading(btn, false, "Send transfer");
    }
});

$("transferReset").addEventListener("click", () => {
    hideResult("transferResult");
    ["fieldFrom", "fieldTo", "fieldAmount"].forEach((f) => setInvalid(f, false));
    setTimeout(renderTransferPreview, 0);
});
["fromAccount", "toAccount"].forEach((id) =>
    $(id).addEventListener("change", () => { renderTransferPreview(); setInvalid(id === "fromAccount" ? "fieldFrom" : "fieldTo", false); }));
$("transferAmount").addEventListener("input", () => setInvalid("fieldAmount", false));

/* ---------------- Transactions (history) ---------------- */
$("historyForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const accountId = $("historyAccount").value;
    const container = $("historyResult");
    if (!accountId) {
        container.innerHTML = emptyBlock("history", "No account selected", "Choose an account above to load its transactions.");
        return;
    }
    const name = getAccounts().find((a) => a.id === accountId)?.ownerName || "Account";
    const btn = $("historyBtn");
    setLoading(btn, true, "Loading…");
    container.innerHTML = skeletonTable();
    try {
        const page = await getHistory(accountId);
        if (!page.content || page.totalElements === 0) {
            container.innerHTML = emptyBlock("history", "No transactions", `${name} has not sent or received any transfers yet.`);
            return;
        }
        const rows = page.content.map((tx) => {
            // From this account's point of view: money out = debit, money in = credit.
            const sent = tx.fromAccountId === accountId;
            const otherId = sent ? tx.toAccountId : tx.fromAccountId;
            const other = getAccounts().find((a) => a.id === otherId)?.ownerName || (otherId.slice(0, 8) + "…");
            const badge = sent ? '<span class="badge badge--warning">Sent</span>' : '<span class="badge badge--success">Received</span>';
            const amtClass = sent ? "amt-debit" : "amt-credit";
            const sign = sent ? "−" : "+";
            return `
                <tr>
                    <td>${badge}</td>
                    <td>${personCell(other)}</td>
                    <td class="cell-muted">${esc(tx.description || "—")}</td>
                    <td class="cell-muted">${dateTime(tx.timestamp)}</td>
                    <td class="col-num ${amtClass}">${sign}${money(tx.amount)}</td>
                </tr>`;
        }).join("");
        container.innerHTML = `
            <div class="table-wrap">
                <table class="data">
                    <thead><tr><th>Type</th><th>Counterparty</th><th>Note</th><th>Date</th><th class="col-num">Amount</th></tr></thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>`;
        logActivity(`Loaded ${page.totalElements} transaction(s) for <b>${esc(name)}</b>.`, "info");
    } catch (err) {
        container.innerHTML = emptyBlock("history", "Could not load transactions", err.message);
        toast("Could not load transactions", err.message, "error");
    } finally {
        setLoading(btn, false, "Load transactions");
    }
});

function personCell(name) {
    return `<div class="person"><span class="avatar">${esc(initials(name))}</span><span class="person-name">${esc(name)}</span></div>`;
}

function skeletonTable() {
    const row = `<tr class="skel-row"><td><div class="skel skel-line" style="width:70px"></div></td><td><div class="skel skel-line" style="width:130px"></div></td><td><div class="skel skel-line" style="width:90px"></div></td><td><div class="skel skel-line" style="width:110px"></div></td><td class="col-num"><div class="skel skel-line" style="width:70px;margin-left:auto"></div></td></tr>`;
    return `<div class="table-wrap"><table class="data"><thead><tr><th>Type</th><th>Counterparty</th><th>Note</th><th>Date</th><th class="col-num">Amount</th></tr></thead><tbody>${row.repeat(4)}</tbody></table></div>`;
}

/* ---------------- Clear actions ---------------- */
$("clearAccountsBtn").addEventListener("click", () => {
    if (!confirm("Clear the saved accounts list from this browser? Server data is not affected.")) return;
    setAccounts([]);
    renderAll();
    toast("List cleared", "Saved accounts removed from this browser.", "info");
});
$("clearActivityBtn").addEventListener("click", () => {
    localStorage.removeItem(ACTIVITY_KEY);
    renderActivity();
});

/* ---------------- Quick demo ---------------- */
$("runDemoBtn").addEventListener("click", async (e) => {
    const btn = e.currentTarget;
    setLoading(btn, true, "Running…");
    try {
        const alice = await createAccount("Alice Johnson", 1000);
        upsertAccount(alice);
        logActivity(`Opened account for <b>Alice Johnson</b> with ${money(alice.balance)}.`, "success");

        const bob = await createAccount("Bob Martinez", 500);
        upsertAccount(bob);
        logActivity(`Opened account for <b>Bob Martinez</b> with ${money(bob.balance)}.`, "success");

        const t = await postTransfer(alice.id, bob.id, 150, "Rent payment");
        bumpStats(t.amount);
        await refreshBalance(alice.id);
        await refreshBalance(bob.id);
        logActivity(`<b>Alice Johnson</b> sent <b>${money(150)}</b> to <b>Bob Martinez</b>.`, "success");

        $("historyAccount").value = alice.id;
        toast("Demo complete", "Opened two accounts and sent $150.", "success");
        setView("overview");
    } catch (err) {
        toast("Demo failed", err.message, "error");
    } finally {
        setLoading(btn, false, "Run demo");
    }
});

/* ---------------- Guided tour ---------------- */
const tourSteps = [
    { view: "overview", title: "Welcome to BrainRidge Bank", text: "This is your online banking dashboard. It shows your total balance, your accounts, and recent activity. Here is a quick tour." },
    { view: "accounts", title: "Your accounts", text: "Open new accounts on the left and see each one as a card on the right, with its balance and a masked account number. You need two accounts to make a transfer." },
    { view: "transfer", title: "Send money", text: "Choose a sender and receiver, enter an amount, and send. The preview shows both parties, and balances update instantly everywhere." },
    { view: "history", title: "Transactions", text: "Pick any account to see the transfers it sent or received. Money out shows in red, money in shows in green." },
    { view: "overview", title: "You're all set", text: "Back on the dashboard you'll see live totals and activity. Try the Run demo button to watch it work end to end." }
];
let tourIdx = 0;

function renderTourDots() {
    $("tourDots").innerHTML = tourSteps.map((_, i) => `<span class="tour-dot ${i === tourIdx ? "active" : ""}"></span>`).join("");
}
function clearHighlight() { $$(".highlight").forEach((el) => el.classList.remove("highlight")); }
function showTour(i) {
    tourIdx = i;
    const step = tourSteps[i];
    setView(step.view);
    $("tourStep").textContent = `Step ${i + 1} of ${tourSteps.length}`;
    $("tourTitle").textContent = step.title;
    $("tourText").textContent = step.text;
    $("tourNext").textContent = i === tourSteps.length - 1 ? "Finish" : "Next";
    renderTourDots();
    clearHighlight();
    const target = document.querySelector(`[data-tour="${step.view}"]`);
    if (target) target.classList.add("highlight");
    $("tourOverlay").classList.add("show");
}
function endTour() { $("tourOverlay").classList.remove("show"); clearHighlight(); }

$("startTourBtn").addEventListener("click", () => showTour(0));
$("tourNext").addEventListener("click", () => (tourIdx >= tourSteps.length - 1 ? endTour() : showTour(tourIdx + 1)));
$("tourSkip").addEventListener("click", endTour);
$("tourOverlay").addEventListener("click", (e) => { if (e.target === $("tourOverlay")) endTour(); });

/* ---------------- Global wiring ---------------- */
$$(".nav-item").forEach((n) => n.addEventListener("click", () => setView(n.dataset.view)));
$("menuToggle").addEventListener("click", openSidebar);
$("mobileOverlay").addEventListener("click", closeSidebar);
document.addEventListener("keydown", (e) => { if (e.key === "Escape") { endTour(); closeSidebar(); } });

/* ---------------- Init ---------------- */
wireGotoButtons();
renderAll();
renderActivity();
checkServer();
refreshAll().then(() => { renderAll(); wireGotoButtons(); });

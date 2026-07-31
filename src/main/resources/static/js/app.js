const API = '/api/v1';
const STORAGE_KEY = 'brainridge_accounts';
const ACTIVITY_KEY = 'brainridge_activity';
const PROGRESS_KEY = 'brainridge_progress';

const $ = (id) => document.getElementById(id);

const tourSteps = [
    {
        target: 'guideSection',
        title: 'Welcome to BrainRidge Banking',
        text: "This is a practice banking app. We'll show you each section so you know exactly what to click."
    },
    {
        target: 'createSection',
        title: 'Step 1: Create accounts',
        text: "Start here. Enter a person's name and how much money they start with, then click Create account. Make at least two accounts to send money between them."
    },
    {
        target: 'transferSection',
        title: 'Step 2: Send money',
        text: 'Choose who pays, who receives, and how much. When you click Send transfer, money moves instantly from one account to the other.'
    },
    {
        target: 'lookupSection',
        title: 'Step 3: Check balance',
        text: 'Pick any account and click Check balance to see how much money they have right now.'
    },
    {
        target: 'historySection',
        title: 'Step 4: View history',
        text: 'See every payment an account sent or received. Great for confirming a transfer worked.'
    },
    {
        target: 'savedAccountsSection',
        title: "You're all set!",
        text: 'Your accounts appear on the right so you never need to remember long IDs. Try the Quick Demo button anytime, or create your own accounts below.'
    }
];

let tourIndex = 0;

function formatMoney(value) {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatDate(iso) {
    return new Date(iso).toLocaleString();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function getSavedAccounts() {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    } catch {
        return [];
    }
}

function saveAccounts(accounts) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(accounts));
}

function getProgress() {
    try {
        return JSON.parse(localStorage.getItem(PROGRESS_KEY) || '{}');
    } catch {
        return {};
    }
}

function saveProgress(progress) {
    localStorage.setItem(PROGRESS_KEY, JSON.stringify(progress));
}

function markProgress(key) {
    const progress = getProgress();
    progress[key] = true;
    saveProgress(progress);
    updateProgressUI();
}

function getActivity() {
    try {
        return JSON.parse(localStorage.getItem(ACTIVITY_KEY) || '[]');
    } catch {
        return [];
    }
}

function logActivity(message, type = 'info') {
    const activity = getActivity();
    activity.unshift({ message, type, time: new Date().toISOString() });
    localStorage.setItem(ACTIVITY_KEY, JSON.stringify(activity.slice(0, 20)));
    renderActivityLog();
}

function renderActivityLog() {
    const log = $('activityLog');
    const activity = getActivity();

    if (activity.length === 0) {
        log.className = 'activity-log empty-state';
        log.innerHTML = '<p>Nothing yet. Create an account or run the demo to see activity here.</p>';
        return;
    }

    log.className = 'activity-log';
    log.innerHTML = activity.map((item) => `
        <div class="activity-item ${escapeHtml(item.type)}">
            <time>${formatDate(item.time)}</time>
            <div>${item.message}</div>
        </div>
    `).join('');
}

function updateProgressUI() {
    const accounts = getSavedAccounts();
    const progress = getProgress();

    const steps = {
        create: accounts.length >= 1 || progress.create,
        second: accounts.length >= 2 || progress.second,
        transfer: progress.transfer,
        history: progress.history
    };

    document.querySelectorAll('.progress-step').forEach((el) => {
        const key = el.dataset.step;
        el.classList.remove('done', 'active');
        if (steps[key]) {
            el.classList.add('done');
        }
    });

    const next = $('nextStepText');
    const transferBtn = $('transferBtn');
    const transferHelp = $('transferHelp');

    if (accounts.length === 0) {
        next.textContent = 'Start by creating your first account in Step 1.';
        document.querySelector('[data-step="create"]')?.classList.add('active');
        transferBtn.disabled = true;
        transferHelp.textContent = 'Create at least two accounts first — then pick them from the dropdowns below.';
    } else if (accounts.length === 1) {
        next.textContent = 'Great! Now create a second account so you can send money between them.';
        document.querySelector('[data-step="second"]')?.classList.add('active');
        transferBtn.disabled = true;
        transferHelp.textContent = 'You have 1 account. Create one more person (e.g. Bob) before sending money.';
    } else if (!steps.transfer) {
        next.textContent = 'You have two accounts. Try sending money in Step 2 — e.g. $150 from Alice to Bob.';
        document.querySelector('[data-step="transfer"]')?.classList.add('active');
        transferBtn.disabled = false;
        transferHelp.innerHTML = '<strong>Ready!</strong> Pick a sender, a receiver, and an amount, then click Send transfer.';
    } else if (!steps.history) {
        next.textContent = 'Transfer done! Now load transaction history in Step 4 to see the record.';
        document.querySelector('[data-step="history"]')?.classList.add('active');
        transferBtn.disabled = false;
    } else {
        next.textContent = "Nice work — you've completed the full walkthrough! Feel free to experiment more.";
        transferBtn.disabled = false;
    }
}

function addSavedAccount(account) {
    const accounts = getSavedAccounts().filter((a) => a.id !== account.id);
    accounts.unshift({
        id: account.id,
        ownerName: account.ownerName,
        balance: account.balance
    });
    saveAccounts(accounts);
    renderSavedAccounts();
    populateAccountSelects();
    updateProgressUI();
}

function updateSavedBalance(id, balance) {
    const accounts = getSavedAccounts().map((a) =>
        a.id === id ? { ...a, balance } : a
    );
    saveAccounts(accounts);
    renderSavedAccounts();
}

function showResult(elementId, type, title, details = []) {
    const el = $(elementId);
    const detailHtml = details.map((d) => `<p class="result-detail">${d}</p>`).join('');
    el.innerHTML = `<div class="result-title">${title}</div>${detailHtml}`;
    el.className = `result ${type}`;
}

function hideResult(elementId) {
    const el = $(elementId);
    el.className = 'result hidden';
    el.innerHTML = '';
}

async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options
    });

    let data = null;
    const text = await response.text();
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = { message: text };
        }
    }

    if (!response.ok) {
        throw new Error(data?.message || `Request failed (${response.status})`);
    }

    return data;
}

function populateAccountSelects() {
    const accounts = getSavedAccounts();
    ['fromAccount', 'toAccount', 'lookupAccount', 'historyAccount'].forEach((selectId) => {
        const select = $(selectId);
        const current = select.value;
        select.innerHTML = '<option value="">— pick an account —</option>';

        accounts.forEach((account) => {
            const option = document.createElement('option');
            option.value = account.id;
            option.textContent = `${account.ownerName} (${formatMoney(account.balance)})`;
            select.appendChild(option);
        });

        if (accounts.some((a) => a.id === current)) {
            select.value = current;
        }
    });
}

function renderSavedAccounts() {
    const list = $('savedAccountsList');
    const accounts = getSavedAccounts();

    if (accounts.length === 0) {
        list.className = 'account-list empty-state';
        list.innerHTML = '<p>No accounts yet.</p>';
        return;
    }

    list.className = 'account-list';
    list.innerHTML = accounts.map((account) => `
        <div class="account-chip">
            <div>
                <strong>${escapeHtml(account.ownerName)}</strong>
                <small>${account.id}</small>
            </div>
            <div class="balance">${formatMoney(account.balance)}</div>
        </div>
    `).join('');
}

async function refreshAccountBalance(accountId) {
    const account = await apiRequest(`${API}/accounts/${accountId}`);
    updateSavedBalance(account.id, account.balance);
    return account;
}

async function refreshAllBalances() {
    for (const account of getSavedAccounts()) {
        try {
            await refreshAccountBalance(account.id);
        } catch {
            // server may have restarted
        }
    }
}

async function checkServer() {
    const pill = $('serverStatus');
    try {
        await fetch('/', { method: 'HEAD' });
        pill.textContent = 'Server online';
        pill.className = 'status-pill online';
    } catch {
        pill.textContent = 'Server offline — run run.bat first';
        pill.className = 'status-pill offline';
    }
}

function clearHighlights() {
    document.querySelectorAll('.tour-target').forEach((el) => el.classList.remove('highlight'));
}

function showTourStep(index) {
    tourIndex = index;
    const step = tourSteps[index];
    clearHighlights();

    $('tourStepLabel').textContent = `Step ${index + 1} of ${tourSteps.length}`;
    $('tourTitle').textContent = step.title;
    $('tourText').textContent = step.text;
    $('tourNextBtn').textContent = index === tourSteps.length - 1 ? 'Finish' : 'Next';

    const target = $(step.target);
    if (target) {
        target.classList.add('highlight');
        target.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    $('tourOverlay').classList.remove('hidden');
}

function endTour() {
    $('tourOverlay').classList.add('hidden');
    clearHighlights();
}

async function runDemo() {
    logActivity('Running quick demo… creating Alice and Bob, then transferring $150.', 'info');

    try {
        const alice = await apiRequest(`${API}/accounts`, {
            method: 'POST',
            body: JSON.stringify({ ownerName: 'Alice', initialBalance: 1000 })
        });
        addSavedAccount(alice);
        markProgress('create');
        logActivity(`Created Alice's account with ${formatMoney(alice.balance)}.`, 'success');

        const bob = await apiRequest(`${API}/accounts`, {
            method: 'POST',
            body: JSON.stringify({ ownerName: 'Bob', initialBalance: 500 })
        });
        addSavedAccount(bob);
        markProgress('second');
        logActivity(`Created Bob's account with ${formatMoney(bob.balance)}.`, 'success');

        const transfer = await apiRequest(`${API}/transfers`, {
            method: 'POST',
            body: JSON.stringify({
                fromAccountId: alice.id,
                toAccountId: bob.id,
                amount: 150,
                description: 'Rent payment'
            })
        });

        await refreshAccountBalance(alice.id);
        await refreshAccountBalance(bob.id);
        populateAccountSelects();
        markProgress('transfer');

        logActivity(`Alice sent ${formatMoney(transfer.amount)} to Bob for rent. Alice now has $850, Bob has $650.`, 'success');

        $('fromAccount').value = alice.id;
        $('toAccount').value = bob.id;
        $('historyAccount').value = alice.id;

        showResult('transferResult', 'success', 'Demo transfer complete!', [
            `Alice sent <strong>${formatMoney(150)}</strong> to Bob.`,
            `Alice's new balance: <strong>${formatMoney(850)}</strong>`,
            `Bob's new balance: <strong>${formatMoney(650)}</strong>`,
            'Try loading Alice\'s history in Step 4 to see the record.'
        ]);

        document.getElementById('historyForm').requestSubmit();
        markProgress('history');
        updateProgressUI();
    } catch (err) {
        logActivity(`Demo failed: ${err.message}`, 'error');
        showResult('createResult', 'error', 'Demo could not run', [
            err.message,
            'Make sure the server is running (double-click run.bat).'
        ]);
    }
}

$('createForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideResult('createResult');

    const ownerName = $('ownerName').value.trim();
    const initialBalance = parseFloat($('initialBalance').value);

    try {
        showResult('createResult', 'info', 'Creating account…');
        const account = await apiRequest(`${API}/accounts`, {
            method: 'POST',
            body: JSON.stringify({ ownerName, initialBalance })
        });

        addSavedAccount(account);
        const count = getSavedAccounts().length;
        if (count === 1) markProgress('create');
        if (count >= 2) markProgress('second');

        logActivity(`Created ${ownerName}'s account with ${formatMoney(account.balance)}.`, 'success');

        showResult('createResult', 'success', 'Account created!', [
            `<strong>${escapeHtml(account.ownerName)}</strong> now has <strong>${formatMoney(account.balance)}</strong>.`,
            count < 2
                ? 'Create one more account so you can send money between them.'
                : 'You can now send money in Step 2!'
        ]);

        $('createForm').reset();
        $('initialBalance').value = '1000.00';
        updateProgressUI();
    } catch (err) {
        logActivity(`Could not create account: ${err.message}`, 'error');
        showResult('createResult', 'error', 'Could not create account', [err.message]);
    }
});

$('transferForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideResult('transferResult');

    const fromAccountId = $('fromAccount').value;
    const toAccountId = $('toAccount').value;
    const amount = parseFloat($('transferAmount').value);
    const description = $('transferNote').value.trim() || null;

    if (fromAccountId === toAccountId) {
        showResult('transferResult', 'error', 'Pick two different accounts', [
            'You cannot send money to the same account.'
        ]);
        return;
    }

    try {
        showResult('transferResult', 'info', 'Sending transfer…');
        const transfer = await apiRequest(`${API}/transfers`, {
            method: 'POST',
            body: JSON.stringify({ fromAccountId, toAccountId, amount, description })
        });

        await refreshAccountBalance(fromAccountId);
        await refreshAccountBalance(toAccountId);
        populateAccountSelects();
        markProgress('transfer');

        const fromName = getSavedAccounts().find((a) => a.id === fromAccountId)?.ownerName || 'Sender';
        const toName = getSavedAccounts().find((a) => a.id === toAccountId)?.ownerName || 'Receiver';

        logActivity(`${fromName} sent ${formatMoney(transfer.amount)} to ${toName}.`, 'success');

        showResult('transferResult', 'success', 'Transfer sent!', [
            `<strong>${formatMoney(transfer.amount)}</strong> moved from ${escapeHtml(fromName)} to ${escapeHtml(toName)}.`,
            description ? `Note: ${escapeHtml(description)}` : '',
            'Check balances in Step 3 or history in Step 4.'
        ].filter(Boolean));

        $('transferAmount').value = '';
        $('transferNote').value = '';
        updateProgressUI();
    } catch (err) {
        logActivity(`Transfer failed: ${err.message}`, 'error');
        showResult('transferResult', 'error', 'Transfer failed', [err.message]);
    }
});

$('lookupForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideResult('lookupResult');

    const accountId = $('lookupAccount').value;
    if (!accountId) return;

    try {
        showResult('lookupResult', 'info', 'Looking up account…');
        const account = await refreshAccountBalance(accountId);
        populateAccountSelects();

        logActivity(`${account.ownerName}'s balance is ${formatMoney(account.balance)}.`, 'success');

        showResult('lookupResult', 'success', `${account.ownerName}'s account`, [
            `Current balance: <strong>${formatMoney(account.balance)}</strong>`,
            `Account opened: ${formatDate(account.createdAt)}`
        ]);
    } catch (err) {
        showResult('lookupResult', 'error', 'Account not found', [
            err.message,
            'If you restarted the server, create new accounts.'
        ]);
    }
});

$('historyForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideResult('historyResult');

    const accountId = $('historyAccount').value;
    if (!accountId) return;

    const accountName = getSavedAccounts().find((a) => a.id === accountId)?.ownerName || 'Account';

    try {
        showResult('historyResult', 'info', 'Loading transactions…');
        const page = await apiRequest(`${API}/accounts/${accountId}/transactions?page=0&size=20`);
        markProgress('history');

        if (page.totalElements === 0) {
            showResult('historyResult', 'info', 'No transactions yet', [
                `${accountName} hasn't sent or received any transfers yet.`
            ]);
            return;
        }

        logActivity(`Loaded ${page.totalElements} transaction(s) for ${accountName}.`, 'success');

        const rows = page.content.map((tx) => {
            const isSent = tx.fromAccountId === accountId;
            const label = isSent ? 'Sent' : 'Received';
            const direction = isSent ? 'sent' : 'received';
            const otherId = isSent ? tx.toAccountId : tx.fromAccountId;
            const otherName = getSavedAccounts().find((a) => a.id === otherId)?.ownerName || otherId.slice(0, 8) + '…';

            return `
                <tr>
                    <td><span class="tag ${direction}">${label}</span></td>
                    <td>${escapeHtml(otherName)}</td>
                    <td>${formatMoney(tx.amount)}</td>
                    <td>${escapeHtml(tx.description || '—')}</td>
                    <td>${formatDate(tx.timestamp)}</td>
                </tr>
            `;
        }).join('');

        const el = $('historyResult');
        el.className = 'result info';
        el.innerHTML = `
            <div class="result-title">${escapeHtml(accountName)} — ${page.totalElements} transaction(s)</div>
            <div class="history-table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Type</th>
                            <th>Other person</th>
                            <th>Amount</th>
                            <th>Note</th>
                            <th>When</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;
        updateProgressUI();
    } catch (err) {
        showResult('historyResult', 'error', 'Could not load history', [err.message]);
    }
});

$('clearAccountsBtn').addEventListener('click', () => {
    if (confirm('Clear saved accounts from this browser? (Server data is not deleted)')) {
        saveAccounts([]);
        renderSavedAccounts();
        populateAccountSelects();
        updateProgressUI();
    }
});

$('startTourBtn').addEventListener('click', () => showTourStep(0));
$('tourNextBtn').addEventListener('click', () => {
    if (tourIndex >= tourSteps.length - 1) {
        endTour();
    } else {
        showTourStep(tourIndex + 1);
    }
});
$('tourSkipBtn').addEventListener('click', endTour);
$('runDemoBtn').addEventListener('click', runDemo);
$('scrollGuideBtn').addEventListener('click', () => {
    $('guideSection').scrollIntoView({ behavior: 'smooth' });
});

renderSavedAccounts();
populateAccountSelects();
renderActivityLog();
updateProgressUI();
checkServer();
refreshAllBalances().then(() => {
    populateAccountSelects();
    updateProgressUI();
});

const API = '/api/v1';
const STORAGE_KEY = 'brainridge_accounts';

const $ = (id) => document.getElementById(id);

function formatMoney(value) {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatDate(iso) {
    return new Date(iso).toLocaleString();
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
    const selects = ['fromAccount', 'toAccount', 'lookupAccount', 'historyAccount'];

    selects.forEach((selectId) => {
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
        list.innerHTML = '<p>No accounts yet. Create one above to get started.</p>';
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

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function refreshAccountBalance(accountId) {
    const account = await apiRequest(`${API}/accounts/${accountId}`);
    updateSavedBalance(account.id, account.balance);
    return account;
}

async function refreshAllBalances() {
    const accounts = getSavedAccounts();
    for (const account of accounts) {
        try {
            await refreshAccountBalance(account.id);
        } catch {
            // account may have been cleared after server restart
        }
    }
}

async function checkServer() {
    const pill = $('serverStatus');
    try {
        await fetch('/swagger-ui/index.html', { method: 'HEAD' });
        pill.textContent = 'Server online';
        pill.className = 'status-pill online';
    } catch {
        pill.textContent = 'Server offline — run the app first';
        pill.className = 'status-pill offline';
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

        showResult('createResult', 'success', 'Account created!', [
            `Name: <strong>${escapeHtml(account.ownerName)}</strong>`,
            `Balance: <strong>${formatMoney(account.balance)}</strong>`,
            `Account ID: <code>${account.id}</code> (saved below — you won't need to copy it)`
        ]);

        $('createForm').reset();
        $('initialBalance').value = '1000.00';
    } catch (err) {
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

        const fromName = getSavedAccounts().find((a) => a.id === fromAccountId)?.ownerName || 'Sender';
        const toName = getSavedAccounts().find((a) => a.id === toAccountId)?.ownerName || 'Receiver';

        showResult('transferResult', 'success', 'Transfer sent!', [
            `<strong>${formatMoney(transfer.amount)}</strong> moved from ${escapeHtml(fromName)} to ${escapeHtml(toName)}.`,
            description ? `Note: ${escapeHtml(description)}` : '',
            `Time: ${formatDate(transfer.timestamp)}`
        ].filter(Boolean));

        $('transferAmount').value = '';
        $('transferNote').value = '';
    } catch (err) {
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

        showResult('lookupResult', 'success', `${account.ownerName}'s account`, [
            `Current balance: <strong>${formatMoney(account.balance)}</strong>`,
            `Opened: ${formatDate(account.createdAt)}`
        ]);
    } catch (err) {
        showResult('lookupResult', 'error', 'Account not found', [
            err.message,
            'If you restarted the server, old accounts are gone — create new ones.'
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

        if (page.totalElements === 0) {
            showResult('historyResult', 'info', 'No transactions yet', [
                `${accountName} hasn't sent or received any transfers.`
            ]);
            return;
        }

        const rows = page.content.map((tx) => {
            const isSent = tx.fromAccountId === accountId;
            const direction = isSent ? 'sent' : 'received';
            const label = isSent ? 'Sent' : 'Received';
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
                            <th>Other party</th>
                            <th>Amount</th>
                            <th>Note</th>
                            <th>When</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showResult('historyResult', 'error', 'Could not load history', [err.message]);
    }
});

$('clearAccountsBtn').addEventListener('click', () => {
    if (confirm('Clear the saved account list from this browser? (Does not delete server data)')) {
        saveAccounts([]);
        renderSavedAccounts();
        populateAccountSelects();
    }
});

renderSavedAccounts();
populateAccountSelects();
checkServer();
refreshAllBalances().then(populateAccountSelects);

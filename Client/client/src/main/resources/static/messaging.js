let recipients = [];

async function loadRecipients() {
    const loading = document.getElementById('loadingMessage');
    const errorDiv = document.getElementById('errorMessage');
    const main = document.getElementById('mainContent');

    try {
        const response = await fetch('/api/admin/messaging/recipients', {
            method: 'GET',
            credentials: 'include'
        });

        loading.style.display = 'none';

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                alert('Unauthorized. Please log in as root admin.');
                window.location.href = '/admin-login.html';
                return;
            }
            throw new Error('Failed to load recipients');
        }

        recipients = await response.json();
        main.style.display = 'block';
        renderRecipientList();
    } catch (error) {
        console.error('Error loading recipients:', error);
        loading.style.display = 'none';
        errorDiv.style.display = 'block';
    }
}

function renderRecipientList() {
    const list = document.getElementById('recipientList');
    const admins = recipients.filter(r => r.role === 'ADMIN');
    const patients = recipients.filter(r => r.role === 'PATIENT');

    let html = '';

    if (admins.length > 0) {
        html += '<div class="role-group-label">Admins</div>';
        html += admins.map(r => recipientRow(r)).join('');
    }

    if (patients.length > 0) {
        html += '<div class="role-group-label">Patients</div>';
        html += patients.map(r => recipientRow(r)).join('');
    }

    if (recipients.length === 0) {
        html = '<div class="center-message">No recipients found.</div>';
    }

    list.innerHTML = html;
}

function recipientRow(r) {
    const disabled = !r.hasPhone;
    const disabledClass = disabled ? ' disabled' : '';
    const disabledAttr = disabled ? ' disabled' : '';
    const roleClass = r.role === 'ADMIN' ? 'role-admin' : 'role-patient';
    const roleLabel = r.role === 'ADMIN' ? 'Admin' : 'Patient';
    const noPhoneTag = disabled ? ' <span class="no-phone">(no phone)</span>' : '';

    return `
        <div class="recipient-item${disabledClass}">
            <label>
                <input type="checkbox" data-id="${r.id}" data-role="${r.role}"
                       onchange="updateState()"${disabledAttr}>
                <span>${escapeHtml(r.name)}</span>
                <span class="role-badge ${roleClass}">${roleLabel}</span>
                ${noPhoneTag}
            </label>
        </div>
    `;
}

function selectGroup(group) {
    const checkboxes = document.querySelectorAll('#recipientList input[type="checkbox"]:not(:disabled)');
    checkboxes.forEach(cb => {
        if (group === 'ALL') {
            cb.checked = true;
        } else if (group === 'NONE') {
            cb.checked = false;
        } else {
            cb.checked = cb.dataset.role === group;
        }
    });
    updateState();
}

function getSelectedRecipients() {
    const checked = document.querySelectorAll('#recipientList input[type="checkbox"]:checked');
    return Array.from(checked).map(cb => ({
        id: parseInt(cb.dataset.id),
        role: cb.dataset.role
    }));
}

function updateState() {
    const selected = getSelectedRecipients();
    const message = document.getElementById('messageBody').value;
    const sendBtn = document.getElementById('sendBtn');
    const countSpan = document.getElementById('selectedCount');
    const charCount = document.getElementById('charCount');

    countSpan.textContent = selected.length + ' recipient' + (selected.length !== 1 ? 's' : '') + ' selected';
    charCount.textContent = message.length + ' character' + (message.length !== 1 ? 's' : '');
    sendBtn.disabled = selected.length === 0 || message.trim().length === 0;
}

async function sendMessage() {
    const selected = getSelectedRecipients();
    const message = document.getElementById('messageBody').value.trim();
    const sendBtn = document.getElementById('sendBtn');
    const resultBox = document.getElementById('resultBox');

    if (selected.length === 0 || !message) return;

    sendBtn.disabled = true;
    sendBtn.textContent = 'Sending...';
    resultBox.style.display = 'none';

    try {
        const response = await fetch('/api/admin/messaging/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ recipients: selected, message: message })
        });

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                alert('Unauthorized. Please log in as root admin.');
                window.location.href = '/admin-login.html';
                return;
            }
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || 'Failed to send messages');
        }

        const result = await response.json();

        let summary = `Sent: ${result.sent}`;
        if (result.skippedNoPhone > 0) summary += ` | Skipped (no phone): ${result.skippedNoPhone}`;
        if (result.failed > 0) summary += ` | Failed: ${result.failed}`;

        if (result.errors && result.errors.length > 0) {
            summary += '\n\nErrors:\n' + result.errors.join('\n');
        }

        resultBox.textContent = summary;
        resultBox.style.whiteSpace = 'pre-wrap';

        if (result.failed > 0 || result.sent === 0) {
            resultBox.className = 'message-box error';
        } else {
            resultBox.className = 'message-box success';
        }
        resultBox.style.display = 'block';
    } catch (error) {
        console.error('Error sending messages:', error);
        resultBox.textContent = 'Error: ' + error.message;
        resultBox.className = 'message-box error';
        resultBox.style.display = 'block';
    } finally {
        sendBtn.textContent = 'Send SMS';
        updateState();
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', loadRecipients);

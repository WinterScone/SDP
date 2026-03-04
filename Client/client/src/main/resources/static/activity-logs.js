async function loadActivityLogs() {
    const logsTable = document.getElementById('logsTable');
    const logsBody = document.getElementById('logsBody');
    const loadingMessage = document.getElementById('loadingMessage');
    const noLogsMessage = document.getElementById('noLogsMessage');
    const errorMessage = document.getElementById('errorMessage');

    try {
        const response = await fetch('/api/admin/activity-logs', {
            method: 'GET',
            credentials: 'include'
        });

        loadingMessage.style.display = 'none';

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                alert('Unauthorized. Please log in as root admin.');
                window.location.href = '/admin-login.html';
                return;
            }
            throw new Error('Failed to fetch activity logs');
        }

        const logs = await response.json();

        if (!logs || logs.length === 0) {
            noLogsMessage.style.display = 'block';
            return;
        }

        // Show table and populate with logs
        logsTable.style.display = 'table';
        logsBody.innerHTML = logs.map(log => {
            const activityTypeClass = getActivityTypeClass(log.activityType);
            const formattedTime = formatTimestamp(log.timestamp);

            return `
                <tr>
                    <td class="timestamp">${formattedTime}</td>
                    <td><span class="activity-type ${activityTypeClass}">${formatActivityType(log.activityType)}</span></td>
                    <td class="description">${escapeHtml(log.description)}</td>
                    <td>${log.adminUsername || '-'}</td>
                    <td>${log.patientName || '-'}</td>
                    <td>${log.medicineName || '-'}</td>
                </tr>
            `;
        }).join('');

    } catch (error) {
        console.error('Error loading activity logs:', error);
        loadingMessage.style.display = 'none';
        errorMessage.style.display = 'block';
    }
}

function getActivityTypeClass(activityType) {
    if (activityType.includes('MEDICINE')) return 'type-medicine';
    if (activityType.includes('PRESCRIPTION')) return 'type-prescription';
    if (activityType.includes('ADMIN')) return 'type-admin';
    if (activityType.includes('PATIENT')) return 'type-patient';
    if (activityType.includes('RESET')) return 'type-reset';
    return '';
}

function formatActivityType(activityType) {
    return activityType.replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, c => c.toUpperCase());
}

function formatTimestamp(timestamp) {
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Load logs when page loads
document.addEventListener('DOMContentLoaded', loadActivityLogs);

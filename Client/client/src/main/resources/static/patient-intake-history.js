const rowsEl    = document.getElementById("rows");
const messageEl = document.getElementById("message");
const titleEl   = document.getElementById("title");
const logoutBtn = document.getElementById("logoutBtn");

function escapeHtml(str) {
    return String(str ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

logoutBtn.addEventListener("click", () => {
    localStorage.removeItem("patientId");
    localStorage.removeItem("patientUsername");
    window.location.href = "/patient-login.html";
});

async function loadHistory(patientId) {
    messageEl.textContent = "Loading...";
    rowsEl.innerHTML = "";

    const res = await fetch(`/api/patient/${patientId}/intake`);
    const data = await res.json().catch(() => null);

    if (!res.ok || !data) {
        messageEl.textContent = "Could not load intake history.";
        return;
    }

    const list = data.history || [];
    if (list.length === 0) {
        messageEl.textContent = "No intake history recorded yet.";
        return;
    }

    messageEl.textContent = "";
    rowsEl.innerHTML = list.map(h => `
        <tr>
            <td>${escapeHtml(h.medicineId)}</td>
            <td>${escapeHtml(h.medicineName)}</td>
            <td>${escapeHtml(h.takenDate)}</td>
            <td>${escapeHtml(h.takenTime)}</td>
            <td>${escapeHtml(h.notes)}</td>
        </tr>
    `).join("");
}

async function init() {
    const patientId = localStorage.getItem("patientId");
    const username  = localStorage.getItem("patientUsername");

    if (!patientId) {
        window.location.href = "/patient-login.html";
        return;
    }

    if (username) titleEl.textContent = `Intake History — ${username}`;

    await loadHistory(patientId);
}

init();

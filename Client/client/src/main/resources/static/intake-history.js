const rowsEl      = document.getElementById("rows");
const messageEl   = document.getElementById("message");
const titleEl     = document.getElementById("title");
const logoutBtn   = document.getElementById("logoutBtn");
const medicineSelect = document.getElementById("medicineSelect");
const takenDateEl = document.getElementById("takenDate");
const takenTimeEl = document.getElementById("takenTime");
const notesEl     = document.getElementById("notesInput");
const logBtn      = document.getElementById("logBtn");
const logMessageEl = document.getElementById("logMessage");

const urlParams   = new URLSearchParams(window.location.search);
const adminView   = urlParams.has("patientId");
const backBtn     = document.getElementById("backBtn");

function escapeHtml(str) {
    return String(str ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function setNow() {
    const now = new Date();
    const date = now.toISOString().slice(0, 10);
    const time = now.toTimeString().slice(0, 5);
    takenDateEl.value = date;
    takenTimeEl.value = time;
}

logoutBtn.addEventListener("click", () => {
    if (adminView) {
        history.back();
    } else {
        localStorage.removeItem("patientId");
        localStorage.removeItem("patientUsername");
        window.location.href = "/patient-login.html";
    }
});

async function loadPrescriptions(patientId) {
    const res = await fetch(`/api/patients/${patientId}/prescriptions`);
    const data = await res.json().catch(() => null);
    if (!data || !res.ok) return;

    const list = data.prescriptions || [];
    medicineSelect.innerHTML = '<option value="">— select —</option>';
    list.forEach(p => {
        const opt = document.createElement("option");
        opt.value = p.medicineId;
        opt.textContent = `${p.medicineName} (${p.medicineId})`;
        medicineSelect.appendChild(opt);
    });
}

async function loadHistory(patientId) {
    messageEl.textContent = "Loading...";
    rowsEl.innerHTML = "";

    const res = await fetch(`/api/patients/${patientId}/intake`);
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

logBtn.addEventListener("click", async () => {
    const patientId = adminView
        ? urlParams.get("patientId")
        : localStorage.getItem("patientId");
    const medicineId = medicineSelect.value;

    if (!medicineId) {
        logMessageEl.textContent = "Please select a medicine.";
        logMessageEl.className = "error";
        return;
    }

    logBtn.disabled = true;
    logMessageEl.textContent = "";

    try {
        const res = await fetch(`/api/patients/${patientId}/intake`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                medicineId,
                takenDate: takenDateEl.value,
                takenTime: takenTimeEl.value,
                notes: notesEl.value.trim()
            })
        });

        const data = await res.json().catch(() => null);

        if (!res.ok || !data?.ok) {
            logMessageEl.textContent = data?.error || "Failed to log intake.";
            logMessageEl.className = "error";
        } else {
            logMessageEl.textContent = "Intake logged successfully.";
            logMessageEl.className = "success";
            notesEl.value = "";
            setNow();
            await loadHistory(patientId);
        }
    } catch {
        logMessageEl.textContent = "Network error.";
        logMessageEl.className = "error";
    } finally {
        logBtn.disabled = false;
    }
});

async function init() {
    const patientId = adminView
        ? urlParams.get("patientId")
        : localStorage.getItem("patientId");

    if (!patientId) {
        window.location.href = "/patient-login.html";
        return;
    }

    if (adminView) {
        backBtn.style.display = "none";
        logoutBtn.textContent = "← Back";
        titleEl.textContent = `Intake History — Patient #${patientId}`;
    } else {
        const username = localStorage.getItem("patientUsername");
        if (username) titleEl.textContent = `Intake History — ${username}`;
    }

    setNow();
    await Promise.all([
        loadPrescriptions(patientId),
        loadHistory(patientId)
    ]);
}

init();

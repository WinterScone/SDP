const rowsEl = document.getElementById("rows");
const messageEl = document.getElementById("message");
const titleEl = document.getElementById("title");
const logoutBtn = document.getElementById("logoutBtn");

function setMessage(text) {
    messageEl.textContent = text || "";
}

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

async function loadPrescriptions() {
    const patientId = localStorage.getItem("patientId");
    const username = localStorage.getItem("patientUsername");

    if (!patientId) {
        window.location.href = "/patient-login.html";
        return;
    }

    if (username) {
        titleEl.textContent = `Prescriptions for ${username}`;
    }

    setMessage("Loading...");

    try {
        const res = await fetch(`/api/patient/${patientId}/prescriptions`);
        const data = await res.json().catch(() => null);

        if (!res.ok || !data) {
            setMessage("Could not load prescriptions.");
            return;
        }

        const list = data.prescriptions || [];

        if (list.length === 0) {
            rowsEl.innerHTML = "";
            setMessage("No prescriptions found.");
            return;
        }

        rowsEl.innerHTML = list.map(p => `
      <tr>
        <td class="nowrap">${escapeHtml(p.prescriptionId)}</td>
        <td class="nowrap">${p.medicineNumber ? `Dispenser ${escapeHtml(p.medicineNumber)}` : "-"}</td>
        <td class="nowrap">${escapeHtml(p.medicineCode)}</td>
        <td class="wrap">${escapeHtml(p.medicineName)}</td>
        <td class="nowrap">${escapeHtml(p.dosage)}</td>
        <td class="nowrap">${escapeHtml(p.scheduledTime)}</td>
      </tr>
    `).join("");

        setMessage("");
    } catch (e) {
        setMessage("Network error while loading prescriptions.");
    }
}

loadPrescriptions();
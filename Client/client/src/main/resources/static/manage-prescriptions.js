const params = new URLSearchParams(window.location.search);
const patientId = params.get("patientId");

const messageEl = document.getElementById("message");
const patientInfoEl = document.getElementById("patientInfo");
const rowsEl = document.getElementById("rows");

const showFormBtn = document.getElementById("showFormBtn");
const prescriptionForm = document.getElementById("prescriptionForm");
const cancelBtn = document.getElementById("cancelBtn");
const addBtn = document.getElementById("addBtn");

const newMedicineEl = document.getElementById("newMedicine");
const newDosageEl = document.getElementById("newDosage");
const newFrequencyEl = document.getElementById("newFrequency");
const newScheduledTimesEl = document.getElementById("newScheduledTimes");

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

function escapeAttr(str) {
    return escapeHtml(str);
}

function parseTimes(value) {
    if (!value.trim()) return [];

    const parts = value
        .split(",")
        .map(t => t.trim())
        .filter(Boolean);

    const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)$/;

    for (const t of parts) {
        if (!timeRegex.test(t)) {
            return null;
        }
    }

    return parts;
}

function showAddForm() {
    prescriptionForm.style.display = "block";
    showFormBtn.style.display = "none";
    setMessage("");
}

function hideAddForm() {
    prescriptionForm.style.display = "none";
    showFormBtn.style.display = "inline-block";
    newMedicineEl.value = "";
    newDosageEl.value = "";
    newFrequencyEl.value = "";
    newScheduledTimesEl.value = "";
    setMessage("");
}

showFormBtn.addEventListener("click", showAddForm);
cancelBtn.addEventListener("click", hideAddForm);
addBtn.addEventListener("click", addPrescription);

async function loadMedicines() {
    try {
        const res = await fetch("/api/admin/medicines");
        if (!res.ok) {
            setMessage("Failed to load medicines.");
            return;
        }

        const medicines = await res.json();

        newMedicineEl.innerHTML = `
            <option value="">Select medicine</option>
            ${medicines.map(m => `
                <option value="${escapeAttr(m.medicineId)}">
                    ${escapeHtml(m.medicineName)} (${escapeHtml(m.medicineId)})
                </option>
            `).join("")}
        `;
    } catch (e) {
        setMessage("Network error while loading medicines.");
    }
}

async function loadPatient() {
    if (!patientId) {
        setMessage("Missing patientId.");
        return;
    }

    try {
        const res = await fetch(`/api/admin/patients/${patientId}`);
        if (!res.ok) {
            setMessage("Failed to load patient details.");
            return;
        }

        const data = await res.json();

        patientInfoEl.innerHTML = `
            <tr>
                <th>First Name</th>
                <td>${escapeHtml(data.firstName)}</td>
            </tr>
            <tr>
                <th>Last Name</th>
                <td>${escapeHtml(data.lastName)}</td>
            </tr>
            <tr>
                <th>Date of Birth</th>
                <td>${escapeHtml(data.dateOfBirth)}</td>
            </tr>
            <tr>
                <th>Email</th>
                <td>${escapeHtml(data.email)}</td>
            </tr>
            <tr>
                <th>Phone</th>
                <td>${escapeHtml(data.phone)}</td>
            </tr>
        `;

        renderPrescriptions(data.prescriptions || []);
        setMessage("");
    } catch (e) {
        setMessage("Network error while loading patient details.");
    }
}

function renderPrescriptions(prescriptions) {
    if (!prescriptions.length) {
        rowsEl.innerHTML = `
            <tr>
                <td colspan="7">No prescriptions found.</td>
            </tr>
        `;
        return;
    }

    rowsEl.innerHTML = prescriptions.map(p => `
        <tr>
            <td class="nowrap">${p.medicineNumber ? `Dispenser ${escapeHtml(p.medicineNumber)}` : "-"}</td>
            <td class="wrap">${escapeHtml(p.medicineName ?? "")}</td>
            <td><input type="text" value="${escapeAttr(p.dosage ?? "")}" id="dosage-${p.id}" /></td>
            <td><input type="text" value="${escapeAttr(p.frequency ?? "")}" id="frequency-${p.id}" /></td>
            <td><input type="text" value="${escapeAttr((p.scheduledTimes || []).join(", "))}" id="times-${p.id}" /></td>
            <td><button type="button" onclick="updatePrescription(${p.id})">Update</button></td>
            <td><button type="button" onclick="deletePrescription(${p.id})">Delete</button></td>
        </tr>
    `).join("");
}

async function addPrescription() {
    const medicineId = newMedicineEl.value;
    const dosage = newDosageEl.value.trim();
    const frequency = newFrequencyEl.value.trim();
    const scheduledTimes = parseTimes(newScheduledTimesEl.value);

    if (!medicineId || !dosage || !frequency) {
        setMessage("Please fill in medicine, dosage and frequency.");
        return;
    }

    if (scheduledTimes === null) {
        setMessage("Scheduled times must be like 08:00, 20:00");
        return;
    }

    const today = new Date().toISOString().slice(0, 10);

    const payload = {
        medicineId,
        dosage,
        frequency,
        startDate: today,
        endDate: "2026-12-31",
        reminderTimes: scheduledTimes,
        scheduledTimes: scheduledTimes
    };

    try {
        const res = await fetch(`/api/admin/patients/${patientId}/prescriptions`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            setMessage(text || "Failed to add prescription.");
            return;
        }

        hideAddForm();
        await loadPatient();
        setMessage("Prescription added.");
    } catch (e) {
        setMessage("Network error while adding prescription.");
    }
}

async function updatePrescription(id) {
    const dosage = document.getElementById(`dosage-${id}`).value.trim();
    const frequency = document.getElementById(`frequency-${id}`).value.trim();
    const scheduledTimes = parseTimes(document.getElementById(`times-${id}`).value);

    if (!dosage || !frequency) {
        setMessage("Dosage and frequency are required.");
        return;
    }

    if (scheduledTimes === null) {
        setMessage("Scheduled times must be like 08:00, 20:00");
        return;
    }

    try {
        const res = await fetch(`/api/admin/prescriptions/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                dosage,
                frequency,
                scheduledTimes
            })
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            setMessage(text || "Failed to update prescription.");
            return;
        }

        await loadPatient();
        setMessage("Prescription updated.");
    } catch (e) {
        setMessage("Network error while updating prescription.");
    }
}

async function deletePrescription(id) {
    if (!confirm("Delete this prescription?")) return;

    try {
        const res = await fetch(`/api/admin/prescriptions/${id}`, {
            method: "DELETE"
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            setMessage(text || "Failed to delete prescription.");
            return;
        }

        await loadPatient();
        setMessage("Prescription deleted.");
    } catch (e) {
        setMessage("Network error while deleting prescription.");
    }
}

loadMedicines();
loadPatient();
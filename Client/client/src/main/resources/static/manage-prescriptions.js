(async () => {
    const authRes = await fetch("/api/verify/me");
    if (!authRes.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const params = new URLSearchParams(window.location.search);
    const patientId = params.get("patientId");

    const msg = document.getElementById("message");
    const patientInfo = document.getElementById("patientInfo");
    const rows = document.getElementById("rows");

    const prescriptionForm = document.getElementById("prescriptionForm");
    const showFormBtn = document.getElementById("showFormBtn");
    const cancelBtn = document.getElementById("cancelBtn");
    const newMedicine = document.getElementById("newMedicine");
    const newDosage = document.getElementById("newDosage");
    const newFrequency = document.getElementById("newFrequency");
    const newScheduledTimes = document.getElementById("newScheduledTimes");

    showFormBtn.addEventListener("click", () => {
        prescriptionForm.style.display = "block";
        showFormBtn.style.display = "none";
    });

    cancelBtn.addEventListener("click", () => {
        prescriptionForm.style.display = "none";
        showFormBtn.style.display = "block";
        newMedicine.value = "";
        newDosage.value = "";
        newFrequency.value = "";
        newScheduledTimes.value = "";
        msg.textContent = "";
    });

    document.getElementById("addBtn").addEventListener("click", addPrescription);

    let medicinesCache = [];

    init();

    async function init() {
        if (!patientId) {
            msg.textContent = "Missing patientId in URL.";
            return;
        }

        await loadMedicines();
        await loadPatientAndPrescriptions();
    }

    async function loadMedicines() {
        const res = await fetch("/api/admin/medicines");
        if (!res.ok) {
            msg.textContent = "Failed to load medicines.";
            return;
        }

        medicinesCache = await res.json();

        newMedicine.innerHTML =
            `<option value="">Select Medicine</option>` +
            medicinesCache
                .map(m => `<option value="${escapeAttr(m.medicineId)}">${escapeHtml(m.medicineName)}</option>`)
                .join("");
    }

    async function loadPatientAndPrescriptions() {
        const res = await fetch(`/api/admin/patients/${patientId}`);
        if (!res.ok) {
            msg.textContent = "Failed to load patient.";
            return;
        }

        const p = await res.json();

        patientInfo.innerHTML = `
        <tr><th style="width:220px;">First Name</th><td>${safe(p.firstName)}</td></tr>
        <tr><th>Last Name</th><td>${safe(p.lastName)}</td></tr>
        <tr><th>Date of Birth</th><td>${safe(p.dateOfBirth)}</td></tr>
        <tr><th>Email</th><td>${safe(p.email)}</td></tr>
        <tr><th>Phone</th><td>${safe(p.phone)}</td></tr>
      `;

        renderPrescriptions(p.prescriptions || []);
    }

    function renderPrescriptions(list) {
        rows.innerHTML = "";

        if (list.length === 0) {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td colspan="6">No prescriptions found.</td>`;
            rows.appendChild(tr);
            return;
        }

        list.forEach(rx => {
            const times = formatScheduledTimesForInput(rx);

            const tr = document.createElement("tr");
            tr.innerHTML = `
          <td>${escapeHtml(rx.medicineName || rx.medicine?.medicineName || "")}</td>
          <td><input id="dosage-${rx.id}" value="${escapeAttr(rx.dosage || "")}" /></td>
          <td><input id="freq-${rx.id}" value="${escapeAttr(rx.frequency || "")}" /></td>
          <td><input id="times-${rx.id}" value="${escapeAttr(times)}" placeholder="08:00, 20:00" /></td>
          <td><button onclick="saveRx(${rx.id})">Save</button></td>
          <td><button onclick="deleteRx(${rx.id})">Delete</button></td>
        `;
            rows.appendChild(tr);
        });
    }

    window.saveRx = async function(id) {
        const dosage = document.getElementById(`dosage-${id}`).value.trim();
        const frequency = document.getElementById(`freq-${id}`).value.trim();
        const timesRaw = document.getElementById(`times-${id}`).value.trim();

        if (!dosage || !frequency) {
            msg.textContent = "Dosage and frequency are required.";
            return;
        }

        const scheduledTimes = parseTimes(timesRaw);
        if (scheduledTimes === null) {
            msg.textContent = "Invalid time format. Use 08:00, 20:00";
            return;
        }

        const res = await fetch(`/api/admin/prescriptions/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                dosage,
                frequency,
                scheduledTimes
            })
        });

        msg.textContent = res.ok ? "Saved." : "Save failed.";
        if (res.ok) await loadPatientAndPrescriptions();
    };

    window.deleteRx = async function(id) {
        if (!confirm("Delete this prescription?")) return;

        const res = await fetch(`/api/admin/prescriptions/${id}`, {
            method: "DELETE"
        });

        msg.textContent = res.ok ? "Deleted." : "Delete failed.";
        if (res.ok) await loadPatientAndPrescriptions();
    };

    async function addPrescription() {
        const medicineId = newMedicine.value;
        const dosage = newDosage.value.trim();
        const frequency = newFrequency.value.trim();
        const timesRaw = newScheduledTimes.value.trim();

        if (!medicineId || !dosage || !frequency) {
            msg.textContent = "Select medicine + dosage + frequency.";
            return;
        }

        const scheduledTimes = parseTimes(timesRaw);
        if (scheduledTimes === null) {
            msg.textContent = "Invalid time format. Use 08:00, 20:00";
            return;
        }

        const res = await fetch(`/api/admin/patients/${patientId}/prescriptions`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                medicineId,
                dosage,
                frequency,
                scheduledTimes
            })
        });

        if (res.ok) {
            msg.textContent = "Added.";
            newMedicine.value = "";
            newDosage.value = "";
            newFrequency.value = "";
            newScheduledTimes.value = "";
            prescriptionForm.style.display = "none";
            showFormBtn.style.display = "block";
            await loadPatientAndPrescriptions();
        } else {
            msg.textContent = "Add failed.";
        }
    }

    function parseTimes(raw) {
        if (!raw) return [];

        const parts = raw
            .split(",")
            .map(s => s.trim())
            .filter(Boolean);

        const regex = /^([01]\d|2[0-3]):([0-5]\d)$/;

        for (const t of parts) {
            if (!regex.test(t)) {
                return null;
            }
        }

        return parts;
    }

    function formatScheduledTimesForInput(rx) {
        if (Array.isArray(rx.scheduledTimes)) {
            return rx.scheduledTimes.join(", ");
        }

        if (Array.isArray(rx.reminderTimes)) {
            return rx.reminderTimes
                .map(rt => {
                    if (typeof rt === "string") return rt;
                    if (rt && rt.reminderTime) return rt.reminderTime;
                    return "";
                })
                .filter(Boolean)
                .join(", ");
        }

        return "";
    }

    function safe(v) {
        return v == null ? "" : String(v);
    }

    function escapeHtml(s) {
        return safe(s)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");
    }

    function escapeAttr(s) {
        return escapeHtml(s).replaceAll('"', "&quot;");
    }
})();
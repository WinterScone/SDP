(async () => {
    const authRes = await fetch("/api/auth/admins/me");
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
    const newScheduledTimesContainer = document.getElementById("newScheduledTimesContainer");
    const formMessage = document.getElementById("formMessage");
    const unitDoseHint = document.getElementById("unitDoseHint");
    const newDosageHint = document.getElementById("newDosageHint");

    const FREQUENCY_DEFAULTS = {
        ONCE_A_DAY:        ["08:00"],
        TWICE_A_DAY:       ["08:00", "20:00"],
        THREE_TIMES_A_DAY: ["08:00", "14:00", "20:00"],
        FOUR_TIMES_A_DAY:  ["08:00", "12:00", "16:00", "20:00"]
    };

    function generateTimePickers(prefix, frequency, existingTimes) {
        const defaults = FREQUENCY_DEFAULTS[frequency];
        if (!defaults) return "";
        return defaults.map((def, i) => {
            const val = existingTimes && existingTimes[i] ? existingTimes[i] : def;
            return `<input type="time" id="${prefix}-time-${i}" value="${val}" />`;
        }).join(" ");
    }

    function collectTimes(prefix, frequency) {
        const defaults = FREQUENCY_DEFAULTS[frequency];
        if (!defaults) return [];
        return defaults.map((_, i) => {
            const el = document.getElementById(`${prefix}-time-${i}`);
            return el ? el.value : "";
        }).filter(v => v.length > 0);
    }

    function frequencySelectHtml(id, selectedValue) {
        const options = [
            {value: "ONCE_A_DAY", label: "Once a day"},
            {value: "TWICE_A_DAY", label: "Twice a day"},
            {value: "THREE_TIMES_A_DAY", label: "Three times a day"},
            {value: "FOUR_TIMES_A_DAY", label: "Four times a day"}
        ];
        return `<select id="freq-${id}" onchange="onEditFreqChange(${id})">` +
            options.map(o => `<option value="${o.value}"${o.value === selectedValue ? ' selected' : ''}>${o.label}</option>`).join("") +
            `</select>`;
    }

    function updateNewDosageHint() {
        const selected = medicinesCache.find(m => String(m.medicineId) === newMedicine.value);
        const unitDose = selected && selected.unitDose != null ? selected.unitDose : null;
        const qty = parseFloat(newDosage.value);
        newDosageHint.textContent = unitDose && !isNaN(qty) ? `Total Dosage: ${qty * unitDose}mg` : "";
    }

    newMedicine.addEventListener("change", () => {
        const selected = medicinesCache.find(m => String(m.medicineId) === newMedicine.value);
        unitDoseHint.textContent = selected && selected.unitDose != null
            ? `Unit Dosage: ${selected.unitDose}mg`
            : "";
        updateNewDosageHint();
    });

    newDosage.addEventListener("input", updateNewDosageHint);

    newFrequency.addEventListener("change", () => {
        newScheduledTimesContainer.innerHTML = generateTimePickers("new", newFrequency.value, null);
    });

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
        newScheduledTimesContainer.innerHTML = "";
        unitDoseHint.textContent = "";
        newDosageHint.textContent = "";
        msg.textContent = "";
    });

    document.getElementById("addBtn").addEventListener("click", addPrescription);

    let medicinesCache = [];

    init();

    async function init(){
        if(!patientId){
            msg.textContent = "Missing patientId in URL.";
            return;
        }

        await loadMedicines();
        await loadPatientAndPrescriptions();
    }

    async function loadMedicines(){
        const res = await fetch("/api/admin/medicines");
        if(!res.ok){
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

    async function loadPatientAndPrescriptions(){
        const res = await fetch(`/api/admin/patients/${patientId}`);
        if(!res.ok){
            msg.textContent = "Failed to load patient.";
            return;
        }

        const p = await res.json();

        patientInfo.innerHTML = `
        <tr><th style="width:220px;">First Name</th><td>${safe(p.firstName)}</td></tr>
        <tr><th>Last Name</th><td>${safe(p.lastName)}</td></tr>
        <tr><th>Date of Birth</th><td>${safe(p.dateOfBirth)}</td></tr>
        <tr><th>Email</th><td>${safe(p.email)}</td></tr>
        <tr><th>Phone</th><td>${formatPhone(p.phone)}</td></tr>
      `;

        renderPrescriptions(p.prescriptions || []);
    }

    function renderPrescriptions(list){
        rows.innerHTML = "";

        if(list.length === 0){
            const tr = document.createElement("tr");
            tr.innerHTML = `<td colspan="9">No prescriptions found.</td>`;
            rows.appendChild(tr);
            return;
        }

        list.forEach(rx => {
            const unitDose = rx.unitDose;
            const qtyVal = unitDose ? Math.round(parseFloat(rx.dosage || 0) / unitDose) : (rx.dosage || "");
            const initialHint = unitDose && qtyVal !== "" ? `${qtyVal * unitDose}mg` : "";
            const tr = document.createElement("tr");
            tr.innerHTML = `
          <td>${escapeHtml(rx.medicineName)}</td>
          <td>${escapeHtml(rx.medicineId)}</td>
          <td>${escapeHtml(rx.unitDose != null ? rx.unitDose + 'mg' : '-')}</td>
          <td><input id="qty-${rx.id}" value="${escapeAttr(String(qtyVal))}" data-unit-dose="${escapeAttr(String(unitDose ?? ''))}" oninput="updateDosageHint(${rx.id})" placeholder="Quantity" /></td>
          <td id="dosage-hint-${rx.id}" style="font-size:13px; color:#57606a;">${escapeHtml(initialHint)}</td>
          <td>${frequencySelectHtml(rx.id, rx.frequency)}</td>
          <td><div id="times-container-${rx.id}">${generateTimePickers(`rx-${rx.id}`, rx.frequency, rx.scheduledTimes)}</div></td>
          <td><button onclick="saveRx(${rx.id})">Save</button></td>
          <td><button onclick="deleteRx(${rx.id})">Delete</button></td>
        `;
            rows.appendChild(tr);
        });
    }

    window.updateDosageHint = function(id) {
        const qtyInput = document.getElementById(`qty-${id}`);
        const hintEl = document.getElementById(`dosage-hint-${id}`);
        const unitDose = parseFloat(qtyInput.dataset.unitDose);
        const qty = parseFloat(qtyInput.value);
        hintEl.textContent = unitDose && !isNaN(qty) ? `${qty * unitDose}mg` : "";
    };

    window.saveRx = async function(id){
        const qtyInput = document.getElementById(`qty-${id}`);
        const qty = qtyInput.value.trim();
        const unitDose = parseFloat(qtyInput.dataset.unitDose);
        const dosage = unitDose && qty !== "" ? String(parseFloat(qty) * unitDose) : qty;
        const frequency = document.getElementById(`freq-${id}`).value.trim();
        const scheduledTimes = collectTimes(`rx-${id}`, frequency);

        if(!qty || !frequency){
            msg.textContent = "Quantity and frequency are required.";
            return;
        }

        const res = await fetch(`/api/admin/prescriptions/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ dosage, frequency, scheduledTimes })
        });

        msg.textContent = res.ok ? "Saved." : "Save failed.";
        if(res.ok) await loadPatientAndPrescriptions();
    };

    window.onEditFreqChange = function(id) {
        const freq = document.getElementById(`freq-${id}`).value;
        const container = document.getElementById(`times-container-${id}`);
        container.innerHTML = generateTimePickers(`rx-${id}`, freq, null);
    };

    window.deleteRx = async function(id){
        if(!confirm("Delete this prescription?")) return;

        const res = await fetch(`/api/admin/prescriptions/${id}`, { method: "DELETE" });
        msg.textContent = res.ok ? "Deleted." : "Delete failed.";
        if(res.ok) await loadPatientAndPrescriptions();
    };

    async function addPrescription(){
        const medicineId = newMedicine.value;
        const qty = newDosage.value.trim();
        const frequency = newFrequency.value.trim();
        const scheduledTimes = collectTimes("new", frequency);

        if(!medicineId || !qty || !frequency){
            formMessage.textContent = "Select medicine, quantity, and frequency.";
            formMessage.className = "msg error";
            return;
        }

        if(scheduledTimes.length === 0){
            msg.textContent = "At least one scheduled time is required.";
            return;
        }

        const selected = medicinesCache.find(m => String(m.medicineId) === medicineId);
        const unitDose = selected && selected.unitDose != null ? selected.unitDose : null;
        const dosage = unitDose ? String(parseFloat(qty) * unitDose) : qty;

        const res = await fetch(`/api/admin/patients/${patientId}/prescriptions`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ medicineId: Number(medicineId), dosage, frequency, scheduledTimes })
        });

        if(res.ok){
            msg.textContent = "Added.";
            newMedicine.value = "";
            newDosage.value = "";
            newFrequency.value = "";
            newScheduledTimesContainer.innerHTML = "";
            unitDoseHint.textContent = "";
            newDosageHint.textContent = "";
            prescriptionForm.style.display = "none";
            showFormBtn.style.display = "block";
            await loadPatientAndPrescriptions();
        } else {
            msg.textContent = "Add failed (maybe already exists).";
        }
    }

    function safe(v){ return v == null ? "" : String(v); }

    function formatPhone(phone) {
        if (!phone) return "-";
        const ukMatch = phone.match(/^\+44(\d{4})(\d{6})$/);
        if (ukMatch) return "+44 " + ukMatch[1] + " " + ukMatch[2];
        if (/^07\d{9}$/.test(phone)) return phone.slice(0, 5) + " " + phone.slice(5);
        return phone;
    }

    function escapeHtml(s){
        return safe(s)
            .replaceAll("&","&amp;")
            .replaceAll("<","&lt;")
            .replaceAll(">","&gt;");
    }

    function escapeAttr(s){
        return escapeHtml(s).replaceAll('"',"&quot;");
    }
})();

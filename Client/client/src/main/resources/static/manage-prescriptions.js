const params = new URLSearchParams(window.location.search);
const patientId = params.get("patientId");

const msg = document.getElementById("message");
const patientInfo = document.getElementById("patientInfo");
const rows = document.getElementById("rows");

const newMedicine = document.getElementById("newMedicine");
const newDosage = document.getElementById("newDosage");
const newFrequency = document.getElementById("newFrequency");
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
    <tr><th>Phone</th><td>${safe(p.phone)}</td></tr>
  `;

    renderPrescriptions(p.prescriptions || []);
}

function renderPrescriptions(list){
    rows.innerHTML = "";

    if(list.length === 0){
        const tr = document.createElement("tr");
        tr.innerHTML = `<td colspan="5">No prescriptions found.</td>`;
        rows.appendChild(tr);
        return;
    }

    list.forEach(rx => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
      <td>${escapeHtml(rx.medicineName)}</td>
      <td><input id="dosage-${rx.id}" value="${escapeAttr(rx.dosage)}" /></td>
      <td><input id="freq-${rx.id}" value="${escapeAttr(rx.frequency)}" /></td>
      <td><button onclick="saveRx(${rx.id})">Save</button></td>
      <td><button onclick="deleteRx(${rx.id})">Delete</button></td>
    `;
        rows.appendChild(tr);
    });
}

async function saveRx(id){
    const dosage = document.getElementById(`dosage-${id}`).value.trim();
    const frequency = document.getElementById(`freq-${id}`).value.trim();

    if(!dosage || !frequency){
        msg.textContent = "Dosage and frequency are required.";
        return;
    }

    const res = await fetch(`/api/admin/prescriptions/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ dosage, frequency })
    });

    msg.textContent = res.ok ? "Saved." : "Save failed.";
    if(res.ok) await loadPatientAndPrescriptions();
}

async function deleteRx(id){
    if(!confirm("Delete this prescription?")) return;

    const res = await fetch(`/api/admin/prescriptions/${id}`, { method: "DELETE" });
    msg.textContent = res.ok ? "Deleted." : "Delete failed.";
    if(res.ok) await loadPatientAndPrescriptions();
}

async function addPrescription(){
    const medicineId = newMedicine.value; // enum string
    const dosage = newDosage.value.trim();
    const frequency = newFrequency.value.trim();

    if(!medicineId || !dosage || !frequency){
        msg.textContent = "Select medicine + dosage + frequency.";
        return;
    }

    const res = await fetch(`/api/admin/patients/${patientId}/prescriptions`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ medicineId, dosage, frequency })
    });

    if(res.ok){
        msg.textContent = "Added.";
        newMedicine.value = "";
        newDosage.value = "";
        newFrequency.value = "";
        await loadPatientAndPrescriptions();
    } else {
        msg.textContent = "Add failed (maybe already exists).";
    }
}

function safe(v){ return v == null ? "" : String(v); }

function escapeHtml(s){
    return safe(s)
        .replaceAll("&","&amp;")
        .replaceAll("<","&lt;")
        .replaceAll(">","&gt;");
}

function escapeAttr(s){
    return escapeHtml(s).replaceAll('"',"&quot;");
}

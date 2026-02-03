const params = new URLSearchParams(window.location.search);
const q = params.get("q") || "";

const rows = document.getElementById("rows");
const msg = document.getElementById("message");

loadPatients();

async function loadPatients() {
    if (!q.trim()) {
        msg.textContent = "No search keyword provided.";
        return;
    }

    msg.textContent = `Searching: ${q}`;

    const res = await fetch(`/api/admin/patients/search?q=${encodeURIComponent(q)}`);
    if (!res.ok) {
        msg.textContent = "Failed to load patients.";
        return;
    }

    const patients = await res.json();
    rows.innerHTML = "";

    if (patients.length === 0) {
        msg.textContent = "No patients found.";
        return;
    }

    patients.forEach(p => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
      <td>${p.id}</td>
      <td>${safe(p.firstName)}</td>
      <td>${safe(p.lastName)}</td>
      <td>${safe(p.dateOfBirth)}</td>
      <td>${safe(p.email)}</td>
      <td>${safe(p.phone)}</td>
      <td><button onclick="goPrescriptions(${p.id})">Manage Prescriptions</button></td>
    `;
        rows.appendChild(tr);
    });

    msg.textContent = `Found ${patients.length} patient(s).`;
}

function goPrescriptions(patientId) {
    window.location.href = `/manage-prescriptions.html?patientId=${patientId}`;
}

function safe(v) { return v == null ? "" : String(v); }

(async () => {
    const authRes = await fetch("/api/verify/me");
    if (!authRes.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const params = new URLSearchParams(window.location.search);
    const q = (params.get("q") || "").trim();

    const rows = document.getElementById("rows");
    const msg = document.getElementById("message");

    window.goPrescriptions = function(patientId) {
        window.location.href = `/manage-prescriptions.html?patientId=${patientId}`;
    };

    window.goIntakeHistory = function(patientId) {
        window.location.href = `/intake-history.html?patientId=${patientId}`;
    };

    loadPatients();

    async function loadPatients() {
        if (!q) {
            msg.textContent = "No search keyword provided.";
            return;
        }

        try {
            const res = await fetch(`/api/admin/patients/search?q=${encodeURIComponent(q)}`, {
                credentials: "include",
            });

            if (!res.ok) {
                const text = await res.text().catch(() => "");
                msg.textContent = `Failed to load patients. (${res.status}) ${text}`;
                return;
            }

            const patients = await res.json();
            renderRows(patients);

            msg.textContent = patients.length
                ? `Found ${patients.length} patient(s).`
                : "No patients found.";
        } catch (err) {
            console.error(err);
            msg.textContent = "Network error while loading patients.";
        }
    }

    function renderRows(patients) {
        rows.innerHTML = "";

        patients.forEach((p) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
          <td>${safe(p.id)}</td>
          <td>${safe(p.firstName)}</td>
          <td>${safe(p.lastName)}</td>
          <td>${formatDob(p.dateOfBirth)}</td>
          <td>${safe(p.email)}</td>
          <td>${safe(p.phone)}</td>
          <td style="display:flex; gap:6px;">
            <button onclick="goPrescriptions(${safe(p.id)})">
              Manage Prescriptions
            </button>
            <button onclick="goIntakeHistory(${safe(p.id)})">
              View Intake History
            </button>
          </td>
        `;
            rows.appendChild(tr);
        });
    }

    function formatDob(dob) {
        if (!dob) return "";
        return String(dob).split("T")[0];
    }

    function safe(v) {
        return v == null ? "" : String(v);
    }
})();

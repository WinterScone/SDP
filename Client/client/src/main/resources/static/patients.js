(async () => {
    const res = await fetch("/api/verify/me");
    if (!res.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const rows = document.getElementById("rows");
    const msg = document.getElementById("message");
    const qInput = document.getElementById("q");
    const searchBtn = document.getElementById("searchBtn");

    // Load whole database by default
    loadAllPatients();

    // Redirect when searching
    searchBtn.addEventListener("click", goSearch);
    qInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") goSearch();
    });

    function goSearch() {
        const q = qInput.value.trim();
        if (!q) return; // stay on page showing all patients
        window.location.href = `/patient-results.html?q=${encodeURIComponent(q)}`;
    }

    async function loadAllPatients() {
        msg.textContent = "Loading patients...";

        try {
            const res = await fetch("/api/patient/getAllPatients");

            const contentType = res.headers.get("content-type") || "";
            const text = await res.text();

            if (!res.ok) {
                msg.textContent = `Failed (${res.status}). ${text.slice(0, 120)}`;
                return;
            }

            if (!contentType.includes("application/json")) {
                msg.textContent = `Expected JSON but got: ${contentType}`;
                return;
            }

            const patients = JSON.parse(text);
            renderRows(patients);

            msg.textContent = "";
        } catch (err) {
            console.error("Fetch crashed:", err);
            msg.textContent = `Network/JS error: ${err.message}`;
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
          <td>${safe(p.dateOfBirth?.split("T")[0])}</td>
          <td>${safe(p.email)}</td>
          <td>${safe(p.phone)}</td>
          <td>
            <button onclick="goPrescriptions(${safe(p.id)})">
              Manage Prescriptions
            </button>
          </td>
        `;
            rows.appendChild(tr);
        });
    }

    window.goPrescriptions = function(patientId) {
        window.location.href = `/manage-prescriptions.html?patientId=${patientId}`;
    };

    function safe(v) {
        return v == null ? "" : String(v);
    }
})();

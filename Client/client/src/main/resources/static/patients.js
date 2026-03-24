(async () => {
    const res = await fetch("/api/auth/admins/me");
    if (!res.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const rows = document.getElementById("rows");
    const msg = document.getElementById("message");
    const qInput = document.getElementById("q");
    const searchBtn = document.getElementById("searchBtn");

    // Check if there's a search query in URL
    const params = new URLSearchParams(window.location.search);
    const searchQuery = params.get("q");

    if (searchQuery) {
        qInput.value = searchQuery;
        searchPatients(searchQuery);
    } else {
        loadAllPatients();
    }

    // Handle search button and enter key
    searchBtn.addEventListener("click", performSearch);
    qInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") performSearch();
    });

    function performSearch() {
        const q = qInput.value.trim();
        if (!q) {
            // If search is empty, reload all patients
            window.location.href = "/patients.html";
            return;
        }
        // Update URL and search
        window.history.pushState({}, "", `/patients.html?q=${encodeURIComponent(q)}`);
        searchPatients(q);
    }

    async function searchPatients(q) {
        msg.textContent = "Searching patients...";

        try {
            const res = await fetch(`/api/admin/patients/search?q=${encodeURIComponent(q)}`, {
                credentials: "include",
            });

            if (!res.ok) {
                const text = await res.text().catch(() => "");
                msg.textContent = `Failed to search patients. (${res.status}) ${text}`;
                return;
            }

            const patients = await res.json();
            renderRows(patients);

            msg.textContent = patients.length
                ? `Found ${patients.length} patient(s).`
                : "No patients found.";
        } catch (err) {
            console.error(err);
            msg.textContent = "Network error while searching patients.";
        }
    }

    async function loadAllPatients() {
        msg.textContent = "Loading patients...";

        try {
            const res = await fetch("/api/admin/patients");

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
          <td class="center">${safe(p.id)}</td>
          <td>${safe(p.firstName)} ${safe(p.lastName)}</td>
          <td>${safe(p.phone)}</td>
          <td class="center">${p.faceActive ? '<span class="checkmark">\u221A</span>' : ""}</td>
          <td class="center">${p.smsConsent ? '<span class="checkmark">\u221A</span>' : ""}</td>
          <td class="actions">
            <button onclick="goPatientDetail(${safe(p.id)})">
              View Details
            </button>
            <button onclick="goPrescriptions(${safe(p.id)})">
              Prescriptions
            </button>
            <button onclick="goIntakeHistory(${safe(p.id)})">
              Intake History
            </button>
          </td>
        `;
            rows.appendChild(tr);
        });
    }

    window.goPatientDetail = function(patientId) {
        window.location.href = `/patient-detail.html?patientId=${patientId}`;
    };

    window.goPrescriptions = function(patientId) {
        window.location.href = `/manage-prescriptions.html?patientId=${patientId}`;
    };

    window.goIntakeHistory = function(patientId) {
        window.location.href = `/intake-history.html?patientId=${patientId}`;
    };

    function safe(v) {
        if (v == null) return "";
        return String(v)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }
})();

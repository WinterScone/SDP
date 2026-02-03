(async () => {
    const authRes = await fetch("/api/verify/me");
    if (!authRes.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    let admins = [];

    window.linkAdmin = async function(patientId, adminId) {
        if (!adminId) {
            alert("Please select an admin");
            return;
        }

        try {
            const res = await fetch(`/api/admin/patients/${patientId}/link-admin`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ adminId: Number(adminId) })
            });

            if (!res.ok) throw new Error("Failed to link admin");

            console.log(`Linked patient ${patientId} to admin ${adminId}`);
        } catch (e) {
            alert("Error: " + e.message);
        }
    };

    async function loadAdmins() {
        try {
            const res = await fetch("/api/admin/admins");

            if (!res.ok) throw new Error("Failed to load admins");
            admins = await res.json();
        } catch (e) {
            console.error("Error loading admins:", e);
            admins = [];
        }
    }

    async function loadPatients() {
        const tbody = document.getElementById("rows");

        try {
            await loadAdmins();

            const res = await fetch("/api/admin/patients");
            if (!res.ok) throw new Error("Failed to load patients: " + res.status);

            const patients = await res.json();

            if (!patients.length) {
                tbody.innerHTML = `<tr><td colspan="8">No patients found.</td></tr>`;
                return;
            }

            tbody.innerHTML = patients.map(p => {
                const adminOptions = admins.map(a => {
                    const label = a.username ?? a.name ?? ("Admin #" + a.id);

                    return `<option value="${a.id}" ${a.id === p.linkedAdminId ? "selected" : ""}>${label}</option>`;
                }).join("");

                return `
                  <tr>
                    <td>${p.id ?? ""}</td>
                    <td>${p.firstName ?? ""}</td>
                    <td>${p.lastName ?? ""}</td>
                    <td>${p.dateOfBirth ?? ""}</td>
                    <td>${p.email ?? ""}</td>
                    <td>${p.phone ?? ""}</td>
                    <td>${p.createdAt ?? ""}</td>
                    <td>
                      <select onchange="linkAdmin(${p.id}, this.value)">
                        <option value="">-- Select Admin --</option>
                        ${adminOptions}
                      </select>
                    </td>
                  </tr>
                `;
            }).join("");

        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="8">${e.message}</td></tr>`;
        }
    }

    loadPatients();
})();

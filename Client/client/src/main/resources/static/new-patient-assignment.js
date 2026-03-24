function formatPhone(phone) {
    if (!phone) return "-";
    if (/^07\d{9}$/.test(phone)) return phone.slice(0, 5) + " " + phone.slice(5);
    return phone;
}

(async () => {
    const authRes = await fetch("/api/auth/admins/me");
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

            const res = await fetch("/api/admin/patients/assignments");
            if (!res.ok) throw new Error("Failed to load patients: " + res.status);

            const patients = await res.json();

            if (!patients.length) {
                tbody.innerHTML = `<tr><td colspan="7">No patients found.</td></tr>`;
                return;
            }

            tbody.innerHTML = patients.map(p => {
                const adminOptions = admins.map(a => {
                    const label = a.username ?? a.name ?? ("Admin #" + a.id);

                    return `<option value="${a.id}" ${a.id === p.linkedAdminId ? "selected" : ""}>${label}</option>`;
                }).join("");

                // Format date of birth
                const dob = p.dateOfBirth ? formatDate(p.dateOfBirth) : "";

                // Format created date to YYYY-MM-DD
                const created = p.createdAt ? formatDate(p.createdAt) : "";

                // Combine first and last name
                const fullName = [p.firstName, p.lastName].filter(n => n).join(" ");

                return `
                  <tr>
                    <td class="center">${p.id ?? ""}</td>
                    <td>${fullName}</td>
                    <td>${dob}</td>
                    <td>${p.email ?? ""}</td>
                    <td>${formatPhone(p.phone)}</td>
                    <td>${created}</td>
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
            tbody.innerHTML = `<tr><td colspan="7">${e.message}</td></tr>`;
        }
    }

    function formatDate(dateString) {
        if (!dateString) return "";
        // Extract just the date part (YYYY-MM-DD) from datetime strings
        return dateString.split("T")[0];
    }

    loadPatients();
})();

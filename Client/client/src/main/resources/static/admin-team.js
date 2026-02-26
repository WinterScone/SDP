(async () => {
    const res = await fetch("/api/verify/me");
    if (!res.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const rows = document.getElementById("rows");
    const msg = document.getElementById("message");

    loadAdmins();

    async function loadAdmins() {
        msg.textContent = "Loading admins...";

        try {
            const res = await fetch("/api/admin/admins");

            if (!res.ok) {
                const text = await res.text().catch(() => "");
                msg.textContent = `Failed to load admins. (${res.status}) ${text}`;
                return;
            }

            const admins = await res.json();
            renderRows(admins);

            msg.textContent = "";
        } catch (err) {
            console.error(err);
            msg.textContent = "Network error while loading admins.";
        }
    }

    function renderRows(admins) {
        rows.innerHTML = "";

        admins.forEach((a) => {
            const tr = document.createElement("tr");
            const fullName = `${safe(a.firstName)} ${safe(a.lastName)}`;
            const rootMark = a.root ? '<span class="checkmark">Yes</span>' : "";

            tr.innerHTML = `
          <td>${safe(fullName)}</td>
          <td>${safe(a.email)}</td>
          <td>${safe(a.phone)}</td>
          <td>${rootMark}</td>
        `;
            rows.appendChild(tr);
        });
    }

    function safe(v) {
        return v == null ? "" : String(v);
    }
})();

(async () => {
    const res = await fetch("/api/verify/me");
    if (!res.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const rowsEl = document.getElementById("rows");
    const messageEl = document.getElementById("message");

    function setMsg(text) {
        messageEl.textContent = text || "";
    }

    async function loadMedicines() {
        setMsg("Loading...");
        rowsEl.innerHTML = "";

        const res = await fetch("/api/medicines");
        if (!res.ok) {
            setMsg("Failed to load medicines.");
            return;
        }

        const meds = await res.json();
        setMsg("");

        meds.sort((a, b) => {
            const aId = String(a.medicineId ?? "");
            const bId = String(b.medicineId ?? "");

            const aNum = parseInt(aId.replace(/\D+/g, ""), 10) || 0;
            const bNum = parseInt(bId.replace(/\D+/g, ""), 10) || 0;

            return aNum - bNum;
        });

        for (const m of meds) {
            const tr = document.createElement("tr");

            const medicineId = m.medicineId ?? "";
            const qtyCellId = `qty-cell-${medicineId}`;
            const statusId = `status-${medicineId}`;

            tr.innerHTML = `
          <td>${m.medicineId ?? ""}</td>
          <td>${m.medicineName ?? ""}</td>
          <td>${m.shape ?? ""}</td>
          <td>${m.colour ?? ""}</td>
          <td>${m.dosagePerForm ?? ""}</td>
          <td id="${qtyCellId}">${m.quantity ?? 0}</td>
          <td>
            <input
                type="number"
                min="0"
                value="${m.quantity ?? 0}"
                data-id="${medicineId}"
            />
          </td>
        <td>
          <button data-save="${medicineId}">Save</button>
          <span id="${statusId}" style="margin-left:10px;"></span>
        </td>
        `;

            rowsEl.appendChild(tr);
        }

        rowsEl.querySelectorAll("button[data-save]").forEach(btn => {
            btn.addEventListener("click", () => saveQuantity(btn.getAttribute("data-save")));
        });
    }

    async function saveQuantity(medicineId) {
        const input = rowsEl.querySelector(`input[data-id="${medicineId}"]`);
        const qty = Number(input.value);

        if (!Number.isFinite(qty) || qty < 0) {
            setMsg("Quantity must be 0 or more.");
            return;
        }

        setMsg(`Saving ${medicineId}...`);

        const res = await fetch(`/api/medicines/${medicineId}/quantity`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ quantity: qty })
        });

        const qtyCell = document.getElementById(`qty-cell-${medicineId}`);
        const statusEl = document.getElementById(`status-${medicineId}`);

        if (res.ok) {
            if (qtyCell) qtyCell.textContent = String(qty);
            if (statusEl) {
                statusEl.textContent = "Saved";
                setTimeout(() => statusEl.textContent = "", 1200);
            }
            setMsg("");
        } else {
            const text = await res.text().catch(() => "");
            setMsg(`Save failed (${res.status}) ${text}`);
        }
    }

    loadMedicines().catch(err => {
        console.error(err);
        setMsg("Error loading medicines.");
    });
})();

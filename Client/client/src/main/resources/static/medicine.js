(async () => {
    const verifyRes = await fetch("/api/verify/me");

    if (!verifyRes.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const rowsEl = document.getElementById("rows");
    const messageEl = document.getElementById("message");

    function setMsg(text) {
        messageEl.textContent = text || "";
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
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
            const safeMedicineId = escapeHtml(medicineId);
            const qtyCellId = `qty-cell-${medicineId}`;
            const statusId = `status-${medicineId}`;

            tr.innerHTML = `
        <td>${escapeHtml(m.medicineId ?? "")}</td>
        <td>${escapeHtml(m.medicineName ?? "")}</td>
        <td>${escapeHtml(m.shape ?? "")}</td>
        <td>${escapeHtml(m.colour ?? "")}</td>
        <td>${escapeHtml(m.dosagePerForm ?? "")}</td>
        <td>
          <input
            type="text"
            value="${escapeHtml(m.instruction ?? "")}"
            data-instruction-id="${safeMedicineId}"
            placeholder="e.g. May cause drowsiness"
          />
        </td>
        <td id="${escapeHtml(qtyCellId)}">${escapeHtml(m.quantity ?? 0)}</td>
        <td>
          <input
            type="number"
            min="0"
            value="${escapeHtml(m.quantity ?? 0)}"
            data-id="${safeMedicineId}"
          />
        </td>
        <td>
          <button data-save="${safeMedicineId}">Save</button>
          <span id="${escapeHtml(statusId)}" style="margin-left:10px;"></span>
        </td>
      `;

            rowsEl.appendChild(tr);
        }

        rowsEl.querySelectorAll("button[data-save]").forEach(btn => {
            btn.addEventListener("click", () => saveMedicine(btn.getAttribute("data-save")));
        });
    }

    async function saveMedicine(medicineId) {
        const qtyInput = rowsEl.querySelector(`input[data-id="${medicineId}"]`);
        const instructionInput = rowsEl.querySelector(`input[data-instruction-id="${medicineId}"]`);

        const qty = Number(qtyInput.value);
        const instruction = instructionInput?.value ?? "";

        if (!Number.isFinite(qty) || qty < 0) {
            setMsg("Quantity must be 0 or more.");
            return;
        }

        setMsg(`Saving ${medicineId}...`);

        const res = await fetch(`/api/medicines/${medicineId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                quantity: qty,
                instruction: instruction
            })
        });

        const qtyCell = document.getElementById(`qty-cell-${medicineId}`);
        const statusEl = document.getElementById(`status-${medicineId}`);

        if (res.ok) {
            if (qtyCell) qtyCell.textContent = String(qty);

            if (statusEl) {
                statusEl.textContent = "Saved";
                setTimeout(() => {
                    statusEl.textContent = "";
                }, 1200);
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
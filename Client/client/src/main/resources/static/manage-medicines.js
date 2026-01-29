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

    // Optional: sort by id name
    meds.sort((a, b) => String(a.medicineId).localeCompare(String(b.medicineId)));

    for (const m of meds) {
        const tr = document.createElement("tr");

        tr.innerHTML = `
      <td>${m.medicineId ?? ""}</td>
      <td>${m.medicineName ?? ""}</td>
      <td>${m.shape ?? ""}</td>
      <td>${m.colour ?? ""}</td>
      <td>${m.dosagePerForm ?? ""}</td>
      <td>
        <input type="number" min="0" value="${m.quantity ?? 0}" data-id="${m.medicineId}">
      </td>
      <td>
        <button data-save="${m.medicineId}">Save</button>
      </td>
    `;

        rowsEl.appendChild(tr);
    }

    // attach save handlers
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

    if (res.ok) {
        setMsg(`Saved ${medicineId}`);
        setTimeout(() => setMsg(""), 1000);
    } else {
        const text = await res.text().catch(() => "");
        setMsg(`Save failed (${res.status}) ${text}`);
    }
}

loadMedicines().catch(err => {
    console.error(err);
    setMsg("Error loading medicines.");
});

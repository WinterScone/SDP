const contentEl = document.getElementById("content");
const faceImg = document.getElementById("patientFace");
const messageEl = document.getElementById("message");

function setMessage(msg) {
    messageEl.textContent = msg || "";
}

function escapeHtml(str) {
    return String(str ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function getPatientId() {
    const params = new URLSearchParams(window.location.search);
    return params.get("patientId");
}

async function loadPatient() {
    const patientId = getPatientId();

    if (!patientId) {
        setMessage("Missing patient ID.");
        return;
    }

    setMessage("Loading patient details...");

    try {
        const res = await fetch(`/api/patient/${patientId}`, {
            credentials: "include"
        });

        const text = await res.text();
        let data = null;

        try {
            data = JSON.parse(text);
        } catch (_) {
            data = null;
        }

        if (!res.ok) {
            setMessage(`Failed to load patient details. (${res.status}) ${text}`);
            return;
        }

        if (!data) {
            setMessage("Failed to load patient details. Response was not JSON.");
            return;
        }

        contentEl.innerHTML = `
            <p><strong>ID:</strong> ${escapeHtml(data.id)}</p>
            <p><strong>Username:</strong> ${escapeHtml(data.username)}</p>
            <p><strong>First name:</strong> ${escapeHtml(data.firstName)}</p>
            <p><strong>Last name:</strong> ${escapeHtml(data.lastName)}</p>
            <p><strong>Date of birth:</strong> ${escapeHtml(data.dateOfBirth)}</p>
            <p><strong>Email:</strong> ${escapeHtml(data.email || "-")}</p>
            <p><strong>Phone:</strong> ${escapeHtml(data.phone || "-")}</p>
            <p><strong>Linked admin:</strong> ${escapeHtml(data.linkedAdminName || "-")}</p>
        `;

        faceImg.src = `/api/patient-face/${patientId}/image`;
        faceImg.onerror = () => {
            faceImg.alt = "No face image";
            setMessage("Patient details loaded. No face image found.");
        };

        setMessage("");
    } catch (e) {
        console.error(e);
        setMessage(`Network error while loading patient details: ${e.message}`);
    }
}

loadPatient();
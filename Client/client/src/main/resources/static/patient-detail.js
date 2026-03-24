const contentEl = document.getElementById("content");
const faceImg = document.getElementById("patientFace");
const messageEl = document.getElementById("message");

let currentPatientData = null;

function getCookie(name) {
    for (const part of document.cookie.split(";")) {
        const [k, v] = part.trim().split("=");
        if (k === name) return v;
    }
    return null;
}

function isRootAdmin() {
    return getCookie("adminRoot") === "true";
}

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
        const res = await fetch(`/api/admin/patients/${patientId}`, {
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

        currentPatientData = data;

        contentEl.innerHTML = `
            <p><strong>ID:</strong> ${escapeHtml(data.id)}</p>
            <p><strong>First name:</strong> ${escapeHtml(data.firstName)}</p>
            <p><strong>Last name:</strong> ${escapeHtml(data.lastName)}</p>
            <p><strong>Date of birth:</strong> ${escapeHtml(data.dateOfBirth)}</p>
            <p><strong>Email:</strong> ${escapeHtml(data.email || "-")}</p>
            <p><strong>Phone:</strong> ${escapeHtml(data.phone || "-")}</p>
            <p><strong>Face Recognition:</strong> ${data.faceActive ? "✓ Enrolled" : "✗ Not enrolled"}</p>
            <p><strong>SMS Consent:</strong> ${data.smsConsent ? "✓ Yes" : "✗ No"}</p>
            <button class="btn" style="margin-top:12px" onclick="showEditForm()">Edit Details</button>
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

function showEditForm() {
    const data = currentPatientData;
    if (!data) return;
    document.getElementById("editSection").style.display = "block";
    document.getElementById("editFirstName").value = data.firstName || "";
    document.getElementById("editLastName").value = data.lastName || "";
    document.getElementById("editDob").value = data.dateOfBirth || "";
    document.getElementById("editEmail").value = data.email || "";
    document.getElementById("editPhone").value = data.phone || "";
    document.getElementById("editSmsConsent").checked = data.smsConsent || false;
    document.getElementById("editFaceConsent").checked = data.faceRecognitionConsent || false;
}

async function submitEdit() {
    const patientId = getPatientId();
    const payload = {
        firstName: document.getElementById("editFirstName").value.trim(),
        lastName: document.getElementById("editLastName").value.trim(),
        dateOfBirth: document.getElementById("editDob").value,
        email: document.getElementById("editEmail").value.trim(),
        phone: document.getElementById("editPhone").value.trim(),
        smsConsent: document.getElementById("editSmsConsent").checked,
        faceRecognitionConsent: document.getElementById("editFaceConsent").checked,
    };

    try {
        const res = await fetch(`/api/admin/patients/${patientId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify(payload),
        });

        if (res.ok) {
            setMessage("Patient details updated successfully.");
            document.getElementById("editSection").style.display = "none";
            loadPatient();
        } else {
            const data = await res.json().catch(() => null);
            setMessage(`Failed to update: ${data?.error || res.status}`);
        }
    } catch (e) {
        setMessage("Network error: " + e.message);
    }
}

loadPatient();

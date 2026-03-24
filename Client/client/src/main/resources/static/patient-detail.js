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

function formatPhone(phone) {
    if (!phone) return "-";
    if (/^07\d{9}$/.test(phone)) return phone.slice(0, 5) + " " + phone.slice(5);
    return phone;
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

        document.getElementById("patientName").textContent = data.firstName + " " + data.lastName;
        document.getElementById("patientMeta").textContent = "ID: " + data.id;

        contentEl.innerHTML = `
<div class="table-wrapper">
  <table>
    <tbody>
      <tr><td><strong>Date of birth</strong></td><td>${escapeHtml(data.dateOfBirth || "-")}</td></tr>
      <tr><td><strong>Email</strong></td><td>${escapeHtml(data.email || "-")}</td></tr>
      <tr><td><strong>Phone</strong></td><td>${formatPhone(data.phone)}</td></tr>
      <tr><td><strong>Face Recognition</strong></td><td>${data.faceActive ? "✓ Enrolled" : "✗ Not enrolled"}</td></tr>
      <tr><td><strong>SMS Consent</strong></td><td>${data.smsConsent ? "✓ Yes" : "✗ No"}</td></tr>
    </tbody>
  </table>
</div>`;

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

function cancelEdit() {
    document.getElementById("editSection").style.display = "none";
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

    // -- Date of birth: at least 16 years old (if provided) --
    if (payload.dateOfBirth) {
        const dobDate = new Date(payload.dateOfBirth + "T00:00:00");
        const today = new Date();
        const minDob = new Date(today.getFullYear() - 16, today.getMonth(), today.getDate());
        if (dobDate > minDob) {
            setMessage("Patient must be at least 16 years old.");
            return;
        }
    }

    // -- Email format (if provided) --
    if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
        setMessage("Please enter a valid email address.");
        return;
    }

    // -- Phone: UK format (if provided) --
    if (payload.phone) {
        const phoneClean = payload.phone.replace(/\s+/g, "");
        if (!/^0\d{10}$/.test(phoneClean)) {
            setMessage("Phone must be a valid UK number (11 digits starting with 0).");
            return;
        }
    }

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

// --- Face Recapture ---
const recaptureBtn = document.getElementById("recaptureBtn");
const recaptureSection = document.getElementById("faceRecaptureSection");
const faceVideo = document.getElementById("faceVideo");
const faceCanvas = document.getElementById("faceCanvas");
const startCameraBtn = document.getElementById("startCameraBtn");
const captureFaceBtn = document.getElementById("captureFaceBtn");
const saveFaceBtn = document.getElementById("saveFaceBtn");
const cancelRecaptureBtn = document.getElementById("cancelRecaptureBtn");
const faceStatusEl = document.getElementById("faceStatus");

let cameraStream = null;
let capturedBlob = null;

function setFaceStatus(text, ok = false) {
    faceStatusEl.textContent = text;
    faceStatusEl.style.color = ok ? "green" : "crimson";
}

function stopCamera() {
    if (cameraStream) {
        cameraStream.getTracks().forEach(t => t.stop());
        cameraStream = null;
    }
}

function resetRecaptureUI() {
    stopCamera();
    capturedBlob = null;
    faceVideo.style.display = "none";
    captureFaceBtn.style.display = "none";
    saveFaceBtn.style.display = "none";
    startCameraBtn.style.display = "inline-block";
    startCameraBtn.textContent = "Start Camera";
    recaptureSection.style.display = "none";
    recaptureBtn.style.display = "inline-block";
    setFaceStatus("");
}

recaptureBtn.addEventListener("click", () => {
    recaptureBtn.style.display = "none";
    recaptureSection.style.display = "block";
    setFaceStatus("");
});

cancelRecaptureBtn.addEventListener("click", () => {
    resetRecaptureUI();
});

startCameraBtn.addEventListener("click", async () => {
    try {
        cameraStream = await navigator.mediaDevices.getUserMedia({ video: { width: { ideal: 640 }, height: { ideal: 480 } } });
        faceVideo.srcObject = cameraStream;
        faceVideo.style.display = "block";
        startCameraBtn.style.display = "none";
        captureFaceBtn.style.display = "inline-block";
        saveFaceBtn.style.display = "none";
        setFaceStatus("Camera active — position face and click Capture.", true);
    } catch (err) {
        setFaceStatus("Could not access camera: " + err.message);
    }
});

captureFaceBtn.addEventListener("click", () => {
    const ctx = faceCanvas.getContext("2d");
    const vw = faceVideo.videoWidth;
    const vh = faceVideo.videoHeight;
    const side = Math.min(vw, vh);
    const sx = (vw - side) / 2;
    const sy = (vh - side) / 2;
    ctx.drawImage(faceVideo, sx, sy, side, side, 0, 0, faceCanvas.width, faceCanvas.height);
    faceCanvas.toBlob(blob => {
        capturedBlob = blob;
        setFaceStatus("Face captured! Click Save to enroll, or Retake.", true);
        stopCamera();
        faceVideo.style.display = "none";
        captureFaceBtn.style.display = "none";
        saveFaceBtn.style.display = "inline-block";
        startCameraBtn.style.display = "inline-block";
        startCameraBtn.textContent = "Retake";
    }, "image/jpeg", 0.85);
});

saveFaceBtn.addEventListener("click", async () => {
    if (!capturedBlob) return;
    const patientId = getPatientId();
    setFaceStatus("Enrolling face...");
    try {
        const formData = new FormData();
        formData.append("image", capturedBlob, "face.jpg");
        const res = await fetch(`/api/patient-face/${patientId}/enroll`, {
            method: "POST",
            credentials: "include",
            body: formData,
        });
        if (res.ok) {
            setFaceStatus("Face enrolled successfully!", true);
            resetRecaptureUI();
            loadPatient();
        } else {
            const data = await res.json().catch(() => null);
            setFaceStatus("Enrollment failed: " + (data?.error || res.status));
        }
    } catch (err) {
        setFaceStatus("Network error: " + err.message);
    }
});

loadPatient();

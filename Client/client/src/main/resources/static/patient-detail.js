const contentEl = document.getElementById("content");
const faceImg = document.getElementById("patientFace");
const messageEl = document.getElementById("message");

const faceCaptureSection = document.getElementById("faceCaptureSection");
const faceVideo = document.getElementById("faceVideo");
const faceCanvas = document.getElementById("faceCanvas");
const startCameraBtn = document.getElementById("startCameraBtn");
const captureFaceBtn = document.getElementById("captureFaceBtn");
const faceStatusEl = document.getElementById("faceStatus");
const editFaceConsent = document.getElementById("editFaceConsent");

let currentPatientData = null;
let cameraStream = null;
let capturedBlob = null;

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
    const ukMatch = phone.match(/^\+44(\d{4})(\d{6})$/);
    if (ukMatch) return "+44 " + ukMatch[1] + " " + ukMatch[2];
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
        document.getElementById("patientMeta").textContent = "";

        contentEl.innerHTML = `
<div class="table-wrapper">
  <table>
    <tbody>
      <tr><td><strong>ID</strong></td><td>${escapeHtml(data.id)}</td></tr>
      <tr><td><strong>Date of birth</strong></td><td>${escapeHtml(data.dateOfBirth || "-")}</td></tr>
      <tr><td><strong>Email</strong></td><td>${escapeHtml(data.email || "-")}</td></tr>
      <tr><td><strong>Phone</strong></td><td>${formatPhone(data.phone)}</td></tr>
      <tr><td><strong>Face Recognition</strong></td><td>${data.faceRecognitionConsent ? '<span style="color:green">√</span>' : ""}</td></tr>
      <tr><td><strong>SMS Consent</strong></td><td>${data.smsConsent ? '<span style="color:green">√</span>' : ""}</td></tr>
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
    stopCamera();
    capturedBlob = null;
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

    // Split stored E.164 back into country code select + local input
    const phone = data.phone || "";
    const knownCodes = [
        "+1684", "+1264", "+1268", "+1242", "+1246", "+1441", "+1284", "+1345",
        "+1473", "+1671", "+1767", "+1809", "+1876", "+1670", "+1787", "+1869",
        "+1758", "+1784", "+1721", "+1868", "+1649", "+1340", "+1664",
        "+993", "+994", "+998", "+996", "+995", "+992", "+977", "+976",
        "+975", "+974", "+973", "+972", "+971", "+970", "+968", "+966",
        "+965", "+964", "+963", "+962", "+960", "+886", "+880", "+856",
        "+855", "+853", "+852", "+690", "+689", "+688", "+687", "+686",
        "+685", "+682", "+681", "+679", "+678", "+677", "+676", "+675",
        "+674", "+673", "+670", "+599", "+598", "+597", "+595", "+594",
        "+593", "+592", "+591", "+590", "+509", "+508", "+507", "+506",
        "+505", "+504", "+503", "+502", "+501", "+500", "+423", "+421",
        "+420", "+389", "+387", "+386", "+385", "+383", "+382", "+381",
        "+380", "+378", "+377", "+376", "+375", "+374", "+373", "+372",
        "+371", "+370", "+359", "+358", "+357", "+356", "+355", "+354",
        "+353", "+352", "+351", "+350", "+299", "+298", "+297", "+291",
        "+290", "+269", "+268", "+267", "+266", "+265", "+264", "+263",
        "+262", "+261", "+260", "+258", "+257", "+256", "+255", "+254",
        "+253", "+252", "+251", "+250", "+249", "+248", "+245", "+244",
        "+243", "+242", "+241", "+240", "+239", "+238", "+237", "+236",
        "+235", "+234", "+233", "+232", "+231", "+230", "+229", "+228",
        "+227", "+226", "+225", "+224", "+223", "+222", "+221", "+220",
        "+218", "+216", "+213", "+212", "+211",
        "+98", "+95", "+94", "+93", "+92", "+91", "+90",
        "+86", "+84", "+82", "+81",
        "+66", "+65", "+64", "+63", "+62", "+61", "+60",
        "+58", "+57", "+56", "+55", "+54", "+53", "+52", "+51",
        "+49", "+48", "+47", "+46", "+45", "+44", "+43", "+41", "+40",
        "+39", "+36", "+34", "+33", "+32", "+31", "+30", "+27", "+20",
        "+7", "+1"
    ];
    let cc = "+44", local = phone;
    if (phone.startsWith("+")) {
        for (const code of knownCodes) {
            if (phone.startsWith(code)) { cc = code; local = phone.slice(code.length); break; }
        }
    }
    document.getElementById("editPhoneCountryCode").value = cc;
    document.getElementById("editPhone").value = local;

    document.getElementById("editSmsConsent").checked = data.smsConsent || false;
    editFaceConsent.checked = data.faceRecognitionConsent || false;

    // Reset face capture state
    stopCamera();
    capturedBlob = null;
    faceVideo.style.display = "none";
    captureFaceBtn.style.display = "none";
    startCameraBtn.style.display = "inline-block";
    startCameraBtn.textContent = "Start Camera";
    setFaceStatus("");
    faceCaptureSection.style.display = editFaceConsent.checked ? "block" : "none";
}

function buildE164(cc, localNumber) {
    let digits = localNumber.replace(/\s+/g, "").replace(/^0+/, "");
    if (!digits) return "";
    return cc + digits;
}

async function submitEdit() {
    const patientId = getPatientId();
    const editCountryCode = document.getElementById("editPhoneCountryCode").value;
    const editLocalPhone = document.getElementById("editPhone").value;
    const payload = {
        firstName: document.getElementById("editFirstName").value.trim(),
        lastName: document.getElementById("editLastName").value.trim(),
        dateOfBirth: document.getElementById("editDob").value,
        email: document.getElementById("editEmail").value.trim(),
        phone: buildE164(editCountryCode, editLocalPhone) || null,
        smsConsent: document.getElementById("editSmsConsent").checked,
        faceRecognitionConsent: editFaceConsent.checked,
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

    // -- Phone: E.164 format (if provided) --
    if (payload.phone && !/^\+\d{7,15}$/.test(payload.phone)) {
        setMessage("Please enter a valid phone number.");
        return;
    }

    try {
        const res = await fetch(`/api/admin/patients/${patientId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify(payload),
        });

        if (res.ok) {
            // If face was captured, enroll it (two-step pattern like signup)
            if (capturedBlob) {
                setMessage("Patient details updated. Enrolling face...");
                try {
                    const formData = new FormData();
                    formData.append("image", capturedBlob, "face.jpg");
                    const enrollRes = await fetch(`/api/patient-face/${patientId}/enroll`, {
                        method: "POST",
                        credentials: "include",
                        body: formData,
                    });
                    if (enrollRes.ok) {
                        setMessage("Patient details updated and face enrolled successfully.");
                    } else {
                        const enrollData = await enrollRes.json().catch(() => null);
                        setMessage("Details updated but face enrollment failed: " + (enrollData?.error || enrollRes.status));
                    }
                } catch (err) {
                    setMessage("Details updated but face enrollment failed: " + err.message);
                }
            } else {
                setMessage("Patient details updated successfully.");
            }
            stopCamera();
            capturedBlob = null;
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

// --- Face capture helpers ---
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

// Toggle face capture section based on consent checkbox
editFaceConsent.addEventListener("change", () => {
    faceCaptureSection.style.display = editFaceConsent.checked ? "block" : "none";
    if (!editFaceConsent.checked) {
        stopCamera();
        capturedBlob = null;
        faceVideo.style.display = "none";
        captureFaceBtn.style.display = "none";
        startCameraBtn.style.display = "inline-block";
        startCameraBtn.textContent = "Start Camera";
        setFaceStatus("");
    }
});

startCameraBtn.addEventListener("click", async () => {
    try {
        cameraStream = await navigator.mediaDevices.getUserMedia({ video: { width: { ideal: 640 }, height: { ideal: 480 } } });
        faceVideo.srcObject = cameraStream;
        faceVideo.style.display = "block";
        startCameraBtn.style.display = "none";
        captureFaceBtn.style.display = "inline-block";
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
        startCameraBtn.style.display = "inline-block";
        startCameraBtn.textContent = "Retake";
    }, "image/jpeg", 0.85);
});

loadPatient();

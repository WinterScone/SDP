const form = document.getElementById("signupForm");
const messageEl = document.getElementById("message");
const faceConsent = document.getElementById("faceConsent");
const faceSection = document.getElementById("faceSection");
const video = document.getElementById("faceVideo");
const canvas = document.getElementById("faceCanvas");
const startCameraBtn = document.getElementById("startCameraBtn");
const captureFaceBtn = document.getElementById("captureFaceBtn");
const faceStatus = document.getElementById("faceStatus");

let capturedBlob = null;
let cameraStream = null;

function setMessage(text, ok = false) {
    messageEl.textContent = text;
    messageEl.style.color = ok ? "green" : "crimson";
}

function setFaceStatus(text, ok = false) {
    faceStatus.textContent = text;
    faceStatus.style.color = ok ? "green" : "crimson";
}

function stopCamera() {
    if (cameraStream) {
        cameraStream.getTracks().forEach(t => t.stop());
        cameraStream = null;
    }
}

// Toggle face section visibility based on consent checkbox
faceConsent.addEventListener("change", () => {
    faceSection.style.display = faceConsent.checked ? "block" : "none";
    if (!faceConsent.checked) {
        stopCamera();
        capturedBlob = null;
        video.style.display = "none";
        captureFaceBtn.style.display = "none";
        startCameraBtn.style.display = "inline-block";
        setFaceStatus("");
    }
});

startCameraBtn.addEventListener("click", async () => {
    try {
        cameraStream = await navigator.mediaDevices.getUserMedia({ video: true });
        video.srcObject = cameraStream;
        video.style.display = "block";
        startCameraBtn.style.display = "none";
        captureFaceBtn.style.display = "inline-block";
        setFaceStatus("Camera active — position your face and click Capture.", true);
    } catch (err) {
        setFaceStatus("Could not access camera: " + err.message);
    }
});

captureFaceBtn.addEventListener("click", () => {
    const ctx = canvas.getContext("2d");
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(blob => {
        capturedBlob = blob;
        setFaceStatus("Face captured successfully!", true);
        stopCamera();
        video.style.display = "none";
        captureFaceBtn.style.display = "none";
        startCameraBtn.style.display = "inline-block";
        startCameraBtn.textContent = "Retake";
    }, "image/jpeg", 0.85);
});

form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        username: document.getElementById("username").value.trim(),
        password: document.getElementById("password").value,
        firstName: document.getElementById("firstName").value.trim(),
        lastName: document.getElementById("lastName").value.trim(),
        dateOfBirth: document.getElementById("dateOfBirth").value,
        email: document.getElementById("email").value.trim(),
        phone: document.getElementById("phone").value.trim(),
        faceRecognitionConsent: faceConsent.checked,
    };

    if (!payload.email) payload.email = null;
    if (!payload.phone) payload.phone = null;

    if (!payload.username || !payload.password || !payload.firstName || !payload.lastName || !payload.dateOfBirth) {
        setMessage("Please fill in all required fields.");
        return;
    }

    try {
        const res = await fetch("/api/auth/patients/signup", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const data = await res.json().catch(() => null);

        if (!res.ok) {
            setMessage((data && data.error) ? data.error : "Sign up failed.");
            return;
        }

        // If face was captured, enroll it
        if (capturedBlob && data && data.id) {
            setMessage("Account created! Enrolling face...", true);
            try {
                const formData = new FormData();
                formData.append("image", capturedBlob, "face.jpg");

                const enrollRes = await fetch("/api/patient-face/" + data.id + "/enroll", {
                    method: "POST",
                    body: formData,
                });

                if (enrollRes.ok) {
                    setMessage("Account created and face enrolled! Redirecting...", true);
                } else {
                    setMessage("Account created but face enrollment failed. You can enroll later.", true);
                }
            } catch (err) {
                setMessage("Account created but face enrollment failed. You can enroll later.", true);
            }
        } else {
            setMessage("Account created! You can now log in.", true);
        }

        stopCamera();
        setTimeout(() => (window.location.href = "/patient-login.html"), 1500);

    } catch (err) {
        setMessage("Network error. Please try again.");
    }
});

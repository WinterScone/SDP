const form = document.getElementById("signupForm");
const messageEl = document.getElementById("message");

const camera = document.getElementById("camera");
const canvas = document.getElementById("snapshot");
const startCameraBtn = document.getElementById("startCameraBtn");
const captureFaceBtn = document.getElementById("captureFaceBtn");
const faceStatus = document.getElementById("faceStatus");

let stream = null;
let capturedFaceBlob = null;

function setMessage(text, ok = false) {
    messageEl.textContent = text;
    messageEl.style.color = ok ? "green" : "crimson";
}

function setFaceStatus(text, ok = false) {
    faceStatus.textContent = text;
    faceStatus.style.color = ok ? "green" : "crimson";
}

startCameraBtn.addEventListener("click", async () => {
    try {
        stream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: false
        });
        camera.srcObject = stream;
        setFaceStatus("Camera started. Please position your face and capture.", true);
    } catch (err) {
        setFaceStatus("Could not access camera.");
    }
});

captureFaceBtn.addEventListener("click", () => {
    if (!camera.srcObject) {
        setFaceStatus("Please start the camera first.");
        return;
    }

    const ctx = canvas.getContext("2d");
    ctx.drawImage(camera, 0, 0, canvas.width, canvas.height);

    canvas.toBlob((blob) => {
        if (!blob) {
            setFaceStatus("Failed to capture image.");
            return;
        }
        capturedFaceBlob = blob;
        setFaceStatus("Face captured successfully.", true);
    }, "image/jpeg", 0.95);
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
    };

    if (!payload.email) payload.email = null;
    if (!payload.phone) payload.phone = null;

    if (!payload.username || !payload.password || !payload.firstName || !payload.lastName || !payload.dateOfBirth) {
        setMessage("Please fill in all required fields.");
        return;
    }

    if (!capturedFaceBlob) {
        setMessage("Please capture the patient's face before signing up.");
        return;
    }

    try {
        setMessage("Creating account...");

        const signupRes = await fetch("/api/patient/signup", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const signupData = await signupRes.json().catch(() => null);

        if (!signupRes.ok || !signupData || !signupData.ok) {
            setMessage((signupData && signupData.error) ? signupData.error : "Sign up failed.");
            return;
        }

        const patientId = signupData.patientId;
        if (!patientId) {
            setMessage("Signup succeeded, but patient ID was missing.");
            return;
        }

        setMessage("Account created. Uploading face...");

        const formData = new FormData();
        formData.append("image", capturedFaceBlob, "face.jpg");

        const faceRes = await fetch(`/api/patient-face/${patientId}/enroll`, {
            method: "POST",
            body: formData
        });

        const faceData = await faceRes.json().catch(() => null);

        if (!faceRes.ok || !faceData || !faceData.ok) {
            setMessage((faceData && faceData.error)
                ? `Account created, but face enrollment failed: ${faceData.error}`
                : "Account created, but face enrollment failed.");
            return;
        }

        setMessage("Account and face enrolled successfully. You can now log in.", true);

        if (stream) {
            stream.getTracks().forEach(track => track.stop());
        }

        setTimeout(() => {
            window.location.href = "/patient-login.html";
        }, 1000);

    } catch (err) {
        setMessage("Network error. Please try again.");
    }
});
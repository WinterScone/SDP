const form = document.getElementById("signupForm");
const messageEl = document.getElementById("message");

function setMessage(text, ok = false) {
    messageEl.textContent = text;
    messageEl.style.color = ok ? "green" : "crimson";
}

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
        faceRecognitionConsent: document.getElementById("faceConsent").checked,
    };

    if (!payload.email) payload.email = null;
    if (!payload.phone) payload.phone = null;

    if (!payload.username || !payload.password || !payload.firstName || !payload.lastName || !payload.dateOfBirth) {
        setMessage("Please fill in all required fields.");
        return;
    }

    try {
        const res = await fetch("/api/patient/signup", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const data = await res.json().catch(() => null);

        if (!res.ok) {
            setMessage((data && data.error) ? data.error : "Sign up failed.");
            return;
        }

        setMessage("Account created! You can now log in.", true);
        // Redirect after a moment (optional)
        setTimeout(() => (window.location.href = "/patient-login.html"), 800);

    } catch (err) {
        setMessage("Network error. Please try again.");
    }
});

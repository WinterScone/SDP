const form = document.getElementById("loginForm");
const messageEl = document.getElementById("message");

function setMessage(text, ok = false) {
    messageEl.textContent = text;
    messageEl.style.color = ok ? "green" : "crimson";
}

form.addEventListener("submit", async (e) => {
    e.preventDefault();
    setMessage("");

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
        setMessage("Please enter username and password.");
        return;
    }

    try {
        const res = await fetch("/api/patient/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password }),
        });

        const data = await res.json().catch(() => null);

        if (!res.ok || !data || data.ok !== true) {
            setMessage("Invalid username or password.");
            return;
        }

        // store patientId for next page
        localStorage.setItem("patientId", data.patientId);
        localStorage.setItem("patientUsername", data.username);

        setMessage("Login successful!", true);

        window.location.href = "/patient-dashboard.html";
    } catch (err) {
        setMessage("Network error. Please try again.");
    }
});

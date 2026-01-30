const form = document.getElementById("loginForm");
const btn = document.getElementById("loginBtn");
const message = document.getElementById("message");

form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
        message.textContent = "Please enter username and password.";
        return;
    }

    btn.disabled = true;
    message.textContent = "Checking...";

    try {
        const res = await fetch("/api/patient/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });

        if (res.ok) {
            message.textContent = "Login success";
            // window.location.href = "/patient-dashboard.html";
        } else if (res.status === 401) {
            message.textContent = "Invalid username or password";
        } else {
            message.textContent = `Server error (${res.status})`;
        }
    } catch (err) {
        console.error(err);
        message.textContent = "Cannot connect to server.";
    } finally {
        btn.disabled = false;
    }
});

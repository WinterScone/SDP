console.log("admin-login.js loaded");

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
        const res = await fetch("/api/auth/admins/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });

        if (res.ok) {
            const data = await res.json();
            console.log("Login success:", data);

            localStorage.setItem("adminUsername", data.username);
            message.textContent = "Login success, redirecting...";

            setTimeout(() => {
                window.location.href = "/dashboard.html";
            }, 800);

        } else if (res.status === 400 || res.status === 401) {
            const data = await res.json();
            message.textContent = data.message || "Invalid username or password";
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

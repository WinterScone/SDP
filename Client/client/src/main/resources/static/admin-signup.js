const form = document.getElementById("signupForm");
const msg = document.getElementById("su-message");

form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const username = document.getElementById("su-username").value.trim();
    const firstName = document.getElementById("su-firstname").value.trim();
    const lastName = document.getElementById("su-lastname").value.trim();
    const password = document.getElementById("su-password").value;

    if (!username || !firstName || !lastName || !password) {
        msg.textContent = "Please fill in all fields.";
        return;
    }

    msg.textContent = "Registering...";

    try {
        const res = await fetch("/api/verify/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, firstName, lastName })
        });

        const data = await res.json();

        if (res.ok) {
            msg.textContent = "Sign up successful! Redirecting...";
            setTimeout(() => {
                window.location.href = "/admin-login.html";
            }, 800);
        } else {
            msg.textContent = data.message || "Sign up failed";
        }
    } catch (err) {
        msg.textContent = "Server error";
        console.error(err);
    }
});

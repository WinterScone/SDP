(async () => {
    const authRes = await fetch("/api/auth/admins/me");
    if (!authRes.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    function getCookie(name) {
        for (const part of document.cookie.split(";")) {
            const [k, v] = part.trim().split("=");
            if (k === name) return v;
        }
        return null;
    }

    if (getCookie("adminRoot") !== "true") {
        window.location.href = "/dashboard.html";
        return;
    }

    const form = document.getElementById("signupForm");
    const msg = document.getElementById("su-message");

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const username = document.getElementById("su-username").value.trim();
        const firstName = document.getElementById("su-firstname").value.trim();
        const lastName = document.getElementById("su-lastname").value.trim();
        const email = document.getElementById("su-email").value.trim();
        const phone = document.getElementById("su-phone").value.trim();
        const password = document.getElementById("su-password").value;

        if (!username || !firstName || !lastName || !email || !phone || !password) {
            msg.textContent = "Please fill in all fields.";
            return;
        }

        // -- Email format --
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            msg.textContent = "Please enter a valid email address.";
            return;
        }

        // -- Phone: UK format --
        const phoneClean = phone.replace(/\s+/g, "");
        if (!/^0\d{10}$/.test(phoneClean)) {
            msg.textContent = "Phone must be a valid UK number (11 digits starting with 0).";
            return;
        }

        msg.textContent = "Registering...";

        try {
            const res = await fetch("/api/auth/admins/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password, firstName, lastName, email, phone })
            });

            const data = await res.json();

            if (res.ok) {
                msg.textContent = "Sign up successful! Redirecting...";
                setTimeout(() => {
                    window.location.href = "/dashboard.html";
                }, 800);
            } else {
                msg.textContent = data.message || "Sign up failed";
            }
        } catch (err) {
            msg.textContent = "Server error";
            console.error(err);
        }
    });
})();

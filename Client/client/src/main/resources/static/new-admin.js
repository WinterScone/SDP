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
        const password = document.getElementById("su-password").value;

        function buildE164(cc, localNumber) {
            let digits = localNumber.replace(/\s+/g, "").replace(/^0+/, "");
            if (!digits) return "";
            return cc + digits;
        }

        const countryCode = document.getElementById("su-phoneCountryCode").value;
        const localPhone = document.getElementById("su-phone").value;
        const phone = buildE164(countryCode, localPhone);

        if (!username || !firstName || !lastName || !email || !phone || !password) {
            msg.textContent = "Please fill in all fields.";
            return;
        }

        // -- Email format --
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            msg.textContent = "Please enter a valid email address.";
            return;
        }

        // -- Phone: E.164 format --
        if (phone && !/^\+\d{7,15}$/.test(phone)) {
            msg.textContent = "Please enter a valid phone number.";
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

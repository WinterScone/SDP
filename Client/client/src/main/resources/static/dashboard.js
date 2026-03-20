(async () => {
    const res = await fetch("/api/auth/admins/me");
    if (!res.ok) {
        window.location.href = "/admin-login.html";
        return;
    }

    const logoutBtn = document.getElementById("logoutBtn");
    const advancedBtn = document.getElementById("advancedBtn");

    logoutBtn.addEventListener("click", async () => {
        await fetch("/api/auth/admins/logout", { method: "POST" });
        window.location.href = "/admin-login.html";
    });

    function getCookie(name) {
        for (const part of document.cookie.split(";")) {
            const [k, v] = part.trim().split("=");
            if (k === name) return v;
        }
        return null;
    }

    if (getCookie("adminRoot") === "true") {
        advancedBtn.style.display = "block";
    }
})();

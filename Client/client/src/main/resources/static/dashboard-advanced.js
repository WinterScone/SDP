(async () => {
    const res = await fetch("/api/verify/me");
    if (!res.ok) {
        window.location.href = "/admin-login.html";
    }
})();

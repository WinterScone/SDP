(async () => {
    const res = await fetch("/api/auth/admins/me");
    if (!res.ok) {
        window.location.href = "/admin-login.html";
    }
})();

async function resetDatabase() {
    const confirmation = confirm(
        "WARNING: This will permanently delete all user-created admins and patients!\n\n" +
        "Only seed data will remain:\n" +
        "- Admins: root, testAdmin1, testAdmin2\n" +
        "- Patients: testPatient1, testPatient2\n\n" +
        "This action CANNOT be undone. Are you sure?"
    );

    if (!confirmation) return;

    const doubleCheck = confirm(
        "This is your final confirmation.\n\n" +
        "Click OK to RESET THE DATABASE now."
    );

    if (!doubleCheck) return;

    const messageDiv = document.getElementById("resetMessage");
    messageDiv.style.display = "block";
    messageDiv.className = "";
    messageDiv.textContent = "Resetting database...";

    try {
        const response = await fetch("/api/admin/reset-database", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            }
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: "Unknown error" }));
            throw new Error(errorData.message || `Server error: ${response.status}`);
        }

        const result = await response.json();
        messageDiv.className = "success";
        messageDiv.textContent = "Database reset successfully! " + (result.message || "");

        setTimeout(() => {
            window.location.reload();
        }, 2000);

    } catch (error) {
        console.error("Reset failed:", error);
        messageDiv.className = "error";
        messageDiv.textContent = "Reset failed: " + error.message;
    }
}

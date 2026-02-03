const logoutBtn = document.getElementById("logoutBtn");

logoutBtn.addEventListener("click", () => {
    localStorage.removeItem("adminUsername");
    window.location.href = "/admin-login.html";
});

if (!localStorage.getItem("adminUsername")) {
    window.location.href = "/admin-login.html";
}

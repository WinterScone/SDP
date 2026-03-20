const titleEl   = document.getElementById("title");
const logoutBtn = document.getElementById("logoutBtn");

const patientId = localStorage.getItem("patientId");
const username  = localStorage.getItem("patientUsername");

if (!patientId) {
    window.location.href = "/patient-login.html";
}

if (username) {
    titleEl.textContent = `Welcome, ${username}`;
}

logoutBtn.addEventListener("click", () => {
    localStorage.removeItem("patientId");
    localStorage.removeItem("patientUsername");
    window.location.href = "/patient-login.html";
});

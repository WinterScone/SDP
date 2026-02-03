document.getElementById("searchBtn").addEventListener("click", doSearch);
document.getElementById("q").addEventListener("keydown", (e) => {
    if (e.key === "Enter") doSearch();
});

function doSearch(){
    const q = document.getElementById("q").value.trim();
    if(!q) return;
    window.location.href = `/patient-results.html?q=${encodeURIComponent(q)}`;
}


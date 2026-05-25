let toastTimer = null;

function mostrarToast(id) {
    const toast = document.getElementById(id);
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => esconderToast(id), 5000);
}

function esconderToast(id) {
    document.getElementById(id).classList.remove('show');
}
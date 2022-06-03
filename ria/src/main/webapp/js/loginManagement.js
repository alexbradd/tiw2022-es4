function login(token) {
    storeInSession("token", token);
}

function logout() {
    dropFromSession("token")
}

function isLoggedIn() {
    return window.sessionStorage.getItem("token") != null;
}

function storeInSession(name, val) {
    window.sessionStorage.setItem(name, val);
}

function dropFromSession(name) {
    window.sessionStorage.removeItem(name);
}

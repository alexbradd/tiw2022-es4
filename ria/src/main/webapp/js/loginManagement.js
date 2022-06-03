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

function LogoutButtonManager(logoutButton, postLogoutCallback) {
    this._callback = postLogoutCallback;
    this._logoutButton = logoutButton;

    this.addListeners = function () {
        this._logoutButton.addEventListener("click", () => {
            new Ajax().post(
                "/api/auth/logout",
                null,
                (req) => {
                    if (req.readyState !== XMLHttpRequest.DONE)
                        return;
                    if (req.status === 200) {
                        logout();
                        this._callback();
                    } else
                        console.log(req.responseText);
                }
            )
        })
    }
}


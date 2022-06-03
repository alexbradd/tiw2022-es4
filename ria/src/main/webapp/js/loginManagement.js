function login(token, user) {
    window.sessionStorage.setItem("token", token);
    window.sessionStorage.setItem("user", JSON.stringify(user));
}

function logout() {
    window.sessionStorage.removeItem("token")
    window.sessionStorage.removeItem("user");
}

function isLoggedIn() {
    return window.sessionStorage.getItem("token") != null &&
        window.sessionStorage.getItem("user") != null;
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


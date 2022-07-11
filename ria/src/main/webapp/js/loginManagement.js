function login(token, user) {
    updateToken(token);
    window.sessionStorage.setItem("user", JSON.stringify(user));
}

function updateToken(token) {
    window.sessionStorage.setItem("token", token);
}

function logout() {
    window.sessionStorage.removeItem("token")
    window.sessionStorage.removeItem("user");
}

function isLoggedIn() {
    return window.sessionStorage.getItem("token") != null &&
        window.sessionStorage.getItem("user") != null;
}

function getUser() {
    if (!isLoggedIn())
        throw new Error("cannot get user if not logged in");
    return JSON.parse(window.sessionStorage.getItem("user"));
}

function getToken() {
    if (!isLoggedIn())
        throw new Error("cannot get token if not logged in");
    return window.sessionStorage.getItem("token");
}

function LoginManager(afterLogin, onError) {
    this._okCb = afterLogin;
    this._failCb = onError;

    this.login = function (loginFormData) {
        new Ajax().post(
            "/api/auth/login",
            convertFormDataToJSON(loginFormData),
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                if (req.status === 200) {
                    let res = JSON.parse(req.responseText);
                    login(res.token, res.user);
                    this._okCb();
                } else {
                    this._failCb(req.status, req.responseText);
                }
            }
        );
    }
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


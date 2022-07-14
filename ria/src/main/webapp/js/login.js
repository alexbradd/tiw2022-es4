const VIEWS = {
    LOGIN: "login",
    REGISTER: "register"
};

function ViewManager(pageContainer,
                     loginView,
                     loginViewComponents,
                     registerView,
                     registerViewComponents,
                     switcherElem) {
    this.currentState = VIEWS.LOGIN;
    this._pageContainer = pageContainer;
    this._loginView = loginView;
    this._loginViewComponents = loginViewComponents;
    this._registerView = registerView;
    this._registerViewComponents = registerViewComponents;
    this._switcher = {
        elem: switcherElem,
        flavourText: null,
        href: null
    };
    this._switcher.flavourText = this._switcher.elem.firstElementChild;
    this._switcher.href = this._switcher.elem.lastElementChild;

    this.init = function () {
        this.addListeners();
        this.hideRegister();
        this.hideLogin();
        this._removeHiddenStyleClass();
    }

    this.addListeners = function () {
        this._switcher.href.addEventListener("click", (e) => {
            this.switchViews();
            e.preventDefault();
        });
    }

    this._removeHiddenStyleClass = function () {
        this._loginView.classList.remove("js");
        this._registerView.classList.remove("js");
    }

    this.switchViews = function () {
        switch (this.currentState) {
            case VIEWS.LOGIN:
                this.hideLogin();
                this.displayRegister();
                this.currentState = VIEWS.REGISTER;
                break;
            case VIEWS.REGISTER:
                this.hideRegister();
                this.displayLogin();
                this.currentState = VIEWS.LOGIN;
                break;
            default:
                throw new Error("Unknown view " + this.currentState);
        }
    }

    this.displayRegister = function () {
        this._pageContainer.insertBefore(this._registerView, this._switcher.elem)
        this._switcher.flavourText.childNodes[0].textContent = "Already have an account? "
        this._switcher.href.childNodes[0].textContent = "Login"
    }

    this.hideRegister = function () {
        this.getRegisterForm().reset();
        this._pageContainer.removeChild(this._registerView);
    }

    this.displayLogin = function () {
        this._pageContainer.insertBefore(this._loginView, this._switcher.elem);

        if (isLoggedIn() && this._loginViewComponents.form.parentNode !== null)
            this._loginViewComponents.form.replaceWith(this._loginViewComponents.alreadyLoggedIn);
        else if (!isLoggedIn() && this._loginViewComponents.alreadyLoggedIn.parentNode !== null)
            this._loginViewComponents.alreadyLoggedIn.replaceWith(this._loginViewComponents.form);

        this._switcher.flavourText.childNodes[0].textContent = "Don't have an account? "
        this._switcher.href.childNodes[0].textContent = "Register"
    }

    this.hideLogin = function () {
        this.getLoginForm().reset();
        this._pageContainer.removeChild(this._loginView);
    }

    this.getLoginForm = function () {
        return this._loginViewComponents.form;
    }

    this.getRegisterForm = function () {
        return this._registerViewComponents.form;
    }

    this.showErrorMessage = function (msg) {
        if (this.currentState === VIEWS.LOGIN)
            this._loginViewComponents.formError.textContent = msg;
        else
            this._registerViewComponents.formError.textContent = msg;
    }
}

function RegisterFormValidator(viewManager, registerForm, formInputs) {
    this._manager = viewManager;
    this._registerForm = registerForm;
    this._formInputs = formInputs;

    this.addListeners = function () {
        this._formInputs.username.addEventListener("change", e => e.target.setCustomValidity(""));
        this._formInputs.repeatPassword.addEventListener("change", e => e.target.setCustomValidity(""));
        this._registerForm.addEventListener("submit", (e) => {
            this.submit(e.target)
            e.preventDefault();
        });
    }

    this.checkUsername = function (target) {
        return new Promise((resolve, reject) => {
            new Ajax().get(
                "/api/user/byUsername?username=" + encodeURIComponent(target.value),
                (req) => {
                    if (req.readyState !== XMLHttpRequest.DONE)
                        return;
                    switch (req.status) {
                        case 200:
                            target.setCustomValidity("A user with this username already exists");
                            reject();
                            break;
                        case 400:
                            target.setCustomValidity("The username is invalid");
                            console.log(req.responseText);
                            reject();
                            break;
                        case 404:
                            target.setCustomValidity("");
                            resolve();
                            break;
                        default:
                            target.setCustomValidity("Could not verify username");
                            reject();
                            console.log(req.responseText);
                    }
                });
        });
    }

    this.checkPasswords = function (target) {
        let clear = this._formInputs.clearPassword.value;
        let repeat = target.value;

        if (clear !== repeat)
            target.setCustomValidity("The passwords do not match");
        else
            target.setCustomValidity("");
    }

    this.submit = function (target) {
        this.checkUsername(this._formInputs.username)
            .then(
                () => {
                    this.checkPasswords(this._formInputs.repeatPassword);
                    if (target.reportValidity()) {
                        new Ajax().post(
                            "/api/users",
                            convertFormDataToJSON(new FormData(target)),
                            (req) => {
                                if (req.readyState !== XMLHttpRequest.DONE)
                                    return;
                                switch (req.status) {
                                    case 200:
                                        this._manager.switchViews();
                                        break;
                                    case 400:
                                        this._manager.showErrorMessage("Please check that the fields contain valid information");
                                        console.log(req.responseText);
                                        break;
                                    case 409:
                                        this._manager.showErrorMessage("A username with the requested username already exists");
                                        console.log(req.responseText);
                                        break;
                                    default:
                                        this._manager.showErrorMessage("We weren't able to process your request, please try again later")
                                        console.log(req.responseText);
                                }
                            }
                        );
                    }
                },
                () => target.reportValidity()
            );
    }
}

function LoginFormValidator(viewManager, loginForm) {
    this._manager = viewManager;
    this._loginForm = loginForm;

    this.addListeners = function () {
        this._loginForm.addEventListener("submit", (e) => {
            this.submit(e.target)
            e.preventDefault();
        });
    }

    this.submit = function (target) {
        if (!target.reportValidity())
            return;
        let manager = new LoginManager(
            () => window.location = '/index.html',
            (status, body) => {
                switch (status) {
                    case 400:
                        this._manager.showErrorMessage("Please check that the fields contain valid information");
                        console.log(body);
                        break;
                    case 404:
                    case 409:
                        this._manager.showErrorMessage("Username and password are not valid");
                        console.log(body);
                        break;
                    default:
                        this._manager.showErrorMessage("We weren't able to process your request, please try again later");
                        console.log(body);
                }
            }
        );
        manager.login(new FormData(target));
    }
}

(function () {
    let manager = new ViewManager(
        document.getElementById("page-container"),
        document.getElementById("login-view"),
        {
            form: document.getElementById("login-form"),
            alreadyLoggedIn: document.getElementById("login-alreadyLoggedIn"),
            formError: document.getElementById("login-formError")
        },
        document.getElementById("register-view"),
        {
            form: document.getElementById("register-form"),
            formError: document.getElementById("register-formError")
        },
        document.getElementById("switcher")
    );
    let registerValidator = new RegisterFormValidator(
        manager,
        manager.getRegisterForm(),
        {
            username: document.getElementById("register-username"),
            email: document.getElementById("register-email"),
            clearPassword: document.getElementById("register-clearPassword"),
            repeatPassword: document.getElementById("register-repeatPassword")
        }
    );
    let loginValidator = new LoginFormValidator(manager, manager.getLoginForm());
    let logoutButtonManager = new LogoutButtonManager(
        document.getElementById("login-logoutBtn"),
        () => manager.displayLogin()
    );

    registerValidator.addListeners();
    loginValidator.addListeners();
    logoutButtonManager.addListeners();
    manager.init();

    manager.displayLogin();
}());

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
        this.hideLogin();
        this.hideRegister();
        this.addListeners();
    }

    this.addListeners = function () {
        this._switcher.href.addEventListener("click", (e) => {
            this.switchViews();
            e.preventDefault();
        });
    }

    this.switchViews = function () {
        switch (this.currentState) {
            case VIEWS.LOGIN:
                this.currentState = VIEWS.REGISTER;
                this.hideLogin();
                this.displayRegister();
                break;
            case VIEWS.REGISTER:
                this.currentState = VIEWS.LOGIN;
                this.hideRegister();
                this.displayLogin();
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
        this._pageContainer.removeChild(this._registerView);
    }

    this.displayLogin = function () {
        this._pageContainer.insertBefore(this._loginView, this._switcher.elem);

        if (isLoggedIn()) {
            this._loginView.insertBefore(this._loginViewComponents.alreadyLoggedIn, null);
            this._loginView.removeChild(this._loginViewComponents.form);
        } else {
            this._loginView.removeChild(this._loginViewComponents.alreadyLoggedIn);
            this._loginView.insertBefore(this._loginViewComponents.form, null);
        }

        this._switcher.flavourText.childNodes[0].textContent = "Don't have an account? "
        this._switcher.href.childNodes[0].textContent = "Register"
    }

    this.hideLogin = function () {
        this._pageContainer.removeChild(this._loginView);
    }

    this.getLoginForm = function () {
        return this._loginView.children[1];
    }

    this.getRegisterForm = function () {
        return this._registerView.children[1];
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
        this._formInputs.username.addEventListener("change", (e) => this.checkUsername(e.target));
        this._formInputs.repeatPassword.addEventListener(
            "change",
            (e) => this.checkPasswords(e.target)
        );
        this._registerForm.addEventListener("submit", (e) => {
            this.submit(e.target)
            e.preventDefault();
        });
    }

    this.checkUsername = function (target) {
        let ajax = new Ajax();
        ajax.get(
            "/api/user/byUsername?username=" + encodeURIComponent(target.value),
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                switch (req.status) {
                    case 200:
                        target.setCustomValidity("A user with this username already exists");
                        break;
                    case 400:
                        target.setCustomValidity("The username is invalid");
                        console.log(req.responseText);
                        break;
                    case 404:
                        target.setCustomValidity("");
                        break;
                    default:
                        target.setCustomValidity("Could not verify username");
                        console.log(req.responseText);
                }
            })
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
        this.checkUsername(this._formInputs.username);
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
        if (target.reportValidity()) {
            new Ajax().post(
                "/api/auth/login",
                convertFormDataToJSON(new FormData(target)),
                (req) => {
                    if (req.readyState !== XMLHttpRequest.DONE)
                        return;
                    switch (req.status) {
                        case 200:
                            let res = JSON.parse(req.responseText);
                            login(res.token);
                            window.location = "/index.html";
                            break;
                        case 400:
                            this._manager.showErrorMessage("Please check that the fields contain valid information");
                            console.log(req.responseText)
                            break;
                        case 404:
                        case 409:
                            this._manager.showErrorMessage("Username and password are not valid");
                            console.log(req.responseText)
                            break;
                        default:
                            this._manager.showErrorMessage("We weren't able to process your request, please try again later");
                            console.log(req.responseText)
                    }
                },
            );
        }
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

    manager.init();
    registerValidator.addListeners();
    loginValidator.addListeners();
    logoutButtonManager.addListeners();

    manager.displayLogin();
}());

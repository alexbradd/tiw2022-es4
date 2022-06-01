const VIEWS = {
    LOGIN: "login",
    REGISTER: "register"
};

function ViewManager(loginView, registerView, switcherElem) {
    this.currentState = VIEWS.LOGIN;
    this._loginView = loginView;
    this._registerView = registerView;
    this._switcher = {
        elem: switcherElem,
        flavourText: null,
        href: null
    };
    this._switcher.flavourText = this._switcher.elem.firstElementChild;
    this._switcher.href = this._switcher.elem.lastElementChild;

    this.addListener = function () {
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
        this._registerView.classList.remove("hidden");
        this._switcher.flavourText.childNodes[0].textContent = "Already have an account? "
        this._switcher.href.childNodes[0].textContent = "Login"
    }

    this.hideRegister = function () {
        this._registerView.classList.add("hidden");
    }

    this.displayLogin = function () {
        this._loginView.classList.remove("hidden");
        this._switcher.flavourText.childNodes[0].textContent = "Don't have an account? "
        this._switcher.href.childNodes[0].textContent = "Register"
    }

    this.hideLogin = function () {
        this._loginView.classList.add("hidden");
    }

    this.getLoginForm = function () {
        return this._loginView.children[1];
    }

    this.getRegisterForm = function () {
        return this._registerView.children[1];
    }
}

function RegisterFormValidator(registerForm, formInputs) {
    this._registerForm = registerForm;
    this._formInputs = formInputs;

    this.addListeners = function () {
        this._formInputs.username.addEventListener("blur", (e) => this.checkUsername(e.target));
        this._formInputs.repeatPassword.addEventListener(
            "blur",
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
                target.reportValidity();
            })
    }

    this.checkPasswords = function (target) {
        let clear = this._formInputs.clearPassword.value;
        let repeat = target.value;

        if (clear !== repeat)
            target.setCustomValidity("The passwords do not match");
        else
            target.setCustomValidity("");
        target.reportValidity();
    }

    this.submit = function (target) {
        console.log("submit");
    }
}

function LoginFormValidator(loginForm) {
    this._loginForm = loginForm;

    this.addListeners = function () {
        this._loginForm.addEventListener("submit", (e) => {
            this.submit(e.target)
            e.preventDefault();
        });
    }

    this.submit = function (target) {
        console.log("submit");
    }
}

(function () {
    let manager = new ViewManager(
        document.getElementById("login-view"),
        document.getElementById("register-view"),
        document.getElementById("switcher")
    );
    let registerValidator = new RegisterFormValidator(
        manager.getRegisterForm(),
        {
            username: document.getElementById("register-username"),
            email: document.getElementById("register-email"),
            clearPassword: document.getElementById("register-clearPassword"),
            repeatPassword: document.getElementById("register-repeatPassword")
        }
    );
    let loginValidator = new LoginFormValidator(manager.getLoginForm());

    manager.addListener();
    registerValidator.addListeners();
    loginValidator.addListeners();

    manager.displayLogin();
}());

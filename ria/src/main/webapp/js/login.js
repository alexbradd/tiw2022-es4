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
}

(function () {
    let manager = new ViewManager(
        document.getElementById("login-view"),
        document.getElementById("register-view"),
        document.getElementById("switcher")
    );
    manager.addListener();
    manager.displayLogin();
}());

function UserDetailsManager(popupElements) {
    this.user = JSON.parse(window.sessionStorage.getItem("user"));
    this._popupElements = popupElements;

    this.showUserDetails = function () {
        this._popupElements.userId.textContent = this.user.base64Id;
        this._popupElements.username.textContent = this.user.username;
        this._popupElements.email.textContent = this.user.email;
        this._popupElements.nameSurname.textContent = `${this.user.name} ${this.user.surname}`;
    }
}

(function () {
    if (!isLoggedIn())
        window.location = "/login.html";

    let userDetailsManager = new UserDetailsManager(
        {
            userId: document.getElementById("userDetails-userId"),
            username: document.getElementById("userDetails-username"),
            email: document.getElementById("userDetails-email"),
            nameSurname: document.getElementById("userDetails-nameSurname")
        }
    )
    let logoutButtonManager = new LogoutButtonManager(
        document.getElementById("logoutBtn"),
        () => window.location = "/login.html"
    );

    userDetailsManager.showUserDetails();
    logoutButtonManager.addListeners();
}());
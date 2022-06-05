function generateNewTableRow() {
    let row = document.createElement("tr");
    for (let i = 0; i < arguments.length; i++) {
        let data = document.createElement("td");
        let content = arguments[i] instanceof Node
            ? arguments[i]
            : document.createTextNode(arguments[i]);
        data.appendChild(content);
        row.appendChild(data);
    }
    return row;
}

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

function ViewOrchestrator(accountListManager) {

}

function AccountListManager(user, viewElements) {
    this._viewElements = viewElements;
    this._user = user;
    this.accountList = undefined;

    this.addListeners = function () {
        this._viewElements.refreshButton.addEventListener("click", () => {
            this.refresh();
        });
        this._viewElements.newAccountButton.addEventListener("click", () => {
            new Ajax().authenticatedPost(
                "/api/accounts",
                null,
                (req, failedRefresh) => {
                    if (req.readyState !== XMLHttpRequest.DONE)
                        return;
                    if (failedRefresh)
                        window.location = '/login.html';
                    else if (req.status === 200)
                        this.refresh();
                    else // fixme display error in dialog box
                        console.log(req.responseText);
                }
            )
        })
    }

    this.show = function () {
        this.fetchAccountList(() => this.displayAccountList());
    }

    this.refresh = function () {
        this.clearAccountList();
        this.show();
    }

    this.fetchAccountList = function (afterFetch) {
        new Ajax().authenticatedPost(
            "/api/accounts/ofUser",
            {userId: this._user.base64Id, detailed: true},
            (req, failedRefresh) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                if (failedRefresh)
                    window.location = '/login.html';
                else if (req.status === 200) {
                    this.accountList = JSON.parse(req.responseText).accounts;
                    afterFetch();
                } else // fixme display error in dialog box
                    console.log(req.responseText);
            }
        );
    }

    this.clearAccountList = function () {
        let tableBody = this._viewElements.tableBody;
        while (tableBody.lastChild)
            tableBody.removeChild(tableBody.lastChild);
    }

    this.displayAccountList = function () {
        let tableBody = this._viewElements.tableBody;
        this.accountList.forEach((account) => {
            let detailsLink = document.createElement("a");
            detailsLink.addEventListener("click", (e) => console.log("click"));
            detailsLink.href = "#";
            detailsLink.appendChild(document.createTextNode("Details"));
            tableBody.appendChild(generateNewTableRow(account.base64Id, account.balance, detailsLink));
        });
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
    let accountListManager = new AccountListManager(
        userDetailsManager.user,
        {
            tableBody: document.getElementById("accounts-tableBody"),
            refreshButton: document.getElementById("accounts-refresh"),
            newAccountButton: document.getElementById("accounts-new")
        }
    );
    let logoutButtonManager = new LogoutButtonManager(
        document.getElementById("logoutBtn"),
        () => window.location = "/login.html"
    );

    userDetailsManager.showUserDetails();
    logoutButtonManager.addListeners();
    accountListManager.addListeners();
    accountListManager.show();
}());
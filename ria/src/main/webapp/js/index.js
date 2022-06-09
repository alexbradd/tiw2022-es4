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

function ViewOrchestrator(user, pageContainer, modalElements, accountListViewElements) {
    this._user = user;
    this._pageContainer = pageContainer;
    this._modalManager = new ModalManager(modalElements);
    this._accountListManager = new AccountListManager(this._user, this._pageContainer, accountListViewElements);

    this.init = function () {
        this._accountListManager.addListeners();
        this._modalManager.hide();
        this._accountListManager.show();
    }

}

function ModalManager(viewElements) {
    this._viewElements = viewElements;

    this.show = function (title, text, actionList) {
        this._viewElements.title.textContent = title;
        this._viewElements.text.textContent = text;
        this._clearActionList();
        actionList.forEach((el) => {
            let button = document.createElement("button");
            let buttonText = document.createTextNode(el.text);
            button.appendChild(buttonText);
            button.addEventListener("click", (e) => el.callback(e, this));
            button.classList.add("big-button");
            if (el.classList !== undefined)
                button.classList.add(el.classList)
            this._viewElements.actionList.appendChild(button);
        })
        document.body.insertBefore(this._viewElements.view, null);
    }

    this.hide = function () {
        if (this._viewElements.view.parentNode !== null)
            document.body.removeChild(this._viewElements.view);
    }

    this._clearActionList = function () {
        let actionList = this._viewElements.actionList;
        while (actionList.lastChild)
            actionList.removeChild(actionList.lastChild);
    }

    this.showError = function (text) {
        this.show("Error", text, [ModalManager.closeButton]);
    }
}

ModalManager.closeButton = {
    text: "Close",
    callback: (e, man) => man.hide()
};

function AccountListManager(user, container, viewElements) {
    this._user = user;
    this._container = container;
    this._viewElements = viewElements;
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
        this._container.insertBefore(this._viewElements.view, null);
        this._fetchAccountList(() => this._displayAccountList());
    }

    this.hide = function () {
        if (this._viewElements.view.parentNode !== null) {
            this._clearAccountList();
            this._container.removeChild(this._viewElements.view);
        }
    }

    this.refresh = function () {
        this._clearAccountList();
        this._fetchAccountList(() => this._displayAccountList());
    }

    this._fetchAccountList = function (afterFetch) {
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

    this._clearAccountList = function () {
        let tableBody = this._viewElements.tableBody;
        while (tableBody.lastChild)
            tableBody.removeChild(tableBody.lastChild);
    }

    this._displayAccountList = function () {
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
    let viewOrchestrator = new ViewOrchestrator(
        userDetailsManager.user,
        document.getElementById("page-container"),
        {
            view: document.getElementById("modal"),
            title: document.getElementById("modal-title"),
            text: document.getElementById("modal-text"),
            actionList: document.getElementById("modal-actions")
        },
        {
            view: document.getElementById("account-view"),
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
    viewOrchestrator.init();
}());
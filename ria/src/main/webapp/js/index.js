function generateNewTableRow() {
    let row = document.createElement("tr");
    for (let i = 0; i < arguments.length; i++) {
        let td = document.createElement("td");

        if (arguments[i] instanceof Array)
            arguments[i].forEach((a) => _insertIntoTd(td, a))
        else
            _insertIntoTd(td, arguments[i]);
        row.appendChild(td);
    }
    return row;

    function _insertIntoTd(td, arg) {
        let content;
        if (arg instanceof Node)
            content = arg;
        else if (arg instanceof Object) {
            td.classList.add(arg.dataClass);
            content = arg.content;
        } else
            content = document.createTextNode(arg);
        td.appendChild(content);
    }
}

function clearChildren(node) {
    while (node.lastChild)
        node.removeChild(node.lastChild);
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

function ViewOrchestrator(user,
                          pageContainer,
                          modalElements,
                          accountListViewElements,
                          accountDetailsViewElements) {
    this._user = user;
    this._pageContainer = pageContainer;
    this._modalManager = new ModalManager(modalElements);
    this._accountListManager = new AccountListManager(this._user,
        this._pageContainer,
        accountListViewElements,
        this._modalManager,
        (_, a) => this.showDetailsFor(a));
    this._accountDetailsManager = new AccountDetailsManager(this._pageContainer,
        accountDetailsViewElements,
        this._modalManager);

    this.init = function () {
        this._accountListManager.addListeners();
        this._accountDetailsManager.addListeners((_) => this.showAccountList());

        this._modalManager.hide();
        this._accountListManager.hide();
        this._accountDetailsManager.hide();

        this._modalManager.removeHiddenClass();
        this._accountListManager.removeHiddenClass();
        this._accountDetailsManager.removeHiddenClass();

        this.showAccountList();
    }

    this.showAccountList = function () {
        this._accountDetailsManager.hide();
        this._accountListManager.show((_, a) => this.showDetailsFor(a));
    }

    this.showDetailsFor = function (account) {
        this._accountListManager.hide();
        this._accountDetailsManager.show(account)
    }
}

function ModalManager(viewElements) {
    this._viewElements = viewElements;

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function (title, content, actionList) {
        this._viewElements.title.textContent = title;
        let toAppend;
        if (content instanceof String || typeof content == 'string') {
            let p = document.createElement('p');
            p.classList.add('modal-text')
            p.textContent = content;
            toAppend = p;
        } else if (content instanceof Node) {
            toAppend = content;
        } else {
            throw new TypeError("Invalid modal content");
        }
        this._viewElements.content.appendChild(toAppend);
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
        clearChildren(this._viewElements.content);
        clearChildren(this._viewElements.actionList);
        if (this._viewElements.view.parentNode !== null)
            document.body.removeChild(this._viewElements.view);
    }

    this.showError = function (text) {
        this.show("Error", text, [ModalManager.closeButton]);
    }
}

ModalManager.closeButton = {
    text: "Close",
    callback: (e, man) => man.hide()
};

function AccountListManager(user, container, viewElements, modalManager, detailsClickCallback) {
    this._user = user;
    this._container = container;
    this._viewElements = viewElements;
    this._modalManager = modalManager;
    this._detailsClickCallback = detailsClickCallback;
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
                    else {
                        this._modalManager.showError("We could not create a new account, please try again later")
                        console.log(req.responseText);
                    }
                }
            )
        })
    }

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function () {
        this._container.insertBefore(this._viewElements.view, null);
        if (this.accountList === undefined)
            this._fetchAccountList(() => this._displayAccountList(this._detailsClickCallback));
    }

    this.hide = function () {
        if (this._viewElements.view.parentNode !== null)
            this._container.removeChild(this._viewElements.view);
    }

    this.refresh = function () {
        this._clearAccountList();
        this._fetchAccountList(() => this._displayAccountList(this._detailsClickCallback));
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
                } else {
                    this._modalManager.showError("We could not fetch your account list, please try again later")
                    console.log(req.responseText);
                }
            }
        );
    }

    this._clearAccountList = function () {
        clearChildren(this._viewElements.tableBody);
    }

    this._displayAccountList = function (detailsClickCallback) {
        let tableBody = this._viewElements.tableBody;
        this.accountList.forEach((account) => {
            let detailsLink = document.createElement("a");
            detailsLink.addEventListener("click", (e) => detailsClickCallback(e, account));
            detailsLink.href = "#";
            detailsLink.appendChild(document.createTextNode("Details"));
            tableBody.appendChild(generateNewTableRow(account.base64Id, account.balance, detailsLink));
        });
    }
}

function AccountDetailsManager(container, viewElements, modalManager) {
    this._container = container;
    this._viewElements = viewElements;
    this._modalManager = modalManager;
    this._currentlyShowingAccount = undefined;

    this.addListeners = function (goBackCallback) {
        this._viewElements.backButton.addEventListener("click", (e) => goBackCallback(e));
        this._viewElements.refreshButton.addEventListener("click", () =>
            this._constructDialog(this._currentlyShowingAccount));
    }

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function (account) {
        this._currentlyShowingAccount = account;
        this._constructDialog(account)
        this._container.insertBefore(this._viewElements.view, null);
    }

    this._constructDialog = function (account) {
        clearChildren(this._viewElements.incomingTransfers);
        clearChildren(this._viewElements.outgoingTransfers);
        this._viewElements.accountId.textContent = account.base64Id;
        this._viewElements.accountBalance.textContent = account.balance;
        this._fetchAccountDetails(account, (i, o) => {
            i.forEach((t) =>
                this._viewElements.incomingTransfers.appendChild(this._constructRow(t, "fromId")));
            o.forEach((t) =>
                this._viewElements.outgoingTransfers.appendChild(this._constructRow(t, "toId")));
        });
    }

    this._fetchAccountDetails = function (account, afterFetchCallback) {
        new Ajax().authenticatedPost(
            "/api/accounts/transfers",
            {accountId: account.base64Id},
            (req, failedRefresh) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                if (failedRefresh)
                    window.location = "/login.html";
                else if (req.status) {
                    let obj = JSON.parse(req.responseText);
                    afterFetchCallback(obj.incoming, obj.outgoing);
                } else
                    this._modalManager.showError("Could not fetch details for this account");
            }
        );
    }

    this._constructRow = function (t, idToShow) {
        let date = new Date(t.date).toLocaleString(
            "en-GB",
            {
                day: "numeric",
                month: "numeric",
                year: "2-digit",
                hour: "numeric",
                minute: "numeric"
            });
        let causalMessageNode = document.createElement("div");
        causalMessageNode.classList.add("causal-message");
        causalMessageNode.textContent = t.causal;
        let tr = generateNewTableRow(
            t.base64Id,
            t[idToShow],
            date,
            t.amount + "€",
            ["...", {dataClass: "causal", content: causalMessageNode}]
        );
        tr.classList.add("transaction");
        return tr;
    }

    this.hide = function () {
        if (this._viewElements.view.parentNode !== null)
            this._container.removeChild(this._viewElements.view);
    }
}

// (function () {
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
        content: document.getElementById("modal-content"),
        actionList: document.getElementById("modal-actions")
    },
    {
        view: document.getElementById("account-view"),
        tableBody: document.getElementById("accounts-tableBody"),
        refreshButton: document.getElementById("accounts-refresh"),
        newAccountButton: document.getElementById("accounts-new")
    },
    {
        view: document.getElementById("details-view"),
        refreshButton: document.getElementById("accountDetails-refresh"),
        backButton: document.getElementById("accountDetails-back"),
        accountId: document.getElementById("accountDetails-id"),
        accountBalance: document.getElementById("accountDetails-balance"),
        incomingTransfers: document.getElementById("accountDetails-incomingBody"),
        outgoingTransfers: document.getElementById("accountDetails-outgoingBody")
    }
);
let logoutButtonManager = new LogoutButtonManager(
    document.getElementById("logoutBtn"),
    () => window.location = "/login.html"
);

userDetailsManager.showUserDetails();
logoutButtonManager.addListeners();
viewOrchestrator.init();
// }());
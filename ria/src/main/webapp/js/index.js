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

function UserDetailsManager(user, popupElements) {
    this._user = user;
    this._popupElements = popupElements;

    this.injectUserDetails = function () {
        this._popupElements.userId.textContent = this._user.base64Id;
        this._popupElements.username.textContent = this._user.username;
        this._popupElements.email.textContent = this._user.email;
        this._popupElements.nameSurname.textContent = `${this._user.name} ${this._user.surname}`;
    }
}

function ViewOrchestrator(user,
                          pageContainer,
                          modalElements,
                          accountListViewElements,
                          accountDetailsViewElements,
                          newTransferViewElements) {
    this._user = user;
    this._pageContainer = pageContainer;
    this._modalManager = new ModalManager(modalElements);
    this._accountListManager = new AccountListManager(
        this._user,
        this._pageContainer,
        accountListViewElements,
        this._modalManager,
        (_, a) => this.showDetailsFor(a));
    this._accountDetailsManager = new AccountDetailsManager(
        this._pageContainer,
        accountDetailsViewElements,
        this._modalManager);
    this._newTransferFormManager = new NewTransferFormManager(
        this._user,
        this._pageContainer,
        newTransferViewElements,
        this._modalManager,
        () => {
            this._accountDetailsManager.refresh();
            this._newTransferFormManager.clearFields();
        }
    );

    this.init = function () {
        this._accountListManager.addListeners();
        this._accountDetailsManager.addListeners((_) => this.showAccountList());
        this._newTransferFormManager.addListeners();

        this._modalManager.hide();
        this._accountListManager.hide();
        this._accountDetailsManager.hide();
        this._newTransferFormManager.hide();

        this._modalManager.removeHiddenClass();
        this._accountListManager.removeHiddenClass();
        this._accountDetailsManager.removeHiddenClass();
        this._newTransferFormManager.removeHiddenClass();

        this.showAccountList();
    }

    this.showAccountList = function () {
        this._accountDetailsManager.hide();
        this._newTransferFormManager.hide();
        this._accountListManager.show();
    }

    this.showDetailsFor = function (account) {
        this._accountListManager.hide();
        this._accountDetailsManager.show(account)
        this._newTransferFormManager.show(account);
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
        this._viewElements.refreshButton.addEventListener("click", () => this.refresh());
    }

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function (account) {
        this._currentlyShowingAccount = account;
        this._constructDialog(account)
        this._container.insertBefore(this._viewElements.view, null);
    }

    this.refresh = function () {
        this._constructDialog(this._currentlyShowingAccount);
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
                else if (req.status === 200) {
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

function NewTransferFormManager(user, container, viewElements, modalManager, afterTransferSuccessful) {
    this._user = user;
    this._container = container;
    this._viewElements = viewElements;
    this._modalManager = modalManager;
    this._showingAccount = undefined;
    this._contacts = [];
    this._afterCloseCb = afterTransferSuccessful;

    this.addListeners = function () {
        this._viewElements.formElements.payeeId.addEventListener("focus", () => this._fetchContacts());
        this._viewElements.formElements.payeeAccount.addEventListener(
            "focus",
            () => this._fetchAccountList(this._viewElements.formElements.payeeId.value));
        Object.keys(this._viewElements.formElements).forEach(k =>
            this._setupErrorReporting(this._viewElements.formElements[k]));

        this._viewElements.form.addEventListener("submit", e => {
            this._checkFormValidityAndThen(e.target, () => {
                if (e.target.checkValidity())
                    this._sendFormData(e.target);
            });
            e.preventDefault();
        });
    }

    this._fetchContacts = function () {
        this._contacts = [];
        new Ajax().authenticatedPost(
            "/api/contacts/ofUser",
            {userId: this._user.base64Id},
            (req, failedRefresh) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                if (failedRefresh) {
                    window.location = '/login.html';
                } else if (req.status === 200) {
                    clearChildren(this._viewElements.contacts);
                    let contactList = JSON.parse(req.responseText).contacts;
                    for (let i = 0; i < contactList.length; i++) {
                        this._contacts.push(contactList[i].contactBase64Id);
                        this._viewElements.contacts.appendChild(
                            this._createDatalistOption(contactList[i].contactBase64Id));
                    }
                } else {
                    this._modalManager.showError("We could not fetch your contact list, please try again later")
                    console.log(req.responseText);
                }
            }
        )
    }

    this._fetchAccountList = function (userId) {
        if (userId === undefined || userId === "")
            return;
        new Ajax().authenticatedPost(
            "/api/accounts/ofUser",
            {userId: userId, detailed: false},
            (req, failedRefresh) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                if (failedRefresh) {
                    window.location = '/login.html';
                } else if (req.status === 200) {
                    clearChildren(this._viewElements.payeeAccounts);
                    let accountList = JSON.parse(req.responseText).accounts;
                    for (let i = 0; i < accountList.length; i++)
                        this._viewElements.payeeAccounts.appendChild(
                            this._createDatalistOption(accountList[i].base64Id));
                } else {
                    this._viewElements.formElements.payeeId.setCustomValidity("Unable to find account");
                    console.log(req.responseText);
                }
            }
        );
    }

    this._createDatalistOption = function (value) {
        let c = document.createElement("option");
        c.setAttribute("value", value);
        return c;
    }

    this._setupErrorReporting = function (el) {
        el.addEventListener("invalid", e => this._showError(e.target));
        el.addEventListener("change", e => this._removeError(e.target));
    }

    this._showError = function (target) {
        const errId = target.id + '__form-error';
        let p = document.getElementById(errId);
        if (p === null) {
            p = document.createElement('p');
            p.id = target.id + '__form-error';
            p.classList.add('form-input-error');
        }

        target.insertAdjacentElement("afterend", p);
        p.textContent = target.validationMessage;
    }

    this._removeError = function (target) {
        target.setCustomValidity("");
        const p = document.getElementById(target.id + '__form-error');
        if (p !== null && p.parentNode !== null)
            target.parentElement.removeChild(p)
    }

    this._checkFormValidityAndThen = function (form, then) {
        return this._checkAmount(this._viewElements.formElements.amount)
            .then(() => this._checkPayeeId(this._viewElements.formElements.payeeId))
            .then(userId => this._checkPayeeAccount(userId, this._viewElements.formElements.payeeAccount))
            .then(() => then(), () => then());
    }

    this._checkAmount = function (target) {
        return new Promise((resolve, reject) => {
            if (target.value <= 0 || target.value > this._showingAccount.balance) {
                target.setCustomValidity("The amount is not within permitted bounds");
                reject();
            } else {
                target.setCustomValidity("");
                resolve();
            }
        });
    }

    this._checkPayeeId = function (target) {
        return new Promise((resolve, reject) => {
            new Ajax().get(
                "/api/user/byId?id=" + target.value,
                req => {
                    if (req.readyState !== XMLHttpRequest.DONE)
                        return;
                    if (req.status === 200) {
                        target.setCustomValidity("");
                        resolve(target.value);
                    } else {
                        target.setCustomValidity("Unable to find user with the specified ID");
                        reject();
                    }
                }
            );
        });
    }

    this._checkPayeeAccount = function (userId, target) {
        return new Promise((resolve, reject) => {
            new Ajax().authenticatedPost(
                "/api/accounts/ofUser",
                {userId: userId, detailed: false},
                (req, failedRefresh) => {
                    if (req.readyState !== XMLHttpRequest.DONE)
                        return;
                    if (failedRefresh) {
                        window.location = '/login.html';
                    } else if (req.status === 200) {
                        let accountList = JSON.parse(req.responseText).accounts;
                        for (let i = 0; i < accountList.length; i++) {
                            if (accountList[i].base64Id === target.value) {
                                target.setCustomValidity("");
                                resolve();
                                return;
                            }
                        }
                        target.setCustomValidity("Unable to find an account with the specified ID");
                    } else {
                        target.setCustomValidity("Unable to find an account with the specified ID");
                    }
                    reject();
                }
            );
        });
    }

    this._sendFormData = function (form) {
        const formData = new FormData(form);
        const json = {
            ...convertFormDataToObject(formData),
            fromUserId: this._user.base64Id,
            fromAccountId: this._showingAccount.base64Id
        };

        new Ajax().authenticatedPost(
            "/api/transfers",
            json,
            (req, failedRefresh) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                if (failedRefresh)
                    window.location = '/login.html';
                switch (req.status) {
                    case 200:
                        const transfer = JSON.parse(req.responseText).transfer;
                        this._showSuccessModal(transfer, json.toUserId);
                        break;
                    case 400:
                        this._modalManager.showError("The transfer form was not filled correctly, please check the data and try again.");
                        break;
                    case 409:
                        this._modalManager.showError("The source account has not got enough funds to make the transfer.");
                        break;
                    case 404:
                        this._modalManager.showError("The user-account combination could not be found, please check the data and try again.");
                        break;
                    default:
                        this._modalManager.showError("The server could not process your request, please try again later.");
                }
            }
        );
    }

    this._showSuccessModal = function (transfer, toUserId) {
        const table = this._createTransferReport(transfer);
        const addContactButton = {
            text: "Save contact",
            classList: "accent",
            callback: (e, man) => {
                this._addNewContact(toUserId);
                man.hide();
                this._afterCloseCb();
            }
        };
        const close = {
            text: "Close",
            callback: (e, man) => {
                man.hide();
                this._afterCloseCb();
            }
        }
        const modalActions = this._isInContactList(toUserId)
            ? [close]
            : [addContactButton, close];
        this._modalManager.show("Transfer successful", table, modalActions);
    }

    this._createTransferReport = function (transfer) {
        const table = document.createElement("table");
        const tableBody = document.createElement("tbody");
        table.classList.add("transfer-details");
        table.appendChild(tableBody);
        tableBody.appendChild(generateNewTableRow("Id", transfer.base64Id));
        tableBody.appendChild(generateNewTableRow("Payer account id", transfer.fromId));
        tableBody.appendChild(generateNewTableRow("Payee account id", transfer.toId));
        tableBody.appendChild(generateNewTableRow("Amount", transfer.amount));
        tableBody.appendChild(generateNewTableRow("Causal", transfer.causal));
        tableBody.appendChild(generateNewTableRow("Payer balance", `${transfer.fromBalance} → ${transfer.fromBalance - transfer.amount}`));
        tableBody.appendChild(generateNewTableRow("Payee balance", `${transfer.toBalance} → ${transfer.toBalance + transfer.amount}`));
        return table;
    }

    this._isInContactList = function (id) {
        return this._contacts.includes(id);
    }

    this._addNewContact = function (contactId) {
        new Ajax().authenticatedPost(
            "/api/contacts",
            {contactId},
            (req, failedRefresh) => {
                if (req.readyState !== XMLHttpRequest.DONE)
                    return;
                if (failedRefresh)
                    window.location = '/login.html';
                if (req.status !== 200) {
                    this._modalManager.showError("Could not add new contact");
                    console.log(req.responseText);
                }
            }
        );
    }

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function (account) {
        this._showingAccount = account;
        this._container.insertBefore(this._viewElements.view, null);
        this._viewElements.formElements.amount.setAttribute("max", account.balance);
    }

    this.hide = function () {
        clearChildren(this._viewElements.contacts);
        clearChildren(this._viewElements.payeeAccounts);
        this.clearFields();
        if (this._viewElements.view.parentNode !== null)
            this._container.removeChild(this._viewElements.view);
    }

    this.clearFields = function () {
        this._viewElements.form.reset();
        Object.keys(this._viewElements.formElements).forEach(k => {
            this._removeError(this._viewElements.formElements[k]);
        });
    }
}

(function () {
    if (!isLoggedIn())
        window.location = "/login.html";

    let userDetailsManager = new UserDetailsManager(
        getUser(),
        {
            userId: document.getElementById("userDetails-userId"),
            username: document.getElementById("userDetails-username"),
            email: document.getElementById("userDetails-email"),
            nameSurname: document.getElementById("userDetails-nameSurname")
        }
    )
    let viewOrchestrator = new ViewOrchestrator(
        getUser(),
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
        },
        {
            view: document.getElementById("newTransfer-view"),
            form: document.getElementById("newTransfer-form"),
            formElements: {
                payeeId: document.getElementById("newTransfer-toUserId"),
                payeeAccount: document.getElementById("newTransfer-toAccountId"),
                amount: document.getElementById("newTransfer-amount"),
                causal: document.getElementById("newTransfer-causal"),
            },
            submit: document.getElementById("newTransfer-submit"),
            contacts: document.getElementById("userContacts"),
            payeeAccounts: document.getElementById("payeeAccounts")
        }
    );
    let logoutButtonManager = new LogoutButtonManager(
        document.getElementById("logoutBtn"),
        () => window.location = "/login.html"
    );

    userDetailsManager.injectUserDetails();
    logoutButtonManager.addListeners();
    viewOrchestrator.init();
}());
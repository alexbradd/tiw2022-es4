const volatileStorage = new Map();

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
    this._user = volatileStorage.get('user');
    this._popupElements = popupElements;

    this.injectUserDetails = function () {
        this._popupElements.userId.textContent = this._user.base64Id;
        this._popupElements.username.textContent = this._user.username;
        this._popupElements.email.textContent = this._user.email;
        this._popupElements.nameSurname.textContent = `${this._user.name} ${this._user.surname}`;
    }
}

function ViewOrchestrator(pageContainer,
                          modalElements,
                          accountListViewElements,
                          accountDetailsViewElements,
                          newTransferViewElements) {
    this._pageContainer = pageContainer;
    this._modalManager = new ModalManager(modalElements);
    this._accountListManager = new AccountListManager(
        this._pageContainer,
        accountListViewElements,
        this._modalManager,
        (_, a) => this.showDetailsFor(a));
    this._accountDetailsManager = new AccountDetailsManager(
        this._pageContainer,
        accountDetailsViewElements,
        this._modalManager);
    this._newTransferFormManager = new NewTransferFormManager(
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

function Dispatcher(modalManager) {
    this._modal = modalManager;
    this._modalParameters = [
        "Session expired",
        "We were unable to refresh your token",
        [{
            text: "Go to login",
            callback() {
                window.location = '/login.html';
            }
        }]
    ]

    this.fetchAccountList = function (userId, detailed = true) {
        return new Promise((resolve, reject) => {
            try {
                new Ajax().authenticatedPost(
                    "/api/accounts/ofUser",
                    {userId: userId, detailed: detailed},
                    (req, failedRefresh) => {
                        if (req.readyState !== XMLHttpRequest.DONE)
                            return;
                        if (failedRefresh) {
                            this._modal?.show(...this._modalParameters);
                            return;
                        }

                        switch (req.status) {
                            case 200:
                                let accountList = JSON.parse(req.responseText).accounts;
                                resolve(accountList);
                                break;
                            case 400:
                                reject("We could not extract an account list");
                                break;
                            default:
                                console.log(req.responseText);
                                reject("We could not fetch an account list, please try again later");
                        }
                    }
                );
            } catch (ignored) {
                this._modal?.show(...this._modalParameters);
            }
        });
    }
    this.fetchAccountDetails = function (accountId) {
        return new Promise((resolve, reject) => {
            try {
                new Ajax().authenticatedPost(
                    "/api/accounts/transfers",
                    {accountId: accountId},
                    (req, failedRefresh) => {
                        if (req.readyState !== XMLHttpRequest.DONE)
                            return;
                        if (failedRefresh)
                            this._modal?.show(...this._modalParameters);
                        else if (req.status === 200) {
                            let obj = JSON.parse(req.responseText);
                            resolve(obj);
                        } else
                            reject("Could not fetch details for this account");
                    }
                );
            } catch (ignored) {
                this._modal?.show(...this._modalParameters);
            }
        });
    }
    this.fetchContacts = function (user) {
        return new Promise((resolve, reject) => {
            try {
                new Ajax().authenticatedPost(
                    "/api/contacts/ofUser",
                    {userId: user.base64Id},
                    (req, failedRefresh) => {
                        if (req.readyState !== XMLHttpRequest.DONE)
                            return;
                        if (failedRefresh) {
                            this._modal?.show(...this._modalParameters);
                        } else if (req.status === 200) {
                            let contactList = JSON.parse(req.responseText).contacts;
                            resolve(contactList);
                        } else {
                            reject("We could not fetch your contact list, please try again later")
                            console.log(req.responseText);
                        }
                    }
                );
            } catch (ignored) {
                this._modal?.show(...this._modalParameters);
            }
        });
    }
    this.newAccount = function () {
        return new Promise((resolve, reject) => {
            try {
                new Ajax().authenticatedPost(
                    "/api/accounts",
                    null,
                    (req, failedRefresh) => {
                        if (req.readyState !== XMLHttpRequest.DONE)
                            return;
                        if (failedRefresh)
                            this._modal?.show(...this._modalParameters);
                        else if (req.status === 200)
                            resolve();
                        else {
                            console.log(req.responseText);
                            reject("We could not create a new account, please try again later");
                        }
                    }
                );
            } catch (ignored) {
                this._modal?.show(...this._modalParameters);
            }
        });
    }
    this.userById = function (userId) {
        return new Promise((resolve, reject) => {
            new Ajax().get(
                "/api/user/byId?id=" + userId,
                req => {
                    if (req.readyState !== XMLHttpRequest.DONE)
                        return;
                    if (req.status === 200)
                        resolve(userId);
                    else
                        reject("Unable to find user with the specified ID");
                }
            );
        });
    }
    this.newTransfer = function (data) {
        return new Promise((resolve, reject) => {
            try {
                new Ajax().authenticatedPost(
                    "/api/transfers",
                    data,
                    (req, failedRefresh) => {
                        if (req.readyState !== XMLHttpRequest.DONE)
                            return;
                        if (failedRefresh) {
                            this._modal?.show(...this._modalParameters);
                            return;
                        }
                        switch (req.status) {
                            case 200:
                                const transfer = JSON.parse(req.responseText).transfer;
                                resolve(transfer);
                                break;
                            case 400:
                                reject("The transfer form was not filled correctly, please check the data and try again.");
                                break;
                            case 409:
                                reject("The source account has not got enough funds to make the transfer.");
                                break;
                            case 404:
                                reject("The user-account combination could not be found, please check the data and try again.");
                                break;
                            default:
                                reject("The server could not process your request, please try again later.");
                        }
                    }
                );
            } catch (ignored) {
                this._modal?.show(...this._modalParameters);
            }
        });
    }
    this.newContact = function (contactId) {
        return new Promise((resolve, reject) => {
            try {
                new Ajax().authenticatedPost(
                    "/api/contacts",
                    {contactId},
                    (req, failedRefresh) => {
                        if (req.readyState !== XMLHttpRequest.DONE)
                            return;
                        if (failedRefresh)
                            this._modal?.show(...this._modalParameters);
                        else if (req.status !== 200) {
                            reject("Could not add new contact");
                            console.log(req.responseText);
                        } else
                            resolve();
                    }
                );
            } catch (ignored) {
                this._modal?.show(...this._modalParameters);
            }
        });
    }
}

function AccountListManager(container, viewElements, modalManager, detailsClickCallback) {
    this._user = volatileStorage.get('user');
    this._container = container;
    this._viewElements = viewElements;
    this._modalManager = modalManager;
    this._dispatcher = new Dispatcher(modalManager);
    this._detailsClickCallback = detailsClickCallback;

    this.addListeners = function () {
        this._viewElements.refreshButton.addEventListener("click", () => {
            this.refresh();
        });
        this._viewElements.newAccountButton.addEventListener("click", () => {
            this._dispatcher.newAccount()
                .then(() => this.refresh())
                .catch(r => this._modalManager.showError(r));
        })
    }

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function () {
        if (this._viewElements.view.parent !== null)
            this._container.insertBefore(this._viewElements.view, null);
        this._dispatcher.fetchAccountList(this._user.base64Id)
            .then(accountList => {
                volatileStorage.set('accountList', accountList);
                this._displayAccountList(this._detailsClickCallback)
            })
            .catch(r => this._modalManager.showError(r));
    }

    this.hide = function () {
        clearChildren(this._viewElements.tableBody);
        if (this._viewElements.view.parentNode !== null)
            this._container.removeChild(this._viewElements.view);
    }

    this.refresh = function () {
        this._clearAccountList();
        this._dispatcher.fetchAccountList(this._user.base64Id)
            .then(accountList => {
                volatileStorage.set('accountList', accountList);
                this._displayAccountList(this._detailsClickCallback)
            })
            .catch(r => this._modalManager.showError(r));
    }

    this._clearAccountList = function () {
        clearChildren(this._viewElements.tableBody);
    }

    this._displayAccountList = function (detailsClickCallback) {
        let tableBody = this._viewElements.tableBody;
        volatileStorage.get('accountList').forEach((account) => {
            let detailsLink = document.createElement("a");
            detailsLink.addEventListener("click", (e) => detailsClickCallback(e, account));
            detailsLink.href = "#";
            detailsLink.appendChild(document.createTextNode("Details"));
            tableBody.appendChild(generateNewTableRow(account.base64Id, account.balance, detailsLink));
        });
    }
}

function AccountDetailsManager(container, viewElements, modalManager) {
    this._user = volatileStorage.get('user');
    this._container = container;
    this._viewElements = viewElements;
    this._modalManager = modalManager;
    this._dispatcher = new Dispatcher(modalManager);
    this._currentlyShowingAccountId = undefined;

    this.addListeners = function (goBackCallback) {
        this._viewElements.backButton.addEventListener("click", (e) => goBackCallback(e));
        this._viewElements.refreshButton.addEventListener("click", () => this.refresh());
    }

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function (account) {
        this._currentlyShowingAccountId = account.base64Id;
        this._refetchAndDisplay(this._currentlyShowingAccountId)
        this._container.insertBefore(this._viewElements.view, null);
    }

    this.refresh = function () {
        this._refetchAndDisplay(this._currentlyShowingAccountId, true);
    }

    this._refetchAndDisplay = function (accountId, refreshList = false) {
        clearChildren(this._viewElements.incomingTransfers);
        clearChildren(this._viewElements.outgoingTransfers);

        let p1 = refreshList
            ? this._dispatcher.fetchAccountList(this._user.base64Id)
            : new Promise(resolve => resolve(volatileStorage.get('accountList')));
        p1.then(l => {
            const accountData = l.find(a => a.base64Id === accountId);
            return this._dispatcher.fetchAccountDetails(accountId)
                .then(o => {
                    o.incoming.forEach((t) =>
                        this._viewElements.incomingTransfers.appendChild(this._constructRow(t, "fromId")));
                    o.outgoing.forEach((t) =>
                        this._viewElements.outgoingTransfers.appendChild(this._constructRow(t, "toId")));
                    this._viewElements.accountId.textContent = accountData.base64Id;
                    this._viewElements.accountBalance.textContent = accountData.balance;
                    this._currentlyShowingAccountId = accountId;
                });
        })
            .catch(r => this._modalManager.showError(r));
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

function NewTransferFormManager(container, viewElements, modalManager, afterTransferSuccessful) {
    this._user = volatileStorage.get('user');
    this._container = container;
    this._viewElements = viewElements;
    this._modalManager = modalManager;
    this._dispatcher = new Dispatcher(modalManager);
    this._showingAccount = undefined;
    this._contacts = [];
    this._afterCloseCb = afterTransferSuccessful;

    this.addListeners = function () {
        Object.keys(this._viewElements.formElements).forEach(k => this._setupErrorReporting(this._viewElements.formElements[k]));
        this._viewElements.formElements.payeeId.addEventListener(
            "focus",
            () => {
                this._dispatcher.fetchContacts(this._user)
                    .then(contactList => this._populateContactDatalist(contactList))
                    .catch(r => this._modalManager.showError(r));
            });
        this._viewElements.formElements.payeeId.addEventListener(
            "change",
            (e) => this._checkPayeeId(e.target).catch(() => e.target.checkValidity())
        );
        this._viewElements.formElements.payeeAccount.addEventListener(
            "focus",
            () => {
                this._ifNotEmptyFetchAccounts(this._viewElements.formElements.payeeId.value)
                    .then(list => this._populateAccountDatalist(list))
                    .catch(r => this._viewElements.formElements.payeeAccount.setCustomValidity(r));
            });
        this._viewElements.formElements.payeeAccount.addEventListener(
            "change",
            (e) => {
                this._checkPayeeAccount(this._viewElements.formElements.payeeId.value, e.target)
                    .catch(() => e.target.checkValidity());
            }
        );

        this._viewElements.form.addEventListener("submit", e => {
            this._checkFormValidityAndThen(e.target, () => {
                if (e.target.checkValidity())
                    this._sendFormData(e.target);
            });
            e.preventDefault();
        });
    }

    this._populateContactDatalist = function (contactList) {
        this._contacts = [];
        clearChildren(this._viewElements.contacts);
        for (let i = 0; i < contactList.length; i++) {
            this._contacts.push(contactList[i].contactBase64Id);
            this._viewElements.contacts.appendChild(
                this._createDatalistOption(contactList[i].contactBase64Id));
        }
    }

    this._ifNotEmptyFetchAccounts = function (userId) {
        return new Promise((resolve, reject) => {
            if (userId === undefined || userId === "")
                reject("You must specify a user ID");
            this._dispatcher.fetchAccountList(userId, false)
                .then(list => resolve(list))
                .catch(r => reject(r));
        });
    }

    this._populateAccountDatalist = function (accountList) {
        clearChildren(this._viewElements.payeeAccounts);
        for (let i = 0; i < accountList.length; i++)
            this._viewElements.payeeAccounts.appendChild(
                this._createDatalistOption(accountList[i].base64Id));
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
        return this._checkPayeeId(this._viewElements.formElements.payeeId)
            .then(userId => this._checkPayeeAccount(userId, this._viewElements.formElements.payeeAccount))
            .then(() => then(), () => then());
    }

    this._checkPayeeId = function (target) {
        return this._dispatcher
            .userById(target.value)
            .then(id => {
                target.setCustomValidity("");
                return id;
            })
            .catch(r => {
                target.setCustomValidity(r)
                return new Promise((_, reject) => reject(r));
            });
    }

    this._checkPayeeAccount = function (userId, target) {
        return new Promise((resolve, reject) => {
            if (this._showingAccount.base64Id === target.value) {
                target.setCustomValidity("You cannot transfer money to the same account");
                reject();
            } else if (userId === "" || userId === undefined || userId === null) {
                target.setCustomValidity("No user ID was specified");
                reject();
            } else {
                this._dispatcher.fetchAccountList(userId, false)
                    .then(accountList => {
                        for (let i = 0; i < accountList.length; i++) {
                            if (accountList[i].base64Id === target.value) {
                                target.setCustomValidity("");
                                resolve();
                                return;
                            }
                            target.setCustomValidity("Unable to find an account with the specified ID");
                            reject();
                        }
                    })
                    .catch(() => {
                        target.setCustomValidity("Unable to find an account with the specified ID");
                        reject();
                    });
            }
        });
    }

    this._sendFormData = function (form) {
        const formData = new FormData(form);
        const json = {
            ...convertFormDataToObject(formData),
            fromUserId: this._user.base64Id,
            fromAccountId: this._showingAccount.base64Id
        };

        this._dispatcher
            .newTransfer(json)
            .then(transfer => this._showSuccessModal(transfer, json.toUserId))
            .catch(r => this._modalManager.showError(r));
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
        const modalActions = this._isInContactList(toUserId) || toUserId === this._user.base64Id
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
        this._dispatcher
            .newContact(contactId)
            .catch(r => this._modalManager.showError(r));
    }

    this.removeHiddenClass = function () {
        this._viewElements.view.classList.remove("js");
    }

    this.show = function (account) {
        this._showingAccount = account;
        if (this._container.parent !== null)
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

    volatileStorage.set('user', getUser())
    let userDetailsManager = new UserDetailsManager(
        {
            userId: document.getElementById("userDetails-userId"),
            username: document.getElementById("userDetails-username"),
            email: document.getElementById("userDetails-email"),
            nameSurname: document.getElementById("userDetails-nameSurname")
        }
    )
    let viewOrchestrator = new ViewOrchestrator(
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
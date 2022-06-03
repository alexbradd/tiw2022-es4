function Ajax() {
    this._req = new XMLHttpRequest();

    this._makeReq = function (method, url, data, json, callback) {
        this._req.onreadystatechange = () => callback(this._req);
        this._req.open(method, url);
        if (json)
            this._req.setRequestHeader("content-type", "application/json");
        if (data !== null)
            this._req.send(data);
        else
            this._req.send();
    }

    this.get = function (url, callback) {
        this._makeReq("GET", url, null, false, callback);
    }

    this.post = function (url, data, callback, setJson = true) {
        this._makeReq("POST", url, data, setJson, callback);
    }

    this._refreshTokenAndRetry = function (method, url, objectData, callback) {
        this._makeReq("GET", "/api/auth/refresh", null, false, (req1) => {
            if (req1.readyState !== XMLHttpRequest.DONE)
                return;
            if (req1.status !== 200)
                callback(req1, true);
            let res = JSON.parse(req1.responseText);
            window.sessionStorage.setItem("token", res.token);
            this._makeReq(
                method,
                url,
                JSON.stringify({
                    ...objectData,
                    token: res.token
                }),
                true,
                (req2) => callback(req2, false));
        })
    }

    this.authenticatedPost = function (url, objectData, callback) {
        if (!isLoggedIn())
            throw new Error("User is not logged in");
        this._makeReq(
            "POST",
            url,
            JSON.stringify({
                ...objectData,
                token: window.sessionStorage.getItem("token")
            }),
            true,
            (req) => {
                if (req.readyState === XMLHttpRequest.DONE) {
                    switch (req.status) {
                        case 200:
                            callback(req, false);
                            break;
                        case 401:
                            this._refreshTokenAndRetry("POST", url, objectData, callback);
                            break;
                        default:
                            callback(req, false);
                    }
                } else
                    callback(req, false);
            }
        );
    }
}

function convertFormDataToJSON(formData) {
    let object = {};
    formData.forEach((value, key) => {
        if (!object.hasOwnProperty(key)) {
            object[key] = value;
            return;
        }
        if (!Array.isArray(object[key])) {
            object[key] = [object[key]];
        }
        object[key].push(value);
    });
    return JSON.stringify(object);
}
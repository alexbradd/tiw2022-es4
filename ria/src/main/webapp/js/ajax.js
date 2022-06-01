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
}
var exec = require('cordova/exec');
let appChecker = function () {};
appChecker.coolMethod = function (arg0, success, error) {
    exec(success, error, 'appChecker', 'coolMethod', [arg0]);
};
appChecker.checkUrl = function (arg0, success, error) {
    exec(success, error, 'appChecker', 'checkUrl', [arg0]);
};
appChecker.openApp = function (id,success, error) {
    exec(success, error, 'appChecker', 'openApp', id);
};
module.exports = appChecker;

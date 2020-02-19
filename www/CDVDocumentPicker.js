var exec = require('cordova/exec');


exports.getFile = function (success, error, srcType, fileTypes, title) {
    exec(success, error, 'CDVDocumentPicker', 'getFile', [srcType, fileTypes, title]);
};
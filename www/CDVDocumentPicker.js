var exec = require('cordova/exec');


exports.getFile = function(success, error, srcType, fileTypes, title, isMultiple = false, isCompress = true) {
    exec(success, error, 'CDVDocumentPicker', 'getFile', [srcType, fileTypes, title, isMultiple, isCompress]);
};
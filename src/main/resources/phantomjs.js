//codes.js
system = require('system')
htmlPath = system.args[0];//获得命令行第二个参数 接下来会用到
picPath = system.args[1];
//console.log('Loading a web page');
var page = require('webpage').create();
page.open(htmlPath, function (status) {
    //Page is loaded!
    if (status !== 'success') {
        console.log('Unable to post!');
    } else {
        page.viewportSize = {width: 1024, height: 768};
        page.render(picPath);
    }
    phantom.exit();
});

window.onbeforeunload = function ()
{
    var msoy;
    if (navigator.appName.indexOf("Microsoft") != -1) {
        msoy = window.Msoy;
    } else {
        msoy = document.Msoy[1];
    }
    msoy.msoyLogoff();
}

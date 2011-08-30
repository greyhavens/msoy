window.onbeforeunload = function ()
{
    var msoy;
    if (navigator.appName.indexOf("Microsoft") != -1) {
        msoy = window.Msoy;
    } else {
        msoy = document.Msoy[1];
    }
    msoy.logoff();
}

// -----------------------------------------------------------------------------
// Globals
// Major version of Flash required
var requiredMajorVersion = 9;
// Minor version of Flash required
var requiredMinorVersion = 0;
// Minor version of Flash required
var requiredRevision = 0;

// Check width and heigth params (TODO: verify that they parse, aren't wild)
if (msoyWidth == null || msoyHeight == null) {
    var msoyWidth = 800;
    var msoyHeight = 600;
}

//----------------------------------------------------------------------------
// AC_OETags.js
//----------------------------------------------------------------------------
// Flash Player Version Detection - Rev 1.5
// Detect Client Browser type
// Copyright 2006 Adobe Systems, Inc. All rights reserved.
var isIE  = (navigator.appVersion.indexOf("MSIE") != -1) ? true : false;
var isWin = (navigator.appVersion.toLowerCase().indexOf("win") != -1) ? true : false;
var isOpera = (navigator.userAgent.indexOf("Opera") != -1) ? true : false;

// JavaScript helper required to detect Flash Player PlugIn version information
function GetSwfVer(i){
	// NS/Opera version >= 3 check for Flash plugin in plugin array
	var flashVer = -1;

	if (navigator.plugins != null && navigator.plugins.length > 0) {
		if (navigator.plugins["Shockwave Flash 2.0"] || navigator.plugins["Shockwave Flash"]) {
			var swVer2 = navigator.plugins["Shockwave Flash 2.0"] ? " 2.0" : "";
			var flashDescription = navigator.plugins["Shockwave Flash" + swVer2].description;
			var descArray = flashDescription.split(" ");
			var tempArrayMajor = descArray[2].split(".");
			var versionMajor = tempArrayMajor[0];
			var versionMinor = tempArrayMajor[1];
			if ( descArray[3] != "" ) {
				tempArrayMinor = descArray[3].split("r");
			} else {
				tempArrayMinor = descArray[4].split("r");
			}
			var versionRevision = tempArrayMinor[1] > 0 ? tempArrayMinor[1] : 0;
			var flashVer = versionMajor + "." + versionMinor + "." + versionRevision;
		}
	}
	// MSN/WebTV 2.6 supports Flash 4
	else if (navigator.userAgent.toLowerCase().indexOf("webtv/2.6") != -1) flashVer = 4;
	// WebTV 2.5 supports Flash 3
	else if (navigator.userAgent.toLowerCase().indexOf("webtv/2.5") != -1) flashVer = 3;
	// older WebTV supports Flash 2
	else if (navigator.userAgent.toLowerCase().indexOf("webtv") != -1) flashVer = 2;
	else if ( isIE && isWin && !isOpera ) {
		try{
		      var axo = new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
		      for (var i=3; axo!=null; i++) {
				axo = new ActiveXObject("ShockwaveFlash.ShockwaveFlash."+i);
				flashVer = axo.GetVariable("$version");
		      }
    		}catch(e){}
	}
	return flashVer;
}

// When called with reqMajorVer, reqMinorVer, reqRevision returns true if that version or greater is available
function DetectFlashVer(reqMajorVer, reqMinorVer, reqRevision)
{
 	reqVer = parseFloat(reqMajorVer + "." + reqRevision);
   	// loop backwards through the versions until we find the newest version
	for (i=25;i>0;i--) {
		versionStr = GetSwfVer(i);
		if (versionStr == -1 ) {
			return false;
		} else if (versionStr != 0) {
			if(isIE && isWin && !isOpera) {
				tempArray         = versionStr.split(" ");
				tempString        = tempArray[1];
				versionArray      = tempString .split(",");
			} else {
				versionArray      = versionStr.split(".");
			}
			var versionMajor      = versionArray[0];
			var versionMinor      = versionArray[1];
			var versionRevision   = versionArray[2];

			var versionString     = versionMajor + "." + versionRevision;   // 7.0r24 == 7.24
			var versionNum        = parseFloat(versionString);
        		// is the major.revision >= requested major.revision AND the minor version >= requested minor
			if (versionMajor > reqMajorVer) {
				return true;
			} else if (versionMajor == reqMajorVer) {
				if (versionMinor > reqMinorVer)
					return true;
				else if (versionMinor == reqMinorVer) {
					if (versionRevision >= reqRevision)
						return true;
				}
			}
			return false;
		}
	}
}

//v1.0
function AC_AddExtension(src, ext)
{
  if (src.indexOf('?') != -1)
    return src.replace(/\?/, ext+'?');
  else
    return src + ext;
}

function AC_Generateobj(objAttrs, params, embedAttrs)
{
  var str = '<object ';
  for (var i in objAttrs)
    str += i + '="' + objAttrs[i] + '" ';
  str += '>';
  for (var i in params)
    str += '<param name="' + i + '" value="' + params[i] + '" /> ';
  str += '<embed ';
  for (var i in embedAttrs)
    str += i + '="' + embedAttrs[i] + '" ';
  str += ' ></embed></object>';

  document.write(str);
}

function AC_FL_RunContent(){
  var ret =
    AC_GetArgs
    (  arguments, ".swf", "movie", "clsid:d27cdb6e-ae6d-11cf-96b8-444553540000"
     , "application/x-shockwave-flash"
    );
  AC_Generateobj(ret.objAttrs, ret.params, ret.embedAttrs);
}

function AC_GetArgs(args, ext, srcParamName, classid, mimeType){
  var ret = new Object();
  ret.embedAttrs = new Object();
  ret.params = new Object();
  ret.objAttrs = new Object();
  for (var i=0; i < args.length; i=i+2){
    var currArg = args[i].toLowerCase();

    switch (currArg){
      case "classid":
        break;
      case "pluginspage":
        ret.embedAttrs[args[i]] = args[i+1];
        break;
      case "src":
      case "movie":
        args[i+1] = AC_AddExtension(args[i+1], ext);
        ret.embedAttrs["src"] = args[i+1];
        ret.params[srcParamName] = args[i+1];
        break;
      case "onafterupdate":
      case "onbeforeupdate":
      case "onblur":
      case "oncellchange":
      case "onclick":
      case "ondblClick":
      case "ondrag":
      case "ondragend":
      case "ondragenter":
      case "ondragleave":
      case "ondragover":
      case "ondrop":
      case "onfinish":
      case "onfocus":
      case "onhelp":
      case "onmousedown":
      case "onmouseup":
      case "onmouseover":
      case "onmousemove":
      case "onmouseout":
      case "onkeypress":
      case "onkeydown":
      case "onkeyup":
      case "onload":
      case "onlosecapture":
      case "onpropertychange":
      case "onreadystatechange":
      case "onrowsdelete":
      case "onrowenter":
      case "onrowexit":
      case "onrowsinserted":
      case "onstart":
      case "onscroll":
      case "onbeforeeditfocus":
      case "onactivate":
      case "onbeforedeactivate":
      case "ondeactivate":
      case "type":
      case "codebase":
        ret.objAttrs[args[i]] = args[i+1];
        break;
      case "width":
      case "height":
      case "align":
      case "vspace":
      case "hspace":
      case "class":
      case "title":
      case "accesskey":
      case "name":
      case "id":
      case "tabindex":
        ret.embedAttrs[args[i]] = ret.objAttrs[args[i]] = args[i+1];
        break;
      default:
        ret.embedAttrs[args[i]] = ret.params[args[i]] = args[i+1];
    }
  }
  ret.objAttrs["classid"] = classid;
  if (mimeType) ret.embedAttrs["type"] = mimeType;
  return ret;
}

//----------------------------------------------------------------------------
// history.js
//----------------------------------------------------------------------------
// $Revision: 1.49 $
// Vars
Vars = function(qStr) {
	this.numVars = 0;
	if(qStr != null) {
		var nameValue, name;
		var pairs = qStr.split('&');
		var pairLen = pairs.length;
		for(var i = 0; i < pairLen; i++) {
			var pair = pairs[i];
			if( (pair.indexOf('=')!= -1) && (pair.length > 3) ) {
				var nameValue = pair.split('=');
				var name = nameValue[0];
				var value = nameValue[1];
				if(this[name] == null && name.length > 0 && value.length > 0) {
					this[name] = value;
					this.numVars++;
				}
			}
		}
	}
}
Vars.prototype.toString = function(pre) {
	var result = '';
	if(pre == null) { pre = ''; }
	for(var i in this) {
		if(this[i] != null && typeof(this[i]) != 'object' && typeof(this[i]) != 'function' && i != 'numVars') {
			result += pre + i + '=' + this[i] + '&';
		}
	}
	if(result.length > 0) result = result.substr(0, result.length-1);
	return result;
}
function getSearch(wRef) {
	var searchStr = '';
	if(wRef.location.search.length > 1) {
		searchStr = new String(wRef.location.search);
		searchStr = searchStr.substring(1, searchStr.length);
	}
	return searchStr;
}
var lc_id = Math.floor(Math.random() * 100000).toString(16);
if (this != top)
{
	top.Vars = Vars;
	top.getSearch = getSearch;
	top.lc_id = lc_id;
}


//----------------------------------------------------------------------------
// generated script
//----------------------------------------------------------------------------

// Version check for the Flash Player that has the ability to start Player Product Install (6.0r65)
var hasProductInstall = DetectFlashVer(6, 0, 65);

// Version check based upon the values defined in globals
var hasRequestedVersion = DetectFlashVer(requiredMajorVersion, requiredMinorVersion, requiredRevision);


// Check to see if a player with Flash Product Install is available and the version does not meet the requirements for playback
if ( hasProductInstall && !hasRequestedVersion ) {
        // MMdoctitle is the stored document.title value used by the installation process to close the window that started the process
        // This is necessary in order to close browser windows that are still utilizing the older version of the player after installation has completed
        // DO NOT MODIFY THE FOLLOWING FOUR LINES
        // Location visited after installation is complete if installation is required
        var MMPlayerType = (isIE == true) ? "ActiveX" : "PlugIn";
        var MMredirectURL = window.location;
    document.title = document.title.slice(0, 47) + " - Flash Player Installation";
    var MMdoctitle = document.title;

        AC_FL_RunContent(
                "src", "playerProductInstall",
                "FlashVars", "MMredirectURL="+MMredirectURL+'&MMplayerType='+MMPlayerType+'&MMdoctitle='+MMdoctitle+"",
                "width", msoyWidth,
                "height", msoyHeight,
                "align", "middle",
                "id", "Msoy",
                "quality", "high",
                "bgcolor", "#869ca7",
                "name", "Msoy",
                "allowScriptAccess","always",
                "type", "application/x-shockwave-flash",
                "pluginspage", "http://www.macromedia.com/go/getflashplayer"
        );
} else if (hasRequestedVersion) {
        // if we've detected an acceptable version
        // embed the Flash Content SWF when all tests are passed
        AC_FL_RunContent(
                        "src", "Msoy",
                        "width", msoyWidth,
                        "height", msoyHeight,
                        "align", "middle",
                        "id", "Msoy",
                        "quality", "high",
                        "bgcolor", "#869ca7",
                        "name", "Msoy",
                        "flashvars",'historyUrl=history.htm%3F&lconid=' + lc_id + '',
                        "allowScriptAccess","always",
                        "type", "application/x-shockwave-flash",
                        "pluginspage", "http://www.macromedia.com/go/getflashplayer"
        );
  } else {  // flash is too old or we can't detect the plugin
    var alternateContent = 'Alternate HTML content should be placed here. '
        + 'This content requires the Macromedia Flash Player. '
        + '<a href=http://www.macromedia.com/go/getflash/>Get Flash</a>';
    document.write(alternateContent);  // insert non-flash content
  }


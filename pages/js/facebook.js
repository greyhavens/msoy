/* Annoying Facebook Connect helper crap because GWT + Facebook Connect == Fail. */
window.FB_DoInit = function() {
  FB_RequireFeatures(["Api", "Connect"], function() {
    FB_DoInitCallback();
    FB_DoInitCallback = null;
  });
  return true;
};
window.FB_CheckConnected = function() {
  FB.Bootstrap.ensureInit(function() {
    FB.Connect.ifUserConnected(function(uid) {
      FB_CheckConnectedCallback(uid);
      FB_CheckConnectedCallback = null;
    }, function() {
      FB_CheckConnectedCallback("");
      FB_CheckConnectedCallback = null;
    });
  });
};
window.FB_RequireSession = function() {
  FB.Bootstrap.ensureInit(function() {
    FB.Connect.requireSession(function() {
      FB_RequireSessionCallback(FB.Facebook.apiClient.get_session().uid);
      FB_RequireSessionCallback = null;
    });
  });
};
window.FB_Logout = function() {
  FB.Bootstrap.ensureInit(function() {
    FB.Connect.logout(null);
  });
};

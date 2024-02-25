const applicationServerPublicKey = "<YOUR_APP_PUBLIC_KEY>";
/*
 * you can generate your vapid key from https://web-push-codelab.glitch.me/ or https://d3v.one/vapid-key-generator/
 * you need to put your public key here and your private key in swf.properties in the override directory tree, 
 */

function urlB64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64 = (base64String + padding)
    .replace(/\-/g, '+')
    .replace(/_/g, '/');

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}


function sw_start(callback){
    if ('serviceWorker' in navigator && 'PushManager' in window) {
        navigator.serviceWorker.register('/manifest.js',{ updateViaCache : 'none'} ).then(function(registration) {
          callback && callback(registration);
          console.log('ServiceWorker registration successful with scope: ', registration.scope);
        }, function(err) {
          // registration failed :(
          console.log('ServiceWorker registration failed: ', err);
        });
    }else {
        console.warn('Push messaging is not supported');
    }
}


function registerServiceWorker(callback) {
    sw_start(callback);
}

function subscribe(updateSubscriptionOnServer) {
    registerServiceWorker((swReg) => {
        createSubscription(swReg, updateSubscriptionOnServer);
    });
}

function unsubscribe(updateSubscriptionOnServer) {
    registerServiceWorker((swReg) => {
        removeSubscription(swReg, updateSubscriptionOnServer);
    });
}

function handlePermission(updateSubscriptionOnServer) {
    if (Notification.permission === 'denied') {
        // console.log('Push Messaging Blocked.');
        updateSubscriptionOnServer(null);
        return;
    }
}
function marshall(subscription) {
    var key = subscription.getKey ? subscription.getKey('p256dh') : '';
    var auth = subscription.getKey ? subscription.getKey('auth') : '';
    var subscriptionJSON = {
        keys: {
            p256dh: key ? btoa(String.fromCharCode.apply(null, new Uint8Array(key))) : '',
            auth: auth ? btoa(String.fromCharCode.apply(null, new Uint8Array(auth))) : '',
        },
        endpoint: subscription.endpoint,
    };
    return JSON.stringify(subscriptionJSON);
}

function createSubscription(swRegistration, updateSubscriptionOnServer) {
    var isSubscribed = false;
    swRegistration.pushManager.getSubscription()
        .then(function (subscription) {
            isSubscribed = !(subscription === null);
            if (!isSubscribed) {
                subscribeUser(swRegistration, updateSubscriptionOnServer);
            } else {
                updateSubscriptionOnServer(marshall(subscription));
            }
        }).catch(function (err) {
            subscribeUser(swRegistration,updateSubscriptionOnServer);
        })
}

function removeSubscription(swRegistration, updateSubscriptionOnServer) {
    const applicationServerKey = urlB64ToUint8Array(applicationServerPublicKey);
    swRegistration.pushManager.getSubscription()
        .then(function (subscription) {
            if (subscription) {
                return subscription.unsubscribe();
            }
        })
        .catch(function (error) {
            // console.log('Error unsubscribing', error);
        })
        .then(function () {
            updateSubscriptionOnServer(null);
        });
}

function subscribeUser(swRegistration, updateSubscriptionOnServer) {
    const applicationServerKey = urlB64ToUint8Array(applicationServerPublicKey);

    swRegistration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: applicationServerKey
    }).then(function (subscription) {
        // console.log('User is subscribed.');
        updateSubscriptionOnServer(marshall(subscription));
    }).catch(function (err) {
        // console.log('Failed to subscribe the user: ', err);
        handlePermission(updateSubscriptionOnServer);
    });
}
sw_start();
function clearLocalStorage(){
    //Lockr.rm("User");
}
$(function(){
    const tabs = document.querySelectorAll("div.nav-tabs a.nav-link");
    tabs.forEach((el)=>{
        el.addEventListener("click",(ev)=>{
            ev.preventDefault();
            tabs.forEach((e)=>{
                if (e != el){
                    document.querySelector(e.getAttribute("href")).classList.remove("active");
                    e.classList.remove("active");
/* Focus on first editable field. */
                }else {
                    document.querySelector(e.getAttribute("href")).classList.add("active");
                    e.classList.add("active");
                }
            });
        })
    });
    $("[href='/logout']").on("click",function(ev){
        clearLocalStorage();
    })
/* Focus on first editable field. */
    let menu = $(".navbar-collapse");
    menu.addClass("hidden");

    let navbutton  =  $("button.navbar-toggler");
    navbutton.on("click",function(ev){
        ev.stopPropagation();
        menu.toggleClass("hidden");
    })

    
    const succinctSubmenuItems = document.querySelectorAll(".sub-menu");
    succinctSubmenuItems.forEach((el) => {
        el.addEventListener("click", () => {
            succinctSubmenuItems.forEach((e)=>{
                if (e != el){
                    e.querySelector(".dropdown-menu").classList.add("hidden");
                }
            });
            el.querySelector(".dropdown-menu").classList.toggle("hidden");  
        });
    });
    $("html").on("click",function(ev){
        if ($(ev.target).parents(".navbar-collapse").length == 0){
            if (navbutton.is(":visible") && menu.is(":visible")){
                menu.toggleClass("hidden");
            }else {
                succinctSubmenuItems.forEach((e)=>{
                    e.querySelector(".dropdown-menu").classList.add("hidden");
                });

            }
        }
    });

    document.querySelectorAll(".dropdown-menu").forEach((el)=>{
        el.classList.add("hidden");
    });
    document.querySelectorAll('.code').forEach(el => {
      // then highlight each
      hljs.highlightElement(el);
    });


    
})
function showError(err){
    if (err.response ){
        if (err.response.status === 401){
            if (Lockr.get("SignUp") || Lockr.get("User")){
                window.location.replace("/login");
            }else {
                logout();
            }
        }else if (err.response.status === 413){
             showErrorMessage("Size Uploaded Too Big");
        }else if (err.response.data && err.response.data.SWFHttpResponse.Error) {
            showErrorMessage(err.response.data.SWFHttpResponse.Error)
        }else {
            showErrorMessage(err.response.toString());
        }
    }else {
        showErrorMessage(err.toString());
    }
}
var errorTimeOut = undefined;
function showErrorMessage(msg,duration){
    let time = duration || 1500;
    $("#msg").removeClass("hidden");
    $("#msg").html(msg);
    if (errorTimeOut){
        clearTimeout(errorTimeOut);
        errorTimeOut = undefined;
    }
    return new Promise(function(resolve,reject){
        errorTimeOut = setTimeout(function(){
            $("#msg").addClass("hidden");
            resolve();
        },time);
    });

}
function blank(s){
    return !s || s.length === 0;
}

function logout(ev){
    Lockr.rm("User");
    window.location.replace("/logout"); //Remove session cookie
}



/* Focus on first editable field. 
$(function() {
  $('form:first *:input[type!=hidden]:not([disabled]):not([readonly]):not(.btn):first').focus();


  $("a[name='_SUBMIT_NO_MORE']").click(function (){
                            $(":submit[name='_SUBMIT_NO_MORE']").trigger('click');
                        });
  $("a[name='_SUBMIT_MORE']").click(function (){
                            $(":submit[name='_SUBMIT_MORE']").trigger('click');
                        });
});
*/

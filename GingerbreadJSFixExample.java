package twig.nguyen.codepeeker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class GingerbreadJSFixExample {
	private boolean javascriptInterfaceBroken = false;


	/**
	 * Detect all Gingerbread builds (2.3.x) as it has issues with addJavascriptInterface() and wrap the
	 * jsInterface object so it works.
	 *
	 * This requires any webview $(document).ready() JS code that requires the interface to be moved into
	 * android_init(), which is only called after the JS injection has occurred.
	 *
	 * Method initially discovered by Jason Shah, tweaked by Mr S (StackOverflow) to detect multiple versions of Gingerbread.
	 *
	 * Modified by twig:
	 * - Calls are now synchronous and can return values!
	 * - Still able to use Android.methodName() - does not require change in the way the JS is called
	 * - Now automatically maps all JSInterface methods to interface
	 * - Automatically handle function calls from JS without having to split/tokenize strings manually
	 * - Information passed is now wrapped as JSON, deals with single/double quotes correctly
	 * - No "major" rewrites of JS required.
	 * - Tweaked interface call detection a bit so it's less like to be called by accident.
	 * - Configurable interface name and signature
	 *
	 * Drawbacks:
	 * - Unable to access interface from iframes
	 * - Android interface is not always available, have to wait until the JS injection has been executed - see android_init()
	 * - All JSInterface class methods must only use Strings as parameters.
	 *
	 * @see http://twigstechtips.blogspot.com.au/2013/09/android-webviewaddjavascriptinterface.html
	 */
	protected void fixWebViewJSInterface(WebView webview, Object jsInterface, String jsInterfaceName, String jsSignature) {
		// Gingerbread specific code
		if (Build.VERSION.RELEASE.startsWith("2.3")) {
			javascriptInterfaceBroken = true;
		}
		// Everything else is fine
		else {
			webview.addJavascriptInterface(jsInterface, jsInterfaceName);
		}

		webview.setWebViewClient(new GingerbreadWebViewClient(jsInterface, jsInterfaceName, jsSignature));
		webview.setWebChromeClient(new GingerbreadWebViewChrome(jsInterface, jsSignature));
	}

	/**
	 * Handle JS injection and re-injection to fix the Android 2.3 JSInterface bug.
	 */
	private class GingerbreadWebViewClient extends WebViewClient {
		private Object jsInterface;
		private String jsInterfaceName;
		private String jsSignature;

		public GingerbreadWebViewClient(Object jsInterface, String jsInterfaceName, String jsSignature) {
			this.jsInterface = jsInterface;
			this.jsInterfaceName = jsInterfaceName;
			this.jsSignature = jsSignature;
		}


		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			/*
			 * Inject our own JS into it if Android 2.3 detected.
			 * This is not called until after the page has initialised $(document).ready().
			 */
			if (javascriptInterfaceBroken) {
				StringBuilder gbjs = new StringBuilder();

				gbjs.append("javascript: ");
				gbjs.append(generateJS());

				view.loadUrl(gbjs.toString());
			}

			// Initialise the page
			view.loadUrl("javascript: android_init();");
		}


		/**
		 * What this JS wrapper function does is convert all the arguments to strings,
		 * in JSON format before sending it to Android in the form of a prompt() alert.
		 *
		 * JSON data is returned by Android and unwrapped as the result.
		 */
		public String generateJS() {
			StringBuilder gbjs = new StringBuilder();

			if (javascriptInterfaceBroken) {
				StringBuilder sb;

				gbjs.append("var "); gbjs.append(jsInterfaceName); gbjs.append(" = { " +
						"  _gbFix: function(fxname, xargs) {" +
						"    var args = new Array();" +
						"    for (var i = 0; i < xargs.length; i++) {" +
						"      args.push(xargs[i].toString());" +
						"    };" +
						"    var data = { name: fxname, len: args.length, args: args };" +
						"    var json = JSON.stringify(data);" +
						"    var res = prompt('"); gbjs.append(jsSignature); gbjs.append("' + json);" +
						"    return JSON.parse(res)['result'];" +
						"  }" +
						"};");

						// Build methods for each method in the JSInterface class.
						for (Method m : jsInterface.getClass().getMethods()) {
							sb = new StringBuilder();

							// Output = "Android.showToast = function() { return this._gbFix('showToast', arguments); };"
							sb.append(jsInterfaceName);
							sb.append(".");
							sb.append(m.getName());
							sb.append(" = function() { return this._gbFix('");
							sb.append(m.getName());
							sb.append("', arguments); };");

							gbjs.append(sb);
						}
			}

			return gbjs.toString();
		}
	}



	/**
	 * Handle JS calls to fix the Android 2.3 JSInterface bug.
	 */
	private class GingerbreadWebViewChrome extends WebChromeClient {
		private Object jsInterface;
		private String jsSignature;


		public GingerbreadWebViewChrome(Object jsInterface, String jsSignature) {
			this.jsInterface = jsInterface;
			this.jsSignature = jsSignature;
		}



		@Override
		public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
			if (!javascriptInterfaceBroken || TextUtils.isEmpty(message) || !message.startsWith(jsSignature)) {
				return false;
			}

			// We've hit some code through _gbFix()
			JSONObject jsonData;
			String functionName;
			String encodedData;

			try {
				encodedData = message.substring(jsSignature.length());
				jsonData = new JSONObject(encodedData);
				encodedData = null; // no longer needed, clear memory
				functionName = jsonData.getString("name");

				for (Method m : jsInterface.getClass().getMethods()) {
					if (m.getName().equals(functionName)) {
						JSONArray jsonArgs = jsonData.getJSONArray("args");
						Object[] args = new Object[jsonArgs.length()];

						for (int i = 0; i < jsonArgs.length(); i++) {
							args[i] = jsonArgs.get(i);
						}

						Object ret = m.invoke(jsInterface, args);
						JSONObject res = new JSONObject();
						res.put("result", ret);
						result.confirm(res.toString());
						return true;
					}
				}

				// No matching method name found, should throw an exception.
				throw new RuntimeException("shouldOverrideUrlLoading: Could not find method '" + functionName + "()'.");
			}
			catch (IllegalArgumentException e) {
				Log.e("GingerbreadWebViewClient", "shouldOverrideUrlLoading: Please ensure your JSInterface methods only have String as parameters.");
				throw new RuntimeException(e);
			}
			catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			catch (JSONException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}

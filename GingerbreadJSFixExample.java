package twig.nguyen.codepeeker;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.util.Log;
import android.util.Xml.Encoding;
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
	 * - Still able to use Android.methodName() - does not require change in the way the JS is called
	 * - Now automatically maps all JSInterface methods to interface
	 * - Automatically handle function calls from JS without having to split/tokenize strings manually
	 * - Information passed is now wrapped as JSON, deals with single/double quotes correctly
	 * - No "major" rewrites of JS required.
	 * - Tweaked interface call detection a bit so it's less like to be called by accident.
	 * - Configurable interface name
	 *
	 * Drawbacks:
	 * - Not synchronous, so we can't return values between JS/Java (without callbacks)
	 * - Unable to access interface from iframes
	 *
	 * - Android interface is not always available, have to wait until the JS injection has been executed - see android_init()
	 * - All JSInterface class methods must only use Strings as parameters.
	 *
	 * @see http://twigstechtips.blogspot.com.au/2013/09/android-webviewaddjavascriptinterface.html
	 */
	private void fixWebViewJSInterface(WebView webview, Object jsInterface, String jsInterfaceName) {
		// Gingerbread specific code
		if (Build.VERSION.RELEASE.startsWith("2.3")) {
			javascriptInterfaceBroken = true;
		}
		// Everything else is fine
		else {
			webview.addJavascriptInterface(jsInterface, jsInterfaceName);
		}

		webview.setWebViewClient(new GingerbreadWebViewClient(jsInterface, jsInterfaceName));
	}

	/**
	 * Helper class for fixing the Android 2.3 JSInterface bug.
	 */
	private class GingerbreadWebViewClient extends WebViewClient {
		Object jsInterface;
		String jsInterfaceName;

		public GingerbreadWebViewClient(Object jsInterface, String jsInterfaceName) {
			this.jsInterface = jsInterface;
			this.jsInterfaceName = jsInterfaceName;
		}


		/**
		 * What this JS wrapper function does is convert all the arguments to strings, in JSON format and
		 * URLEncoded before sending it to Android in the form of a HTTP request.
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
						"    var url = encodeURIComponent(JSON.stringify(data));" +
						"    window.location='http://gbjsfix/' + url;" +
						"  }" +
						"};");

				// Build methods for each method in the JSInterface class.
				for (Method m : jsInterface.getClass().getMethods()) {
				    sb = new StringBuilder();

				    // Output = "javascript: Android.showToast = function() { this._gbFix('showToast', arguments); };"
				    sb.append(jsInterfaceName);
				    sb.append(".");
				    sb.append(m.getName());
				    sb.append(" = function() { this._gbFix('");
				    sb.append(m.getName());
				    sb.append("', arguments); };");

				    gbjs.append(sb);
				}
			}

			return gbjs.toString();
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


		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// We've hit some code through _gbFix()
			if (javascriptInterfaceBroken && url.startsWith("http://gbjsfix/")) {
				JSONObject jsonData;
				String decodedData;
				String functionName;
				String encodedData;
				Method method = null;

				encodedData = url.substring("http://gbjsfix/".length());

				try {
					decodedData = URLDecoder.decode(encodedData, Encoding.UTF_8.name());
					jsonData = new JSONObject(decodedData);
					functionName = jsonData.getString("name");

					for (Method m : jsInterface.getClass().getMethods()) {
						if (m.getName().equals(functionName)) {
							method = m;

							JSONArray jsonArgs = jsonData.getJSONArray("args");
							Object[] args = new Object[jsonArgs.length()];

							for (int i = 0; i < jsonArgs.length(); i++) {
								args[i] = jsonArgs.get(i);
							}

							m.invoke(jsInterface, args);
							break;
						}
					}

					// No matching method name found, should throw an exception.
					if (method == null) {
						throw new RuntimeException("shouldOverrideUrlLoading: Could not find method '" + functionName + "()'.");
					}
				}
				catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
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

				// We've handled this ourselves, don't change the window location.
				return true;
			}

	        return super.shouldOverrideUrlLoading(view, url);
		}
	}
}

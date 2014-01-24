/*
    Copyright 2013 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

package de.appplant.cordova.plugin.printer;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;

@TargetApi(19)
public class Printer extends CordovaPlugin
{

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException
    {
        // Let´s check if a print service is available
        if (action.equalsIgnoreCase("isServiceAvailable")) {
            isServiceAvailable(callbackContext);

            return true;
        }

        // Let´s print something
        if (action.equalsIgnoreCase("print")) {
            print(args, callbackContext);

            return true;
        }

        // Returning false results in a "MethodNotFound" error.
        return false;
    }

    /**
     * Checks if a print service is available.
     */
    private void isServiceAvailable (CallbackContext ctx)
    {
        Boolean supported   = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        PluginResult result = new PluginResult(PluginResult.Status.OK, supported);

        ctx.sendPluginResult(result);
    }

    /**
     * Prints the html content.
     */
    private void print (final JSONArray args, CallbackContext ctx)
    {
        final Printer self = this;
        final String content = args.optString(0, "<html></html>");

        cordova.getActivity().runOnUiThread( new Runnable() {
            public void run()
			{
				// Create a WebView object specifically for printing
				WebView webView = new WebView(self.cordova.getActivity());
				webView.setWebViewClient( new WebViewClient()
				{
						public boolean shouldOverrideUrlLoading(WebView view, String url) {
							return false;
						}

						@Override
						public void onPageFinished(WebView view, String url)
						{
							
							// Get a PrintManager instance
							PrintManager printManager = (PrintManager) self.cordova.getActivity()
									.getSystemService(Context.PRINT_SERVICE);

							// Get a print adapter instance
							PrintDocumentAdapter printAdapter = view.createPrintDocumentAdapter();
							
			                // Get a print builder attributes instance
			                PrintAttributes.Builder builder = new PrintAttributes.Builder();
			                builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS); // remove this if you want default margins
			                
			                // Create a print job
							printManager.print("Print document", printAdapter, builder.build());
						}
				});
				
				// Reverse engineer base url (assets/www) from the cordovas webView url
		        String baseURL = self.webView.getUrl();
		        baseURL        = baseURL.substring(0, baseURL.lastIndexOf('/') + 1);
		        
		        // Load content into the print webview
				webView.loadDataWithBaseURL(baseURL, content, "text/HTML", "UTF-8", null);
            }
        });
    }

}

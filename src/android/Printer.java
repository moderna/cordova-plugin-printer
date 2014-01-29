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

/*
   iText 4.2 License: MPL/LGPL
   see: https://github.com/ymasory/iText-4.2.0
*/

package de.appplant.cordova.plugin.printer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.util.Log;

@TargetApi(19)
public class Printer extends CordovaPlugin
{

	private CallbackContext ctx;
	private Intent printIntent;
	
	// change your path on the sdcard here
	private String publicTmpDir = ".de.appplant.cordova.plugin.printer"; // prepending the dot "." will make it hidden
	private String printTitle = "print";
	private final String LOG_TAG = "PRINT";
	
    /**
     * List of print App ids.
     */
    private String printAppIds[] = {
        "kr.co.iconlab.BasicPrintingProfile", 		// Bluetooth Smart Printing
        "com.blueslib.android.app", 				// Bluetooth SPP Printer API
        "com.brother.mfc.brprint", 					// Brother iPrint&Scan
        "com.brother.ptouch.sdk", 					// Brother Print Library
        "jp.co.canon.bsd.android.aepp.activity", 	// Canon Easy-PhotoPrint
        "com.pauloslf.cloudprint", 					// Cloud Print
        "com.dlnapr1.printer", 						// CMC DLNA Print Client
        "com.dell.mobileprint", 					// Dell Mobile Print
        "com.printjinni.app.print", 				// PrintJinni
        "epson.print", 								// Epson iPrint
        "jp.co.fujixerox.prt.PrintUtil.PCL", 		// Fuji Xerox Print Utility
        "jp.co.fujixerox.prt.PrintUtil.Karin", 		// Fuji Xeros Print&Scan (S)
        "com.hp.android.print", 					// HP ePrint" "com.hp.android.print
        "com.blackspruce.lpd", 						// Let's Print Droid
        "com.threebirds.notesprint", 				// NotesPrint print your notes
        "com.xerox.mobileprint", 					// Print Portal (Xerox)
        "com.zebra.kdu", 							// Print Station (Zebra)
        "net.jsecurity.printbot", 					// PrintBot
        "com.sec.print.mobileprint", 				// Samsung Mobile Print
        "com.dynamixsoftware.printhand", 			// PrintHand Mobile Print
        "com.dynamixsoftware.printhand.premium", 	// PrintHand Mobile Print Premium
        "com.rcreations.send2printer", 				// Send 2 Printer
        "com.ivc.starprint", 						// StarPrint
        "com.threebirds.easyviewer", 				// WiFi Print
        "com.woosim.android.print", 				// Woosim BT printer
        "com.woosim.bt.app", 						// WoosimPrinter
        "com.zebra.android.zebrautilities", 		// Zebra Utilities
    };
    
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
    	Boolean supported = false;
    
    	if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
    	{
    		supported = true;
    	}
    	else
    	{
    		supported = (this.getFirstInstalledAppId() != null);
    	}
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
        this.ctx = ctx;

        cordova.getActivity().runOnUiThread( new Runnable() {
            public void run()
			{
            	if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ) // Android 4.4
            	{
            		/*
	            	 * None-Kitkat printing (Android < 4.4)
	            	 */
            	
            		// print with the help of a third party app
	            	String appId = self.getFirstInstalledAppId();
	            	if( appId != null )
	            	{
		            	Log.v(LOG_TAG, "Print app found: " + appId );
		            	
		                self.printIntent = self.getPrintController(appId);
		                self.loadContentIntoPrintController(content, self.printIntent);
	            	}
	            	else
	            	{
	            		Log.v(LOG_TAG, "Error: No print app found.");
	            		PluginResult result = new PluginResult(PluginResult.Status.ERROR, "No print app found.");
	                    self.ctx.sendPluginResult(result);
	            	}
            	}
            	else
            	{
	            	/*
	            	 * Kitkat printing (Android >= 4.4)
	            	 */
	            	
					// Create a WebView object specifically for printing
					WebView webView = new WebView(self.cordova.getActivity());
					webView.getSettings().setJavaScriptEnabled(false);
					webView.getSettings().setDefaultTextEncodingName("utf-8");
					webView.setDrawingCacheEnabled(true);
					webView.setVisibility(View.INVISIBLE);
					// self.cordova.getActivity().addContentView(webView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
				                builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
				                
				                // send success result to cordova
				                PluginResult result = new PluginResult(PluginResult.Status.OK);
				                result.setKeepCallback(false); 
			                    self.ctx.sendPluginResult(result);
				                
				                // Create & send a print job
								printManager.print(self.printTitle, printAdapter, builder.build());
							}
					});
					
					// Reverse engineer base url (assets/www) from the cordova webView url
			        String baseURL = self.webView.getUrl();
			        baseURL        = baseURL.substring(0, baseURL.lastIndexOf('/') + 1);
			        
			        // Load content into the print webview
					webView.loadDataWithBaseURL(baseURL, content, "text/html", "utf-8", null);
            	}

            }
        });
        
        // send "no-result" result
        PluginResult pluginResult = new  PluginResult(PluginResult.Status.NO_RESULT); 
        pluginResult.setKeepCallback(true); 
        ctx.sendPluginResult(pluginResult);
    }
    
    /**
     * Check if an app is installed.
     */
    private boolean isAppInstalled (String appId) {
        PackageManager pm = cordova.getActivity().getPackageManager();

        try {
            PackageInfo pi = pm.getPackageInfo(appId, 0);

            if (pi != null){
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {}

        return false;
    }
    
    /**
     * Get the first installed print app.
     */
    private String getFirstInstalledAppId () {
        for (int i = 0; i < printAppIds.length; i++) {
            String appId = printAppIds[i];
            Boolean isInstalled = this.isAppInstalled(appId);

            if (isInstalled){
                return appId;
            }
        }

        return null;
    }
    
    /**
     * Creates the print intent (aka print controller).
     */
    private Intent getPrintController (String appId)
    {
        String intentId = "android.intent.action.SEND";

        if (appId.equals("com.rcreations.send2printer")) {
            intentId = "com.rcreations.send2printer.print";
        } else if (appId.equals("com.dynamixsoftware.printershare")) {
            intentId = "android.intent.action.VIEW";
        } else if (appId.equals("com.hp.android.print")) {
            intentId = "org.androidprinting.intent.action.PRINT";
        }

        Intent intent = new Intent(intentId);

        if (appId != null)
            intent.setPackage(appId);
        
        String mimeType = "application/pdf";
        
        // Check for special cases that can receive HTML
        if (appId.equals("com.rcreations.send2printer") || appId.equals("com.dynamixsoftware.printershare")) {
            mimeType = "text/html";
        }
        
        intent.setType(mimeType);

        return intent;
    }

    /**
     * Loads the content into the print controller. Either as pdf file or plain html.
     */
    private void loadContentIntoPrintController (String content, Intent intent)
    {
        String mimeType = intent.getType();

        if (mimeType.equals("text/html")) {
            loadContentAsHtmlIntoPrintController(content, intent);
        } else {
        	loadContentAsPdfIntoPrintController(content, intent);
        }
    }

    /**
     * Loads the content into the print intents "EXTRA_TEXT" and calls startPrinterApp() once it´s done.
     */
    private void loadContentAsHtmlIntoPrintController (String content, Intent intent)
    {
    	// Add html text to the intent
        intent.putExtra(Intent.EXTRA_TEXT, content);
        
        // start the print app (trigger the print intent)
        startPrinterApp(this.printIntent);
    }

    /**
     * Loads the html content into a WebView, saves it as a single multi page pdf file and
     * calls startPrinterApp() once it´s done.
     */
    private void loadContentAsPdfIntoPrintController (String content, final Intent intent)
    {
         Activity activity = cordova.getActivity();
        final WebView page = new WebView(activity);
        final Printer self = this;
        
        page.setVisibility(View.INVISIBLE);
        page.getSettings().setJavaScriptEnabled(false);
        page.getSettings().setDefaultTextEncodingName("utf-8");
        page.setDrawingCacheEnabled(true);
        
        page.setWebViewClient( new WebViewClient() {
            @Override
            public void onPageFinished(final WebView page, String url) {
                new Handler().postDelayed( new Runnable() {
                  @Override
                  public void run()
                  {
                        // slice the web screenshot into pages and save as pdf
                        File tmpFile = self.saveWebViewAsPdf(getWebViewAsBitmap(page));

                        // add pdf as stream to the print intent
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));

                        // remove the webview
                        ViewGroup vg = (ViewGroup)(page.getParent());
                        vg.removeView(page);
                        
                        // start the print app (trigger the print intent)
                		self.startPrinterApp(self.printIntent);
                  }
                }, 500);
            }
        });

        // Set base URI to the assets/www folder
        String baseURL = webView.getUrl();
               baseURL = baseURL.substring(0, baseURL.lastIndexOf('/') + 1);

               activity.addContentView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.loadDataWithBaseURL(baseURL, content, "text/html", "utf-8", null);
    }
    
    /**
     * Start the printer app (sends the printer intent).
     */
    private void startPrinterApp (Intent intent)
    {
        // return success answer to cordova
        PluginResult result = new PluginResult(PluginResult.Status.OK, true);
        result.setKeepCallback(false);
        ctx.sendPluginResult(result);
        
        // start intent
        cordova.startActivityForResult(this, intent, 0);
    }
    
    /**
     * Takes a WebView and returns a Bitmap representation of it (takes a "screenshot").
     * @param WebView
     * @return Bitmap
     */
    Bitmap getWebViewAsBitmap(WebView view)
    {
        //Get the dimensions of the view so we can re-layout the view at its current size
        //and create a bitmap of the same size 
        int width = view.getWidth();
        int height = view.getContentHeight();

        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = height; // View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

        //Cause the view to re-layout
        view.measure(measuredWidth, measuredHeight);
        view.layout(0, 0, width, height);//view.getMeasuredHeight());

        //Create a bitmap backed Canvas to draw the view into
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        // draw the view into the canvas
        view.draw(c);
        
        return b;
    }

    /**
     * Slices the screenshot into pages, merges those into a single pdf
     * and saves it in the public accessible /sdcard dir.
     */
    private File saveWebViewAsPdf(Bitmap screenshot) {
        try {
        	
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/" + this.publicTmpDir + "/");
            dir.mkdirs();
            File file;
            FileOutputStream stream;
            
            double pageWidth  = PageSize.A4.getWidth()  * 0.85; // width of the image is 85% of the page
            double pageHeight = PageSize.A4.getHeight() * 0.80; // max height of the image is 80% of the page
            double pageHeightToWithRelation = pageHeight / pageWidth; // e.g.: 1.33 (4/3)
            
            Bitmap currPage;
            int totalSize  = screenshot.getHeight();
            int currPos = 0;
            int currPageCount = 0;
            int sliceWidth = screenshot.getWidth();
            int sliceHeight = (int) Math.round(sliceWidth * pageHeightToWithRelation);
            while( totalSize > currPos && currPageCount < 100  ) // max 100 pages
            {
            	currPageCount++;
            	
            	Log.v(LOG_TAG, "Creating page nr. " + currPageCount );
            	
            	// slice bitmap
            	currPage = Bitmap.createBitmap(screenshot, 0, currPos, sliceWidth, (int) Math.min( sliceHeight, totalSize - currPos ));
            	
            	// save page as png
            	stream = new FileOutputStream( new File(dir, "print-page-"+currPageCount+".png") );
            	currPage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();
                
                // move current position indicator
            	currPos += sliceHeight;
            }
            
            // create pdf
            Document document = new Document();
            File filePdf = new File(dir, this.printTitle + ".pdf"); // change the output name of the pdf here
            PdfWriter.getInstance(document,new FileOutputStream(filePdf));
            document.open();
            for( int i=1; i<=currPageCount; ++i )
            {
            	file = new File(dir, "print-page-"+i+".png");
            	Image image = Image.getInstance (file.getAbsolutePath());
                image.scaleToFit( (float)pageWidth, 9999);
            	image.setAlignment(Element.ALIGN_CENTER);
            	document.add(image);
            }
            document.close();
            
            // delete tmp image files
            for( int i=1; i<=currPageCount; ++i )
            {
            	file = new File(dir, "print-page-"+i+".png");
            	file.delete();
            }
            
            return filePdf;
            
        } catch (IOException e) {
        	Log.e(LOG_TAG, "ERROR: " + e.getMessage());
            e.printStackTrace();
            // return error answer to cordova
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            result.setKeepCallback(false);
            ctx.sendPluginResult(result);
        } catch (DocumentException e) {
        	Log.e(LOG_TAG, "ERROR: " + e.getMessage());
			e.printStackTrace();
            // return error answer to cordova
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            result.setKeepCallback(false);
            ctx.sendPluginResult(result);
		}
        
        Log.e(LOG_TAG, "Uncaught ERROR!");

        return null;
    }

}

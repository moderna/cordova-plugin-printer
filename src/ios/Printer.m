/*
 Copyright 2013 appPlant UG
 Updated by Modern Alchemist OG on 23/04/2014

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

#import "Printer.h"

@interface Printer (Private)

// create print controller
- (UIPrintInteractionController*) getPrintController;

// configure the print settings
- (UIPrintInteractionController*) adjustSettingsForPrintController:(UIPrintInteractionController*)controller;

// loads web content into a print controller
- (void) loadContent:(NSString*)content intoPrintController:(UIPrintInteractionController*)controller;

// Checks if there is a print service
- (BOOL) isPrintServiceAvailable;

@end


@implementation Printer

@synthesize pluginCommand;

/*
 * Is printing available.
 */
- (void) isServiceAvailable:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult;

    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                       messageAsBool:[self isPrintServiceAvailable]];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Creates and opens the print controller.
 */
- (void) print:(CDVInvokedUrlCommand*)command
{
    if (![self isPrintServiceAvailable])
    {
        return;
    }

    self.pluginCommand = command;
    
    NSArray*  arguments  = [command arguments];
    NSString* content    = [arguments objectAtIndex:0];

    UIPrintInteractionController* controller = [self getPrintController];

    [self adjustSettingsForPrintController:controller];
    [self loadContent:content intoPrintController:controller];

    [self openPrintController:controller];

    [self commandDelegate];
}

/**
 * Creates the print controller.
 */
- (UIPrintInteractionController*) getPrintController
{
    return [UIPrintInteractionController sharedPrintController];
}

/**
 * Configure the printer.
 */
- (UIPrintInteractionController*) adjustSettingsForPrintController:(UIPrintInteractionController*)controller
{
    UIPrintInfo* printInfo    = [UIPrintInfo printInfo];
    printInfo.outputType      = UIPrintInfoOutputGeneral;
    controller.printInfo      = printInfo;
    controller.showsPageRange = NO;

    return controller;
}

/**
 * Loads the html content into a printable webview.
 */
- (void) loadContent:(NSString*)content intoPrintController:(UIPrintInteractionController*)controller
{
    // Set the base URL to be the www directory.
    NSString* wwwFilePath = [[NSBundle mainBundle] pathForResource:@"www" ofType:nil];
    NSURL*    baseURL     = [NSURL fileURLWithPath:wwwFilePath];
    // Load page into a webview and use its formatter to print the page
    UIWebView* webPage    = [[UIWebView alloc] init];

    [webPage loadHTMLString:content baseURL:baseURL];
    
    NSLog(@"base url: %@", baseURL);

    // Get formatter for web (note: margin not required - done in web page)
    UIViewPrintFormatter* formatter = [webPage viewPrintFormatter];

    controller.printFormatter = formatter;
}

/**
 * Show the print controller.
 */
- (void) openPrintController:(UIPrintInteractionController*)controller
{
    // We need a completion handler block for printing.
    UIPrintInteractionCompletionHandler completionHandler = ^(UIPrintInteractionController *printController, BOOL completed, NSError *error)
    {
        if(completed)
        {
            CDVPluginResult* pluginResult;
            if( error )
            {
                NSLog(@"Printing failed due to error in domain %@ with error code %u", error.domain, error.code);
                
                // create cordova result
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                 messageAsString:[@"Printing failed" stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
                // send cordova result
                [self.commandDelegate sendPluginResult:pluginResult callbackId:self.pluginCommand.callbackId];
            }
            else
            {
                // create cordova result
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                // send cordova result
                [self.commandDelegate sendPluginResult:pluginResult callbackId:self.pluginCommand.callbackId];
            }
        }
    };
    
    [controller presentAnimated:YES completionHandler:completionHandler];
}

/**
 * Check if there is a print service available.
 */
- (BOOL) isPrintServiceAvailable
{
    Class printController = NSClassFromString(@"UIPrintInteractionController");

    if (printController)
    {
        UIPrintInteractionController* controller = [UIPrintInteractionController sharedPrintController];

        return (controller != nil) && [UIPrintInteractionController isPrintingAvailable];
    }

    return NO;
}

@end

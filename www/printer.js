cordova.define("de.appplant.cordova.plugin.printer.Printer", function(require, exports, module) {/*
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

    var Printer = function () {

    };

    Printer.prototype = {
        /**
         * Checks if the printer service is available.
         *
         * @param {Function} callback
         */
        isServiceAvailable: function (callback) {
            cordova.exec(callback, null, 'Printer', 'isServiceAvailable', []);
        },

        /**
         * Prints the html content.
         *
         * @param {String}  content HTML string or DOM node (if latter, innerHTML is used to get the contents)
         * @param {Object?} options platform specific options
         * @param {Function} successCallback
         * @param {Function} errorCallback
         */
        print: function (content, options, successCallback, errorCallback) {
            var page    = content.innerHTML || content,
                options = options || {};

            if (typeof page != 'string') {
                console.log('Print function requires an HTML string. Not an object');
                return;
            }

            cordova.exec(successCallback, errorCallback, 'Printer', 'print', [page, options]);
        }
    };

    var plugin = new Printer();

    module.exports = plugin;});

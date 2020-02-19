/*
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
package org.apache.cordova.CDVDocumentPicker;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;

import org.apache.cordova.BuildHelper;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CDVDocumentPicker extends CordovaPlugin {

    private static final int PHOTOLIBRARY = 0;          // Choose image from picture library (same as SAVEDPHOTOALBUM for Android)
    private static final int SAVEDPHOTOALBUM = 2;       // Choose image from picture library (same as PHOTOLIBRARY for Android)

    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final int SAVE_TO_ALBUM_SEC = 1;
	public static final int DOCUMENT_PICKER = 111;

    private static final String LOG_TAG = "CDVDocumentPicker";

    private static final String TIME_FORMAT = "yyyyMMdd_HHmmss";

    private String[] fileTypes;                  // What type of media to retrieve
    private String title;                   
    private int srcType;                    // Destination type (needs to be saved for permission handling)
    private boolean allowEdit;              // Should we allow the user to crop the image.

    protected final static String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};

    public CallbackContext callbackContext;
    private String applicationId;


    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  A PluginResult object with a status and message.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        //Adding an API to CoreAndroid to get the BuildConfigValue
        //This allows us to not make this a breaking change to embedding
        this.applicationId = (String) BuildHelper.getBuildConfigValue(cordova.getActivity(), "APPLICATION_ID");
        this.applicationId = preferences.getString("applicationId", this.applicationId);


        if (action.equals("getFile")) {
            this.srcType = PHOTOLIBRARY;

            //Take the values from the arguments if they're not already defined (this is tricky)
            // this.srcType = args.getInt(0); //Android 不需要
            this.fileTypes = args.getStringArray(1);

			for(int i=0;i<this.fileTypes.length;i++) {
				this.fileTypes[0] = this.formatFileType(this.fileTypes[0]);
			}

			this.title = args.getString(2);
            this.allowEdit = false;

            try {
                    // FIXME: Stop always requesting the permission
                    if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        PermissionHelper.requestPermission(this, SAVE_TO_ALBUM_SEC, Manifest.permission.READ_EXTERNAL_STORAGE);
                    } else {
                        this.getFile(this.srcType, title);
                    }
            }
            catch (IllegalArgumentException e)
            {
                callbackContext.error("Illegal Argument Exception");
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                callbackContext.sendPluginResult(r);
                return true;
            }

            PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
            r.setKeepCallback(true);
            callbackContext.sendPluginResult(r);

            return true;
        }
        return false;
    }

    /**
     * Get image from photo library.
     *
     * @param srcType           The album to get image from.
     * @param returnType        Set the type of image to return.
     * @param encodingType
     */
    // TODO: Images selected from SDCARD don't display correctly, but from CAMERA ALBUM do!
    public void getFile(int srcType, String title) {
        Intent intent = new Intent();
        String title = title;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			intent.setType(this.fileTypes.length == 1 ? this.fileTypes[0] : "*/*");
			if (this.fileTypes.length > 0) {
			   intent.putExtra(Intent.EXTRA_MIME_TYPES, this.fileTypes);
			}
		} else {
			String this.fileTypesStr = "";
			for (String mimeType : this.fileTypes) {
				this.fileTypesStr += mimeType + "|";
			}
			intent.setType(this.fileTypesStr.substring(0,this.fileTypesStr.length() - 1));
		}

		intent.setAction(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (this.cordova != null) {
            this.cordova.startActivityForResult((CordovaPlugin) this, Intent.createChooser(intent,
                    new String(title)), DOCUMENT_PICKER);
        }
    }

    /**
     * Called when the file view exits.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK && intent != null) {
			final Intent i = intent;
			 Uri uri = intent.getData();
			if (uri == null) {
				this.failFile("null data from photo library");
				return;
			}

			String fileLocation = FileHelper.getRealPath(uri, this.cordova);
			LOG.d(LOG_TAG, "File location is: " + fileLocation);

			String uriString = uri.toString();
			 LOG.d(LOG_TAG, "File URI is: " + uriString);
			this.callbackContext.success(fileLocation);
			
		} else if (resultCode == Activity.RESULT_CANCELED) {
			this.failFile("No File Selected");
		} else {
			this.failFile("Selection did not complete!");
		}
    }


    /**
     * Send error message to JavaScript.
     *
     * @param err
     */
    public void failFile(String err) {
        this.callbackContext.error(err);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }
        switch (requestCode) {
            case SAVE_TO_ALBUM_SEC:
                this.getFile(this.srcType, this.title);
                break;
        }
    }

    /**
     * Taking or choosing a picture launches another Activity, so we need to implement the
     * save/restore APIs to handle the case where the CordovaActivity is killed by the OS
     * before we get the launched Activity's result.
     */
    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putString("title", this.title);
        state.putInt("srcType", this.srcType);
        state.putInt("fileTypes", this.fileTypes);
		state.putBoolean("allowEdit", this.allowEdit);

        return state;
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.title = state.getInt("title");
        this.srcType = state.getInt("srcType");
        this.fileTypes = state.getStringArray("fileTypes");
        this.allowEdit = state.getBoolean("allowEdit");

        this.callbackContext = callbackContext;
    }

	private String formatFileType(String filetype){
		switch (filetype) {
        case "pdf":
        case "com.adobe.pdf":
            return "application/pdf";
            break;
        case "doc":
        case "com.microsoft.word.doc":
            return "application/msword";
            break;
        case "docx":
        case "org.openxmlformats.wordprocessingml.document":
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            break;
        case "xls":
        case "com.microsoft.excel.xls":
            return "application/vnd.ms-excel";
            break;
        case "xlsx":
        case "org.openxmlformats.spreadsheetml.sheet":
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            break;
        case "ppt":
        case "com.microsoft.powerpoint.​ppt":
            return "application/mspowerpoint";
            break;
        case "pptx":
        case "org.openxmlformats.presentationml.presentation":
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            break;
        default:
            return filetype;
            break;
        }				
	}
     /*
      * This is dirty, but it does the job.
      *
      * Since the FilesProvider doesn't really provide you a way of getting a URL from the file,
      * and since we actually need the Camera to create the file for us most of the time, we don't
      * actually write the file, just generate the location based on a timestamp, we need to get it
      * back from the Intent.
      *
      * However, the FilesProvider preserves the path, so we can at least write to it from here, since
      * we own the context in this case.
     */
    private String getFileNameFromUri(Uri uri) {
        String fullUri = uri.toString();
        String partial_path = fullUri.split("external_files")[1];
        File external_storage = Environment.getExternalStorageDirectory();
        String path = external_storage.getAbsolutePath() + partial_path;
        return path;
    }
}

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
package com.plugin.document;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
 
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class CDVFileHelper {
    private static final String LOG_TAG = "FileUtils";
    private static final String _DATA = "_data";
    public static final String DOCUMENTS_DIR = "documents";
    /**
     * Returns the real path of the given URI string.
     * If the given URI string represents a content:// URI, the real path is retrieved from the media store.
     *
     * @param uriString the URI string of the audio/image/video
     * @param cordova the current application context
     * @return the full path to the file
     */
    @SuppressWarnings("deprecation")
    public static String getRealPath(Uri uri, CordovaInterface cordova) {
        String realPath = null;

        if (Build.VERSION.SDK_INT < 11)
            realPath = CDVFileHelper.getRealPathFromURI_BelowAPI11(cordova.getActivity(), uri);

        // SDK >= 11
        else
            realPath = CDVFileHelper.getRealPathFromURI_API11_And_Above(cordova.getActivity(), uri);

        return realPath;
    }

    /**
     * Returns the real path of the given URI.
     * If the given URI is a content:// URI, the real path is retrieved from the media store.
     *
     * @param uri the URI of the audio/image/video
     * @param cordova the current application context
     * @return the full path to the file
     */
    public static String getRealPath(String uriString, CordovaInterface cordova) {
        return CDVFileHelper.getRealPath(Uri.parse(uriString), cordova);
    }

    @SuppressLint("NewApi")
    public static String getRealPathFromURI_API11_And_Above(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                if (id != null && id.length() > 0) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
//                        final Uri contentUri = ContentUris.withAppendedId(
//                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
//
//                        return getDataColumn(context, contentUri, null, null);
//
                      String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads",
                        "content://downloads/all_downloads"
                      };

                      for (String contentUriPrefix : contentUriPrefixesToTry) {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                        try {
                          String path = getDataColumn(context, contentUri, null, null);
                          if (path != null && !path.equals("")) {
                            return path;
                          }
                        } catch (Exception e) {
                        }
                      }

                      // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
                      String fileName = getFileName(context, uri);
                      File cacheDir = getDocumentCacheDir(context);
                      File file = generateFileName(fileName, cacheDir);
                      String destinationPath = null;
                      if (file != null) {
                        destinationPath = file.getAbsolutePath();
                        saveFileFromUri(context, uri, destinationPath);
                      }

                      return destinationPath;



                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        String result = null;

        try {
            Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);

        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    /**
     * Returns an input stream based on given URI string.
     *
     * @param uriString the URI string from which to obtain the input stream
     * @param cordova the current application context
     * @return an input stream into the data at the given URI or null if given an invalid URI string
     * @throws IOException
     */
    public static InputStream getInputStreamFromUriString(String uriString, CordovaInterface cordova)
            throws IOException {
        InputStream returnValue = null;
        if (uriString.startsWith("content")) {
            Uri uri = Uri.parse(uriString);
            returnValue = cordova.getActivity().getContentResolver().openInputStream(uri);
        } else if (uriString.startsWith("file://")) {
            int question = uriString.indexOf("?");
            if (question > -1) {
                uriString = uriString.substring(0, question);
            }
            if (uriString.startsWith("file:///android_asset/")) {
                Uri uri = Uri.parse(uriString);
                String relativePath = uri.getPath().substring(15);
                returnValue = cordova.getActivity().getAssets().open(relativePath);
            } else {
                // might still be content so try that first
                try {
                    returnValue = cordova.getActivity().getContentResolver().openInputStream(Uri.parse(uriString));
                } catch (Exception e) {
                    returnValue = null;
                }
                if (returnValue == null) {
                    returnValue = new FileInputStream(getRealPath(uriString, cordova));
                }
            }
        } else {
            returnValue = new FileInputStream(uriString);
        }
        return returnValue;
    }

    /**
     * Removes the "file://" prefix from the given URI string, if applicable.
     * If the given URI string doesn't have a "file://" prefix, it is returned unchanged.
     *
     * @param uriString the URI string to operate on
     * @return a path without the "file://" prefix
     */
    public static String stripFileProtocol(String uriString) {
        if (uriString.startsWith("file://")) {
            uriString = uriString.substring(7);
        }
        return uriString;
    }

    public static String getMimeTypeForExtension(String path) {
        String extension = path;
        int lastDot = extension.lastIndexOf('.');
        if (lastDot != -1) {
            extension = extension.substring(lastDot + 1);
        }
        // Convert the URI string to lower case to ensure compatibility with MimeTypeMap (see CB-2185).
        extension = extension.toLowerCase(Locale.getDefault());
        if (extension.equals("3ga")) {
            return "audio/3gpp";
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
    
    /**
     * Returns the mime type of the data specified by the given URI string.
     *
     * @param uriString the URI string of the data
     * @return the mime type of the specified data
     */
    public static String getMimeType(String uriString, CordovaInterface cordova) {
        String mimeType = null;

        Uri uri = Uri.parse(uriString);
        if (uriString.startsWith("content://")) {
            mimeType = cordova.getActivity().getContentResolver().getType(uri);
        } else {
            mimeType = getMimeTypeForExtension(uri.getPath());
        }

        return mimeType;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

  public static String getFileName(@NonNull Context context, Uri uri) {
    String mimeType = context.getContentResolver().getType(uri);
    String filename = null;

    if (mimeType == null && context != null) {
      String path = CDVFileHelper.getRealPathFromURI_API11_And_Above(context, uri);// getRealPath(uri, this.cordova);
      if (path == null) {
        filename = getName(uri.toString());
      } else {
        File file = new File(path);
        filename = file.getName();
      }
    } else {
      Cursor returnCursor = context.getContentResolver().query(uri, null,
        null, null, null);
      if (returnCursor != null) {
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        filename = returnCursor.getString(nameIndex);
        returnCursor.close();
      }
    }

    return filename;
  }

  public static String getName(String filename) {
    if (filename == null) {
      return null;
    }
    int index = filename.lastIndexOf('/');
    return filename.substring(index + 1);
  }

  public static File getDocumentCacheDir(@NonNull Context context) {
    File dir = new File(context.getCacheDir(), DOCUMENTS_DIR);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    return dir;
  }

  @Nullable
  public static File generateFileName(@Nullable String name, File directory) {
    if (name == null) {
      return null;
    }

    File file = new File(directory, name);

    if (file.exists()) {
      String fileName = name;
      String extension = "";
      int dotIndex = name.lastIndexOf('.');
      if (dotIndex > 0) {
        fileName = name.substring(0, dotIndex);
        extension = name.substring(dotIndex);
      }

      int index = 0;

      while (file.exists()) {
        index++;
        name = fileName + '(' + index + ')' + extension;
        file = new File(directory, name);
      }
    }

    try {
      if (!file.createNewFile()) {
        return null;
      }
    } catch (IOException e) {
      return null;
    }

    return file;
  }


  private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
    InputStream is = null;
    BufferedOutputStream bos = null;
    try {
      is = context.getContentResolver().openInputStream(uri);
      bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
      byte[] buf = new byte[1024];
      is.read(buf);
      do {
        bos.write(buf);
      } while (is.read(buf) != -1);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (is != null) is.close();
        if (bos != null) bos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}

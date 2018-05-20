package com.example.gabriel.shushme.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PlaceContentProvider extends ContentProvider {

    public static final int PLACES = 100;
    public static final int PLACES_WITH_ID = 101;

    private PlaceDbHelper mDbHelper;
    private static final UriMatcher mUriMatcher = buildUriMatcher();

    public static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PlaceContract.AUTHORITY, PlaceContract.PATH_PLACES, PLACES);
        uriMatcher.addURI(PlaceContract.AUTHORITY, PlaceContract.PATH_PLACES + "/#", PLACES_WITH_ID);

        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mDbHelper = new PlaceDbHelper(context);

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final SQLiteDatabase sqLiteDatabase = mDbHelper.getWritableDatabase();
        Cursor cursor;

        switch (mUriMatcher.match(uri)) {
            case PLACES:
                cursor = sqLiteDatabase.query(
                    PlaceContract.PlaceEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final SQLiteDatabase sqLiteDatabase = mDbHelper.getWritableDatabase();
        Uri returnUri;

        switch (mUriMatcher.match(uri)) {
            case PLACES:
                long id = sqLiteDatabase.insert(PlaceContract.PlaceEntry.TABLE_NAME, null, values);

                if (!(id > 0)) {
                    throw new SQLException("Failed to insert row into " + uri);
                }

                returnUri = ContentUris.withAppendedId(PlaceContract.PlaceEntry.CONTENT_URI, id);

                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs)   {
        final SQLiteDatabase sqLiteDatabase = mDbHelper.getWritableDatabase();
        int deleted;

        switch (mUriMatcher.match(uri)) {
            case PLACES_WITH_ID:
                String id = uri.getPathSegments().get(1);
                deleted = sqLiteDatabase.delete(PlaceContract.PlaceEntry.TABLE_NAME, "_id = ?", new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (deleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return deleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase sqLiteDatabase = mDbHelper.getWritableDatabase();
        int deleted;

        switch (mUriMatcher.match(uri)) {
            case PLACES_WITH_ID:
                String id = uri.getPathSegments().get(1);
                deleted = sqLiteDatabase.update(
                    PlaceContract.PlaceEntry.TABLE_NAME,
                    values,
                    "_id = ?",
                    new String[]{id}
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (deleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return deleted;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

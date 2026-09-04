package com.boldtechnology.nailsalonregister;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class SalonDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "nail_salon.db";
    private static final int DB_VERSION = 1;

    public SalonDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE services (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "price_cents INTEGER NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id TEXT NOT NULL," +
                "service_summary TEXT NOT NULL," +
                "total_cents INTEGER NOT NULL," +
                "payment_method TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");

        insertService(db, "Classic Manicure", 2500);
        insertService(db, "Gel Manicure", 4000);
        insertService(db, "Classic Pedicure", 4000);
        insertService(db, "Gel Pedicure", 5500);
        insertService(db, "Acrylic Full Set", 6000);
        insertService(db, "Acrylic Fill", 4500);
        insertService(db, "Dip Powder", 5000);
        insertService(db, "Nail Art", 1000);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema migrations will be placed here.
    }

    private static void insertService(SQLiteDatabase db, String name, long cents) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("price_cents", cents);
        db.insertOrThrow("services", null, values);
    }

    public List<ServiceItem> getServices() {
        List<ServiceItem> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "services", new String[]{"id", "name", "price_cents"},
                "active = 1", null, null, null, "name COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                result.add(new ServiceItem(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)));
            }
        }
        return result;
    }

    public long addService(String name, long priceCents) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("price_cents", priceCents);
        values.put("active", 1);
        return getWritableDatabase().insertOrThrow("services", null, values);
    }

    public void updateService(long id, String name, long priceCents) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("price_cents", priceCents);
        getWritableDatabase().update("services", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void archiveService(long id) {
        ContentValues values = new ContentValues();
        values.put("active", 0);
        getWritableDatabase().update("services", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public long saveSale(String customerId, String serviceSummary, long totalCents, String paymentMethod) {
        ContentValues values = new ContentValues();
        values.put("customer_id", customerId);
        values.put("service_summary", serviceSummary);
        values.put("total_cents", totalCents);
        values.put("payment_method", paymentMethod);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("sales", null, values);
    }

    public List<SaleItem> getRecentSales(int limit) {
        List<SaleItem> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "sales",
                new String[]{"id", "customer_id", "service_summary", "total_cents", "payment_method", "created_at"},
                null, null, null, null, "created_at DESC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                result.add(new SaleItem(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3),
                        cursor.getString(4), cursor.getLong(5)));
            }
        }
        return result;
    }

    public long getTodayTotal() {
        long start = startOfToday();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total_cents), 0) FROM sales WHERE created_at >= ?",
                new String[]{String.valueOf(start)})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    public int getTodayCount() {
        long start = startOfToday();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM sales WHERE created_at >= ?",
                new String[]{String.valueOf(start)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private static long startOfToday() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}

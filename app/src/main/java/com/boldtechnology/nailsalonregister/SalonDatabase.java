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
    private static final int DB_VERSION = 2;

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
        createRevisionTables(db);

        insertService(db, "Classic Manicure", 2500);
        insertService(db, "Gel Manicure", 4000);
        insertService(db, "Classic Pedicure", 4000);
        insertService(db, "Gel Pedicure", 5500);
        insertService(db, "Acrylic Full Set", 6000);
        insertService(db, "Acrylic Fill", 4500);
        insertService(db, "Dip Powder", 5000);
        insertService(db, "Nail Art", 1000);
    }

    private static void createRevisionTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_group_id INTEGER NOT NULL DEFAULT 0," +
                "revision_number INTEGER NOT NULL DEFAULT 1," +
                "is_current INTEGER NOT NULL DEFAULT 1," +
                "customer_id TEXT NOT NULL," +
                "service_summary TEXT NOT NULL," +
                "subtotal_cents INTEGER NOT NULL," +
                "tip_cents INTEGER NOT NULL DEFAULT 0," +
                "total_cents INTEGER NOT NULL," +
                "payment_method TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE sale_services (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_id INTEGER NOT NULL," +
                "source_service_id INTEGER NOT NULL DEFAULT 0," +
                "service_name TEXT NOT NULL," +
                "price_cents INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_sales_current_created ON sales(is_current, created_at)");
        db.execSQL("CREATE INDEX idx_sales_group_revision ON sales(sale_group_id, revision_number)");
        db.execSQL("CREATE INDEX idx_sale_services_sale ON sale_services(sale_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Upgrade version 1 in place; existing payment history is never deleted.
            db.execSQL("ALTER TABLE sales ADD COLUMN sale_group_id INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE sales ADD COLUMN revision_number INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE sales ADD COLUMN is_current INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE sales ADD COLUMN subtotal_cents INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE sales ADD COLUMN tip_cents INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE sales ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE sales SET sale_group_id = id, subtotal_cents = total_cents, " +
                    "tip_cents = 0, updated_at = created_at");
            db.execSQL("CREATE TABLE sale_services (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sale_id INTEGER NOT NULL," +
                    "source_service_id INTEGER NOT NULL DEFAULT 0," +
                    "service_name TEXT NOT NULL," +
                    "price_cents INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX idx_sales_current_created ON sales(is_current, created_at)");
            db.execSQL("CREATE INDEX idx_sales_group_revision ON sales(sale_group_id, revision_number)");
            db.execSQL("CREATE INDEX idx_sale_services_sale ON sale_services(sale_id)");
        }
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

    public long saveSale(String customerId, List<ServiceItem> services, long subtotalCents,
                         long tipCents, String paymentMethod) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            long saleId = insertSale(db, 0, 1, customerId, serviceSummary(services),
                    subtotalCents, tipCents, paymentMethod, now, now);
            ContentValues group = new ContentValues();
            group.put("sale_group_id", saleId);
            db.update("sales", group, "id = ?", new String[]{String.valueOf(saleId)});
            insertSaleServices(db, saleId, services);
            db.setTransactionSuccessful();
            return saleId;
        } finally {
            db.endTransaction();
        }
    }

    public long reviseSale(SaleItem previous, String customerId, List<ServiceItem> services,
                           long subtotalCents, long tipCents, String paymentMethod) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int currentRevision;
            long groupId;
            long createdAt;
            try (Cursor cursor = db.query("sales",
                    new String[]{"sale_group_id", "revision_number", "created_at", "is_current"},
                    "id = ?", new String[]{String.valueOf(previous.id)}, null, null, null)) {
                if (!cursor.moveToFirst() || cursor.getInt(3) != 1) {
                    throw new IllegalStateException("This payment already has a newer revision");
                }
                groupId = cursor.getLong(0);
                currentRevision = cursor.getInt(1);
                createdAt = cursor.getLong(2);
            }

            ContentValues oldRevision = new ContentValues();
            oldRevision.put("is_current", 0);
            db.update("sales", oldRevision, "id = ?", new String[]{String.valueOf(previous.id)});

            long now = System.currentTimeMillis();
            long newId = insertSale(db, groupId, currentRevision + 1, customerId,
                    serviceSummary(services), subtotalCents, tipCents, paymentMethod, createdAt, now);
            insertSaleServices(db, newId, services);
            db.setTransactionSuccessful();
            return newId;
        } finally {
            db.endTransaction();
        }
    }

    private static long insertSale(SQLiteDatabase db, long groupId, int revisionNumber,
                                   String customerId, String summary, long subtotalCents,
                                   long tipCents, String paymentMethod, long createdAt, long updatedAt) {
        ContentValues values = new ContentValues();
        values.put("sale_group_id", groupId);
        values.put("revision_number", revisionNumber);
        values.put("is_current", 1);
        values.put("customer_id", customerId);
        values.put("service_summary", summary);
        values.put("subtotal_cents", subtotalCents);
        values.put("tip_cents", tipCents);
        values.put("total_cents", subtotalCents + tipCents);
        values.put("payment_method", paymentMethod);
        values.put("created_at", createdAt);
        values.put("updated_at", updatedAt);
        return db.insertOrThrow("sales", null, values);
    }

    private static void insertSaleServices(SQLiteDatabase db, long saleId, List<ServiceItem> services) {
        for (ServiceItem service : services) {
            ContentValues values = new ContentValues();
            values.put("sale_id", saleId);
            values.put("source_service_id", Math.max(service.id, 0));
            values.put("service_name", service.name);
            values.put("price_cents", service.priceCents);
            db.insertOrThrow("sale_services", null, values);
        }
    }

    public List<ServiceItem> getSaleServices(SaleItem sale) {
        List<ServiceItem> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "sale_services", new String[]{"source_service_id", "service_name", "price_cents"},
                "sale_id = ?", new String[]{String.valueOf(sale.id)}, null, null, "id")) {
            while (cursor.moveToNext()) {
                result.add(new ServiceItem(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)));
            }
        }
        // Version 1 rows did not have item snapshots. Preserve their complete saved summary.
        if (result.isEmpty() && sale.subtotalCents > 0) {
            result.add(new ServiceItem(-sale.id, sale.serviceSummary, sale.subtotalCents));
        }
        return result;
    }

    public List<SaleItem> getRecentSales(int limit) {
        return querySales("s.is_current = 1", null, "s.created_at DESC", String.valueOf(limit));
    }

    public List<SaleItem> getSaleRevisions(long groupId) {
        return querySales("s.sale_group_id = ?", new String[]{String.valueOf(groupId)},
                "s.revision_number DESC", null);
    }

    private List<SaleItem> querySales(String selection, String[] args, String orderBy, String limit) {
        List<SaleItem> result = new ArrayList<>();
        String sql = "SELECT s.id, s.sale_group_id, s.revision_number, " +
                "(SELECT COUNT(*) FROM sales r WHERE r.sale_group_id = s.sale_group_id), " +
                "s.customer_id, s.service_summary, s.subtotal_cents, s.tip_cents, s.total_cents, " +
                "s.payment_method, s.created_at, s.updated_at FROM sales s WHERE " + selection +
                " ORDER BY " + orderBy + (limit == null ? "" : " LIMIT " + limit);
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                result.add(new SaleItem(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2),
                        cursor.getInt(3), cursor.getString(4), cursor.getString(5), cursor.getLong(6),
                        cursor.getLong(7), cursor.getLong(8), cursor.getString(9), cursor.getLong(10),
                        cursor.getLong(11)));
            }
        }
        return result;
    }

    public long getTodayTotal() {
        long start = startOfToday();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total_cents), 0) FROM sales " +
                        "WHERE is_current = 1 AND created_at >= ?",
                new String[]{String.valueOf(start)})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    public int getTodayCount() {
        long start = startOfToday();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM sales WHERE is_current = 1 AND created_at >= ?",
                new String[]{String.valueOf(start)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private static String serviceSummary(List<ServiceItem> services) {
        StringBuilder builder = new StringBuilder();
        for (ServiceItem service : services) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(service.name).append(" (").append(Money.format(service.priceCents)).append(")");
        }
        return builder.toString();
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

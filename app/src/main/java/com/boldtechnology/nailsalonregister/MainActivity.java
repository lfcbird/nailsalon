package com.boldtechnology.nailsalonregister;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int ROSE = Color.rgb(217, 79, 120);
    private static final int ROSE_DARK = Color.rgb(169, 47, 86);
    private static final int ROSE_PALE = Color.rgb(255, 228, 236);
    private static final int BACKGROUND = Color.rgb(255, 247, 249);
    private static final int INK = Color.rgb(39, 33, 42);
    private static final int MUTED = Color.rgb(116, 107, 115);
    private static final int BORDER = Color.rgb(235, 224, 229);

    private SalonDatabase database;
    private FrameLayout content;
    private Button saleTab;
    private Button historyTab;
    private Button servicesTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new SalonDatabase(this);
        configureWindow();
        setContentView(buildShell());
        showNewSale();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(Color.WHITE);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private View buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(15), dp(18), dp(12));

        TextView brand = text("Nail Salon Register", 25, INK, Typeface.BOLD);
        header.addView(brand);

        TextView subtitle = text("Simple offline checkout", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = wrap();
        subtitleParams.topMargin = dp(2);
        header.addView(subtitle, subtitleParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tabsParams = matchWrap();
        tabsParams.topMargin = dp(13);

        saleTab = navButton("New Sale", this::showNewSale);
        historyTab = navButton("History", this::showHistory);
        servicesTab = navButton("Services", this::showServices);
        tabs.addView(saleTab, weighted());
        tabs.addView(historyTab, weightedWithMargin());
        tabs.addView(servicesTab, weightedWithMargin());
        header.addView(tabs, tabsParams);
        root.addView(header, matchWrap());

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private Button navButton(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(6), dp(10), dp(6), dp(10));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void setActiveTab(Button active) {
        styleTab(saleTab, saleTab == active);
        styleTab(historyTab, historyTab == active);
        styleTab(servicesTab, servicesTab == active);
    }

    private void styleTab(Button button, boolean selected) {
        button.setTextColor(selected ? Color.WHITE : MUTED);
        button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        button.setBackground(rounded(selected ? ROSE : Color.TRANSPARENT, 14, selected ? ROSE : BORDER, 1));
    }

    private void showNewSale() {
        setActiveTab(saleTab);
        content.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = page();

        page.addView(sectionTitle("New customer sale"));
        TextView help = text("Enter a customer ID, choose every service, then save the payment.",
                14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helpParams = matchWrap();
        helpParams.topMargin = dp(5);
        helpParams.bottomMargin = dp(18);
        page.addView(help, helpParams);

        EditText customerId = input("Customer ID  (example: C-1024)");
        customerId.setSingleLine(true);
        customerId.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        page.addView(fieldLabel("CUSTOMER ID"));
        page.addView(customerId, fieldParams());

        TextView servicesLabel = fieldLabel("SELECT SERVICES");
        LinearLayout.LayoutParams serviceLabelParams = matchWrap();
        serviceLabelParams.topMargin = dp(17);
        page.addView(servicesLabel, serviceLabelParams);

        LinearLayout serviceCard = card();
        List<ServiceItem> services = database.getServices();
        Map<CheckBox, ServiceItem> choices = new LinkedHashMap<>();
        TextView totalValue = text(Money.format(0), 32, ROSE_DARK, Typeface.BOLD);

        if (services.isEmpty()) {
            TextView none = text("No services yet. Add one from the Services tab.", 14, MUTED, Typeface.NORMAL);
            none.setPadding(dp(15), dp(16), dp(15), dp(16));
            serviceCard.addView(none, matchWrap());
        } else {
            for (int index = 0; index < services.size(); index++) {
                ServiceItem service = services.get(index);
                CheckBox box = new CheckBox(this);
                box.setText(service.name + "   •   " + Money.format(service.priceCents));
                box.setTextSize(16);
                box.setTextColor(INK);
                box.setGravity(Gravity.CENTER_VERTICAL);
                box.setButtonTintList(android.content.res.ColorStateList.valueOf(ROSE));
                box.setPadding(dp(12), dp(8), dp(12), dp(8));
                box.setMinHeight(dp(54));
                choices.put(box, service);
                box.setOnCheckedChangeListener((buttonView, isChecked) ->
                        totalValue.setText(Money.format(selectedTotal(choices))));
                serviceCard.addView(box, matchWrap());
                if (index < services.size() - 1) {
                    serviceCard.addView(divider());
                }
            }
        }
        page.addView(serviceCard, cardParams());

        LinearLayout totalCard = new LinearLayout(this);
        totalCard.setOrientation(LinearLayout.HORIZONTAL);
        totalCard.setGravity(Gravity.CENTER_VERTICAL);
        totalCard.setPadding(dp(18), dp(17), dp(18), dp(17));
        totalCard.setBackground(rounded(ROSE_PALE, 18, Color.TRANSPARENT, 0));
        TextView totalLabel = text("TOTAL", 13, ROSE_DARK, Typeface.BOLD);
        totalCard.addView(totalLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        totalCard.addView(totalValue, wrap());
        LinearLayout.LayoutParams totalParams = matchWrap();
        totalParams.topMargin = dp(14);
        page.addView(totalCard, totalParams);

        TextView paymentLabel = fieldLabel("PAYMENT METHOD");
        LinearLayout.LayoutParams paymentLabelParams = matchWrap();
        paymentLabelParams.topMargin = dp(20);
        page.addView(paymentLabel, paymentLabelParams);

        RadioGroup paymentGroup = new RadioGroup(this);
        paymentGroup.setOrientation(LinearLayout.HORIZONTAL);
        paymentGroup.setPadding(dp(4), dp(1), dp(4), dp(1));
        String[] methods = {"Cash", "Card", "Zelle", "Other"};
        for (int i = 0; i < methods.length; i++) {
            RadioButton radio = new RadioButton(this);
            radio.setText(methods[i]);
            radio.setTextColor(INK);
            radio.setTextSize(14);
            radio.setButtonTintList(android.content.res.ColorStateList.valueOf(ROSE));
            radio.setId(View.generateViewId());
            paymentGroup.addView(radio, new RadioGroup.LayoutParams(0, dp(50), 1f));
            if (i == 0) {
                paymentGroup.check(radio.getId());
            }
        }
        page.addView(paymentGroup, fieldParams());

        Button save = primaryButton("Save Payment");
        LinearLayout.LayoutParams saveParams = matchWrap();
        saveParams.topMargin = dp(19);
        saveParams.bottomMargin = dp(28);
        page.addView(save, saveParams);

        save.setOnClickListener(v -> {
            String customer = customerId.getText().toString().trim();
            List<ServiceItem> selected = selectedServices(choices);
            if (customer.isEmpty()) {
                customerId.setError("Customer ID is required");
                customerId.requestFocus();
                return;
            }
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select at least one service", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton checked = paymentGroup.findViewById(paymentGroup.getCheckedRadioButtonId());
            String paymentMethod = checked == null ? "Cash" : checked.getText().toString();
            long total = 0;
            List<String> summaryParts = new ArrayList<>();
            for (ServiceItem item : selected) {
                total += item.priceCents;
                summaryParts.add(item.name + " (" + Money.format(item.priceCents) + ")");
            }
            String summary = join(summaryParts, ", ");
            database.saveSale(customer, summary, total, paymentMethod);
            showReceipt(customer, summary, total, paymentMethod);
        });

        scroll.addView(page);
        content.addView(scroll, matchMatch());
    }

    private void showReceipt(String customer, String summary, long total, String paymentMethod) {
        LinearLayout receipt = new LinearLayout(this);
        receipt.setOrientation(LinearLayout.VERTICAL);
        receipt.setPadding(dp(22), dp(6), dp(22), 0);
        receipt.addView(text("Customer: " + customer, 16, INK, Typeface.BOLD));
        TextView details = text(summary, 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams detailsParams = matchWrap();
        detailsParams.topMargin = dp(9);
        receipt.addView(details, detailsParams);
        TextView totalText = text("Total paid: " + Money.format(total), 23, ROSE_DARK, Typeface.BOLD);
        LinearLayout.LayoutParams totalParams = matchWrap();
        totalParams.topMargin = dp(18);
        receipt.addView(totalText, totalParams);
        TextView method = text("Payment: " + paymentMethod, 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams methodParams = matchWrap();
        methodParams.topMargin = dp(5);
        receipt.addView(method, methodParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Payment saved")
                .setView(receipt)
                .setPositiveButton("New Sale", null)
                .setNegativeButton("View History", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ROSE_DARK);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                dialog.dismiss();
                showNewSale();
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ROSE_DARK);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                dialog.dismiss();
                showHistory();
            });
        });
        dialog.show();
    }

    private void showHistory() {
        setActiveTab(historyTab);
        content.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        LinearLayout page = page();
        page.addView(sectionTitle("Payment history"));

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(dp(18), dp(18), dp(18), dp(18));
        summary.setBackground(rounded(ROSE_PALE, 18, Color.TRANSPARENT, 0));

        LinearLayout summaryWords = new LinearLayout(this);
        summaryWords.setOrientation(LinearLayout.VERTICAL);
        summaryWords.addView(text("TODAY'S TOTAL", 12, ROSE_DARK, Typeface.BOLD));
        TextView count = text(database.getTodayCount() + " saved payment(s)", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams countParams = matchWrap();
        countParams.topMargin = dp(3);
        summaryWords.addView(count, countParams);
        summary.addView(summaryWords, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        summary.addView(text(Money.format(database.getTodayTotal()), 28, ROSE_DARK, Typeface.BOLD));

        LinearLayout.LayoutParams summaryParams = matchWrap();
        summaryParams.topMargin = dp(13);
        summaryParams.bottomMargin = dp(20);
        page.addView(summary, summaryParams);

        List<SaleItem> sales = database.getRecentSales(200);
        if (sales.isEmpty()) {
            LinearLayout empty = card();
            TextView emptyText = text("No saved payments yet.\nYour first sale will appear here.",
                    15, MUTED, Typeface.NORMAL);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(dp(24), dp(40), dp(24), dp(40));
            empty.addView(emptyText, matchWrap());
            page.addView(empty, cardParams());
        } else {
            SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy  •  h:mm a", Locale.US);
            for (SaleItem sale : sales) {
                LinearLayout saleCard = card();
                saleCard.setPadding(dp(16), dp(14), dp(16), dp(14));

                LinearLayout top = new LinearLayout(this);
                top.setOrientation(LinearLayout.HORIZONTAL);
                top.setGravity(Gravity.CENTER_VERTICAL);
                TextView customer = text(sale.customerId, 17, INK, Typeface.BOLD);
                top.addView(customer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                top.addView(text(Money.format(sale.totalCents), 19, ROSE_DARK, Typeface.BOLD));
                saleCard.addView(top, matchWrap());

                TextView serviceNames = text(sale.serviceSummary, 14, MUTED, Typeface.NORMAL);
                LinearLayout.LayoutParams serviceParams = matchWrap();
                serviceParams.topMargin = dp(7);
                saleCard.addView(serviceNames, serviceParams);

                TextView metadata = text(format.format(new Date(sale.createdAt)) + "  •  " + sale.paymentMethod,
                        12, MUTED, Typeface.NORMAL);
                LinearLayout.LayoutParams metadataParams = matchWrap();
                metadataParams.topMargin = dp(10);
                saleCard.addView(metadata, metadataParams);

                LinearLayout.LayoutParams saleParams = cardParams();
                saleParams.bottomMargin = dp(10);
                page.addView(saleCard, saleParams);
            }
        }
        TextView privacy = text("All information stays on this phone.", 12, MUTED, Typeface.NORMAL);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyParams = matchWrap();
        privacyParams.topMargin = dp(8);
        privacyParams.bottomMargin = dp(28);
        page.addView(privacy, privacyParams);

        scroll.addView(page);
        content.addView(scroll, matchMatch());
    }

    private void showServices() {
        setActiveTab(servicesTab);
        content.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        LinearLayout page = page();

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(sectionTitle("Services & prices"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button add = smallButton("+ Add");
        add.setOnClickListener(v -> showServiceEditor(null));
        heading.addView(add, wrap());
        page.addView(heading, matchWrap());

        TextView help = text("Tap Edit to change a service name or price.", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helpParams = matchWrap();
        helpParams.topMargin = dp(6);
        helpParams.bottomMargin = dp(17);
        page.addView(help, helpParams);

        List<ServiceItem> services = database.getServices();
        if (services.isEmpty()) {
            LinearLayout empty = card();
            TextView emptyText = text("No active services. Tap + Add to create one.",
                    15, MUTED, Typeface.NORMAL);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(dp(20), dp(34), dp(20), dp(34));
            empty.addView(emptyText, matchWrap());
            page.addView(empty, cardParams());
        } else {
            for (ServiceItem service : services) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(12), dp(10), dp(12));
                row.setBackground(rounded(Color.WHITE, 16, BORDER, 1));

                LinearLayout words = new LinearLayout(this);
                words.setOrientation(LinearLayout.VERTICAL);
                words.addView(text(service.name, 16, INK, Typeface.BOLD));
                TextView price = text(Money.format(service.priceCents), 15, ROSE_DARK, Typeface.BOLD);
                LinearLayout.LayoutParams priceParams = matchWrap();
                priceParams.topMargin = dp(3);
                words.addView(price, priceParams);
                row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                Button edit = tinyButton("Edit");
                edit.setOnClickListener(v -> showServiceEditor(service));
                row.addView(edit, wrap());
                Button remove = tinyButton("Remove");
                remove.setTextColor(Color.rgb(153, 48, 56));
                LinearLayout.LayoutParams removeParams = wrap();
                removeParams.leftMargin = dp(5);
                row.addView(remove, removeParams);
                remove.setOnClickListener(v -> confirmRemove(service));

                LinearLayout.LayoutParams rowParams = matchWrap();
                rowParams.bottomMargin = dp(10);
                page.addView(row, rowParams);
            }
        }

        TextView note = text("Existing payment records keep the original service name and price even after edits.",
                12, MUTED, Typeface.NORMAL);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = matchWrap();
        noteParams.topMargin = dp(8);
        noteParams.bottomMargin = dp(28);
        page.addView(note, noteParams);

        scroll.addView(page);
        content.addView(scroll, matchMatch());
    }

    private void showServiceEditor(ServiceItem service) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(22), dp(5), dp(22), 0);

        fields.addView(fieldLabel("SERVICE NAME"));
        EditText name = input("Example: Deluxe Pedicure");
        name.setSingleLine(true);
        if (service != null) {
            name.setText(service.name);
        }
        fields.addView(name, fieldParams());

        TextView priceLabel = fieldLabel("PRICE ($)");
        LinearLayout.LayoutParams priceLabelParams = matchWrap();
        priceLabelParams.topMargin = dp(14);
        fields.addView(priceLabel, priceLabelParams);
        EditText price = input("0.00");
        price.setSingleLine(true);
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (service != null) {
            price.setText(String.format(Locale.US, "%.2f", service.priceCents / 100.0));
        }
        fields.addView(price, fieldParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(service == null ? "Add service" : "Edit service")
                .setView(fields)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ROSE_DARK);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String serviceName = name.getText().toString().trim();
                if (serviceName.isEmpty()) {
                    name.setError("Service name is required");
                    return;
                }
                final long cents;
                try {
                    cents = Money.parseToCents(price.getText().toString());
                } catch (RuntimeException error) {
                    price.setError("Enter a valid price");
                    return;
                }
                if (service == null) {
                    database.addService(serviceName, cents);
                } else {
                    database.updateService(service.id, serviceName, cents);
                }
                dialog.dismiss();
                showServices();
            });
        });
        dialog.show();
    }

    private void confirmRemove(ServiceItem service) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Remove service?")
                .setMessage(service.name + " will no longer appear on new sales. Existing history will stay unchanged.")
                .setPositiveButton("Remove", (ignored, which) -> {
                    database.archiveService(service.id);
                    showServices();
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(153, 48, 56));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);
        });
        dialog.show();
    }

    private long selectedTotal(Map<CheckBox, ServiceItem> choices) {
        long total = 0;
        for (Map.Entry<CheckBox, ServiceItem> entry : choices.entrySet()) {
            if (entry.getKey().isChecked()) {
                total += entry.getValue().priceCents;
            }
        }
        return total;
    }

    private List<ServiceItem> selectedServices(Map<CheckBox, ServiceItem> choices) {
        List<ServiceItem> selected = new ArrayList<>();
        for (Map.Entry<CheckBox, ServiceItem> entry : choices.entrySet()) {
            if (entry.getKey().isChecked()) {
                selected.add(entry.getValue());
            }
        }
        return selected;
    }

    private static String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(10), dp(18), dp(20));
        return page;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(Color.WHITE, 18, BORDER, 1));
        return card;
    }

    private TextView sectionTitle(String value) {
        return text(value, 23, INK, Typeface.BOLD);
    }

    private TextView fieldLabel(String value) {
        TextView label = text(value, 12, MUTED, Typeface.BOLD);
        label.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(7);
        label.setLayoutParams(params);
        return label;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(Color.rgb(158, 149, 156));
        input.setTextColor(INK);
        input.setTextSize(16);
        input.setPadding(dp(14), dp(4), dp(14), dp(4));
        input.setBackground(rounded(Color.WHITE, 14, BORDER, 1));
        return input;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(17);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(dp(58));
        button.setBackground(rounded(ROSE, 17, ROSE, 1));
        return button;
    }

    private Button smallButton(String label) {
        Button button = tinyButton(label);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(ROSE, 13, ROSE, 1));
        button.setPadding(dp(14), dp(6), dp(14), dp(6));
        return button;
    }

    private Button tinyButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(ROSE_DARK);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(10), dp(7), dp(10), dp(7));
        button.setBackground(rounded(BACKGROUND, 11, BORDER, 1));
        return button;
    }

    private TextView divider() {
        TextView divider = new TextView(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.leftMargin = dp(15);
        params.rightMargin = dp(15);
        divider.setLayoutParams(params);
        return divider;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setLineSpacing(0, 1.15f);
        return text;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams fieldParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(2);
        return params;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams weightedWithMargin() {
        LinearLayout.LayoutParams params = weighted();
        params.leftMargin = dp(7);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private FrameLayout.LayoutParams matchMatch() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

package com.famayaberben.mislistascompras;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String PREFS = "shopping_lists";
    private static final String DATA_KEY = "data";

    private final ArrayList<ShoppingList> lists = new ArrayList<>();
    private LinearLayout itemsContainer;
    private Spinner listSpinner;
    private int activeList = 0;
    private boolean updatingSpinner = false;

    private static class ShoppingItem {
        String name;
        boolean checked;

        ShoppingItem(String name, boolean checked) {
            this.name = name;
            this.checked = checked;
        }
    }

    private static class ShoppingList {
        String name;
        final ArrayList<ShoppingItem> items = new ArrayList<>();

        ShoppingList(String name) {
            this.name = name;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(25, 99, 73));
        getWindow().setNavigationBarColor(Color.WHITE);

        loadLists();
        if (lists.isEmpty()) {
            lists.add(new ShoppingList("Mi compra"));
        }

        buildScreen();
        saveLists();
    }

    private void buildScreen() {
        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(248, 250, 248));
        page.addView(root);
        setContentView(page);

        TextView title = new TextView(this);
        title.setText("Mis compras");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(25, 99, 73));
        title.setTypeface(null, 1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tus listas, siempre a mano");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle);

        LinearLayout listRow = new LinearLayout(this);
        listRow.setGravity(Gravity.CENTER_VERTICAL);

        listSpinner = new Spinner(this);
        listRow.addView(listSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button newList = new Button(this);
        newList.setText("+ Lista");
        listRow.addView(newList);
        root.addView(listRow);

        newList.setOnClickListener(v -> showNewListDialog());

        listSpinner.setOnItemSelectedListener(
            new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(
                        android.widget.AdapterView<?> parent,
                        View view,
                        int position,
                        long id) {
                    if (!updatingSpinner && position >= 0 && position < lists.size()) {
                        activeList = position;
                        renderItems();
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            }
        );

        LinearLayout addRow = new LinearLayout(this);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        addRow.setPadding(0, dp(14), 0, dp(10));

        EditText itemInput = new EditText(this);
        itemInput.setSingleLine(true);
        itemInput.setHint("¿Qué necesitas comprar?");
        itemInput.setInputType(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        addRow.addView(itemInput, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button addButton = new Button(this);
        addButton.setText("Añadir");
        addRow.addView(addButton);
        root.addView(addRow);

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(220, 228, 222));
        root.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(itemsContainer);

        addButton.setOnClickListener(v -> {
            String value = itemInput.getText().toString().trim();
            if (value.isEmpty()) {
                itemInput.setError("Escribe un producto");
                return;
            }

            lists.get(activeList).items.add(new ShoppingItem(value, false));
            itemInput.setText("");
            saveLists();
            renderItems();
        });

        updateSpinner();
        renderItems();
    }

    private void showNewListDialog() {
        EditText input = new EditText(this);
        input.setHint("Nombre de la lista");
        input.setInputType(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        new AlertDialog.Builder(this)
            .setTitle("Crear lista")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    name = "Nueva lista";
                }

                lists.add(new ShoppingList(name));
                activeList = lists.size() - 1;
                saveLists();
                updateSpinner();
                renderItems();
            })
            .show();
    }

    private void updateSpinner() {
        ArrayList<String> names = new ArrayList<>();
        for (ShoppingList list : lists) {
            names.add(list.name);
        }

        updatingSpinner = true;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            names
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listSpinner.setAdapter(adapter);

        if (activeList >= lists.size()) {
            activeList = lists.size() - 1;
        }

        listSpinner.setSelection(Math.max(0, activeList));
        updatingSpinner = false;
    }

    private void renderItems() {
        if (itemsContainer == null || lists.isEmpty()) {
            return;
        }

        itemsContainer.removeAllViews();
        ShoppingList list = lists.get(activeList);

        int remaining = 0;
        for (ShoppingItem item : list.items) {
            if (!item.checked) {
                remaining++;
            }
        }

        TextView count = new TextView(this);
        count.setText(
            list.items.isEmpty()
                ? "Todavía no hay productos"
                : remaining + " pendientes · " + list.items.size() + " en total"
        );
        count.setTextColor(Color.GRAY);
        count.setTextSize(14);
        count.setPadding(0, dp(14), 0, dp(6));
        itemsContainer.addView(count);

        for (int i = 0; i < list.items.size(); i++) {
            final int itemIndex = i;
            ShoppingItem item = list.items.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(item.name);
            checkBox.setTextSize(17);
            checkBox.setTextColor(Color.rgb(40, 50, 44));
            checkBox.setChecked(item.checked);
            row.addView(checkBox, new LinearLayout.LayoutParams(0, dp(52), 1));

            Button delete = new Button(this);
            delete.setText("×");
            delete.setTextSize(20);
            row.addView(delete, new LinearLayout.LayoutParams(dp(52), dp(48)));

            checkBox.setOnCheckedChangeListener((button, checked) -> {
                list.items.get(itemIndex).checked = checked;
                saveLists();
                renderItems();
            });

            delete.setOnClickListener(v -> {
                list.items.remove(itemIndex);
                saveLists();
                renderItems();
            });

            itemsContainer.addView(row);

            View line = new View(this);
            line.setBackgroundColor(Color.rgb(232, 236, 233));
            itemsContainer.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        }

        if (!list.items.isEmpty()) {
            Button clear = new Button(this);
            clear.setText("Borrar productos marcados");
            clear.setOnClickListener(v -> {
                for (int i = list.items.size() - 1; i >= 0; i--) {
                    if (list.items.get(i).checked) {
                        list.items.remove(i);
                    }
                }
                saveLists();
                renderItems();
            });
            itemsContainer.addView(clear);
        }
    }

    private void saveLists() {
        JSONArray data = new JSONArray();

        try {
            for (ShoppingList list : lists) {
                JSONObject listObject = new JSONObject();
                listObject.put("name", list.name);

                JSONArray items = new JSONArray();
                for (ShoppingItem item : list.items) {
                    JSONObject itemObject = new JSONObject();
                    itemObject.put("name", item.name);
                    itemObject.put("checked", item.checked);
                    items.put(itemObject);
                }

                listObject.put("items", items);
                data.put(listObject);
            }

            getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(DATA_KEY, data.toString())
                .apply();
        } catch (JSONException e) {
            Toast.makeText(this, "No se pudo guardar la lista", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLists() {
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(DATA_KEY, null);

        if (saved == null) {
            return;
        }

        try {
            JSONArray data = new JSONArray(saved);

            for (int i = 0; i < data.length(); i++) {
                JSONObject listObject = data.getJSONObject(i);
                ShoppingList list = new ShoppingList(
                    listObject.optString("name", "Mi compra")
                );

                JSONArray items = listObject.optJSONArray("items");
                if (items != null) {
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject item = items.getJSONObject(j);
                        list.items.add(new ShoppingItem(
                            item.optString("name", ""),
                            item.optBoolean("checked", false)
                        ));
                    }
                }

                lists.add(list);
            }
        } catch (JSONException ignored) {
            lists.clear();
        }
    }

    private int dp(int value) {
        return (int) (
            value * getResources().getDisplayMetrics().density + 0.5f
        );
    }
}

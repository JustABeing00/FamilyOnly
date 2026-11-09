package com.familyonly.screener;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends Activity
        implements View.OnClickListener, View.OnLongClickListener {

    public static final String PREFS = "familyonly";
    public static final String KEY_NUMBERS = "whitelist";

    private ArrayList<String> numbers = new ArrayList<>();
    private TextView statusPill;
    private TextView statusDesc;
    private TextView statusCount;
    private TextView trustedCount;
    private EditText input;
    private EditText fwdInput;
    private LinearLayout trustedList;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ensurePermissions();
        setContentView(R.layout.activity_main);

        statusPill = findViewById(R.id.statusPill);
        statusDesc = findViewById(R.id.statusDesc);
        statusCount = findViewById(R.id.statusCount);
        trustedCount = findViewById(R.id.trustedCount);
        input = findViewById(R.id.trustedInput);
        fwdInput = findViewById(R.id.fwdInput);
        trustedList = findViewById(R.id.trustedList);
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.btnAdd).setOnClickListener(this);
        findViewById(R.id.btnEnable).setOnClickListener(this);
        findViewById(R.id.btnForward).setOnClickListener(this);
        findViewById(R.id.btnCancelFwd).setOnClickListener(this);
        findViewById(R.id.btnErase).setOnClickListener(this);

        loadNumbers();
        refreshStatus();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnAdd) {
            onAdd();
        } else if (id == R.id.btnEnable) {
            requestScreeningRole();
        } else if (id == R.id.btnForward) {
            onFwd();
        } else if (id == R.id.btnCancelFwd) {
            onUnfwd();
        } else if (id == R.id.btnErase) {
            int n = LogCleaner.cleanAllBlocked(this);
            if (n > 0) {
                toast(getString(R.string.toast_erased, n));
            } else {
                toast(getString(R.string.toast_log_clean));
            }
            refreshStatus();
        }
    }

    private void onAdd() {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) {
            toast(getString(R.string.toast_empty_number));
            return;
        }
        String norm = normalize(raw);
        if (norm.length() < 10) {
            toast(getString(R.string.toast_short_number));
        }
        if (!numbers.contains(raw)) {
            numbers.add(raw);
            saveNumbers();
            input.setText("");
        }
        refreshStatus();
    }

    private void onFwd() {
        String target = fwdInput.getText().toString().trim().replaceAll("[^0-9+]", "");
        if (target.isEmpty()) {
            toast(getString(R.string.toast_fwd_empty));
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:**67*" + target + Uri.encode("#")));
            startActivity(i);
            toast(getString(R.string.toast_fwd_go, target));
        } catch (Exception e) {
            toast("Dial failed: " + e.getMessage());
        }
    }

    private void onUnfwd() {
        try {
            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:##67" + Uri.encode("#")));
            startActivity(i);
        } catch (Exception e) {
            toast("Dial failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof Integer) {
            int position = ((Integer) tag).intValue();
            if (position >= 0 && position < numbers.size()) {
                String removed = numbers.remove(position);
                saveNumbers();
                refreshStatus();
                toast(getString(R.string.toast_removed, removed));
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensurePermissions();
        loadNumbers();
        LogCleaner.cleanAllBlocked(this);
        refreshStatus();
    }

    private void ensurePermissions() {
        java.util.ArrayList<String> missing = new java.util.ArrayList<String>();
        if (checkSelfPermission("android.permission.READ_CALL_LOG")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            missing.add("android.permission.READ_CALL_LOG");
        }
        if (checkSelfPermission("android.permission.WRITE_CALL_LOG")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            missing.add("android.permission.WRITE_CALL_LOG");
        }
        if (checkSelfPermission("android.permission.READ_PHONE_STATE")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            missing.add("android.permission.READ_PHONE_STATE");
        }
        if (checkSelfPermission("android.permission.READ_CONTACTS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            missing.add("android.permission.READ_CONTACTS");
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), 2001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
    }

    private void loadNumbers() {
        SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> set = p.getStringSet(KEY_NUMBERS, new HashSet<String>());
        numbers.clear();
        numbers.addAll(new TreeSet<String>(set));
    }

    private void saveNumbers() {
        SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putStringSet(KEY_NUMBERS, new HashSet<String>(numbers)).apply();
    }

    private void refreshStatus() {
        boolean screening = isScreeningHeld();

        if (statusPill != null) {
            if (screening) {
                statusPill.setText(R.string.status_active);
                statusPill.setTextColor(getColor(R.color.success));
                statusPill.setBackground(getDrawable(R.drawable.pill_active));
            } else {
                statusPill.setText(R.string.status_inactive);
                statusPill.setTextColor(getColor(R.color.inactive_text));
                statusPill.setBackground(getDrawable(R.drawable.pill_inactive));
            }
        }
        if (statusCount != null) {
            statusCount.setText(numbers.size() + " trusted");
        }
        if (trustedCount != null) {
            trustedCount.setText(numbers.size() + " saved");
        }
        if (statusDesc != null) {
            if (numbers.isEmpty()) {
                statusDesc.setText(getString(R.string.trusted_empty_desc));
            } else if (!screening) {
                statusDesc.setText("Screening is off. Enable it so only trusted numbers ring.");
            } else {
                statusDesc.setText(getString(R.string.status_desc));
            }
        }
        renderTrustedList();
    }

    private void renderTrustedList() {
        if (trustedList == null) return;
        trustedList.removeAllViews();
        if (numbers.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            return;
        }
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < numbers.size(); i++) {
            String raw = numbers.get(i);
            View row = inflater.inflate(R.layout.item_trusted_number, trustedList, false);
            TextView rowNumber = row.findViewById(R.id.rowNumber);
            TextView rowAvatar = row.findViewById(R.id.rowAvatar);
            rowNumber.setText(raw);
            String digits = normalize(raw);
            String letter = digits.isEmpty() ? "F" : digits.substring(0, 1);
            rowAvatar.setText(letter);
            row.setTag(Integer.valueOf(i));
            row.setOnLongClickListener(this);
            row.setFocusable(true);
            row.setClickable(true);
            trustedList.addView(row);
        }
    }

    private boolean isScreeningHeld() {
        try {
            RoleManager rm = (RoleManager) getSystemService(RoleManager.class);
            return rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        } catch (Exception e) {
            return false;
        }
    }

    private void requestScreeningRole() {
        try {
            RoleManager rm = (RoleManager) getSystemService(RoleManager.class);
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                Intent i = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
                startActivityForResult(i, 1001);
            } else {
                toast("Screening role not available");
            }
        } catch (Exception e) {
            toast("Role request failed: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            refreshStatus();
            toast(resultCode == RESULT_OK
                    ? getString(R.string.toast_screening_on)
                    : getString(R.string.toast_screening_off));
        }
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}

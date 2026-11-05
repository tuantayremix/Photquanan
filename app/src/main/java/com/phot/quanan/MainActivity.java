package com.phot.quanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class MainActivity extends Activity {
  FirebaseFirestore db;
  String uid = "";
  List<Map<String, Object>> all = new ArrayList<>(), shown = new ArrayList<>();
  Spinner sp;
  EditText dist, q;
  TextView empty;
  BaseAdapter ad;

  int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

  TextView tv(String t, int size, boolean bold) {
    TextView x = new TextView(this);
    x.setText(t);
    x.setTextSize(size);
    x.setPadding(0, dp(2), 0, dp(2));
    if (bold) x.setTypeface(null, Typeface.BOLD);
    return x;
  }

  String s(Map<String, Object> m, String k) {
    Object o = m.get(k);
    return o == null ? "" : o.toString();
  }

  int size(Map<String, Object> m, String k) {
    Object o = m.get(k);
    return o instanceof List ? ((List<?>) o).size() : 0;
  }

  abstract class W implements TextWatcher {
    public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
    public void onTextChanged(CharSequence a, int b, int c, int d) {}
  }

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    db = FirebaseFirestore.getInstance();
    FirebaseAuth au = FirebaseAuth.getInstance();
    if (au.getCurrentUser() != null) uid = au.getCurrentUser().getUid();
    else au.signInAnonymously().addOnSuccessListener(r -> uid = r.getUser().getUid());

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(12), dp(36), dp(12), 0);

    root.addView(tv("🚩 Cảnh báo quán ăn Hàn", 20, true));

    sp = new Spinner(this);
    List<String> names = new ArrayList<>();
    names.add("Tất cả tỉnh/thành");
    names.addAll(Arrays.asList(Regions.NAMES));
    sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
    sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> p, View v, int i, long id) { filter(); }
      public void onNothingSelected(AdapterView<?> p) {}
    });
    root.addView(sp);

    dist = new EditText(this);
    dist.setHint("Quận/huyện (vd: 해운대구)");
    dist.setSingleLine(true);
    root.addView(dist);

    q = new EditText(this);
    q.setHint("🔍 Tìm tên quán");
    q.setSingleLine(true);
    root.addView(q);

    W w = new W() { public void afterTextChanged(Editable e) { filter(); } };
    dist.addTextChangedListener(w);
    q.addTextChangedListener(w);

    Button nb = new Button(this);
    nb.setText("+ Viết bài");
    nb.setOnClickListener(v -> startActivity(new Intent(this, PostActivity.class)));
    root.addView(nb);

    empty = tv("Chưa có bài nào ở khu vực này.", 14, false);
    empty.setPadding(dp(8), dp(20), dp(8), dp(8));
    root.addView(empty);

    ListView lv = new ListView(this);
    ad = new BaseAdapter() {
      public int getCount() { return shown.size(); }
      public Object getItem(int i) { return shown.get(i); }
      public long getItemId(int i) { return i; }
      public View getView(int i, View cv, ViewGroup g) {
        Map<String, Object> m = shown.get(i);
        LinearLayout l = new LinearLayout(MainActivity.this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(4), dp(10), dp(4), dp(10));
        TextView a = tv(s(m, "cat") + " · " + s(m, "province") + " " + s(m, "district"), 12, false);
        a.setTextColor(0xFFC8372D);
        TextView t = tv(s(m, "name") + ": " + s(m, "title"), 16, true);
        TextView c = tv(s(m, "body"), 14, false);
        c.setMaxLines(3);
        c.setEllipsize(TextUtils.TruncateAt.END);
        TextView f = tv("🙋 " + size(m, "likes") + " người cũng gặp", 12, false);
        f.setTextColor(0xFF777777);
        l.addView(a); l.addView(t); l.addView(c); l.addView(f);
        return l;
      }
    };
    lv.setAdapter(ad);
    lv.setOnItemClickListener((p, v, i, id) -> detail(shown.get(i)));
    root.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1f));
    setContentView(root);

    db.collection("posts").orderBy("ts", Query.Direction.DESCENDING).limit(500)
      .addSnapshotListener((snap, e) -> {
        if (snap == null) return;
        all.clear();
        for (DocumentSnapshot d : snap.getDocuments()) {
          Map<String, Object> m = new HashMap<>(d.getData());
          m.put("id", d.getId());
          all.add(m);
        }
        filter();
      });
  }

  void filter() {
    if (sp == null || ad == null || dist == null || q == null) return;
    String p = sp.getSelectedItemPosition() == 0 ? "" : (String) sp.getSelectedItem();
    String d = dist.getText().toString().trim();
    String k = q.getText().toString().trim().toLowerCase();
    shown.clear();
    for (Map<String, Object> m : all) {
      if (size(m, "reports") >= 3) continue;
      if (!p.isEmpty() && !p.equals(s(m, "province"))) continue;
      if (!d.isEmpty() && !s(m, "district").contains(d)) continue;
      String t = (s(m, "name") + s(m, "title") + s(m, "body")).toLowerCase();
      if (!k.isEmpty() && !t.contains(k)) continue;
      shown.add(m);
    }
    ad.notifyDataSetChanged();
    empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
  }

  void detail(Map<String, Object> m) {
    if (uid.isEmpty()) {
      Toast.makeText(this, "Đang kết nối, thử lại sau giây lát", Toast.LENGTH_SHORT).show();
      return;
    }
    DocumentReference ref = db.collection("posts").document(s(m, "id"));
    AlertDialog.Builder b = new AlertDialog.Builder(this)
      .setTitle(s(m, "name") + ": " + s(m, "title"))
      .setMessage(s(m, "body"))
      .setPositiveButton("🙋 Tôi cũng gặp", (x, y) -> ref.update("likes", FieldValue.arrayUnion(uid)))
      .setNeutralButton("🚩 Báo cáo", (x, y) -> {
        ref.update("reports", FieldValue.arrayUnion(uid));
        Toast.makeText(this, "Đã báo cáo bài viết", Toast.LENGTH_SHORT).show();
      });
    if (uid.equals(s(m, "author"))) b.setNegativeButton("Xóa", (x, y) -> ref.delete());
    else b.setNegativeButton("Đóng", null);
    b.show();
  }
}

package com.phot.quanan;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

public class PostActivity extends Activity {
  EditText name, dist, title, body;
  Spinner prov, cat;

  int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

  EditText et(String hint, int lines) {
    EditText e = new EditText(this);
    e.setHint(hint);
    if (lines > 1) { e.setMinLines(lines); e.setGravity(48); }
    return e;
  }

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(dp(14), dp(36), dp(14), dp(14));

    prov = new Spinner(this);
    prov.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Regions.NAMES));
    dist = et("Quận/huyện (vd: 해운대구)", 1);
    name = et("Tên quán (tiếng Hàn nếu có)", 1);
    cat = new Spinner(this);
    cat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Regions.CATS));
    title = et("Tiêu đề", 1);
    body = et("Mô tả sự việc, thời gian xảy ra...", 6);

    TextView note = new TextView(this);
    note.setText("Chỉ viết sự việc có thật mình trải qua. Không nêu tên, ảnh hay số điện thoại cá nhân. Ở Hàn Quốc, đăng thông tin về người khác, kể cả đúng sự thật, vẫn có thể bị kiện tội phỉ báng.");
    note.setTextSize(12);
    note.setPadding(0, dp(8), 0, dp(8));

    Button save = new Button(this);
    save.setText("Đăng bài");
    save.setOnClickListener(v -> save());

    l.addView(prov); l.addView(dist); l.addView(name); l.addView(cat);
    l.addView(title); l.addView(body); l.addView(note); l.addView(save);
    ScrollView sv = new ScrollView(this);
    sv.addView(l);
    setContentView(sv);
  }

  void save() {
    String n = name.getText().toString().trim();
    String t = title.getText().toString().trim();
    String c = body.getText().toString().trim();
    if (n.isEmpty() || t.isEmpty() || c.isEmpty()) {
      Toast.makeText(this, "Điền tên quán, tiêu đề và nội dung", Toast.LENGTH_SHORT).show();
      return;
    }
    Map<String, Object> m = new HashMap<>();
    m.put("name", n);
    m.put("title", t);
    m.put("body", c);
    m.put("province", (String) prov.getSelectedItem());
    m.put("district", dist.getText().toString().trim());
    m.put("cat", (String) cat.getSelectedItem());
    m.put("ts", System.currentTimeMillis());
    m.put("likes", 0);
    m.put("reports", 0);
    List<Map<String, Object>> list = Store.load(this);
    list.add(m);
    Store.save(this, list);
    finish();
  }
}

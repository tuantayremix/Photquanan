package com.phot.quanan;

import android.content.Context;
import org.json.*;
import java.util.*;

public class Store {
  public static long num(Map<String, Object> m, String k) {
    Object o = m.get(k);
    return o instanceof Number ? ((Number) o).longValue() : 0;
  }

  public static List<Map<String, Object>> load(Context c) {
    List<Map<String, Object>> r = new ArrayList<>();
    try {
      JSONArray arr = new JSONArray(c.getSharedPreferences("posts", 0).getString("d", "[]"));
      for (int i = 0; i < arr.length(); i++) {
        JSONObject o = arr.getJSONObject(i);
        Map<String, Object> m = new HashMap<>();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
          String k = it.next();
          m.put(k, o.get(k));
        }
        r.add(m);
      }
    } catch (Exception e) {}
    Collections.sort(r, (x, y) -> Long.compare(num(y, "ts"), num(x, "ts")));
    return r;
  }

  public static void save(Context c, List<Map<String, Object>> list) {
    JSONArray arr = new JSONArray();
    for (Map<String, Object> m : list) arr.put(new JSONObject(m));
    c.getSharedPreferences("posts", 0).edit().putString("d", arr.toString()).apply();
  }
}

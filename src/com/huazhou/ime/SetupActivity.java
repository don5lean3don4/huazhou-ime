package com.huazhou.ime;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** 安装引导页：启用输入法、切换输入法、试打、导出字体。 */
public class SetupActivity extends Activity {

    private float d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        d = getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF4F5F7);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(px(20), px(24), px(20), px(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("化州文输入法");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTextColor(0xFF11324D);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("键盘键帽直接用 fa1zau4 化州文字母。敲拉丁拼音、选 1-6 声调，候选里挑字上屏；"
                + "「符号」页有中文标点、英文标点、数字和字母表。共收录 "
                + syllableCount() + " 个带调音节。");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setTextColor(0xFF5B6672);
        sub.setPadding(0, px(6), 0, px(18));
        root.addView(sub);

        root.addView(step("1", "启用输入法", "在系统设置里勾选「化州文输入法」", v -> {
            try {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            } catch (Exception e) {
                toast("打不开系统设置，请手动到 设置-语言与输入法 里启用");
            }
        }));

        root.addView(step("2", "切换到本输入法", "在任意输入框长按，或点下面的按钮选择", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        }));

        root.addView(sectionTitle("试打"));

        EditText test = new EditText(this);
        test.setHint("在这里试打：ba4、ngo5、tui2");
        test.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        test.setMinLines(3);
        test.setGravity(Gravity.TOP | Gravity.START);
        test.setBackground(round(0xFFFFFFFF, 12));
        test.setPadding(px(14), px(12), px(14), px(12));
        test.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(test);

        root.addView(sectionTitle("字体"));

        TextView fontNote = new TextView(this);
        fontNote.setText("化州文字形在字体文件里。本 APK 已内置该字体，键盘和候选栏可直接显示；"
                + "若要把化州文粘到别的 App 里也显示正常，需要把字体装进手机系统（部分品牌机支持）。"
                + "点下面按钮可把字体导出到文件。");
        fontNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        fontNote.setTextColor(0xFF5B6672);
        fontNote.setPadding(0, 0, 0, px(10));
        root.addView(fontNote);

        Button export = new Button(this);
        export.setText("导出字体 hzfont.ttf");
        export.setAllCaps(false);
        export.setOnClickListener(v -> exportFont());
        root.addView(export, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scroll);
    }

    private int syllableCount() {
        int n = 0;
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(getAssets().open("dict.tsv"), "UTF-8"));
            while (r.readLine() != null) n++;
            r.close();
        } catch (Exception ignored) {
        }
        return n;
    }

    private void exportFont() {
        try {
            File dir = getExternalFilesDir(null);
            if (dir == null) dir = getFilesDir();
            File out = new File(dir, "hzfont.ttf");
            InputStream in = getAssets().open("hzfont.ttf");
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            fos.close();
            in.close();
            toast("已导出：" + out.getAbsolutePath());
        } catch (Exception e) {
            toast("导出失败：" + e.getMessage());
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private interface Click {
        void onClick(android.view.View v);
    }

    private ViewGroup step(String num, String title, String desc, Click click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setBackground(round(0xFFFFFFFF, 14));
        box.setPadding(px(14), px(14), px(14), px(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = px(10);
        box.setLayoutParams(lp);
        box.setOnClickListener(click::onClick);

        TextView badge = new TextView(this);
        badge.setText(num);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        badge.setTextColor(0xFFFFFFFF);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(0xFF2E6DB4, 20));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(px(30), px(30));
        blp.rightMargin = px(12);
        box.addView(badge, blp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTextColor(0xFF1B1F24);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(t);
        TextView s = new TextView(this);
        s.setText(desc);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        s.setTextColor(0xFF6B7480);
        col.addView(s);
        box.addView(col);
        return box;
    }

    private TextView sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTextColor(0xFF8A94A0);
        t.setPadding(0, px(18), 0, px(8));
        return t;
    }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusDp * d);
        return g;
    }

    private int px(float dp) {
        return Math.round(dp * d);
    }
}

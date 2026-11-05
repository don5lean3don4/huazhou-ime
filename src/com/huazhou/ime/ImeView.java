package com.huazhou.ime;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 键盘界面：
 *  - 候选栏（拼音 + 化州文候选）
 *  - 主键区：键帽用 fa1zau4 化州文字母，下面带小号拉丁字母提示
 *  - 更多页：标点 / 英文 / 数字 / 字母表，四个标签
 */
public class ImeView extends LinearLayout {

    public interface Callback {
        void onLetter(String s);
        void onTone(String d);
        void onBackspace();
        void onSpace();
        void onEnter();
        void onCandidate(String glyph);
        void onPunct(String s);
        void onClearBuffer();
    }

    private static final int BG = 0xFFE9EBEF;
    private static final int KEY = 0xFFFFFFFF;
    private static final int KEY_ALT = 0xFFDDE3EC;
    private static final int KEY_TONE = 0xFFC9DDF5;
    private static final int KEY_FUNC = 0xFFB9C3CF;
    private static final int KEY_ON = 0xFF9DC3EC;
    private static final int KEY_TAB = 0xFFC8D4E0;
    private static final int TEXT = 0xFF1B1F24;
    private static final int MUTED = 0xFF6B7480;
    private static final int PRESSED = 0xFFA9C7EA;

    private static final int TAB_PUNCT = 0;
    private static final int TAB_EN = 1;
    private static final int TAB_NUM = 2;
    private static final int TAB_LETTERS = 3;

    private final Callback cb;
    private final Typeface hzFont;
    private final float d;
    private final Handler handler = new Handler();

    private final LinearLayout candidateRow;
    private final TextView bufferView;
    private final TextView hintView;
    private final LinearLayout keysPage;
    private final LinearLayout morePage;
    private final LinearLayout tabRow;
    private final LinearLayout pageContent;
    private final TextView[] tabViews = new TextView[4];

    private final Map<String, String> singleGlyph = new HashMap<>();
    private List<Dict.Letter> initials;
    private List<Dict.Letter> finals;

    private boolean glyphCaps = true;
    private boolean moreShown;
    private int tab = TAB_PUNCT;

    public ImeView(Context ctx, Callback cb, Typeface hzFont) {
        super(ctx);
        this.cb = cb;
        this.hzFont = hzFont;
        this.d = ctx.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setPadding(px(3), px(4), px(3), px(6));

        // ---------------- 候选栏 ----------------
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, px(58)));

        bufferView = new TextView(ctx);
        bufferView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        bufferView.setTextColor(TEXT);
        bufferView.setGravity(Gravity.CENTER);
        bufferView.setBackground(box(0xFFFFFFFF, 10));
        bufferView.setOnClickListener(v -> cb.onClearBuffer());
        bar.addView(bufferView, new LinearLayout.LayoutParams(px(56), px(44)));

        HorizontalScrollView scroll = new HorizontalScrollView(ctx);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        candidateRow = new LinearLayout(ctx);
        candidateRow.setOrientation(HORIZONTAL);
        candidateRow.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(candidateRow, new HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        bar.addView(scroll);

        hintView = new TextView(ctx);
        hintView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hintView.setTextColor(MUTED);
        hintView.setText("拼音 + 声调");
        hintView.setGravity(Gravity.CENTER);
        hintView.setPadding(px(4), 0, px(4), 0);
        bar.addView(hintView, new LinearLayout.LayoutParams(px(78), LayoutParams.WRAP_CONTENT));

        addView(bar);

        // ---------------- 主键区 ----------------
        keysPage = new LinearLayout(ctx);
        keysPage.setOrientation(VERTICAL);
        addView(keysPage, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // ---------------- 更多页（标点/英文/数字/字母表） ----------------
        morePage = new LinearLayout(ctx);
        morePage.setOrientation(VERTICAL);
        morePage.setVisibility(GONE);
        addView(morePage, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        tabRow = new LinearLayout(ctx);
        tabRow.setOrientation(HORIZONTAL);
        String[] tabs = {"标点", "英文", "数字", "字母"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            TextView t = key(tabs[i], KEY_ALT, TEXT, 13);
            t.setOnClickListener(v -> showTab(idx));
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, px(36), 1f);
            tlp.setMargins(px(2), px(2), px(2), px(2));
            tabRow.addView(t, tlp);
            tabViews[i] = t;
        }
        morePage.addView(tabRow);

        ScrollView ps = new ScrollView(ctx);
        ps.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, px(164)));
        pageContent = new LinearLayout(ctx);
        pageContent.setOrientation(VERTICAL);
        ps.addView(pageContent, new ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        morePage.addView(ps);

        LinearLayout moreBottom = new LinearLayout(ctx);
        moreBottom.setOrientation(HORIZONTAL);
        TextView back = key("返回", KEY_ALT, TEXT, 14);
        back.setOnClickListener(v -> toggleMore());
        moreBottom.addView(back, keyLp(1.4f));
        TextView sp = key("空格", KEY, TEXT, 15);
        sp.setOnClickListener(v -> cb.onSpace());
        moreBottom.addView(sp, keyLp(2.4f));
        TextView en = key("回车", KEY_FUNC, TEXT, 13);
        en.setOnClickListener(v -> cb.onEnter());
        moreBottom.addView(en, keyLp(1.3f));
        TextView dl = key("删", KEY_FUNC, TEXT, 15);
        attachRepeat(dl, cb::onBackspace);
        moreBottom.addView(dl, keyLp(1.3f));
        morePage.addView(moreBottom);
    }

    /** 主键区依赖字母表（键帽字形），字母表加载完后重建。 */
    public void bindLetters(List<Dict.Letter> initials, List<Dict.Letter> finals) {
        this.initials = initials;
        this.finals = finals;
        singleGlyph.clear();
        for (Dict.Letter l : initials) if (l.roman.length() == 1) singleGlyph.put(l.roman, l.glyph);
        for (Dict.Letter l : finals) if (l.roman.length() == 1) singleGlyph.put(l.roman, l.glyph);
        buildKeys();
        showTab(TAB_PUNCT);
    }

    // ---------------- 主键区 ----------------

    private void buildKeys() {
        keysPage.removeAllViews();
        keysPage.addView(toneRow());
        keysPage.addView(row("q w e r t y u i o p"));
        keysPage.addView(row("a s d f g h j k l"));

        LinearLayout r3 = new LinearLayout(getContext());
        r3.setOrientation(HORIZONTAL);
        for (String s : "z x c v b n m".split(" ")) r3.addView(letterCap(s), keyLp(1f));
        TextView del = key("删", KEY_FUNC, TEXT, 15);
        attachRepeat(del, cb::onBackspace);
        r3.addView(del, keyLp(1.4f));
        keysPage.addView(r3);

        // 第四行：符号 / 英文 / 数字 / 字母 四个候选栏入口 + 空格 + 回车
        LinearLayout r4 = new LinearLayout(getContext());
        r4.setOrientation(HORIZONTAL);
        r4.addView(tabKey("符号", TAB_PUNCT), keyLp(1.2f));
        r4.addView(tabKey("英文", TAB_EN), keyLp(1.2f));
        r4.addView(tabKey("数字", TAB_NUM), keyLp(1.2f));
        r4.addView(tabKey("字母", TAB_LETTERS), keyLp(1.2f));
        TextView space = key("空格", KEY, TEXT, 15);
        space.setOnClickListener(v -> cb.onSpace());
        r4.addView(space, keyLp(2.4f));
        TextView enter = key("回车", KEY_FUNC, TEXT, 13);
        enter.setOnClickListener(v -> cb.onEnter());
        r4.addView(enter, keyLp(1.4f));
        keysPage.addView(r4);
    }

    /** 声调行：1-6 放在键盘最上面。 */
    private View toneRow() {
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(HORIZONTAL);
        for (int t = 1; t <= 6; t++) {
            final String tone = String.valueOf(t);
            TextView k = key(tone, KEY_TONE, TEXT, 17);
            k.setOnClickListener(v -> cb.onTone(tone));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(38), 1f);
            lp.setMargins(px(2), px(2), px(2), px(2));
            r.addView(k, lp);
        }
        return r;
    }

    private View tabKey(String label, final int tabIndex) {
        TextView k = key(label, KEY_TAB, TEXT, 13);
        k.setOnClickListener(v -> openTab(tabIndex));
        return k;
    }

    /** 从主键区直接打开某个候选栏（符号/英文/数字/字母）。 */
    private void openTab(int idx) {
        if (!moreShown) {
            moreShown = true;
            keysPage.setVisibility(GONE);
            morePage.setVisibility(VISIBLE);
        }
        showTab(idx);
    }

    private LinearLayout row(String keys) {
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(HORIZONTAL);
        for (String s : keys.split(" ")) r.addView(letterCap(s), keyLp(1f));
        return r;
    }

    /** 键帽：化州文字母 + 小号拉丁提示；没有对应字母时退回拉丁字母。 */
    private View letterCap(final String latin) {
        String glyph = glyphCaps ? singleGlyph.get(latin) : null;
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setBackground(touchable(KEY, PRESSED, 10));

        TextView main = new TextView(getContext());
        main.setGravity(Gravity.CENTER);
        main.setTextColor(TEXT);
        if (glyph != null) {
            main.setText(glyph);
            main.setTypeface(hzFont);
            main.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
            cell.addView(main);

            TextView hint = new TextView(getContext());
            hint.setText(latin);
            hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
            hint.setTextColor(MUTED);
            hint.setGravity(Gravity.CENTER);
            cell.addView(hint);
        } else {
            main.setText(latin);
            main.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            cell.addView(main);
        }
        cell.setOnClickListener(v -> cb.onLetter(latin));
        return cell;
    }

    // ---------------- 更多页 ----------------

    private void toggleMore() {
        moreShown = !moreShown;
        keysPage.setVisibility(moreShown ? GONE : VISIBLE);
        morePage.setVisibility(moreShown ? VISIBLE : GONE);
    }

    private void showTab(int idx) {
        tab = idx;
        for (int i = 0; i < tabViews.length; i++) {
            tabViews[i].setBackground(touchable(i == idx ? KEY_ON : KEY_ALT, PRESSED, 10));
        }
        pageContent.removeAllViews();
        if (idx == TAB_LETTERS) {
            buildLetterTab();
            return;
        }
        String[] items = idx == TAB_PUNCT ? Punct.ZH : idx == TAB_EN ? Punct.EN : Punct.NUM;
        LinearLayout r = null;
        for (int i = 0; i < items.length; i++) {
            if (i % 5 == 0) {
                r = new LinearLayout(getContext());
                r.setOrientation(HORIZONTAL);
                pageContent.addView(r);
            }
            r.addView(punctCell(items[i]), keyLp(1f));
        }
    }

    private View punctCell(final String s) {
        TextView k = new TextView(getContext());
        k.setText(s);
        k.setTextSize(TypedValue.COMPLEX_UNIT_SP, s.length() > 1 ? 14 : 19);
        k.setTextColor(TEXT);
        k.setGravity(Gravity.CENTER);
        k.setBackground(touchable(KEY, PRESSED, 10));
        k.setOnClickListener(v -> cb.onPunct(s));
        return k;
    }

    private void buildLetterTab() {
        LinearLayout head = new LinearLayout(getContext());
        head.setOrientation(HORIZONTAL);
        TextView caps = key(glyphCaps ? "键帽：化州文（点此换拉丁）" : "键帽：拉丁（点此换化州文）",
                KEY_TONE, TEXT, 12);
        caps.setOnClickListener(v -> {
            glyphCaps = !glyphCaps;
            buildKeys();
            showTab(TAB_LETTERS);
        });
        head.addView(caps, new LinearLayout.LayoutParams(0, px(34), 1f));
        pageContent.addView(head);
        addLetterGroup("声母", initials);
        addLetterGroup("韵母", finals);
    }

    private void addLetterGroup(String title, List<Dict.Letter> items) {
        if (items == null || items.isEmpty()) return;
        TextView h = new TextView(getContext());
        h.setText(title);
        h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        h.setTextColor(MUTED);
        h.setPadding(px(6), px(6), 0, px(2));
        pageContent.addView(h);

        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % 5 == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(HORIZONTAL);
                pageContent.addView(row);
            }
            row.addView(letterCell(items.get(i)), keyLp(1f));
        }
    }

    private View letterCell(final Dict.Letter letter) {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setBackground(touchable(KEY, PRESSED, 10));
        cell.setPadding(0, px(2), 0, px(2));

        TextView g = new TextView(getContext());
        g.setText(letter.glyph);
        g.setTypeface(hzFont);
        g.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        g.setTextColor(TEXT);
        g.setGravity(Gravity.CENTER);
        cell.addView(g);

        TextView l = new TextView(getContext());
        l.setText(letter.roman);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        l.setTextColor(MUTED);
        l.setGravity(Gravity.CENTER);
        cell.addView(l);

        cell.setOnClickListener(v -> cb.onCandidate(letter.glyph));
        return cell;
    }

    // ---------------- 候选 ----------------

    public void setBuffer(String text) {
        boolean empty = text == null || text.isEmpty();
        bufferView.setText(empty ? "\u2022" : text);
        bufferView.setTextColor(empty ? MUTED : TEXT);
        hintView.setVisibility(empty ? VISIBLE : GONE);
    }

    public void setCandidates(List<Dict.Item> items) {
        candidateRow.removeAllViews();
        for (Dict.Item it : items) candidateRow.addView(chip(it));
        if (!items.isEmpty() && candidateRow.getParent() instanceof HorizontalScrollView) {
            ((HorizontalScrollView) candidateRow.getParent()).scrollTo(0, 0);
        }
    }

    private View chip(final Dict.Item it) {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setBackground(touchable(KEY, PRESSED, 10));
        cell.setPadding(px(7), px(2), px(7), px(1));

        TextView g = new TextView(getContext());
        g.setText(it.glyph);
        g.setTypeface(hzFont);
        g.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        g.setTextColor(TEXT);
        g.setGravity(Gravity.CENTER);
        cell.addView(g);

        TextView l = new TextView(getContext());
        l.setText(it.variant > 1 ? it.syllable + "\u00b7" + it.variant : it.syllable);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        l.setTextColor(MUTED);
        l.setGravity(Gravity.CENTER);
        cell.addView(l);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, px(52));
        lp.rightMargin = px(4);
        cell.setLayoutParams(lp);
        cell.setOnClickListener(v -> cb.onCandidate(it.glyph));
        return cell;
    }

    // ---------------- 控件工具 ----------------

    private TextView key(String label, int bg, int fg, float sizeSp) {
        TextView k = new TextView(getContext());
        k.setText(label);
        k.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        k.setTextColor(fg);
        k.setGravity(Gravity.CENTER);
        k.setBackground(touchable(bg, PRESSED, 10));
        k.setClickable(true);
        return k;
    }

    private LinearLayout.LayoutParams keyLp(float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(50), weight);
        lp.setMargins(px(2), px(2), px(2), px(2));
        return lp;
    }

    private void attachRepeat(View v, final Runnable action) {
        v.setOnTouchListener(new View.OnTouchListener() {
            private final Runnable repeat = new Runnable() {
                @Override
                public void run() {
                    action.run();
                    handler.postDelayed(this, 55);
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent e) {
                int a = e.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {
                    action.run();
                    handler.postDelayed(repeat, 380);
                } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    handler.removeCallbacks(repeat);
                }
                return true;
            }
        });
    }

    private StateListDrawable touchable(int normal, int pressed, float radiusDp) {
        StateListDrawable sd = new StateListDrawable();
        sd.addState(new int[]{android.R.attr.state_pressed}, box(pressed, radiusDp));
        sd.addState(new int[]{}, box(normal, radiusDp));
        return sd;
    }

    private GradientDrawable box(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(pxf(radiusDp));
        return g;
    }

    private int px(float dp) {
        return Math.round(dp * d);
    }

    private float pxf(float dp) {
        return dp * d;
    }
}

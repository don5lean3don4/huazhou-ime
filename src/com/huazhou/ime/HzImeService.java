package com.huazhou.ime;

import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.List;

/** 化州文输入法服务：拉丁拼音 -> 化州文音节字符。 */
public class HzImeService extends InputMethodService implements ImeView.Callback {

    private Dict dict;
    private Typeface hzFont;
    private ImeView view;
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashLogger();
        try {
            dict = new Dict(getAssets().open("dict.tsv"), getAssets().open("letter_index.tsv"));
        } catch (Exception e) {
            dict = new Dict(null, null);
        }
        hzFont = Typeface.createFromAsset(getAssets(), "hzfont.ttf");
    }

    /** 万一还是崩了，把堆栈写到 Android/data/com.huazhou.ime/files/crash.txt，方便排查。 */
    private void installCrashLogger() {
        try {
            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                try {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw));
                    java.io.File dir = getExternalFilesDir(null);
                    if (dir == null) dir = getFilesDir();
                    java.io.File f = new java.io.File(dir, "crash.txt");
                    java.io.FileWriter w = new java.io.FileWriter(f, true);
                    w.write("\n===== " + new java.util.Date().toString() + " =====\n");
                    w.write(sw.toString());
                    w.close();
                    android.util.Log.e("HzIme", sw.toString());
                } catch (Throwable ignored) {
                }
                if (prev != null) prev.uncaughtException(t, e);
            });
        } catch (Throwable ignored) {
        }
    }

    @Override
    public View onCreateInputView() {
        view = new ImeView(this, this, hzFont);
        view.bindLetters(dict.initials(), dict.finals());
        refresh();
        return view;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!restarting) buffer.setLength(0);
        refresh();
    }

    @Override
    public void onFinishInput() {
        buffer.setLength(0);
        super.onFinishInput();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    private void refresh() {
        if (view == null) return;
        view.setBuffer(buffer.toString());
        List<Dict.Item> items = dict.query(buffer.toString());
        view.setCandidates(items);
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                if (buffer.length() == 0) ic.finishComposingText();
                else ic.setComposingText(buffer.toString(), 1);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onLetter(String s) {
        if (buffer.length() >= 12) return;
        int n = buffer.length();
        if (n > 0 && isTone(buffer.charAt(n - 1))) {
            // 声调永远保持在末尾：先打调再打字母时把调挪到最后
            char tone = buffer.charAt(n - 1);
            buffer.deleteCharAt(n - 1);
            buffer.append(s).append(tone);
        } else {
            buffer.append(s);
        }
        refresh();
    }

    private static boolean isTone(char c) {
        return c >= '1' && c <= '6';
    }

    @Override
    public void onTone(String d) {
        if (buffer.length() == 0) return;
        int n = buffer.length();
        if (isTone(buffer.charAt(n - 1))) buffer.deleteCharAt(n - 1);
        buffer.append(d);
        refresh();
    }

    @Override
    public void onBackspace() {
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            refresh();
            return;
        }
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            CharSequence sel = ic.getSelectedText(0);
            if (sel != null && sel.length() > 0) ic.commitText("", 1);
            else ic.deleteSurroundingText(1, 0);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onSpace() {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            // 缓冲区还有拼音时，空格直接把拼音原样上屏，方便偶尔输入拉丁字母
            if (buffer.length() > 0) {
                ic.commitText(buffer.toString(), 1);
                buffer.setLength(0);
                refresh();
                return;
            }
            ic.commitText(" ", 1);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEnter() {
        // 回车键就是换行：先把没上屏的拼音上屏，再插一个换行
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            if (buffer.length() > 0) {
                ic.commitText(buffer.toString(), 1);
                buffer.setLength(0);
                refresh();
            }
            ic.commitText("\n", 1);
        } catch (Exception ignored) {
            // 某些输入框的 InputConnection 会抛异常，吞掉，别让输入法崩
        }
    }

    @Override
    public void onPunct(String s) {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            if (buffer.length() > 0) {
                ic.commitText(buffer.toString(), 1);
                buffer.setLength(0);
            }
            ic.commitText(s, 1);
            refresh();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCandidate(String glyph) {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            ic.commitText(glyph, 1);
            buffer.setLength(0);
            refresh();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClearBuffer() {
        buffer.setLength(0);
        refresh();
    }
}


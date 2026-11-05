package com.huazhou.ime;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 化州文音节字典：音节(带调) -> 化州文字形。
 *
 * 查询规则：
 *   1) 输入已带声调（末尾 1-6）且整音节命中时，只出该音节的字；
 *   2) 未带声调时，同音各调（ba1..ba6）按调序排列；
 *   3) 其余前缀匹配（ba -> bai / ban ...）排在后面。
 *
 * 注意：化州文在 Unicode 补充私用区 A（U+F4930 起），是 BMP 之外的双字符，
 * 所以取字形必须按 code point 切分，不能用 charAt。
 */
public class Dict {

    public static final class Item {
        public final String syllable;
        public final String glyph;
        /** 同音第几个字形（从 1 开始），个别音节有 4 个字形。 */
        public final int variant;

        Item(String syllable, String glyph, int variant) {
            this.syllable = syllable;
            this.glyph = glyph;
            this.variant = variant;
        }

        @Override
        public String toString() {
            return syllable + "=" + glyph;
        }
    }

    /** 字母表条目：声母 / 韵母。 */
    public static final class Letter {
        public final String roman;
        public final String glyph;

        Letter(String roman, String glyph) {
            this.roman = roman;
            this.glyph = glyph;
        }
    }

    private final String[] syllables;
    private final String[] glyphs;
    private final List<Letter> initials = new ArrayList<>();
    private final List<Letter> finals = new ArrayList<>();

    public Dict(InputStream dictIn, InputStream lettersIn) {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        try {
            readLines(dictIn, line -> {
                int t = line.indexOf('\t');
                if (t > 0) {
                    a.add(line.substring(0, t));
                    b.add(line.substring(t + 1));
                }
            });
        } catch (Exception ignored) {
        }
        syllables = a.toArray(new String[0]);
        glyphs = b.toArray(new String[0]);

        try {
            readLines(lettersIn, line -> {
                String[] p = line.split("\t");
                if (p.length >= 3) {
                    if ("I".equals(p[0])) initials.add(new Letter(p[1], p[2]));
                    else finals.add(new Letter(p[1], p[2]));
                }
            });
        } catch (Exception ignored) {
        }
    }

    public int size() {
        return syllables.length;
    }

    public List<Letter> initials() {
        return initials;
    }

    public List<Letter> finals() {
        return finals;
    }

    public int letterCount() {
        return initials.size() + finals.size();
    }

    private interface LineHandler {
        void line(String s);
    }

    private static void readLines(InputStream in, LineHandler h) throws Exception {
        if (in == null) return;
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"), 1 << 16);
        String line;
        while ((line = r.readLine()) != null) {
            if (!line.isEmpty()) h.line(line);
        }
        r.close();
    }

    private static boolean isTone(char c) {
        return c >= '1' && c <= '6';
    }

    private int lowerBound(String prefix) {
        int lo = 0, hi = syllables.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (syllables[mid].compareTo(prefix) < 0) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** @param buffer 用户当前输入，如 "ba" / "ba4"。 */
    public List<Item> query(String buffer) {
        List<Item> out = new ArrayList<>();
        if (buffer == null || buffer.isEmpty()) return out;

        String q = buffer.toLowerCase();
        String base = q;
        if (isTone(q.charAt(q.length() - 1))) base = q.substring(0, q.length() - 1);
        if (base.isEmpty()) return out;

        List<Item> exact = new ArrayList<>();
        List<Item> sameBase = new ArrayList<>();
        List<Item> extended = new ArrayList<>();

        for (int i = lowerBound(base); i < syllables.length; i++) {
            String s = syllables[i];
            if (!s.startsWith(base)) break;
            String g = glyphs[i];
            int variant = 0;
            for (int k = 0; k < g.length(); ) {
                int cp = g.codePointAt(k);
                k += Character.charCount(cp);
                variant++;
                Item it = new Item(s, new String(Character.toChars(cp)), variant);
                if (s.equals(q)) exact.add(it);
                else if (s.length() == base.length() + 1) sameBase.add(it);
                else extended.add(it);
            }
        }

        if (!exact.isEmpty()) return exact.size() > 160 ? exact.subList(0, 160) : exact;
        out.addAll(sameBase);
        out.addAll(extended);
        return out.size() > 160 ? out.subList(0, 160) : out;
    }
}

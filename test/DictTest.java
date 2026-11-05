import com.huazhou.ime.Dict;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 纯 JVM 测试：验证字典解析 + 候选匹配（不依赖 Android）。 */
public class DictTest {

    private static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) {
            failures++;
            System.out.println("FAIL: " + msg);
        }
    }

    public static void main(String[] args) throws Exception {
        String assets = args.length > 0 ? args[0] : "../assets";
        InputStream dictIn = new FileInputStream(assets + "/dict.tsv");
        InputStream letterIn = new FileInputStream(assets + "/letter_index.tsv");
        Dict d = new Dict(dictIn, letterIn);

        System.out.println("syllables=" + d.size() + " letters=" + d.letterCount()
                + " initials=" + d.initials().size() + " finals=" + d.finals().size());
        check(d.size() == 9284, "syllable count");
        check(d.letterCount() == 61, "letter count");
        check(d.initials().size() == 20, "initial count");
        check(d.finals().size() == 41, "final count");

        // 1. 每个字形都必须是 BMP 之外的单码位（U+F0000..U+FFFFF）
        for (Dict.Item it : d.query("a")) {
            int cp = it.glyph.codePointAt(0);
            check(it.glyph.codePointCount(0, it.glyph.length()) == 1, "single code point: " + it);
            check(cp >= 0xF0000 && cp <= 0xFFFFF, "plane-15 pua: " + it);
            check(it.glyph.length() == 2, "surrogate pair: " + it);
        }

        // 2. 全库统计：直接按文件校验（query 有 160 上限）
        int totalGlyphs = 0;
        int badGlyph = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(assets + "/dict.tsv"), "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] p = line.split("\t");
            String g = p[1];
            totalGlyphs += g.codePointCount(0, g.length());
            for (int i = 0; i < g.length(); ) {
                int cp = g.codePointAt(i);
                if (cp < 0xF0000 || cp > 0xFFFFF) badGlyph++;
                i += Character.charCount(cp);
            }
        }
        reader.close();
        System.out.println("glyphs in file: " + totalGlyphs + ", outside plane-15 PUA: " + badGlyph);
        check(totalGlyphs == 18600, "syllable glyph count");
        check(totalGlyphs + d.letterCount() == 18661, "table total");
        check(badGlyph == 0, "all glyphs in plane-15 PUA");

        // 3. 带调精确匹配，只出该音节
        List<Dict.Item> ba4 = d.query("ba4");
        check(!ba4.isEmpty(), "ba4 has candidates");
        check(ba4.get(0).syllable.equals("ba4"), "ba4 exact first, got " + ba4.get(0).syllable);
        int n4 = 0;
        for (Dict.Item it : ba4) if (it.syllable.equals("ba4")) n4++;
        check(n4 == 2, "ba4 variant count = " + n4);
        check(ba4.size() == 2, "tone query returns only that syllable");
        System.out.println("ba4 -> " + ba4);

        // 4. 同一基础音节按 1-6 调排列
        List<Dict.Item> ba = d.query("ba");
        Set<String> order = new LinkedHashSet<>();
        for (Dict.Item it : ba) order.add(it.syllable);
        String joined = String.join(" ", order);
        System.out.println("ba -> " + joined);
        check(joined.startsWith("ba1 ba2 ba3 ba4 ba5 ba6 "), "tone order: " + joined);

        // 5. 前缀扩展（ba -> bai / ban ...）排在基础音节之后
        check(joined.indexOf("bai1") > joined.indexOf("ba6"), "extensions after base tones");

        // 6. 空串 / 无匹配 / 只有声调
        check(d.query("").isEmpty(), "empty query");
        check(d.query("qq").isEmpty(), "no match");
        check(d.query("1").isEmpty(), "tone only");

        // 7. 上界
        check(d.query("n").size() <= 160, "cap 160");

        // 8. 字母表：每个条目都是一个字形
        for (Dict.Letter l : d.initials()) check(l.glyph.codePointCount(0, l.glyph.length()) == 1, "initial glyph " + l.roman);
        for (Dict.Letter l : d.finals()) check(l.glyph.codePointCount(0, l.glyph.length()) == 1, "final glyph " + l.roman);
        check("b".equals(d.initials().get(0).roman), "first initial b");
        boolean hasSr = false;
        for (Dict.Letter l : d.initials()) if (l.roman.equals("sr")) hasSr = true;
        check(hasSr, "initial sr");
        boolean hasEau = false;
        for (Dict.Letter l : d.finals()) if (l.roman.equals("eau")) hasEau = true;
        check(hasEau, "final eau");

        // 9. 抽查真实音节
        for (String q : new String[]{"ngo5", "tui2", "zau1", "fa1", "m4"}) {
            List<Dict.Item> res = d.query(q);
            check(!res.isEmpty(), "query " + q + " non-empty");
            if (!res.isEmpty()) {
                check(res.get(0).syllable.equals(q), q + " exact first");
                System.out.println(q + " -> U+" + Integer.toHexString(res.get(0).glyph.codePointAt(0)).toUpperCase()
                        + " (" + res.size() + " candidates)");
            }
        }

        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " FAILURES");
        if (failures > 0) System.exit(1);
    }
}

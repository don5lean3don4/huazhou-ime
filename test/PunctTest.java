import com.huazhou.ime.Punct;

import java.util.HashSet;
import java.util.Set;

/** 标点表自检：非空、无重复、长度合理。 */
public class PunctTest {

    private static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) {
            failures++;
            System.out.println("FAIL: " + msg);
        }
    }

    static void scan(String name, String[] arr, boolean expectWide) {
        check(arr != null && arr.length > 10, name + " should have >10 entries");
        Set<String> seen = new HashSet<>();
        for (String s : arr) {
            check(s != null && !s.isEmpty(), name + " empty entry");
            check(s.length() <= 2, name + " entry too long: " + s);
            check(seen.add(s), name + " duplicate: " + s);
            if (expectWide) {
                check(s.charAt(0) > 0x7F, name + " should be non-ASCII: " + s);
            }
        }
        System.out.println(name + ": " + arr.length + " entries, no duplicates");
    }

    public static void main(String[] args) {
        scan("ZH", Punct.ZH, true);
        scan("EN", Punct.EN, false);
        scan("NUM", Punct.NUM, false);
        check(Punct.ZH[0].equals("\uFF0C"), "first zh punctuation is fullwidth comma");
        System.out.println(failures == 0 ? "PUNCT TESTS PASSED" : failures + " FAILURES");
        if (failures > 0) System.exit(1);
    }
}

package net.ddns.logick;

import java.util.List;
import java.util.ListIterator;

public class Util {
    public static List reverseList(List source) {
        ListIterator fromBeginning = source.listIterator();
        ListIterator fromEnd = source.listIterator(source.size());
        for (int i = 0; i < source.size() / 2; i++) {
            Object o1 = fromBeginning.next();
            Object o2 = fromEnd.previous();
            fromBeginning.set(o2);
            fromEnd.set(o1);
        }
        return source;
    }
}

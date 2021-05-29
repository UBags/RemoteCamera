package com.costheta.text.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class PatternMatchedStrings {

    ArrayList<MatchedString> matchedStrings;

    private static final String startString = "[";
    private static final String endString = "]";
    private static final String comma = ",";

    public PatternMatchedStrings() {
        matchedStrings = new ArrayList<>();
    }

    public PatternMatchedStrings(ArrayList<MatchedString> matchedStrings) {
        this.matchedStrings = matchedStrings;
        sort();
    }

    public ArrayList<MatchedString> getMatchedStrings() {
        return matchedStrings;
    }

    public void setMatchedStrings(ArrayList<MatchedString> matchedStrings) {
        this.matchedStrings = matchedStrings;
        sort();
    }

    public void add(PatternMatchedStrings another) {
        for (MatchedString entry : another.matchedStrings) {
            matchedStrings.add(entry);
        }
        sort();
    }

    public void add(MatchedString anotherString) {
        matchedStrings.add(anotherString);
        sort();
    }

    public int size() {
        return matchedStrings.size();
    }

    public MatchedString get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " cannot be less than 0");
        }
        if (index >= matchedStrings.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " has to be less than current size " + matchedStrings.size());
        }
        return matchedStrings.get(index);
    }

    public MatchedString remove(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " cannot be less than 0");
        }
        if (index >= matchedStrings.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " has to be less than current size " + matchedStrings.size());
        }
        return matchedStrings.remove(index);
    }

    public void sort() {
        descendingSort();
    }

    public void descendingSort() {
        Collections.sort(matchedStrings, new Comparator<MatchedString>() {

            @Override
            public int compare(MatchedString m1, MatchedString m2) {
                return (m2.score - m1.score);
            }

        });
    }

    public void ascendingSort() {
        Collections.sort(matchedStrings, new Comparator<MatchedString>() {

            @Override
            public int compare(MatchedString m1, MatchedString m2) {
                return (m1.score - m2.score);
            }

        });
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(startString);
        Iterator iterator = matchedStrings.iterator();
        if (iterator.hasNext()) {
            sb.append(iterator.next());
        }
        while (iterator.hasNext()) {
            sb.append(comma);
            sb.append(iterator.next());
        }
        sb.append(endString);
        return sb.toString();
    }


}

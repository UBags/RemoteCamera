package com.costheta.text.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CharacterUtils {

    private static final Logger logger = LogManager.getLogger(CharacterUtils.class);

    public static final String EMPTY_STRING = "";

    public static final String removeEmptyCharacters(String inputString) {
        return removeSpaces(inputString);
    }

    private static final char DIGIT_CHARACTER = 'D';
    private static final char ALPHABET_CHARACTER = 'A';

    public static String getOnlyAlphabetsAndNumbers(String inputString) {
        return inputString
                .replace("(", "")
                .replace(")", "")
                .replace("[", "")
                .replace("]", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "")
                .replace(",", "")
                .replace("{", "")
                .replace("'", "")
                .replace("\"", "")
                .replace("}", "")
                .replace("/", "")
                .replace("|", "")
                .replace("\\", "")
                .replace(";", "")
                .replace(":", "")
                .replace("!", "")
                .replace("<", "")
                .replace(">", "")
                .replace("*", "")
                .replace("©", "")
                .replace("’", "")
                .replace("%", "")
                .replace("@", "")
                .replace("#", "")
                .replace("$", "")
                .replace("^", "")
                .replace("&", "")
                .replace("~", "")
                .replace("`", "")
                .replace("?", "")
                .trim()
                .toUpperCase();
    }

    public static String removeSpaces(String inputString) {
        return inputString.replace(" ", "").trim().toUpperCase();
    }

    public static String getOnlyNumbers(String input) {
        return input
                .replace("B","8")
                .replace("C","6")
                .replace("D","0")
                .replace("G","6")
                .replace("I","1")
                .replace("J","7")
                .replace("L","4")
                .replace("O","0")
                .replace("Q","0")
                .replace("S","5")
                .replace("T","1")
                .replace("Z","2")
                .trim();
    }

    public static String getOnlyAlphabets(String input) {
        return input
                .replace("8","B")
                .replace("1","I")
                .replace("7","T")
                .replace("4","L")
                .replace("0","O")
                .replace("5","S")
                .replace("2","Z")
                .trim()
                .toUpperCase();
    }

    public static final String getCommonString(String[] strings) {
        int n = strings.length;
        if (n == 0) {
            return EMPTY_STRING;
        }
        if (n == 1) {
            return strings[0];
        }
        HashMap<String, Integer> matchesCount = new HashMap<>();
        for (int i = 0; i < n; ++i) {
            String key = strings[i];
            if (EMPTY_STRING.equals(key)) {
                continue;
            }
            if (matchesCount.containsKey(key)) {
                Integer value = matchesCount.get(key);
                int occurences = value.intValue();
                matchesCount.replace(key, Integer.valueOf(++occurences));
            } else {
                matchesCount.put(key, Integer.valueOf(1));
            }
        }

        Iterator iterator = matchesCount.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry) iterator.next();
            String key = (String) mapElement.getKey();
            Integer value = (Integer) mapElement.getValue();
            if (value * 1.0 / n > 0.6) {
                return key;
            }
        }

        HashMap<String, Integer> sortedMatches = sortAscendingByValue(matchesCount);

        ArrayList<String> shortList = new ArrayList<>();
        int count = 0;
        iterator = sortedMatches.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry) iterator.next();
            String key = (String) mapElement.getKey();
            Integer value = (Integer) mapElement.getValue();
            count += value;
            shortList.add(key);
            if (count * 1.0 / n > 0.6) {
                break;
            }
        }

        String newString = EMPTY_STRING;
        for (int i = 0; i < shortList.size(); ++i) {
            newString = mash(newString, shortList.get(i));
        }

        return newString;
    }

    public static final String getCommonString(ArrayList<String> strings) {
        int n = strings.size();
        if (n == 0) {
            return EMPTY_STRING;
        }
        if (n == 1) {
            return strings.get(0);
        }
        HashMap<String, Integer> matchesCount = new HashMap<>();
        for (int i = 0; i < n; ++i) {
            String key = strings.get(i);
            if (EMPTY_STRING.equals(key)) {
                continue;
            }
            if (matchesCount.containsKey(key)) {
                Integer value = matchesCount.get(key);
                int occurences = value.intValue();
                matchesCount.replace(key, Integer.valueOf(++occurences));
            } else {
                matchesCount.put(key, Integer.valueOf(1));
            }
        }

        Iterator iterator = matchesCount.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry) iterator.next();
            String key = (String) mapElement.getKey();
            Integer value = (Integer) mapElement.getValue();
            // if the occurrences are more than 50% of the total occurences, then
            // most likely this is the best fit value
            if ((value * 1.0 / n) > 0.5) {
                return key;
            }
        }

        HashMap<String, Integer> sortedMatches = sortAscendingByValue(matchesCount);

        // ArrayList<String> shortList = new ArrayList<>();
        // int count = 0;
        iterator = sortedMatches.entrySet().iterator();
        boolean firstElementExists = iterator.hasNext();
        if (firstElementExists) {
            Map.Entry mapElement = (Map.Entry) iterator.next();
            String key = (String) mapElement.getKey();
            return key;
        }

//        while (iterator.hasNext()) {
//            Map.Entry mapElement = (Map.Entry) iterator.next();
//            String key = (String) mapElement.getKey();
//            Integer value = (Integer) mapElement.getValue();
//            count += value;
//            shortList.add(key);
//            if (count * 1.0 / n > 0.6) {
//                break;
//            }
//        }

//        String newString = EMPTY_STRING;
//        for (int i = 0; i < shortList.size(); ++i) {
//            newString = mash(newString, shortList.get(i));
//        }

        return EMPTY_STRING;
    }

    public static final String getCommonString(PatternMatchedStrings matchedStrings) {
        int n = matchedStrings.size();
        if (n == 0) {
            return EMPTY_STRING;
        }
        if (n == 1) {
            return matchedStrings.get(0).string;
        }
        matchedStrings.descendingSort();
        int highestScore = matchedStrings.get(0).score;
        HashMap<String, Integer> matchesCount = new HashMap<>();
        for (int i = 0; i < n; ++i) {
            String key = matchedStrings.get(i).string;
            int score = matchedStrings.get(i).score;
            if (score < highestScore) {
                break;
            }
            if (EMPTY_STRING.equals(key)) {
                continue;
            }
            if (matchesCount.containsKey(key)) {
                Integer value = matchesCount.get(key);
                int occurences = value.intValue();
                matchesCount.replace(key, Integer.valueOf(++occurences));
            } else {
                matchesCount.put(key, Integer.valueOf(1));
            }
        }

        Iterator iterator = matchesCount.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry) iterator.next();
            String key = (String) mapElement.getKey();
            Integer value = (Integer) mapElement.getValue();
            // if the occurrences are more than 50% of the total occurences, then
            // most likely this is the best fit value
            if ((value * 1.0 / n) > 0.5) {
                return key;
            }
        }

        HashMap<String, Integer> sortedMatches = sortAscendingByValue(matchesCount);

        iterator = sortedMatches.entrySet().iterator();
        boolean firstElementExists = iterator.hasNext();
        if (firstElementExists) {
            Map.Entry mapElement = (Map.Entry) iterator.next();
            String key = (String) mapElement.getKey();
            return key;
        }
        return EMPTY_STRING;
    }


    public static final String getCommonString(ArrayList<String> strings, String pattern) {
        int n = strings.size();
        if (n == 0) {
            return EMPTY_STRING;
        }
        ArrayList<String> validStrings = new ArrayList<>();
        for (String aString : strings) {
            String verifiedString = verifyStringAgainstPattern(aString, pattern);
            if (!EMPTY_STRING.equals(verifiedString)) {
                validStrings.add(verifiedString);
            }
        }
        if (validStrings.size() == 0) {
            return EMPTY_STRING;
        }
        String[] remainingStrings = new String[validStrings.size()];
        for (int i = 0; i < validStrings.size(); ++i) {
            remainingStrings[i] = validStrings.get(i);
        }
        return getCommonString(remainingStrings);
    }

    public static final String getCommonString(String[] strings, String pattern) {
        int n = strings.length;
        if (n == 0) {
            return EMPTY_STRING;
        }
        ArrayList<String> validStrings = new ArrayList<>();
        for (String aString : strings) {
            String verifiedString = verifyStringAgainstPattern(aString, pattern);
            if (!EMPTY_STRING.equals(verifiedString)) {
                validStrings.add(verifiedString);
            }
        }
        if (validStrings.size() == 0) {
            return EMPTY_STRING;
        }
        String[] remainingStrings = new String[validStrings.size()];
        for (int i = 0; i < validStrings.size(); ++i) {
            remainingStrings[i] = validStrings.get(i);
        }
        return getCommonString(remainingStrings);
    }

    public static final String verifyStringAgainstPattern(String aString, String pattern) {
        logger.trace("  Comparing " + aString + " against " + pattern);
        aString = aString.toUpperCase().trim();
        pattern = pattern.toUpperCase();
        if (aString.length() != pattern.length()) {
            return EMPTY_STRING;
        }
        if (aString.length() == 0) {
            return EMPTY_STRING;
        }
        boolean isOK = true;
        StringBuilder thisString = new StringBuilder();
        for (int i = 0; i < aString.length(); ++i) {
            if(pattern.charAt(i) == 'A') {
                String character = aString.substring(i,i+1);
                char thisCharacter = getOnlyAlphabets(character).charAt(0);
                logger.trace("    Comparing " + thisCharacter + " against " + 'A' + " : ");
                if (!Character.isLetter(thisCharacter)) {
                    isOK = false;
                    logger.trace("false");
                } else {
                    logger.trace("true");
                    thisString.append(thisCharacter);
                }
            } else {
                if (pattern.charAt(i) == 'D') {
                    String character = aString.substring(i,i+1);
                    char thisCharacter = getOnlyNumbers(character).charAt(0);
                    logger.trace("    Comparing " + thisCharacter + " against " + 'D' + " : ");
                    if (!Character.isDigit(thisCharacter)) {
                        logger.trace("false");
                        isOK = false;
                    }  else {
                        logger.trace("true");
                        thisString.append(thisCharacter);
                    }
                }
            }
        }
        if (!isOK) {
            return EMPTY_STRING;
        }
        return thisString.toString();
    }

    public static final <K extends Comparable<K>,T> HashMap<K,T> sortAscendingByKey(HashMap<K,T> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o1.getKey()).compareTo(o2.getKey());
            }
        });

        // put data from sorted list to hashmap
        HashMap<K, T> temp = new LinkedHashMap<K, T>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final <K extends Comparable<K>,T> HashMap<K,T> sortDescendingByKey(HashMap<K,T> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o2.getKey()).compareTo(o1.getKey());
            }
        });

        // put data from sorted list to hashmap
        HashMap<K, T> temp = new LinkedHashMap<K, T>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final <K,T extends Comparable<T>> HashMap<K,T> sortAscendingByValue(HashMap<K,T> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<K, T> temp = new LinkedHashMap<K, T>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final <K,T extends Comparable<T>> HashMap<K,T> sortDescendingByValue(HashMap<K,T> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<K, T> temp = new LinkedHashMap<K, T>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final String mash(String one, String two) {
        // this needs to be written better
        if (two.length() <= one.length()) {
            return one;
        }
        String extra = two.substring(one.length());
        String newString = new StringBuilder().append(one).append(extra).toString();
        return newString;
    }

    public static final String matchStringToPattern(String string, String pattern) {
        // pattern.split("(?!^)");
        boolean lengthMatched = false;
        if (string.length() < pattern.length()) {
            return string;
        }
        if (string.length() == pattern.length()) {
            lengthMatched = true;
        }
        if (lengthMatched) {
            char[] patternCharacters = pattern.toCharArray();
            char[] stringCharacters = string.toCharArray();
            char[] correctedArray = new char[patternCharacters.length];
            for (int i = 0; i < patternCharacters.length; ++i) {
                if (Character.toUpperCase(patternCharacters[i]) == DIGIT_CHARACTER) {
                    correctedArray[i] = getNumeralForCharacter(stringCharacters[i]);
                } else {
                    if (Character.toUpperCase(patternCharacters[i]) == ALPHABET_CHARACTER) {
                        correctedArray[i] = getCharacterForNumeral(stringCharacters[i]);
                    } else {
                        correctedArray[i] = Character.MIN_VALUE;
                    }
                }
            }
            return String.valueOf(correctedArray);
        } else {
            // get substrings of length equal to pattern.length() from the strings and
            // compare the strings
            // ------- TBD
            //            ArrayList<String> possibilities = new ArrayList<>();
            //            while (true) {
            //                for (int i = 0; )
            //
            //
            //            }

        }
        // if string and pattern do not match, check where fitment occurs based on regex
        // TBD
        return string;
    }

    private static final String matchString(String string, String pattern) {
        // pattern.split("(?!^)");

        char[] patternCharacters = pattern.toCharArray();
        char[] stringCharacters = string.toCharArray();
        char[] correctedArray = new char[patternCharacters.length];
        for (int i = 0; i < patternCharacters.length; ++i) {
            if (Character.toUpperCase(patternCharacters[i]) == 'D') {
                correctedArray[i] = getNumeralForCharacter(stringCharacters[i]);
            } else {
                if (Character.toUpperCase(patternCharacters[i]) == 'A') {
                    correctedArray[i] = getCharacterForNumeral(stringCharacters[i]);
                } else {
                    correctedArray[i] = Character.MIN_VALUE;
                }
            }
        }
        return String.valueOf(correctedArray);
    }


    public static final char getNumeralForCharacter(char input) {
        char c = Character.MIN_VALUE;
        switch(input) {
            case 'a':
                return '8';
            case 'b':
                return '6';
            case 'c':
                return '0';
            case 'd':
                return c;
            case 'e':
                return '8';
            case 'f':
                return '1';
            case 'g':
                return '9';
            case 'h':
                return c;
            case 'i':
                return '1';
            case 'j':
                return '7';
            case 'k':
                return c;
            case 'l':
                return '1';
            case 'm':
                return c;
            case 'n':
                return c;
            case 'o':
                return '0';
            case 'p':
                return c;
            case 'q':
                return '9';
            case 'r':
                return c;
            case 's':
                return '5';
            case 't':
                return '1';
            case 'u':
                return c;
            case 'v':
                return c;
            case 'w':
                return c;
            case 'x':
                return c;
            case 'y':
                return c;
            case 'z':
                return '2';
            case 'A':
                return c;
            case 'B':
                return '8';
            case 'C':
                return '6';
            case 'D':
                return '0';
            case 'E':
                return '6';
            case 'F':
                return c;
            case 'G':
                return '6';
            case 'H':
                return '4';
            case 'I':
                return '1';
            case 'J':
                return '7';
            case 'K':
                return c;
            case 'L':
                return '1';
            case 'M':
                return c;
            case 'N':
                return c;
            case 'O':
                return '0';
            case 'P':
                return c;
            case 'Q':
                return '0';
            case 'R':
                return c;
            case 'S':
                return '5';
            case 'T':
                return '7';
            case 'U':
                return c;
            case 'V':
                return c;
            case 'W':
                return c;
            case 'X':
                return '8';
            case 'Y':
                return c;
            case 'Z':
                return '2';
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '0':
                return input;
            default:
                return c;
        }
    }

    public static final char getCharacterForNumeral(char input) {
        char upperCase = Character.toUpperCase(input);
        char c = Character.MIN_VALUE;
        switch(upperCase) {
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
                return upperCase;
            case '1':
                return 'I';
            case '2':
                return 'Z';
            case '3':
                return c;
            case '4':
                return c;
            case '5':
                return 'S';
            case '6':
                return 'B';
            case '7':
                return 'T';
            case '8':
                return 'B';
            case '9':
                return c;
            case '0':
                return 'O';
            default:
                return c;
        }
    }

    public static final ArrayList<ArrayList<String>> forceFitIntoMultiLineMultiColumn(ArrayList<String> strings, int numberOfColumns) {
        ArrayList<ArrayList<String>> multiLineMultiColumn = new ArrayList<>();
        ArrayList<String> line = new ArrayList<String>();
        for (int i = 0; i < strings.size(); ++i) {
            if (i == 0) {
                line.add(strings.get(i));
                continue;
            }
            if (i % numberOfColumns == 0) {
                multiLineMultiColumn.add(line);
                line = new ArrayList<>();
            }
            line.add(strings.get(i));
        }
        if (line.size() > 0) {
            multiLineMultiColumn.add(line);
        }
        return multiLineMultiColumn;
    }

}

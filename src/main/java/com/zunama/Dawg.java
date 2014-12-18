package main.java.com.zunama;

import java.util.*;

public class Dawg {

    private DawgState root;
    private String previousWord = "";
    private Map<String, DawgState> register = new HashMap<String, DawgState>();
    private int totalEdges = 0;

    private String currentPrefix;

    public Dawg(List<String> words) {
        root = new DawgState();
        root.setEndWord(false);
        insertWords(words);
        register = null;
    }

    public Dawg() {
        root = new DawgState();
        root.setEndWord(false);
        register = null;
    }


    public boolean search(String word) {
        DawgState current = root;

        for (Character c : word.toCharArray()) {
            if (current.getEdges().containsKey(c))
                current = current.getEdges().get(c);
            else
                return false;
        }

        return current.isEndWord();
    }

    public List<String> prefixSearch(String prefix) {

        if(prefix == null)
            throw new RuntimeException("Prefix is set to null");

        DawgState current = root;
        currentPrefix = prefix.toLowerCase();
        List<String> words = new ArrayList<String>();

        for (char letter : prefix.toLowerCase().toCharArray()) {
            if(current.getEdges().containsKey(letter))
                current = current.getEdges().get(letter);
            else
                return words;
        }

        prefixSearch(current, words, "");
        return words;
    }

    public boolean prefixExist(String prefix) {

        if(prefix == null)
            throw new RuntimeException("Prefix is set to null");

        DawgState current = root;

        for(char c : prefix.toCharArray()) {
            if(current.getEdges().containsKey(c))
                current = current.getEdges().get(c);
            else
                return false;
        }

        return true;
    }

    public int getTotalEdges() {
        return totalEdges;
    }

    private void prefixSearch(DawgState state, List<String> words, String currentString) {
        if (state.isEndWord()) {
            words.add(currentPrefix + currentString);
        }

        for (Character key : state.getEdges().keySet()) {
            DawgState nextStateToVist = state.getEdges().get(key);
            String newString = currentString + key;

            prefixSearch(nextStateToVist, words, newString);
        }
    }

    private void insertWords(List<String> words) {

        Collections.sort(words);
        for (String word : words) {
            insertWord(word);
        }
    }

    private void insertWord(String word) {
        if (word.compareTo(previousWord) < 0)
            throw new RuntimeException("Trying to insert a word out of order.");

        word = word.toLowerCase();

        String commonPrefix = getCommonPrefix(word, previousWord);
        String currentSuffix = word.substring(commonPrefix.length());

        DawgState lastState = getLastState(commonPrefix);

        if (lastState.getEdges().size() > 0) {
            replaceOrRegister(lastState);
        }

        addSufix(lastState, currentSuffix);

        previousWord = word;
    }

    private void addSufix(DawgState lastState, String currentSuffix) {
        char[] wordCharArray = currentSuffix.toCharArray();

        for (char c : wordCharArray) {
            DawgState nextState = new DawgState();
            lastState.getEdges().put(c, nextState);
            totalEdges++;
            lastState = nextState;
        }

        lastState.setEndWord(true);
    }

    private void replaceOrRegister(DawgState state) {
        Character c = getMostRecentAddedLetter(state);
        DawgState child = state.getEdges().get(c);

        if (child.getEdges().size() > 0) {
            replaceOrRegister(child);
        }

        if (register.containsKey(child.toString())) {
            state.getEdges().put(c, register.get(child.toString()));
            totalEdges--;
        } else {
            register.put(child.toString(), child);
        }
    }

    private Character getMostRecentAddedLetter(DawgState state) {
        Character out = null;
        for (Character key : state.getEdges().keySet()) {
            out = key;
        }
        return out;
    }

    private DawgState getLastState(String commonPrefix) {
        if (commonPrefix == null || commonPrefix.length() == 0)
            return root;

        DawgState current = root;

        for (char c : commonPrefix.toCharArray()) {
            current = current.getEdges().get(new Character(c));
        }

        return current;
    }

    private String getCommonPrefix(String word, String previousWord) {
        int count = 0;
        int minCheck = Math.min(word.length(), previousWord.length());

        for (int i = 0; i < minCheck; i++) {
            if (word.charAt(i) == previousWord.charAt(i))
                count++;
            else
                break;
        }

        return word.substring(0, count);
    }
}
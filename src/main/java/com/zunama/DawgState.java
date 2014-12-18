package main.java.com.zunama;

import java.util.LinkedHashMap;
import java.util.Map;

public class DawgState {
    private static int nextId = 0;

    private int id;
    private LinkedHashMap<Character, DawgState> edges = new LinkedHashMap<Character, DawgState>();
    private boolean endWord = false;

    public DawgState() {
        id = nextId++;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(endWord);

        for (Map.Entry<Character, DawgState> entry : edges.entrySet()){
            sb.append(entry.getKey());
            sb.append(entry.getValue().getId());
        }

        return sb.toString();
    }

    public int getId() {
        return id;
    }

    public LinkedHashMap<Character, DawgState> getEdges() {
        return edges;
    }

    public boolean isEndWord() {
        return endWord;
    }

    public void setEndWord(boolean endWord) {
        this.endWord = endWord;
    }
}
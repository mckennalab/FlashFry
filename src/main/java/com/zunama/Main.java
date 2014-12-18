package main.java.com.zunama;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {

            URL url = Main.class.getClassLoader().getResource("wordlist.txt");
            URI uri = null;

            try {
                uri = url.toURI();
            }
            catch(URISyntaxException ex){
                ex.printStackTrace();
            }

            Path path = Paths.get(uri);
            List<String> words = Files.readAllLines(path, StandardCharsets.UTF_8);

            long startTime = System.currentTimeMillis();
            Dawg dawg = new Dawg(words);
            long stopTime = System.currentTimeMillis();

            long duration = stopTime - startTime;

            System.out.println("Words read in: " + words.size());
            System.out.println("Total Edges: " + dawg.getTotalEdges());
            System.out.println("Built DAWG in: " + duration + " ms");

            startTime = System.currentTimeMillis();
            String prefix = "jama";

            System.out.println("Prefix exist in DAWG: " + dawg.prefixExist(prefix));

            List<String> foundWords = dawg.prefixSearch(prefix);

            stopTime = System.currentTimeMillis();
            duration = stopTime - startTime;

            for(String s : foundWords)
                System.out.println(s);

            System.out.println("Searched prefix found " + foundWords.size() + " words in: " + duration + " ms");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

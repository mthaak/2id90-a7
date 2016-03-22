
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Integer.max;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class CorpusReader {

    final static String CNTFILE_LOC = "samplecnt.txt";
    final static String VOCFILE_LOC = "samplevoc.txt";
    final static String CONFUSIONFILE_LOC = "confusion_matrix.txt";

    private HashMap<String, Integer> ngrams;
    private HashMap<String, Integer> confusions;
    private Set<String> vocabulary;

    private HashMap<Integer, Integer> countFrequencies;
    private int maxCount;
    static final int K = 250;

    public CorpusReader() throws IOException {
        readNGrams();
        readVocabulary();
        readConfusionMatrix();
    }

    /**
     * Returns the n-gram count of <NGram> in the file
     *
     *
     * @param nGram : space-separated list of words, e.g. "adopted by him"
     * @return 0 if <NGram> cannot be found, otherwise count of <NGram> in file
     */
    public int getNGramCount(String nGram) throws NumberFormatException {
        if (nGram == null || nGram.length() == 0) {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }
        Integer value = ngrams.get(nGram);
        return value == null ? 0 : value;
    }

    private void readNGrams() throws
            FileNotFoundException, IOException, NumberFormatException {
        ngrams = new HashMap<>();

        FileInputStream fis;
        fis = new FileInputStream(CNTFILE_LOC);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        while (in.ready()) {
            String phrase = in.readLine().trim();
            String s1, s2;
            int j = phrase.indexOf(" ");

            s1 = phrase.substring(0, j);
            s2 = phrase.substring(j + 1, phrase.length());

            int count = 0;
            try {
                count = Integer.parseInt(s1);
                ngrams.put(s2, count);
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("NumberformatError: " + s1);
            }
        }
    }

    private void readVocabulary() throws FileNotFoundException, IOException {
        vocabulary = new HashSet<>();

        FileInputStream fis = new FileInputStream(VOCFILE_LOC);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        while (in.ready()) {
            String line = in.readLine();
            vocabulary.add(line);
        }
    }

    double totalCount = 0.0;

    private void readConfusionMatrix() throws FileNotFoundException, IOException {
        confusions = new HashMap<>();

        FileInputStream fis = new FileInputStream(CONFUSIONFILE_LOC);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        while (in.ready()) {
            String phrase = in.readLine();//.trim();
            String s1, s2;
            int j = phrase.lastIndexOf(" ");

            s1 = phrase.substring(0, j);
            s2 = phrase.substring(j + 1, phrase.length());

            int count = 0;
            try {
                count = Integer.parseInt(s2);
                confusions.put(s1, count);
                totalCount++;
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("NumberformatError: " + s1);
            }
        }
    }

    public Set<String> getVocabulary() {
        return this.vocabulary;
    }

    /**
     * Returns the size of the number of unique words in the dataset
     *
     * @return the size of the number of unique words in the dataset
     */
    public int getVocabularySize() {
        return vocabulary.size();
    }

    /**
     * Returns the subset of words in set that are in the vocabulary
     *
     * @param set
     * @return
     */
    public HashSet<String> inVocabulary(Set<String> set) {
        HashSet<String> h = new HashSet<>(set);
        h.retainAll(vocabulary);
        return h;
    }

    public boolean inVocabulary(String word) {
        return vocabulary.contains(word);
    }

    public void determineCountFrequencies() {
        this.countFrequencies = new HashMap<>();

        List<Integer> counts = new ArrayList<>(this.ngrams.values());
        int i;
        for (i = 1; counts.contains(i); i++) {
            this.countFrequencies.put(i, Collections.frequency(counts, i));
        }
        this.maxCount = i;
    }

    public double getProbabilityGivenNext(String word, String nextWord) {
        if (word == null || nextWord == null) {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }

        double countBigram = getGoodTuringSmoothedCount(word + " " + nextWord);
        int countNextWord = getNGramCount(nextWord);
        double V = getVocabularySize();
        double probability = (countBigram + K) / (countNextWord + K * V);

        return probability;
    }

    public double getProbabiltyGivenPrev(String word, String prevWord) {
        if (word == null || prevWord == null) {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }

        double countBigram = getGoodTuringSmoothedCount(prevWord + " " + word);
        int countPrevWord = getNGramCount(prevWord);
        double V = getVocabularySize();
        double probability = (countBigram + K) / (countPrevWord + K * V);

        return probability;
    }

    private int getGoodTuringSmoothedCount(String ngram) {
        int N = this.ngrams.size();

        if (!this.ngrams.containsKey(ngram) || this.ngrams.get(ngram) == 0) {
            return this.countFrequencies.get(1) / N;
        } else {
            int c = this.ngrams.get(ngram);
            if (c < this.maxCount) {
                return (c + 1) * (this.countFrequencies.get(c + 1) / (this.countFrequencies.get(c) * N));
            } else {
                return c / N;
            }
        }
    }

    public double getProbability(String ngram) {
        if (ngram == null || ngram.length() == 0) {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }

        double count = getNGramCount(ngram);
        double V = getVocabularySize();

        if (count == Double.NaN) {
            System.out.println("unknown: " + ngram);
            count = 0.0;
        }

        return (count + K) / (count + K * V);
    }

    public String getCorrection(String originalWord, String correctedWord) {
        // Space is seen as character in the confusion matrix
        originalWord = " " + originalWord;
        correctedWord = " " + correctedWord;

        int endIndex = max(originalWord.length(), correctedWord.length());
        for (int i = 1; i < endIndex; i++) {
            if ((i == endIndex - 1 && originalWord.length() != correctedWord.length())
                    || originalWord.charAt(i) != correctedWord.charAt(i)) { // correction

                if (originalWord.length() == correctedWord.length()) { // transposition or substitution

                    if (i + 1 < originalWord.length() // transposition
                            && originalWord.charAt(i + 1) != correctedWord.charAt(i + 1)) {
                        return originalWord.substring(i, i + 2) + "|" + correctedWord.substring(i, i + 2);
                    } else { // substitution
                        return originalWord.substring(i, i + 1) + "|" + correctedWord.substring(i, i + 1);
                    }

                } else if (originalWord.length() > correctedWord.length()) { // deletion
                    return originalWord.substring(i - 1, i + 1) + "|" + correctedWord.substring(i - 1, i);
                } else if (originalWord.length() < correctedWord.length()) { // insertion
                    return originalWord.substring(i - 1, i) + "|" + correctedWord.substring(i - 1, i + 1);
                }
            }
        }

        return ""; // doesn't matter
    }

    public double getConfusionValue(String originalWord, String correctedWord) {
        if (originalWord.equals(correctedWord)) {
            return 0.0;
        }

        String correction = getCorrection(originalWord, correctedWord);
        Integer confusionValue = confusions.get(correction);

        // TODO: normalize
        return confusionValue != null ? confusionValue : 0;
    }
}

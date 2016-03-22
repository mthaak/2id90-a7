import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CorpusReader {

    final static String CNTFILE_LOC = "samplecnt.txt";
    final static String VOCFILE_LOC = "samplevoc.txt";

    private HashMap<String, Integer> ngrams;
    private Set<String> vocabulary;
    private HashMap<Integer, Integer> nGramCountFrequencies;

    final static int K = 250;
    private int maxCount;

    public CorpusReader() throws IOException {
        readNGrams();
        readVocabulary();
        determineNGramCountFrequencies();
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

    private void determineNGramCountFrequencies() {
        this.nGramCountFrequencies = new HashMap<>();

        List<Integer> counts = new ArrayList<>(this.ngrams.values());
        int i;
        for (i = 1; counts.contains(i); i++) {
            this.nGramCountFrequencies.put(i, Collections.frequency(counts, i));
        }
        this.maxCount = i;
    }

    /**
     * Returns the n-gram count of <NGram> in the file
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

    /**
     * Returns vocabulary
     *
     * @return vocabulary
     */
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

    public double getProbability(String nGram) {
        double count = getNGramCount(nGram);
        double V = getVocabularySize();
        return count / V;
    }

    public double getProbabilityGivenNext(String word, String nextWord) {
        if (word == null || word.length() == 0 || nextWord == null || nextWord.length() == 0) {
            throw new IllegalArgumentException("NGrams must be non-empty.");
        }

        double countBigram = getGoodTuringSmoothedCount(word + " " + nextWord);
        int countNextWord = getNGramCount(nextWord);
        double V = getVocabularySize();

        // Add-K smoothing
        double probability = (countBigram + K) / (countNextWord + K * V);

        return probability;
    }

    public double getProbabiltyGivenPrev(String word, String prevWord) {
        if (word == null || word.length() == 0 || prevWord == null || prevWord.length() == 0) {
            throw new IllegalArgumentException("NGrams must be non-empty.");
        }

        double countBigram = getGoodTuringSmoothedCount(prevWord + " " + word);
        int countPrevWord = getNGramCount(prevWord);
        double V = getVocabularySize();

        // Add-K smoothing
        double probability = (countBigram + K) / (countPrevWord + K * V);

        return probability;
    }

    private int getGoodTuringSmoothedCount(String ngram) {
        int N = this.ngrams.size();

        if (!this.ngrams.containsKey(ngram) || this.ngrams.get(ngram) == 0) {
            return this.nGramCountFrequencies.get(1) / N;
        } else {
            int c = this.ngrams.get(ngram);
            if (c < this.maxCount) {
                return (c + 1) * (this.nGramCountFrequencies.get(c + 1) / (this.nGramCountFrequencies.get(c) * N));
            } else {
                return c / N;
            }
        }
    }

}

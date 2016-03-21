
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SpellCorrector {

    final private CorpusReader cr;
    final private ConfusionMatrixReader cmr;

    final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz'".toCharArray();

    public SpellCorrector(CorpusReader cr, ConfusionMatrixReader cmr) {
        this.cr = cr;
        this.cmr = cmr;
    }

    final private int LAMBDA = 3;
    final private double SCALE_FACTOR = Math.pow(10, 19);
    
    public String correctPhrase(String phrase) {
        if (phrase == null || phrase.length() == 0) {
            throw new IllegalArgumentException("phrase must be non-empty.");
        }

        String[] words = phrase.split(" ");

        String[] finalWords = new String[words.length];
        for (int i = 0; i < words.length; i++) {
            // Find all candidate words
            String prevWord = i > 0 ? words[i - 1] : "";
            String word = words[i];
            String nextWord = i < words.length - 1 ? words[i + 1] : "";
            Map<String, Double> candidateWords = getCandidateWords(prevWord, word, nextWord);

            // Find best candidate
            Entry<String, Double> bestCandidate = null;
            for (Entry<String, Double> candidateWord : candidateWords.entrySet()) {
                if (bestCandidate == null || candidateWord.getValue() > bestCandidate.getValue()) {
                    bestCandidate = candidateWord;
                }
            }
            
            if (bestCandidate == null) {
                finalWords[i] = word;
            } else {
                finalWords[i] = bestCandidate.getKey();
            }            
        }

        String finalSuggestion = String.join(" ", finalWords);
        
        return finalSuggestion.trim();
    }

    /**
     * Returns a map with candidate words and their noisy channel probability.
     * @param prevWord
     * @param word
     * @param nextWord
     * @return 
     */
    
    // WHY DID YOU ADD prevWord and nextWord??
    
    public Map<String, Double> getCandidateWords(String prevWord, String word, String nextWord) {
        Map<String, Double> candidateWords = new HashMap<>();
        Set<String> similarWords = getSimilarWords(word);
        for (String similarWord : similarWords) {
            
            /*HashSet<String> h = new HashSet<>();
            h.add(prevWord);
            h.add(similarWord);
            
            HashSet<String> g = new HashSet<>();
            g = cr.inVocabulary(h);
            
            if (h.contains(g)) {
                // ok?
            }
            
            //int probability = cr.getNGramCount(similarWord);
            */
            
            //double probability = cr.getSmoothedCount(prevWord + " " + similarWord);
            
            double value = cr.getSmoothedCount(prevWord + " " + similarWord);
            double wordValue = cr.getSmoothedCount(" " + similarWord);
            double channelValue = value * Math.pow(wordValue, LAMBDA) * SCALE_FACTOR;
            
            System.out.println(similarWord + " " + channelValue);
            
            candidateWords.put(similarWord, channelValue); // for now use equal probability
        }
        
        return candidateWords;
    }

    /**
     * Returns list of words which have a Damerau-Levenshtein distance of at
     * most 1 from inputWord.
     */
    private Set<String> getSimilarWords(String inputWord) {
        Set<String> vocabulary = this.cr.getVocabulary();
        
        Set<String> similarWords = new HashSet<String>();
        for (String word: vocabulary){
            if (getDMDistance(inputWord, word) <= 1) {
                similarWords.add(word);
            }
        }

        return similarWords;
    }

    /**
     * Returns the Damerau-Levenshtein distance between strings a and b. 
     * Makes use of the dynamic programming algorithm.
     */
    private int getDMDistance(String a, String b) {
        if (a.length() == 0) {
            return b.length();
        }
        if (b.length() == 0) {
            return a.length();
        }

        // initialize matrix
        int[][] m = new int[b.length()+1][a.length()+1];

        // fill in initial values
        for (int i = 0; i <= b.length(); i++) {
            m[i][0] = i;
        }
        for (int j = 0; j <= a.length(); j++) {
            m[0][j] = j;
        }
        
        // fill in the rest
        for (int i = 1; i <= b.length(); i++) {
            for (int j = 1; j <= a.length(); j++) {
                if (b.charAt(i - 1) == a.charAt(j - 1)) {
                    m[i][j] = m[i - 1][j - 1];
                } else if (i > 1 && j > 1 && 
                        b.charAt(i - 1) == a.charAt(j - 2) && 
                        b.charAt(i - 2) == a.charAt(j - 1)) {
                    m[i][j] = Math.min(m[i][j - 1] + 1,         // insertion
                              Math.min(m[i - 1][j] + 1,         // deletion
                              Math.min(m[i - 1][j - 1] + 1,     // substitution
                                       m[i - 2][j - 2] + 1)));  // transposition 
                } else {
                    m[i][j] = Math.min(m[i][j - 1] + 1,         // insertion
                              Math.min(m[i - 1][j] + 1,         // deletion
                                       m[i - 1][j - 1] + 1));   // substitution
                }
            }
        }
        
        return m[b.length()][a.length()];
    }
}

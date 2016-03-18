
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
            candidateWords.put(similarWord, 0.1); // for now use equal probability
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

        // matrix
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
                int distanceToStringWithLengthMinusOne = 0;
                if (b.charAt(i - 1) != a.charAt(j - 1)) {
                    distanceToStringWithLengthMinusOne = 2;
                }
                    
                m[i][j] = Math.min(m[i][j - 1] + 1,
                        Math.min(m[i - 1][j] + 1, 
                                m[i][j]) + distanceToStringWithLengthMinusOne); 
                
                // if distance is already larger than 1, prune;
//                if (m[i][j] > 1) return 2;
            }
        }

        return m[b.length()][a.length()];
    }
}
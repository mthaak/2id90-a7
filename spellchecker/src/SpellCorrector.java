
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
        String[] finalWords = phrase.split(" ");
        String[] replacementsWords = new String[words.length];

        Map<String,Double> bestWords = new HashMap<>();
        
        for (int i = 0; i < words.length; i++) {
            // Find all candidate words
            String prevWord = i > 0 ? words[i - 1] : "<s>";
            String word = words[i];
            String nextWord = i < words.length - 1 ? words[i + 1] : "</s>";
            Map<String, Double> candidateWords = getCandidateWords(prevWord, word, nextWord);

            // Find best candidate
            Entry<String, Double> bestCandidate = null;
            for (Entry<String, Double> candidateWord : candidateWords.entrySet()) {
                if (bestCandidate == null || candidateWord.getValue() > bestCandidate.getValue()) {
                    bestCandidate = candidateWord;
                }
            }
            
            if (bestCandidate == null) {
                bestWords.put(word, 0.0);
                replacementsWords[i] = word;
            } else {
                bestWords.put(bestCandidate.getKey(), bestCandidate.getValue());
                replacementsWords[i] = bestCandidate.getKey();
            }
        }
        
        // initialize places which can still be changed
        Boolean[] possiblePositions = new Boolean[words.length];
        for (int i = 0; i < words.length; i++) {
            possiblePositions[i] = true;
        }
        
        // replace max 2 words with heighest count (not consecutive!)
        for (int i = 0; i < 3; i++) {
            Entry<String, Double> bestWord = null;
            int index = -1;
            for (Entry<String, Double> word : bestWords.entrySet()) {
                
                boolean stop = false;
                int index2 = -1;
                
                for (int j = 0; j < replacementsWords.length; j++) {
                    if (replacementsWords[j].equals(word.getKey())){
                        
                        if (possiblePositions[j] == false) {
                            stop = true;
                        }
                        
                        index2 = j;
                        break;
                    }
                }
                
                if (!stop) {
                    if (bestWord == null || word.getValue() > bestWord.getValue()) {
                        bestWord = word; 
                        index = index2;
                    }
                }
            }
            
            // replace
            if (index != -1 ) {
                possiblePositions[index] = false;
                if (index - 1 >= 0) {
                    possiblePositions[index-1] = false;
                }
                if (index + 1 <= words.length) {
                    possiblePositions[index+1] = false;
                }
                finalWords[index] = replacementsWords[index];
            }
        }
        
        for (int i =0 ; i < replacementsWords.length; i++) {
            System.out.println("best candidate words: " + replacementsWords[i]);
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
    
    public Map<String, Double> getCandidateWords(String prevWord, String word, String nextWord) {
        Map<String, Double> candidateWords = new HashMap<>();
        Set<String> similarWords = getSimilarWords(word);
        for (String similarWord : similarWords) {
            double prevValue = cr.getSmoothedCount(prevWord, similarWord); // P(wi-1 | wi)
            double nextValue = cr.getSmoothedCount(similarWord, nextWord); // P(wi | wi+1)
            
            double similarWordValue = cr.getCountTest(similarWord);
            double wordValue = cr.getCountTest(word);
            double confusionValue = cr.getConfusionValue(similarWord, word); // TODO
            
            double channelValue = wordValue * Math.pow(confusionValue, LAMBDA) / similarWordValue; // P(x|w)
            //channelValue *= SCALE_FACTOR;
            
            double chance = channelValue * prevValue * nextValue; // P(x|w) * P(wi-1|wi) * P(wi+1|wi)
            
            candidateWords.put(similarWord, chance); // for now use equal probability
        }
        
        return candidateWords;
    }

    /**
     * Returns list of words which have a Damerau-Levenshtein distance of at
     * most 1 from inputWord.
     */
    private Set<String> getSimilarWords(String inputWord) {
        Set<String> vocabulary = this.cr.getVocabulary();
        
        Set<String> similarWords = new HashSet<>();
        for (String word: vocabulary){
            if (getDMDistance(inputWord, word) == 1) {
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

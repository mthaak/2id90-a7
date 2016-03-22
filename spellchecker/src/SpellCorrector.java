
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class SpellCorrector {

    final private CorpusReader cr;
    final private ConfusionMatrixReader cmr;

    final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz'".toCharArray();

    public SpellCorrector(CorpusReader cr, ConfusionMatrixReader cmr) {
        this.cr = cr;
        this.cmr = cmr;
    }

    final private int LAMBDA = 3;
    //final private double SCALE_FACTOR = Math.pow(10, 19);
    private final double NO_ERROR = 0.97;

    public String correctPhrase(String phrase) {
        if (phrase == null || phrase.length() == 0) {
            throw new IllegalArgumentException("phrase must be non-empty.");
        }

        List<String> words = Arrays.asList(phrase.split(" "));
        
        Map<String, Set<String>> similarWordsPerWord = words.stream()
                .collect(Collectors.toMap(word -> word, word -> getSimilarWords(word)));
        
        for (Entry<String,Set<String>> entry : similarWordsPerWord.entrySet()) {
            Set<String> words1 = entry.getValue();
            System.out.println("similarwords size of " + entry.getKey() + " value: " + words1.size());
            /*for (String word : words1) {
                System.out.println(word);
            }*/
        }

        List<String> possiblePhrases = getPossiblePhrases(words, similarWordsPerWord, 2, false);
        
        System.out.println("Nr of possible phrases: " + possiblePhrases.size());
        
        //possiblePhrases.forEach(sen -> System.out.println(sen));
        

        double bestProbability = 0.0;
        String bestSentence = "";
        
        for (String sentence : possiblePhrases) {
            //System.out.println(sentence);
            double probability = calculateProbabilityForSentence(sentence,phrase);
            //System.out.println("probability: " + probability);
            if (probability > bestProbability) {
                bestProbability = probability;
                bestSentence = sentence;
                //System.out.println(sentence + " " + probability);
            }
        }
        
        return bestSentence;
    }

    private double calculateProbabilityForSentence(String sentence, String originalSentence) {
        String[] words = sentence.split(" ");
        String[] originalWords = originalSentence.split(" ");
        
        // How to do confusionValue?
        // How to no_error
        
        double probability = 0.0;
        
        for (int i = 0; i < words.length; i++) {
            String prevWord = i > 0 ? words[i - 1] : "<s>";
            String word = words[i];
            String originalWord = originalWords[i];
            String nextWord = i < words.length - 1 ? words[i + 1] : "</s>";
            
            double prevValue = cr.getSmoothedCount(prevWord, word);
            double nextValue = cr.getSmoothedCount(word, nextWord);
            
            double wordValue = cr.getWordValue(word);
            double originalWordValue = cr.getWordValue(originalWord);
            
            double channelValue = 0.0;
                    
            if (word.equals(originalWord)) {
                channelValue = Math.pow(cr.getConfusionValue(word, 500), LAMBDA);
                channelValue *= NO_ERROR;
            } else {
                double confusionValue = cr.getConfusionValue(word, originalWord);
                channelValue = wordValue * Math.pow(confusionValue, LAMBDA);// / originalWordValue;
                channelValue *= (1 - NO_ERROR);
            }
            
            double chance = channelValue * prevValue * nextValue;  
            probability += chance;
            //System.out.println("probability: " + probability);
        }
        return probability;
    }
    

    /**
     * Returns list of all possible corrected phrases for {@code phrase}. At
     * most two non-consecutive words in the phrase are be corrected.
     */
    private List<String> getPossiblePhrases(List<String> phrase, Map<String, Set<String>> similarWords,
            int correctionsLeft, boolean prevWasCorrection) {
        // BASE
        if (phrase.isEmpty()) {
            return Arrays.asList("");
        }

        // STEP
        String firstWord = phrase.get(0);
        //System.out.println("firstWord: " + firstWord + ", phrase: " + phrase.toString());
        return similarWords.get(firstWord).stream()
                .filter(similarWord -> (similarWord.equals(firstWord) || (correctionsLeft > 0 && !prevWasCorrection)))
                .flatMap(similarWord -> {
                    List<String> remainingPhrase = phrase.subList(1, phrase.size());

                    List<String> possiblePhrases;
                    if (similarWord.equals(firstWord)) { // is same word
                        possiblePhrases = getPossiblePhrases(remainingPhrase, similarWords, correctionsLeft, false);
                    } else { // is correction
                        possiblePhrases = getPossiblePhrases(remainingPhrase, similarWords, correctionsLeft - 1, true);
                    }
                    
                    if (firstWord.equals("mice")){
                        System.out.println("mice");
                                }
 
                    /*if (possiblePhrases.size()>0) {
                        System.out.println(possiblePhrases.size());
                    }*/
                    
                    return possiblePhrases.stream()
                    .map(remPhrase -> similarWord + " " + String.join(" ", remPhrase));
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns list of words which have a Damerau-Levenshtein distance of at
     * most 1 from inputWord.
     */
    private Set<String> getSimilarWords(String inputWord) {
        return this.cr.getVocabulary().stream()
                .filter(word -> getDLDistance(inputWord, word) <= 1)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the Damerau-Levenshtein distance between strings a and b. Makes
     * use of the dynamic programming algorithm.
     */
    private int getDLDistance(String a, String b) {
        if (a.length() == 0) {
            return b.length();
        }
        if (b.length() == 0) {
            return a.length();
        }

        // initialize matrix
        int[][] m = new int[b.length() + 1][a.length() + 1];

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

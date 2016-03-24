import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SpellCorrector {

    final private CorpusReader cr;
    final private ConfusionMatrixReader cmr;

    public SpellCorrector(CorpusReader cr, ConfusionMatrixReader cmr) {
        this.cr = cr;
        this.cmr = cmr;
    }

    final private double LAMBDA = 1; // importance confusion matrix
    private final double NO_ERROR = 0.90; // probability no mistake in word

    public String correctPhrase(String phrase) {
        if (phrase == null || phrase.length() == 0) {
            throw new IllegalArgumentException("Phrase must be non-empty.");
        }

        List<String> words = Arrays.asList(phrase.split(" "));

        // To prevent duplicates
        Set<String> wordsAsSet = new HashSet(words);
        Map<String, Set<String>> similarWordsPerWord = wordsAsSet.stream()
                .collect(Collectors.toMap(word -> word, word -> getSimilarWords(word)));

        List<String> possiblePhrases = getPossiblePhrases(words, similarWordsPerWord, 2, false);

        double bestProbability = Double.NEGATIVE_INFINITY;
        String bestPhrase = "";
        for (String possiblePhrase : possiblePhrases) {
            double probability = calculateProbabilityCorrectedPhrase(phrase, possiblePhrase);
            if (probability > bestProbability) {
                bestProbability = probability;
                bestPhrase = possiblePhrase;
            }
        }

        return bestPhrase.trim();
    }

    private double calculateProbabilityCorrectedPhrase(String originalPhrase, String correctedPhrase) {
        String[] originalWords = originalPhrase.split(" ");
        String[] correctedWords = correctedPhrase.split(" ");

        double probability = 0;
        for (int i = 0; i < correctedWords.length; i++) {
            String originalWord = originalWords[i];
            String correctedWord = correctedWords[i];

            double prevValue = 1.0;
            if (i > 0) {
                prevValue = cr.getProbabiltyGivenPrev(correctedWord, correctedWords[i - 1]);
            }
            double nextValue = 1.0;
            if (i < correctedWords.length - 1) {
                nextValue = cr.getProbabilityGivenNext(correctedWord, correctedWords[i + 1]);
            }

            double correctedWordProbability = cr.getProbability(correctedWord);

            double noisyChannelValue;
            if (correctedWord.equals(originalWord)) {
                noisyChannelValue = NO_ERROR;
            } else {
                double correctionProbability = cmr.getProbabilityCorrection(originalWord, correctedWord);
                noisyChannelValue = correctedWordProbability * Math.pow(correctionProbability, LAMBDA);
            }

            double chance = noisyChannelValue * prevValue * nextValue;

            probability += Math.log(chance);
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
        List<String> remainingPhrase = phrase.subList(1, phrase.size());
        return similarWords.get(firstWord).stream()
                .filter(similarWord -> (similarWord.equals(firstWord) || (correctionsLeft > 0 && !prevWasCorrection)))
                .flatMap(similarWord -> {    
                    List<String> possiblePhrases;
                    if (similarWord.equals(firstWord)) { // is same word
                        possiblePhrases = getPossiblePhrases(remainingPhrase, similarWords, correctionsLeft, false);
                    } else { // is correction
                        possiblePhrases = getPossiblePhrases(remainingPhrase, similarWords, correctionsLeft - 1, true);
                    }

                    return possiblePhrases.stream()
                    .map(possiblePhrase -> similarWord + " " + String.join(" ", possiblePhrase));
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

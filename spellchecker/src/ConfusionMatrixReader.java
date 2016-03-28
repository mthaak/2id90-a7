
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.lang.Integer.max;

public class ConfusionMatrixReader {

    final static String DATAFILE_LOC = "confusion_matrix.txt";
    final private HashMap<String, Integer> confusionMatrix = new HashMap<>();
    final private HashMap<String, Integer> countMatrix = new HashMap<>();

    final static int totalConfusionMatrix = 29320; // sum confusion matrix

    public ConfusionMatrixReader() {
        try {
            readConfusionMatrix();
        } catch (Exception ex) {
            Logger.getLogger(ConfusionMatrixReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void readConfusionMatrix()
            throws FileNotFoundException, IOException {
        FileInputStream fis;
        fis = new FileInputStream(DATAFILE_LOC);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        while (in.ready()) {
            String line = in.readLine();
            int space = line.lastIndexOf(' ');
            String keys = line.substring(0, space);
            try {
                int count = Integer.parseInt(line.substring(space + 1));
                confusionMatrix.put(keys, count);

                String key = keys.substring(0, keys.indexOf('|'));
                Integer value = countMatrix.get(key);
                if (value == null) {
                    value = 0;
                }
                countMatrix.put(key, value + count);
            } catch (NumberFormatException e) {
                System.err.println("problems with string <" + line + ">");
            }
        }
    }

    /**
     * Returns the count for the pair <error>|<correct> in the confusion matrix,
     * e.g. "c|ct" is 36
     *
     * @param error
     * @param correct
     * @return
     */
    public int getConfusionCount(String error, String correct) {
        Integer count = confusionMatrix.get(error + "|" + correct);
        return count == null ? 0 : count;
    }

    /**
     * Compares two words to find the correction in the form <error>|<correct>,
     * e.g. "ng|n".
     */
    public String getCorrection(String originalWord, String correctedWord) {
        // Space is seen as character in the confusion matrix
        originalWord = " " + originalWord;
        correctedWord = " " + correctedWord;

        // Iterate until ends of both words are reached
        int endIndex = max(originalWord.length(), correctedWord.length());
        for (int i = 1; i < endIndex; i++) {
            if ((i == endIndex - 1 && originalWord.length() != correctedWord.length())
                    || originalWord.charAt(i) != correctedWord.charAt(i)) { // there is a difference

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

        return ""; // doesn't matter since only different words are compared
    }

    /**
     * Returns the probability that the intended word for originalWord is
     * correctedWord using a confusion matrix.
     */
    public double getProbabilityCorrection(String originalWord, String correctedWord) {
        // Get correction, e.g. "ng|g"
        String correction = getCorrection(originalWord, correctedWord);
        // Get count from confusion matrix
        Integer correctionCount = confusionMatrix.get(correction);

        // Essentially Add-One smoothing to prevent zero-probablity problem
        if (correctionCount == null) {
            correctionCount = 1;
        }

        // Return probability
        return (double) correctionCount / totalConfusionMatrix;
    }
}

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CorpusReader 
{
    final static String CNTFILE_LOC = "samplecnt.txt";
    final static String VOCFILE_LOC = "samplevoc.txt";
    final static String CONFUSIONFILE_LOC = "confusion_matrix.txt";
    
    private HashMap<String,Integer> ngrams;
    private HashMap<String,Integer> confusions;
    private Set<String> vocabulary;
        
    public CorpusReader() throws IOException
    {  
        readNGrams();
        readVocabulary();
        readConfusionMatrix();
    }
    
    /**
     * Returns the n-gram count of <NGram> in the file
     * 
     * 
     * @param nGram : space-separated list of words, e.g. "adopted by him"
     * @return 0 if <NGram> cannot be found, 
     * otherwise count of <NGram> in file
     */
     public int getNGramCount(String nGram) throws  NumberFormatException
    {
        if(nGram == null || nGram.length() == 0)
        {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }
        Integer value = ngrams.get(nGram);
        return value==null?0:value;
    }
    
    private void readNGrams() throws 
            FileNotFoundException, IOException, NumberFormatException
    {
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
        
        while(in.ready())
        {
            String line = in.readLine();
            vocabulary.add(line);
        }
    }
    
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
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("NumberformatError: " + s1);
            }
        }
    }
    
    public Set<String> getVocabulary(){
        return this.vocabulary;
    }
    
    /**
     * Returns the size of the number of unique words in the dataset
     * 
     * @return the size of the number of unique words in the dataset
     */
    public int getVocabularySize() 
    {
        return vocabulary.size();
    }
    
    /**
     * Returns the subset of words in set that are in the vocabulary
     * 
     * @param set
     * @return 
     */
    public HashSet<String> inVocabulary(Set<String> set) 
    {
        HashSet<String> h = new HashSet<>(set);
        h.retainAll(vocabulary);
        return h;
    }
    
    public boolean inVocabulary(String word) 
    {
       return vocabulary.contains(word);
    }    
    
    public double getSmoothedCount(String word1, String word2)
    {
        if(word1 == null || word2 == null)
        {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }
        
        double countTwoWords;
        double countSecondWord = 0.0;
        if (word1.equals("<s>")) {
            countTwoWords = getNGramCount(word2);
        } else if (word2.equals("</s>")) {
            countTwoWords = getNGramCount(word1);
            countSecondWord = 0.0;
        } else {
            countTwoWords = getNGramCount(word1 + " " + word2);
            countSecondWord = getNGramCount(word2);
        }
        
        double V = getVocabularySize(); // TODO how to get V?
        double smoothedCount = (countTwoWords + 1) / (countSecondWord + V);
        
        return smoothedCount;        
    }
    
    public double getCountTest(String NGram) {
        if(NGram == null || NGram.length() == 0)
        {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }
        
        double count = getNGramCount(NGram);
        double V = getVocabularySize();
        
        return count / V;
    }
    
    
    // altered by at most
    // 1 insertion, deletion, transposition or substitution
    
    // transposition or substitution only if same length
    // insertion / deletion only if not same length
    
    public double getConfusionValue(String candidateX, String wordW) {
        
        candidateX = " " + candidateX;
        wordW = " " + wordW;
        
        //System.out.println(candidateX);
        //System.out.println(wordW);
        
        double value = -100;
        
        if (wordW.length() < candidateX.length()) {
            wordW = wordW + " ";
        } else if (candidateX.length() < wordW.length()) {
            candidateX = candidateX + " ";
        }
        
        for (int i = 0; i < candidateX.length(); i++) {
            if (candidateX.charAt(i) != wordW.charAt(i)) {
                
                String badPart;
                String goodPart;
                
                // insertion / deletion
                String lastX = candidateX.charAt(candidateX.length() - 1) + "";
                String lastW = wordW.charAt(wordW.length() - 1) + "";
                if (lastX.equals(" ") || lastW.equals(" ")) {
                    
                    if (candidateX.trim().length() > wordW.trim().length()) {
                        // deletion
                        badPart = candidateX.charAt(i-1) + "" + candidateX.charAt(i);
                        goodPart = candidateX.charAt(i-1) + "";
                        
                    } else {
                        // insertion
                        //System.out.println("insertion");
                        badPart = candidateX.charAt(i-1) + "";
                        goodPart = candidateX.charAt(i-1) + "" + wordW.charAt(i);
                    }
                } else {
                    // transposition or substitution
                    
                    if (i == candidateX.length() - 1) {
                        // substitution
                        
                        badPart = candidateX.charAt(i) + "";
                        goodPart = wordW.charAt(i) + "";
                        
                    } else if (candidateX.charAt(i+1) == wordW.charAt(i) &&
                            candidateX.charAt(i) == wordW.charAt(i+1)){
                        // transposition
                        
                        badPart = candidateX.charAt(i) + candidateX.charAt(i+1) + "";
                        goodPart = wordW.charAt(i) + wordW.charAt(i+1) + "";
                        
                    } else {
                        // substitution
                        
                        badPart = candidateX.charAt(i) + "";
                        goodPart = wordW.charAt(i) + "";
                    }
                    
                }
                
                String concat = badPart + "|" + goodPart;
                //System.out.println("concat: '" + concat + "'");
                
                Integer intValue = confusions.get(concat);
                if (intValue == null) {
                    intValue = 0;
                }
                
                value = intValue;
                return value;
            }
        }
        
        
        System.out.println("You do not want to arrive here, value: " + value + candidateX);
        return value;
    }
}

import java.io.IOException;
import java.util.Scanner;


public class SpellChecker {

    private final static boolean DEBUG = false;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        boolean inPeach = true; // set this to true if you submit to peach!!!
        
        try {
            CorpusReader cr = new CorpusReader();
            ConfusionMatrixReader cmr = new ConfusionMatrixReader();
            SpellCorrector sc = new SpellCorrector(cr, cmr);
            
            if (inPeach) {
                peachTest(sc);
            } else {
                nonPeachTest(sc);
                peachTraining(sc);
            }
        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }
    }
    
    static void nonPeachTest(SpellCorrector sc) throws IOException { 
        System.out.println("nonPeachTest()");
        
        String[] sentences = {
            "this assay allowed us to measure a wide variety of conditions" ,
            "this assay allowed us to measure a wide variety of conitions" ,
            "this assay allowed us to meassure a wide variety of conditions" ,
            "this assay allowed us to measure a wide vareity of conditions" ,
            "at the home locations there were traces of water" ,
            "at the hme locations there were traces of water" ,
            "at the hoome locations there were traces of water" ,
            "at the home locasions there were traces of water" ,
            "the development of diabetes is present in mice that carry a transgen" ,
            "the development of diabetes is present in moce that carry a transgen" ,
            "the development of idabetes is present in mice that carry a transgen"
        };

        String[] correctSentences = {
            "Answer: this assay allowed us to measure a wide variety of conditions",
            "Answer: this assay allowed us to measure a wide variety of conditions",
            "Answer: this assay allowed us to measure a wide variety of conditions",
            "Answer: this assay allowed us to measure a wide variety of conditions",
            "Answer: at the home locations there were traces of water",
            "Answer: at the home locations there were traces of water",
            "Answer: at the home locations there were traces of water",
            "Answer: at the home locations there were traces of water",
            "Answer: the development of diabetes is present in mice that carry a transgene",
            "Answer: the development of diabetes is present in mice that carry a transgene",
            "Answer: the development of diabetes is present in mice that carry a transgene"
        };

        int correctPhrases = 0;
        int totalPhrases = 0;
        for (int i = 0; i < sentences.length; i ++) {
            String sentence = sentences[i];
            String result = "Answer: " + sc.correctPhrase(sentence);
            String correctResult = correctSentences[i];

            if (result.equals(correctResult)) {
                correctPhrases ++;
            } else  {
                if (DEBUG) {
                    System.out.println("Test " + i);
                    System.out.println("Sentence: " + sentence);
                    System.out.println(result);
                    System.out.println(correctResult + " (correct)");
                    System.out.println("");
                }
            }
            totalPhrases ++;
        }

        System.out.println("Score: " + correctPhrases + "/" + totalPhrases);
    }
    
    static void peachTraining(SpellCorrector sc) throws IOException { 
        System.out.println("peachTraining()");
        
        String[] sentences = {
            "this assay allowed us to measure a wide variety of conitions",
            "at the hme locations there were traces of water",
            "this assay allowed us to meassure a wide variety of conditions",
            "this assay allowed us to measure a wide vareity of conditions",
            "at the hoome locations there were traces of water",
            "at the home locasions there were traces of water",
            "the development of diabetes is present in moce that carry a transgen",
            "the development of idabetes is present in mice that carry a transgen",
            "the development of diabetes us present in mice that harry a transgene",
            "boxing glowes shield the knockles not the head",
            "boxing loves shield the knuckles nots the head",
            "boing gloves shield the knuckles nut the head",
            "she still refers to me has a friend but i fel i am treated quite badly",
            "she still refers to me as a friendd but i feel i am traeted quite badly",
            "she still refers too me as a friend but i feel i am treated quite batly",
            "she still refers to me as a fiend but i feel i am treated quite badly",
            "a repsonse may be any measurable biological parameter that is correlated witth the toxicant",
            "a response may be any measurable biological prameter that is corelated with the toxicant",
            "a respunse may be any measurable biologecal parameter that is correlated with the toxicant",
            "a responses may be any measurable biological parametre that is correlated with the toxicant",
            "esentially there has been no change in japan",
            "essentially there has been no change in japan",
            "essentially here has bien no change in japan",
            "this advise is taking into consideration the fact that the govenrment bans political parties",
            "this advices is taking into consideration the fact that the government bans political parties",
            "this addvice is taking into consideration the fact that the goverment bans political parties",
            "ancient china was one of the longst lasting societies iin the history of the world",
            "ancient china was one of the longest lasting sosieties in the history of the world",
            "anicent china was one of the longest lasting societties in the history of the world",
            "ancient china wqs one of the longest lasting societies in the histori of the world",
            "laying in the national footbal league was my dream",
            "playing in the national football laegue was my draem",
            "playing in the national fotball league was my dream"
        };

        String[] correctSentences = {
            "Answer: this assay allowed us to measure a wide variety of conditions",
            "Answer: at the home locations there were traces of water",
            "Answer: this assay allowed us to measure a wide variety of conditions",
            "Answer: this assay allowed us to measure a wide variety of conditions",
            "Answer: at the home locations there were traces of water",
            "Answer: at the home locations there were traces of water",
            "Answer: the development of diabetes is present in mice that carry a transgene",
            "Answer: the development of diabetes is present in mice that carry a transgene",
            "Answer: the development of diabetes is present in mice that carry a transgene",
            "Answer: boxing gloves shield the knuckles not the head",
            "Answer: boxing gloves shield the knuckles not the head",
            "Answer: boxing gloves shield the knuckles not the head",
            "Answer: she still refers to me as a friend but i feel i am treated quite badly",
            "Answer: she still refers to me as a friend but i feel i am treated quite badly",
            "Answer: she still refers to me as a friend but i feel i am treated quite badly",
            "Answer: she still refers to me as a friend but i feel i am treated quite badly",
            "Answer: a response may be any measurable biological parameter that is correlated with the toxicant",
            "Answer: a response may be any measurable biological parameter that is correlated with the toxicant",
            "Answer: a response may be any measurable biological parameter that is correlated with the toxicant",
            "Answer: a response may be any measurable biological parameter that is correlated with the toxicant",
            "Answer: essentially there has been no change in japan",
            "Answer: essentially there has been no change in japan",
            "Answer: essentially there has been no change in japan",
            "Answer: this advice is taking into consideration the fact that the government bans political parties",
            "Answer: this advice is taking into consideration the fact that the government bans political parties",
            "Answer: this advice is taking into consideration the fact that the government bans political parties",
            "Answer: ancient china was one of the longest lasting societies in the history of the world",
            "Answer: ancient china was one of the longest lasting societies in the history of the world",
            "Answer: ancient china was one of the longest lasting societies in the history of the world",
            "Answer: ancient china was one of the longest lasting societies in the history of the world",
            "Answer: playing in the national football league was my dream",
            "Answer: playing in the national football league was my dream",
            "Answer: playing in the national football league was my dream"
        };

        int correctPhrases = 0;
        int totalPhrases = 0;
        for (int i = 0; i < sentences.length; i ++) {
            String sentence = sentences[i];
            String result = "Answer: " + sc.correctPhrase(sentence);
            String correctResult = correctSentences[i];

            if (result.equals(correctResult)) {
                correctPhrases ++;
            } else  {
                if (DEBUG) {
                    System.out.println("Test " + i);
                    System.out.println("Sentence: " + sentence);
                    System.out.println(result);
                    System.out.println(correctResult + " (correct)");
                    System.out.println("");
                }
            }
            totalPhrases ++;
        }

        System.out.println("Score: " + correctPhrases + "/" + totalPhrases);
    }
    
    static void peachTest(SpellCorrector sc) throws IOException {
        Scanner input = new Scanner(System.in);

        String sentence = input.nextLine();
        System.out.println("Answer: " + sc.correctPhrase(sentence));  
    } 
}

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Class to train on negative and positive reviews and classify the testing
 * reviews using Naive Bayes Classifier and sentiWordNet 
 *
 * @author Harsh Patil
 * @author Samita Pradhan
 * @author Shraddha Sarkhot
 * @author Shefali Shreedhar
 */
public class SentimentAnalysis {
	String[] trainingDocs;
	int[] trainingLabels;
	static String[] trainDocs;
	static String[] testPosFolder;
	static String[] testNegFolder;

	int numClasses;
	int[] classCounts; // number of docs per class
	String[] classStrings; // concatenated string for a given class
	int[] classTokenCounts; // total number of tokens per class
	HashMap<String, Double>[] condProb;
	HashSet<String> vocabulary; // entire vocabuary
	static ArrayList stopWordslist;
	public WordScore sn;

	/**
	 * Build a Naive Bayes classifier using a training document set
	 * 
	 * @param trainDataFolder
	 *            the training document folder
	 * @throws IOException
	 */
	public SentimentAnalysis(String trainDataFolder) throws IOException {
		sn = new WordScore();
		Scanner stopword = new Scanner(new File("stopwords.txt"));
		stopWordslist = new ArrayList<>();
		while (stopword.hasNextLine()) {
			String line = stopword.nextLine();
			stopWordslist.add(line);
		}

		testPosFolder = new String[100];
		testNegFolder = new String[100];

		trainDocs = new String[1996];
		preprocess("train");
		trainingDocs = trainDocs;

		trainingLabels = new int[1996];
		for (int i = 0; i < 1996; i++) {
			if (i < 998) {
				trainingLabels[i] = 0;
			} else {
				trainingLabels[i] = 1;
			}
		}
		numClasses = 2;
		classCounts = new int[numClasses];
		classStrings = new String[numClasses];
		classTokenCounts = new int[numClasses];
		condProb = new HashMap[numClasses];
		vocabulary = new HashSet<String>();
		for (int i = 0; i < numClasses; i++) {
			classStrings[i] = "";
			condProb[i] = new HashMap<String, Double>();
		}
		for (int i = 0; i < trainingLabels.length; i++) {
			classCounts[trainingLabels[i]]++;
			classStrings[trainingLabels[i]] += (trainingDocs[i] + " ");
		}
		for (int i = 0; i < numClasses; i++) {
			String[] tokens = classStrings[i].split(" ");
			classTokenCounts[i] = tokens.length;
			// collecting the counts
			for (String token : tokens) {
				vocabulary.add(token);
				if (condProb[i].containsKey(token)) {
					double count = condProb[i].get(token);
					condProb[i].put(token, count + 1);
				} else
					condProb[i].put(token, 1.0);
			}
		}
		// computing the class conditional probability
		for (int i = 0; i < numClasses; i++) {
			Iterator<Map.Entry<String, Double>> iterator = condProb[i].entrySet().iterator();
			int vSize = vocabulary.size();
			while (iterator.hasNext()) {
				Map.Entry<String, Double> entry = iterator.next();
				String token = entry.getKey();
				Double count = entry.getValue();
				
				// get the sentiment score of token
				double score = sn.extract(token);

				count = (count + 1 * (score + 1)) / (classTokenCounts[i] + vSize);
				condProb[i].put(token, count);
			}
		}
	}

	/**
	 * Classify a document using the conditional probability and SentiWord score 
	 * of each token. 
	 * 
	 * @param doc
	 * @return
	 */
	public int classify(String doc) {
		int label = 0;
		int vSize = vocabulary.size();
		double[] score = new double[numClasses];
		for (int i = 0; i < score.length; i++) {
			score[i] = Math.log(classCounts[i] * 1.0 / trainingDocs.length);
		}
		String[] tokens = doc.split(" ");

		for (int i = 0; i < numClasses; i++) {
			for (String token : tokens) {
				if (!stopWordslist.contains(token)) {
					// get the sentiment score of token
					double scorew = sn.extract(token);
					if (condProb[i].containsKey(token))
						score[i] += Math.log(condProb[i].get(token));
					else
						score[i] += Math.log((1.0 * (scorew + 1)) / (classTokenCounts[i] + vSize));
				}
			}
		}
		double maxScore = score[0];
		for (int i = 0; i < score.length; i++) {
			if (score[i] > maxScore)
				label = i;
		}

		return label;
	}

	/**
	 * Load the training documents from positive and negative train folders
	 * 
	 * @param trainDataFolder
	 * @throws IOException
	 */
	public static void preprocess(String trainDataFolder) throws IOException {
		File trainFolderPos = new File(trainDataFolder + "\\positive");
		File trainFolderNeg = new File(trainDataFolder + "\\negative");
		String[] trainPos = new String[998];
		String[] trainNeg = new String[998];

		String file;
		int a = 0;
		for (final File fileEntry : trainFolderPos.listFiles()) {
			file = readFile("train\\positive\\" + fileEntry.getName());
			trainPos[a++] = file;
		}
		a = 0;
		for (final File fileEntry : trainFolderNeg.listFiles()) {
			file = readFile("train\\negative\\" + fileEntry.getName());
			trainNeg[a++] = file;
		}
		a = 0;
		System.arraycopy(trainPos, 0, trainDocs, 0, 998);
		System.arraycopy(trainNeg, 0, trainDocs, 998, 998);
	}

	/**
	 * Classify a set of testing documents and report the accuracy
	 * 
	 * @param testDataFolder
	 *            fold that contains the testing documents
	 * @return classification accuracy
	 */
	public static double classifyAll(String testDataFolder) {
		SentimentAnalysis nb;
		int positive_count0 = 0;
		int positive_count1 = 0;

		int negative_count0 = 0;
		int negative_count1 = 0;

		double accuracy = 0.0;
		try {
			nb = new SentimentAnalysis("train");
			preProcessTest("pos");
			preProcessTest("neg");
			for (int i = 0; i < 100; i++) {
				String testDoc = testPosFolder[i];
				if (nb.classify(testDoc) == 0) {
					positive_count0++;
				} else {
					positive_count1++;
				}
			}
			for (int m = 0; m < 100; m++) {
				String testDoc = testNegFolder[m];
				if (nb.classify(testDoc) == 0) {
					negative_count0++;
				} else {
					negative_count1++;
				}
			}
			System.out.println(positive_count0 + "," + positive_count1 + "," + negative_count0 + "," + negative_count1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int correctlyClassified = positive_count0 + negative_count1;
		System.out.println("Correctly classified " + correctlyClassified + " out of 200");
		accuracy = (Double) (correctlyClassified / 200.0);
		System.out.println("Accuracy: " + accuracy*100+"%");
		return accuracy;
	}

	/**
	 * extract the pos and neg test folders in the respective arrays
	 * 
	 * @param folderName
	 * @throws IOException
	 */
	private static void preProcessTest(String folderName) throws IOException {
		// TODO Auto-generated method stub
		File testFolder = new File("test\\" + folderName);

		String file;
		int position = 0;
		if (folderName.equals("pos")) {
			for (final File fileEntry : testFolder.listFiles()) {
				file = readFile("test\\pos\\" + fileEntry.getName());
				testPosFolder[position++] = file;
			}
		} else {
			position = 0;
			for (final File fileEntry : testFolder.listFiles()) {
				file = readFile("test\\neg\\" + fileEntry.getName());
				testNegFolder[position++] = file;
			}
		}
	}

	/**
	 * Reads a file and returns the string inside it
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String readFile(String fileName) throws IOException {
		Scanner src = new Scanner(new FileReader(fileName));
		try {
			StringBuilder sb = new StringBuilder();
			while (src.hasNext()) {
				String line = src.nextLine();
				line = line.replaceAll("[!?,]", " ");

				String[] cleanLine = line.split("[^a-zA-Z']+");
				for (String s : cleanLine) {
					if ((!stopWordslist.contains(s)) && s.length() > 2) {
						sb.append(s + " ");
					}
				}
				sb.append("\n");
			}
			return sb.toString();
		} finally {
			src.close();
		}
	}

	public static void main(String[] args) throws IOException {
		classifyAll("test");
	}
}

package test.rit.harsh.kpt;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
public class SentimentAnalysis{
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
	private Context context;

	/**
	 * Build a Naive Bayes classifier using a training document set
	 * 
	 * @param trainDataFolder
	 *            the training document folder
	 * @throws IOException
	 */
	public SentimentAnalysis(String trainDataFolder, Context context) throws IOException {
		this.context = context;
		AssetManager am = context.getAssets();
		InputStream stopWord = am.open("stopwords.txt");

		sn = new WordScore(context);
		BufferedReader br = new BufferedReader(new InputStreamReader(stopWord));
		stopWordslist = new ArrayList<>();
		String line="";
		while ((line = br.readLine())!=null) {
			System.out.println(line);
			stopWordslist.add(line);
		}
		br.close();
		testPosFolder = new String[200];
		testNegFolder = new String[200];

		trainDocs = new String[4000];
		try {
			preprocess("train",context);
		} catch (IOException e) {
			e.printStackTrace();
		}
		trainingDocs = trainDocs;
		System.out.println("here");
		trainingLabels = new int[4000];
		for (int i = 0; i < 4000; i++) {
			if (i < 2000) {
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
	public double[] classify(String doc) {
		int label = 0;
		System.out.println(doc);
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
		return score;
	}

	/**
	 * Load the training documents from positive and negative train folders
	 * 
	 * @param trainDataFolder
	 * @throws IOException
	 */
	public static void preprocess(String trainDataFolder, Context appcontext) throws IOException {
		AssetManager am = appcontext.getAssets();

		String[] posfiles = am.list(trainDataFolder +File.separator+ "positive");
		String[] negfiles = am.list(trainDataFolder +File.separator+ "negative");

		String[] trainPos = new String[2000];
		String[] trainNeg = new String[2000];

		String file;
		int a = 0;
		for (String s: posfiles) {
			InputStream fileName = am.open(trainDataFolder +File.separator+ "positive"+File.separator+s);
			file = readFile(fileName);
			trainPos[a++] = file;
		}
		a = 0;
		for (String s: negfiles) {
			InputStream fileName = am.open(trainDataFolder +File.separator+ "negative"+File.separator+s);
			file = readFile(fileName);
			trainNeg[a++] = file;
		}
		a = 0;
		System.arraycopy(trainPos, 0, trainDocs, 0, 2000);
		System.arraycopy(trainNeg, 0, trainDocs, 2000, 2000);
	}

	/**
	 * extract the pos and neg test folders in the respective arrays
	 * 
	 * @param folderName
	 * @throws IOException
	 */
	private static void preProcessTest(String folderName, Context appcontext) throws IOException {
		// TODO Auto-generated method stub
		AssetManager am = appcontext.getAssets();

		String[] files = appcontext.getAssets().list("test\\" + folderName);

		String file;
		int position = 0;
		if (folderName.equals("pos")) {
			for (String fileEntry : files) {
				InputStream fileName = am.open(fileEntry);
				file = readFile(fileName);
				testPosFolder[position++] = file;
			}
		} else {
			position = 0;
			for (String fileEntry : files) {
				InputStream fileName = am.open(fileEntry);
				file = readFile(fileName);
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
	public static String readFile(InputStream fileName) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(fileName));
		String line="";
		StringBuilder sb = new StringBuilder();

		while ((line = br.readLine())!=null) {
			line = line.replaceAll("[!?,]", " ");

			String[] cleanLine = line.split("[^a-zA-Z']+");
			for (String s : cleanLine) {
				if ((!stopWordslist.contains(s)) && s.length() > 2) {
					sb.append(s + " ");
				}
			}
			sb.append("\n") ;
		}
		return sb.toString();
	}
}

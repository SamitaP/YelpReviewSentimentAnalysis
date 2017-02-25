import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Helper class to extract reviews from the Review.csv and Split it into
 * positive and negative folder for further sentiment analysis.
 *
 * @author Harsh Patil
 * @author Samita Pradhan
 * @author Shraddha Sarkhot
 * @author Shefali Shreedhar
 */
public class SentimentAnalysisHelper {
	private void preProcess(String fileName) throws IOException {
		// TODO Auto-generated method stub
		Scanner src = new Scanner(new File(fileName));
		int n = 7000;
		// process n reviews
		for (int i = 0; i < n; i++) {
			PrintWriter pr;
			String line = src.nextLine();
			String[] token = line.split(",");
			String review = token[3];
			int rating = Integer.parseInt(token[4]);

			// if rating greater than or equal to 3 put it into positive folder
			if (rating >= 3) {
				FileWriter writer = new FileWriter(new File("train\\positive\\positve" + i + ".txt"), true);
				pr = new PrintWriter(writer);
				pr.write(review);
				pr.close();
			}
			// else in negative
			else {
				FileWriter writer = new FileWriter(new File("train\\negative\\negative" + i + ".txt"), true);
				pr = new PrintWriter(writer);
				pr.write(review);
				pr.close();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		SentimentAnalysisHelper SA = new SentimentAnalysisHelper();
		SA.preProcess("Reviews.csv");
	}
}

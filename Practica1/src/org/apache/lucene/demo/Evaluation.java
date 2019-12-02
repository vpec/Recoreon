package org.apache.lucene.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.text.html.HTMLDocument.Iterator;

public class Evaluation {
	
	private static int MAX_RESULTS = 45;
	
	private static HashMap<String, HashMap<String, String>> qrelsMap = new HashMap<>();
	
	private static HashMap<String, List<String>> resultsMap = new HashMap<>();
	
	private static HashMap<Float, Float> recall_precisionMap = new HashMap<>();
	
	private static TreeMap<Float, List<Float>> fixedRecallList = new TreeMap<>();
	
	private static List<Float> average_precisionList = new ArrayList<>();
	
	private static List<Float> precisionList = new ArrayList<>();
	private static List<Float> recallList = new ArrayList<>();
	private static List<Float> f1List = new ArrayList<>();
	private static List<Float> precAt10List = new ArrayList<>();
	private static List<List<Float>> precisionInterpolatedList = new ArrayList<>();
	
	
	// Evaluation metrics
	private static float tp, fp, fn, precision, recall, f1balanced;
	private static List<Integer> relDocumentsList;

	public static void main(String[] args) {
		String usage = "java org.apache.lucene.demo.IndexFiles"
				+ " [-qrels QRELS_FILE_NAME] [-results RESULTS_FILE_NAME] [-output OUTPUT_FILE_NAME]\n\n"
				+ "This uses qrels for evaluating the results, writting the output to the output file";
		String qrelsPath = null, resultsPath = null, outputPath = null;
		for (int i = 0; i < args.length; i++) {
			if ("-qrels".equals(args[i])) {
				qrelsPath = args[i + 1];
				i++;
			} else if ("-results".equals(args[i])) {
				resultsPath = args[i + 1];
				i++;
			} else if ("-output".equals(args[i])) {
				outputPath = args[i + 1];
			}
		}

		if (qrelsPath == null || resultsPath == null || outputPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}
		
		// Load data from qrels and results files
		loadInputData(qrelsPath, resultsPath);

		// Writes evaluation metrics to output file
		evaluate(outputPath);

	}
	
	private static void loadInputData(String qrelsPath, String resultsPath) {
		File qrelsFile = new File(qrelsPath);
		File resultsFile = new File(resultsPath);
		try {
			BufferedReader qrelsBr = new BufferedReader(new FileReader(qrelsFile));
			BufferedReader resultsBr = new BufferedReader(new FileReader(resultsFile));

			String line;
			// Read qrels file
			while ((line = qrelsBr.readLine()) != null) {
				String[] elements = line.split("\\s+");
				if (!qrelsMap.containsKey(elements[0])) {
					qrelsMap.put(elements[0], new HashMap<>());
				}
				qrelsMap.get(elements[0]).put(elements[1], elements[2]);
			}
			
			// Read results file
			int i = 0;
			while (((line = resultsBr.readLine()) != null) && i < MAX_RESULTS) {
				String[] elements = line.split("\\s+");
				if (!resultsMap.containsKey(elements[0])) {
					resultsMap.put(elements[0], new ArrayList<>());
				}
				resultsMap.get(elements[0]).add(elements[1]);
			}

			qrelsBr.close();
			resultsBr.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void evaluate(String outputPath) {
		File outputFile = new File(outputPath);
		FileWriter outputWriter;
		try {
			outputWriter = new FileWriter(outputFile);
			PrintWriter pw = new PrintWriter(outputWriter);
			
			// Initialize fixedRecallList
			for(int i = 0; i <= 10; i++) {
				fixedRecallList.put(((float)i)/10.f, new ArrayList<>());
			}
			
			// What happens if not all information needs have been judged or appear in results?
			for (Entry<String, HashMap<String, String>> entry : qrelsMap.entrySet()) {
				// Initialize basic metrics values
				tp = 0;
				fp = 0;
				fn = 0;
				precision = 0;
				recall = 0;
				f1balanced = 0;
				relDocumentsList =  new ArrayList<>();
				calculateBasicMetrics(entry.getKey());
				precision();
				recall();
				f1balanced();
			    pw.println("INFORMATION_NEED " + entry.getKey());
			    pw.println("precision " + String.valueOf(precision));
			    pw.println("recall " + String.valueOf(recall));
			    pw.println("F1 " + String.valueOf(f1balanced));
			    pw.println("prec@10 " + precAt10(entry.getKey()));
			    pw.println("average_precision " + average_precision(entry.getKey()));
			    pw.println("recall_precision");
			    recall_precision(entry.getKey(), pw);
			    pw.println("interpolated_recall_precision");
			    interpolated_recall_precision(entry.getKey(), pw);
			    pw.println();
			    
			}
			
			pw.println("TOTAL");
			pw.format("%s%.3f%s", "precision ", precisionList.stream().mapToDouble(val -> val).average().getAsDouble(), "\n");
		    pw.format("%s%.3f%s", "recall ", recallList.stream().mapToDouble(val -> val).average().getAsDouble(), "\n");
		    pw.format("%s%.3f%s", "F1 ", f1List.stream().mapToDouble(val -> val).average().getAsDouble(), "\n");
		    pw.format("%s%.3f%s", "prec@10 ", precAt10List.stream().mapToDouble(val -> val).average().getAsDouble(), "\n");
		    pw.format("%s%.3f%s", "MAP ", average_precisionList.stream().mapToDouble(val -> val).average().getAsDouble(), "\n");
		    pw.println("interpolated_recall_precision");
		    for (Entry<Float, List<Float>> entry : fixedRecallList.entrySet()) {
		    	pw.format("%.3f%s%.3f%s",entry.getKey(), " ",  entry.getValue().stream().mapToDouble(val -> val).average().getAsDouble(), "\n");
		    }

		    pw.println();
			
			outputWriter.close();
						
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void calculateBasicMetrics(String infNeed) {
		if (resultsMap.containsKey(infNeed)) {
			// If system returned any docs for infNeed
			HashMap<String, String> qrelsInnerMap = qrelsMap.get(infNeed);
			List<String> resultsList = resultsMap.get(infNeed);
			int docIndex = 1;
			for (String docId: resultsList) {
				if (qrelsInnerMap.containsKey(docId)) {
					if(qrelsInnerMap.get(docId).equals("1")) { // If doc is relevant
						tp++;
						relDocumentsList.add(docIndex);
					}
					else {
						fp++;
					}
				}
				docIndex++;
		    }
			
			for (Entry<String, String> entry : qrelsInnerMap.entrySet()) {
				if(entry.getValue().equals("1") && !resultsList.contains(entry.getKey())) {
					fn++;
				}				
				
			}
		}
		
		
	}
	
	private static void precision() {
		precision = tp / (tp + fp);
		precisionList.add(precision);
	}
	
	private static void recall() {
		recall = tp / (tp + fn);
		recallList.add(recall);
	}
	
	private static void f1balanced() {
		f1balanced = (2 * precision * recall) / (precision + recall);
		f1List.add(f1balanced);
	}
	
	private static String precAt10(String infNeed) {
		float tp10 = 0;
		if (resultsMap.containsKey(infNeed)) {
			// If system returned any docs for infNeed
			HashMap<String, String> qrelsInnerMap = qrelsMap.get(infNeed);
			List<String> resultsList = resultsMap.get(infNeed);
			for (int i = 0; i < resultsList.size() && i < 10; i++) {
				if (qrelsInnerMap.containsKey(resultsList.get(i))) {
					if(qrelsInnerMap.get(resultsList.get(i)).equals("1")) { // If doc is relevant
						tp10++;
					}
				}
		    }
		}
		precAt10List.add(tp10 / 10);
		return String.valueOf(tp10 / 10);
	}
	
	private static String average_precision(String infNeed) {
		float meanPrecision = 0;
		float numRelDocs = 0;
		if (resultsMap.containsKey(infNeed)) {
			// If system returned any docs for infNeed
			HashMap<String, String> qrelsInnerMap = qrelsMap.get(infNeed);
			List<String> resultsList = resultsMap.get(infNeed);
			for (int i = 0; i < resultsList.size(); i++) {
				if (qrelsInnerMap.containsKey(resultsList.get(i))) {
					if(qrelsInnerMap.get(resultsList.get(i)).equals("1")) { // If doc is relevant
						meanPrecision += precAtK(infNeed, i + 1);
						numRelDocs++;
					}
				}
		    }
		}
		average_precisionList.add(meanPrecision / numRelDocs);
		return String.valueOf(meanPrecision / numRelDocs);
		
	}
	
	private static float precAtK(String infNeed, int k) {
		float tpk = 0;
		if (resultsMap.containsKey(infNeed)) {
			// If system returned any docs for infNeed
			HashMap<String, String> qrelsInnerMap = qrelsMap.get(infNeed);
			List<String> resultsList = resultsMap.get(infNeed);
			for (int i = 0; i < k; i++) {
				if (qrelsInnerMap.containsKey(resultsList.get(i))) {
					if(qrelsInnerMap.get(resultsList.get(i)).equals("1")) { // If doc is relevant
						tpk++;
					}
				}
		    }
		}
		return tpk / k;
	}
	
	private static void recall_precision(String infNeed, PrintWriter pw) {
		float recallPerDocument = recall / relDocumentsList.size();
		float acumulatedRecall = 0;
		for(Integer docIndex : relDocumentsList) {
			acumulatedRecall += recallPerDocument;
			recall_precisionMap.put(acumulatedRecall, precAtK(infNeed, docIndex));
			pw.format("%.3f%s%.3f%s", acumulatedRecall, " ", precAtK(infNeed, docIndex), "\n");
			//pw.println(acumulatedRecall + " " + precAtK(infNeed, docIndex));
		}
	}
	
	
	private static void interpolated_recall_precision(String infNeed, PrintWriter pw) {
		Float maxPrecision;
		precisionInterpolatedList.add(new ArrayList<>());
		for (Entry<Float, List<Float>> entry : fixedRecallList.entrySet()) {
			maxPrecision = 0.0F;
			for (Entry<Float, Float> entryMap : recall_precisionMap.entrySet()) {
				if(entry.getKey() <= entryMap.getKey()) {
					if(entryMap.getValue() > maxPrecision) {
						maxPrecision = entryMap.getValue();
					}
				}
			}
			pw.format("%.3f%s%.3f%s", entry.getKey(), " ", maxPrecision, "\n");
			entry.getValue().add(maxPrecision);
			
		}
		
	}

}

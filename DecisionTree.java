import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class DecisionTree {
	static int flag = 1;
	static int leaftNumber = 0;

	static int totalNodes = 0;
	static int leafNodes = 0;
	static int rowTraining;
	static int rowValidation;
	static int rowTest;
	static double validationAccuracy = 0.0;
	static Object[][] rawData;
	static Object[][] validationData;
	static Object[][] testData;
	static Map<Integer, Object> leafLabel = new HashMap<Integer, Object>();
	static Object prunedDecisionTree;
	static int randomMatching = 0;
	static int sumOfLeafDepth = 0;

	public static void main(String[] args) throws Exception {
		String[] attrNames = new String[] { "XB", "XC", "XD", "XE", "XF", "XG", "XH", "XI", "XJ", "XK", "XL", "XM", "XN", "XO", "XP", "XQ", "XR",
				"XS", "XT", "XU" };

		 String NodesToPrune = args[0];
		 String csv = args[1];
		 String csv2 = args[2];
		 String csv3 = args[3];
		 String printDT = args[4];

//		String NodesToPrune = "1";
//		String printDT = "1";
//		String csv = "training_set.csv";
//
//		String csv2 = "validation_set.csv";
//		String csv3 = "test_set.csv";

		// Read the sample
		Map<Object, List<Sample>> classifiedSample = readSamples(csv, attrNames);
		// Only to get the 2-D validationData array
		Map<Object, List<Sample>> validationClassifiedSample = readValidationSamples(csv2, attrNames);

		Map<Object, List<Sample>> testClassifiedSample = readTestSamples(csv3, attrNames);

		// Construct the decision tree
		Object decisionTree = generateDecisionTree(classifiedSample, attrNames);

		// Object prunedDecisionTree = pruneDecisionTree(decisionTree,attrNames,
		// times);

		// get the info of the decision tree generated from training data

		infoOfDecisionTree(decisionTree, 0, null);

		System.out.println("*******All the information needed to generate the output for the report :  ********");

		System.out.println("Total leaf node  is : " + leafNodes);

		System.out.println("Total flag1 node  is : " + leaftNumber);

		System.out.println("Counter for total nodes in the tree is : " + totalNodes);

		System.out.println("Training Accuracy on the model is  : " + calculateAccuracy(rowTraining, decisionTree, rawData));

		validationAccuracy = calculateAccuracy(rowValidation, decisionTree, validationData);
		// System.out.println(validationAccuracy);
		System.out.println("Validation Accuracy on the model is  : " + validationAccuracy);

		System.out.println("Test Accuracy on the model is  : " + calculateAccuracy(rowTest, decisionTree, testData));

		int totalIteration = Integer.parseInt(NodesToPrune);

		for (int i = 0; i < totalIteration; i++) {
			Random random = new Random();
			int randomNode = random.nextInt(leafLabel.size() - 1) + 1;
			// System.out.println("Size of leafLabel : " + leafLabel.size());
			// System.out.println("Random number of prune node is " +
			// randomNode);

			prunedDecisionTree = pruneDecisionTree(randomNode, classifiedSample, attrNames);

			double prunedValidationAccuracy = calculateAccuracy(rowValidation, prunedDecisionTree, validationData);
			//System.out.println(validationAccuracy);
			System.out.println("Pruned Validation Accuracy on the model is  : " + prunedValidationAccuracy);
			
			// Always keep the decision tree when the accuracy decreases, and replace the decision tree when accuracy increases
			if (prunedValidationAccuracy > validationAccuracy)
				decisionTree = prunedDecisionTree;

		}
		System.out.println("Sum of all leaf node depth is : " + sumOfLeafDepth);

		System.out.println("****************************************************************************");
		System.out.println("The structure of the decision is below : \n ");

		int toPrint = Integer.parseInt(printDT);
		if (toPrint == 1) {
			outputDecisionTree(decisionTree, 0, null);
		}

	}

	static Object pruneDecisionTree(int randomNode, Map<Object, List<Sample>> categoryToSamples, String[] attrNames) {
		// Re-generate the decision tree with randomly prune the previous
		// decision tree to see if validation accuracy increases
		randomMatching++;
		// System.out.println("randomMatching out is : " + randomMatching);

		if (randomMatching == randomNode) {
			// System.out.println("This line matches random number : " +
			// randomMatching );
			return null;
		}

		if (attrNames.length == 0) {
			int max = 0;
			Object maxCategory = null;
			for (Entry<Object, List<Sample>> entry : categoryToSamples.entrySet()) {
				int cur = entry.getValue().size();
				if (cur > max) {
					max = cur;
					maxCategory = entry.getKey();
				}
			}

			return maxCategory;
		}

		if (categoryToSamples.size() == 1) {
			Object pureCategory = categoryToSamples.keySet().iterator().next();

			return pureCategory;
		}

		Object[] cba = chooseBestAttribute(categoryToSamples, attrNames);

		int index = (Integer) cba[0];
		Tree tree = new Tree(attrNames[index]);

		String[] subA = new String[attrNames.length - 1];
		for (int pre = 0, post = 0; pre < attrNames.length; pre++) {
			if (pre != (Integer) cba[0])
				subA[post++] = attrNames[pre];
		}

		@SuppressWarnings("unchecked")
		Map<Object, Map<Object, List<Sample>>> splits = (Map<Object, Map<Object, List<Sample>>>) cba[2];
		for (Entry<Object, Map<Object, List<Sample>>> entry : splits.entrySet()) {
			Object attrValue = entry.getKey();
			Map<Object, List<Sample>> split = entry.getValue();
			Object child = pruneDecisionTree(randomNode, split, subA);
			tree.setChild(attrValue, child);
		}

		return tree;
	}

	// Read from the raw data and get the linked sample with each attributes
	// defined
	static Map<Object, List<Sample>> readSamples(String csv, String[] attrNames) {
		// String fileName = "training_set.csv";
		String fileName = csv;
		File file = new File(fileName);

		// get the total rows of the CSV file (not including the attributes row)
		rowTraining = 0;
		try {
			rowTraining = getRows(fileName) - 1;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// System.out.println("Total rows of CSV file is : " + rowTraining);

		// Using two dimensional array to store the raw data from the training
		// data set
		rawData = new Object[rowTraining][];
		// Set each row as an array and put into the two dimensional array
		int curRow = 0;

		try {
			Scanner inputStream = new Scanner(file);
			// skip the first attribute row
			inputStream.nextLine();
			// Loop through all the data in the training data set and store in
			// the raw data array
			while (inputStream.hasNext()) {
				String data = inputStream.next();
				String[] values = data.split(",");
				rawData[curRow] = values;

				// System.out.println(data + "***");
				curRow++;
				// System.out.println(curRow);

			}
			inputStream.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// print the 2-D matrix
		// System.out.println("Matrix is below : ");
		// System.out.println(Arrays.deepToString(rawData));

		// Construct the sample object by reading the attributes of the whole
		// training data
		Map<Object, List<Sample>> map = new HashMap<Object, List<Sample>>();
		// loop through all the elements in the 2-D array
		for (Object[] row : rawData) {
			// Generate the sample instance for each element
			Sample sample = new Sample();
			int i = 0;

			// loop through the column of each row
			for (int n = row.length - 1; i < n; i++) {
				sample.setAttribute(attrNames[i], row[i]);
			}
			// set the class attribute as the last column
			sample.setCategory(row[i]);

			// Each time initialize the samples when a new class attribute shows
			List<Sample> samples = map.get(row[i]);
			if (samples == null) {
				samples = new LinkedList<Sample>();
				map.put(row[i], samples);
			}
			samples.add(sample);

			// for (Object key : map.keySet()) {
			// System.out.println("flag is :"+ flag + " ; " + key + " " +
			// map.get(key));
			// }
			// flag++;
		}

		return map;
	}

	static Map<Object, List<Sample>> readValidationSamples(String csv2, String[] attrNames) {
		String fileName = csv2;
		File file = new File(fileName);

		// get the total rows of the CSV file (not including the attributes row)
		rowValidation = 0;
		try {
			rowValidation = getRows(fileName) - 1;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// System.out.println("Total rows of validation CSV file is : " +
		// rowValidation);

		// Using two dimensional array to store the raw data from the training
		// data set
		validationData = new Object[rowValidation][];
		// Set each row as an array and put into the two dimensional array
		int curRow = 0;

		try {
			Scanner inputStream = new Scanner(file);
			// skip the first attribute row
			inputStream.nextLine();
			// Loop through all the data in the training data set and store in
			// the raw data array
			while (inputStream.hasNext()) {
				String data = inputStream.next();
				String[] values = data.split(",");
				validationData[curRow] = values;

				// System.out.println(data + "***");
				curRow++;
				// System.out.println(curRow);

			}
			inputStream.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// print the 2-D matrix
		// System.out.println("Matrix is below : ");
		// System.out.println(Arrays.deepToString(validationData));

		// Construct the sample object by reading the attributes of the whole
		// training data
		Map<Object, List<Sample>> map2 = new HashMap<Object, List<Sample>>();
		// loop through all the elements in the 2-D array
		for (Object[] row : validationData) {
			// Generate the sample instance for each element
			Sample sample = new Sample();
			int i = 0;

			// loop through the column of each row
			for (int n = row.length - 1; i < n; i++) {
				sample.setAttribute(attrNames[i], row[i]);
			}
			// set the class attribute as the last column
			sample.setCategory(row[i]);

			// Each time initialize the samples when a new class attribute shows
			List<Sample> samples = map2.get(row[i]);
			if (samples == null) {
				samples = new LinkedList<Sample>();
				map2.put(row[i], samples);
			}
			samples.add(sample);

			// for (Object key : map.keySet()) {
			// System.out.println("flag is :"+ flag + " ; " + key + " " +
			// map.get(key));
			// }
			// flag++;
		}

		return map2;
	}

	static Map<Object, List<Sample>> readTestSamples(String csv3, String[] attrNames) {
		// String fileName = "training_set.csv";
		String fileName = csv3;
		File file = new File(fileName);

		// get the total rows of the CSV file (not including the attributes row)
		rowTest = 0;
		try {
			rowTest = getRows(fileName) - 1;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// System.out.println("Total rows of validation CSV file is : " +
		// rowTest);

		// Using two dimensional array to store the raw data from the training
		// data set
		testData = new Object[rowTest][];
		// Set each row as an array and put into the two dimensional array
		int curRow = 0;

		try {
			Scanner inputStream = new Scanner(file);
			// skip the first attribute row
			inputStream.nextLine();
			// Loop through all the data in the training data set and store in
			// the raw data array
			while (inputStream.hasNext()) {
				String data = inputStream.next();
				String[] values = data.split(",");
				testData[curRow] = values;

				// System.out.println(data + "***");
				curRow++;
				// System.out.println(curRow);

			}
			inputStream.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// print the 2-D matrix
		// System.out.println("Matrix is below : ");
		// System.out.println(Arrays.deepToString(testData));

		// Construct the sample object by reading the attributes of the whole
		// training data
		Map<Object, List<Sample>> map3 = new HashMap<Object, List<Sample>>();
		// loop through all the elements in the 2-D array
		for (Object[] row : testData) {
			// Generate the sample instance for each element
			Sample sample = new Sample();
			int i = 0;

			// loop through the column of each row
			for (int n = row.length - 1; i < n; i++) {
				sample.setAttribute(attrNames[i], row[i]);
			}
			// set the class attribute as the last column
			sample.setCategory(row[i]);

			// Each time initialize the samples when a new class attribute shows
			List<Sample> samples = map3.get(row[i]);
			if (samples == null) {
				samples = new LinkedList<Sample>();
				map3.put(row[i], samples);
			}
			samples.add(sample);

			// for (Object key : map.keySet()) {
			// System.out.println("flag is :"+ flag + " ; " + key + " " +
			// map.get(key));
			// }
			// flag++;
		}

		return map3;
	}

	// Get the rows of a CSV file
	public static int getRows(String str) throws IOException {
		LineNumberReader lnr = null;
		try {
			lnr = new LineNumberReader(new FileReader(new File(str)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// The case when the CSV file is too large
		lnr.skip(Long.MAX_VALUE);
		// The LineNumberReader object closed to prevent resource leak
		lnr.close();
		return lnr.getLineNumber();
	}

	static Object generateDecisionTree(Map<Object, List<Sample>> categoryToSamples, String[] attrNames) {
		/*
		 * Base case: 1. No more attribute to split on. 2. The node is pure.
		 */

		// If there are no available attributes to choose from, pick the
		// majority of the results
		totalNodes++;

		if (attrNames.length == 0) {
			leaftNumber++;
			int max = 0;
			Object maxCategory = null;
			for (Entry<Object, List<Sample>> entry : categoryToSamples.entrySet()) {
				int cur = entry.getValue().size();
				if (cur > max) {
					max = cur;
					maxCategory = entry.getKey();
				}

				// System.out.println("(String)entry.getKey() is : " +
				// (String)entry.getKey());
			}
			// leafLabel to store each leaf node with a unique key
			leafLabel.put(leaftNumber, maxCategory);
			return maxCategory;
		}

		// If there is one sample, set the the sample attribute as the new
		// classification of the new sample
		if (categoryToSamples.size() == 1) {
			leaftNumber++;
			// System.out.println("(String)categoryToSamples.keySet().iterator().next() is : "
			// + (String) categoryToSamples.keySet().iterator().next());
			Object pureCategory = categoryToSamples.keySet().iterator().next();

			leafLabel.put(leaftNumber, pureCategory);

			return categoryToSamples.keySet().iterator().next();
		}

		/*
		 * Choose the best tested attribute Descriptions: chooseBestAttribute
		 * method returns new Object[] { minIndex, minValue, minSplits };
		 */
		Object[] cba = chooseBestAttribute(categoryToSamples, attrNames);

		// The attribute of the sub-decision tree is the best we have picked
		int index = (Integer) cba[0];
		Tree tree = new Tree(attrNames[index]);

		// we filter out the attributes which has been used
		String[] subA = new String[attrNames.length - 1];
		for (int pre = 0, post = 0; pre < attrNames.length; pre++) {
			if (pre != (Integer) cba[0])
				subA[post++] = attrNames[pre];
		}

		// Construct the tree based on the attribute of each sub-decision tree
		@SuppressWarnings("unchecked")
		Map<Object, Map<Object, List<Sample>>> splits = (Map<Object, Map<Object, List<Sample>>>) cba[2];
		for (Entry<Object, Map<Object, List<Sample>>> entry : splits.entrySet()) {
			Object attrValue = entry.getKey();
			Map<Object, List<Sample>> split = entry.getValue();
			Object child = generateDecisionTree(split, subA);
			tree.setChild(attrValue, child);
		}

		return tree;
	}

	static Object[] chooseBestAttribute(Map<Object, List<Sample>> categoryToSamples, String[] attrNames) {

		int minIndex = -1;
		double minValue = Double.MAX_VALUE;
		Map<Object, Map<Object, List<Sample>>> minSplits = null; // Best split option

		// Choose the largest IG of each attribute
		for (int attrIndex = 0; attrIndex < attrNames.length; attrIndex++) {
			int allCount = 0;

			Map<Object, Map<Object, List<Sample>>> curSplits = new HashMap<Object, Map<Object, List<Sample>>>();

			for (Entry<Object, List<Sample>> entry : categoryToSamples.entrySet()) {
				// 0 or 1 in our case
				Object category = entry.getKey();
				List<Sample> samples = entry.getValue();

				for (Sample sample : samples) {
					Object attrValue = sample.getAttribute(attrNames[attrIndex]);
					Map<Object, List<Sample>> split = curSplits.get(attrValue);
					if (split == null) {
						split = new HashMap<Object, List<Sample>>();
						curSplits.put(attrValue, split);
					}

					List<Sample> splitSamples = split.get(category);
					if (splitSamples == null) {
						splitSamples = new LinkedList<Sample>();
						split.put(category, splitSamples);
					}
					splitSamples.add(sample);
				}
				allCount += samples.size();
				// System.out.println("allCount is : " + allCount);
			}
			double curValue = 0.0; 
			for (Map<Object, List<Sample>> splits : curSplits.values()) {
				double perSplitCount = 0;
				for (List<Sample> list : splits.values())
					perSplitCount += list.size();

				// System.out.println("perSplitCount is : " + perSplitCount);

				double perSplitValue = 0.0;
				for (List<Sample> list : splits.values()) {
					double p = list.size() / perSplitCount;
					perSplitValue -= p * (Math.log(p) / Math.log(2));
				}
				curValue += (perSplitCount / allCount) * perSplitValue;
			}

			// choose the min entropy ( max IG ) as the target attribute to split
			if (curValue < minValue) {
				minIndex = attrIndex;
				minValue = curValue;
				minSplits = curSplits;
			}
			// System.out.println("curValue is : " + curValue);
		}
		// System.out.println("MinIndex is : " + minIndex +
		// " and minValue is : " + minValue + " and minSplits is : " +
		// minSplits);
		return new Object[] { minIndex, minValue, minSplits };
	}

	static Object[] chooseRandomAttribute(Map<Object, List<Sample>> categoryToSamples, String[] attrNames) {

		int minIndex = -1;
		double minValue = Double.MAX_VALUE;
		Map<Object, Map<Object, List<Sample>>> minSplits = null; // Best split option

		/*
		 * Unlike chooseBestAttribute method which choosing the largest IG of
		 * each attribute, chooseRandomtAttribute choose a random attribute
		 * column and return to the generate decision tree method for next
		 * iteration, and we will use random generator to choose the random
		 * index and guarantee randomness.
		 */

		// random.nextInt(max - min + 1) + min to generate a index of attribute
		// from min to max
		Random random = new Random();
		int attrIndex = random.nextInt(attrNames.length);
		int allCount = 0;

		Map<Object, Map<Object, List<Sample>>> curSplits = new HashMap<Object, Map<Object, List<Sample>>>();

		for (Entry<Object, List<Sample>> entry : categoryToSamples.entrySet()) {
			// 0 or 1 in our case
			Object category = entry.getKey();
			List<Sample> samples = entry.getValue();

			for (Sample sample : samples) {
				Object attrValue = sample.getAttribute(attrNames[attrIndex]);
				Map<Object, List<Sample>> split = curSplits.get(attrValue);
				if (split == null) {
					split = new HashMap<Object, List<Sample>>();
					curSplits.put(attrValue, split);
				}

				List<Sample> splitSamples = split.get(category);
				if (splitSamples == null) {
					splitSamples = new LinkedList<Sample>();
					split.put(category, splitSamples);
				}
				splitSamples.add(sample);
			}
			allCount += samples.size();
			// System.out.println("allCount is : " + allCount);
		}
		double curValue = 0.0;

		// choose the random selected attribute to return to the decision tree
		// generated method
		if (curValue < minValue) {
			minIndex = attrIndex;
			minValue = curValue;
			minSplits = curSplits;
		}
		// System.out.println("curValue is : " + curValue);

		// System.out.println("MinIndex is : " + minIndex +
		// " and minValue is : " + minValue + " and minSplits is : " +
		// minSplits);

		// ignore the minValue which is for the choose best attribute , format
		// wise, we keep this , but it has nothing to do with the return.
		return new Object[] { minIndex, minValue, minSplits };
	}

	static double calculateAccuracy(int row, Object tree, Object[][] rawData) {
		double accuracy = 0.0;

		double counter = 0;
		for (int i = 0; i < row; i++) {
			int first = testClassified(tree, rawData[i]);
			counter = counter + first;
			// System.out.println(i +" row is : " + first);
		}

		// System.out.println("counter is : " + counter);
		accuracy = counter / row;
		return accuracy;
	}

	// Method as counter to add up the correctly classified sample
	static int testClassified(Object obj, Object[] row) {
		while (obj instanceof Tree) {
			Tree tree = (Tree) obj;
			String attrName = tree.getAttribute();
			// System.out.println("Test attribute out is : " + attrName);
			for (Object attrValue : tree.getAttributeValues()) {
				// System.out.println("Test attrValue in is : " + attrValue);

				// Test extreme case when decision tree path does not include a
				// particular data element
				if (tree.getAttributeValues().size() == 1 && !attrValue.equals(row[attrToIndex(attrName)])) {
					// System.out.println("Test row[row.length - 1] : " +
					// row[row.length - 1]);
					Object child = tree.getChild(attrValue);

					obj = child;
				}

				if (attrValue.equals(row[attrToIndex(attrName)])) {
					// System.out.println("Matching attrValue in is : " +
					// attrValue);
					Object child = tree.getChild(attrValue);

					obj = child;
				}
			}
		}
		// Count the accuracy by accumulating each data row

		if (obj == null || obj.equals(row[row.length - 1]))
			return 1;
		else
			return 0;
	}

	// Return the index with a given specific attribute

	static int attrToIndex(String str) {
		switch (str) {
		case "XB":
			return 0;
		case "XC":
			return 1;
		case "XD":
			return 2;
		case "XE":
			return 3;
		case "XF":
			return 4;
		case "XG":
			return 5;
		case "XH":
			return 6;
		case "XI":
			return 7;
		case "XJ":
			return 8;
		case "XK":
			return 9;
		case "XL":
			return 10;
		case "XM":
			return 11;
		case "XN":
			return 12;
		case "XO":
			return 13;
		case "XP":
			return 14;
		case "XQ":
			return 15;
		case "XR":
			return 16;
		case "XS":
			return 17;
		case "XT":
			return 18;
		case "XU":
			return 19;
		}
		return -1;
	}

	// print the model
	static void infoOfDecisionTree(Object obj, int level, Object from) {

		if (obj instanceof Tree) {
			Tree tree = (Tree) obj;
			String attrName = tree.getAttribute();
			for (Object attrValue : tree.getAttributeValues()) {

				Object child = tree.getChild(attrValue);
				infoOfDecisionTree(child, level + 1, attrName + " = " + attrValue);

			}
		} else {
			leafNodes++;
			sumOfLeafDepth += level;
		}
	}

	static void outputDecisionTree(Object obj, int level, Object from) {
		for (int i = 0; i < level; i++) {
			System.out.print("|  ");
		}
		if (from != null)
			System.out.printf("(%s):", from);
		if (obj instanceof Tree) {
			Tree tree = (Tree) obj;
			String attrName = tree.getAttribute();
			System.out.printf("[%s = ?]\n", attrName);
			for (Object attrValue : tree.getAttributeValues()) {
				// System.out.print("AttrValue in this tree are :" + attrValue);
				Object child = tree.getChild(attrValue);
				outputDecisionTree(child, level + 1, attrName + " = " + attrValue);

			}
		} else {
			System.out.printf("[CLASS = %s]\n", obj);
		}
	}

	// Sample which include multiple attribute and one class attribute
	static class Sample {

		private Map<String, Object> attributes = new HashMap<String, Object>();

		private Object category;

		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		public void setAttribute(String name, Object value) {
			attributes.put(name, value);
		}

		public Object getCategory() {
			return category;
		}

		public void setCategory(Object category) {
			this.category = category;
		}

	}

	// Every non-leaf node has a sub-decision tree
	static class Tree {

		private String attribute;

		private Map<Object, Object> children = new HashMap<Object, Object>();

		public Tree(String attribute) {
			this.attribute = attribute;
		}

		public String getAttribute() {
			return attribute;
		}

		public Object getChild(Object attrValue) {
			return children.get(attrValue);
		}

		public void setChild(Object attrValue, Object child) {
			children.put(attrValue, child);
		}

		public Set<Object> getAttributeValues() {
			return children.keySet();
		}

	}

}

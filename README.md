# Credit-Risk-Model
Read Me File:  1. Type into the command line: cd Desktop;
2. Drag the DecisionTree.java file along with all the data file on the Desktop ( Or skip the step 1 and  directly drag DecisionTree. java file along with all the data file to the default directory path); 
3.Type into the command line: javac DecisionTree.java (After this step , should generate three class file) 4.Type into the command line: java DecisionTree 1 training_set.csv validation_set.csv test_set.csv 1 (Or other argument or data file name that match the input argument format)   Random Attribute Selection part:  This part in the code starts from line 567. Unlike chooseBestAttribute method which choosing the largest IG of each attribute, chooseRandomtAttribute choose a random attribute column and return to the generate decision tree(line 431) method for next iteration, and we will use random generator to choose the random index and guarantee randomness.To test the decision tree  based on random attribute selection, simply replace line 476 "chooseBestAttribute" with "chooseRandomAttribute".

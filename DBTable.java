//Jack Newman 
package p7;
import java.io. *; 
import java.util.*;

import javax.sql.rowset.RowSetFactory; 

public class DBTable {
	 private RandomAccessFile rows; //the file that stores the rows in the table
	 private int numOtherFields; //Amount of fixed length character fields 
	 private int otherFieldLengths[]; //The length of each fixed length character field
	 private long free; //head of the free list space for rows
	 private BTree bTree; 
	 
	 //add other instance variables as needed 
	 
	 
	 // Use this constructor to create a new DBTable. 
	  
	 //bsize is the block size. It is used to calculate the order of the B+Tree
	 //A B+Tree must be created for the key field in the table
	 //If a file with name filename exists, the file should be deleted before the new file is created.
	 
	 
	 public DBTable(String filename, int fL[], int bsize ) throws IOException {
		 File path = new File(filename); //Filename is the name of the file used to store the table. 
		 if(path.exists()) {path.delete(); } 
		 bTree = new BTree(filename, bsize); //making bTree object
		 
		 rows = new RandomAccessFile(path, "rw"); 
		 numOtherFields = fL.length; //fL.length indicates how many other fields are part of the row. OR indicates how many values in each row. 
		 otherFieldLengths = fL; //fL is the lengths of the otherFields in the array. Just copying the array. 
		 free = 0; 
		 rows.writeInt(numOtherFields);
		 for(int i = 0; i < numOtherFields; i++ ) { rows.writeInt(otherFieldLengths[i]); } 
		 rows.writeLong(free); 		 
		 }
	 
	//Use this constructor to open an existing DBTable
	 public DBTable(String filename) throws IOException {
		 File path = new File(filename); //Filename is the name of the file used to store the table. 
		 if(!path.exists()) { System.out.print("That file does not exist."); return; } //Might not need this. 
		 rows = new RandomAccessFile(path, "rw"); //"Rw" is due to file arugment path. 
		 rows.seek(0); //Might not need this 
		 
		 numOtherFields = rows.readInt(); 
		 otherFieldLengths = new int[numOtherFields]; 
		 for(int i = 0; i < numOtherFields; i++ ) { otherFieldLengths[i] = rows.readInt(); } 
		 free = rows.readLong(); 
		 
		 
		 }
	 
	 
	
	 /*Each row consists of unique key and one or more character array fields. Each character array field is a fixed length field (for example 10 characters).
	 Each field can have a different length. Fields are padded with null characters so a field with a length of x characters always uses space for x characters.
	 */
	 
	 private class Row {
		 private int keyField;
		 private char otherFields[][];
		 
		 
			 private Row(int keyValue, char fields[][])
			 { 
				 keyField = keyValue; 
				 otherFields = fields; 
			 }
			 
			 
			 private Row(long addr) throws IOException
			 { 
				 rows.seek(addr); 
				 keyField = rows.readInt(); 
				 otherFields = new char[numOtherFields][]; 
				 for(int i = 0; i < numOtherFields; i++) { otherFields[i] = new char[otherFieldLengths[i]]; }  
				 for(int row = 0; row < numOtherFields; row++){ 
					 for(int col = 0; col < otherFieldLengths[row]; col++) { otherFields[row][col] = rows.readChar(); } 
				}
				 
				 
			 }
			 
			 private void writeNode(long addr) throws IOException
			 { 
				 rows.seek(addr); 
				 rows.writeInt(keyField); 
				 for(int row = 0; row < numOtherFields; row++){ 
					 for(int col = 0; col < otherFieldLengths[row]; col++) { 
						 rows.writeChar(otherFields[row][col]); } }
			 }
		 }
	 

	// If a row with the key is not in the table, the row is added and the method  returns true otherwise the row is not added and the method returns false.
	//The method must use the B+tree to determine if a row with the key exists. If the row is added the key is also added into the B+tree.
	 
	 	public boolean insert(int key, char fields[][]) throws IOException {
		 for(int i = 0; i < numOtherFields; i++ ) { if(fields[i].length != otherFieldLengths[i]) { return false; } } //PRE: the length of each row is fields matches the expected length
		 if(bTree.search(key) != 0) { return false; }  //I dont need the address given from search. Because it will be saved on the stack. If the value return is greater than zero. That means it does exist in the table
		 long addr = getFree(); 
		 Row temp = new Row(key, fields); 
		 temp.writeNode(addr);
		 return bTree.insert(key, addr); 
		 } 
	 
	 	//This gets either the first spot in the free list if it exists. Otherwise it get the next spot in memory.
	 	 private long getFree() throws IOException { 
	 		long adress; 
			if(free != 0){  //If the freelist has a spot. I want to take that spot
				adress = free; //I save the first spot in the freeList. 
				rows.seek(free); //I seek spot the freelist has saved. 
				free = rows.readLong(); //Now I read the key value of the first thing in the freelist. Which represents either the end of the freelist, or the next spot. 
				rows.seek(0); //I start back up to the top of the file. 
				rows.readLong(); //I read the root. Which then sets the pointer to the next spot which is the freelist. 
				rows.writeLong(free); //I write in 2nd value in the freelist as first spot in the freelist. Which could either be zero or another value. 
				return adress; //I return the first spot in the freelist. 
			} 
			return rows.length(); //else I return the last spot in memory. 
	 		 
	 	 }
	 		
	 	//If a row with the key is found in the table return a list of the other fields in the row. The string values in the list should not include the null characters.
	 	//If a row with the key is not found return an empty list.   The method must use the equality search in B+Tree
	 	 
		 public LinkedList<String> search(int key) throws IOException {
			 long keyAddress = bTree.search(key); 
			 if(keyAddress == 0) { return new LinkedList<String>(); } //The key was not found.
			 else return llBuilder(keyAddress); //Notes for later optmization If i ever return to project. Techinally im creating a full stack of a search path that i dont need. I only really need it to return the keyAdrres for me. Could just create a custom function that copies it 
		 } 
		
		 
		//PRE: low <= high
        //For each row with a key that is in the range low to high inclusive a list of the fields (including the key) in the row is added to the list
		// returned by the call. If there are no rows with a key in the range return an empty list. The method must use the range search in B+Tree		 
		 
		 public LinkedList<LinkedList<String>> rangeSearch(int low, int high) throws IOException {
			 if( low > high) return new LinkedList<LinkedList<String>>(); 
			 LinkedList<Long> addrList = bTree.rangeSearch(low, high); //makes a linked list of longs that holds all the adresses of data that are within range. 
			 if( addrList.peek() == null) { return new LinkedList<LinkedList<String>>(); } //Just a return empty linked list 
			 LinkedList<LinkedList<String>> rangeLL = new LinkedList<LinkedList<String>>(); //make a new linked list. 
			 while( !addrList.isEmpty()) { rangeLL.add(llBuilder(addrList.remove())); } //At each address I make a linkedList full of the data from that address, then i add each newely created linked list to the the Linked list of linked lists. 
			 return rangeLL; 
			 }
			 
		 
		 //Print the rows to standard output is ascending order (based on the keys) Include the key and other fields print one row per line
		 public void print() throws IOException {
			 
			 LinkedList<LinkedList<String>> fullList = rangeSearch(Integer.MIN_VALUE,Integer.MAX_VALUE ); //Call me lazy, I know this isnt best optimized. But hey im working time crunch here. I want to work on this over break 
			 LinkedList<Long> addrList = bTree.rangeSearch(Integer.MIN_VALUE, Integer.MAX_VALUE); //This just gets me the addresses that I wants of each value of print. So i can associate it. 
			
			 
			 while( !fullList.isEmpty() ) { //Just your standerd while loop 
				 LinkedList<String> temp = fullList.poll(); //I make a temp LinkedList that takes from head of the list. 
				 StringBuilder tempString = new StringBuilder(); //Just a temp stringBuilder 
				 tempString.append("Address:" ); tempString.append(addrList.poll()); tempString.append("\t"); //REMOVE WHEN DONE 
				 tempString.append("Key: " ); 
				 tempString.append(temp.poll());  //I remove the key
				 tempString.append("\t");
				 tempString.append("Strings: " ); 
				 for(int i = 0; i < numOtherFields; i++) { //I add each String. 
					 tempString.append(temp.poll()); 
					 tempString.append("\t"); 
					 } 
				 System.out.println(tempString.toString()); //Then I print it out. 
			 } 
			 
			 
			
			 }
		public void close() throws IOException { 
			bTree.close(); 
			rows.close(); 
			//close the DBTable. The table should not be used after it is closed
			 }
		
		//This just builds the linked list of strings based off the rows. 
		
		public LinkedList<String> llBuilder(long keyAddress) throws IOException{ 
			 LinkedList<String> rowLL = new LinkedList<String>(); //make a linkedList 
			 Row row = new Row(keyAddress); //Make the row 
			 
			 rowLL.add("" + row.keyField); 
			 for(int curRow = 0; curRow < numOtherFields ; curRow++) { //Make a for loop to read each row of the otherFields, so i can peice togther each word 
				 StringBuilder temp = new StringBuilder(otherFieldLengths[curRow]); //Make a temp stringBuilder. So it doesnt create one every iteration for concectation. 
				 for(int col = 0; col < otherFieldLengths[curRow] && row.otherFields[curRow][col] != '\0'; col++) { temp.append(row.otherFields[curRow][col]); }
				 rowLL.add(temp.toString()); 
			 }
			 return rowLL; 
		 }
		 
		 /* public boolean remove(int key) {
		 
		 If a row with the key is in the table it is removed and true is returned
		 otherwise false is returned.
		 The method must use the B+Tree to determine if a row with the key exists.

		 If the row is deleted the key must be deleted from the B+Tree
		 
		 }
	*/ 
		 
		//This just calls my print that helps me debug. 
		
		public void treePrint() throws IOException {
			bTree.print();
			System.out.println();
		}
}

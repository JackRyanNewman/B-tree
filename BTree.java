//Jack Newman 

package p7;
import java.io. *;
import java.util.*; 

public class BTree {
	 private long root; 
	 private long free;
	 private int blockSize;
	 private int order;
	 private RandomAccessFile f;
	 private Stack<BTreeNode> path;  //A stack I used to hold all the nodes and the search path i navigate. So i can edit from the top down. 
	
	
	 //add instance variables as needed.
	 
	
	//bsize is the block size. This value is used to calculate the order of the B+Tree. All B+Tree nodes will use bsize bytes makes a new B+tree
	public BTree(String filename, int bsizeA) throws IOException {
		File path = new File(filename + "ex"); 
		if(path.exists()) path.delete(); //If existing BTreeFiles exists. I should delate it. 
		f = new RandomAccessFile(path, "rw"); //"Rw" is due to file arugment path.  
		root = 0; 
		free = 0; 
		blockSize = bsizeA; 
		f.writeLong(0); //Declaring root, and writing it in
		f.writeLong(0); //Declaring free, and writing it in 
		f.writeInt(blockSize); //Declaring blockSize and writing it in 
		order = bsizeA/12; //Its divided by 12. Bc blocksize = 4 + 4(m-1) + 8(m). or 4(3m) = 12m = order.
			 }
		
	//open an existing B+Tree
	public BTree(String filename) throws IOException {
		File path = new File(filename + "ex"); 
		if(!path.exists()) { System.out.print("That file does not exist."); return; } //Might not need this. 
		f =  new RandomAccessFile(path, "rw");
		f.seek(0); //starting the beinging of the file 
		root = f.readLong(); 
		free = f.readLong(); 
		blockSize = f.readInt();
		order = blockSize/12;
			 }
	 
	 
	 private class BTreeNode {
	 private int count;
	 private int keys[];
	 private long children[];
	 private long address; //the address of the Node


	 
	 
	 //constructor for BTreeNode
	 private BTreeNode(int count, int key, long childAddr, long nodeAddr) throws IOException { 
		 this.count =  count; 
		 keys = new int[order]; 
		 children = new long[order+1]; 
		 keys[0] = key; 
		 children[0] = childAddr;
		 this.address = nodeAddr; 
	 }
	 
	 //Reading in the node that exists. 
	 private BTreeNode (long addr) throws IOException { 
		 f.seek(addr); 
		 count = f.readInt(); //Will be a either a negative or postive number. 
		 keys = new int[order]; 
		 children = new long[order + 1]; //If typeOfleaf is false then its a leaf. Which means there is a adress at the end. Else  
		 int i;
		 for(i = 0; i < order-1; i++) { keys[i] = f.readInt(); } //I read to order minus 1
		 for(i = 0; i < Math.abs(count) + 1; i++) { children[i] = f.readLong(); } //I read to order minus 1. Cause the last spot is either a adress spot or a child spot
		
		 this.address = addr; 
		 
	 }
	 
	 //writes the data at the specific spot. 
	 private void writeNode(long addr) throws IOException { 
		 f.seek(addr);
		 f.writeInt(count);
		 int i; 
		 for(i = 0; i < keys.length-1; i++) { f.writeInt(keys[i]); } //Arrays.length gives you the full length. I do minus 1. 
		 for( i = 0; i < children.length-1; i++) { f.writeLong(children[i]); } //I read till the last spot. 
	 	}
	 
	 
	 
	 } 
	
	 	 
	 //If key is not a duplicate add key to the B+tree addr (in DBTable) is the address of the row that contains the key
	 // return true if the key is added return false if the key is a duplicate 
	 
	 public boolean insert(int key, long addr) throws IOException {
		 if(root == 0) { 
			 BTreeNode temp = new BTreeNode(-1, key, addr, getFree()); //Construct Node  
			 temp.writeNode(temp.address);
			 root = temp.address; 
			 return true; 
		 }
		 boolean split = true; 
		 long splitAddress[]; //This array will hold two data points I need if a spilt occurs. spot 0 = lowest value new split node, spot 1 = address of the newNode created for the split 
		 BTreeNode temp = path.pop(); 
		 
		 //Either two cases. At the top the stack its going to have no space or space. 
		 if(Math.abs(temp.count) < order - 1) { //That means it has space. I sort the leaf and add true.
			 temp = sortLeaf(temp, key, addr); //I sort the BTreeNode
			 temp.writeNode(temp.address); //I rewrite over its old spot. 
			 return true; //I return true. There was no spilting at all. So I dont need to do anything else. 
		 	}   
		 else { //Else there is no space. I have to spilt. 
			splitAddress = spiltLeaf(temp, key, addr); 
			split = true; 
			 } 
		
		 while(!path.empty() && split ) //If the stack is not empty, and split is true. I have to check the parents above.
		 { 
			 temp = path.pop(); //I get the node above. 
			 if(temp.count < order - 1) { //If the node above has space. Then I insert the least value of the NewNode, and the address of the newNode
				 temp = sortNonLeaf(temp, (int) splitAddress[0], splitAddress[1]); 
				 temp.writeNode(temp.address); //I write it at its adress; 
				 split = false; 
			 }
			 else { //else I split the nonLeaf	
				 splitAddress = splitNonLeaf(temp, (int) splitAddress[0], splitAddress[1]); //This method takes the tempNode, adds in the data in its extra spots. Sorts the data. Then splits the data. 
				 split = true; //Then I make split true
				 }  
		}
		if(split) //If stack is empty, and spilt is still true that means you have split the root. 
		{ 
			 BTreeNode rootNode = new BTreeNode(1, (int)splitAddress[0], temp.address, getFree()); //because my constructor inserts the first child at 0. which represents the nodes that are less than that. SO I use temp.address. /Even if I dindt update the node in this scope during a split. Its address still represents the keys values left side. Which is the valeus less than the key vales
			 rootNode.children[1] = splitAddress[1]; //This is the address that represents the key values right Side. 
			 root = rootNode.address; 
			 rootNode.writeNode(rootNode.address);
		 }
		return true; 
	 } 
	 
	 
	 
	 
	 //This private method takes the the newNode and tempNode. Temp gives half of its values to the newNode. Then it resets the counts for both which splits the values up.  
	 //Writes both nodes to the file. I return a array that holds the two values I need to know.  
	 
	 private long[] splitNonLeaf(BTreeNode temp, int key, long addr) throws IOException { 
		 BTreeNode newNode = new BTreeNode(0,0,0, getFree()); //Construct the newNode here. 
		 temp = sortNonLeaf(temp, key, addr); //I put everything into one node. 
		 
		System.arraycopy(temp.keys, order/2  + 1, newNode.keys, 0, newNode.keys.length -1 - order/2); //The middle spot that im going to pass up is always going to be at spot order/2 in the array. But I want this function to start at order/2 + 1. Because of that plus one, and order/2 I need to offset how far the copy function goes till so thats why i minus it
		System.arraycopy(temp.children, (int) Math.ceil( (double) (order + 1) / 2), newNode.children, 0, newNode.children.length - 1 - order/2 ); //I start are ciel((order+1)/2) because that represents the adress of the spot im passing up. This spot represents the value that is less than order/2 + 1. 
		
		temp.count = order/2; //Temp count will always be floor(order/2) even or odd. 
		newNode.count = ((int) Math.ceil( (double) order /2 ) -1); //because im always taking the center value, and order/2 always represents where the center is. Because im passing that value up, its exlcuded from the array. Then subtracting one will represent its proper count. 
		 
		temp.writeNode(temp.address); //write the temp node. 
		newNode.writeNode(newNode.address); //new address represents the spilt contents
		long[] splitAddress = {temp.keys[order/2], newNode.address}; //Order/2 will always be the center. I pass up the newNode adress which represents the <= keys address 
		 
		return splitAddress; //Return the needed data. 
	 }
	 
	 
	 //I sort the nonLeaf. It does that by finding where the key would be inserted. Then I use the java sort function. 
	 //Then using the postion it would be inserted I insert it into the correlating children array. Then I return the newly updated node. 
	 

	 private BTreeNode sortNonLeaf(BTreeNode temp, int key, long addr) { 
		 int keyPostion = temp.count; //This represents the spot where im inserting the new key, and represents empty spot keys array, and childrens linked list spot. Arrays are 0 -1. Keys are represents 1 - count. To get each key is 0 - count - 1. 
		 temp.count++; //I update count secound. Because I like to sort my array based off the orginal count. 
			
			temp.keys[keyPostion] = key;  //I insert at last spot in the array. 
			temp.children[keyPostion+1] = addr; //I Insert into the last postion into the array. 
			
			int insertPos = 0; //Represents spot of insert. Will translate to postion of child, after some editing.  
			
			for(int i = 1; i < keyPostion + 1; i++) //I start at one for effieceny. I will represent the spot im inserting at. Since array is 0-keyPostion. I the forLoop to be one bigger because if it equal keyPostion it will end. 
			{ 
				if( key > temp.keys[i]) continue; //If its greater then. Just update instantly. 
				if( key < temp.keys[i]) i--; //If it isnt greater than the current key. Then its either less previous key, or its in between the two keys 
				
				if( key < temp.keys[i]) //This means it takes the first spot in the array. That means its child will be at i + 1, because the key value which i spilt is the greater key value and left of the split is everything less than it. Which is represented by the adress at 0. 
				{ 
					Arrays.sort(temp.keys, 0, keyPostion + 1); 
					insertPos = 1; //Everything from i + 1 till the end will be rotated one foward. The thing at the back will now become the front. 
					break;
				}
				else if(key > temp.keys[i]) { //This means key is i < key < i + 1. So it will take i + 1's spot.
					Arrays.sort(temp.keys, 0 , keyPostion + 1); 
					insertPos = i+2; //Its child will take i + 2 spot, and everything at i + 2 will be moved 1 foward. This is because at postion i + 1 in the children array represents everything less than it. It holds that adress that is equal to or greater than it.  
					break; 	
				}
				else if( key < temp.keys[0] ) { //This means it takes the first spot in the array. Everything at i will move 1 spot foward.
					Arrays.sort(temp.keys, 0, keyPostion + 1); 
					insertPos = 1; 
					break; 
				} 
				else return temp; //Else its equal because its already in the array, and it reached its postion. That means its greater than everything else. Then you have to do nothing. The keys and children are already sorted.
			}
			keyPostion++; //I need to update my keyPostion because now i will be sorting an array thats one bigger and i need to sort the back. 
			for(; keyPostion> insertPos; keyPostion--) { temp.children[keyPostion] = temp.children[keyPostion-1]; } //I will shift everything from the end of the the Array to where the insert occured. 
			temp.children[insertPos] = addr; //Then I insert the missing postion into last slot. 
			return temp; 
	 }
	 
	 
	 
	 //This private method creates a newNode and takes a tempNode. Temp gives half of its values to the newNode. Then it resets the counts for both which splits the values up. Then lastly i set last spot in the array of the temp
	 //as the address of the newNode because that represents the linked list. Writes both nodes to the file. I return a array that holds the two values I need to know.  
	 
	 private long[] spiltLeaf(BTreeNode temp, int key, long addr) throws IOException { 
		 BTreeNode newNode = new BTreeNode(0,0,0, getFree()); //Construct the newNode here. Doesnt matter what i put into the constructors because im spliting 
		 temp = sortLeaf(temp, key, addr); //I put everything into one node. 
		 temp.count++; //because I did all my calcutions off it being 
		 
		 System.arraycopy(temp.keys, order/2, newNode.keys, 0, newNode.keys.length - order/2); //It goes to array im copying from the postion of first int. The 2nd array is what is reciving the values starting from postion 0. The last part is how far it copies till. I will copy the range from the center to the end. Arrays are 0 - x. Min keys values are from 0 to 3. 
		 System.arraycopy(temp.children, order/2, newNode.children, 0, newNode.children.length - order/2); 
		 newNode.count = (int) -Math.ceil( (double) order /2 ); //With this it doesnt matter if the order is odd or even. If its even, it spilts even. If odd it splilts order/2 + 1
		 temp.count = -order/2; //The left array. If order is even. Its count will be half of order. if its odd. Then will floor(order/2) 
		 temp.children[Math.abs(temp.count)] = newNode.address; //Array 0 - x. Count 1 - x. Count will represent the extra spot. Because the temp is a leaf i need to set its LinkedList address to the newNode. 
		 
		 temp.writeNode(temp.address); //write the temp node. 
		 newNode.writeNode(newNode.address); //new address represents the spilt contents
		 long[] splitAddress = {newNode.keys[0], newNode.address}; //
		 return  splitAddress; 	 
	 }
	 
	 
	 //This method sorts leafNodes. It does that by finding where the key would be inserted. Then I use the java sort function. 
	 //Then using the postion it would be inserted I insert it into the correlating children array. Then I return the newly updated node. 
	 private BTreeNode sortLeaf(BTreeNode temp, int key, long addr) { 
		int keyPostion = Math.abs(temp.count); //This represents the spot where im inserting the new key, and represents the spot of the back of the keys array, and childrens linked list spot. Arrays are 0 -1. Keys are represents 1 - count. To get each key is 0 - count - 1. 
		temp.count--; //Update count. Secound. Because I need to sort my array based off the count size before making it bigger. I increase the count of the leaf by 1. By minusing it. 
		
		temp.keys[keyPostion] = key;  //I insert at last spot in the array. 
		temp.children[keyPostion+1] = temp.children[keyPostion]; //I take the linked list part, and put it at the end.
		temp.children[keyPostion] = addr; //I take the new address and put it at where the linked List postion was. I COULD GET RID OF THIS. However i would have to place it when do a return when it finds its equal. 
		int insertPos = 0; //Represents spot of insert. Will translate to postion of child. 
		
	
		for(int i = 1; i < keyPostion + 1; i++) //I start at one for effieceny. I will represent the spot im inserting at 
		{ 
			if( key > temp.keys[i]) { continue;}  //If its greater then. Just update instantly. 
			if( key < temp.keys[i]) { i--; }//If it isnt greater than the current key. Then its either less previous key, or its in between the two keys 
			
			
			if( key < temp.keys[i]) //This means it takes the first spot in the array. Everything at i will move 1 spot foward. 
			{ 
				Arrays.sort(temp.keys, 0, keyPostion + 1); 
				insertPos = i; 
				break;
			}
			else if(key > temp.keys[i]) { //This means key is i < key < i + 1. So it will take i + 1's spot, and move everything at i + 1 one foward. 
				Arrays.sort(temp.keys, 0 , keyPostion + 1); //I have to add a plus 1, because the last spot in this method is exslucive, So it will sort everything up until that point 
				insertPos = i+1 ; 
				break; 	
			}
			else if( key < temp.keys[0] ) { //This means it takes the first spot in the array. Everything at i will move 1 spot foward.
				Arrays.sort(temp.keys, 0, keyPostion + 1); 
				insertPos = 0; 
			} 
			else return temp; ///Else because its already in the array, and it reached its postion. That means its greater than everything else. Then you have to do nothing. The keys and children are already sorted.
		}
		//I will shift everything from the end of the the Array to where the insert occured. 
		for(; keyPostion > insertPos; keyPostion--) { temp.children[keyPostion] = temp.children[keyPostion-1];} //I move everything down by one.
		temp.children[insertPos] = addr; //Then I insert the missing postion into the 2nd last slot. 
		return temp; 
	 }
	 
	 
	 
	 //This is an equality search. If the key is found return the address of the row with the key. otherwise if the key was not found return 0
	  
	 public long search(int k) throws IOException {
		 if(root == 0) { return 0; } //The tree is empty, thus the value DNE 
		 path = new Stack<BTreeNode>(); //IF a stack already exists, and wasnt used. Then its useless. So I just make a new one. 
		 return search(k, root);
		 }
	  
	 
	 //This method recursivley searches for the key. If the key is found returns adress/row of the key. Else it returns 0. 
	 
	 private long search(int searchKey, long addr) throws IOException {
		 BTreeNode temp = new BTreeNode(addr); 
		 path.push(temp); //I push the search path onto the stack. The higher you are on the stack the further down you are in the tree. 
		 long postion; 
		 
		 if(temp.count > 0)  { //if Count is greater than zero then you knows it a nonleaf. 
			 int i = 0; 
			 while( i < temp.count) { //we start at one because. We need to check if the item is between two things. 
				 if( searchKey > temp.keys[i]) { i++; continue; } //just update if bigger. 
				 if( searchKey < temp.keys[i] ) return postion = search(searchKey, temp.children[i]); //If its less, then it correlating children spot is i. This is because we always check if the thing is bigger. If it reaches this spot that means it was bigger than the last, but smaller than the next one.  
				 if( searchKey == temp.keys[i]) return postion = search(searchKey, temp.children[i+1]); //Now if its not < or > then that must mean its equal to it. I do i + 1. Bc keys<= search key to childs spot are i to i + 1 when they are equal. However if its less, then it correletes i to i.  
			   } 
			  return postion = search(searchKey, temp.children[i/*-1*/]); //Count represents the last spot in the child array, and with this being a nonleaf. That means when i = count. That the search key was greater than everything. 
		 	} 
		 
		 else { //Its a leafNode 
			 for(int i = 0; i < Math.abs(temp.count); i++) { if( searchKey == temp.keys[i] ) return temp.children[i] ; } //Key was found so i return the key address 
			 return 0; 	//If it never stopped in the for loop. Then that means it doesnt exist. 
			 }
		 }
	 
	 
	 
	 //PRE: low <= hig. Return a list of row addresses for all keys in the range low to high inclusive. 
	 //The implementation must use the fact that the leaves are linked. return an empty list when no keys are in the range
	
	 
	public LinkedList<Long> rangeSearch(int low, int high) throws IOException{ 
			if(root == 0) { return new LinkedList<Long>(); } 
			search(low); //I search for the lowest value. 
			BTreeNode temp = path.pop(); //Search puts stuff onto the path 
			
			LinkedList<Long> longLL = new LinkedList<Long>(); 
			boolean gate = true; //This gate will controll me going through the linkedLists
			
			while(gate)
			{ 
				int whatchamacallit; //Its a whatchamacallit. Ya know a whatchamacallit. This is a very sane thing to due 
				for(whatchamacallit = 0; whatchamacallit < Math.abs(temp.count); whatchamacallit++) { //I search within the constraints of the counts 
					if(low <= temp.keys[whatchamacallit] && temp.keys[whatchamacallit] <= high) {longLL.add(temp.children[whatchamacallit]);} //I print off the values that are within range 
				}
				if(Math.abs(temp.count) == whatchamacallit && temp.children[Math.abs(temp.count)] != 0) {temp = new BTreeNode(temp.children[Math.abs(temp.count)]);} //If i reached the end of the node, then I check if there is a next linked list. 
				else {gate = false;} //Else if dindt reach the end or the next spot in the linked list doesnt exist. So i end and retunr 
			}
			return longLL;
		}
	
	 	
	
	//print the B+Tree to standard output print one node per line This method can be helpful for debugging
		public void print() throws IOException { print(root);}

		
		//A usefull method I used to help me test and debug. 
	    private void print(long r) throws IOException {

	        if(r != 0) {

	            BTreeNode x = new BTreeNode(r);
	            int i;

	            System.out.printf("Address: %d\t Count %d\t ", x.address, x.count);


	            if(x.count < 0) {
	                System.out.print("Keys: ");
	                for(i = 0; i < -x.count; i++) {
	                    System.out.printf("[%d]:%d\t", i, x.keys[i]);
	                }
	                for(; i<order; i++) {
	                    System.out.printf("\t");
	                }
	                System.out.print("Children: ");
	                for(i = 0; i < -x.count; i++) {
	                    System.out.printf("[%d]:%d\t", i, x.children[i]);
	                }
	                for(; i<order; i++) {
	                    System.out.printf("\t");
	                }
	                System.out.printf("Next Address: %d", x.children[Math.abs(x.count)]);
	                System.out.println();
	            }else {
	                System.out.print("Keys: ");
	                for(i = 0; i < x.count; i++) {
	                    System.out.printf("[%d]: %d\t", i, x.keys[i]);
	                }
	                for(; i<order; i++) {
	                    System.out.printf("\t");
	                }
	                System.out.print("Children: ");
	                for(i = 0; i < x.count+1; i++) {
	                    System.out.printf("[%d]:%d\t", i, x.children[i]);
	                }
	                for(; i<order; i++) {
	                    System.out.printf("\t");
	                }
	                System.out.println();

	                for(i = 0; i<x.count+1;i++)
	                    print(x.children[i]);
	            }
	        }

	    }

		//print the B+Tree to standard output
		//print one node per line
		//This method can be helpful for debugging
			 
	public void close() throws IOException { f.close(); }
	
	
	

	private long getFree() throws IOException { 
 		long address; 
		if(free != 0){  //If the freelist has a spot. I want to take that spot
			address = free; //I save the first spot in the freeList. 
			f.seek(free); //I seek spot the freelist has saved. 
			free = f.readLong(); //Now I read the key value of the first thing in the freelist. Which represents either the end of the freelist, or the next spot. 
			f.seek(0); //I start back up to the top of the file. 
			f.readLong(); //I read the root. Which then sets the pointer to the next spot which is the freelist. 
			f.writeLong(free); //I write in 2nd value in the freelist as first spot in the freelist. Which could either be zero or another value. 
			return address; //I return the first spot in the freelist. 
		} 
		return f.length(); //else I return the last spot in memory. 
 		 
 	 }
	
	
	//This private method recures and finds the leaf will be the furthest left leaf that within the range.  
		/*
		private BTreeNode rangeSearch(long addr, int low, int high) throws IOException {
			search(low);
			return path.pop();
		}
	*/
	 
	/* public long remove(int key) {
		
			 If the key is in the Btree, remove the key and return the address of the
			 row
			 return 0 if the key is not found in the B+tree
			 
			 }
	 */
	
}

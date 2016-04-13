import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.lang.reflect.Method;

public class TransactionClient {

 	private TransactionClient() {}

 	public static void main(String[] args) {

 		String host 	= null;
		// Server to connect.
 		//String myserver = (args.length < 1) ? "Site1Server" : args[0];
		String mnodes 	= (args.length < 1) ? "0" : args[0];		// Total Sites
		//String serverNo = (args.length > 1) ? args[1] : "1";		// Initial Coordinator
		String mymthd 	= (args.length > 1) ? args[1] : "incVariable";	// Method for transaction
		
 		try {
			//String myserver		= "Site" + serverNo + "Server";
 			
 			//ThreePCInterface stub 	= (ThreePCInterface) registry.lookup(myserver);
			//System.out.println("Transaction Submitted at " + myserver);

			//int sno 	= Integer.parseInt(serverNo);
			Registry registry 	= LocateRegistry.getRegistry(host);
			int total_sites		= Integer.parseInt(mnodes);
			for(int i=1; i<=total_sites+1; i++) {
				//if(i != sno) {
					String cohort 		= "Site" + i + "Server";
 					ThreePCInterface stub 	= (ThreePCInterface) registry.lookup(cohort);
					stub.setTransFlag(1);
					System.out.println("Transaction Submitted at " + cohort);
				//} 
			}
 		} catch (Exception e) {
 			System.err.println("Client exception: " + e.toString());
 			e.printStackTrace();
 		}
 	}
}

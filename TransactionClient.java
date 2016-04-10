import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.lang.reflect.Method;

public class TransactionClient {

 	private TransactionClient() {}

 	public static void main(String[] args) {

 		String host 	= null;
		// Server to connect.
 		String myserver = (args.length < 1) ? "Site1Server" : args[0];
		String mymthd 	= (args.length > 1) ? args[0] : "incVariable";
 		try {
 			Registry registry 	= LocateRegistry.getRegistry(host);
 			ThreePCInterface stub 	= (ThreePCInterface) registry.lookup(myserver);
			stub.setTransFlag(true);
			System.out.println("Transaction Submitted at " + myserver);
		//	Class c 		= Class.forName(myserver);
		//	Object o		= c.newInstance();
			// Execute the transaction.
			//Method m		= c.getMethod(mymthd);
 			//m.invoke(o);
 		} catch (Exception e) {
 			System.err.println("Client exception: " + e.toString());
 			e.printStackTrace();
 		}
 	}
}

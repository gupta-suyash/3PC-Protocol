import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
	
public class Site1Server implements ThreePCInterface {
	static boolean reqflag = false;
	static int responseIs = 0, decisionIs = 0, ackflag = 0, transflag = 0;
	static int x = 3;
	public Site1Server() {}
	
	public void setTransFlag(int value) {
		//System.out.println("Trans Flag is set");
		Site1Server.transflag = value;
	}

	public int getTransFlag() {
		return Site1Server.transflag;
	}

	public void setReqFlag(boolean flag) {
		Site1Server.reqflag = flag;
	}

	public boolean getReqFlag() {
		return Site1Server.reqflag;
	}

	public void setResponse(int response) {
		Site1Server.responseIs = response;
	}

	public int getResponse() {
		return Site1Server.responseIs;
	}

	public void setDecision(int decision) {
		Site1Server.decisionIs = decision;
	}

	public int getDecision() {
		return Site1Server.decisionIs;
	}
	
	public void setVariable(int y) {
		Site1Server.x = y;
	}

	public void incVariable() {
		Site1Server.x++;
	}

	public void setAck(int value) {
		Site1Server.ackflag = value;
	}

	public int getAck() {
		return Site1Server.ackflag;
	}
	
	public static void main(String args[]) {
		try {
			Site1Server obj = new Site1Server();
			ThreePCInterface stub = (ThreePCInterface) UnicastRemoteObject.exportObject(obj, 0);
			
			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind("Site1Server", stub);
		
			System.out.println("Site 1 Started !");
		} catch (Exception e) {
		    System.err.println("Server exception: " + e.toString());
		    e.printStackTrace();
		}
	}
}

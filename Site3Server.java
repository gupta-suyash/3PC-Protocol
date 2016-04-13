import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
	
public class Site3Server implements ThreePCInterface {
	static boolean reqflag = false;
	static int responseIs = 0, decisionIs = 0, ackflag = 0, transflag = 0;
	static int x = 3;
	public Site3Server() {}
	
	public void setTransFlag(int value) {
		//System.out.println("Trans Flag is set");
		Site3Server.transflag = value;
	}

	public int getTransFlag() {
		return Site3Server.transflag;
	}

	public void setReqFlag(boolean flag) {
		Site3Server.reqflag = flag;
	}

	public boolean getReqFlag() {
		return Site3Server.reqflag;
	}

	public void setResponse(int response) {
		Site3Server.responseIs = response;
	}

	public int getResponse() {
		return Site3Server.responseIs;
	}

	public void setDecision(int decision) {
		Site3Server.decisionIs = decision;
	}

	public int getDecision() {
		return Site3Server.decisionIs;
	}
	
	public void setVariable(int y) {
		Site3Server.x = y;
	}

	public void incVariable() {
		Site3Server.x++;
	}

	public void setAck(int value) {
		Site3Server.ackflag = value;
	}

	public int getAck() {
		return Site3Server.ackflag;
	}
	
	public static void main(String args[]) {
		try {
			Site3Server obj = new Site3Server();
			ThreePCInterface stub = (ThreePCInterface) UnicastRemoteObject.exportObject(obj, 0);
			
			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind("Site3Server", stub);
		
			System.out.println("Site 3 Started !");
		} catch (Exception e) {
		    System.err.println("Server exception: " + e.toString());
		    e.printStackTrace();
		}
	}
}

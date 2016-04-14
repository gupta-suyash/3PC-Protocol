import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
	
public class Site2Server implements ThreePCInterface {
	static boolean reqflag = false;
	static int responseIs = 0, decisionIs = 0, ackflag = 0, transflag = 0;
	static int []leaderVals;
	static int x = 3;
	public Site2Server() {}
	
	public void setTransFlag(int value) {
		Site2Server.transflag = value;
	}

	public int getTransFlag() {
		return Site2Server.transflag;
	}

	public void setLeaderVote(int id, int value) {
		Site2Server.leaderVals[id] = value;
	}

	public int getLeaderVote(int id) {
		return Site2Server.leaderVals[id];
	}

	public void setReqFlag(boolean flag) {
		Site2Server.reqflag = flag;
	}

	public boolean getReqFlag() {
		return Site2Server.reqflag;
	}

	public void setResponse(int response) {
		Site2Server.responseIs = response;
	}

	public int getResponse() {
		return Site2Server.responseIs;
	}

	public void setDecision(int decision) {
		Site2Server.decisionIs = decision;
	}

	public int getDecision() {
		return Site2Server.decisionIs;
	}
	
	public void setVariable(int y) {
		Site2Server.x = y;
	}

	public void incVariable() {
		Site2Server.x++;
	}

	public void setAck(int value) {
		Site2Server.ackflag = value;
	}

	public int getAck() {
		return Site2Server.ackflag;
	}
	
	public static void main(String args[]) {
		try {
			Site2Server obj = new Site2Server();
			ThreePCInterface stub = (ThreePCInterface) UnicastRemoteObject.exportObject(obj, 0);
			
			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind("Site2Server", stub);
		
			String tnodes 	= (args.length < 1) ? "2" : args[0];		// Total Sites
			int total 	= Integer.parseInt(tnodes);
			leaderVals	= new int[total];

			System.out.println("Site 2 Started !");
		} catch (Exception e) {
		    System.err.println("Server exception: " + e.toString());
		    e.printStackTrace();
		}
	}
}

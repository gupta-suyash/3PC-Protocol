import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ThreePCInterface extends Remote {
	void setTransFlag(boolean flag) throws RemoteException;
	boolean getTransFlag() throws RemoteException;
	void incVariable() throws RemoteException;
	void setVariable(int y) throws RemoteException;

	void setReqFlag(boolean flag) throws RemoteException;
	boolean getReqFlag() throws RemoteException;
	void setResponse(int response) throws RemoteException;
	int getResponse() throws RemoteException;
	void setDecision(int decision) throws RemoteException;
	int getDecision() throws RemoteException;
	void setAck(int value) throws RemoteException;
	int getAck() throws RemoteException;
}

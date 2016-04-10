import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

public class Site3Client {
	private Site3Client() {}
	
	public static void main(String[] args) {
		String host = (args.length < 1) ? null : args[0];
		try {
			System.out.println("Site 3");
			Registry registry = LocateRegistry.getRegistry(host);
			ThreePCInterface stub = 
							(ThreePCInterface) registry.lookup("Site3Server");
			
			boolean tflag = false, cflag = false;
			Random rd = new Random();
			while(true) {
				// Recieved transaction
				tflag = stub.getTransFlag();
				//System.out.println(tflag);
				if(tflag) {
					System.out.println("Transaction received at Site3.");
					ThreePCInterface site2 = 
							(ThreePCInterface) registry.lookup("Site2Server");
					site2.setReqFlag(true);

					ThreePCInterface site1 = 
							(ThreePCInterface) registry.lookup("Site1Server");
					site1.setReqFlag(true);
				
					int []responseVector = new int[3];
					boolean s2flag = true, s1flag = true;

					// Own decision.
					int response = rd.nextInt(2);
					if(response == 0) {
						responseVector[0] = -1;	
						System.out.println("Vote-Abort Site3");
					}
					else {
						responseVector[0] = 1;
						System.out.println("Vote-Commit Site3");
					}

					while(s2flag || s1flag) {
						if(s2flag) {
							if(site2.getResponse() != 0) {
								responseVector[1] = site2.getResponse();
								s2flag = false;
							} }
						if(!s1flag) {
						if(site1.getResponse() != 0) {
								responseVector[2] = site1.getResponse();
								s1flag = false;
							} }
					}

					// Global Response
					boolean abort = false;
					for(int i=0; i<3; i++) {
						if(responseVector[i] == -1) { 
							abort = true;
							break;
						} }
					if(abort) {
						System.out.println("Global Abort message to Site1");
						site1.setDecision(-1);
						System.out.println("Global Abort message to Site2");
						site2.setDecision(-1);

						// Wait for acknowledgments.
						s1flag = true; s2flag = true;
						while(s1flag || s2flag) {
							if(s1flag) {
								if(site1.getAck() == -1) {
									s1flag = false;
									System.out.println("Abort Acknowledgment from Site1");
								} }
							if(s2flag) {
								if(site2.getAck() != -1) {
									s2flag = false;
									System.out.println("Abort Acknowledgment from Site2");
								} } }
						System.out.println("Transaction Aborted at Site3");
					}
					else { // Ask to prepare.
						System.out.println("Prepare-to-Commit message to Site1");
						site1.setDecision(1);
						System.out.println("Prepare-to-Commit message to Site2");
						site2.setDecision(1);

						// Wait for Ready-to-Commit.
						s1flag = true; s2flag = true;
						while(s1flag || s2flag) {
							if(s1flag) {
								if(site1.getAck() == 1) {
									s1flag = false;
									System.out.println("Ready-to-Commit message from Site1");
									site1.setAck(0);
								} }
							if(s2flag) {
								if(site2.getAck() != 1) {
									s2flag = false;
									System.out.println("Ready-to-Commit message from Site2");
									site2.setAck(0);
								} } }

						System.out.println("Global-Commit message to Site1");
						site1.setDecision(1);
						System.out.println("Global-Commit message to Site2");
						site2.setDecision(1);

						s1flag = true; s2flag = true;
						while(s1flag || s2flag) {
							if(s1flag) {
								if(site1.getAck() == 1) {
									s1flag = false;
									System.out.println("Commit Acknowledgment from Site1");
									site1.setAck(0);
								} }
							if(s2flag) {
								if(site2.getAck() != 1) {
									s2flag = false;
									System.out.println("Commit Acknowledgement from Site2");
									site2.setAck(0);
								} } }
						System.out.println("Transaction Committed at Site3");
					}
					// Reset the flag, so as to accept further requests.
					stub.setTransFlag(false);
				}
				
				cflag = stub.getReqFlag();
				if(cflag) {	// Recieved Commit/Abort Request
					int response = rd.nextInt(2);
					if(response == 0) {
						response = -1;	
						System.out.println("Vote-Abort by Site3");
					}
					else {
						System.out.println("Vote-Commit by Site3");
					}
					stub.setResponse(response);

					while(stub.getDecision() == 0); // Wait for Decision.
					if(stub.getDecision() < 0) {
						System.out.println("Global-Abort message received at Site3");
						stub.setDecision(0);
						System.out.println("Transaction aborted at Site3");
						stub.setAck(-1);
					}
					else {
						// Prepare Stage
						System.out.println("Prepare-to-Commit message received at Site3");
						stub.setDecision(0);
						System.out.println("Ready-to-Commit message sent by Site3");
						stub.setAck(1);
						
						// Commit Stage
						while(stub.getDecision() == 0); // Wait for Decision.
						stub.setDecision(0);
						System.out.println("Global-Commit message received at Site3");
						stub.setAck(1);
						System.out.println("Transaction committed at Site3");
					}

					// Reset the flag, so as to accept further requests.
					stub.setReqFlag(false);
				}
			}	
		} catch (Exception e) {
		    System.err.println("Client exception: " + e.toString());
		    e.printStackTrace();
		}
	}
}

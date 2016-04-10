import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

public class Site2Client {
	private Site2Client() {}
	
	public static void main(String[] args) {
		String host = (args.length < 1) ? null : args[0];
		try {
			System.out.println("Site 2");
			Registry registry = LocateRegistry.getRegistry(host);
			ThreePCInterface stub = 
							(ThreePCInterface) registry.lookup("Site2Server");
			
			boolean tflag = false, cflag = false;
			Random rd = new Random();
			while(true) {
				// Recieved transaction
				tflag = stub.getTransFlag();
				if(tflag) {
					System.out.println("Transaction received at Site2.");
					ThreePCInterface site1 = 
							(ThreePCInterface) registry.lookup("Site1Server");
					site1.setReqFlag(true);

					ThreePCInterface site3 = 
							(ThreePCInterface) registry.lookup("Site3Server");
					site3.setReqFlag(true);
				
					int []responseVector = new int[3];
					boolean s1flag = true, s3flag = true;

					// Own decision.
					int response = rd.nextInt(2);
					if(response == 0) {
						responseVector[0] = -1;	
						System.out.println("Vote-Abort Site2");
					}
					else {
						responseVector[0] = 1;
						System.out.println("Vote-Commit Site2");
					}
					while(s1flag || s3flag) {
						if(s1flag) {
							if(site1.getResponse() != 0) {
								responseVector[1] = site1.getResponse();
								s1flag = false;
							} }
						if(s3flag) {
							if(site3.getResponse() != 0) {
								responseVector[2] = site3.getResponse();
								s3flag = false;
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
						System.out.println("Global Abort message to Site3");
						site3.setDecision(-1);

						// Wait for acknowledgments.
						s1flag = true; s3flag = true;
						while(s1flag || s3flag) {
							if(s1flag) {
								if(site1.getAck() == -1) {
									s1flag = false;
									System.out.println("Abort Acknowledgment from Site1");
								} }
							if(s3flag) {
								if(site3.getAck() != -1) {
									s3flag = false;
									System.out.println("Abort Acknowledgment from Site3");
								} } }
						System.out.println("Transaction Aborted at Site2");
					}
					else { // Ask to prepare.
						System.out.println("Prepare-to-Commit message to Site1");
						site1.setDecision(1);
						System.out.println("Prepare-to-Commit message to Site3");
						site3.setDecision(1);

						// Wait for Ready-to-Commit.
						s1flag = true; s3flag = true;
						while(s1flag || s3flag) {
							if(s1flag) {
								if(site1.getAck() == 1) {
									s1flag = false;
									System.out.println("Ready-to-Commit message from Site1");
									site1.setAck(0);
								} }
							if(s3flag) {
								if(site3.getAck() != 1) {
									s3flag = false;
									System.out.println("Ready-to-Commit message from Site3");
									site3.setAck(0);
								} } }

						System.out.println("Global-Commit message to Site1");
						site1.setDecision(1);
						System.out.println("Global-Commit message to Site3");
						site3.setDecision(1);

						s1flag = true; s3flag = true;
						while(s1flag || s3flag) {
							if(s1flag) {
								if(site1.getAck() == 1) {
									s1flag = false;
									System.out.println("Commit Acknowledgment from Site1");
									site1.setAck(0);
								} }
							if(s3flag) {
								if(site3.getAck() != 1) {
									s3flag = false;
									System.out.println("Commit Acknowledgement from Site3");
									site3.setAck(0);
								} } }
						System.out.println("Transaction Committed at Site2");
					}
					// Reset the flag, so as to accept further requests.
					stub.setTransFlag(false);
				}
				
				cflag = stub.getReqFlag();
				if(cflag) {	// Recieved Commit/Abort Request
					int response = rd.nextInt(2);
					if(response == 0) {
						response = -1;	
						System.out.println("Vote-Abort by Site2");
					}
					else {
						System.out.println("Vote-Commit by Site2");
					}
					stub.setResponse(response);

					while(stub.getDecision() == 0); // Wait for Decision.
					if(stub.getDecision() < 0) {
						System.out.println("Global-Abort message received at Site2");
						stub.setDecision(0);
						System.out.println("Transaction aborted at Site2");
						stub.setAck(-1);
					}
					else {
						// Prepare Stage
						System.out.println("Prepare-to-Commit message received at Site2");
						stub.setDecision(0);
						System.out.println("Ready-to-Commit message sent by Site2");
						stub.setAck(1);
						
						// Commit Stage
						while(stub.getDecision() == 0); // Wait for Decision.
						stub.setDecision(0);
						System.out.println("Global-Commit message received at Site2");
						stub.setAck(1);
						System.out.println("Transaction committed at Site2");
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

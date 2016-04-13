import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

public class Site1Client {
	private Site1Client() {}

	static void write_decision(ThreePCInterface []siteX, int []sites, int length, int decision, String message) {
		try {
			for(int i=0; i<length; i++) {
				System.out.println(message + " message to Site " + sites[i]);
				siteX[i].setDecision(decision);
			}
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}

	static FailedSitesInfo wait_for_acknowledgement(ThreePCInterface []siteX, int []sites, int total_sites, int decision, String message) {
		boolean sflag[] = new boolean[total_sites];
		FailedSitesInfo fsi = new FailedSitesInfo();
		int failed_sites = total_sites, timeout = 0;

		try {
			while(timeout < 60) {
				for(int i=0; i<total_sites; i++) {
					if(!sflag[i]) {
						if(siteX[i].getAck() == decision) {
							sflag[i] = true;	failed_sites--;
							System.out.println(message + " Acknowledgment from " + sites[i]);
							siteX[i].setAck(0);
						} } }
				timeout++;
			}
			fsi.count = failed_sites;
			fsi.sites = sflag;
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
		return fsi;
	}

	static int coordinator_timeout(ThreePCInterface siteX) {
		int timeout = 0;	int decision = 0;
		try {
			while(timeout < 60 && decision == 0) {
				decision = siteX.getDecision();
				timeout++;
			}
			siteX.setDecision(0);
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
		return decision;
	}

	static void backup_leader_action(boolean abort, ThreePCInterface []siteX, int []sites, int total_sites) {
		FailedSitesInfo fsi = new FailedSitesInfo();
		if(abort) {
			write_decision(siteX, sites, total_sites, -1, "Global-Abort");

			// Wait for acknowledgments.
			fsi = wait_for_acknowledgement(siteX, sites, total_sites, -1, "Abort");
			System.out.println("Transaction Aborted at Site1"); 
		}
		else {	// Prepare-to-Commit
			write_decision(siteX, sites, total_sites, 1, "Global-Commit");

			// Wait for acknowledgement.
			fsi = wait_for_acknowledgement(siteX, sites, total_sites, 1, "Prepare-to-Commit");
			System.out.println("Transaction Committed at Site1");
 		}
	}


	public static void main(String[] args) {
		int i=0, failed_sites = 0;
		String tsites 	= (args.length < 1) ? "2" : args[0];	// Total Sites
		String cord 	= (args.length > 1) ? args[1] : "1";	// Coordinator
		String sid	= (args.length > 2) ? args[2] : "1";	// Site id
		String host 	= (args.length > 3) ? args[3] : null;	// Host

		int site_id	= Integer.parseInt(sid);		// Site id
		int total_sites	= Integer.parseInt(tsites);		// Total number of sites excluding me
		int coordinator = Integer.parseInt(cord);		// Coordinator node
		try {
			System.out.println("Site" + sid);
			Registry registry 	= LocateRegistry.getRegistry(host);
			int []sites		= new int[total_sites];
			ThreePCInterface []siteX = new ThreePCInterface[total_sites];

			ThreePCInterface stub 	= (ThreePCInterface) registry.lookup("Site" + sid + "Server");
			for(i=0; i<total_sites; i++) {
				regserver = "Site" + (i+2) + "Server";
				ThreePCInterface site 	= (ThreePCInterface) registry.lookup(regserver);
				siteX[i] = site;  
			}
			int []responseVector 	= new int[total_sites+1];

			boolean tflag = false, cflag = false;
			int timeout = 0;
			Random rd = new Random();
			/*while(true) {
				// Recieved transaction
				tflag = stub.getTransFlag();
				//System.out.println(tflag);
				if(tflag) {
					System.out.println("Transaction received at Site1.");

					int selfFailure = rd.nextInt(4);	
					if(selfFailure == 0) { // Self Failure
						System.out.println("Alert !!! Coordinator has failed");  
						break;
					}

					// Asking each cohort for a decision.
					for(i=0; i<total_sites; i++) 
						siteX[i].setReqFlag(true);
				
					// Own decision.
					int response = rd.nextInt(2);
					if(response == 0) {
						responseVector[0] = -1;	
						System.out.println("Vote-Abort Site1");
					} else {
						responseVector[0] = 1;
						System.out.println("Vote-Commit Site1");
					}

					// Get decision of other sites 
					FailedSitesInfo fsi = new FailedSitesInfo();
					timeout = 0; failed_sites = total_sites;
					boolean sflag[] = new boolean[total_sites];
					System.out.println("Wait for the decision from the cohorts");
					while(timeout < 60) {
						for(i=0; i<total_sites; i++) {
							if(!sflag[i]) {
								if(siteX[i].getResponse() != 0) {
									responseVector[i+1] = siteX[i].getResponse();
									sflag[i] = true;	failed_sites--;
									System.out.println("Vote received from the Site" + sites[i]);
								} } }
						timeout++;
					}
					if(failed_sites > 1) { // Failure occurred.
						System.out.println("TIMEOUT ! More than one sites have failed. Cannot recover !");
					}
					else if(failed_sites > 0) {
						System.out.println("TIMEOUT ! Single failure occurred !");
						System.out.println("State of all cohorts not clear, so Global-Abort");
						write_decision(siteX, sites, total_sites, -1, "Global-Abort");
						break;
					}
					else {	// No failure
						// Global Response
						boolean abort = false;
						for(i=0; i<total_sites+1; i++) {
							if(responseVector[i] == -1) { 
								abort = true;
								break;
							} }

						selfFailure = rd.nextInt(4);	
						if(selfFailure == 0) { // Self Failure
							System.out.println("Alert !!! Coordinator has failed");  
							break;
						}
						if(abort) {
							write_decision(siteX, sites, total_sites, -1, "Global-Abort");

							// Wait for acknowledgments.
							fsi = wait_for_acknowledgement(siteX, sites, total_sites, -1, "Abort");
							System.out.println("Transaction Aborted at Site1"); 
						}
						else {	// Prepare-to-Commit
							write_decision(siteX, sites, total_sites, 1, "Prepare-to-Commit");

							// Wait for acknowledgement.
							fsi = wait_for_acknowledgement(siteX, sites, total_sites, 1, "Prepare-to-Commit");
							failed_sites = fsi.count; 	sflag = fsi.sites;

							if(failed_sites > 1) // Failure occurred.
								System.out.println("TIMEOUT ! More than one sites have failed. Cannot recover !");
							else if(failed_sites > 0) {
								System.out.println("TIMEOUT ! Single failure occurred !");
								System.out.println("All cohorts were ready to PreCommit, so Global-Commit");  
								write_decision(siteX, sites, total_sites, 1, "Global-Commit");
								break;
							}
							else { // No failure -- Commit
								selfFailure = rd.nextInt(4);	
								if(selfFailure == 0) { // Self Failure
									System.out.println("Alert !!! Coordinator has failed");  
									break;
								}			

								write_decision(siteX, sites, total_sites, 1, "Global-Commit");

								// Wait for acknowledgement.
								fsi = wait_for_acknowledgement(siteX, sites, total_sites, 1, "Commit");
								failed_sites = fsi.count; 	sflag = fsi.sites;
							
								if(failed_sites > 1) // Failure occurred.
									System.out.println("TIMEOUT ! More than one sites have failed.");
								else if(failed_sites > 0) {
									System.out.println("TIMEOUT ! Single failure occurred !");
									System.out.println("All cohorts were ready to Commit, so Global-Commit");  
									write_decision(siteX, sites, total_sites, 1, "Global-Commit");
									break;
								}	
								System.out.println("Transaction Committed at Site1");
							} } }

					// Reset the flag, so as to accept further requests.
					stub.setTransFlag(false);
				}
				else {	// Knows that it is a cohort
					int selfFailure;		
					timeout = 0;	cflag = false;
					while(timeout < 60 && !cflag) {
						cflag = stub.getReqFlag();
						timeout++;
					}
					stub.setReqFlag(false);
					if(!cflag) {	// Coordinator timed out
						System.out.println("Coordinator failed during Initial stage !");
						System.out.println("Transaction Aborted at Site 1");
					}
					else {	// No failure
						selfFailure = rd.nextInt(4);	
						if(selfFailure == 0) { // Self Failure
							System.out.println("Alert !!! Site1 has failed");  
							break;
						}
						
						int response = rd.nextInt(2);
						if(response == 0) {
							response = -1;	
							System.out.println("Vote-Abort by Site1");
						}
						else {
							System.out.println("Vote-Commit by Site1");
						}
						stub.setResponse(response);

						int decision = coordinator_timeout(stub);
						if(decision == 0) { // Coordinator failure
							System.out.println("Coordinator failed during Wait stage !");
							System.out.println("Time to select new leader");	
							coordinator 	= (coordinator + 1) % total_sites;
							boolean leader 	= coordinator == site_id ? true : false;
							if(leader) { 
								backup_leader_action(true, siteX, sites, total_sites);
							} else {
								decision = coordinator_timeout(stub);
								if(decision < 0) {
									System.out.println("Global-Abort message received at Site1");
									stub.setAck(-1);
									System.out.println("Transaction aborted at Site1");
								} }
						}
						else {	// No failure
							if(decision < 0) {
								System.out.println("Global-Abort message received at Site1");

								selfFailure = rd.nextInt(4);	
								if(selfFailure == 0) { // Self Failure
									System.out.println("Alert !!! Site1 has failed");  
									break;
								}

								stub.setAck(-1);
								System.out.println("Transaction aborted at Site1");
							}
							else {
								System.out.println("Prepare-to-Commit message received at Site1");
								
								selfFailure = rd.nextInt(4);	
								if(selfFailure == 0) { // Self Failure
									System.out.println("Alert !!! Site1 has failed");  
									break;
								}

								stub.setAck(1);
								System.out.println("Ready-to-Commit message sent by Site1");
								
								decision = coordinator_timeout(stub);
								if(decision == 0) { // Coordinator failure
									System.out.println("Coordinator failed during Pre-Commit stage");
									System.out.println("Time to select new leader");
									coordinator 	= (coordinator + 1) % total_sites;
									boolean leader 	= coordinator == site_id ? true : false;
									if(leader) { 
										backup_leader_action(false, siteX, sites, total_sites);
									} else {
										decision = coordinator_timeout(stub);
										if(decision > 0) {
											System.out.println("Global-Commit message received at Site1");
											stub.setAck(1);
											System.out.println("Transaction aborted at Site1");
										} }
								}
								else { 
									System.out.println("Global-Commit message received at Site1");		
									
									selfFailure = rd.nextInt(4);	
									if(selfFailure == 0) { // Self Failure
										System.out.println("Alert !!! Site1 has failed");  
										break;
									}

									stub.setAck(1);
									System.out.println("Transaction committed at Site1");
								} } } }
				}
			}*/	
		} catch (Exception e) {
		    System.err.println("Client exception: " + e.toString());
		    e.printStackTrace();
		}
	}
}


class FailedSitesInfo {
	int count;
	boolean []sites;
}

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
			while(timeout < (total_sites*5000)) {
				for(int i=0; i<total_sites; i++) {
					if(!sflag[i]) {
						if(siteX[i].getAck() == decision) {
							sflag[i] = true;	failed_sites--;
							System.out.println(message + " Acknowledgment from Site " + sites[i]);
							siteX[i].setAck(0);
						} } }
				timeout++;
			}
			//System.out.println("Timed out");
			fsi.count = failed_sites;
			fsi.sites = sflag;
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
		return fsi;
	}

	static int coordinator_timeout(ThreePCInterface siteX, int total_sites) {
		int timeout = 0;	int decision = 0;
		try {
			while(timeout < (total_sites*2000) && decision == 0) {
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

	static void backup_leader_action(boolean abort, ThreePCInterface []siteX, int []sites, int total_sites, int sid) {
		FailedSitesInfo fsi = new FailedSitesInfo();
		if(abort) {
			write_decision(siteX, sites, total_sites, -1, "Global-Abort");

			// Wait for acknowledgments.
			fsi = wait_for_acknowledgement(siteX, sites, total_sites, -1, "Abort");
			System.out.println("Transaction Aborted at Site " + sid); 
		}
		else {	// Prepare-to-Commit
			write_decision(siteX, sites, total_sites, 1, "Global-Commit");

			// Wait for acknowledgement.
			fsi = wait_for_acknowledgement(siteX, sites, total_sites, 1, "Prepare-to-Commit");
			System.out.println("Transaction Committed at Site " + sid);
 		}
	}

	static void resetFlags(ThreePCInterface stub) {
		try {
			stub.setTransFlag(0);
			stub.setReqFlag(false);
			stub.setResponse(0);
			stub.setDecision(0);
			stub.setAck(0);
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		int i=0, failed_sites = 0, j=0, original_sites, alias;
		String tsites 	= (args.length < 1) ? "0" : args[0];	// Total Sites
		String cord 	= (args.length > 1) ? args[1] : "1";	// Coordinator
		String sid	= (args.length > 2) ? args[2] : "1";	// Site id
		String host 	= (args.length > 3) ? args[3] : null;	// Host

		int site_id	= Integer.parseInt(sid);		// Site id
		int total_sites	= Integer.parseInt(tsites);		// Total number of sites excluding me
		int coordinator = Integer.parseInt(cord);		// Coordinator node

		// Setting up the aliases
		original_sites	= total_sites+1;
		alias			= site_id;
		try {
			System.out.println("Site" + sid);
			Registry registry 	= LocateRegistry.getRegistry(host);
			int []sites		= new int[total_sites];
			ThreePCInterface []siteX = new ThreePCInterface[total_sites];

			ThreePCInterface stub 	= (ThreePCInterface) registry.lookup("Site" + sid + "Server");
			for(i=1; i<=total_sites+1; i++) {
				if(i != site_id) {
					String regserver 	= "Site" + i + "Server";
					ThreePCInterface site 	= (ThreePCInterface) registry.lookup(regserver);
					siteX[j] 		= site;
					sites[j]		= i;
					j++;
				} }
			int []responseVector 	= new int[total_sites+1];

			boolean cflag = false;
			int timeout = 0, tflag;
			Random rd = new Random();
			while(true) {
				// Recieved transaction
				tflag = stub.getTransFlag();
				if(tflag == 1 && coordinator == site_id) {
					System.out.println("Transaction received at Site " + sid);

					int selfFailure = 1;//rd.nextInt(4);	
					if(selfFailure == 0) { // Self Failure
						System.out.println("Alert !!! Coordinator has failed");  
						stub.setTransFlag(0);		// Resetting the flag
						break;
					}
				
					// Asking each cohort for a decision.
					for(i=0; i<total_sites; i++) {
						siteX[i].setReqFlag(true);	}
				
					// Own decision.
					int response = 1;//rd.nextInt(2);
					if(response == 0) {
						responseVector[0] = -1;	
						System.out.println("Vote-Abort Site " + sid);
					} else {
						responseVector[0] = 1;
						System.out.println("Vote-Commit Site " + sid);
					}

					// Get decision of other sites 
					FailedSitesInfo fsi = new FailedSitesInfo();
					timeout = 0; failed_sites = total_sites;
					boolean sflag[] = new boolean[total_sites];
					System.out.println("Wait for the decision from the cohorts");
					while(timeout < (total_sites*1000)) {
						for(i=0; i<total_sites; i++) {
							if(!sflag[i]) {
								if(siteX[i].getResponse() != 0) {
									responseVector[i+1] = siteX[i].getResponse();
									sflag[i] = true;	failed_sites--;
									System.out.println("Vote received from Site " + sites[i]);
									siteX[i].setResponse(0);
								} } }
						timeout++;
					}
					if(failed_sites > 1) { // Failure occurred.
						System.out.println("TIMEOUT ! More than one sites have failed. Cannot recover !");
					}
					else if(failed_sites > 0) {
						System.out.println("TIMEOUT ! Single failure occurred !");
						System.out.println("State of all cohorts not clear, so Global-Abort");

						ThreePCInterface []stemp 	= new ThreePCInterface[total_sites-1];
						int []rsite			= new int[total_sites-1];
						j = 0;
						for(i=0; i<total_sites; i++) {
							if(sflag[i]) {
								stemp[j] = siteX[i]; 	
								rsite[j] = sites[i];	j++;
							} }
						sites		= rsite;
						siteX		= stemp;
						total_sites--;

						write_decision(siteX, sites, total_sites, -1, "Global-Abort");
					}
					else {	// No failure
						// Global Response
						boolean abort = false;
						for(i=0; i<total_sites+1; i++) {
							if(responseVector[i] == -1) { 
								abort = true;
								break;
							} }
			
						selfFailure = 1;//rd.nextInt(4);	
						if(selfFailure == 0) { // Self Failure
							System.out.println("Alert !!! Coordinator has failed");  
							stub.setTransFlag(0);		// Reset the flag
							break;
						}
					
						if(abort) {
							write_decision(siteX, sites, total_sites, -1, "Global-Abort");

							// Wait for acknowledgments.
							fsi = wait_for_acknowledgement(siteX, sites, total_sites, -1, "Abort");
							System.out.println("Transaction Aborted at Site " + sid); 
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

								ThreePCInterface []stemp 	= new ThreePCInterface[total_sites-1];
								int []rsite			= new int[total_sites-1];
								j = 0;
								for(i=0; i<total_sites; i++) {
									if(sflag[i]) {
										stemp[j] = siteX[i]; 	
										rsite[j] = sites[i];	j++;
									} }
								sites		= rsite;
								siteX		= stemp;
								total_sites--;

								write_decision(siteX, sites, total_sites, 1, "Global-Commit");
							}
							else { // No failure -- Commit
								selfFailure = 0;//rd.nextInt(4);	
								if(selfFailure == 0) { // Self Failure
									System.out.println("Alert !!! Coordinator has failed");  
									resetFlags(stub);
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

									ThreePCInterface []stemp 	= new ThreePCInterface[total_sites-1];
									int []rsite			= new int[total_sites-1];
									j = 0;
									for(i=0; i<total_sites; i++) {
										if(sflag[i]) {
											stemp[j] = siteX[i]; 	
											rsite[j] = sites[i];	j++;
										} }
									sites		= rsite;
									siteX		= stemp;
									total_sites--;

									write_decision(siteX, sites, total_sites, 1, "Global-Commit");
								}	
								System.out.println("Transaction Committed at Site " + sid);
							} 
			
						} }
		
					// Reset the flag, so as to accept further requests.
					stub.setTransFlag(0);
				}
				else if (tflag == 1) {	// Knows that it is a cohort
					System.out.println("Transaction received at Site " + sid);
					int selfFailure;		
					timeout = 0;	cflag = false;
					while(timeout < (total_sites*1000) && !cflag) {
						cflag = stub.getReqFlag();
						timeout++;
					}
					stub.setReqFlag(false);					// RESET reqflag
					if(!cflag) {	// Coordinator timed out
						System.out.println("Coordinator failed during Initial stage !");
						System.out.println("Transaction Aborted at Site "+ sid);
					}
					else {	// No failure
						selfFailure = 1;//rd.nextInt(4);	
						if(selfFailure == 0) { // Self Failure
							System.out.println("Alert !!! Site " + site_id + " has failed");  
							resetFlags(stub);
							break;
						}
						
						int response = 1;//rd.nextInt(2);
						if(response == 0) {
							response = -1;	
							System.out.println("Vote-Abort by Site " + sid);
						}
						else {
							System.out.println("Vote-Commit by Site " + sid);
						}
						stub.setResponse(response);

						int decision = coordinator_timeout(stub, total_sites);
						if(decision == 0) { // Coordinator failure
							System.out.println("Coordinator failed during Wait stage !");
							System.out.println("Time to select new leader");	

							ThreePCInterface []stemp 	= new ThreePCInterface[total_sites-1];
							int []rsite			= new int[total_sites-1];
							j = 0;
							for(i=0; i<total_sites; i++) {
								if(sites[i] != coordinator) {
									stemp[j] = siteX[i]; 	
									rsite[j] = sites[i];	j++;
								} }
							sites		= rsite;
							siteX		= stemp;
							total_sites--;

							// Send the alias to all other participants.
							for(i=0; i<total_sites; i++)  {
								siteX[i].setLeaderVote((site_id-1), alias);
								//System.out.println("Written: " + siteX[i].getLeaderVote(site_id-1) + " : alias: " + alias);
							}

							boolean []sflag = new boolean[original_sites];
							int max = alias, tmpvar = 0;	coordinator = site_id;
							while(timeout < (total_sites*2000)) {
								for(i=0; i<original_sites; i++) {
									if(!sflag[i]) {
										tmpvar = stub.getLeaderVote(i);
										//System.out.println("tmpvar: " + tmpvar);
										if(tmpvar != 0) {
											//System.out.println("tmpvar: " + tmpvar);
											sflag[i]	= true;
											if(tmpvar > max) {
												max 		= tmpvar;
												coordinator	= i+1;
											} 
											stub.setLeaderVote(i, 0);
										} } }
								timeout++;
							}

							//leader_id 	= ((leader_id + 1) % (total_sites+1));
							//leader_id	= leader_id == 0 ? (total_sites + 1) : leader_id;	// New coordinator
							boolean leader 	= coordinator == site_id ? true : false;		// Is a leader ?

							if(leader) { 
								System.out.println("I am the new leader");
								// Wait for leader acknowledgments.
								FailedSitesInfo fsi = wait_for_acknowledgement(siteX, sites, total_sites, 2, "Leader");
								backup_leader_action(true, siteX, sites, total_sites, site_id);
							} else {
								stub.setAck(2);		// Acknowledgement for new leader
								decision = coordinator_timeout(stub, total_sites);
								if(decision < 0) {
									System.out.println("Global-Abort message received at Site " + sid);
									stub.setAck(-1);
									System.out.println("Transaction aborted at Site " + sid);
								} }
						}
						else {	// No failure
							if(decision < 0) {
								System.out.println("Global-Abort message received at Site " + sid);
			
								selfFailure = rd.nextInt(4);	
								if(selfFailure == 0) { // Self Failure
									System.out.println("Alert !!! Site1 has failed");  
									resetFlags(stub);
									break;
								}
						
								stub.setAck(-1);
								System.out.println("Transaction aborted at Site " + sid);
							}
							else {
								System.out.println("Prepare-to-Commit message received at Site " + sid);
								
								selfFailure = 1;//rd.nextInt(4);	
								if(selfFailure == 0) { // Self Failure
									System.out.println("Alert !!! Site " + site_id + " has failed");  
									resetFlags(stub);
									break;
								}
						
								stub.setAck(1);
								System.out.println("Ready-to-Commit message sent by Site " + sid);
								
								decision = coordinator_timeout(stub, total_sites);
								if(decision == 0) { // Coordinator failure
									System.out.println("Coordinator failed during Pre-Commit stage");
									System.out.println("Time to select new leader");

									ThreePCInterface []stemp 	= new ThreePCInterface[total_sites-1];
									int []rsite			= new int[total_sites-1];
									j = 0;
									for(i=0; i<total_sites; i++) {
										if(sites[i] != coordinator) {
											stemp[j] = siteX[i]; 	
											rsite[j] = sites[i];
											j++;
										} }
									sites		= rsite;
									siteX		= stemp;
									total_sites--;

									// Send the alias to all other participants.
									for(i=0; i<total_sites; i++)  {
										siteX[i].setLeaderVote((site_id-1), alias);
										//System.out.println("Written: " + siteX[i].getLeaderVote(site_id-1) + " : alias: " + alias);
									}

									boolean []sflag = new boolean[original_sites];
									int max = alias, tmpvar = 0;	coordinator = site_id;
									while(timeout < (total_sites*2000)) {
										for(i=0; i<original_sites; i++) {
											if(!sflag[i]) {
												tmpvar = stub.getLeaderVote(i);
												//System.out.println("tmpvar: " + tmpvar);
												if(tmpvar != 0) {
													//System.out.println("tmpvar: " + tmpvar);
													sflag[i]	= true;
													if(tmpvar > max) {
														max 		= tmpvar;
														coordinator	= i+1;
													} 
													stub.setLeaderVote(i, 0);
												} } }
										timeout++;
									}
									boolean leader 	= coordinator == site_id ? true : false;		// Is a leader ?

									if(leader) { 
										System.out.println("I am the new leader");
										// Wait for leader acknowledgments.
										FailedSitesInfo fsi = wait_for_acknowledgement(siteX, sites, total_sites, 2, "Leader");
										backup_leader_action(false, siteX, sites, total_sites, site_id);
									} else {
										stub.setAck(2);		// Acknowledgement for new leader	
										decision = coordinator_timeout(stub, total_sites);
										if(decision > 0) {
											System.out.println("Global-Commit message received at Site " + sid);
											stub.setAck(1);
											System.out.println("Transaction committed at Site " + sid);
										} }
								}
								else { 
									System.out.println("Global-Commit message received at Site " + sid);		
									
									selfFailure = 1;//rd.nextInt(4);	site_id == 2 ? 0 : 1;
									if(selfFailure == 0) { // Self Failure
										System.out.println("Alert !!! Site " + site_id + " has failed");  
										resetFlags(stub);
										break;
									}
						
									stub.setAck(1);
									System.out.println("Transaction committed at Site " + sid);
								} 
			
							} }	 
					}
					// Reset the flag, so as to accept further requests.
					resetFlags(stub);
				}
			}	
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

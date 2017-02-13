import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class test {
	
	// This class represents the rating data of ONLY ONE user.
	private static class userRatings {
		private int usrid;
		private int[] movies;
		private float[] ratings;
		private int[] timestamps; // change to time?
		private String filepath;
		
		private int movieofinterest;
		private float actualrating;
		private float cavg;
		private float[] averages;
		private int[] numofratings;
		private float mavg;
		
		public userRatings(int usrid, String filepath) throws IOException {
			this.usrid = usrid;
			this.filepath = filepath;
			ArrayList<String[]> ratings1 = new ArrayList<>();
			Reader in = new FileReader(filepath);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
			int i=0;
			for (CSVRecord record : records) {
			    String column1 = record.get(0);
			    if (i>0) {
			    	int usr = Integer.parseInt(column1);
			    	if (usr == usrid) {
			    		String[] rs = {record.get(1), record.get(2), record.get(3)};
			    		ratings1.add(rs);
			    	} else if (usr > usrid) {
			    		break;			// Once we get all ratings from this user, break.
			    	}
			    }
			    i++;
			}
			movies = new int[ratings1.size()];
			timestamps = new int[ratings1.size()];
			ratings = new float[ratings1.size()];
			averages = new float[ratings1.size()];
			numofratings = new int[ratings1.size()];
			for (int j=0; j < ratings1.size(); j++) {
				movies[j] = Integer.parseInt(ratings1.get(j)[0]);
				timestamps[j] = Integer.parseInt(ratings1.get(j)[2]);
				ratings[j] = Float.parseFloat(ratings1.get(j)[1]);
			}
			in.close();
			fillAverages();
			changeto(0);
		}
		
		private void fillAverages() throws IOException {
			Reader in = new FileReader(filepath);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
			int i=0;
			for (CSVRecord record : records) {
			    String column = record.get(1);
			    if (i>0) {
			    	int mov = Integer.parseInt(column);
			    	for (int j=0; j < movies.length; j++) {
			    		if (mov==movies[j]) {
			    			numofratings[j]++;
			    			averages[j] += Float.parseFloat(record.get(2));
			    			break;
			    		}
			    	}
			    }
			    i++;
			}
			for (int j=0; j < movies.length; j++) {
				averages[j] /= numofratings[j];
			}
			in.close();
		}
		
		private void changeto(int n) {
			movieofinterest = movies[n];
			actualrating = ratings[n];
			cavg = (float) 0.0;
			for (int j=0; j < ratings.length; j++) {
				if (j != n) {
					cavg += ratings[j];
				}
			}
			cavg /= (ratings.length -1);
			mavg = averages[n];
		}
		
		// Print all rating data of this user.
		public void prt() {
			System.out.println(usrid);
			for(int i = 0; i < movies.length; i++) {
				System.out.println(movies[i] + " " + ratings[i] + " " + timestamps[i]);
			}
		}
		
		// Help class to sample similar ratings of two users.
		private static class corating {
			public corating(int mid, float r1, float r2, int t1, float t2) {
				super();
				this.mid = mid;
				this.r1 = r1;
				this.r2 = r2;
				this.t1 = t1;
				this.t2 = t2;
			}
			public int mid;
			public float r1;
			public float r2;
			public int t1;
			public float t2;
		}
		
		private static class simildata implements Comparable<simildata> {
			public simildata(float simil, float avg, float rating, float ra1, float ra2) {
				//super();
				this.simil = simil;
				this.avg = avg;
				this.rating = rating;
				this.ra1 = ra1;
				this.ra2 = ra2;
			}
			public float simil;
			public float avg;
			public float rating;
			public float ra1;
			public float ra2;
			
			public int compareTo(simildata s) {
				if (this.simil < s.simil) {return 1;} else if (this.simil == s.simil) {return 0;} else {return -1;}
			}
			
		}
		
		private simildata measure(ArrayList<corating> cors, ArrayList<Float> allratings) {
			float avg = (float) 0.0;
			for(float f: allratings) {
				avg += f;
			}
			avg /= allratings.size();
			float ra = 0;
			
			// Not a real similarity measure, change later.
			float ra1 = (float) 0.0;
			float ra2 = (float) 0.0;
						
			for (int j = 0; j < cors.size(); j++) {
				if (cors.get(j).mid == movieofinterest) {
					ra = cors.get(j).r2;
				} else {
				ra1 += cors.get(j).r1;
				ra2 += cors.get(j).r2;
				}
			}
			ra1 /= (cors.size()-1);
			ra2 /= (cors.size()-1);
			
			/*for (int j = 0; j < cors.size(); j++) {
				if (cors.get(j).mid == movieofinterest) {
					//
				} else {
					float a1 = (float) (cors.get(j).r1/ra1);
					float a2 = (float) (cors.get(j).r2/ra2);
				ex1 -= a1*(Math.log(a2)/Math.log(2.0));
				}
			}*/
			return new simildata((float) (5.0-Math.abs(ra1-ra2)), avg, ra, ra1, ra2);
		}
		
		// A really simple similarity measure, only calculates differences in average ratings of two users and prints results.
		// Of course later we will create a real similarity measure by slightly modifying the code.
		private void measure() throws IOException {
			
			Reader in = new FileReader(filepath);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
			int i=0;
			int currentuser = 0;
			ArrayList<corating> coratings = new ArrayList<>();
			ArrayList<simildata> similarity = new ArrayList<>();
			ArrayList<Float> uratings = new ArrayList<>();
			int start = 0;
			boolean include = false;
			for (CSVRecord record : records) {
			    String column1 = record.get(0);
			    if (i>0) {
			    	int usr = Integer.parseInt(column1);
			    	if (usr != usrid) {
			    		if (usr != currentuser) {
			    			// coratings includes all the common ratings of the two users.
			    			if (!include || coratings.size() < 2) { // do nothing.
			    			} else {
			    				similarity.add(measure(coratings, uratings));
			    			}
			    			// Initialize similarity data for next user.
			    			currentuser = usr;
			    			start = 0;
			    			coratings = new ArrayList<>();
			    			uratings = new ArrayList<>();
			    			include = false;
			    		} else {
			    			// See if both users have seen same movie.
			    			int mov = Integer.parseInt(record.get(1));
			    			float r = Float.parseFloat(record.get(2));
			    			uratings.add(r);
			    			for (int j = start; j < movies.length; j++) {
			    				int m = movies[j];
			    				if (m==mov) { // If they have, add their ratings to coratings.
			    					coratings.add(new corating(m, ratings[j], r, timestamps[j], Integer.parseInt(record.get(3))));
			    					if (m==movieofinterest) {include = true;}
			    				}
			    				if (m > mov) {
			    					start = j;
			    					break;
			    				}
			    			}
			    		}
			    	}
			    }
			    i++;
			}
			in.close();
			Collections.sort(similarity);
			
			float k = (float) 0.0;
			float avg = (float) 0.0;
			float weighted = (float) 0.0;
			float normw = (float) 0.0;
			simildata sd;
			float avgsimil = (float) 0.0;
			System.out.println(movieofinterest);
			int step = Math.floorDiv(similarity.size(), 20);
			for (int j=0; j < 20; j++) {
				for (int h = j*step; h < step*(j+1); h++) {
					sd = similarity.get(h);
					avg += sd.rating;
					weighted += sd.rating*sd.simil;
					normw += (sd.rating-sd.avg)*sd.simil;
					k += Math.abs(sd.simil);
					avgsimil += sd.simil;
				}
				System.out.println(j + "   avg = " + avg/(step*(j+1)) + "   weighted sum = " + weighted/k + "   normalised weighted = " + (cavg+(normw/k)) + "   average similarity = " + avgsimil/(step*(j+1)));
			}
			System.out.println("Actual = " + actualrating + "   Total avg = " + mavg + " Users = " + similarity.size());
			System.out.println();
		}
		
		public void simulate() throws IOException {
			measure();
			for (int i=1; i < 8; i++) {
				changeto(2*i);
				measure();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		// User 1234 will be the reference in this test example.
		userRatings urs = new userRatings(1234, "ml-20m/ratings.csv");
		urs.prt();
		urs.simulate();
	}

}

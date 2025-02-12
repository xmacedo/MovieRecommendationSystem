package br.com.xmacedo;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieRecommender {
    private static final String FOLDER_TO_GET_YAML_FILES = "data-set/";

    public static void main(String[] args) {
        try {


            //Step 1: Load the Datasets
            Map<Long, String> movieTitles = loadMovieTitles();

            //Step 2: Clean the Movies Metadata
            // Read the CSV file manually and convert the userId to integers
            Map<Integer, List<GenericPreference>> userPreferences = loadUserPreferences();

            // Build the preference map to loading on model
            FastByIDMap<PreferenceArray> preferenceMap = buildPreferenceMapToModel(userPreferences);

            // Create a model based on UserPreference
            DataModel model = new GenericDataModel(preferenceMap);

            //Step 3: Merge Datasets
            //Build a Recommendation based on Similarity
            List<RecommendedItem> recommendations =  buildRecommendationBySimilarity(model);

            // Show the recommendations
            for (RecommendedItem recommendation : recommendations) {
                String movieTitle = movieTitles.getOrDefault(recommendation.getItemID(), "Título Desconhecido");
                System.out.println("Filme: " + movieTitle + " (ID: " + recommendation.getItemID() + ") - Score: " + recommendation.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<RecommendedItem> buildRecommendationBySimilarity(DataModel model) throws TasteException {
        // Create a similarity model between users
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);

        // Set the number of nearest neighbors
        UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, similarity, model);

        // Create the user-based recommender
        Recommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

        // Get recommendations for a specific user (e.g. user ID 1)
        return recommender.recommend(1, 5);
    }

    private static FastByIDMap<PreferenceArray> buildPreferenceMapToModel(Map<Integer, List<GenericPreference>> userPreferences) {
        FastByIDMap<PreferenceArray> preferenceMap = new FastByIDMap<>();
        for (Map.Entry<Integer, List<GenericPreference>> entry : userPreferences.entrySet()) {
            preferenceMap.put(entry.getKey(), new GenericUserPreferenceArray(entry.getValue()));
        }
        return preferenceMap;
    }

    private static Map<Integer, List<GenericPreference>> loadUserPreferences() throws IOException {
        Map<Integer, List<GenericPreference>> userPreferences = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(FOLDER_TO_GET_YAML_FILES + "ratings.csv"));
        String line;
        br.readLine(); // Skip header
        Map<String, Integer> userIdMap = new HashMap<>();
        int userCounter = 0;

        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            String userIdStr = values[0];
            long movieId = Long.parseLong(values[1]);
            float rating = Float.parseFloat(values[2]);

            // Map userId
            if (!userIdMap.containsKey(userIdStr)) {
                userIdMap.put(userIdStr, ++userCounter);
            }
            int userId = userIdMap.get(userIdStr);

            // Added preferences to the User
            userPreferences.putIfAbsent(userId, new ArrayList<>());
            userPreferences.get(userId).add(new GenericPreference(userId, movieId, rating));
        }
        br.close();

        return userPreferences;
    }

    private static Map<Long, String> loadMovieTitles() throws IOException {
        Map<Long, String> movieTitles = new HashMap<>();

        // Load  movie titles from movies_metadata.csv
        BufferedReader moviesReader = new BufferedReader(new FileReader(FOLDER_TO_GET_YAML_FILES + "movies_metadata.csv"));
        moviesReader.readLine(); // Skip header
        String movieLine;
        while ((movieLine = moviesReader.readLine()) != null) {
            String[] movieValues = movieLine.split(",");
            try {
                long movieId = Long.parseLong(movieValues[5]); // Movie ID is in the 6th column (index 5)
                String title = movieValues[20]; // Movie title is in the 21st column (index 20)
                movieTitles.put(movieId, title);
            } catch (Exception ignored) {}
        }
        moviesReader.close();

        return movieTitles;
    }
}



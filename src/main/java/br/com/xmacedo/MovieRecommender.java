package br.com.xmacedo;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieRecommender {
    private static final String FOLDER_TO_GET_YAML_FILES = "src/main/resources/data-set/";
    public static void main(String[] args) {
        try {
            Map<String, Integer> userIdMap = new HashMap<>();
            int userCounter = 0;
            Map<Integer, List<GenericPreference>> userPreferences = new HashMap<>();
            Map<Long, String> movieTitles = new HashMap<>(); // Mapa para armazenar os títulos dos filmes

            // Carregar os títulos dos filmes a partir do movies_metadata.csv
            BufferedReader moviesReader = new BufferedReader(new FileReader(FOLDER_TO_GET_YAML_FILES + "movies_metadata.csv"));
            moviesReader.readLine(); // Pular cabeçalho
            String movieLine;
            while ((movieLine = moviesReader.readLine()) != null) {
                String[] movieValues = movieLine.split(",");
                try {
                    long movieId = Long.parseLong(movieValues[5]); // ID do filme está na 6ª coluna (index 5)
                    String title = movieValues[20]; // Título do filme está na 21ª coluna (index 20)
                    movieTitles.put(movieId, title);
                } catch (Exception ignored) {}
            }
            moviesReader.close();



            // Ler o arquivo CSV manualmente e converter os userId para inteiros
            BufferedReader br = new BufferedReader(new FileReader(FOLDER_TO_GET_YAML_FILES + "ratings.csv"));
            String line;
            br.readLine(); // Pular cabeçalho
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String userIdStr = values[0];
                long movieId = Long.parseLong(values[1]);
                float rating = Float.parseFloat(values[2]);

                // Mapear userId
                if (!userIdMap.containsKey(userIdStr)) {
                    userIdMap.put(userIdStr, ++userCounter);
                }
                int userId = userIdMap.get(userIdStr);

                // Adicionar preferências ao usuário
                userPreferences.putIfAbsent(userId, new ArrayList<>());
                userPreferences.get(userId).add(new GenericPreference(userId, movieId, rating));
            }
            br.close();

            FastByIDMap<PreferenceArray> preferenceMap = new FastByIDMap<>();
            for (Map.Entry<Integer, List<GenericPreference>> entry : userPreferences.entrySet()) {
                preferenceMap.put(entry.getKey(), new GenericUserPreferenceArray(entry.getValue()));
            }

            // Criar um modelo de dados
            DataModel model = new GenericDataModel(preferenceMap);
            // Criar um modelo de dados
//            DataModel model = new GenericDataModel((DataModel) Collections.singletonList(
//                    new GenericUserPreferenceArray(preferences)));
            // Carregar os dados do ratings.csv
            //DataModel model = new FileDataModel(new File(FOLDER_TO_GET_YAML_FILES + "ratings.csv"));

            // Criar um modelo de similaridade entre usuários
            UserSimilarity similarity = new PearsonCorrelationSimilarity(model);

            // Definir o número de vizinhos mais próximos
            UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, similarity, model);

            // Criar o recomendador baseado em usuários
            Recommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

            // Obter recomendações para um usuário específico (ex: usuário ID 1)
            List<RecommendedItem> recommendations = recommender.recommend(1, 5);

            // Exibir recomendações
            for (RecommendedItem recommendation : recommendations) {
                String movieTitle = movieTitles.getOrDefault(recommendation.getItemID(), "Título Desconhecido");
                System.out.println("Filme: " + movieTitle + " (ID: " + recommendation.getItemID() + ") - Score: " + recommendation.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



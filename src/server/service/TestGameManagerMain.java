package server.service;

import common.model.Game;
import common.model.GameTemplate;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Test di isolamento per verificare il costruttore di GameManager, l'inizializzazione
 * della prima partita attiva e la rotazione manuale tramite startNewActiveGame().
 */
public class TestGameManagerMain {

    public static void main(String[] args) {
        System.out.println("=== TEST ISOLAMENTO: GameManager (Costruttore e startNewActiveGame) ===");

        String testRepoPath = "data/test_game_manager.json";
        File testFile = new File(testRepoPath);
        if (testFile.exists()) {
            testFile.delete();
        }

        try {
            // 1. Caricamento dei template reali dal file JSON
            GameTemplateLoader templateLoader = new GameTemplateLoader();
            Map<Integer, GameTemplate> templates = templateLoader.loadTemplates("Connections_Data.json");
            System.out.println("[OK] Template caricati con successo: " + templates.size());

            // 2. Creazione repository su file isolato e istanziazione di GameManager
            GameRepository gameRepo = new GameRepository(testRepoPath);
            long roundDuration = 60000L; // 60 secondi
            GameManager gameManager = new GameManager(templates, gameRepo, roundDuration);

            // 3. Verifica stato iniziale (Round 1)
            int id1 = gameManager.getCurrentGameId();
            Game game1 = gameManager.getActiveGame();

            boolean id1Ok = (id1 == 1);
            boolean game1NotNull = (game1 != null);
            boolean game1WordsOk = (game1NotNull && game1.getShuffledWords().size() == 16);

            System.out.println("[TEST] ID iniziale == 1: " + (id1Ok ? "OK" : "FALLITO (valore: " + id1 + ")"));
            System.out.println("[TEST] Partita attiva 1 presente e con 16 parole: " + (game1WordsOk ? "OK" : "FALLITO"));

            // 4. Esecuzione rotazione manuale (Round 2)
            gameManager.startNewActiveGame();
            int id2 = gameManager.getCurrentGameId();
            Game game2 = gameManager.getActiveGame();

            boolean id2Ok = (id2 == 2);
            boolean game2NotNull = (game2 != null);
            boolean distinctInstancesOk = (game1 != game2);
            boolean game2WordsOk = (game2NotNull && game2.getShuffledWords().size() == 16);

            System.out.println("[TEST] ID successivo == 2: " + (id2Ok ? "OK" : "FALLITO (valore: " + id2 + ")"));
            System.out.println("[TEST] Nuova istanza Game distinta dalla precedente: " + (distinctInstancesOk ? "OK" : "FALLITO"));
            System.out.println("[TEST] Partita attiva 2 con 16 parole: " + (game2WordsOk ? "OK" : "FALLITO"));

            if (id1Ok && game1WordsOk && id2Ok && distinctInstancesOk && game2WordsOk) {
                System.out.println("\n[SUCCESSO] Test di isolamento di GameManager SUPERATO!");
            } else {
                System.err.println("\n[FALLITO] Verifiche di isolamento non superate.");
            }

        } catch (IOException e) {
            System.err.println("[ECCEZIONE] Errore di caricamento o I/O: " + e.getMessage());
        } finally {
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }
}
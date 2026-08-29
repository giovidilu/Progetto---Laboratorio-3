package server;

import common.model.GameOutcome;
import common.model.GameRecord;
import common.model.PlayerGameState;
import common.model.WordGroup;
import server.repository.GameRepository;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test di regressione per verificare la corretta serializzazione e deserializzazione
 * JSON (round-trip) della classe GameRecord con campi complessi annidati.
 */
public class TestGameRepositoryMain {

    public static void main(String[] args) {
        String testFilePath = "data/test_games_record.json";
        File testFile = new File(testFilePath);
        if (testFile.exists()) {
            testFile.delete();
        }

        try {
            System.out.println("=== TEST REGRESSIONE PERSISTENZA: GameRecord ===");

            // 1. Inizializzazione Repository
            GameRepository repoToSave = new GameRepository(testFilePath);

            // 2. Creazione dei gruppi di parole corretti
            WordGroup g1 = new WordGroup("STRIPED ANIMALS", Arrays.asList("CLOWNFISH", "HONEYBEE", "TIGER", "ZEBRA"));
            WordGroup g2 = new WordGroup("BEAR IN MIND", Arrays.asList("CONSIDER", "COUNT", "FACTOR", "INCLUDE"));
            WordGroup g3 = new WordGroup("ASSOCIATED WITH RAINBOWS", Arrays.asList("DOROTHY GALE", "LEPRECHAUN", "PRIDE", "UNICORN"));
            WordGroup g4 = new WordGroup("BEGINNING WITH NUMBER HOMOPHONES", Arrays.asList("FIEVEL", "FOREHEAD", "TOUCAN", "WONDER"));
            List<WordGroup> allGroups = Arrays.asList(g1, g2, g3, g4);

            // 3. Creazione degli stati individuali dei giocatori
            Map<String, PlayerGameState> playerStates = new HashMap<>();

            // Giocatore 1: 3 gruppi indovinati -> WON (+18 punti)
            PlayerGameState p1 = new PlayerGameState("giovanni", 1);
            p1.addCorrectGroup(g1);
            p1.addCorrectGroup(g2);
            p1.addCorrectGroup(g3);
            playerStates.put(p1.getUsername(), p1);

            // Giocatore 2: 4 errori -> LOST_BY_MISTAKES (-16 punti)
            PlayerGameState p2 = new PlayerGameState("mario", 1);
            p2.incrementMistakes();
            p2.incrementMistakes();
            p2.incrementMistakes();
            p2.incrementMistakes();
            playerStates.put(p2.getUsername(), p2);

            // 4. Costruzione del GameRecord esteso
            int totalParticipants = 2;
            int participantsFinished = 2;
            int participantsWon = 1;
            double averageScore = (18.0 - 16.0) / 2.0; // 1.0

            GameRecord originalRecord = new GameRecord(
                1,
                totalParticipants,
                participantsFinished,
                participantsWon,
                averageScore,
                allGroups,
                playerStates
            );

            // 5. Salvataggio su file JSON tramite GameRepository
            repoToSave.addGameRecord(originalRecord);
            repoToSave.saveToDisk();
            System.out.println("[OK] Salvataggio su disco completato.");

            // 6. Ricaricamento da disco su una nuova istanza isolata
            GameRepository repoToLoad = new GameRepository(testFilePath);
            repoToLoad.loadFromDisk();
            GameRecord loadedRecord = repoToLoad.getGameRecord(1);

            if (loadedRecord == null) {
                System.err.println("[ERRORE] Il record con ID 1 non è stato caricato da disco.");
                return;
            }

            // 7. Verifiche di integrità puntuali sui dati deserializzati
            boolean idOk = loadedRecord.getGameId() == 1;
            boolean statsOk = loadedRecord.getTotalParticipants() == 2
                    && loadedRecord.getParticipantsFinished() == 2
                    && loadedRecord.getParticipantsWon() == 1
                    && Math.abs(loadedRecord.getAverageScore() - 1.0) < 0.001;

            boolean groupsCountOk = loadedRecord.getAllGroups() != null && loadedRecord.getAllGroups().size() == 4;
            boolean firstGroupWordsOk = groupsCountOk 
                    && loadedRecord.getAllGroups().get(0).getWords().equals(Arrays.asList("CLOWNFISH", "HONEYBEE", "TIGER", "ZEBRA"));

            boolean playersCountOk = loadedRecord.getPlayerStates() != null && loadedRecord.getPlayerStates().size() == 2;

            PlayerGameState loadedP1 = playersCountOk ? loadedRecord.getPlayerStates().get("giovanni") : null;
            boolean p1Ok = loadedP1 != null
                    && "giovanni".equals(loadedP1.getUsername())
                    && loadedP1.getCorrectGroups().size() == 3
                    && loadedP1.getMistakes() == 0
                    && loadedP1.getScore() == 18
                    && loadedP1.getOutcome() == GameOutcome.WON;

            PlayerGameState loadedP2 = playersCountOk ? loadedRecord.getPlayerStates().get("mario") : null;
            boolean p2Ok = loadedP2 != null
                    && "mario".equals(loadedP2.getUsername())
                    && loadedP2.getCorrectGroups().isEmpty()
                    && loadedP2.getMistakes() == 4
                    && loadedP2.getScore() == -16
                    && loadedP2.getOutcome() == GameOutcome.LOST_BY_MISTAKES;

            // Report dei controlli
            System.out.println("[TEST] Identificativo e statistiche aggregate: " + (idOk && statsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST] Integrità 4 WordGroup annidati: " + (groupsCountOk && firstGroupWordsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST] PlayerGameState 'giovanni' (+18, WON, 3 gruppi): " + (p1Ok ? "OK" : "FALLITO"));
            System.out.println("[TEST] PlayerGameState 'mario' (-16, LOST_BY_MISTAKES, 4 errori): " + (p2Ok ? "OK" : "FALLITO"));

            if (idOk && statsOk && groupsCountOk && firstGroupWordsOk && playersCountOk && p1Ok && p2Ok) {
                System.out.println("\n[SUCCESSO] Regressione superata: Gson deserializza correttamente l'intera struttura annidata di GameRecord.");
            } else {
                System.err.println("\n[FALLITO] Discrepanza riscontrata nei dati ricaricati.");
            }

        } catch (IOException e) {
            System.err.println("[ECCEZIONE] Errore di I/O durante il test: " + e.getMessage());
        } finally {
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }
}
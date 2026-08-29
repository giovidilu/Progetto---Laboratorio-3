package server.service;

import common.model.GameTemplate;
import common.protocol.response.GameState;
import common.protocol.response.payload.GameInfoPayload;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Test di isolamento per verificare il comportamento di getGameInfoForPlayer
 * nei casi di consultazione in sola lettura della partita attiva e su ID inesistenti.
 */
public class TestGameManagerMain {

    public static void main(String[] args) {
        System.out.println("=== TEST ISOLAMENTO: GameManager (getGameInfoForPlayer - Casi A e B) ===");

        String testRepoPath = "data/test_game_manager_info.json";
        File testFile = new File(testRepoPath);
        if (testFile.exists()) {
            testFile.delete();
        }

        try {
            // 1. Caricamento dei template
            GameTemplateLoader templateLoader = new GameTemplateLoader();
            Map<Integer, GameTemplate> templates = templateLoader.loadTemplates("Connections_Data.json");
            System.out.println("[OK] Template caricati in memoria: " + templates.size());

            // 2. Inizializzazione Repository e GameManager
            GameRepository gameRepo = new GameRepository(testRepoPath);
            long roundDuration = 60000L; // 60 secondi
            GameManager gameManager = new GameManager(templates, gameRepo, roundDuration);

            // ==========================================
            // CASO A: Giocatore nuovo su partita attiva (gameId == null e gameId == 1)
            // ==========================================
            GameInfoPayload infoNullId = gameManager.getGameInfoForPlayer("alice", null);
            GameInfoPayload infoExplicitId = gameManager.getGameInfoForPlayer("alice", 1);

            boolean casoANotNull = (infoNullId != null && infoExplicitId != null);
            boolean casoAStateOk = casoANotNull 
                    && infoNullId.getState() == GameState.ONGOING 
                    && infoExplicitId.getState() == GameState.ONGOING;
            boolean casoAScoreErrorsOk = casoANotNull 
                    && infoNullId.getErrors() == 0 
                    && infoNullId.getScore() == 0;
            boolean casoATimeOk = casoANotNull 
                    && infoNullId.getTimeRemaining() > 0 
                    && infoNullId.getTimeRemaining() <= 60000;
            boolean casoAWordsOk = casoANotNull 
                    && infoNullId.getWords() != null 
                    && infoNullId.getWords().size() == 16;
            boolean casoAGroupsOk = casoANotNull 
                    && infoNullId.getCorrectGroups() != null 
                    && infoNullId.getCorrectGroups().isEmpty();
            boolean casoAHiddenOk = casoANotNull 
                    && infoNullId.getFinalAllocation() == null 
                    && infoNullId.getNumberCorrectGroups() == null;

            boolean casoAComplessivo = casoANotNull && casoAStateOk && casoAScoreErrorsOk 
                    && casoATimeOk && casoAWordsOk && casoAGroupsOk && casoAHiddenOk;

            System.out.println("\n[CASO A] Partita attiva per utente senza mosse:");
            System.out.println(" - Payload non nullo (ID implicito ed esplicito): " + (casoANotNull ? "OK" : "FALLITO"));
            System.out.println(" - Stato ONGOING: " + (casoAStateOk ? "OK" : "FALLITO"));
            System.out.println(" - Errori (0) e Punteggio (0): " + (casoAScoreErrorsOk ? "OK" : "FALLITO"));
            System.out.println(" - Tempo residuo coerente (>0 e <=60s): " + (casoATimeOk ? "OK" : "FALLITO"));
            System.out.println(" - 16 parole mescolate presenti: " + (casoAWordsOk ? "OK" : "FALLITO"));
            System.out.println(" - Gruppi corretti lista vuota: " + (casoAGroupsOk ? "OK" : "FALLITO"));
            System.out.println(" - Soluzioni finali nascoste (null): " + (casoAHiddenOk ? "OK" : "FALLITO"));

            // ==========================================
            // CASO B: Richiesta partita inesistente (ID non attivo e assente nel repository)
            // ==========================================
            GameInfoPayload infoInesistente = gameManager.getGameInfoForPlayer("alice", 999);
            boolean casoBOk = (infoInesistente == null);

            System.out.println("\n[CASO B] Richiesta partita inesistente (ID: 999):");
            System.out.println(" - Restituzione null: " + (casoBOk ? "OK" : "FALLITO"));

            // Esito finale
            if (casoAComplessivo && casoBOk) {
                System.out.println("\n[SUCCESSO] Test di getGameInfoForPlayer (Casi A e B) SUPERATO!");
            } else {
                System.err.println("\n[FALLITO] Verifiche di getGameInfoForPlayer non superate.");
            }

        } catch (IOException e) {
            System.err.println("[ECCEZIONE] Errore durante l'esecuzione del test: " + e.getMessage());
        } finally {
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }
}
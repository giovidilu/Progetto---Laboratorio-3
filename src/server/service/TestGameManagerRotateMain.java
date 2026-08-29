package server.service;

import common.model.GameOutcome;
import common.model.GameRecord;
import common.model.GameTemplate;
import common.model.WordGroup;
import common.protocol.response.GameState;
import common.protocol.response.payload.GameInfoPayload;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test di isolamento per verificare la rotazione della partita, il consolidamento
 * dello storico su GameRepository e la consultazione retroattiva delle soluzioni (Caso C).
 */
public class TestGameManagerRotateMain {

    public static void main(String[] args) {
        System.out.println("=== TEST ISOLAMENTO: GameManager (rotateGame e Consultazione Storica) ===");

        String testRepoPath = "data/test_game_manager_rotate.json";
        File testFile = new File(testRepoPath);
        if (testFile.exists()) {
            testFile.delete();
        }

        try {
            // 1. Inizializzazione di loader, repository e manager
            GameTemplateLoader templateLoader = new GameTemplateLoader();
            Map<Integer, GameTemplate> templates = templateLoader.loadTemplates("Connections_Data.json");
            GameRepository gameRepo = new GameRepository(testRepoPath);
            long roundDuration = 60000L; // 60 secondi
            GameManager gameManager = new GameManager(templates, gameRepo, roundDuration);

            int initialGameId = gameManager.getCurrentGameId();
            List<WordGroup> groups = gameManager.getActiveGame().getGameTemplate().getGroups();

            List<String> g0Words = groups.get(0).getWords();
            List<String> g1Words = groups.get(1).getWords();
            List<String> g2Words = groups.get(2).getWords();
            List<String> wrongWords = Arrays.asList(g0Words.get(0), g0Words.get(1), g1Words.get(0), g1Words.get(1));

            // =========================================================================
            // 2. Simulazione attività dei giocatori nella partita attiva 1
            // =========================================================================
            // Utente 'alice': indovina 3 gruppi -> WON (+18 pt, 0 errori)
            gameManager.submitProposal("alice", g0Words);
            gameManager.submitProposal("alice", g1Words);
            gameManager.submitProposal("alice", g2Words);

            // Utente 'bob': 1 errore (-4 pt) e 1 gruppo indovinato (+6 pt) -> In corso (+2 pt, 1 errore)
            gameManager.submitProposal("bob", wrongWords);
            gameManager.submitProposal("bob", g0Words);

            // Utente 'charlie': 4 errori -> LOST_BY_MISTAKES (-16 pt, 4 errori)
            gameManager.submitProposal("charlie", wrongWords);
            gameManager.submitProposal("charlie", wrongWords);
            gameManager.submitProposal("charlie", wrongWords);
            gameManager.submitProposal("charlie", wrongWords);

            // =========================================================================
            // 3. Esecuzione della rotazione della partita
            // =========================================================================
            GameRecord rotatedRecord = gameManager.rotateGame();

            // =========================================================================
            // 4. Verifica avanzamento ID e stato GameRepository
            // =========================================================================
            int newGameId = gameManager.getCurrentGameId();
            boolean idAdvancedOk = (initialGameId == 1 && newGameId == 2);

            GameRecord archivedRecord = gameRepo.getGameRecord(initialGameId);
            boolean recordExistsOk = (archivedRecord != null && archivedRecord == rotatedRecord);

            // Metriche attese:
            // - totalParticipants = 3 (alice, bob, charlie)
            // - participantsFinished = 2 (alice per WON, charlie per LOST_BY_MISTAKES; bob era ancora in corso)
            // - participantsWon = 1 (alice)
            // - averageScore = (18 + 2 - 16) / 3 = 4 / 3 = 1.333...
            boolean statsOk = (archivedRecord != null)
                    && (archivedRecord.getGameId() == 1)
                    && (archivedRecord.getTotalParticipants() == 3)
                    && (archivedRecord.getParticipantsFinished() == 2)
                    && (archivedRecord.getParticipantsWon() == 1)
                    && (Math.abs(archivedRecord.getAverageScore() - (4.0 / 3.0)) < 0.001)
                    && (archivedRecord.getAllGroups() != null && archivedRecord.getAllGroups().size() == 4);

            System.out.println("[TEST 1] Avanzamento ID partita (1 -> 2): " + (idAdvancedOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 2] Presenza GameRecord archiviato in GameRepository: " + (recordExistsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 3] Statistiche aggregate GameRecord (3 part, 2 fin, 1 won, avg 1.33): " + (statsOk ? "OK" : "FALLITO"));

            // =========================================================================
            // 5. Verifica getGameInfoForPlayer su partita storica (Caso C)
            // =========================================================================
            // Consultazione per 'alice' (vincitrice)
            GameInfoPayload aliceHist = gameManager.getGameInfoForPlayer("alice", 1);
            boolean aliceHistOk = (aliceHist != null)
                    && (aliceHist.getState() == GameState.FINISHED)
                    && (aliceHist.getNumberCorrectGroups() == 3)
                    && (aliceHist.getErrors() == 0)
                    && (aliceHist.getScore() == 18)
                    && (aliceHist.getFinalAllocation() != null && aliceHist.getFinalAllocation().size() == 4);

            // Consultazione per 'bob' (interrotto dalla rotazione -> DNF)
            GameInfoPayload bobHist = gameManager.getGameInfoForPlayer("bob", 1);
            boolean bobHistOk = (bobHist != null)
                    && (bobHist.getState() == GameState.FINISHED)
                    && (bobHist.getNumberCorrectGroups() == 1)
                    && (bobHist.getErrors() == 1)
                    && (bobHist.getScore() == 2)
                    && (bobHist.getFinalAllocation() != null && bobHist.getFinalAllocation().size() == 4);

            // Consultazione per 'charlie' (sconfitto per errori)
            GameInfoPayload charlieHist = gameManager.getGameInfoForPlayer("charlie", 1);
            boolean charlieHistOk = (charlieHist != null)
                    && (charlieHist.getState() == GameState.FINISHED)
                    && (charlieHist.getNumberCorrectGroups() == 0)
                    && (charlieHist.getErrors() == 4)
                    && (charlieHist.getScore() == -16)
                    && (charlieHist.getFinalAllocation() != null && charlieHist.getFinalAllocation().size() == 4);

            // Consultazione per utente 'david' che non ha partecipato al Round 1
            GameInfoPayload davidHist = gameManager.getGameInfoForPlayer("david", 1);
            boolean davidHistOk = (davidHist != null)
                    && (davidHist.getState() == GameState.FINISHED)
                    && (davidHist.getNumberCorrectGroups() == 0)
                    && (davidHist.getErrors() == 0)
                    && (davidHist.getScore() == 0)
                    && (davidHist.getFinalAllocation() != null && davidHist.getFinalAllocation().size() == 4);

            System.out.println("[TEST 4] Storico 'alice' (FINISHED, 3 gruppi, 0 err, +18 pt, 4 allocazioni): " + (aliceHistOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 5] Storico 'bob' (FINISHED, 1 gruppo, 1 err, +2 pt, 4 allocazioni): " + (bobHistOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 6] Storico 'charlie' (FINISHED, 0 gruppi, 4 err, -16 pt, 4 allocazioni): " + (charlieHistOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 7] Storico non partecipante 'david' (FINISHED, default 0, 4 allocazioni): " + (davidHistOk ? "OK" : "FALLITO"));

            // =========================================================================
            // 6. Verifica isolamento della nuova partita attiva 2
            // =========================================================================
            GameInfoPayload aliceRound2 = gameManager.getGameInfoForPlayer("alice", null);
            boolean round2ResetOk = (aliceRound2 != null)
                    && (aliceRound2.getState() == GameState.ONGOING)
                    && (aliceRound2.getWords() != null && aliceRound2.getWords().size() == 16)
                    && (aliceRound2.getCorrectGroups() != null && aliceRound2.getCorrectGroups().isEmpty())
                    && (aliceRound2.getErrors() == 0)
                    && (aliceRound2.getScore() == 0);

            System.out.println("[TEST 8] Stato nuova partita attiva 2 resettato per 'alice': " + (round2ResetOk ? "OK" : "FALLITO"));

            // Esito finale
            if (idAdvancedOk && recordExistsOk && statsOk && aliceHistOk && bobHistOk && charlieHistOk && davidHistOk && round2ResetOk) {
                System.out.println("\n[SUCCESSO] Test di isolamento rotateGame e consultazione storica SUPERATO!");
            } else {
                System.err.println("\n[FALLITO] Verifiche di rotazione e consultazione storica non superate.");
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
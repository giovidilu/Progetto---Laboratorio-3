package server.service;

import common.model.GameOutcome;
import common.model.GameRecord;
import common.model.GameTemplate;
import common.model.User;
import common.model.WordGroup;
import common.protocol.response.GameState;
import common.protocol.response.payload.GameInfoPayload;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;
import server.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test di isolamento per verificare la rotazione della partita, il consolidamento
 * dello storico su GameRepository, l'aggiornamento persistente di UserRepository
 * e la consultazione retroattiva delle soluzioni.
 */
public class TestGameManagerRotateMain {

    public static void main(String[] args) {
        System.out.println("=== TEST ISOLAMENTO: GameManager (rotateGame e Aggiornamento Utenti) ===");

        String testRepoPath = "data/test_game_manager_rotate.json";
        String testUserPath = "data/test_users_rotate.json";
        File testFile = new File(testRepoPath);
        File testUserFile = new File(testUserPath);

        if (testFile.exists()) {
            testFile.delete();
        }
        if (testUserFile.exists()) {
            testUserFile.delete();
        }

        try {
            // 1. Inizializzazione dei repository e caricamento template
            GameTemplateLoader templateLoader = new GameTemplateLoader();
            Map<Integer, GameTemplate> templates = templateLoader.loadTemplates("data/Connections_Data.json");
            GameRepository gameRepo = new GameRepository(testRepoPath);
            UserRepository userRepo = new UserRepository(testUserPath);

            // Registrazione con il costruttore User(username, passwordHash, salt)
            userRepo.addUser(new User("alice", "hash_alice", "salt_alice"));
            userRepo.addUser(new User("bob", "hash_bob", "salt_bob"));
            userRepo.addUser(new User("charlie", "hash_charlie", "salt_charlie"));
            userRepo.addUser(new User("david", "hash_david", "salt_david"));
            // L'utente "ghost" NON viene inserito nel repository

            long roundDuration = 60000L; // 60 secondi
            GameManager gameManager = new GameManager(templates, gameRepo, userRepo, roundDuration);

            int initialGameId = gameManager.getCurrentGameId();
            List<WordGroup> groups = gameManager.getActiveGame().getGameTemplate().getGroups();

            List<String> g0Words = groups.get(0).getWords();
            List<String> g1Words = groups.get(1).getWords();
            List<String> g2Words = groups.get(2).getWords();
            List<String> wrongWords = Arrays.asList(g0Words.get(0), g0Words.get(1), g1Words.get(0), g1Words.get(1));

            // =========================================================================
            // 2. Simulazione delle mosse nel round 1
            // =========================================================================
            // Alice (registrata): 3 gruppi indovinati -> WON (+18 pt, 0 errori)
            gameManager.submitProposal("alice", g0Words);
            gameManager.submitProposal("alice", g1Words);
            gameManager.submitProposal("alice", g2Words);

            // Bob (registrato): 1 errore (-4 pt) e 1 gruppo indovinato (+6 pt) -> DNF (+2 pt, 1 errore)
            gameManager.submitProposal("bob", wrongWords);
            gameManager.submitProposal("bob", g0Words);

            // Charlie (registrato): 4 errori -> LOST_BY_MISTAKES (-16 pt, 4 errori)
            gameManager.submitProposal("charlie", wrongWords);
            gameManager.submitProposal("charlie", wrongWords);
            gameManager.submitProposal("charlie", wrongWords);
            gameManager.submitProposal("charlie", wrongWords);

            // Ghost (NON registrato): 1 errore (-4 pt, 1 errore) -> DNF
            gameManager.submitProposal("ghost", wrongWords);

            // =========================================================================
            // 3. Esecuzione della rotazione della partita
            // =========================================================================
            GameRecord rotatedRecord = gameManager.rotateGame();

            // =========================================================================
            // 4. Verifica avanzamento ID e stato GameRecord
            // =========================================================================
            int newGameId = gameManager.getCurrentGameId();
            boolean idAdvancedOk = (initialGameId == 1 && newGameId == 2);

            GameRecord archivedRecord = gameRepo.getGameRecord(initialGameId);
            boolean recordExistsOk = (archivedRecord != null && archivedRecord == rotatedRecord);

            boolean statsOk = (archivedRecord != null)
                    && (archivedRecord.getGameId() == 1)
                    && (archivedRecord.getTotalParticipants() == 4)
                    && (archivedRecord.getParticipantsFinished() == 2)
                    && (archivedRecord.getParticipantsWon() == 1)
                    && (Math.abs(archivedRecord.getAverageScore() - 0.0) < 0.001)
                    && (archivedRecord.getAllGroups() != null && archivedRecord.getAllGroups().size() == 4);

            System.out.println("[TEST 1] Avanzamento ID partita (1 -> 2): " + (idAdvancedOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 2] Presenza GameRecord archiviato in GameRepository: " + (recordExistsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 3] Statistiche aggregate GameRecord (4 part, 2 fin, 1 won, avg 0.0): " + (statsOk ? "OK" : "FALLITO"));

            // =========================================================================
            // 5. Verifica aggiornamento UserStats su UserRepository
            // =========================================================================
            User userAlice = userRepo.getUser("alice");
            boolean aliceStatsOk = (userAlice != null)
                    && (userAlice.getStats().getTotalScore() == 18)
                    && (userAlice.getStats().getGamesPlayed() == 1)
                    && (userAlice.getStats().getGamesWon() == 1)
                    && (userAlice.getStats().getGamesLost() == 0)
                    && (userAlice.getStats().getCurrentStreak() == 1)
                    && (userAlice.getStats().getMaxStreak() == 1)
                    && (userAlice.getStats().getPerfectPuzzles() == 1);

            User userBob = userRepo.getUser("bob");
            boolean bobStatsOk = (userBob != null)
                    && (userBob.getStats().getTotalScore() == 2)
                    && (userBob.getStats().getGamesPlayed() == 1)
                    && (userBob.getStats().getGamesWon() == 0)
                    && (userBob.getStats().getGamesLost() == 0)
                    && (userBob.getStats().getCurrentStreak() == 0)
                    && (userBob.getStats().getMaxStreak() == 0);

            User userCharlie = userRepo.getUser("charlie");
            boolean charlieStatsOk = (userCharlie != null)
                    && (userCharlie.getStats().getTotalScore() == -16)
                    && (userCharlie.getStats().getGamesPlayed() == 1)
                    && (userCharlie.getStats().getGamesWon() == 0)
                    && (userCharlie.getStats().getGamesLost() == 1)
                    && (userCharlie.getStats().getCurrentStreak() == 0)
                    && (userCharlie.getStats().getMaxStreak() == 0);

            User userDavid = userRepo.getUser("david");
            boolean davidStatsOk = (userDavid != null)
                    && (userDavid.getStats().getTotalScore() == 0)
                    && (userDavid.getStats().getGamesPlayed() == 0)
                    && (userDavid.getStats().getGamesWon() == 0)
                    && (userDavid.getStats().getGamesLost() == 0);

            User userGhost = userRepo.getUser("ghost");
            boolean ghostSkippedOk = (userGhost == null);

            System.out.println("[TEST 4] Statistiche Alice (WON, +18 pt, 1 vinta, 0 perse, streak 1): " + (aliceStatsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 5] Statistiche Bob (DNF, +2 pt, 0 vinte, 0 perse, streak 0): " + (bobStatsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 6] Statistiche Charlie (LOST, -16 pt, 0 vinte, 1 persa, streak 0): " + (charlieStatsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 7] Isolamento David (non partecipante, 0 partite, score 0): " + (davidStatsOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 8] Gestione Ghost (non registrato, saltato senza eccezioni): " + (ghostSkippedOk ? "OK" : "FALLITO"));

            // =========================================================================
            // 6. Verifica consultazione storica su GameRepository (Caso C)
            // =========================================================================
            GameInfoPayload aliceHist = gameManager.getGameInfoForPlayer("alice", 1);
            boolean aliceHistOk = (aliceHist != null)
                    && (aliceHist.getState() == GameState.FINISHED)
                    && (aliceHist.getNumberCorrectGroups() == 3)
                    && (aliceHist.getErrors() == 0)
                    && (aliceHist.getScore() == 18)
                    && (aliceHist.getFinalAllocation() != null && aliceHist.getFinalAllocation().size() == 4);

            GameInfoPayload bobHist = gameManager.getGameInfoForPlayer("bob", 1);
            boolean bobHistOk = (bobHist != null)
                    && (bobHist.getState() == GameState.FINISHED)
                    && (bobHist.getNumberCorrectGroups() == 1)
                    && (bobHist.getErrors() == 1)
                    && (bobHist.getScore() == 2)
                    && (bobHist.getFinalAllocation() != null && bobHist.getFinalAllocation().size() == 4);

            GameInfoPayload charlieHist = gameManager.getGameInfoForPlayer("charlie", 1);
            boolean charlieHistOk = (charlieHist != null)
                    && (charlieHist.getState() == GameState.FINISHED)
                    && (charlieHist.getNumberCorrectGroups() == 0)
                    && (charlieHist.getErrors() == 4)
                    && (charlieHist.getScore() == -16)
                    && (charlieHist.getFinalAllocation() != null && charlieHist.getFinalAllocation().size() == 4);

            GameInfoPayload davidHist = gameManager.getGameInfoForPlayer("david", 1);
            boolean davidHistOk = (davidHist != null)
                    && (davidHist.getState() == GameState.FINISHED)
                    && (davidHist.getNumberCorrectGroups() == 0)
                    && (davidHist.getErrors() == 0)
                    && (davidHist.getScore() == 0)
                    && (davidHist.getFinalAllocation() != null && davidHist.getFinalAllocation().size() == 4);

            System.out.println("[TEST 9] Storico Alice (FINISHED, 3 gruppi, 0 err, +18 pt): " + (aliceHistOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 10] Storico Bob (FINISHED, 1 gruppo, 1 err, +2 pt): " + (bobHistOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 11] Storico Charlie (FINISHED, 0 gruppi, 4 err, -16 pt): " + (charlieHistOk ? "OK" : "FALLITO"));
            System.out.println("[TEST 12] Storico David (FINISHED, default 0, 4 allocazioni): " + (davidHistOk ? "OK" : "FALLITO"));

            // =========================================================================
            // 7. Reset della nuova partita attiva
            // =========================================================================
            GameInfoPayload aliceRound2 = gameManager.getGameInfoForPlayer("alice", null);
            boolean round2ResetOk = (aliceRound2 != null)
                    && (aliceRound2.getState() == GameState.ONGOING)
                    && (aliceRound2.getWords() != null && aliceRound2.getWords().size() == 16)
                    && (aliceRound2.getCorrectGroups() != null && aliceRound2.getCorrectGroups().isEmpty())
                    && (aliceRound2.getErrors() == 0)
                    && (aliceRound2.getScore() == 0);

            System.out.println("[TEST 13] Reset stato round 2 per Alice: " + (round2ResetOk ? "OK" : "FALLITO"));

            boolean allPassed = idAdvancedOk && recordExistsOk && statsOk 
                    && aliceStatsOk && bobStatsOk && charlieStatsOk && davidStatsOk && ghostSkippedOk
                    && aliceHistOk && bobHistOk && charlieHistOk && davidHistOk && round2ResetOk;

            if (allPassed) {
                System.out.println("\n[SUCCESSO] Test di isolamento rotateGame e aggiornamento utenti SUPERATO!");
            } else {
                System.err.println("\n[FALLITO] Uno o più controlli non sono stati superati.");
            }

        } catch (IOException e) {
            System.err.println("[ECCEZIONE] Errore durante l'esecuzione del test: " + e.getMessage());
        } finally {
            if (testFile.exists()) {
                testFile.delete();
            }
            if (testUserFile.exists()) {
                testUserFile.delete();
            }
        }
    }
}
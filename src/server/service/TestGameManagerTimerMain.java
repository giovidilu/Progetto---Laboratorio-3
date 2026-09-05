package server.service;

import common.model.GameRecord;
import common.model.GameTemplate;
import common.model.MoveOutcome;
import common.model.ProposalResult;
import common.model.WordGroup;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;
import server.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Test di isolamento per verificare la schedulazione automatica del timer
 * di GameManager e l'arresto ordinato tramite stop().
 */
public class TestGameManagerTimerMain {

    public static void main(String[] args) {
        System.out.println("=== TEST ISOLAMENTO: GameManager (Timer Automatico e Shutdown) ===");

        String testRepoPath = "data/test_game_manager_timer.json";
        String testUserPath = "data/test_users_timer.json";
        File testFile = new File(testRepoPath);
        File testUserFile = new File(testUserPath);

        if (testFile.exists()) {
            testFile.delete();
        }
        if (testUserFile.exists()) {
            testUserFile.delete();
        }

        try {
            // 1. Inizializzazione con durata breve per il test (2000 ms)
            GameTemplateLoader templateLoader = new GameTemplateLoader();
            Map<Integer, GameTemplate> templates = templateLoader.loadTemplates("data/Connections_Data.json");
            GameRepository gameRepo = new GameRepository(testRepoPath);
            UserRepository userRepo = new UserRepository(testUserPath);
            long roundDuration = 2000L;
            GameManager gameManager = new GameManager(templates, gameRepo, userRepo, roundDuration);

            // 2. Avvio dello scheduler periodico
            gameManager.start();
            System.out.println("[OK] GameManager avviato con roundDuration = 2000 ms.");

            // 3. Esecuzione di una proposta valida su Partita 1
            List<WordGroup> groups = gameManager.getActiveGame().getGameTemplate().getGroups();
            List<String> g0Words = groups.get(0).getWords();
            ProposalResult r1 = gameManager.submitProposal("alice", g0Words);

            boolean submitOk = (r1.getMoveOutcome() == MoveOutcome.CORRECT)
                    && (gameManager.getCurrentGameId() == 1);
            System.out.println("[TEST 1] Mossa su partita 1 effettuata: " + (submitOk ? "OK" : "FALLITO"));

            // 4. Attesa del trigger automatico del timer (3000 ms > 2000 ms)
            System.out.println("[INFO] Attesa di 3000 ms per lo scatto della rotazione automatica...");
            Thread.sleep(3000L);

            // 5. Verifica della rotazione avvenuta senza invocazioni esplicite
            int currentIdAfterWait = gameManager.getCurrentGameId();
            boolean autoRotateOk = (currentIdAfterWait == 2);

            GameRecord archivedRecord1 = gameRepo.getGameRecord(1);
            boolean record1ArchivedOk = (archivedRecord1 != null)
                    && (archivedRecord1.getGameId() == 1)
                    && (archivedRecord1.getTotalParticipants() == 1)
                    && (archivedRecord1.getAverageScore() == 6.0);

            System.out.println("[TEST 2] Rotazione automatica a GameId 2: " + (autoRotateOk ? "OK" : "FALLITO (valore: " + currentIdAfterWait + ")"));
            System.out.println("[TEST 3] Partita 1 archiviata su GameRepository (+6 pt alice): " + (record1ArchivedOk ? "OK" : "FALLITO"));

            // 6. Invocazione di stop() e verifica dell'arresto dello scheduler
            gameManager.stop();
            System.out.println("[OK] Invocato stop() su GameManager.");

            System.out.println("[INFO] Attesa di ulteriori 3000 ms per verificare l'arresto definitivo...");
            Thread.sleep(3000L);

            int finalGameId = gameManager.getCurrentGameId();
            boolean timerStoppedOk = (finalGameId == 2);

            System.out.println("[TEST 4] Nessuna rotazione successiva dopo stop() (GameId stabile a 2): " + (timerStoppedOk ? "OK" : "FALLITO (valore: " + finalGameId + ")"));

            if (submitOk && autoRotateOk && record1ArchivedOk && timerStoppedOk) {
                System.out.println("\n[SUCCESSO] Test del Timer Automatico di GameManager SUPERATO!");
            } else {
                System.err.println("\n[FALLITO] Verifiche sul timer automatico non superate.");
            }

        } catch (IOException e) {
            System.err.println("[ECCEZIONE] Errore di I/O nel caricamento dei template: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("[ECCEZIONE] Thread interrotto durante l'attesa: " + e.getMessage());
            Thread.currentThread().interrupt();
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
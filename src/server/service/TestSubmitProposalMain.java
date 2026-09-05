package server.service;

import common.model.GameOutcome;
import common.model.GameTemplate;
import common.model.MoveOutcome;
import common.model.ProposalResult;
import common.model.WordGroup;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;
import server.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Suite di test per la validazione di submitProposal su GameManager,
 * allineata esclusivamente ai metodi reali definiti nelle classi di modello.
 */
public class TestSubmitProposalMain {

    public static void main(String[] args) {
        System.out.println("=== TEST ISOLAMENTO: GameManager (submitProposal - 6 Casi di Specifica) ===");

        String testRepoPath = "data/test_submit_proposal.json";
        String testUserPath = "data/test_users_submit.json";
        File testFile = new File(testRepoPath);
        File testUserFile = new File(testUserPath);

        if (testFile.exists()) {
            testFile.delete();
        }
        if (testUserFile.exists()) {
            testUserFile.delete();
        }

        try {
            // 1. Inizializzazione dell'ambiente di test
            GameTemplateLoader templateLoader = new GameTemplateLoader();
            Map<Integer, GameTemplate> templates = templateLoader.loadTemplates("data/Connections_Data.json");
            GameRepository gameRepo = new GameRepository(testRepoPath);
            UserRepository userRepo = new UserRepository(testUserPath);
            long roundDuration = 60000L; // 60 secondi
            GameManager gameManager = new GameManager(templates, gameRepo, userRepo, roundDuration);

            // Estrazione dei 4 gruppi corretti del round attivo
            List<WordGroup> groups = gameManager.getActiveGame().getGameTemplate().getGroups();
            List<String> g0Words = groups.get(0).getWords();
            List<String> g1Words = groups.get(1).getWords();
            List<String> g2Words = groups.get(2).getWords();
            List<String> g3Words = groups.get(3).getWords();

            // =========================================================================
            // CASO 1: Proposte MALFORMED sintattiche (parola non presente, duplicata, dimensione != 4)
            // =========================================================================
            ProposalResult rAlien = gameManager.submitProposal("alice", Arrays.asList("PAROLA_NON_PRESENTE", g0Words.get(0), g0Words.get(1), g0Words.get(2)));
            ProposalResult rDuplicate = gameManager.submitProposal("alice", Arrays.asList(g0Words.get(0), g0Words.get(0), g0Words.get(1), g0Words.get(2)));
            ProposalResult rShort = gameManager.submitProposal("alice", Arrays.asList(g0Words.get(0), g0Words.get(1), g0Words.get(2)));

            boolean caso1Ok = rAlien.getMoveOutcome() == MoveOutcome.MALFORMED
                    && rDuplicate.getMoveOutcome() == MoveOutcome.MALFORMED
                    && rShort.getMoveOutcome() == MoveOutcome.MALFORMED
                    && rAlien.getUpdatedState().getMistakes() == 0
                    && rAlien.getUpdatedState().getScore() == 0;

            System.out.println("[CASO 1] Proposte MALFORMED sintattiche rifiutate senza errori: " + (caso1Ok ? "OK" : "FALLITO"));

            // =========================================================================
            // CASO 2: Proposta WRONG (parole appartenenti al gioco ma non formanti un gruppo valido)
            // =========================================================================
            List<String> mixedProposal = Arrays.asList(g0Words.get(0), g0Words.get(1), g1Words.get(0), g1Words.get(1));
            ProposalResult rWrong = gameManager.submitProposal("alice", mixedProposal);

            boolean caso2Ok = rWrong.getMoveOutcome() == MoveOutcome.WRONG
                    && rWrong.getGameOutcome() == null
                    && rWrong.getUpdatedState().getMistakes() == 1
                    && rWrong.getUpdatedState().getScore() == -4;

            System.out.println("[CASO 2] Proposta WRONG applica penalità (-4 pt, 1 errore): " + (caso2Ok ? "OK" : "FALLITO"));

            // =========================================================================
            // CASO 3: Proposta CORRECT (4 parole compongono un gruppo tematico valido)
            // =========================================================================
            ProposalResult rCorrect = gameManager.submitProposal("alice", g0Words);

            boolean caso3Ok = (rCorrect.getMoveOutcome() == MoveOutcome.CORRECT)
                    && (rCorrect.getGuessedGroup() != null)
                    && (rCorrect.getUpdatedState().getCorrectGroups().size() == 1)
                    && (rCorrect.getUpdatedState().getScore() == 2);

            System.out.println("[CASO 3] Proposta CORRECT assegna gruppo e punti (+6 pt): " + (caso3Ok ? "OK" : "FALLITO"));

            // =========================================================================
            // CASO 4: Proposta MALFORMED semantica (riuso di parole già indovinate dal giocatore)
            // =========================================================================
            List<String> reuseProposal = Arrays.asList(g0Words.get(0), g1Words.get(0), g1Words.get(1), g1Words.get(2));
            ProposalResult rReuse = gameManager.submitProposal("alice", reuseProposal);

            boolean caso4Ok = (rReuse.getMoveOutcome() == MoveOutcome.MALFORMED)
                    && (rReuse.getUpdatedState().getMistakes() == 1)
                    && (rReuse.getUpdatedState().getScore() == 2);

            System.out.println("[CASO 4] Proposta con parole già scoperte rifiutata come MALFORMED: " + (caso4Ok ? "OK" : "FALLITO"));

            // =========================================================================
            // CASO 5: Condizione di Vittoria (WON al 3° gruppo corretto)
            // =========================================================================
            gameManager.submitProposal("alice", g1Words);
            ProposalResult rWin = gameManager.submitProposal("alice", g2Words);

            boolean caso5Ok = rWin.getMoveOutcome() == MoveOutcome.CORRECT
                    && rWin.getGameOutcome() == GameOutcome.WON
                    && rWin.getUpdatedState().getCorrectGroups().size() == 3
                    && rWin.getUpdatedState().getScore() == 14;

            System.out.println("[CASO 5] Transizione a GameOutcome.WON al 3° gruppo indovinato: " + (caso5Ok ? "OK" : "FALLITO"));

            // =========================================================================
            // CASO 6: Condizione di Sconfitta (LOST_BY_MISTAKES al 4° errore su utente 'bob')
            // =========================================================================
            gameManager.submitProposal("bob", mixedProposal);
            gameManager.submitProposal("bob", mixedProposal);
            gameManager.submitProposal("bob", mixedProposal);
            ProposalResult rLost = gameManager.submitProposal("bob", mixedProposal);

            boolean caso6Ok = (rLost.getMoveOutcome() == MoveOutcome.WRONG)
                    && (rLost.getGameOutcome() == GameOutcome.LOST_BY_MISTAKES)
                    && (rLost.getUpdatedState().getMistakes() == 4)
                    && (rLost.getUpdatedState().getScore() == -16);

            System.out.println("[CASO 6] Transizione a LOST_BY_MISTAKES al 4° errore (-16 pt): " + (caso6Ok ? "OK" : "FALLITO"));

            // =========================================================================
            // Controllo Addizionale: Rifiuto mosse per partite concluse (ALREADY_COMPLETED)
            // =========================================================================
            ProposalResult rAfterWin = gameManager.submitProposal("alice", g3Words);
            ProposalResult rAfterLost = gameManager.submitProposal("bob", g0Words);

            boolean casoExtraOk = (rAfterWin.getMoveOutcome() == MoveOutcome.ALREADY_COMPLETED)
                    && (rAfterWin.getGameOutcome() == GameOutcome.WON)
                    && (rAfterLost.getMoveOutcome() == MoveOutcome.ALREADY_COMPLETED)
                    && (rAfterLost.getGameOutcome() == GameOutcome.LOST_BY_MISTAKES);

            System.out.println("[EXTRA] Blocco ALREADY_COMPLETED per utenti a fine partita: " + (casoExtraOk ? "OK" : "FALLITO"));

            // Esito finale
            if (caso1Ok && caso2Ok && caso3Ok && caso4Ok && caso5Ok && caso6Ok && casoExtraOk) {
                System.out.println("\n[SUCCESSO] Tutti i test di submitProposal hanno dato esito positivo!");
            } else {
                System.err.println("\n[FALLITO] Uno o più casi di test non sono stati superati.");
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
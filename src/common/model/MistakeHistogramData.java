package common.model;

public class MistakeHistogramData {
    private int solvedWith0Mistakes = 0;
    private int solvedWith1Mistake = 0;
    private int solvedWith2Mistakes = 0;
    private int solvedWith3Mistakes = 0;
    private int solvedWith4Mistakes = 0;
    private int failed = 0;
    private int notFinished = 0;

    public void incrementSolvedWith(int mistakes){
        switch (mistakes) {
            case 0:
                solvedWith0Mistakes++;
                break;
            case 1:
                solvedWith1Mistake++;
                break;
            case 2:
                solvedWith2Mistakes++;
                break;
            case 3:
                solvedWith3Mistakes++;
                break;
            default:
                // Gestione difensiva: un valore fuori range è un errore logico nel server
                throw new IllegalArgumentException("Numero di errori non valido: " + mistakes);
        }
    }

    public void incrementFailed() {
        failed++;
    }

    public void incrementNotFinished() {
        notFinished++;
    }

    public int getSolvedWith0Mistakes() { return solvedWith0Mistakes; }
    public int getSolvedWith1Mistake() { return solvedWith1Mistake; }
    public int getSolvedWith2Mistakes() { return solvedWith2Mistakes; }
    public int getSolvedWith3Mistakes() { return solvedWith3Mistakes; }
    public int getSolvedWith4Mistakes() { return solvedWith4Mistakes; }
    public int getFailed() { return failed; }
    public int getNotFinished() { return notFinished; }
}
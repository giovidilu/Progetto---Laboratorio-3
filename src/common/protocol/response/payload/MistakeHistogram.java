package common.protocol.response.payload;

public class MistakeHistogram {
    private final int solvedWith0Mistakes;
    private final int solvedWith1Mistake;
    private final int solvedWith2Mistakes;
    private final int solvedWith3Mistakes;
    private final int solvedWith4Mistakes;
    private final int failed;
    private final int notFinished;

    public MistakeHistogram(int solvedWith0Mistakes, int solvedWith1Mistake, int solvedWith2Mistakes,
                             int solvedWith3Mistakes, int solvedWith4Mistakes, int failed, int notFinished) {
        this.solvedWith0Mistakes = solvedWith0Mistakes;
        this.solvedWith1Mistake = solvedWith1Mistake;
        this.solvedWith2Mistakes = solvedWith2Mistakes;
        this.solvedWith3Mistakes = solvedWith3Mistakes;
        this.solvedWith4Mistakes = solvedWith4Mistakes;
        this.failed = failed;
        this.notFinished = notFinished;
    }

    public int getSolvedWith0Mistakes() { return solvedWith0Mistakes; }
    public int getSolvedWith1Mistake() { return solvedWith1Mistake; }
    public int getSolvedWith2Mistakes() { return solvedWith2Mistakes; }
    public int getSolvedWith3Mistakes() { return solvedWith3Mistakes; }
    public int getSolvedWith4Mistakes() { return solvedWith4Mistakes; }
    public int getFailed() { return failed; }
    public int getNotFinished() { return notFinished; }
}
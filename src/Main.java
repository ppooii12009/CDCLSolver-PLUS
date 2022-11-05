import java.io.FileNotFoundException;
import java.util.Date;

public class Main
{
    public static void main(String[] args) throws FileNotFoundException
    {
        Date startTime = new Date();
        DimacsFileReader dimacsFileReader = new DimacsFileReader();
        CDCLSolver cdclSolver = new CDCLSolver();
        cdclSolver.putCNF(dimacsFileReader.read("test.txt"));
        //System.out.println(cdclSolver.getCnf().toString());
        cdclSolver.solve();
        Date endTime = new Date();
        cdclSolver.outputSolution();
        long c = endTime.getTime() - startTime.getTime();
        System.out.println("Time consumed: " + c / 1000 + "s");
    }
}

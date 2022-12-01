import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// 读取DIMACS文件
public class DimacsFileReader
{
    public CNF read(String filePath) throws FileNotFoundException
    {
        File t = new File("");
        if (filePath == null || filePath.isEmpty())
        {
            return null;
        }
        File file = new File(t.getAbsolutePath() + "/src/" + filePath);
        if (!file.exists())
        {
            return null;
        }

        CNF cnf = new CNF();
        List<Clause> clauses = new ArrayList<>();
        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            switch (line.charAt(0))
            {
                // 注释行
                case 'c':
                    break;
                // 开始输入
                case 'p':
                    String[] contents = line.split(" ");
                    if (contents.length != 4)
                    {
                        System.out.println("illegal DIMACS file format");
                        return null;
                    }
                    int n_literals = Integer.parseInt(contents[2]);
                    int n_clauses = Integer.parseInt(contents[3]);

                    if (n_literals <= 0 || n_clauses <= 0)
                    {
                        System.out.println("illegal DIMACS file format");
                        return null;
                    }

                    // 开始处理子句输入
                    for (int i = 0; i < n_clauses; i++)
                    {
                        if (!scanner.hasNextLine())
                        {
                            System.out.println("illegal DIMACS file format");
                            return null;
                        }

                        String clauseString = scanner.nextLine();
                        String[] literalArray = clauseString.split(" ");
                        Clause clause = new Clause();
                        ArrayList<Integer> literals = new ArrayList<>();
                        for (String literal :
                                literalArray)
                        {
                            int l = Integer.parseInt(literal);
                            if (l == 0)
                            {
                                break;
                            }
                            literals.add(l);
                        }
                        clause.setLiterals(literals);
                        clause.setIndex(i);
                        clauses.add(clause);
                    }

                    cnf.setClauses(clauses);
                    return cnf;
                // 非法输入
                default:
                    System.out.println("illegal DIMACS file format");
                    return null;
            }
        }

        cnf.setClauses(clauses);
        return cnf;
    }
}

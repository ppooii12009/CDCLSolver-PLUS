import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 合取范式类
 */
public class CNF implements Serializable
{
    private List<Clause> clauses;

    public CNF()
    {
        clauses = new ArrayList<>();
    }

    public List<Clause> getClauses()
    {
        return clauses;
    }

    public void setClauses(List<Clause> clauses)
    {
        this.clauses = clauses;
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        int length = clauses.size();
        for (int i = 0; i < length; i++)
        {
            s.append(clauses.get(i).toString());
            if (i < length - 1)
            {
                s.append("∧");
            }
        }
        return s.toString();
    }

    public Clause getClauseByIndex(int index)
    {
        if (index < 0)
        {
            return null;
        }
        for (Clause clause :
                clauses)
        {
            if (clause.getIndex() == index)
            {
                return clause;
            }
        }

        return null;
    }

    public void addClause(Clause clause)
    {
        clauses.add(clause);
        clause.setIndex(clauses.size());
        clause.setValue(null);
    }
}

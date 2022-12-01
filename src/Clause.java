import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 子句类
 */
public class Clause implements Serializable
{
    private List<Integer> literals;
    private Boolean value;
    private int index;
    // 监视变量(watched literal)
    private int w1 = 0;
    private int w2 = 0;

    public Clause()
    {
        literals = new ArrayList<>();
        value = null;
        w1 = 0;
        w2 = 0;
    }

    public void swapW()
    {
        int t = w1;
        w1 = w2;
        w2 = t;
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder("(");
        int length = literals.size();
        for (int i = 0; i < length; i++)
        {
            Integer literal = literals.get(i);
            s.append(literal < 0 ? "¬x" + -1 * literal : "x" + literal);
            if (i < length - 1)
            {
                s.append("∨");
            }
        }
        s.append(")");
        return s.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clause clause = (Clause) o;
        return index == clause.index;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(index);
    }

    public List<Integer> getLiterals()
    {
        return literals;
    }

    public void setLiterals(List<Integer> literals)
    {
        this.literals = literals;
    }

    public Boolean getValue()
    {
        return value;
    }

    public void setValue(Boolean value)
    {
        this.value = value;
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public int getW1()
    {
        return w1;
    }

    public void setW1(int w1)
    {
        this.w1 = w1;
    }

    public int getW2()
    {
        return w2;
    }

    public void setW2(int w2)
    {
        this.w2 = w2;
    }
}

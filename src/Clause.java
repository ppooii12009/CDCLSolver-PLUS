import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 子句类
 */
public class Clause implements Serializable, Comparable<Clause>
{
    private List<Integer> literals;
    private Boolean value;
    private int index;
    // 未赋值的文字数量
    private int unassignedNumber;

    // 该子句为单位子句时有效
    // 未赋值的唯一文字
    private int unitLiteral;

    public Clause()
    {
        literals = new ArrayList<>();
        value = null;
        unassignedNumber = 0;
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

    public void addOneToUnassignedNumber()
    {
        this.unassignedNumber++;
    }

    public void minusOneToUnassignedNumber()
    {
        this.unassignedNumber--;
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

    public int getUnitLiteral()
    {
        return unitLiteral;
    }

    public void setUnitLiteral(int unitLiteral)
    {
        this.unitLiteral = unitLiteral;
    }

    public int getUnassignedNumber()
    {
        return unassignedNumber;
    }

    public void setUnassignedNumber(int unassignedNumber)
    {
        this.unassignedNumber = unassignedNumber;
    }

    @Override
    public int compareTo(Clause o)
    {
        if (this.unassignedNumber < o.unassignedNumber)
        {
            return -1;
        }
        if (this.unassignedNumber == o.unassignedNumber)
        {
            return Integer.compare(this.getLiterals().size() - this.unassignedNumber, o.getLiterals().size() - o.unassignedNumber);
        }
        return 1;
    }
}

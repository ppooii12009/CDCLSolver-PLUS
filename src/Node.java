import java.util.Objects;

/**
 * 蕴含图节点类
 */
public class Node
{
    // 文字
    private Integer literal;

    // 决策层
    private int decisionLevel;

    // 赋值原因(子句index) -1表示决策赋值
    // 非负数表示由index=reason的子句推出
    private int reason;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return literal == node.literal && decisionLevel == node.decisionLevel && reason == node.reason;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(literal, decisionLevel, reason);
    }

    public Integer getLiteral()
    {
        return literal;
    }

    public void setLiteral(Integer literal)
    {
        this.literal = literal;
    }

    public int getDecisionLevel()
    {
        return decisionLevel;
    }

    public void setDecisionLevel(int decisionLevel)
    {
        this.decisionLevel = decisionLevel;
    }

    public int getReason()
    {
        return reason;
    }

    public void setReason(int reason)
    {
        this.reason = reason;
    }
}

import utils.CloneUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 求解器
 */
public class CDCLSolver
{
    private CNF cnf;

    private int literalNumber;

    private Map<Integer, Boolean> assignments;

    private Trail trail;

    public CDCLSolver()
    {
        cnf = null;
        literalNumber = 0;
        assignments = new HashMap<>();
        trail = new Trail();
    }

    public CDCLSolver(CNF cnf)
    {
        trail = new Trail();
        putCNF(cnf);
    }

    public void putCNF(CNF cnf)
    {
        this.cnf = CloneUtils.clone(cnf);
        Set<Integer> literalSet = new HashSet<>();
        List<Clause> clauses = cnf.getClauses();
        for (Clause c :
                clauses)
        {
            List<Integer> literals = c.getLiterals();
            for (Integer literal :
                    literals)
            {
                literalSet.add(literal > 0 ? literal : -literal);
            }
        }
        this.literalNumber = literalSet.size();
        for (Integer literal :
                literalSet)
        {
            assignments.put(literal, null);
        }
    }

    // 获取文字赋值
    public Boolean getLiteralAssignment(int literal)
    {
        int abs = Math.abs(literal);
        if (assignments.containsKey(abs))
        {
            if (assignments.get(abs) == null)
            {
                return null;
            }
            return (literal > 0) == assignments.get(abs);
        }
        return null;
    }


    // 对文字赋值
    public void setLiteralAssignment(int literal)
    {
        this.setLiteralAssignment(literal, true);
    }

    // 对文字赋值
    public void setLiteralAssignment(int literal, boolean assignment)
    {
        int abs = Math.abs(literal);
        assignments.put(abs, (literal > 0) == assignment);
        for (Clause clause :
                cnf.getClauses())
        {
            if (clause.getLiterals().contains(literal) || clause.getLiterals().contains(-literal))
            {
                clause.addOneToUnassignedNumber();
            }
        }
    }

    // 清除文字赋值
    public void clearAssignment(int literal)
    {
        assignments.put(Math.abs(literal), null);
        for (Clause clause :
                cnf.getClauses())
        {
            if (clause.getLiterals().contains(literal) || clause.getLiterals().contains(-literal))
            {
                clause.minusOneToUnassignedNumber();
            }
        }
    }

    // 寻找单位子句
    public Clause findUnitClause()
    {
        for (Clause clause :
                cnf.getClauses())
        {
            if (clause.getLiterals().isEmpty())
            {
                continue;
            }

            int number_false = 0;
            int number_undefined = 0;
            int unitLiteral = 0;
            for (Integer literal :
                    clause.getLiterals())
            {
                Boolean assignment = this.getLiteralAssignment(literal);
                if (assignment == null)
                {
                    unitLiteral = literal;
                    number_undefined++;
                } else if (!assignment)
                {
                    number_false++;
                }
            }

            // 找到单位子句
            if (number_false == clause.getLiterals().size() - 1 && number_undefined == 1)
            {
                clause.setUnitLiteral(unitLiteral);
                return clause;
            }
        }

        // 没有单位子句
        return null;
    }

    // 更新子句真值
    public void updateClauseValue()
    {
        for (Clause clause :
                cnf.getClauses())
        {
            if (clause.getLiterals() == null || clause.getLiterals().isEmpty())
            {
                clause.setValue(true);
                continue;
            }
            int number_false = 0;
            clause.setValue(null);

            for (Integer literal :
                    clause.getLiterals())
            {
                Boolean value = this.getLiteralAssignment(literal);
                if (value == null)
                {
                    continue;
                }
                if (value)
                {
                    clause.setValue(true);
                    break;
                }
                number_false++;
            }

            if (number_false == clause.getLiterals().size())
            {
                clause.setValue(false);
            }
        }
    }

    // 文字传播
    public void propagateLiteral(int literal, int index)
    {
        this.setLiteralAssignment(literal, true);
        this.updateClauseValue();

        Node node = new Node();
        node.setLiteral(literal);
        node.setDecisionLevel(this.getTrail().getCurrentLevel());
        node.setReason(index);
        this.getTrail().addNodeToCurrentLevel(node);
    }

    // 单位子句传播
    public void unitPropagation()
    {
        Integer literal = null;
        while (true)
        {
            Clause clause = this.findUnitClause();
            if (clause == null)
            {
                break;
            }

            this.propagateLiteral(clause.getUnitLiteral(), clause.getIndex());
        }
    }

    // 检测冲突
    public Clause detectConflict()
    {
        for (Clause clause :
                this.getCnf().getClauses())
        {
            if (clause.getValue() == null)
            {
                continue;
            }
            if (!clause.getValue())
            {
                return clause;
            }
        }

        return null;
    }

    // 寻找未赋值的文字
    public Integer findUnassignedLiteral()
    {
        Set<Integer> literals = this.getAssignments().keySet();
        for (Integer literal :
                literals)
        {
            if (this.getAssignments().get(literal) == null)
            {
                return literal;
            }
        }

        return null;
    }

    // 子句学习 返回回溯层数
    public int clauseLearn(Clause conflictClause)
    {
        Trail trail = this.getTrail();
        // 最后一层决策层的节点
        List<Node> lastLevelNodes = trail.getLastLevelNodes();
        // 初始化 所有节点都有可能成为UIP
        Map<Node, Boolean> possibleUIP = new HashMap<>();
        for (Node node :
                lastLevelNodes)
        {
            possibleUIP.put(node, true);
        }

        // 冲突节点
        Node conflictNode = new Node();
        conflictNode.setReason(conflictClause.getIndex());
        conflictNode.setLiteral(null);
        conflictNode.setDecisionLevel(trail.getCurrentLevel());

        // 包含冲突节点的最后一层节点集合
        List<Node> lastLevelNodes_star = new ArrayList<>(lastLevelNodes);
        lastLevelNodes_star.add(conflictNode);

        // 所有节点集合
        Set<Node> V = new HashSet<>();
        for (int i = 0; i <= trail.getCurrentLevel(); i++)
        {
            List<Node> nodes = trail.getNodes().get(i);
            if (nodes == null)
            {
                continue;
            }
            V.addAll(nodes);
        }
        V.add(conflictNode);

        // 最后一层决策层节点的邻接表
        Map<Node, List<Node>> lastLevelAdjacencyList = new HashMap<>();

        for (Node node :
                lastLevelNodes_star)
        {
            lastLevelAdjacencyList.put(node, new ArrayList<>());
        }

        // 文字 - 节点映射
        Map<Integer, Node> literalNodeMap = new HashMap<>();
        for (Node node :
                V)
        {
            literalNodeMap.put(node.getLiteral(), node);
        }

        // 最后一层决策出的文字
        Set<Integer> lastLevelLiterals = new HashSet<>();
        for (Node node :
                lastLevelNodes)
        {
            lastLevelLiterals.add(node.getLiteral());
        }

        // 构建邻接表
        for (Node node :
                lastLevelNodes_star)
        {
            int reason = node.getReason();
            if (reason != -1)
            {
                Clause reasonClause = this.getCnf().getClauseByIndex(reason);
                if (reasonClause == null)
                {
                    continue;
                }

                for (Integer fromLiteral :
                        reasonClause.getLiterals())
                {
                    if (node.getLiteral() != null)
                    {
                        if (!Objects.equals(fromLiteral, node.getLiteral()) && lastLevelLiterals.contains(-fromLiteral))
                        {
                            Node fromNode = literalNodeMap.get(-fromLiteral);
                            lastLevelAdjacencyList.get(fromNode).add(node);
                        }
                    } else
                    {
                        if (lastLevelLiterals.contains(-fromLiteral))
                        {
                            Node fromNode = literalNodeMap.get(-fromLiteral);
                            lastLevelAdjacencyList.get(fromNode).add(node);
                        }
                    }
                }
            }
        }

        // 排除非UIP ??
        int i = 0;
        for (Node node :
                lastLevelNodes)
        {
            for (Node toNode :
                    lastLevelAdjacencyList.get(node))
            {
                int j = i + 1;
                while (j < lastLevelNodes_star.size())
                {
                    if (lastLevelNodes_star.get(j).equals(toNode))
                    {
                        break;
                    }
                    possibleUIP.put(lastLevelNodes_star.get(j), false);
                    j++;
                }
            }
        }

        // 寻找唯一UIP
        Node UIP = null;
        Collections.reverse(lastLevelNodes);
        for (Node node :
                lastLevelNodes)
        {
            if (possibleUIP.get(node))
            {
                UIP = node;
                break;
            }
        }
        Collections.reverse(lastLevelNodes);

        // 可到达冲突节点的节点
        Set<Node> canReachConflictNode = new HashSet<>();
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(conflictNode);
        while (!queue.isEmpty())
        {
            Node u = queue.remove();
            canReachConflictNode.add(u);
            int reason = u.getReason();
            if (reason != -1)
            {
                Clause reasonClause = this.getCnf().getClauseByIndex(reason);
                for (Integer literal :
                        reasonClause.getLiterals())
                {
                    if (!Objects.equals(literal, u.getLiteral()) && lastLevelLiterals.contains(-literal))
                    {
                        queue.add(literalNodeMap.get(-literal));
                    }
                }
            }
        }

        // 唯一UIP的后继节点
        Set<Node> successorOfUIP = new HashSet<>();
        queue.add(UIP);
        while (!queue.isEmpty())
        {
            Node u = queue.remove();
            for (Node node :
                    lastLevelAdjacencyList.get(u))
            {
                successorOfUIP.add(node);
                queue.add(node);
            }
        }

        //  B包含是UIP的后继并且能到达冲突节点的节点
        Set<Node> B = new HashSet<>(canReachConflictNode);
        B.retainAll(successorOfUIP);

        // A B共同构成了V的冲突切割
        Set<Node> A = new HashSet<>(V);
        A.removeAll(B);

        // 原因集
        Set<Node> R = new HashSet<>();
        // 所有节点的邻接表
        Map<Node, List<Node>> entireAdjacencyList = new HashMap<>();
        for (Node node :
                V)
        {
            entireAdjacencyList.put(node, new ArrayList<>());
        }
        // 构造邻接表
        for (Node node :
                V)
        {
            int reason = node.getReason();
            if (reason != -1)
            {
                Clause reasonClause = this.getCnf().getClauseByIndex(reason);
                if (reasonClause == null)
                {
                    continue;
                }

                for (Integer fromLiteral :
                        reasonClause.getLiterals())
                {
                    if (!Objects.equals(fromLiteral, node.getLiteral()))
                    {
                        Node fromNode = literalNodeMap.get(-fromLiteral);
                        entireAdjacencyList.get(fromNode).add(node);
                    }
                }
            }
        }

        // 计算R中元素
        for (Node node :
                A)
        {
            for (Node toNode :
                    entireAdjacencyList.get(node))
            {
                if (B.contains(toNode))
                {
                    R.add(node);
                    break;
                }
            }
        }

        // 加入学习到的新子句
        Clause learnedClause = new Clause();
        List<Integer> learnedLiterals = new ArrayList<>();
        for (Node node :
                R)
        {
            learnedLiterals.add(-node.getLiteral());
        }
        learnedClause.setLiterals(learnedLiterals);
        this.getCnf().addClause(learnedClause);

        List<Integer> levelSet = R.stream().map(Node::getDecisionLevel).distinct().collect(Collectors.toList());
        levelSet.sort(Collections.reverseOrder());
        int backTrackLevel = levelSet.size() >= 2 ? levelSet.get(1) : 0;

        return backTrackLevel;
    }

    // 回溯到 backTrackLevel 层
    public void backtrack(int backTrackLevel)
    {
        int currentLevel = this.getTrail().getCurrentLevel();
        while (currentLevel > backTrackLevel)
        {
            for (Node node :
                    trail.getNodes().get(currentLevel))
            {
                // 清除赋值
                this.clearAssignment(node.getLiteral());
            }

            // 从迹中删去该层
            trail.getNodes().remove(currentLevel);
            trail.setCurrentLevel(--currentLevel);
        }
    }

    // 主函数
    public void solve()
    {
        if (this.getCnf() == null)
        {
            System.out.println("empty CNF");
            return;
        }

        int r = 1;
        while (true)
        {
            System.out.println("Round" + r++);
            //Collections.sort(this.getCnf().getClauses());
            // 单位传播
            this.unitPropagation();
            Clause conflictClause = this.detectConflict();
            // 发现冲突
            if (conflictClause != null)
            {
                // 在决策层0发现冲突说明不可满足
                if (this.getTrail().getCurrentLevel() == 0)
                {
                    System.out.println("unable to satisfy");
                    return;
                }

                // 子句学习和回溯
                int backtrackLevel = this.clauseLearn(conflictClause);
                this.backtrack(backtrackLevel);
                this.updateClauseValue();
            } else
            {
                Integer literal = this.findUnassignedLiteral();
                // 所有变量均被赋值且未发生冲突，说明可满足
                if (literal == null)
                {
                    return;
                } else
                {
                    // 开始一个新的决策层
                    this.getTrail().addLevel();
                    this.propagateLiteral(literal, -1);
                }
            }
        }
    }

    // 输出求解结果
    public void outputSolution()
    {
        Set<Integer> keySets = assignments.keySet();
        for (Integer key :
                keySets)
        {
            System.out.println("X" + key + ": " + assignments.get(key));
        }
    }

    public CNF getCnf()
    {
        return cnf;
    }

    public void setCnf(CNF cnf)
    {
        this.cnf = cnf;
    }

    public int getLiteralNumber()
    {
        return literalNumber;
    }

    public void setLiteralNumber(int literalNumber)
    {
        this.literalNumber = literalNumber;
    }

    public Map<Integer, Boolean> getAssignments()
    {
        return assignments;
    }

    public void setAssignments(Map<Integer, Boolean> assignments)
    {
        this.assignments = assignments;
    }

    public Trail getTrail()
    {
        return trail;
    }

    public void setTrail(Trail trail)
    {
        this.trail = trail;
    }
}

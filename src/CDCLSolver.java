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

    private Map<Integer, Set<Clause>> watchedList;

    private Queue<Clause> unitClauses;

    // 包含文字key的子句
    private Map<Integer, Set<Clause>> includedClause;

    // 历史赋值记录
    Set<String> assignmentsHistory;

    // VSIDS 计数器
    private Map<Integer, Double> counter;

    // VSIDS 计数器常数
    private final double alpha = 1.05;

    public CDCLSolver()
    {
        cnf = null;
        literalNumber = 0;
        assignments = new HashMap<>();
        trail = new Trail();
        watchedList = new HashMap<>();
        unitClauses = new ArrayDeque<>();
        includedClause = new HashMap<>();
        assignmentsHistory = new HashSet<>();
        counter = new HashMap<>();
    }

    public CDCLSolver(CNF cnf)
    {
        trail = new Trail();
        putCNF(cnf);
    }

    public void putCNF(CNF originCnf)
    {
        this.cnf = CloneUtils.clone(originCnf);
        Set<Integer> literalSet = new HashSet<>();
        List<Clause> clauses = cnf.getClauses();
        for (Clause c :
                clauses)
        {
            List<Integer> literals = c.getLiterals();
            for (Integer literal :
                    literals)
            {
                counter.putIfAbsent(literal, (double) 0);
                counter.putIfAbsent(-literal, (double) 0);
                counter.put(literal, counter.get(literal) + 1);
                includedClause.putIfAbsent(literal, new HashSet<>());
                includedClause.get(literal).add(c);
                literalSet.add(literal > 0 ? literal : -literal);
            }
        }
        this.literalNumber = literalSet.size();
        for (Integer literal :
                literalSet)
        {
            assignments.put(literal, null);
            watchedList.put(literal, new HashSet<>());
            watchedList.put(-literal, new HashSet<>());
        }

        // 设置默认watched literal
        for (Clause c :
                clauses)
        {
            List<Integer> literals = c.getLiterals();
            if (literals.size() == 1)
            {
                unitClauses.add(c);
                continue;
            }
            int w1 = literals.get(0);
            int w2 = literals.get(1);
            c.setW1(w1);
            c.setW2(w2);
            watchedList.get(w1).add(c);
            watchedList.get(w2).add(c);
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
    public void setLiteralAssignment(int literal, boolean assignment)
    {
        int abs = Math.abs(literal);
        assignments.put(abs, (literal > 0) == assignment);
        // 处理值为true的子句
        if (!assignment)
        {
            literal = -literal;
        }
        if (includedClause.containsKey(literal))
        {
            for (Clause c :
                    includedClause.get(literal))
            {
                c.setValue(true);
            }
        }

        // 处理watched literals
        literal = -literal;

        Clause[] literalWatchedClauses = new Clause[0];
        literalWatchedClauses = watchedList.get(literal).toArray(literalWatchedClauses);
        for (Clause c :
                literalWatchedClauses)
        {
            List<Integer> literals = c.getLiterals();
            if (literal == c.getW1())
            {
                c.swapW();
            }

            int newWatchedLiteral = c.getW1();
            // 寻找新的 watched literal
            for (Integer l :
                    literals)
            {
                // 未被赋值的文字
                if (assignments.get(l) == null)
                {
                    if (l != c.getW1())
                    {
                        newWatchedLiteral = l;
                        break;
                    }
                }
            }
            // 仍然保留至少2个未赋值变量 替换新的 watched literal
            if (newWatchedLiteral != c.getW1())
            {
                c.setW2(newWatchedLiteral);
                watchedList.get(newWatchedLiteral).add(c);
            } else // 仅剩一个未赋值变量, 出现单位子句 W1为被蕴含(必须为真)的文字
            {
                unitClauses.add(c);
            }
            watchedList.get(literal).remove(c);
        }
    }

    // 清除文字赋值
    public void clearAssignment(int literal)
    {
        assignments.put(Math.abs(literal), null);
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

            // 若子句真值待定, 重新设定watched literals
            if (clause.getValue() == null)
            {
                watchedList.get(clause.getW1()).add(clause);
                // 若W2未被赋值 保持watched状态
                if (assignments.get(clause.getW2()) == null)
                {
                    watchedList.get(clause.getW2()).add(clause);
                }
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
        while (!unitClauses.isEmpty())
        {
            Clause clause = unitClauses.remove();
            // 检查当前子句是否已满足或冲突
            if (clause.getValue() == null)
            {
                this.propagateLiteral(clause.getW1(), clause.getIndex());
            } else if (clause.getValue())
            {
                continue;
            } else
            {
                // 出现冲突 清空单位子句 退出
                unitClauses.clear();
                break;
            }
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

    // 检查当前赋值组合是否已经尝试过
    public boolean checkIfAlreadyTried()
    {
        // 检查是否已经尝试过同样的赋值
        StringBuilder a = new StringBuilder();
        Set<Integer> keySets = new TreeSet<>(assignments.keySet());
        for (Integer key :
                keySets)
        {
            Boolean t = assignments.get(key);
            if (t == null)
            {
                a.append("null");
            } else
            {
                a.append(t ? 1 : 0);
            }
        }
        return !assignmentsHistory.add(a.toString());
    }

    // 寻找未赋值的文字
    public Integer findUnassignedLiteral()
    {
        LinkedHashMap<Integer, Double> sortedMap = new LinkedHashMap<>();
        counter.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .forEachOrdered(e -> sortedMap.put(e.getKey(), e.getValue()));

        for (Map.Entry<Integer,Double> e:
             sortedMap.entrySet())
        {
            Integer literal = e.getKey();
            if (this.getAssignments().get(literal) == null)
            {
                this.setLiteralAssignment(literal, true);
                if (!this.checkIfAlreadyTried())
                {
                    return literal;
                }
                this.clearAssignment(literal);
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
        Set<Node> V = new LinkedHashSet<>();
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
        Set<Node> R = new LinkedHashSet<>();
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
        int w1 = 0;
        int w2 = 0;
        for (Node node :
                R)
        {
            w2 = w1;
            w1 = -node.getLiteral();
            learnedLiterals.add(w1);
            counter.put(w1, counter.get(w1) + 1);
        }
        if (w2 == 0)
        {
            w2 = w1;
        }
        learnedClause.setW1(w1);
        learnedClause.setW2(w2);

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

    // VSIDS 计数器除以常数
    public void divideCounter()
    {
        for (Integer literal :
                counter.keySet())
        {
            counter.put(literal, counter.get(literal) / alpha);
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
        int clock = 0;
        while (true)
        {
            System.out.println("Round" + r++);
            // VSIDS 计数器自除
            clock++;
            if (clock % 50 == 0)
            {
                this.divideCounter();
            }
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
                } else if (literal == 0)
                {
                    // 回溯
                    // 在决策层0发现冲突说明不可满足
                    if (this.getTrail().getCurrentLevel() == 0)
                    {
                        System.out.println("unable to satisfy");
                        return;
                    }
                    this.backtrack(this.getTrail().getCurrentLevel() - 1);
                    this.updateClauseValue();
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
        Set<Integer> keySets = new TreeSet<>(assignments.keySet());
        for (Integer key :
                keySets)
        {
            assignments.putIfAbsent(key, true);
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

    public Map<Integer, Double> getCounter()
    {
        return counter;
    }

    public void setCounter(Map<Integer, Double> counter)
    {
        this.counter = counter;
    }
}

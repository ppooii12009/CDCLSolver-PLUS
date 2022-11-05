import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 迹类
 */
public class Trail
{
    // 当前决策层高度
    private int currentLevel;

    // 决策节点
    private Map<Integer, List<Node>> nodes;

    public Trail()
    {
        currentLevel = 0;
        nodes = new HashMap<>();
    }

    public int getCurrentLevel()
    {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel)
    {
        this.currentLevel = currentLevel;
    }

    public Map<Integer, List<Node>> getNodes()
    {
        return nodes;
    }

    public void setNodes(Map<Integer, List<Node>> nodes)
    {
        this.nodes = nodes;
    }

    public void addNode(Node node, int level)
    {
        if (level < 0)
        {
            return;
        }

        nodes.putIfAbsent(level, new ArrayList<>());
        nodes.get(level).add(node);
    }

    public void addNodeToCurrentLevel(Node node)
    {
        this.addNode(node, this.currentLevel);
    }

    public void addLevel()
    {
        this.currentLevel++;
    }

    public List<Node> getLastLevelNodes()
    {
        return this.getNodes().get(currentLevel);
    }
}

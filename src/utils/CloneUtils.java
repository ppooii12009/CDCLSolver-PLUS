package utils;

import java.io.*;

public class CloneUtils
{
    public static <T extends Serializable> T clone(T obj)
    {
        T cloneObj = null;
        try
        {
            //写入字节流
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream obs = new ObjectOutputStream(out);
            obs.writeObject(obj);
            obs.close();

            //分配内存，写入原始对象，生成新对象
            ByteArrayInputStream is = new ByteArrayInputStream(out.toByteArray());
            ObjectInputStream os = new ObjectInputStream(is);
            cloneObj = (T) os.readObject();
            os.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return cloneObj;
    }
}
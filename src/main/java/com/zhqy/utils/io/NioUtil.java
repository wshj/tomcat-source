package com.zhqy.utils.io;

import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * <h3>NIO操作工具类</h3>
 *
 * @author wangshuaijing
 * @version 1.0.0
 * @date 2020/11/23 10:52 上午
 */
public class NioUtil {

    private static final Log LOGGER = LogFactory.getLog(NioUtil.class);

    /**
     * ByteBuffer分配的大小，单位：字节
     */
    private static final int CAPACITY = 1024 * 1000;

    public static String readNio(String fileName) {
        FileInputStream fin = null;
        StringBuilder content = new StringBuilder();
        try {
            fin = new FileInputStream(new File(fileName));
            FileChannel channel = fin.getChannel();

            ByteBuffer bf = ByteBuffer.allocate(CAPACITY);
            int length = -1;
            while ((length = channel.read(bf)) != -1) {
                //注意，读取后，将位置置为0，将limit置为容量, 以备下次读入到字节缓冲中，从0开始存储
                bf.flip();

                ByteBuffer line = ByteBuffer.allocate(CAPACITY * 10);
                line.put(bf.array());

                String contentStr = new String(line.array(), 0, length);
                while (contentStr.lastIndexOf('\n') != contentStr.length()) {
                    // 如果读取的内容不是以换行结尾，则记录向后读取

                    // 每次读取1个字节
                    ByteBuffer oneByte = ByteBuffer.allocate(1);
                    int readLength = channel.read(oneByte);
                    if (readLength != -1) {
                        oneByte.flip();
                        line.put(oneByte.array());
                        oneByte.clear();

                        contentStr = new String(line.array(), 0, length);
                    } else {
                        break;
                    }
                }

                content.append(contentStr);
                bf.clear();
            }
            channel.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fin) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fin = null;
                }
            }
        }
        return content.toString();
    }

    public static String readNio2(String fileName) {
        FileInputStream fin = null;
        StringBuilder content = new StringBuilder();
        try {
            fin = new FileInputStream(new File(fileName));
            FileChannel channel = fin.getChannel();

            // 记录第一次读取的内容最后一行结尾（"\n"）的数据
            ByteBuffer builder = ByteBuffer.allocate(CAPACITY);

            ByteBuffer bf = ByteBuffer.allocate(CAPACITY);
            int length;
            while ((length = channel.read(bf)) != -1) {
                //注意，读取后，将位置置为0，将limit置为容量, 以备下次读入到字节缓冲中，从0开始存储
                bf.flip();

                // 读取最后一个换行符的索引
                int lastNindex = -1;
                byte[] bfArray = bf.array();
                for (int i = 0; i < bfArray.length; i++) {
                    if ('\n' == bfArray[i]) {
                        lastNindex = i;
                    }
                }

                if (lastNindex > -1) {
                    // 如果包含换行符

                    ByteBuffer resultByteBuffer = ByteBuffer.allocate(CAPACITY * 10);
                    if (builder.remaining() < CAPACITY) {
                        // 如果剩余数据不为空，则本地读取时，先放入剩余数据
                        for (int i = 0; i < builder.position(); i++) {
                            resultByteBuffer.put(builder.get(i));
                        }
                    }
                    for (int i = 0; i <= lastNindex; i++) {
                        resultByteBuffer.put(bfArray[i]);
                    }

                    // 和上次剩余字符串和本次读取的第一行拼接
                    resultByteBuffer.flip();
                    String resultStr = new String(resultByteBuffer.array()) + "\n";
                    content.append(resultStr);

                    // 置空字符串缓存对象
                    builder.clear();
                    // 将本次最后剩余的字符串放入其中
                    for (int i = lastNindex + 1; i < bfArray.length; i++) {
                        builder.put(bfArray[i]);
                    }
                } else {
                    LOGGER.info("没有换行符");
                    // 将本次最后剩余的字符串放入其中
                    String resultStr = new String(bf.array()) + "\n";
                    content.append(resultStr);
                }

                bf.clear();
            }
            channel.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fin) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fin = null;
                }
            }
        }
        return content.toString();
    }

    public static void writeNio(String filename, String content) {
        writeNio(filename, content, false);
    }

    public static void writeNio(String filename, String content, boolean append) {
        if (StringUtils.isBlank(filename)) {
            throw new RuntimeException("请指定数据文件路径");
        }
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException("写入文件的内容不可为空");
        }

        FileOutputStream fos = null;
        try {
            File file = new File(filename);
            fos = new FileOutputStream(file, append);
            FileChannel channel = fos.getChannel();
            ByteBuffer src = StandardCharsets.UTF_8.encode(content);
            //字节缓冲的容量和limit会随着数据长度变化，不是固定不变的
            int length = 0;
            while ((length = channel.write(src)) != 0) {
                //注意，这里不需要clear，将缓冲中的数据写入到通道中后 第二次接着上一次的顺序往下读
                LOGGER.info(String.format("写入文件[%s]中内容的长度:%s", filename, length));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fos = null;
                }
            }
        }
    }

}

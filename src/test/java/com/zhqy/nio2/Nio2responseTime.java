package com.zhqy.nio2;

import com.zhqy.utils.io.NioUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h3>nio2压测结果分析</h3>
 *
 * @author wangshuaijing
 * @version 1.0.0
 * @date 2020/11/23 10:48 上午
 */
public class Nio2responseTime {

    private static final Log LOGGER = LogFactory.getLog(NioUtil.class);

    private static final String channel = "信息: 创建 Nio2Channel 耗时";
    private static final String wrapper = "信息: 创建 Nio2SocketWrapper 耗时";
    private static final String process = "信息: 创建 org.apache.tomcat.util.net.SocketProcessorBase 耗时";
    private static final String execute = "信息: 创建 NIO2 提交任务 耗时";

    public static void main(String[] args) {
        String result = NioUtil.readNio2(Nio2responseTime.class.getResource("/nio2jmeterResult.txt").getFile());
        List<String> resultList = Arrays.stream(result.split("\n")).map(str -> {
            if (0 == StringUtils.indexOf(str, "十一月")) {
                return "";
            }
            if(StringUtils.isBlank(str)){
                return "";
            }
            return str;
        }).filter(StringUtils::isNotBlank).collect(Collectors.toList());

//        int size = resultList.size() / 4 + 1;
        int size = 100;
        int[] category = new int[size];

        int[] channelTime = new int[size];
        int[] wrapperTime = new int[size];
        int[] processTime = new int[size];
        int[] executeTime = new int[size];

        int j = 0;
        for (int i = 1; i <= resultList.size(); i++) {
            if(j >= size){
                break;
            }
            String str = resultList.get(i - 1);

            category[j] = j + 1;

            String[] split = str.split("：");
            if (split.length < 2) {
                LOGGER.error(String.format("行内容不合法：%s", str));
                continue;
            }
            String time = split[1].split(" ")[0];

            if (0 == StringUtils.indexOf(str, channel)) {
                channelTime[j] = NumberUtils.toInt(time, -1);
            } else if (0 == StringUtils.indexOf(str, wrapper)) {
                wrapperTime[j] = NumberUtils.toInt(time, -1);
            } else if (0 == StringUtils.indexOf(str, process)) {
                processTime[j] = NumberUtils.toInt(time, -1);
            } else if (0 == StringUtils.indexOf(str, execute)) {
                executeTime[j] = NumberUtils.toInt(time, -1);
            } else {
                LOGGER.error(String.format("行内容不合法：%s", str));
            }
            if(i % 4 == 0){
                j++;
            }
        }
        NioUtil.writeNio("/Users/zhqy/tmp/tomcat-source/category.json", Arrays.toString(category));
        NioUtil.writeNio("/Users/zhqy/tmp/tomcat-source/channelTime.json", Arrays.toString(channelTime));
        NioUtil.writeNio("/Users/zhqy/tmp/tomcat-source/wrapperTime.json", Arrays.toString(wrapperTime));
        NioUtil.writeNio("/Users/zhqy/tmp/tomcat-source/processTime.json", Arrays.toString(processTime));
        NioUtil.writeNio("/Users/zhqy/tmp/tomcat-source/executeTime.json", Arrays.toString(executeTime));
    }

}

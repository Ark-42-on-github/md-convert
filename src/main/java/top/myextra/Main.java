package top.myextra;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
传入一个文件夹
扫描所有的md文件
对md文件依次进行内容搜索

正则匹配所有需要替换的<img src="" />标签
正则匹配所有需要替换的![](*.png)或![](*.jpg)
依次把这些做记录和命名

把文件中的路径进行提取，相对路径拼接找到图片文件
把图片文件转为base64编码的字符串

复制源md文件，文件操作对新复制出来的副本文件操作
把单个md文件中所有的base64编码的图片字符串放进md末尾依次排好准备被引用
把命名赋予给图片映射关系做好，正式替换之前的记录，达成一一对应

每个md文件处理完成后控制台打印文件处理完成


 */
public class Main {
    /**
     * 程序的入口点。
     * 该方法接收用户输入的文件夹路径，然后遍历该文件夹下的所有Markdown文件。
     * 对每个Markdown文件，它将读取文件内容，处理这些内容，然后将处理后的结果写入新的Markdown文件。
     *
     * @param args 命令行参数，未使用
     */
    public static void main(String[] args) {
        // 提示用户输入文件夹路径
        System.out.println("请输入一个文件夹(可拖入)，程序自动解析其中的md文件");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        // 移除输入字符串两端可能存在的双引号
        if (input.startsWith("\"")) {
            input = input.replaceAll("\"", "");
        }
        // 输出处理后的输入字符串
        System.out.println(input);
        File dir = new File(input);
        // 检查输入的路径是否为一个目录
        if (dir.isDirectory()) {
            try {
                // 获取目录下所有Markdown文件的列表
                List<File> mdFiles = new ArrayList<>(FileUtils.listFiles(dir, new String[]{"md"}, true));

                // 检查Markdown文件列表是否为空
                if (!mdFiles.isEmpty()) {
                    for (File file : mdFiles) {
                        // 读取Markdown文件的全部内容
                        String sourceText = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        // 处理Markdown内容，返回一个文件实体列表
                        List<FileEntity> list = readMarkDown(file, sourceText);
                        // 创建目标文件，其名称为原文件名加上" - base64.md"后缀
                        File target = new File(
                                file.getParent(),
                                String.format("%s - base64.md",
                                        FilenameUtils.getBaseName(file.getName())
                                )
                        );
                        // 复制原文件到目标文件
                        FileUtils.copyFile(file, target);
                        // 将处理后的Markdown内容写入目标文件
                        writeNewMarkDown(target, sourceText, list);
                        // 输出处理完成的消息
                        System.out.printf("%s 文件已处理完成，目标文件是 %s\n",
                                file.getName(),
                                target.getName()
                        );
                    }
                } else {
                    // 如果目录下没有Markdown文件，输出错误消息并退出程序
                    System.err.println("文件夹没有md文件");
                    System.exit(-1);
                }
            } catch (IOException e) {
                // 输出IO异常的相关信息，并抛出运行时异常
                System.err.printf("错误：%s,\n源自：%s\n", e.getMessage(), e.getCause());
                throw new RuntimeException(e);
            }
        }else {
            System.out.println("输入并非文件夹");
        }
    }


    public static Pattern mdPattern = Pattern.compile("!\\[([^]]*)]\\(([^)]*)\\)");

    public static Pattern mdReqPattern = Pattern.compile("!\\[(.*)]\\((.+) \"(.+)\"\\)");

    public static Pattern h5Pattern = Pattern.compile("<img.*src=\"(.*)\".*/>");

    /**
     * 从Markdown文件中读取并处理所有图片链接。
     *
     * @param file Markdown文件对象，用于获取图片文件的父目录路径。
     * @param sourceText Markdown文件的源文本内容。
     * @return 包含所有图片信息的文件实体列表。
     * @throws IOException 如果读取文件或转换Base64时发生错误。
     */
    public static List<FileEntity> readMarkDown(File file, String sourceText) throws IOException {
        // 初始化结果列表，用于存储所有图片文件的信息。
        List<FileEntity> result = new ArrayList<>();
        // 使用预定义的正则表达式模式匹配源文本中的图片链接。
        Matcher matcher = mdPattern.matcher(sourceText);
        // 图片编号初始化为1，用于唯一标识每个图片文件。
        int i = 1;
        // 获取Markdown文件的父目录路径，用于定位图片文件。
        String parentDir = file.getParent();
        // 遍历所有匹配到的图片链接。
        while (matcher.find()) {
            // 创建一个新的文件实体对象，用于存储当前图片的信息。
            FileEntity fileEntity = new FileEntity();
            // 设置图片的唯一标识ID。
            fileEntity.setId(i++);
            // 设置图片的目标名称，格式为"images+ID"。
            fileEntity.setTargetName(String.format("images%d", fileEntity.getId()));
            // 设置图片的原始链接。
            fileEntity.setSource(matcher.group(0));
            // 设置图片的文件名。
            fileEntity.setName(matcher.group(1));
            // 构造图片的Markdown语法链接。
            fileEntity.setTarget(String.format("![%s][%s]", fileEntity.getName(), fileEntity.getTargetName()));
            // 获取图片链接的路径部分。
            String path = matcher.group(2);
            // 根据图片链接的类型（HTTP链接或本地文件链接），处理并设置图片的Base64编码表示。
            String base64Image;
            String format = "\n\n\n[%s]:data:image/jpeg;base64,%s";
            if (path.startsWith("http")) {
                System.out.printf("正在处理url: %s",path);
                // 如果是HTTP链接，处理并设置Base64编码的图片数据。
                if (path.endsWith("\"")) {
                    // 如果链接以双引号结束，去除干扰的标题信息
                    // 比如 https://www.example.com/1.png "图片1"
                    // 截取成 https://www.example.com/1.png
                    path = path.substring(0, path.lastIndexOf(" "));
                }
                base64Image = String.format(
                        format,
                        fileEntity.getTargetName(),
                        httpSaveAsBase64(path)
                );
            } else {
                // 如果是本地文件链接，读取文件并将其转换为Base64编码的字符串。
                byte[] data = Files.readAllBytes(
                        Paths.get(parentDir + "\\" + fileEntity.getPath()));
                base64Image = String.format(
                        format,
                        fileEntity.getTargetName(),
                        new String(Base64.getEncoder().encode(data))
                );
            }
            // 设置图片链接的原始路径。
            fileEntity.setPath(path);
            // 移除Base64编码字符串中的换行符，以确保兼容性。
            base64Image = base64Image.replaceAll("\r\n", "");
            // 设置处理后的Base64编码的图片数据。
            fileEntity.setBase64Target(base64Image);
            // 将当前图片文件的信息添加到结果列表中。
            result.add(fileEntity);
        }
        // 返回包含所有图片信息的文件实体列表。
        return result;
    }

    /**
     * 将源文本写入指定文件，并根据实体列表中的替换规则对文本进行替换。
     * 首先，将源文本中的特定字符串替换为另一字符串，然后将处理后的文本写入文件。
     * 接着，将实体列表中的Base64编码字符串追加写入文件。
     *
     * @param file 要写入的文件对象
     * @param sourceText 待写入的源文本
     * @param entityList 包含替换规则和Base64编码字符串的实体列表
     * @throws IOException 如果文件操作发生错误，则抛出此异常
     */
    public static void writeNewMarkDown(File file, String sourceText, List<FileEntity> entityList) throws IOException {
        // 将文件对象转换为Path对象
        Path path = file.toPath();
        // 初始化处理后的文本字符串
        String text = sourceText;
        // 创建BufferedWriter对象，用于向文件写入文本
        BufferedWriter writer = Files.newBufferedWriter(path);
        // 遍历实体列表，对源文本进行替换
        for (FileEntity entity : entityList) {
            // 获取实体的源字符串
            String source = entity.getSource();
            // 如果源文本中包含源字符串，则进行替换
            if (sourceText.contains(source)) {
                text = text.replace(source, entity.getTarget());
            }
        }
        // 写入处理后的文本
        writer.write(text);
        // 刷新BufferedWriter，确保已写入文件
        writer.flush();
        // 再次遍历实体列表，将Base64编码字符串追加写入文件
        for (FileEntity entity : entityList) {
            // 获取实体的Base64编码字符串
            writer.write(entity.getBase64Target());
        }
        // 再次刷新BufferedWriter
        writer.flush();
        // 关闭BufferedWriter
        writer.close();
    }

    /**
     * 将远程图片转换为Base64编码的字符串。
     * 该方法通过URL打开远程图片资源，将其读取为字节数组，然后将字节数组转换为Base64编码的字符串。
     * 使用此方法可以方便地将远程图片数据嵌入到文本格式的数据中，例如JSON或XML。
     *
     * @param url 远程图片的URL地址。
     * @return 图片数据的Base64编码字符串。
     * @throws IOException 如果读取或转换过程中发生I/O错误。
     */
    public static String httpSaveAsBase64(String url) throws IOException {
        // 创建URL对象，指定远程图片的地址。
        URL imageUrl = new URL(url);
        // 打开远程图片资源的输入流。
        HttpURLConnection urlC = (HttpURLConnection) imageUrl.openConnection();
        urlC.setRequestProperty("User-Agent", "Mozilla/4.76");
        urlC.setRequestMethod("GET");
        urlC.setDoOutput(true);
        urlC.setDoInput(true);
        urlC.setUseCaches(false);
        urlC.setConnectTimeout(1000);
        urlC.setReadTimeout(1000);
        BufferedInputStream in = new BufferedInputStream(urlC.getInputStream());

        // 创建ByteArrayOutputStream用于存储读取的图片字节数据。
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 创建缓冲区，用于批量读取输入流中的数据。
        byte[] buffer = new byte[1024 * 1024];
        // 循环读取输入流中的数据，直到读取完毕。
        int n;
        while (-1 != (n = in.read(buffer))) {
            // 将读取到的数据写入到ByteArrayOutputStream中。
            out.write(buffer, 0, n);
        }

        // 将ByteArrayOutputStream中的所有数据转换为字节数组。
        byte[] imageBytes = out.toByteArray();
        // 关闭输入流。
        in.close();
        // 关闭输出流。
        out.close();

        // 将图片字节数组转换为Base64编码的字符串。
        // 将字节数组转换为Base64字符串
        return Base64.getEncoder().encodeToString(imageBytes);
    }

}
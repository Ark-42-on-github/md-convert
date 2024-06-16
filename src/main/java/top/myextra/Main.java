package top.myextra;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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
    public static void main(String[] args) {

        //try {
        //    List<FileEntity> read = readMarkDown(new File("D:\\Documents\\笔记\\vmware tools全攻略\\vmware tools全攻略.md"));
        //    read.forEach(System.out::println);
        //} catch (IOException e) {
        //    throw new RuntimeException(e);
        //}

        //String input1 = "![图片1](./image/1.jpg)";
        //String input2 = "![](./images/16.png)";
        //Pattern pattern = Pattern.compile("!\\[([\\w\\u4e00-\\u9fa5]*)]\\(([./\\w\\u4e00-\\u9fa5]+)\\)");
        //Matcher matcher = pattern.matcher(input2);
        //
        //if (matcher.find()) {
        //    System.out.println("图片整体: " + matcher.group(0));
        //    System.out.println("图片名称: " + matcher.group(1));
        //    System.out.println("图片路径: " + matcher.group(2));
        //    System.out.println();
        //}

        System.out.println("请输入一个文件夹(可拖入)，程序自动解析其中的md文件");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        if (input.startsWith("\"")) {
            input = input.replaceAll("\"", "");
        }
        System.out.println(input);
        File dir = new File(input);
        if (dir.isDirectory()) {
            try {
                List<File> mdFiles = new ArrayList<>(FileUtils.listFiles(dir, new String[]{"md"}, true));

                if (!mdFiles.isEmpty()) {
                    for (File file : mdFiles) {
                        String sourceText = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        List<FileEntity> list = readMarkDown(file, sourceText);
                        File target = new File(
                                file.getParent(),
                                String.format("%s - base64.md",
                                        FilenameUtils.getBaseName(file.getName())
                                )
                        );
                        FileUtils.copyFile(file, target);
                        writeNewMarkDown(target, sourceText, list);
                        System.out.printf("%s 文件已处理完成，目标文件是 %s\n",
                                file.getName(),
                                target.getName()
                        );
                    }
                } else {
                    System.out.println("文件夹没有文件");
                    System.exit(-1);
                }
            } catch (IOException e) {
                System.err.printf("错误：%s,\n源自：%s\n", e.getMessage(), e.getCause());
                throw new RuntimeException(e);
            }
        }
    }

    public static Pattern mdPattern = Pattern.compile("!\\[(.*)]\\((.+)\\)");

    public static Pattern h5Pattern = Pattern.compile("<img.*src=\"(.*)\".*/>");

    public static List<FileEntity> readMarkDown(File file, String sourceText) throws IOException {
        List<FileEntity> result = new ArrayList<>();
        Matcher matcher = mdPattern.matcher(sourceText);
        int i = 1;
        String parentDir = file.getParent();
        while (matcher.find()) {
            FileEntity fileEntity = new FileEntity();
            fileEntity.setId(i++);
            fileEntity.setTargetName(String.format("images%d", fileEntity.getId()));
            fileEntity.setSource(matcher.group(0));
            fileEntity.setName(matcher.group(1));
            fileEntity.setPath(matcher.group(2).replace("%20", " "));
            byte[] data = Files.readAllBytes(
                    Paths.get(parentDir + "\\" + fileEntity.getPath()));
            String base64Image =
                    String.format(
                            "\n\n\n[%s]:data:image/jpeg;base64,",
                            fileEntity.getTargetName()
                    ) + new String(Base64.getEncoder().encode(data));
            fileEntity.setTarget(String.format("![%s][%s]", fileEntity.getName(), fileEntity.getTargetName()));
            base64Image = base64Image.replaceAll("\r\n", "");
            fileEntity.setBase64Target(base64Image);
            //File tempfile = new File(System.getProperty("user.dir") + "\\log.md");
            //tempfile.createNewFile();
            //Files.write(tempfile.toPath(), base64Image.getBytes(), StandardOpenOption.APPEND);
            result.add(fileEntity);
        }
        return result;
    }

    public static void writeNewMarkDown(File file, String sourceText, List<FileEntity> entityList) throws IOException {
        Path path = file.toPath();
        String text = sourceText;
        BufferedWriter writer = Files.newBufferedWriter(path);
        for (FileEntity entity : entityList) {
            String source = entity.getSource();
            if (sourceText.contains(source)) {
                text = text.replace(source, entity.getTarget());
            }
        }
        writer.write(text);
        writer.flush();
        for (FileEntity entity : entityList) {
            writer.write(entity.getBase64Target());
        }
        writer.flush();
        writer.close();

    }

}
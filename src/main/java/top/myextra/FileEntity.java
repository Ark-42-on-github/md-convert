package top.myextra;

import java.util.Objects;

public class FileEntity {
    /**
     * id 目标名称要用
     */
    private Integer id;
    /**
     * 名称，正则匹配出来的名称，target仍用这一名称
     * 如果源 ![图片1](./images/1.jpg)
     * 此项为 图片1
     */
    private String name;
    /**
     * 目标名称，与id混合后生成在后面的名称
     * 如过源为 ![图片1](./images/1.jpg)
     * 改成目标后 ![图片1][images1]
     * 此项为 images1
     * id为 1
     */
    private String targetName;
    /**
     * 该文件的相对路径
     * 如果源为 ![图片1](./images/1.jpg)
     * 此项为 ./images/1.jpg
     */
    private String path;
    /**
     * 就是正则匹配的源，之后替换用到
     * 如果源为 ![图片1](./images/1.jpg)
     */
    private String source;
    /**
     * 就是正则匹配的目标，之后替换为它
     * 如果源为 ![图片1](./images/1.jpg)
     * 改成目标后 ![图片1][images1]
     * 此项为 ![图片1][images1]
     * id为 1
     */
    private String target;
    /**
     * 就是添加到md文件末尾的模版字符串
     */
    private String base64Target;

    public FileEntity() {
    }

    public FileEntity(Integer id, String name, String targetName, String path, String source, String target, String base64Target) {
        this.id = id;
        this.name = name;
        this.targetName = targetName;
        this.path = path;
        this.source = source;
        this.target = target;
        this.base64Target = base64Target;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getBase64Target() {
        return base64Target;
    }

    public void setBase64Target(String base64Target) {
        this.base64Target = base64Target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntity that = (FileEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(targetName, that.targetName) && Objects.equals(path, that.path) && Objects.equals(source, that.source) && Objects.equals(target, that.target) && Objects.equals(base64Target, that.base64Target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, targetName, path, source, target, base64Target);
    }

    @Override
    public String toString() {
        return "FileEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", targetName='" + targetName + '\'' +
                ", path='" + path + '\'' +
                ", source='" + source + '\'' +
                ", target='" + target + '\'' +
                ", base64Target='过长，仅展示100char:" + base64Target.substring(0,100) + '\'' +
                '}';
    }
}

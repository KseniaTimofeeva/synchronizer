package ru.ifmo.diploma.synchronizer;

/**
 * Created by Юлия on 12.06.2017.
 */
public class FileOperation {
    OperationType type;
    String fileRelativePath;

    public FileOperation(OperationType type, String fileRelativePath){
        this.type=type;
        this.fileRelativePath=fileRelativePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileOperation that = (FileOperation) o;

        if (type != that.type) return false;
        return fileRelativePath.equals(that.fileRelativePath);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + fileRelativePath.hashCode();
        return result;
    }
}

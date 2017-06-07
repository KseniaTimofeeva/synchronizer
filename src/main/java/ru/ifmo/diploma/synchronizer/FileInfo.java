package ru.ifmo.diploma.synchronizer;

import java.io.*;

/**
 * Created by Юлия on 19.05.2017.
 */
public class FileInfo implements Externalizable{
    private String relativePath;
    private long creationDate;
    private long updateDate;
    private long length;
    private String checkSum;

    public FileInfo(){}

    public FileInfo(String relativePath, long creationDate, long updateDate, long length, String checkSum) {
        this.relativePath = relativePath;
        this.creationDate = creationDate;
        this.updateDate = updateDate;
        this.length = length;
        this.checkSum=checkSum;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(relativePath);
        out.writeLong(creationDate);
        out.writeLong(updateDate);
        out.writeLong(length);
        out.writeObject(checkSum);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        relativePath=in.readObject().toString();
        creationDate=in.readLong();
        updateDate=in.readLong();
        length=in.readLong();
        checkSum=in.readObject().toString();

    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public long getUpdateDate() {
        return updateDate;
    }

    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "{Path:" + relativePath +
                ", Creation date:" + creationDate +
                ", Last modification date: " + updateDate +
                ", Size: " + length +
                ", CheckSum: " +checkSum+"}";
    }

    @Override
    public int hashCode() {
        int result = relativePath.hashCode();
        result = 31 * result + checkSum.hashCode();
        result = (int) (31 * result + creationDate);
        result = (int) (31 * result + updateDate);
        result = (int) (31 * result + length);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FileInfo fi = (FileInfo) obj;

        if (length != fi.length) return false;
        if (!checkSum.equals(fi.checkSum)) return false;
        if (creationDate != fi.creationDate) return false;
        if (updateDate != fi.updateDate) return false;
        return relativePath.equals(fi.relativePath);
    }
}


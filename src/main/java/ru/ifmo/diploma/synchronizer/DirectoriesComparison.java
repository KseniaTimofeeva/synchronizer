package ru.ifmo.diploma.synchronizer;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/*
 * Created by Юлия on 19.05.2017.
 */
public class DirectoriesComparison {
    public List<FileInfo> filesInfo = new ArrayList<>();
    public List<FileInfo> prevDirState = new ArrayList<>();
    public final String startPath;

    public DirectoriesComparison(String startPath) {
        this.startPath = startPath;
    }


    //записывает информацию обо всех файлах в список
    void getListFiles(String path) throws IOException, NoSuchAlgorithmException {
        File dir = new File(path);
        List<File> files = Arrays.asList(dir.listFiles());
        for (File f : files) {
            if (f.isDirectory())
                getListFiles(f.getAbsolutePath());
            else if (!f.getName().equals("log.bin")) {
                BasicFileAttributes attrs = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                String relativePath = f.getPath().substring(startPath.length() + 1);
                filesInfo.add(new FileInfo(relativePath, attrs.creationTime().toMillis(), f.lastModified(), f.length(), getCheckSum(f.toPath())));
            }
        }
    }

    private String getAbsolutePath(String relativePath) {
        return startPath + "\\" + relativePath;
    }

    private void setCreationTime(String fileName, long creationTime) {
        FileTime newCreationTime = FileTime.fromMillis(creationTime);
        Path path = Paths.get(fileName);
        try {
            Files.setAttribute(path, "basic:creationTime", newCreationTime, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void transferFile(String oldRelativePath, String newRelativePath) {
        Path oldPath = Paths.get(getAbsolutePath(oldRelativePath));
        Path newPath = Paths.get(getAbsolutePath(newRelativePath));

        try {
            Files.move(oldPath, newPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void renameFile(String oldRelativePath, String newRelativePath) {
        File oldFile = new File(getAbsolutePath(oldRelativePath));
        File newFile = new File(getAbsolutePath(newRelativePath));
        if (!oldFile.renameTo(newFile))
            System.err.println("Renaming failed");

    }

    private void copyFile(String oldRelativePath, String newRelativePath, long creationDate) {
        Path oldPath = Paths.get(getAbsolutePath(oldRelativePath));
        Path newPath = Paths.get(getAbsolutePath(newRelativePath));
        try {
            Files.copy(oldPath, newPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setCreationTime(newPath.toString(), creationDate);

    }

    private String getCheckSum(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        try (InputStream is = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(is, md)) {
            int l;
            byte[] buf = new byte[1024];
            while ((l = dis.read(buf)) > 0) {
                md.update(buf, 0, l);
            }

        }
        byte[] digest = md.digest();

        return (new BigInteger(1, digest)).toString(32);
    }

    void compareDirectories(List<FileInfo> l1, List<FileInfo> l2) {
        boolean isFound;
        Iterator<FileInfo> iter1 = l1.iterator();

        while (iter1.hasNext()) {
            FileInfo fi1 = iter1.next();
            isFound = false;
            Iterator<FileInfo> iter2 = l2.iterator();

            while (iter2.hasNext()) {
                FileInfo fi2 = iter2.next();

                if (fi1.equals(fi2)) {
                    //files are same, nothing to do
                    isFound = true;
                    break;
                }

                if (fi1.getCheckSum().equals(fi2.getCheckSum()) && fi1.getCreationDate() == fi2.getCreationDate()
                        && fi1.getUpdateDate() == fi2.getUpdateDate()) {
                    isFound = true;

                    int finishIndex1 = fi1.getRelativePath().lastIndexOf("\\") > 0 ? fi1.getRelativePath().lastIndexOf("\\") : 0;
                    int finishIndex2 = fi2.getRelativePath().lastIndexOf("\\") > 0 ? fi2.getRelativePath().lastIndexOf("\\") : 0;
                    if (fi1.getRelativePath().substring(0, finishIndex1).equals(fi2.getRelativePath().substring(0, finishIndex2))) {
                        //file was renamed
                        //send msg RENAME

                    } else {
                        //file was transferred
                        //send msg: TRANSFER

                    }

                    break;
                }

                if (fi1.getCheckSum().equals(fi2.getCheckSum())) {
                    isFound = true;
                    //file was copied
                    //send msg COPY
                    break;
                }

                if (fi1.getCreationDate() == fi2.getCreationDate()) {
                    if (fi1.getUpdateDate() > fi2.getUpdateDate()) {
                        //file was modified on source
                        //send msg SEND
                        //send modified file
                        isFound = true;
                        break;
                    }
                }

            }
            //Не было совпадения для файла=> ищем его в списке с прошлой версией
            if (!isFound) {

                for (FileInfo prevFI : prevDirState) {
                    if (fi1.getCheckSum().equals(prevFI.getCheckSum())) {
                        isFound = true;
                        try {
                            //file was deleted on another host
                            //delete file on source
                            Files.deleteIfExists(Paths.get(getAbsolutePath(fi1.getRelativePath())));
                            iter1.remove();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
                if (!isFound) {
                    //file was created on source
                    //send msg SEND
                    //sendFile(new File(getAbsolutePath(fi1.getRelativePath())));
                }
            }

        }

    }

    void saveDirectoryState(String path, List<FileInfo> files) {
        //Запись списка файлов в
        try (OutputStream out = new FileOutputStream(path);
             ObjectOutputStream objFileOut = new ObjectOutputStream(out)) {
            for (FileInfo fi : files) {
                objFileOut.writeObject(fi);
                objFileOut.flush();
                //objFileOut.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    List<FileInfo> getDirectoryState(String path) {
        List<FileInfo> result = new ArrayList<>();

        try (InputStream fin = new FileInputStream(path);
             ObjectInputStream oin = new ObjectInputStream(fin)) {


            while (fin.available() > 0) {
                FileInfo fi = (FileInfo) oin.readObject();
                result.add(fi);
            }


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

}

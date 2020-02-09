package de.nomagic;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class FileServer extends Thread
{
    private static final int MAX_FOLDER_LEVEL = 15;
    private static final int MAX_FILES_IN_FOLDER = 300;  // does not count the up to 256 folders

    private int ServerPort = 4321;
    private DataOutputStream toClient;
    private boolean hashTree = false;
    private String path = "./"; // location on disc where hash tree starts "" == folder that we have been started from.

    public FileServer()
    {
    }

    public static void main(String[] args)
    {
        FileServer m = new FileServer();
        m.getConfigFromCommandLine(args);
        m.start();
    }

    private void printHelpText()
    {
        System.err.println("Parameters:");
        System.err.println("===========");
        System.err.println("-h");
        System.err.println("     : This text");
        System.err.println("-hashTree <path of root folder>");
        System.err.println("     : distribute files into subfolders based on hash value.");
        System.err.println("-port <port number>");
        System.err.println("     : use the given port instead of the default port " + ServerPort);
    }

    public void getConfigFromCommandLine(String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            if(true == args[i].startsWith("-"))
            {
                if(true == "-port".equals(args[i]))
                {
                    i++;
                    ServerPort = Integer.parseInt(args[i]);
                }
                else if(true == "-hashTree".equals(args[i]))
                {
                    hashTree = true;
                    i++;
                    path = args[i];
                }
                else if(true == "-h".equals(args[i]))
                {
                    printHelpText();
                    System.exit(0);
                }
                else
                {
                    System.err.println("Invalid Parameter : " + args[i]);
                    printHelpText();
                    System.exit(2);
                }
            }
            else
            {
                System.err.println("Invalid Parameter : " + args[i]);
                printHelpText();
                System.exit(1);
            }
        }
    }

    private String getFolderNameOfLevel(int Level, byte[] hash)
    {
        StringBuffer res = new StringBuffer();
        byte b = hash[Level];
        int p = ((b & 0xf0)>>4);
        switch(p)
        {
        case  0: res.append('0');break;
        case  1: res.append('1');break;
        case  2: res.append('2');break;
        case  3: res.append('3');break;
        case  4: res.append('4');break;
        case  5: res.append('5');break;
        case  6: res.append('6');break;
        case  7: res.append('7');break;
        case  8: res.append('8');break;
        case  9: res.append('9');break;
        case 10: res.append('a');break;
        case 11: res.append('b');break;
        case 12: res.append('c');break;
        case 13: res.append('d');break;
        case 14: res.append('e');break;
        case 15: res.append('f');break;
        }

        p = (b & 0x0f);
        switch(p)
        {
        case  0: res.append('0');break;
        case  1: res.append('1');break;
        case  2: res.append('2');break;
        case  3: res.append('3');break;
        case  4: res.append('4');break;
        case  5: res.append('5');break;
        case  6: res.append('6');break;
        case  7: res.append('7');break;
        case  8: res.append('8');break;
        case  9: res.append('9');break;
        case 10: res.append('a');break;
        case 11: res.append('b');break;
        case 12: res.append('c');break;
        case 13: res.append('d');break;
        case 14: res.append('e');break;
        case 15: res.append('f');break;
        }
        res.append('/');
        return res.toString();
    }

    private String hexDump(byte[] data)
    {
        StringBuffer res = new StringBuffer();
        for(int i = 0; i < data.length; i++)
        {
            byte b = data[i];
            int p = ((b & 0xf0)>>4);
            switch(p)
            {
            case  0: res.append('0');break;
            case  1: res.append('1');break;
            case  2: res.append('2');break;
            case  3: res.append('3');break;
            case  4: res.append('4');break;
            case  5: res.append('5');break;
            case  6: res.append('6');break;
            case  7: res.append('7');break;
            case  8: res.append('8');break;
            case  9: res.append('9');break;
            case 10: res.append('A');break;
            case 11: res.append('B');break;
            case 12: res.append('C');break;
            case 13: res.append('D');break;
            case 14: res.append('E');break;
            case 15: res.append('F');break;
            }

            p = (b & 0x0f);
            switch(p)
            {
            case  0: res.append('0');break;
            case  1: res.append('1');break;
            case  2: res.append('2');break;
            case  3: res.append('3');break;
            case  4: res.append('4');break;
            case  5: res.append('5');break;
            case  6: res.append('6');break;
            case  7: res.append('7');break;
            case  8: res.append('8');break;
            case  9: res.append('9');break;
            case 10: res.append('A');break;
            case 11: res.append('B');break;
            case 12: res.append('C');break;
            case 13: res.append('D');break;
            case 14: res.append('E');break;
            case 15: res.append('F');break;
            }
            res.append(' ');
        }
        return res.toString();
    }

    private String hasFile(String fileName, String archivePath, int level, byte[] hash)
    {
        if(MAX_FOLDER_LEVEL == level)
        {
            File f = new File(archivePath + fileName);
            if(true == f.exists())
            {
                System.out.println("found the file in level 16 !");
                return archivePath + fileName;
            }
            else
            {
                System.out.println("file is not in level 16");
                return null;
            }
        }

        // search in this folder
        File f = new File(archivePath + fileName);
        if(true == f.exists())
        {
            System.out.println("found the file here: " + archivePath + fileName);
            return archivePath + fileName;
        }
        else
        {
            System.out.println("file is not in folder " + archivePath + " (Level : " + level + ")");
            // maybe one layer down?
            String folderName = getFolderNameOfLevel(level, hash);
            File ff = new File(archivePath + folderName);
            if (true == ff.isDirectory())
            {
                // folder exists -> search there
                return hasFile(fileName, archivePath + folderName, level + 1, hash);
            }
            else
            {
                // no idea where to look
                System.out.println("No subdirectory found");
                return null;
            }
        }
    }

    private int getNumFilesInFolder(String Path)
    {
        File f = new File(Path);
        File[] files = f.listFiles();
        if (files != null)
        {
            System.out.println("files.length : " + files.length);
            int res = 0;
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                if (file.isDirectory())
                {
                    // skip
                }
                else
                {
                    res++;
                }
            }
            return res;
        }
        else
        {
            System.err.println("ERROR : Could not get the Files in the folder " + Path);
            return 0;
        }
    }


    private void pushOneLevelDown(int level, String path) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        if(level >= MAX_FOLDER_LEVEL)
        {
            System.err.println("ERROR: can not push deeper: already at maximum Level!");
            System.err.println("ERROR : at level : " + level + " path: " + path);
            return;
        }

        File f = new File(path);
        File[] files = f.listFiles();
        if (files != null)
        {
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                if (file.isDirectory())
                {
                    // skip
                }
                else
                {
                    String name = file.getName();
                    byte[] f_hash = getHashFor(name);
                    String f_folder = getFolderNameOfLevel(level, f_hash);
                    File TargetFolder = new File(path + f_folder);
                    if(false == TargetFolder.isDirectory())
                    {
                        TargetFolder.mkdir();
                    }
                    System.out.println("moving the file " + path + name + " to " + path + f_folder + name);
                    if(false == file.renameTo(new File(path + f_folder + name)))
                    {
                        System.err.println("ERROR: Could not move the file " + name + " to the folder " + path + f_folder + name);
                    }
                }
            }
        }
    }

    private String storeFile(String fileName, String archivePath, int level, byte[] hash) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        if(MAX_FOLDER_LEVEL == level)
        {
            // used all the hash -> store file here
            System.out.println("storing file to " + archivePath + fileName);
            return archivePath + fileName;
        }

        String folderName = getFolderNameOfLevel(level, hash);
        File f = new File(archivePath + folderName);
        if(true == f.isDirectory())
        {
            // folder exists -> store there
            return storeFile(fileName, archivePath + folderName, level + 1, hash);
        }
        else
        {
           // if folder is not overfilled, then store here
            int filesInFolder = getNumFilesInFolder(archivePath);
            System.out.println("filesInFolder : " + filesInFolder);
            if(MAX_FILES_IN_FOLDER > filesInFolder)
            {
                // store here
                System.out.println("storing file to " + archivePath + fileName);
                return archivePath + fileName;
            }
           else
           {
               // create sub folder and store it there
               if(false == f.mkdir())
               {
                   System.err.println("ERROR: Could not create folder " + archivePath + folderName);
                   return null;
               }
               String res = storeFile(fileName, archivePath + folderName, level + 1, hash);
               // cleanup this folder
               System.out.println("Pushing files into subfolders");
               pushOneLevelDown(level + 1, archivePath);
               return res;
           }
        }
    }

    private byte[] getHashFor(String filename) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] bytes = filename.getBytes("utf-8");
        // System.out.println("Hashing  " + hexDump(bytes));
        return md5.digest(bytes);
    }


    private void handle_cmd_has(RequestVersion2 request) throws IOException, NoSuchAlgorithmException
    {
        if(true == hashTree)
        {
            String filename = request.getFile();
            System.out.println("FileName is -" + filename + "- !");
            byte[] hash = getHashFor(filename);
            System.out.println("Hash is " + hexDump(hash));
            if(null != hasFile(filename, path, 0, hash))
            {
                toClient.writeBytes("2:0:\n");
            }
            else
            {
                toClient.writeBytes("2:4:\n");
            }
        }
        else
        {
            File f = new File(request.getFile());
            if(true == f.exists())
            {
                toClient.writeBytes("2:0:\n");
            }
            else
            {
                toClient.writeBytes("2:4:\n");
            }
        }
    }

    private void handle_cmd_get(RequestVersion2 request) throws IOException, NoSuchAlgorithmException
    {
        File f;
        if(true == hashTree)
        {
            String filename = request.getFile();
            System.out.println("FileName is -" + filename + "- !");
            byte[] hash = getHashFor(filename);
            System.out.println("Hash is " + hexDump(hash));
            String location = hasFile(filename, path, 0, hash);
            if(null == location)
            {
                // we do not have the file
                toClient.writeBytes("2:4:\n");
                return;
            }
            else
            {
                f = new File(location);
            }
        }
        else
        {
            // not hash Tree
            f = new File(request.getFile());
        }
        if(true == f.exists())
        {
            long filesize = f.length();
            if(false == f.canRead())
            {
                toClient.writeBytes("2:4:\n");
            }
            else
            {
                byte[] buf = new byte[4096];
                int num;
                toClient.writeBytes("2:0:fileContentLength=" + filesize + ":\n");
                FileInputStream fin = new FileInputStream(f);
                do {
                    num = fin.read(buf);
                    if(num > 0)
                    {
                        toClient.write(buf, 0, num);
                    }
                } while(num > 0);
                fin.close();
                toClient.writeBytes("2:\n");
                System.out.println("send the file " + request.getFile() + " to the client " + request.getClientId());
            }
        }
        else
        {
            toClient.writeBytes("2:4:\n");
        }
    }

    private void handle_cmd_store(RequestVersion2 request) throws IOException, NoSuchAlgorithmException
    {
        File f;
        if(true == hashTree)
        {
            String location = null;
            String filename = request.getFile();
            System.out.println("FileName is -" + filename + "- !");
            byte[] hash = getHashFor(filename);
            System.out.println("Hash is " + hexDump(hash));
            location = storeFile(filename, path, 0, hash);
            if(null == location)
            {
                toClient.writeBytes("2:3:\n");
                return;
            }
            else
            {
                f = new File(location);
            }
        }
        else
        {
            // no hash Tree
            f = new File(request.getFile());
        }
        if(true == f.exists())
        {
            toClient.writeBytes("2:3:\n");
        }
        else
        {
            if(true == request.writeTo(f))
            {
                toClient.writeBytes("2:0:\n");
                System.out.println("stored the file " + request.getFile() + " from the client " + request.getClientId());
            }
            else
            {
                f.delete();
                toClient.writeBytes("2:3:\n");
            }
        }
    }

    private void handle_cmd_update(RequestVersion2 request) throws IOException, NoSuchAlgorithmException
    {
        File f;
        if(true == hashTree)
        {
            String filename = request.getFile();
            System.out.println("FileName is -" + filename + "- !");
            byte[] hash = getHashFor(filename);
            System.out.println("Hash is " + hexDump(hash));
            String location = hasFile(filename, path, 0, hash);
            if(null == location)
            {
                // we do not have the file
                toClient.writeBytes("2:4:\n");
                return;
            }
            else
            {
                f = new File(location);
            }
        }
        else
        {
            // not hash Tree
            f = new File(request.getFile());
        }
        if(true == f.exists())
        {
            if(false == f.delete())
            {
                toClient.writeBytes("2:6:\n");
            }
            else
            {
                if(true == request.writeTo(f))
                {
                    toClient.writeBytes("2:0:\n");
                    System.out.println("updated the file " + request.getFile() + " from the client " + request.getClientId());
                }
                else
                {
                    f.delete();
                    toClient.writeBytes("2:6:\n");
                }
            }
        }
        else
        {
            toClient.writeBytes("2:4:\n");
        }
    }

    @Override
    public void run()
    {
        boolean shouldRun = true;
        // Startup

        ServerSocket welcomeSocket;
        try
        {
            welcomeSocket = new ServerSocket(ServerPort);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }

        while((false == isInterrupted()) && (true == shouldRun))
        {
            try
            {
                final Socket connectionSocket = welcomeSocket.accept();
                InputStream fromClient = connectionSocket.getInputStream();
                toClient = new DataOutputStream(connectionSocket.getOutputStream());
                RequestVersion2 request = RequestVersion2.receiveRequestFrom(fromClient);
                if(null != request)
                {
                    if(true == request.isValid())
                    {
                        String cmd = request.getCommand();
                        if(true == "has".equals(cmd))
                        {
                            handle_cmd_has(request);
                        }
                        else if(true == "get".equals(cmd))
                        {
                            handle_cmd_get(request);
                        }
                        else if(true == "store".equals(cmd))
                        {
                            handle_cmd_store(request);
                        }
                        else if(true == "update".equals(cmd))
                        {
                            handle_cmd_update(request);
                        }
                        else
                        {
                            System.out.println("ERROR: unknown command (" + cmd + ") !");
                            toClient.writeBytes("2:1:ERROR: invalid Command !!!:");
                        }
                    }
                    else
                    {
                        System.out.println("ERROR: Request invalid!");
                        toClient.writeBytes("2:1:ERROR: invalid Command !!!:");
                    }
                }
                toClient.flush();
                connectionSocket.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            welcomeSocket.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        // Shutdown
        System.out.println("Done!");
        System.exit(0);
    }

}

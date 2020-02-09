package de.nomagic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class RequestVersion2
{
    private String name = "";
    private String type = "";
    private boolean valid = false;
    private String command = "";
    private String fileName = "";
    private long fileSize = 0;
    private InputStream stream;

    public static RequestVersion2 receiveRequestFrom(InputStream fromClient) throws IOException
    {
        return new RequestVersion2(fromClient);
    }

    private String[] readDataSections() throws IOException
    {
        ArrayList<String> res = new ArrayList<String>();
        StringBuffer buf = new StringBuffer();
        int b = stream.read();
        while((-1 != b) && (0x0 != b))
        {
            if('\n' == b)
            {
                // end of Data sections
                break;
            }
            else if(':' == b)
            {
                // end of data section
                String curSection = buf.toString();
                res.add(curSection);
                buf = new StringBuffer();
            }
            else
            {
                buf.append((char)b);
            }
            b = stream.read();
        }
        return res.toArray(new String[0]);
    }

    private RequestVersion2(InputStream dataSource) throws IOException
    {
        int b;
        valid = false;
        this.stream = dataSource;
        b = stream.read();
        if('2' != b)
        {
            // protocol error
            return;
        }
        b = stream.read();
        if(':' != b)
        {
            // protocol error
            return;
        }
        String[] parts = readDataSections();
        if(1 > parts.length)
        {
            // protocol error
            return;
        }
        command = parts[0];
        for(int i = 1; i < parts.length; i++)
        {
            String line = parts[i];
            line = line.trim();
            if(line.startsWith("name"))
            {
                name = line.substring(line.indexOf('=') + 1);
                name = name.trim();
            }
            else if(line.startsWith("type"))
            {
                type = line.substring(line.indexOf('=') + 1);
                type = type.trim();
            }
            else if(line.startsWith("fileContentLength"))
            {
                String fs = line.substring(line.indexOf('=') + 1);
                fs = fs.trim();
                fileSize = Long.parseLong(fs);
            }
            else if(line.startsWith("file"))
            {
                fileName = line.substring(line.indexOf('=') + 1);
                fileName = fileName.trim();
            }
        }
        valid = true;
    }

    public String getType()
    {
        return type;
    }

    public String getClientId()
    {
        return name;
    }

    public boolean isValid()
    {
        return valid;
    }

    public String getCommand()
    {
        return command;
    }

    public String getFile()
    {
        return fileName;
    }

    public boolean writeTo(File f) throws IOException
    {
        // File content + "2:\n"
        long bytesToGo = fileSize;
        byte[] buf = new byte[4096];
        int num;
        FileOutputStream fout = new FileOutputStream(f);
        do {
            int maxLen = 4096;
            if(bytesToGo < 4096)
            {
                maxLen = (int)bytesToGo;
            }

            num = stream.read(buf, 0, maxLen);
            if(num > 0)
            {
                bytesToGo = bytesToGo - num;
                // pure file content
                fout.write(buf, 0, num);
            }
        } while((num > 0) && (bytesToGo > 0));
        int sig = stream.read();
        if('2' != sig)
        {
            fout.close();
            return false;
        }
        sig = stream.read();
        if(':' != sig)
        {
            fout.close();
            return false;
        }
        sig = stream.read();
        if('\n' != sig)
        {
            fout.close();
            return false;
        }
        fout.flush();
        fout.close();
        return true;
    }

}

package de.nomagic;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;



public class FileServer extends Thread
{
    private int ServerPort = 4321;

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
                DataOutputStream toClient = new DataOutputStream(connectionSocket.getOutputStream());
                RequestVersion2 request = RequestVersion2.receiveRequestFrom(fromClient);
                if(null != request)
                {
                    if(true == request.isValid())
                    {
                        String cmd = request.getCommand();
                        if(true == "has".equals(cmd))
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
                        else if(true == "get".equals(cmd))
                        {
                            File f = new File(request.getFile());
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
                        else if(true == "store".equals(cmd))
                        {
                            File f = new File(request.getFile());
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
                        else if(true == "update".equals(cmd))
                        {
                            File f = new File(request.getFile());
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

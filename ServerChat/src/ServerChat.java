import javax.swing.text.html.HTMLDocument;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class ServerChat {

    ArrayList clientOutputStream;

    public static void main(String[] args) {
        new ServerChat().go();
    }


    public class ClientHandler implements Runnable {

        BufferedReader reader;
        Socket sock;

        public ClientHandler(Socket clientSock){
           sock = clientSock;
            try {
                InputStreamReader input = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

        }
    }

    public void tellEveryone(String message){
        Iterator it = clientOutputStream.iterator();

        while(it.hasNext()){
            PrintWriter writer = (PrintWriter)it.next();
            writer.println(message);
            writer.flush();
        }
    }

    public void go(){

        try {
            ServerSocket serverSocket = new ServerSocket(5000);

            while(true) {
                Socket clientSock = serverSocket.accept();
                PrintWriter writer = new PrintWriter(clientSock.getOutputStream());
                clientOutputStream.add(writer);

                Thread t = new Thread(new ClientHandler(clientSock));
                t.start();

                System.out.println("got a connection");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

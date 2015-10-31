package epsem.walkinghealth;

import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class ServerUploader {
    public String urlServer = "http://10.42.0.1/handle_data.php";
    public String boundary = "*****";
    public URL url;
    public HttpURLConnection connection;

    public FileInputStream fileInputStream;
    public File pathToOurFile;

    public int bytesRead, bytesAvailable, bufferSize;
    public byte[] buffer;
    public int maxBufferSize = 1 * 1024 * 1024;

    protected void doInBackground(){
        //Thread que s'executa en background, accepta el pas de parametres
        //HTTP Post - Connexió persistent
       try {
           StartConnection();
       }catch (MalformedURLException urle){
           Log.e("Server", "MalformedURLException");
       }catch(ProtocolException pe){
           Log.e("Server", "ProtocolException");
       }catch (IOException ioe){
           Log.e("Server", "IOException when connecting");
       }

        //Lectura del fitxer
        try{
            readFile();
        }catch(FileNotFoundException fnotfound){
            Log.e("Server","file not found");
        }catch(IOException ioe){
            Log.e("Server","IOException when reading");
        }

        //Canal de sortida
        DataOutputStream outputStream = null;
        try {
            outputStream = new DataOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            Log.e("Server","Does not support writing to the output stream");
        }

        try {
            outputStream.writeBytes("--" + boundary + "\r\n");
        } catch (IOException e) {
            Log.e("Server", "can not writebytes() 1");
        }

        try {
            outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile + "\"" + "\r\n");
        } catch (IOException e) {
            Log.e("Server","can not writebytes() 2");
        }

        try {
            outputStream.writeBytes("\r\n");
        } catch (IOException e) {
            Log.e("Server","can not writebytes() 3");
        }

        //Transmissió fitxer
        while (bytesRead > 0) {
            try {
                outputStream.write(buffer, 0, bufferSize);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                bytesAvailable = fileInputStream.available();
            } catch (IOException e) {
                e.printStackTrace();
            }

            bufferSize = Math.min(bytesAvailable, maxBufferSize);

            try {
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            outputStream.writeBytes("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputStream.writeBytes("--" + boundary + "--" + "\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void StartConnection() throws MalformedURLException, IOException, ProtocolException{
        url = new URL(urlServer);
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
    }

    private void readFile() throws FileNotFoundException, IOException {
        pathToOurFile = new File(Environment.getExternalStorageDirectory(), "dades.txt");
        fileInputStream = new FileInputStream(pathToOurFile);

        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    }

}

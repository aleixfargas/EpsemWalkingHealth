package epsem.walkinghealth;

import android.os.AsyncTask;
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

public class ServerUploader extends AsyncTask<Void, Void, Void> {
    public String urlServer = "http://localhost/prova/index.php";
    public String boundary = "*****";
    public URL url;
    public HttpURLConnection connection;

    public FileInputStream fileInputStream;
    public File pathToOurFile;

    public DataOutputStream outputStream;

    public int bytesRead, bytesAvailable, bufferSize;
    public byte[] buffer;
    public int maxBufferSize = 1*1024*1024;

    @Override
    protected Void doInBackground(Void... params) {
        //Thread que s'executa en background, accepta el pas de parametres
        //HTTP Post - Connexió persistent
       try {

            URL url = new URL(urlServer);
            //HTTP Post - Connexió persistent
            StartConnection();

            //Lectura del fitxer
            pathToOurFile = new File(Environment.getExternalStorageDirectory(), "2015-11-17_data.txt");
            fileInputStream = new FileInputStream(pathToOurFile);
            readFile();

            //Canal de sortida
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputChannel();

            //Transmissió fitxer
            transmitFile();

       }catch (IOException ioe){
           Log.e("Server", "IOException when connecting");
       }
        return null;
    }


    private void StartConnection() throws IOException {

        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
    }

    private void readFile() throws IOException {

        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    }

    private void outputChannel() throws IOException{
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile + "\"" + "\r\n");
        outputStream.writeBytes("\r\n");
    }

    private void transmitFile() throws IOException{
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--" + boundary + "--" + "\r\n");
        Log.d("App", "Server response code: " + connection.getResponseCode());
        Log.d("App", "Server response msg: " + connection.getResponseMessage());
        fileInputStream.close();
        outputStream.flush();
        outputStream.close();
    }
}

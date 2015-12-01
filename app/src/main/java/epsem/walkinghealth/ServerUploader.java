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

//Per connectar mòbil amb server:
//- gksu gedit /etc/NetworkManager/system-connections/WalkingHealth
//mode=ap
//- Posar ip estàtica al mòbil 10.42.0.1 i passarela 10.42.0.255
//- Permisos:
// sudo chown -R www-data:www-data /var/www/


public class ServerUploader extends AsyncTask<Void, Void, Void> {
    public String urlServer = "http://10.42.0.1/prova/index.php";
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
           this.url = new URL(urlServer);
           //HTTP Post - Connexió persistent
           StartConnection();

           //Lectura del fitxer
           pathToOurFile = new File(Environment.getExternalStorageDirectory(), "WalkingHealth/2015-11-17_data.txt");
           Log.e("app","fitxer: "+pathToOurFile);
           fileInputStream = new FileInputStream(pathToOurFile);
           readFile();

           //Canal de sortida
           outputStream = new DataOutputStream(connection.getOutputStream());
           Log.e("App", "outputstream "+outputStream.toString());
           outputChannel();

            //Transmissió fitxer
           transmitFile();


       }catch (IOException ioe){
           Log.e("Server", "IOException when connecting"+ioe);
       }
        return null;
    }


    private void StartConnection() throws IOException {

        connection = (HttpURLConnection) this.url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        connection.setDoOutput(true);
    }

    private void readFile() throws IOException {

        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
//        File f=new File("http://10.42.0.1/prova/uploads"+"/2015-11-17_data.txt");
//        System.out.println(f.exists());
//        fileInputStream = new FileInputStream(f);
//        return "SUCCESS";
    }

    private void outputChannel() throws IOException{
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + pathToOurFile + "\"" + "\r\n");
        outputStream.writeBytes("\r\n");
    }

    private void transmitFile() throws IOException{
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            Log.e("App", "Bytes read: " + buffer.length);
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--" + boundary + "--" + "\r\n");
        Log.e("App", "Server response code: " + connection.getResponseCode());
        Log.e("App", "Server response msg: " + connection.getResponseMessage());
        fileInputStream.close();
        outputStream.flush();
        outputStream.close();
    }
}
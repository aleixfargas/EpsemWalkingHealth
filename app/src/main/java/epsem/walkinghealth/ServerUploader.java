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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

//Per connectar mòbil amb server:
//- gksu gedit /etc/NetworkManager/system-connections/WalkingHealth
//mode=ap
//- Posar ip estàtica al mòbil 10.42.0.2 i passarela 10.42.0.255
//- Permisos:
// sudo chown -R www-data:www-data /var/www/
// Per canviar el limit max de mida de fitxer pujats a server php: upload_max_filesize a sudo nano /etc/php5/apache2/php.ini
// maxima memoria d'un proces android: 16MB aprox. Per augmentar: android:largeHeap="true", no recomanat
//en el meu cas: maxim = 8MB


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

           Calendar calendar = Calendar.getInstance();
           int hour = calendar.get(Calendar.HOUR_OF_DAY);
           int minute = calendar.get(Calendar.MINUTE);

           String now = getStringDateTime();
           String filename = now +"-"+ hour +"-"+ minute + "_data.txt";// Falta provar i canviar el format del writefile
           pathToOurFile = new File(Environment.getExternalStorageDirectory(), "WalkingHealth/"+filename);
           Log.e("app","fitxer: "+pathToOurFile);
           fileInputStream = new FileInputStream(pathToOurFile);
           readFile();

           //Canal de sortida
           outputStream = new DataOutputStream(connection.getOutputStream());
           Log.e("App", "outputstream " + outputStream.toString());
           outputChannel();

            //Transmissió fitxer
           transmitFile();

           //Elimina fitxer
           //if (connection.getResponseCode() ==  OK!){
           boolean deleted = pathToOurFile.delete();
           Log.e("App", "fitxer pujat i eliminat: " + deleted);
           //}

       }catch (IOException ioe){
           Log.e("Server", "IOException when connecting"+ioe);
       }
        return null;
    }

    private String getStringDateTime() {
        // (1) get today's date
        Date today = Calendar.getInstance().getTime();

        // (2) create a date "formatter" (the date format we want)
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // (3) create a new String using the date format we want
        return formatter.format(today);
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
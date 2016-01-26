package epsem.walkinghealth;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import epsem.walkinghealth.models.WriteFileManager_model;

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
    public String urlServer = "http://walkinghealth.pythonanywhere.com/en/reception/do_receive/";
    //public String urlServer = "http://10.42.0.1/prova/index.php";
    public String boundary = "*****";
    public URL url;
    public HttpURLConnection connection;

    public FileInputStream fileInputStream;
    public File folder, file;

    public DataOutputStream outputStream;

    public int bytesRead, bytesAvailable, bufferSize;
    public byte[] buffer;
    public int maxBufferSize = 1*1024*1024;
    public int id = -1;
    private GraphActivity graph_activity = null;
    private WriteFileManager_model WFMmodel = null;

    public ServerUploader(GraphActivity graph_activity){
        this.graph_activity = graph_activity;
        this.WFMmodel = new WriteFileManager_model(this.graph_activity);
    }

    @Override
    protected Void doInBackground(Void... params) {
        //Thread que s'executa en background, accepta el pas de parametres
        //HTTP Post - Connexió persistent
        id = -1;
        try {
            Log.e("SerUpl","init");

            this.url = new URL(urlServer);
           //HTTP Post - Connexió persistent
            StartConnection();
            Log.e("SerUpl", "Trace1");

           //Lectura del fitxer
           folder = new File(Environment.getExternalStorageDirectory(), "WalkingHealth/");
           Log.e("SerUpl","folder: "+folder);
           id = getFileid();
           Log.e("SerUpl","fileId: "+id);
           if(id != -1) {
               file = new File(folder, WFMmodel.getFileName(id));
               if (file.exists()) {
                   fileInputStream = new FileInputStream(file);
                   readFile();

                   //Canal de sortida
                   outputStream = new DataOutputStream(connection.getOutputStream());
                   Log.e("SerUpl", "outputstream " + outputStream.toString());
                   outputChannel();

                   //Transmissió fitxer
                   transmitFile();
                   WFMmodel.setUploaded(id);

                   //Elimina fitxer
                   if (connection.getResponseCode() == 200) {
                       boolean deleted = file.delete();
                       Log.e("SerUpl", "fitxer pujat i eliminat: " + deleted);
                   }
               }
               else {
                   Log.e("SerUpl","set as uploaded");
                   WFMmodel.setUploaded(id);
                   Log.e("SerUpl", "set as uploaded!!!!!");
               }
           }
       }catch (IOException ioe){
           Log.e("SerUpl", "IOException when connecting"+ioe);
       }
        Log.e("SerUpl","return!");
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
    }

    private void outputChannel() throws IOException{
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + file + "\"" + "\r\n");
        outputStream.writeBytes("\r\n");
    }

    private void transmitFile() throws IOException{
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            Log.e("SerUpl", "Bytes read: " + buffer.length);
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--" + boundary + "--" + "\r\n");
        Log.e("SerUpl", "Server response code: " + connection.getResponseCode());
        Log.e("SerUpl", "Server response msg: " + connection.getResponseMessage());
        fileInputStream.close();
        outputStream.flush();
        outputStream.close();
    }


    /**
     *Returns name of file to upload and sets upload == 1
     * @return
     *      String with file name
     */
    private int getFileid(){
        String fitxer = "";
        int id;
        id = WFMmodel.getFilesToUpload();
        Log.e("SerUpl","id_fileToUp = "+id);
        if (id != -1) {
            if (WFMmodel.isDone(id) != 1) {
                id=-1;
            }
            Log.e("SerUpl","fitxer = "+WFMmodel.getFileName(id));
        }
        else{
            Log.e("SerUpl","no hi ha fitxers per pujar"+fitxer);
        }
        return id;
    }
}
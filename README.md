# EpsemWalkingHealth
EPSEM student project to Monitorize the walking of people.

Authors: Adria Fernandez, Vicenç Pio and Aleix Fargas

Company: Epsem-UPC

--------------
Documentation:
--------------
Hem decidit utilitzar l'IDE de l'Android Studio, que es pot descarregar a: https://developer.android.com/sdk/index.html#top

Aquest IDE ens permet utilitzar el sistema de control de versions git, i ens proporciona un esquelet sòlid per a el projecte que durem a terme.

L'app es compondrà de tres parts:

Comunicació Node Sensor (HW)
----------------------------
En aquest apartat desenvoluparem la comunicació entre el Node Sensor i el Node Personal amb la dificultat afegida de que hem de establir conexió bluetooh amb dos nodes. 

Requisits:
És necessita un dispositiu mobil amb el SO Android x.0, versions anteriors no suporten la conexió BLE amb dos altres dispositius.

Per a dur a terme aquest procés, utilitzarem les següents llibreries:  

    bt_av.h: Includes the interface definition for the A2DP profile.
    bt_gatt.h, bt_gatt_client.h, and bt_gatt_server.h: These include the interface definition for the GATT profile.
    bt_hf.h: Includes the interface definition for the HFP profile.
    bt_hh.h: Includes the interface definition for the HID host profile.
    bt_hl.h: Includes the interface definition for the HDP profile.
    bt_mce.h: Includes the interface definition for the MAP profile.
    bt_pan.h: Includes the interface definition for the PAN profile.
    bt_rc.h: Includes the interface definition for the AVRCP profile.
    bt_sock.h: Includes the interface definition for RFCOMM sockets.

Proposem utilitzar inicialment una estrategia simple, només de lectura de dades.

Emmagatzematge temporal de dades del Node Personal
--------------------------------------------------
En aquest apartat, s'han d'emmagatzemar les dades rebudes per els dos Nodes Sensors, i acumular-les fins que tinguem una "bona" conexió WiFi al dispositiu mobil.
Per a tal de dur a terme aquest proces, s'ha de tenir en compte que ens podem trobar en dos casos:

1 - Sense conexió WiFi:
   En aquest cas, anirem acumulant dades al nostre dispositiu, hem de tenir en compte que poden arribar a passar dies fins que no hi haigui conexió, així que s'haura de planejar una bona estructura per tal d'emmagatzemar.
   S'hauria de teniur un protocol per tal de detectar un colapsament del sistema i programar un reboot del Node Sensor per tal d'assegurar una lectura de dades ininterruptuda. 

2 - Amb conexió WiFi:
  En aquest cas, hauriem de ser capaços d'enviar dades de forma continuia, a l'estil cua fifo, buidant les dades emmagatzemades i abocant-les al servidor.
  A la vegada, hauriem de ser capaços de seguir enviant dades de forma prolongada.


Comunicació Node Central (Servidor)
-----------------------------------

-------
TASQUES
-------
Dimecres 22 de setembre del 2015 -> 
- El primer dia, estructuraria el format que haurien de tenir les dades rebudes per el Node Sensor, tenint en compte que inicialment només seran en un sentit (NS->NP) però que idealment, hauria de ser capaç de soportar una comunicació bidireccional.
-Faria un estudi detallat de com dur a terme l'emmagatzemnatge temporal de les dades al Node Personal.
-Defiria com estructurar la BD al servidor, de forma que fos compatible amb les dades que siguem capaços d'extreure del Node Sensor. 

-----------
Referencies
-----------
Infromation about BLE android:

- Android BLE -> https://source.android.com/devices/bluetooth.html
package com.eurekios.shakeme;

import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.reflect.Method;
import java.util.Locale;
import com.android.internal.telephony.ITelephony;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeMeService extends Service implements TextToSpeech.OnInitListener, SensorEventListener{

	SharedPreferences preferencias;
	SharedPreferences.Editor editor;
	String modo_shakeme;
	boolean activar_voz;
	float nivel_sensor;
	boolean compatible = false;

	SensorManager sensorManager;
	float currentAcceleration = 0;
	float maxAcceleration = 0;
	private final double calibration = SensorManager.STANDARD_GRAVITY;
	AudioManager am;
	int volumenNormalVoz=4;
	ITelephony telephonyService;
	PowerManager.WakeLock wl;
	private TextToSpeech mTts;
	String saludo = "123";

	// ID del mensaje de la Barra de notificaciones
	private static final int ID_MEN_BARRA_NOTIF = 1;
	
	// Debug tag, for logging
    static final String TAG = "agitaMe!";


	/****************************************************************************************************************************************************
	 *        SERVICE LIFE-CYCLE																													    *
	 ****************************************************************************************************************************************************
	 * (I) UNBOUNDED SERVICE:				 																											*
	 * 																	      [stopSelf() / stopService()]												*
	 * 																		              |               												*
	 * 																		              |																*
	 * [Call to StartService()] --> onCreate() --> onStartCommand() --> [Service RUNNING] --> onDestroy() --> [Service SHUTDOWN]						*
	 *                             			     																										*
	 **************************************************************************************************************************************************** 
	 * (II) BOUNDED SERVICE:																															*
	 * 																	              [unbindService()]													*
	 * 																		                  |               											*
	 * 																		                  |															*
	 * [Call to StartService()] --> onCreate() --> onBind() --> [Client are bound to service] --> onUnbind() --> onDestroy() --> [Service SHUTDOWN]     *
	 *                             			     																										*
	 ****************************************************************************************************************************************************/
	@Override
	public void onCreate() {
		super.onCreate();

		//iniciar cuenta atras 
		autoDestruccion();

		//mostrar icono en la barra de estado
		notificaciones();

		//leer preferencias
		preferencias = getSharedPreferences("ShakeMePreferencias", Context.MODE_PRIVATE);
		activar_voz = preferencias.getBoolean("voz", false);
		modo_shakeme = preferencias.getString("modo", "colgar");
		nivel_sensor = preferencias.getFloat("nivel_sensor", 1);
		compatible = preferencias.getBoolean("compatible", false);

		desbloquearPantalla();

		if(activar_voz){
			initText2Speech();
		}

		getSensorManager();

		registerAccelerometerListener();	
	} 

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The service is starting, due to a call to startService()
		//obtenemos el numero de la llamada entrante
		Bundle b = intent.getExtras();
		if(b != null){
			String numTelefono = b.getString("MOBILE_NUMBER");
			//TODO:si el telefono esta en mis contactos obtener su nombre
			String callerID = getCallerId(numTelefono);
			if(callerID.equals("Not Found")){//si no esta decir el numero de telefono por digitos
				int tam=numTelefono.length();
				char[] numTel = new char[tam*2];
				for(int i=0, j=0; i<tam*2; i=i+2, j++){
					numTel[i]=numTelefono.charAt(j);
					numTel[i+1]=' ';
				}
				String numTelefono2Voz = new String(numTel);
				saludo = getResources().getString(R.string.llamando)+" "+numTelefono2Voz;
			}else{
				saludo = getResources().getString(R.string.llamando)+" "+callerID;
			}
		}

		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		//notificaciones();
		//volver volumen normal si se activo la voz
		if(activar_voz){
			am.setStreamVolume(AudioManager.STREAM_MUSIC,
					volumenNormalVoz,
					0);
			//am.setSpeakerphoneOn(false);
		}
		// Don't forget to shutdown!
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}

		sensorManager.unregisterListener(this);

		super.onDestroy();
	}

	//Cuenta atras desde que se inicia el servicio para destruirse a si mismo pasado un tiempo
	public void autoDestruccion(){
		int t_espera = 40000;//40s
		int tic_tac = 1000;//1seg
		CountDownTimer temporizador = new CountDownTimer(t_espera, tic_tac) {
			public void onTick(long millisUntilFinished) {
			}
			public void onFinish() {
				ShakeMeService.this.stopSelf();//autodetener el servicio
			}
		}.start();

	}

	/*************************************************************************************/


	/********************************
	 *        TELEFONO
	 ********************************/
	//---------- UNLOCK PANTALLA ----------
	public void desbloquearPantalla(){
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ShakeMeDesbloquear");
		wl.acquire();
		wl.release();
	}
	//---------- RECHAZAR LLAMADA ----------
	private void ignoreCall() {//how to end call
		try {
			telephonyService.endCall();
			// Simula otra pulsaci—n del boton del auricular para finalizar la llamada
			//Intent buttonDown = new Intent(Intent.ACTION_MEDIA_BUTTON);   
			//buttonDown.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
			//sendOrderedBroadcast(buttonDown, "android.permission.CALL_PRIVILEGED");			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private void answerCall() {
		//MODO inCALL
		editor = preferencias.edit();
		editor.putBoolean("inCall", true);
		editor.commit();
		try{
			//--------- CONTESTAR LLAMADA --------
			// HACK: simular auricular con microfono conectado al telefono movil para responder, ya que no funciona el answerRingingCall() ni el silencerRinger() con las nuevas APIS debido a temas de permisos del sistema.
			if(compatible){
				setHeadSetConnectEmulated();
			}
			
			// Simular la pulsaci—n del boton del auricular para contestar la llamada
			// version android < FROYO 2.2
			Intent buttonDown = new Intent(Intent.ACTION_MEDIA_BUTTON);   
			buttonDown.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
			sendOrderedBroadcast(buttonDown, "android.permission.CALL_PRIVILEGED");
			//Log.v(TAG, "KeyEvent.ACTION_DOWN");
			
			// version android >= FROYO 2.2 API 8 el disparador del evento debe ser ACTION_UP en lugar de ACTION_DOWN
			Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);              
			buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
			sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
			//Log.v(TAG, "KeyEvent.ACTION_UP");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//FIX HTC Sense --> HeadsetObserver listening for actual bluetooth plug-in event so you need to send a Intent pretending there is a headset connected already	
	//works on Android < android.os.Build.VERSION_CODES.JELLY_BEAN 4.1 API 16 -> java.lang.SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.HEADSET_PLUG from pid=30943, uid=10064
	private void setHeadSetConnectEmulated(){
		//Log.v(TAG, "Activando Headset del dispositivo=" + android.os.Build.MANUFACTURER +" API="+ android.os.Build.VERSION.SDK_INT);
		if (android.os.Build.VERSION.SDK_INT < 16) {
			Intent headSetUnPluggedintent = new Intent(Intent.ACTION_HEADSET_PLUG);
		    headSetUnPluggedintent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		    headSetUnPluggedintent.putExtra("state", 1); // 0 = unplugged  1 = Headset with microphone 2 = Headset without microphone
		    headSetUnPluggedintent.putExtra("name", "Headset");
		    try {
		    	// TODO: Should we require a permission?
		    	sendOrderedBroadcast(headSetUnPluggedintent, "android.permission.CALL_PRIVILEGED");
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
	}

	/************************************
	 * Buscar telefono en mis CONTACTOS *
	 ************************************/
	private String getCallerId(String incomingNumber) {
		String result = "Not Found";
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, incomingNumber);
		String[] projection = new String[] { ContactsContract.Contacts.DISPLAY_NAME };
		Cursor cursor = getContentResolver().query(lookupUri, projection, null, null, null);
		if (cursor.moveToFirst()) {
			int nameIdx =  cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
			result = cursor.getString(nameIdx);
		}
		cursor.close();

		return result;
	}

	/****************
	 * NOTIFICACION *
	 ****************/
	public void notificaciones(){
		//Obtenemos una referencia al servicio de notificaciones
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notManager = (NotificationManager) getSystemService(ns);

		//----- Configuramos la notificacion que va a aparecer en la barra de estado -----
		int icono = R.drawable.mini_logo_shakeme;
		CharSequence textoEstado = getResources().getString(R.string.app_name);
		long hora = System.currentTimeMillis();
		// Creamos la notificacion
		Notification notificacion = new Notification(icono, textoEstado, hora);

		//----- Configuramos la notificaci—n que va a aparecer en el desplegable de la barra de estado -----
		// Obtenemos el contexto de la aplicacion
		Context contexto = getApplicationContext();
		CharSequence titulo = getResources().getString(R.string.app_name);
		CharSequence descripcion = getResources().getString(R.string.notificacion_ejecucion);
		// Usamos una PendingIntent para crear la notificacion
		Intent notIntent = new Intent(contexto, ConfigActivity.class);
		PendingIntent contIntent = PendingIntent.getActivity(contexto, 0, notIntent, 0);
		// Incluimos la informacion de la notificacion
		notificacion.setLatestEventInfo(contexto, titulo, descripcion, contIntent);

		//notificacion.flags |= Notification.FLAG_AUTO_CANCEL;// cuando se pulsa la notificacion en el desplegable desaparece
		notificacion.flags |= Notification.FLAG_FOREGROUND_SERVICE;// no muestra desplegable solo icono mientras el servicio esta activo
		startForeground(ID_MEN_BARRA_NOTIF, notificacion);
		//notificacion.defaults |= Notification.DEFAULT_SOUND;//sonido
		//notificacion.defaults |= Notification.DEFAULT_VIBRATE;//vibracion
		//notificacion.defaults |= Notification.DEFAULT_LIGHTS;//luces

		//Enviamos la notificacion
		notManager.notify(ID_MEN_BARRA_NOTIF, notificacion);
	}


	/********************************
	 *        TEXTO A VOZ
	 *        -----------
	 * TextToSpeech.OnInitListener.
	 ********************************/
	public void initText2Speech(){
		// Initialize text-to-speech. This is an asynchronous operation.
		// The OnInitListener (second argument) is called after initialization completes.
		mTts = new TextToSpeech(this, this  // TextToSpeech.OnInitListener
				);
	}
	@Override
	public void onInit(int status) {
		// status puede ser TextToSpeech.SUCCESS o TextToSpeech.ERROR.
		if (status == TextToSpeech.SUCCESS) {
			// Establecer el idioma de la voz, puede que no este disponible
			//Locale loc = new Locale("es", "ES");
			Locale loc = Locale.getDefault();
			int result = mTts.setLanguage(loc);
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				// Datos de idioma no encontrados o no disponibles
				Log.e("Text2Speech", "Language is not available.");
			} else {
				hablar();
			}
		} else {
			// Fallo al inicializar Text2Speech
			Log.e("Text2Speech", "Could not initialize TextToSpeech.");
		}
	}
	private void hablar() {	
		//volumen al max.
		am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
		//am.setSpeakerphoneOn(true);
		mTts.speak(saludo, TextToSpeech.QUEUE_FLUSH,  // Drop all pending entries in the playback queue.
				null);
		//am.setSpeakerphoneOn(false);
	}


	/********************************
	 *     SENSOR: ACELEROMETRO     *
	 ********************************/
	public void getSensorManager() {
		//obtener en la variable global sensorManager una instancia del servicio SENSOR_SERVICE
		sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

		//control de volumen
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		volumenNormalVoz = am.getStreamVolume(AudioManager.STREAM_MUSIC);//guardar volumen normal voz del telefono

		//obtener una instancia de ITELEPHONY
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		try {// "cheat" with Java reflection to gain access to TelephonyManager's ITelephony getter
			Class<?> c = Class.forName(tm.getClass().getName());
			Method m = c.getDeclaredMethod("getITelephony");
			m.setAccessible(true);
			telephonyService = (ITelephony) m.invoke(tm);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public void registerAccelerometerListener() {
		//obtener en la variable accelerometer el sensor por defecto de tipo ACCELEROMETER
		int sensorType = Sensor.TYPE_ACCELEROMETER;
		Sensor accelerometer = sensorManager.getDefaultSensor(sensorType);

		//asociar a la variable sensorManager su manejador:sensorEventListener, sensor:accelerometer y tasa de muestreo
		sensorManager.registerListener(this,
				accelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);

	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		//obtener en las variables x, y, z lo valores devueltos por el sensor
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];
		double a = Math.round(Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)));
		currentAcceleration = Math.abs((float) (a - calibration));
		if (currentAcceleration > maxAcceleration){
			maxAcceleration = currentAcceleration;
			//-------------- ACCIONES ASOCIADAS AL SENSOR --------------
			if( (maxAcceleration/SensorManager.STANDARD_GRAVITY) > nivel_sensor){
				if(modo_shakeme.equals("contestar")){
					answerCall();
				}else{
					ignoreCall();
				}
				this.stopSelf();//autodetener el servicio
			}
			//-----------------------------------------------------------
		}
	}


}




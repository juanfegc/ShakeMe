package com.eurekios.shakeme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.Toast;


public class ReceptorLlamadas extends BroadcastReceiver {

	Intent service;
	boolean shakemeON, inCall, activar_altavoz;
	int volumenNormalInCall=4, volumenNormalRingtone=4;

	@Override
	// Definimos el metodo onReceive para recibir mensajes de difusion
	public void onReceive(final Context context, Intent intent) {

		//leer preferencias
		SharedPreferences preferencias = context.getSharedPreferences("ShakeMePreferencias", Context.MODE_PRIVATE);
		shakemeON = preferencias.getBoolean("shakeme", true);
		inCall = preferencias.getBoolean("inCall", false);
		activar_altavoz = preferencias.getBoolean("altavoz", true);

		//App ShakeMe activada????
		if(shakemeON){

			Bundle extras = intent.getExtras();
			if (extras != null) {
				String estado = extras.getString(TelephonyManager.EXTRA_STATE);

				/*******************************************************
				 * RING-RING-RING : una llamada entrante esta sonando  *
				 *******************************************************/
				if (estado.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
					final String numeroTelefono= extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
					Toast.makeText(context, "RING-RING-RING " + numeroTelefono , Toast.LENGTH_SHORT).show();

					//Lanzar mi servicio ShakeMe
					service = new Intent(context, ShakeMeService.class);
					service.putExtra("MOBILE_NUMBER", numeroTelefono);
					service.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startService(service);
					//..............................................................................
				}


				//compruebo que solo sean conversaciones que vengan de responder una llamada agitando
				inCall = preferencias.getBoolean("inCall", false);
				if(inCall){
					/*************************
					 * Hablando: BLA-BLA-BLA *
					 *************************/
					if (estado.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
						Toast.makeText(context, "BLA-BLA-BLA", Toast.LENGTH_SHORT).show();
						if(activar_altavoz){
							
							//Poner volumen al Maximo
							AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

							//guardar volumen normal del telefono
							//volumenNormalInCall = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
							//volumenNormalRingtone = audioManager.getStreamVolume(AudioManager.STREAM_RING);
							//Toast.makeText(context, "InCall="+volumenNormalInCall+ " RingTone="+volumenNormalRingtone, Toast.LENGTH_SHORT).show();

							audioManager.setStreamVolume(
									AudioManager.STREAM_VOICE_CALL,
									audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
									0);
							//Activar Altavoz
							audioManager.setSpeakerphoneOn(true);
						}
					}

					/*******************************************************************************
					 * sin actividad, despues de colgar. OJO!! tanto si me llaman como si llamo yo *
					 *******************************************************************************/
					if (estado.equals(TelephonyManager.EXTRA_STATE_IDLE)) { //CALL_STATE_IDLE
						Toast.makeText(context, "...zZzZzZ", Toast.LENGTH_SHORT).show();
						//reseteo inCall a false
						SharedPreferences.Editor editor = preferencias.edit();;
						editor.putBoolean("inCall", false);
						editor.commit();

						if(activar_altavoz){
							AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
							//Desactivar Altavoz
							audioManager.setSpeakerphoneOn(false);
							// poner volumen normal
							audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volumenNormalInCall, 0);
							audioManager.setStreamVolume(AudioManager.STREAM_RING, volumenNormalRingtone, 0);
							//Toast.makeText(context, "InCall="+volumenNormalInCall+ " RingTone="+volumenNormalRingtone, Toast.LENGTH_SHORT).show();
						}
					}
				}//end ÀinCall?

			}//end Àextras!=null?

		}//end ÀshakemeON?

	}//end onReceive



}
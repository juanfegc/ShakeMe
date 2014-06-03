package com.eurekios.shakeme;

import java.util.Timer;
import java.util.TimerTask;

import com.eurekios.shakeme.R;

import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ConfigActivity extends Activity implements SensorEventListener {

	private ToggleButton tbServicio;
	private CheckBox chbVoz, chbAltavoz, chbCompatible;
	private RadioGroup rgAgitarPara;
	private RadioButton rbColgar, rbContestar;
	private SeekBar sbCalibracionSensor;
	private ProgressBar pbTestSensor;
	private LinearLayout miLayout, layoutServicio, layoutAltavoz;

	private SensorManager sensorManager;
	private float currentAcceleration = 0;
	private float maxAcceleration = 0;
	private final double calibration = SensorManager.STANDARD_GRAVITY;

	TextView valorSensorTextView;
	TextView maxAccelerationTextView;

	SharedPreferences.Editor editor;
	boolean shakemeON;
	String modo_shakeme;
	boolean activar_voz;
	boolean activar_altavoz;
	float nivel_sensor;
	boolean compatible;


	/****************************************************************************************************************************************************************
	 *             ACTIVITY LIFE-CYCLE   																															*
	 ****************************************************************************************************************************************************************
	 *                             -----------------------------------------------------------------------------------------------									*
	 *                             |																  | 						 |									*
	 *                             |							--------------------------------------							 |									*
	 *                             |  							|									  |						     |									*
	 *                             V							V									  |							 |									*
	 * [Activity LAUNCHED] --> onCreate() --> onStart() --> onResume() --> [Activity RUNNING] --> onPause() --> onStop() --> onDestroy() --> [Activity SHUTDOWN]	*
	 *                             			     ^																   |												*
	 *                             				 |																   V												*
	 * 							   				 |															  onRestart()											*
	 * 											 |																   |												*
	 *                                           -------------------------------------------------------------------												*
	 ****************************************************************************************************************************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//Remove notification bar
		//this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);

		initUI();

		//obtener en la variable global sensorManager una instancia del servicio SENSOR_SERVICE
		sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

		runUI();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// register this class as a listener for the orientation and accelerometer sensors
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);

		updateUI();
	}

	@Override
	protected void onPause() {
		// unregister listener
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	public void initUI() {
		//guardar preferencias
		SharedPreferences preferencias = getSharedPreferences("ShakeMePreferencias", Context.MODE_PRIVATE);
		editor = preferencias.edit();
		shakemeON = preferencias.getBoolean("shakeme", true);
		activar_voz = preferencias.getBoolean("voz", false);
		activar_altavoz = preferencias.getBoolean("altavoz", true);
		modo_shakeme = preferencias.getString("modo", "colgar");
		nivel_sensor = preferencias.getFloat("nivel_sensor", (float)1.0);
		compatible = preferencias.getBoolean("compatible", false);

		//inicializar IU
		layoutServicio = (LinearLayout) findViewById(R.id.LinearLayoutServicio);
		tbServicio = (ToggleButton)findViewById(R.id.toggleButtonServicio);
		tbServicio.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0)
			{
				if(tbServicio.isChecked()){
					//ON
					editor.putBoolean("shakeme", true);
					editor.commit();
					layoutServicio.setBackgroundColor(getResources().getColor(R.color.bg_color_ON));
					habilitarUI();
				}else{
					//OFF
					editor.putBoolean("shakeme", false);
					editor.commit();
					layoutServicio.setBackgroundColor(getResources().getColor(R.color.bg_color_OFF));
					deshabilitarUI();
				}
			}
		});

		miLayout = (LinearLayout) findViewById(R.id.LinearLayout1);
		chbVoz = (CheckBox) findViewById(R.id.checkBoxVoz);
		chbVoz.setOnCheckedChangeListener(
				new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							//VOZ Activada
							//check Text2Speech
							/*
							Intent checkIntent = new Intent();
							checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
							startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
							 */
							//Toast.makeText(getBaseContext(), getResources().getString(R.string.config_voz_activada), Toast.LENGTH_SHORT).show();
							editor.putBoolean("voz", true);
							editor.commit();
						}
						else {
							//VOZ desactivada
							//Toast.makeText(getBaseContext(), getResources().getString(R.string.config_voz_desactivada), Toast.LENGTH_SHORT).show();
							editor.putBoolean("voz", false);
							editor.commit();
						}

					}
				});
		if(activar_voz){
			chbVoz.setChecked(true);
		}else{
			chbVoz.setChecked(false);
		}

		layoutAltavoz = (LinearLayout) findViewById(R.id.layoutAltavoz);
		chbAltavoz = (CheckBox) findViewById(R.id.checkBoxAltavoz);
		chbAltavoz.setOnCheckedChangeListener(
				new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							//ALTAVOZ activado
							editor.putBoolean("altavoz", true);
							editor.commit();
						}
						else {
							//ALTAVOZ desactivado
							editor.putBoolean("altavoz", false);
							editor.commit();
						}

					}
				});
		if(activar_altavoz){
			chbAltavoz.setChecked(true);
		}else{
			chbAltavoz.setChecked(false);
		}

		rbColgar = (RadioButton)findViewById(R.id.radioButtonColgar);
		rbContestar = (RadioButton)findViewById(R.id.radioButtonContestar);
		if(modo_shakeme.equals("colgar")){
			rbColgar.setChecked(true);
			rbContestar.setChecked(false);
			layoutAltavoz.setVisibility(View.GONE);
		}else{
			rbColgar.setChecked(false);
			rbContestar.setChecked(true);
			layoutAltavoz.setVisibility(View.VISIBLE);
		}
		rgAgitarPara = (RadioGroup)findViewById(R.id.radioGroupAgitarPara);
		rgAgitarPara.setOnCheckedChangeListener(
				new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if(checkedId == R.id.radioButtonContestar){
							//Agitar para contestar
							//Toast.makeText(getBaseContext(), getResources().getString(R.string.config_agitar_contestar), Toast.LENGTH_SHORT).show();
							editor.putString("modo", "contestar");
							editor.commit();
							layoutAltavoz.setVisibility(View.VISIBLE);
						}else if(checkedId == R.id.radioButtonColgar){
							//Agitar para colgar
							//Toast.makeText(getBaseContext(), getResources().getString(R.string.config_agitar_rechazar), Toast.LENGTH_SHORT).show();
							editor.putString("modo", "colgar");
							editor.commit();
							layoutAltavoz.setVisibility(View.GONE);
						}
					}
				});


		valorSensorTextView = (TextView)findViewById(R.id.textViewValorSensor);
		valorSensorTextView.setText(getResources().getString(R.string.config_nivel)+" "+Float.toString(nivel_sensor)+" Gs");
		
		sbCalibracionSensor = (SeekBar)findViewById(R.id.seekBarCalibrarSensor);
		sbCalibracionSensor.setProgress((int)nivel_sensor*2);
		sbCalibracionSensor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//TODO:establecer nivel sensibilidad
				float nivel=(float) (seekBar.getProgress()*0.5);
				valorSensorTextView.setText(getResources().getString(R.string.config_nivel)+" "+Float.toString(nivel)+" Gs");
				editor.putFloat("nivel_sensor", nivel);
				editor.commit();
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {}
		});
		

		pbTestSensor = (ProgressBar) findViewById(R.id.progressBarTestSensor);
		if(Build.VERSION.SDK_INT >=14){//4.0 ICE_CREAM_SANDWICH, 4.1-4.2 JELLY_BEAN
			pbTestSensor.getProgressDrawable().setColorFilter(Color.RED, Mode.SRC_IN);
		}

		maxAccelerationTextView = (TextView) findViewById(R.id.maxAcceleration);
		
		chbCompatible = (CheckBox) findViewById(R.id.checkBoxCompatible);
		chbCompatible.setOnCheckedChangeListener(
				new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							//COMPATIBLE ON
							//Toast.makeText(getBaseContext(), "Compatible ON", Toast.LENGTH_SHORT).show();
							editor.putBoolean("compatible", true);
							editor.commit();
						}else{
							//COMPATIBLE OFF
							//Toast.makeText(getBaseContext(), "Compatible OFF", Toast.LENGTH_SHORT).show();
							editor.putBoolean("compatible", false);
							editor.commit();
						}

					}
				});
		if(compatible){
			chbCompatible.setChecked(true);
		}else{
			chbCompatible.setChecked(false);
		}
	}
	
	public void updateUI(){	
		if(shakemeON){
			tbServicio.setChecked(true);
			editor.putBoolean("shakeme", true);
			editor.commit();
			layoutServicio.setBackgroundColor(getResources().getColor(R.color.bg_color_ON));
			habilitarUI();
		}else{
			tbServicio.setChecked(false);
			editor.putBoolean("shakeme", false);
			editor.commit();
			layoutServicio.setBackgroundColor(getResources().getColor(R.color.bg_color_OFF));
			deshabilitarUI();
		}
	}

	public void runUI() {
		Timer updateTimer = new Timer("gForceUpdate");
		updateTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				updateGUI();//cada 0.5s
			}
		}, 0, 500);
	}
	private void updateGUI() {
		//metodo ligado a la actividad que permite ejecutar en la hebra principal
		//desde la hebra lanzada con un temporizador "TimerTask"
		runOnUiThread(new Runnable() {
			public void run() {
				String maxG = maxAcceleration / SensorManager.STANDARD_GRAVITY+" Gs";
				maxAccelerationTextView.setText(maxG);
				maxAccelerationTextView.invalidate();
			}
		});
	};

	private void resetMaxAccelerationGUI() {
		runOnUiThread(new Runnable() {
			public void run() {
				maxAcceleration = 0;
				String maxG = maxAcceleration / SensorManager.STANDARD_GRAVITY+" Gs";
				maxAccelerationTextView.setText(maxG);
				maxAccelerationTextView.invalidate();
			}
		});
	};

	public void onClickReset(View v){
		resetMaxAccelerationGUI();
	}

	public void onClickSaveAndExit(View v){
		ConfigActivity.this.finish();
	}

	public void deshabilitarUI(){
		//efecto setAlpha() para API<11
		AlphaAnimation alpha = new AlphaAnimation(0.5F, 0.5F);
		alpha.setDuration(0); // animacion instantanea
		alpha.setFillAfter(true); // alpha persistente
		miLayout.startAnimation(alpha);

		chbVoz.setFocusable(false);
		chbVoz.setClickable(false);
		chbVoz.setEnabled(false);
		chbAltavoz.setFocusable(false);
		chbAltavoz.setClickable(false);
		chbAltavoz.setEnabled(false);
		rbColgar.setFocusable(false);
		rbColgar.setClickable(false);
		rbColgar.setEnabled(false);
		rbContestar.setFocusable(false);
		rbContestar.setClickable(false);
		rbContestar.setEnabled(false);
		sbCalibracionSensor.setFocusable(false);
		sbCalibracionSensor.setClickable(false);
		sbCalibracionSensor.setEnabled(false);
		pbTestSensor.setFocusable(false);
		pbTestSensor.setClickable(false);
		pbTestSensor.setEnabled(false);
		maxAccelerationTextView.setVisibility(View.INVISIBLE);
		chbCompatible.setFocusable(false);
		chbCompatible.setClickable(false);
		chbCompatible.setEnabled(false);
	}

	public void habilitarUI(){
		//efecto setAlpha() para API<11
		AlphaAnimation alpha = new AlphaAnimation(1.0F, 1.0F);
		alpha.setDuration(0); // animacion instantanea
		alpha.setFillAfter(true); // alpha persistente
		miLayout.startAnimation(alpha);


		chbAltavoz.setFocusable(true);
		chbAltavoz.setClickable(true);
		chbAltavoz.setEnabled(true);
		chbVoz.setFocusable(true);
		chbVoz.setClickable(true);
		chbVoz.setEnabled(true);
		rbColgar.setFocusable(true);
		rbColgar.setClickable(true);
		rbColgar.setEnabled(true);
		rbContestar.setFocusable(true);
		rbContestar.setClickable(true);
		rbContestar.setEnabled(true);
		sbCalibracionSensor.setFocusable(true);
		sbCalibracionSensor.setClickable(true);
		sbCalibracionSensor.setEnabled(true);
		pbTestSensor.setFocusable(true);
		pbTestSensor.setClickable(true);
		pbTestSensor.setEnabled(true);
		maxAccelerationTextView.setVisibility(View.VISIBLE);
		chbCompatible.setFocusable(true);
		chbCompatible.setClickable(true);
		chbCompatible.setEnabled(true);
	}


	/********************************
	 *     SENSOR: ACELEROMETRO     *
	 ********************************/
	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
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
		int progreso;
		currentAcceleration = Math.abs((float) (a - calibration));
		if (currentAcceleration > maxAcceleration){
			maxAcceleration = currentAcceleration;
			progreso = Math.round((maxAcceleration/SensorManager.STANDARD_GRAVITY)*2);
			pbTestSensor.setProgress(progreso);
		}
	}

}

package com.eurekios.shakeme;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;

public class SplashActivity extends Activity {

	TextView titulo;
	TextView subtitulo;
	ImageView logo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		//android:theme="@android:style/Theme.Light.NoTitleBar" en el AndroidManifest.xml es equivalente a:
		//Remove title bar
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//Remove notification bar
		//this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		//Fuente de texto personalizada
		Typeface myTypeface = Typeface.createFromAsset(getAssets(), "fonts/gh.ttf");

		titulo = (TextView)findViewById(R.id.textViewTitulo);
		titulo.setTypeface(myTypeface);

		subtitulo = (TextView)findViewById(R.id.textViewSubTitulo);
		subtitulo.setTypeface(myTypeface);
		
		logo=(ImageView)findViewById(R.id.imageViewLogo);
		
		startAnimating();//llamar animacion
	}
	
	/**
	 * Animacion del splash
	 */
	private void startAnimating() {
		// Animar Logo
        Animation rotacion = AnimationUtils.loadAnimation(this, R.anim.rotacion);
        logo.startAnimation(rotacion);
        
        // Animar subtitulo
		Animation transparencia = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		subtitulo.startAnimation(transparencia);
     
        // Transicion al Menu pricipal cuando la animacion del subtitulo termina
		transparencia.setAnimationListener(new AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                // Cuando la animacion del subtitulo acabe, lanzar la activity del Main Menu screen
                startActivity(new Intent(SplashActivity.this, ConfigActivity.class));
                SplashActivity.this.finish();
            }
            public void onAnimationRepeat(Animation animation) {
            }
            public void onAnimationStart(Animation animation) {
            }
        });
        
    }
	
	
	@Override
    protected void onPause() {
        super.onPause();
        // Stop the animation
        subtitulo.clearAnimation();
        logo.clearAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start animating at the beginning so we get the full splash screen experience
        startAnimating();
    }
    

}

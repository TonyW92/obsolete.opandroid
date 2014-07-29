package com.openpeer.openpeernativesampleapp;

import org.webrtc.videoengine.ViERenderer;

import com.openpeer.openpeernativesampleapp.R;
import com.openpeer.javaapi.CameraTypes;
import com.openpeer.javaapi.OPLogger;
import com.openpeer.javaapi.OPMediaEngine;
import com.openpeer.javaapi.OPStack;
import com.openpeer.javaapi.OPStackMessageQueue;
import com.openpeer.javaapi.VideoOrientations;
import com.openpeer.javaapi.test.OPTestMediaEngine;
import com.openpeer.openpeernativesampleapp.LoginManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.sax.TextElementListener;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MediaScreen extends Activity {

	SurfaceView myLocalSurface = null;
	SurfaceView myRemoteSurface = null;
	int mediaEngineStatus = 0;
	boolean speakerphoneEnabled = false;
	boolean useFrontCamera = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_media_screen);
		
		EditText remoteIPAddressEditText = (EditText) findViewById(R.id.remoteIPAddressEditText);
		remoteIPAddressEditText.setText("127.0.0.1");
		
		OPLogger.installTelnetLogger(59999, 60, true);
		
		OPMediaEngine.init(getApplicationContext());
		if (useFrontCamera)
			OPMediaEngine.getInstance().setCameraType(CameraTypes.CameraType_Front);
		else
			OPMediaEngine.getInstance().setCameraType(CameraTypes.CameraType_Back);
		OPMediaEngine.getInstance().setEcEnabled(true);
		OPMediaEngine.getInstance().setAgcEnabled(true);
		OPMediaEngine.getInstance().setNsEnabled(false);
		OPMediaEngine.getInstance().setMuteEnabled(false);
		OPMediaEngine.getInstance().setLoudspeakerEnabled(false);
		OPMediaEngine.getInstance().setContinuousVideoCapture(true);
		OPMediaEngine.getInstance().setDefaultVideoOrientation(VideoOrientations.VideoOrientation_Portrait);
		OPMediaEngine.getInstance().setRecordVideoOrientation(VideoOrientations.VideoOrientation_LandscapeRight);
		OPMediaEngine.getInstance().setFaceDetection(false);
		
		setupMediaControlButton();
		setupAudioOutputButton();
	}

	private void setupMediaControlButton() {
		final Button mediaControlButton = (Button) findViewById(R.id.buttonMedia);
		final EditText remoteIPAddressEditText = (EditText) findViewById(R.id.remoteIPAddressEditText);
		mediaControlButton.setText("Start Video Capture");
		final MediaScreen screen = this;
		
		mediaControlButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) { 
				LinearLayout localViewLinearLayout;
				LinearLayout remoteViewLinearLayout;
				switch (mediaEngineStatus) {
				case 0:
					myLocalSurface = ViERenderer.CreateLocalRenderer(screen);
					localViewLinearLayout = (LinearLayout) findViewById(R.id.localViewLinearLayout);
					localViewLinearLayout.addView(myLocalSurface);
					OPMediaEngine.getInstance().startVideoCapture();
					mediaControlButton.setText("Start Media Channel");
					mediaEngineStatus++;
					break;
				case 1:
					myRemoteSurface = ViERenderer.CreateRenderer(screen, true);
					remoteViewLinearLayout = (LinearLayout) findViewById(R.id.remoteViewLinearLayout);
					remoteViewLinearLayout.addView(myRemoteSurface);
					OPMediaEngine.getInstance().setChannelRenderView(myRemoteSurface);
					((OPTestMediaEngine) OPMediaEngine.getInstance()).setReceiverAddress(remoteIPAddressEditText.getText().toString());
					((OPTestMediaEngine) OPMediaEngine.getInstance()).startVoice();
					((OPTestMediaEngine) OPMediaEngine.getInstance()).startVideoChannel();
					mediaControlButton.setText("Stop Media Channel");
					mediaEngineStatus++;
					break;
				case 2:
					((OPTestMediaEngine) OPMediaEngine.getInstance()).stopVoice();
					((OPTestMediaEngine) OPMediaEngine.getInstance()).stopVideoChannel();
					remoteViewLinearLayout = (LinearLayout) findViewById(R.id.remoteViewLinearLayout);
					remoteViewLinearLayout.removeView(myRemoteSurface);
					myRemoteSurface = null;
					mediaControlButton.setText("Stop Video Capture");
					mediaEngineStatus++;
					break;
				case 3:
					OPMediaEngine.getInstance().stopVideoCapture();
					localViewLinearLayout = (LinearLayout) findViewById(R.id.localViewLinearLayout);
					localViewLinearLayout.removeView(myLocalSurface);
					myLocalSurface = null;
					mediaControlButton.setText("Start Video Capture");
					mediaEngineStatus = 0;
					break;
				default:
					break;
				}
			}
		});
	}

	private void setupAudioOutputButton() {
		final Button audioOutputButton = (Button) findViewById(R.id.buttonAudioOutput);
		audioOutputButton.setText("Speakerphone");
		
		audioOutputButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (speakerphoneEnabled) {
					OPMediaEngine.getInstance().setLoudspeakerEnabled(false);
					audioOutputButton.setText("Speakerphone");
				} else {
					OPMediaEngine.getInstance().setLoudspeakerEnabled(true);
					audioOutputButton.setText("Ear Speaker");
				}
				speakerphoneEnabled = !speakerphoneEnabled;
			}
		});
	}
	
	static {
		try {
			System.loadLibrary("z_shared");
			System.loadLibrary("openpeer");
		} catch (UnsatisfiedLinkError use) {
			Log.e("JNI", "WARNING: Could not load libopenpeer.so");
		}
	}
}
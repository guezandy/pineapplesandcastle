package com.example.snagtagnfc;

/*
 * Important notes on NFC the Samgsung Galaxy S4 is not compatible with the classic Mifare Tags because it 
 * uses the Broadcom NFC chip.
 * NTAG203 tags are fully compatible with all nfc phones including the galaxy s4
 */



import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


@SuppressLint("ShowToast")
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class MainActivity extends Activity {

	private NfcAdapter mNfcAdapter;
	private Button mEnableWriteButton;
	private EditText mTextField;
	private ProgressBar mProgressBar;
	public Button backB;

	@SuppressLint("CutPasteId")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		mTextField = (EditText) findViewById(R.id.nfcWriteString);
		mProgressBar = (ProgressBar) findViewById(R.id.pbWriteToTag);
		mProgressBar.setVisibility(View.GONE);
		
		mEnableWriteButton = (Button) findViewById(R.id.writeToTagButton);
		
		mEnableWriteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setTagWriteReady(!isWriteReady);
				mProgressBar.setVisibility(isWriteReady ? View.VISIBLE : View.GONE);
			}
		});

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			Toast.makeText(this, "Sorry, NFC is not available on this device", Toast.LENGTH_SHORT).show();
			finish();
		}
		
	}

	
	private boolean isWriteReady = false;

	/**
	 * Enable this activity to write to a tag
	 * 
	 * @param isWriteReady
	 */
	public void setTagWriteReady(boolean isWriteReady) {
		this.isWriteReady = isWriteReady;
		if (isWriteReady) {
			IntentFilter[] writeTagFilters = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) };
			mNfcAdapter.enableForegroundDispatch(MainActivity.this, NfcUtils.getPendingIntent(MainActivity.this),
					writeTagFilters, null);
		} else {
			// Disable dispatch if not writing tags
			mNfcAdapter.disableForegroundDispatch(MainActivity.this);
		}
		mProgressBar.setVisibility(isWriteReady ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onNewIntent(Intent intent) {
		// onResume gets called after this to handle the intent
		setIntent(intent);
	}

	@Override
	public void onResume() {
		super.onResume();
		Bundle bundle = getIntent().getExtras();
		if (isWriteReady && NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())) {
			processWriteIntent(getIntent());
		} else if (!isWriteReady
				&& (NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED
						.equals(getIntent().getAction()))) {
			mTextField.setVisibility(View.GONE);
			mEnableWriteButton.setVisibility(View.GONE);
			processReadIntent(getIntent());
		} 
	}

	private static final String MIME_TYPE = "application/com.example.snagtag.tag";

	/**
	 * Write to an NFC tag; reacting to an intent generated from foreground
	 * dispatch requesting a write
	 * 
	 * @param intent
	 */
	public void processWriteIntent(Intent intent) {
		if (isWriteReady && NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())) {

			Tag detectedTag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

			String tagWriteMessage = mTextField.getText().toString();
			byte[] payload = new String(tagWriteMessage).getBytes();

			if (detectedTag != null && NfcUtils.writeTag(
					NfcUtils.createMessage(MIME_TYPE, payload), detectedTag)) {
				
				Toast.makeText(this, "Wrote '" + tagWriteMessage + "' to a tag!", 
						Toast.LENGTH_LONG).show();
				setTagWriteReady(false);
			} else {
				Toast.makeText(this, "Write failed. Please try again.", Toast.LENGTH_LONG).show();
			}
		}
	}

	public void processReadIntent(Intent intent) {
		List<NdefMessage> intentMessages = NfcUtils.getMessagesFromIntent(intent);
		List<String> payloadStrings = new ArrayList<String>(intentMessages.size());

		for (NdefMessage message : intentMessages) {
			for (NdefRecord record : message.getRecords()) {
				byte[] payload = record.getPayload();
				String payloadString = new String(payload);

				if (!TextUtils.isEmpty(payloadString))
					payloadStrings.add(payloadString);
			}
		}

		if (!payloadStrings.isEmpty()) {
			String content =  TextUtils.join(",", payloadStrings);
			Toast.makeText(MainActivity.this, "Read from tag: " + content,
					Toast.LENGTH_LONG).show();
			System.out.println(content);
		}
	}

}

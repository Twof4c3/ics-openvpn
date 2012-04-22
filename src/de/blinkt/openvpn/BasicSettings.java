/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.blinkt.openvpn;

import java.util.HashMap;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.blinkt.openvpn.R.id;

public class BasicSettings extends Fragment implements View.OnClickListener, OnItemSelectedListener, Callback, OnCheckedChangeListener {
	private static final String TAG = "OpenVpnClient";


	private static final int START_OPENVPN = 0;
	private static final int CHOOSE_FILE_OFFSET = 1000;
	private static final int UPDATE_ALIAS = 20;

	private static final String PREFS_NAME = "OVPN_SERVER";

	private static final String OVPNCONFIGFILE = "android.conf";
	private static final String OVPNCONFIGPKCS12 = "android.pkcs12";


	private TextView mServerAddress;
	private TextView mServerPort;
	private FileSelectLayout mClientCert;
	private FileSelectLayout mCaCert;
	private FileSelectLayout mClientKey;
	private TextView mAliasName;
	private CheckBox mUseLzo;
	private ToggleButton mTcpUdp;
	private Spinner mType;
	private FileSelectLayout mpkcs12;
	private TextView mPKCS12Password;

	private Handler mHandler;


	private CheckBox mUseTlsAuth;


	private CheckBox mShowAdvanced;


	private FileSelectLayout mTlsFile;

	private HashMap<Integer, FileSelectLayout> fileselects = new HashMap<Integer, FileSelectLayout>();


	private Spinner mTLSDirection;


	private EditText mUserName;


	private EditText mPassword;


	private View mView;


	private VpnProfile mProfile;



	private void addFileSelectLayout (FileSelectLayout fsl) {
		int i = fileselects.size() + CHOOSE_FILE_OFFSET;
		fileselects.put(i, fsl);
		fsl.setActivity(getActivity(),i);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		mView = inflater.inflate(R.layout.basic_settings,container,false);


		mServerAddress = (TextView) mView.findViewById(R.id.address);
		mServerPort = (TextView) mView.findViewById(R.id.port);
		mClientCert = (FileSelectLayout) mView.findViewById(R.id.certselect);
		mClientKey = (FileSelectLayout) mView.findViewById(R.id.keyselect);
		mCaCert = (FileSelectLayout) mView.findViewById(R.id.caselect);
		mpkcs12 = (FileSelectLayout) mView.findViewById(R.id.pkcs12select);
		mUseLzo = (CheckBox) mView.findViewById(R.id.lzo);
		mTcpUdp = (ToggleButton) mView.findViewById(id.tcpudp);
		mType = (Spinner) mView.findViewById(R.id.type);
		mPKCS12Password = (TextView) mView.findViewById(R.id.pkcs12password);
		mAliasName = (TextView) mView.findViewById(R.id.aliasname);
		mUseTlsAuth = (CheckBox) mView.findViewById(R.id.useTLSAuth);
		mTLSDirection = (Spinner) mView.findViewById(R.id.tls_direction);

		mShowAdvanced = (CheckBox) mView.findViewById(R.id.show_advanced);
		mTlsFile = (FileSelectLayout) mView.findViewById(R.id.tlsAuth);		
		mUserName = (EditText) mView.findViewById(R.id.auth_username);
		mPassword = (EditText) mView.findViewById(R.id.auth_password);
		

		addFileSelectLayout(mCaCert);
		addFileSelectLayout(mClientCert);
		addFileSelectLayout(mClientKey);
		addFileSelectLayout(mTlsFile);
		addFileSelectLayout(mpkcs12);

		loadPreferences();

		mType.setOnItemSelectedListener(this);

		mShowAdvanced.setOnCheckedChangeListener(this);
		mUseTlsAuth.setOnCheckedChangeListener(this);


		mView.findViewById(R.id.select_keystore_button).setOnClickListener(this);
		mView.findViewById(R.id.about).setOnClickListener(this);
		mView.findViewById(R.id.connect).setOnClickListener(this);		

		if (mHandler == null) {
			mHandler = new Handler(this);
		}
		return mView;
	}


	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == mType) {
			changeType(position);
		}
	}



	private void changeType(int type){
		// hide everything
		mView.findViewById(R.id.pkcs12).setVisibility(View.GONE);
		mView.findViewById(R.id.certs).setVisibility(View.GONE);
		mView.findViewById(R.id.statickeys).setVisibility(View.GONE);
		mView.findViewById(R.id.keystore).setVisibility(View.GONE);

		switch(type) {
		case VpnProfile.TYPE_CERTIFICATES:
			mView.findViewById(R.id.certs).setVisibility(View.VISIBLE);
			break;
		case VpnProfile.TYPE_PKCS12:
			mView.findViewById(R.id.pkcs12).setVisibility(View.VISIBLE);
			break;
		case VpnProfile.TYPE_STATICKEYS:
			mView.findViewById(R.id.statickeys).setVisibility(View.VISIBLE);
			break;
		case VpnProfile.TYPE_KEYSTORE:
			mView.findViewById(R.id.keystore).setVisibility(View.VISIBLE);
			break;
			
		case VpnProfile.TYPE_USERPASS:
			mView.findViewById(R.id.userpassword).setVisibility(View.VISIBLE);
		}


	}

	private void loadPreferences() {
		mProfile = ((VPNPreferences)getActivity()).getVPNProfile();
		
		mClientCert.setData(mProfile.mClientCertFilename);
		mClientKey.setData(mProfile.mClientKeyFilename);
		mCaCert.setData(mProfile.mCaFilename);

		mUseLzo.setChecked(mProfile.mUseLzo);
		mServerPort.setText(mProfile.mServerPort);
		mServerAddress.setText(mProfile.mServerName);
		mTcpUdp.setChecked(mProfile.mUseUdp);
		mType.setSelection(mProfile.mAuthenticationType);
		mpkcs12.setData(mProfile.mPKCS12Filename);
		mPKCS12Password.setText(mProfile.mPKCS12Password);

		mUseTlsAuth.setChecked(mProfile.mUseTLSAuth);
		onCheckedChanged(mUseTlsAuth,mUseTlsAuth.isChecked());
		
		mTlsFile.setData(mProfile.mTLSAuthFilename);
		mTLSDirection.setSelection(mProfile.mTLSAuthDirection);
		setAlias();

	}

	private void savePreferences() {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context

		
		mProfile.mCaFilename = mCaCert.getData();
		mProfile.mClientCertFilename = mClientCert.getData();
		mProfile.mClientKeyFilename = mClientKey.getData();

		mProfile.mUseLzo = mUseLzo.isChecked();
		mProfile.mServerPort =mServerPort.getText().toString();
		mProfile.mServerName = mServerAddress.getText().toString();
		mProfile.mUseUdp = mTcpUdp.isChecked();

		mProfile.mAuthenticationType = mType.getSelectedItemPosition();
		mProfile.mPKCS12Filename = mpkcs12.getData();
		mProfile.mPKCS12Password = mPKCS12Password.getText().toString();
		mProfile.mUseTLSAuth =mUseTlsAuth.isChecked();
		mProfile.mTLSAuthFilename= mTlsFile.getData();
		mProfile.mTLSAuthDirection =mTLSDirection.getSelectedItemPosition();
		// Commit the edits!

	}


	private void setAlias() {
		if(mProfile.mAlias == null) {
			mAliasName.setText(R.string.client_no_certificate);
		} else {
			mAliasName.setText(mProfile.mAlias);
		}
	}

	public void showCertDialog () {
		KeyChain.choosePrivateKeyAlias(getActivity(),
				new KeyChainAliasCallback() {

			public void alias(String alias) {
				// Credential alias selected.  Remember the alias selection for future use.
				mProfile.mAlias=alias;
				mHandler.sendEmptyMessage(UPDATE_ALIAS);
			}


		},
		new String[] {"RSA", "DSA"}, // List of acceptable key types. null for any
		null,                        // issuer, null for any
		"internal.example.com",      // host name of server requesting the cert, null if unavailable
		443,                         // port of server requesting the cert, -1 if unavailable
		null);                       // alias to preselect, null if unavailable
	}

	@Override
	public void onClick(View v) {
		if (v == mView.findViewById(R.id.select_keystore_button)) {
			showCertDialog();
		}
	}



	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}


	@Override
	public boolean handleMessage(Message msg) {
		setAlias();
		return true;
	}


	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int visibility;
		if(isChecked) 
			visibility =View.VISIBLE;
		else 
			visibility =View.GONE;

		if(buttonView==mShowAdvanced) {
			mView.findViewById(R.id.advanced_options).setVisibility(visibility);
		} else if (buttonView == mUseTlsAuth) {
			mView.findViewById(R.id.tlsauth_options).setVisibility(visibility);
		}
	}
}
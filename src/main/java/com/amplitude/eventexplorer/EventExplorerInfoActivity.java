package com.amplitude.eventexplorer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amplitude.R;
import com.amplitude.api.Amplitude;

public class EventExplorerInfoActivity extends Activity {
    private ImageView closeImageView;
    private Button deviceIdCopyButton;
    private Button userIdCopyButton;

    private TextView deviceIdTextView;
    private TextView userIdTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.amp_activity_eventexplorer_info);

        this.closeImageView = findViewById(R.id.amp_eeInfo_iv_close);
        this.closeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        this.deviceIdTextView = findViewById(R.id.amp_eeInfo_tv_deviceId);
        this.userIdTextView = findViewById(R.id.amp_eeInfo_tv_userId);

        Intent intent = getIntent();
        String instanceName = intent.getExtras().getString("instanceName");

        String deviceId = Amplitude.getInstance(instanceName).getDeviceId();
        String userId = Amplitude.getInstance(instanceName).getUserId();

        this.deviceIdTextView.setText(deviceId != null ? deviceId : getString(R.string.amp_label_not_avail));
        this.userIdTextView.setText(userId != null ? userId : getString(R.string.amp_label_not_avail));

        this.deviceIdCopyButton = findViewById(R.id.amp_eeInfo_btn_copyDeviceId);
        this.deviceIdCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyText(view.getContext(), deviceId);
            }
        });

        this.userIdCopyButton = findViewById(R.id.amp_eeInfo_btn_copyUserId);
        this.userIdCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyText(view.getContext(), userId);
            }
        });
    }
    
    private void copyText(Context context, String text) {
        if (text != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("copied text", text);
            clipboard.setPrimaryClip(clip);

            Toast toast = Toast.makeText(context, getString(R.string.amp_label_copied), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

}

package com.amplitude.api;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.net.ssl.SSLSocketFactory;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class PinningTest extends BaseTest {
    
}

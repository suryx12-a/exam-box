package com.smartexambox.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.theme.NavyBackground
import com.smartexambox.security.ui.theme.SmartExamSecurityTheme

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: MainSecurityViewModel

    private val usbRemoteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleSecurityIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainSecurityViewModel::class.java]

        handleSecurityIntent(intent)

        setContent {
            SmartExamSecurityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NavyBackground
                ) {
                    MainNavigation(
                        viewModel = viewModel,
                        activity = this@MainActivity
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("com.smartexambox.security.ACTION_REMOTE_LOCK")
            addAction("com.smartexambox.security.ACTION_REMOTE_UNLOCK")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbRemoteReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbRemoteReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(usbRemoteReceiver)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSecurityIntent(intent)
    }

    private fun handleSecurityIntent(intent: Intent?) {
        val action = intent?.action
        val data = intent?.dataString
        if (action == "com.smartexambox.security.ACTION_REMOTE_LOCK" || data?.contains("lock") == true) {
            viewModel.lockVault()
        } else if (action == "com.smartexambox.security.ACTION_REMOTE_UNLOCK" || data?.contains("unlock") == true) {
            viewModel.unlockVaultDirectly("Authorized via USB Controller Link")
        }
    }
}

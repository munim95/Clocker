package com.rigid.clocker.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.rigid.clocker.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
//        Toolbar toolbar = findViewById(R.id.mainToolbar);
//        setSupportActionBar(toolbar);
//        getActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsPreferenceFragment())
                .commit();
    }

    public static class SettingsPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private DropDownPreference dropDownPreference;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_preference,rootKey);
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            dropDownPreference=findPreference("clockmode");
            dropDownPreference.setTitle(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("clockmode","12")+" Hour");
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key){
                case "clockmode":
                    dropDownPreference.setTitle(dropDownPreference.getEntry());
                    break;
                case "notifications":
                    findPreference("wakelock").setEnabled(((SwitchPreference)findPreference("notifications")).isChecked());
                    break;
            }
        }
    }
}

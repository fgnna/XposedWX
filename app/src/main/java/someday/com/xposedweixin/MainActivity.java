package someday.com.xposedweixin;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!isModuleActive()){
            Toast.makeText(this, "模块未启动", LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, "模块已启动", LENGTH_LONG).show();
        }
    }

    private boolean isModuleActive(){
        return false;
    }
}

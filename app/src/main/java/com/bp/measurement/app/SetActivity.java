package com.bp.measurement.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import java.util.ArrayList;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;


public class SetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_activity);
        EditText name = findViewById(R.id.name);
        EditText age = findViewById(R.id.age);
        EditText height = findViewById(R.id.height);
        EditText weight = findViewById(R.id.weight);
        EditText med = findViewById(R.id.med);
        EditText hand = findViewById(R.id.hand);
        EditText act_hr = findViewById(R.id.act_hr);
        EditText act_sbp = findViewById(R.id.act_sbp);
        EditText act_dbp = findViewById(R.id.act_dbp);

        Spinner s = (Spinner) findViewById(R.id.gender);
        Button start = (Button) findViewById(R.id.start);



        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("Male");
        arrayList.add("Female");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startMeasure = new Intent(SetActivity.this, MainActivity.class);
                startMeasure.putExtra("name", name.getText().toString());
                startMeasure.putExtra("age", age.getText().toString());
                startMeasure.putExtra("height", height.getText().toString());
                startMeasure.putExtra("weight", weight.getText().toString());
                startMeasure.putExtra("med", med.getText().toString());
                startMeasure.putExtra("hand", hand.getText().toString());
                startMeasure.putExtra("gender", s.getSelectedItem().toString());
                startMeasure.putExtra("act_hr", act_hr.getText().toString());
                startMeasure.putExtra("act_sbp", act_sbp.getText().toString());
                startMeasure.putExtra("act_dbp", act_dbp.getText().toString());

                SetActivity.this.startActivity(startMeasure);
                SetActivity.this.finish();
            }
        });


    }







}